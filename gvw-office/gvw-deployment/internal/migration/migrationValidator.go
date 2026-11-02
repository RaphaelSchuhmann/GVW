package migration

import (
	"context"
	"fmt"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"reflect"
	"sort"
	"strconv"
	"strings"
)

type MigrationValidator struct{}

func NewMigrationValidator() *MigrationValidator {
	return &MigrationValidator{}
}

func (v *MigrationValidator) AreMigrationFieldsValid(migrations []Migration) bool {
	for _, migration := range migrations {
		if !v.isMigrationValid(migration) {
			return false
		}
	}

	return true
}

func (v *MigrationValidator) isMigrationValid(migration Migration) bool {
	if migration.Action == "" {
		return false
	}

	switch migration.Action {
	case MigrationAdd, MigrationDelete:
		return isValidFieldPath(migration.Field)
	case MigrationRename:
		return isValidFieldPath(migration.Field) && isValidMemberName(migration.Value)
	case MigrationConditional:
		c := migration.Conditional
		if !v.isValidCondition(c.Condition) {
			return false
		}
		for _, b := range []Branch{c.TrueBranch, c.FalseBranch} {
			if b.Field != "" && !isValidFieldPath(b.Field) {
				return false
			}
		}
		return true
	default:
		return false
	}
}

func (v *MigrationValidator) isValidCondition(branch Branch) bool {
	return isValidReadPath(branch.Field)
}

// isValidReadPath checks the syntax of a path that is only read: no empty or padded segments.
func isValidReadPath(p string) bool {
	if strings.TrimSpace(p) == "" {
		return false
	}
	for _, seg := range splitPath(p) {
		if seg == "" || seg != strings.TrimSpace(seg) {
			return false
		}
	}
	return true
}

// isValidFieldPath checks a path that is written to: additionally the leaf may not be "*", "_id" or "_rev".
func isValidFieldPath(p string) bool {
	if !isValidReadPath(p) {
		return false
	}
	leaf := leafOf(p)
	return leaf != "*" && leaf != "_id" && leaf != "_rev"
}

// isValidMemberName checks the new name of a renamed member (a single name, not a path).
func isValidMemberName(n string) bool {
	return n != "" && n == strings.TrimSpace(n) && !strings.Contains(n, ".") && n != "*" && n != "_id" && n != "_rev"
}

// ValidateMigrationAgainstSchema checks a concrete (already expanded) migration against the latest models.
// It does nothing when the models of the database are unknown.
func (v *MigrationValidator) ValidateMigrationAgainstSchema(migration Migration, roots []string, schema *Schema) error {
	if schema == nil || len(roots) == 0 {
		return nil
	}

	switch migration.Action {
	case MigrationAdd:
		if !schema.PathExists(roots, migration.Field) {
			return notInSchema("add: field %q does not exist in the models of this database", migration.Field)
		}
	case MigrationRename:
		target := siblingPath(migration.Field, migration.Value)
		if !schema.PathExists(roots, target) {
			return notInSchema("rename: target field %q does not exist in the models of this database", target)
		}
	case MigrationConditional:
		c := migration.Conditional
		for _, b := range []Branch{c.TrueBranch, c.FalseBranch} {
			if b.Field != "" && !schema.PathExists(roots, b.Field) {
				return notInSchema("conditional: field %q does not exist in the models of this database", b.Field)
			}
		}
	}

	return nil
}

func notInSchema(format string, args ...any) error {
	return gvwErrors.Wrap(gvwErrors.CodeMigrationEngineFieldNotInSchema, fmt.Errorf(format, args...))
}

func (v *MigrationValidator) IsMigrationSequenceValid(migrations []Migration) bool {
	if len(migrations) <= 1 {
		return true
	}

	for i, candidate := range migrations {
		if candidate.Database == "" {
			continue
		}

		candidateFields := v.extractAllReferencedFields(candidate)
		if len(candidateFields) == 0 {
			continue
		}

		if v.hasConflictingPriorMigration(migrations[:i], candidate, candidateFields) {
			return false
		}
	}

	return true
}

