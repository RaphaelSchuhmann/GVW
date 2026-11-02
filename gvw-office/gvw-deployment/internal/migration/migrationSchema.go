package migration

import (
	"encoding/json"
	"fmt"
	"math"
	"slices"
	"strings"

	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
)

const (
	// enforceSchemaConformance makes the dry run check every migrated document against the schema.
	enforceSchemaConformance = true
	// strictUnknownMembers rejects members that the schema does not know (except "_"-prefixed CouchDB fields).
	strictUnknownMembers = true
)

type typeKind int

const (
	kindAny typeKind = iota
	kindString
	kindNumber
	kindBool
	kindModel
	kindArray
	kindMap
)

type TypeRef struct {
	Kind    typeKind
	IntOnly bool     // number that must be a whole number (int, long, ...)
	Model   string   // kindModel
	Items   *TypeRef // kindArray
	Values  *TypeRef // kindMap
}

type Model struct {
	Name     string
	Embedded bool
	IsEnum   bool
	Members  map[string]*TypeRef
}

type Schema struct {
	Models map[string]*Model
}

// ParseSchema parses the schema.json produced by the build step.
// Top-level entries that do not look like models are skipped.
func ParseSchema(data []byte) (*Schema, error) {
	var top map[string]json.RawMessage
	if err := json.Unmarshal(data, &top); err != nil {
		return nil, err
	}

	s := &Schema{Models: make(map[string]*Model, len(top))}
	for name, raw := range top {
		var rm struct {
			Embedded bool                       `json:"embedded"`
			ObjType  string                     `json:"objType"`
			Members  map[string]json.RawMessage `json:"members"`
		}
		if err := json.Unmarshal(raw, &rm); err != nil {
			continue // not a model entry (e.g. an enum with a list of constants)
		}
		isEnum := strings.EqualFold(rm.ObjType, "enum")
		if rm.Members == nil && !isEnum {
			continue
		}

		model := &Model{
			Name:     name,
			Embedded: rm.Embedded,
			IsEnum:   isEnum,
			Members:  make(map[string]*TypeRef, len(rm.Members)),
		}
		for memberName, memberRaw := range rm.Members {
			t, err := parseTypeRef(memberRaw)
			if err != nil {
				return nil, fmt.Errorf("model %s, member %s: %w", name, memberName, err)
			}
			model.Members[memberName] = t
		}
		s.Models[name] = model
	}
	return s, nil
}

func parseTypeRef(raw json.RawMessage) (*TypeRef, error) {
	if len(raw) == 0 {
		return &TypeRef{Kind: kindAny}, nil
	}

	var name string
	if err := json.Unmarshal(raw, &name); err == nil {
		return scalarOrModel(name), nil
	}

	var obj struct {
		Type   string          `json:"type"`
		Items  json.RawMessage `json:"items"`
		Values json.RawMessage `json:"values"`
	}
	if err := json.Unmarshal(raw, &obj); err != nil {
		return nil, err
	}

	switch strings.ToLower(obj.Type) {
	case "array", "list", "set":
		items, err := parseTypeRef(obj.Items)
		if err != nil {
			return nil, err
		}
		return &TypeRef{Kind: kindArray, Items: items}, nil
	case "object", "map":
		values, err := parseTypeRef(obj.Values)
		if err != nil {
			return nil, err
		}
		return &TypeRef{Kind: kindMap, Values: values}, nil
	default:
		return &TypeRef{Kind: kindAny}, nil
	}
}

func scalarOrModel(name string) *TypeRef {
	switch strings.ToLower(strings.TrimSpace(name)) {
	case "string":
		return &TypeRef{Kind: kindString}
	case "int", "integer", "long", "short", "byte":
		return &TypeRef{Kind: kindNumber, IntOnly: true}
	case "float", "double", "number", "decimal":
		return &TypeRef{Kind: kindNumber}
	case "bool", "boolean":
		return &TypeRef{Kind: kindBool}
	case "", "any", "object":
		return &TypeRef{Kind: kindAny}
	default:
		return &TypeRef{Kind: kindModel, Model: name}
	}
}

// lookup finds a model by name, ignoring case (config keys may differ in casing from the schema).
func (s *Schema) lookup(name string) *Model {
	if m, ok := s.Models[name]; ok {
		return m
	}
	for n, m := range s.Models {
		if strings.EqualFold(n, name) {
			return m
		}
	}
	return nil
}

// RootModelsFor returns the (non-embedded) models that are stored in the given database.
func (s *Schema) RootModelsFor(database string, modelToDbMap map[string]string) []string {
	var out []string
	for name, db := range modelToDbMap {
		if db != database {
			continue
		}
		if m := s.lookup(name); m != nil && !m.Embedded {
			out = append(out, m.Name)
		}
	}
	slices.Sort(out)
	return out
}

func (s *Schema) memberOfAny(roots []string, name string) bool {
	for _, r := range roots {
		if m := s.Models[r]; m != nil {
			if _, ok := m.Members[name]; ok {
				return true
			}
		}
	}
	return false
}