func (v *MigrationValidator) hasConflictingPriorMigration(priorMigrations []Migration, candidate Migration, candidateFields []string) bool {
	for _, existing := range priorMigrations {
		if !strings.EqualFold(candidate.Database, existing.Database) {
			continue
		}

		existingFields := v.extractAllReferencedFields(existing)
		if !v.hasOverlap(candidateFields, existingFields) {
			continue
		}

		// A conflict occurs if they overlap on the same database/fields
		// but both actions are not Conditional.
		if candidate.Action != MigrationConditional || existing.Action != MigrationConditional {
			return true
		}
	}
	return false
}

func (v *MigrationValidator) extractAllReferencedFields(m Migration) []string {
	var fields []string
	add := func(p string) {
		p = strings.TrimSpace(p)
		if p != "" {
			fields = append(fields, p)
		}
	}

	switch m.Action {
	case MigrationConditional:
		c := m.Conditional
		add(c.Condition.Field)
		add(c.TrueBranch.Field)
		add(c.FalseBranch.Field)
	case MigrationRename:
		add(m.Field)
		if f, val := strings.TrimSpace(m.Field), strings.TrimSpace(m.Value); f != "" && val != "" {
			add(siblingPath(f, val))
		}
	default:
		add(m.Field)
	}

	return fields
}

// hasOverlap treats a path and everything below it as overlapping ("recurrence" vs "recurrence.weekDay").
func (v *MigrationValidator) hasOverlap(a, b []string) bool {
	for _, x := range a {
		for _, y := range b {
			if pathsOverlap(x, y) {
				return true
			}
		}
	}
	return false
}

func (v *MigrationValidator) AllValuesPresent(modelToDbMap map[string]string, items []string) bool {
	values := make(map[string]struct{}, len(modelToDbMap))
	for _, val := range modelToDbMap {
		values[val] = struct{}{}
	}
	for _, s := range items {
		if _, ok := values[s]; !ok {
			return false
		}
	}
	return true
}

func (v *MigrationValidator) ValidateMigrationApplication(ctx context.Context, originalDoc, updatedDoc map[string]any, migration Migration) error {
	if ctx.Err() != nil {
		return gvwErrors.NewError(gvwErrors.CodeCancelledError)
	}

	switch migration.Action {
	case MigrationAdd:
		return v.validateAddAction(originalDoc, updatedDoc, migration)
	case MigrationDelete:
		return v.validateDeleteAction(originalDoc, updatedDoc, migration)
	case MigrationRename:
		return v.validateRenameAction(originalDoc, updatedDoc, migration)
	case MigrationConditional:
		return v.validateConditionalAction(originalDoc, updatedDoc, migration)
	default:
		return fail("unknown action %v", migration.Action)
	}
}

func (v *MigrationValidator) validateAddAction(originalDoc, updatedDoc map[string]any, migration Migration) error {
	before, leaf := resolvePath(originalDoc, migration.Field)
	after, _ := resolvePath(updatedDoc, migration.Field)
	if len(before) != len(after) {
		return fail("add: document structure changed around %q", migration.Field)
	}

	for _, p := range before {
		if _, existed := p[leaf]; existed {
			return fail("add: field %q already existed", migration.Field)
		}
	}
	for _, p := range after {
		if val, ok := p[leaf]; !ok || val != nil {
			return fail("add: field %q should exist with value null", migration.Field)
		}
	}
	return onlyPathsChanged(originalDoc, updatedDoc, migration.Field)
}

func (v *MigrationValidator) validateDeleteAction(originalDoc, updatedDoc map[string]any, migration Migration) error {
	before, leaf := resolvePath(originalDoc, migration.Field)
	after, _ := resolvePath(updatedDoc, migration.Field)
	if len(before) != len(after) {
		return fail("delete: document structure changed around %q", migration.Field)
	}

	for _, p := range after {
		if _, ok := p[leaf]; ok {
			return fail("delete: field %q still present", migration.Field)
		}
	}
	return onlyPathsChanged(originalDoc, updatedDoc, migration.Field)
}