// PathExists reports whether the path is a member path of at least one of the root models.
// Paths through models the schema does not know are given the benefit of the doubt.
func (s *Schema) PathExists(roots []string, path string) bool {
	segs := splitPath(path)
	for _, r := range roots {
		if s.typeHasPath(&TypeRef{Kind: kindModel, Model: r}, segs) {
			return true
		}
	}
	return false
}

func (s *Schema) typeHasPath(t *TypeRef, segs []string) bool {
	if len(segs) == 0 || t == nil {
		return true
	}
	switch t.Kind {
	case kindArray:
		return s.typeHasPath(t.Items, segs)
	case kindMap:
		return s.typeHasPath(t.Values, segs[1:]) // first segment is a dynamic key
	case kindModel:
		m := s.lookup(t.Model)
		if m == nil {
			return true
		}
		member, ok := m.Members[segs[0]]
		if !ok {
			return false
		}
		return s.typeHasPath(member, segs[1:])
	case kindAny:
		return true
	default:
		return false
	}
}

// embeddedPaths returns every path inside the root models at which the embedded model `target` is stored,
// e.g. ["recurrence"] for Event or ["helpCenterCategories"] for AppSettings (arrays are transparent,
// maps of models are reported as "name.*").
func (s *Schema) embeddedPaths(roots []string, target string) []string {
	seen := make(map[string]struct{})
	var out []string

	var walkModel func(name, prefix string, stack map[string]bool)
	var walkType func(t *TypeRef, path string, stack map[string]bool)

	walkModel = func(name, prefix string, stack map[string]bool) {
		m := s.lookup(name)
		if m == nil || stack[m.Name] {
			return
		}
		stack[m.Name] = true
		defer delete(stack, m.Name)

		for _, member := range sortedKeys(m.Members) {
			walkType(m.Members[member], joinPath(prefix, member), stack)
		}
	}

	walkType = func(t *TypeRef, path string, stack map[string]bool) {
		if t == nil {
			return
		}
		switch t.Kind {
		case kindModel:
			if strings.EqualFold(t.Model, target) {
				if _, ok := seen[path]; !ok {
					seen[path] = struct{}{}
					out = append(out, path)
				}
			}
			walkModel(t.Model, path, stack)
		case kindArray:
			walkType(t.Items, path, stack)
		case kindMap:
			walkType(t.Values, joinPath(path, "*"), stack)
		}
	}

	for _, r := range roots {
		walkModel(r, "", make(map[string]bool))
	}
	return out
}

// fieldRefs returns pointers to every field path of a migration that is resolved against the document.
// (For a rename, Value is only the new member name and is not a path.)
func fieldRefs(m *Migration) []*string {
	if m.Action == MigrationConditional {
		c := &m.Conditional
		return []*string{&c.Condition.Field, &c.TrueBranch.Field, &c.FalseBranch.Field}
	}
	return []*string{&m.Field}
}

func invalidPathErr(path, reason string) error {
	return gvwErrors.Wrap(gvwErrors.CodeMigrationEngineInvalidFieldPath, fmt.Errorf("field %q: %s", path, reason))
}

// checkConditionalScope makes sure the condition and both branches of a conditional live in the same object.
func checkConditionalScope(m Migration) error {
	if m.Action != MigrationConditional {
		return nil
	}
	c := m.Conditional
	scope := parentPath(c.Condition.Field)
	for _, b := range []Branch{c.TrueBranch, c.FalseBranch} {
		if b.Field != "" && parentPath(b.Field) != scope {
			return gvwErrors.Wrap(gvwErrors.CodeMigrationEngineConditionalScopeMismatch,
				fmt.Errorf("condition %q and branch %q must live in the same object", c.Condition.Field, b.Field))
		}
	}
	return nil
}