func (v *MigrationValidator) validateRenameAction(originalDoc, updatedDoc map[string]any, migration Migration) error {
	before, leaf := resolvePath(originalDoc, migration.Field)
	after, _ := resolvePath(updatedDoc, migration.Field)
	if len(before) != len(after) {
		return fail("rename: document structure changed around %q", migration.Field)
	}

	for i := range before {
		oldVal, ok := before[i][leaf]
		if !ok {
			return fail("rename: source field %q missing in original", migration.Field)
		}
		if leaf != migration.Value {
			if _, still := after[i][leaf]; still {
				return fail("rename: old field %q still present", migration.Field)
			}
		}
		newVal, ok := after[i][migration.Value]
		if !ok {
			return fail("rename: new field %q missing", siblingPath(migration.Field, migration.Value))
		}
		if !reflect.DeepEqual(oldVal, newVal) {
			return fail("rename: value changed while renaming %q to %q", migration.Field, migration.Value)
		}
	}
	return onlyPathsChanged(originalDoc, updatedDoc, migration.Field, siblingPath(migration.Field, migration.Value))
}

func (v *MigrationValidator) validateConditionalAction(originalDoc, updatedDoc map[string]any, migration Migration) error {
	c := migration.Conditional
	allowed := v.getAllowedFields(c)

	if err := onlyPathsChanged(originalDoc, updatedDoc, allowed...); err != nil {
		return err
	}

	for _, field := range allowed {
		if err := v.validateFieldChanges(originalDoc, updatedDoc, field, c); err != nil {
			return err
		}
	}
	return nil
}

func (v *MigrationValidator) getAllowedFields(c ConditionalMigration) []string {
	var allowed []string
	if c.TrueBranch.Field != "" {
		allowed = append(allowed, c.TrueBranch.Field)
	}
	if c.FalseBranch.Field != "" && c.FalseBranch.Field != c.TrueBranch.Field {
		allowed = append(allowed, c.FalseBranch.Field)
	}
	return allowed
}

func (v *MigrationValidator) validateFieldChanges(originalDoc, updatedDoc map[string]any, field string, c ConditionalMigration) error {
	before, leaf := resolvePath(originalDoc, field)
	after, _ := resolvePath(updatedDoc, field)

	if len(before) != len(after) {
		return fail("conditional: document structure changed around %q", field)
	}

	for i := range before {
		if !keyChanged(before[i], after[i], leaf) {
			continue
		}
		got := after[i][leaf]
		if !v.matchesBranchCondition(field, got, c) {
			return fail("conditional: field %q has unexpected value %v", field, got)
		}
	}
	return nil
}

func (v *MigrationValidator) matchesBranchCondition(field string, got any, c ConditionalMigration) bool {
	okTrue := field == c.TrueBranch.Field && matchesRaw(got, c.TrueBranch.Value)
	okFalse := field == c.FalseBranch.Field && matchesRaw(got, c.FalseBranch.Value)
	return okTrue || okFalse
}

func fail(format string, args ...any) error {
	return gvwErrors.Wrap(gvwErrors.CodeMigrationEngineErrorApplicationValidationFailed, fmt.Errorf(format, args...))
}

func keyChanged(before, after map[string]any, k string) bool {
	b, bOk := before[k]
	a, aOk := after[k]
	return bOk != aOk || !reflect.DeepEqual(b, a)
}

// onlyPathsChanged fails if anything outside the allowed paths (or below them) differs between the two documents.
func onlyPathsChanged(before, after map[string]any, allowed ...string) error {
	b, a := flattenDoc(before), flattenDoc(after)

	seen := make(map[string]struct{}, len(b)+len(a))
	paths := make([]string, 0, len(b)+len(a))
	for p := range b {
		seen[p] = struct{}{}
		paths = append(paths, p)
	}
	for p := range a {
		if _, ok := seen[p]; !ok {
			paths = append(paths, p)
		}
	}
	sort.Strings(paths)

	for _, p := range paths {
		if reflect.DeepEqual(b[p], a[p]) {
			continue
		}
		if !pathAllowed(p, allowed) {
			return fail("unexpected change at %q", p)
		}
	}
	return nil
}

func matchesRaw(val any, raw string) bool {
	switch v := val.(type) {
	case string:
		return v == raw
	case float64:
		f, err := strconv.ParseFloat(raw, 64)
		return err == nil && f == v
	case bool:
		b, err := strconv.ParseBool(raw)
		return err == nil && b == v
	case nil:
		return raw == ""
	}
	return false
}