// ExpandMigration turns a migration whose fields may start with a model name into concrete migrations:
//   - "Event.recurrence.weekDay"  (root model of this database)  -> "recurrence.weekDay"
//   - "Recurrence.weekDay"        (embedded model)               -> one migration per place the embedded
//     model is used, e.g. "recurrence.weekDay" (or "helpCenterCategories.icon" for HelpCenterCategory)
//   - anything else is a plain path and is returned unchanged.
func (s *Schema) ExpandMigration(m Migration, roots []string) ([]Migration, error) {
	base := m
	embeddedModel := ""
	nonEmpty, embeddedCount := 0, 0

	for _, ref := range fieldRefs(&base) {
		if *ref == "" {
			continue
		}
		nonEmpty++

		segs := splitPath(*ref)
		head := segs[0]
		model, isModel := s.Models[head]
		if !isModel || s.memberOfAny(roots, head) {
			continue // plain path, resolved against the document at runtime
		}

		switch {
		case model.Embedded:
			if embeddedModel != "" && embeddedModel != head {
				return nil, invalidPathErr(*ref, "a migration may only reference one embedded model")
			}
			embeddedModel = head
			embeddedCount++
		case slices.Contains(roots, head):
			if len(segs) == 1 {
				return nil, invalidPathErr(*ref, "a model name needs a field after it")
			}
			*ref = strings.Join(segs[1:], ".")
		default:
			return nil, invalidPathErr(*ref, fmt.Sprintf("model %s is not stored in this database", head))
		}
	}

	if embeddedCount > 0 && embeddedCount != nonEmpty {
		return nil, invalidPathErr(m.Field, "embedded-model paths cannot be mixed with other paths in one migration")
	}

	if embeddedModel == "" {
		if err := checkConditionalScope(base); err != nil {
			return nil, err
		}
		return []Migration{base}, nil
	}

	prefixes := s.embeddedPaths(roots, embeddedModel)
	if len(prefixes) == 0 {
		return nil, gvwErrors.Wrap(gvwErrors.CodeMigrationEngineEmbeddedModelNotUsed,
			fmt.Errorf("embedded model %s is not used by %v", embeddedModel, roots))
	}

	return s.generateExpandMigrationOutput(prefixes, m)
}

func (s *Schema) generateExpandMigrationOutput(prefixes []string, m Migration) ([]Migration, error) {
	out := make([]Migration, 0, len(prefixes))
	for _, prefix := range prefixes {
		c := m
		for _, ref := range fieldRefs(&c) {
			if *ref == "" {
				continue
			}
			segs := splitPath(*ref)
			if len(segs) == 1 {
				*ref = prefix
			} else {
				*ref = prefix + "." + strings.Join(segs[1:], ".")
			}
		}
		if err := checkConditionalScope(c); err != nil {
			return nil, err
		}
		out = append(out, c)
	}
	return out, nil
}

// CheckDocument verifies that a (migrated) document matches the schema of the given root model.
func (s *Schema) CheckDocument(model string, doc map[string]any) error {
	m := s.lookup(model)
	if m == nil {
		return nil
	}
	if err := s.checkObject(m, doc, ""); err != nil {
		return gvwErrors.Wrap(gvwErrors.CodeMigrationEngineSchemaMismatch, fmt.Errorf("model %s: %w", m.Name, err))
	}
	return nil
}

func (s *Schema) checkObject(m *Model, obj map[string]any, path string) error {
	for _, key := range sortedKeys(obj) {
		t, ok := m.Members[key]
		if !ok {
			if strictUnknownMembers && !strings.HasPrefix(key, "_") {
				return fmt.Errorf("unknown member %q", joinPath(path, key))
			}
			continue
		}
		if err := s.checkValue(t, obj[key], joinPath(path, key)); err != nil {
			return err
		}
	}
	return nil
}

// checkValue treats null and missing values as valid; only wrong types are reported.
func (s *Schema) checkValue(t *TypeRef, v any, path string) error {
	if v == nil || t == nil {
		return nil
	}

	switch t.Kind {
	case kindString:
		if _, ok := v.(string); !ok {
			return typeErr(path, "string", v)
		}
	case kindNumber:
		f, ok := toFloat(v)
		if !ok {
			return typeErr(path, "number", v)
		}
		if t.IntOnly && f != math.Trunc(f) {
			return fmt.Errorf("%s: expected a whole number, got %v", path, v)
		}
	case kindBool:
		if _, ok := v.(bool); !ok {
			return typeErr(path, "bool", v)
		}
	case kindModel:
		sub := s.lookup(t.Model)
		if sub == nil {
			return nil
		}
		if sub.IsEnum {
			if _, ok := v.(string); !ok {
				return typeErr(path, "string (enum "+sub.Name+")", v)
			}
			return nil
		}
		obj, ok := v.(map[string]any)
		if !ok {
			return typeErr(path, "object ("+sub.Name+")", v)
		}
		return s.checkObject(sub, obj, path)
	case kindArray:
		arr, ok := v.([]any)
		if !ok {
			return typeErr(path, "array", v)
		}
		for i, el := range arr {
			if err := s.checkValue(t.Items, el, fmt.Sprintf("%s[%d]", path, i)); err != nil {
				return err
			}
		}
	case kindMap:
		obj, ok := v.(map[string]any)
		if !ok {
			return typeErr(path, "object", v)
		}
		for _, k := range sortedKeys(obj) {
			if err := s.checkValue(t.Values, obj[k], joinPath(path, k)); err != nil {
				return err
			}
		}
	}
	return nil
}

func typeErr(path, want string, got any) error {
	return fmt.Errorf("%s: expected %s, got %T", path, want, got)
}

func toFloat(v any) (float64, bool) {
	switch n := v.(type) {
	case float64:
		return n, true
	case float32:
		return float64(n), true
	case int:
		return float64(n), true
	case int64:
		return float64(n), true
	case json.Number:
		f, err := n.Float64()
		return f, err == nil
	}
	return 0, false
}
