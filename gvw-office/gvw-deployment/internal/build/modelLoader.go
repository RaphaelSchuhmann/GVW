package build

import (
	"gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
	"slices"
	"strings"
)

type ModelLoader struct {
	logger    *log.Logger
	fsWrapper filesystem.FileSystemWrapper
}

type ClassScope struct {
	name       string
	startDepth int
}

func NewModelLoader(logger *log.Logger, fsWrapper filesystem.FileSystemWrapper) *ModelLoader {
	return &ModelLoader{
		logger:    logger,
		fsWrapper: fsWrapper,
	}
}

func (m *ModelLoader) LoadModel(path string) (map[string]any, error) {
	if path == "" || !m.fsWrapper.DirOrFileExists(path) {
		return nil, gvwErrors.NewError("ModelLoader_Invalid_Path")
	}

	data, err := m.fsWrapper.ReadFile(path)
	if err != nil {
		return nil, gvwErrors.Wrap("ModelLoder_Failed_Reading_File", err)
	}

	modelString := string(data)

	var modelLines []string = strings.Split(modelString, "\n")

	return m.parseLines(modelLines), nil
}

func (m *ModelLoader) parseLines(lines []string) map[string]any {
	result := make(map[string]any)
	var stack []ClassScope
	braceDepth := 0
	var pendingJsonProperty string

	for _, rawLine := range lines {
		line := strings.TrimSpace(rawLine)

		if m.shouldSkipLine(line) {
			continue
		}

		braceDepth = m.updateBraceDepth(line, braceDepth, &stack)
		currentClass := m.getCurrentClass(stack)
		classStartDepth := 0
		if len(stack) > 0 {
			classStartDepth = stack[len(stack)-1].startDepth
		}

		if m.processJsonProperty(line, &pendingJsonProperty) {
			continue
		}

		if m.processClassOrEnumDeclaration(line, braceDepth, &stack, result) {
			continue
		}

		m.processFieldDeclaration(
			line,
			currentClass,
			braceDepth,
			classStartDepth,
			&pendingJsonProperty,
			result,
		)
	}

	return result
}

func (m *ModelLoader) shouldSkipLine(line string) bool {
	return line == "" || line == "*/" || strings.HasPrefix(line, "//") ||
		strings.HasPrefix(line, "/*") || strings.HasPrefix(line, "*") || strings.HasSuffix(line, "*/")
}

func (m *ModelLoader) updateBraceDepth(line string, braceDepth int, stack *[]ClassScope) int {
	for _, char := range line {
		switch char {
		case '{':
			braceDepth++
		case '}':
			braceDepth--
			if len(*stack) > 0 && braceDepth < (*stack)[len(*stack)-1].startDepth {
				*stack = (*stack)[:len(*stack)-1]
			}
		}
	}
	return braceDepth
}

func (m *ModelLoader) getCurrentClass(stack []ClassScope) string {
	if len(stack) > 0 {
		return stack[len(stack)-1].name
	}
	return ""
}

func (m *ModelLoader) processJsonProperty(line string, pendingJsonProperty *string) bool {
	if strings.Contains(line, "@JsonProperty") {
		if start := strings.Index(line, "\""); start != -1 {
			if end := strings.LastIndex(line, "\""); end != -1 && end > start {
				*pendingJsonProperty = line[start+1 : end]
			}
		}
		return true
	}
	return false
}

func (m *ModelLoader) processClassOrEnumDeclaration(line string, braceDepth int, stack *[]ClassScope, result map[string]any) bool {
	if strings.Contains(line, " class ") || strings.Contains(line, " enum ") ||
		strings.HasPrefix(line, "public class") || strings.HasPrefix(line, "public static class") ||
		strings.HasPrefix(line, "class ") || strings.HasPrefix(line, "enum ") {

		classType := "class"
		if strings.Contains(line, " enum ") || strings.HasPrefix(line, "enum ") {
			classType = "enum"
		}

		name := m.extractClassNameFromLine(line)
		if name != "" {
			if classType == "enum" {
				result[name] = map[string]any{"objType": classType}
			} else {
				result[name] = map[string]any{
					"objType": classType,
					"members": make(map[string]any),
				}
			}

			*stack = append(*stack, ClassScope{
				name:       name,
				startDepth: braceDepth,
			})
		}
		return true
	}
	return false
}

func (m *ModelLoader) processFieldDeclaration(
	line string,
	currentClass string,
	braceDepth int,
	classStartDepth int,
	pendingJsonProperty *string,
	result map[string]any,
) {
	if currentClass == "" || braceDepth != classStartDepth || !strings.HasSuffix(line, ";") {
		return
	}

	if strings.Contains(line, "{") {
		return
	}

	line = strings.TrimSuffix(line, ";")
	line = strings.ReplaceAll(line, ", ", ",")
	parts := strings.Fields(line)

	if slices.Contains(parts, "static") {
		return
	}

	cleanParts := m.getCleanParts(parts)

	if len(cleanParts) >= 2 {
		fieldType := cleanParts[0]
		fieldName := cleanParts[1]

		keyName := fieldName
		if *pendingJsonProperty != "" {
			keyName = *pendingJsonProperty
			*pendingJsonProperty = ""
		}

		if classMap, ok := result[currentClass].(map[string]any); ok {
			if classMap["objType"] == "enum" {
				return
			}

			if members, ok := classMap["members"].(map[string]any); ok {
				members[keyName] = fieldType
			}
		}
	}
}

func (m *ModelLoader) getCleanParts(parts []string) []string {
	var cleanParts []string
	for _, part := range parts {
		switch part {
		case "private", "public", "protected", "static", "final", "transient", "volatile":
			continue
		default:
			if strings.HasPrefix(part, "@") {
				continue
			}
			cleanParts = append(cleanParts, part)
		}
	}

	return cleanParts
}

func (m *ModelLoader) extractClassNameFromLine(line string) string {
	var firstIndex int
	if strings.Contains(line, "enum") {
		firstIndex = strings.Index(line, "enum") + 4
	} else {
		firstIndex = strings.Index(line, "class") + 5
	}

	sub := strings.TrimSpace(line[firstIndex:])

	parts := strings.Fields(sub)
	if len(parts) > 0 {
		return strings.TrimSuffix(parts[0], "{")
	}

	return ""
}

func (m *ModelLoader) NormalizeAndValidateSchema(rawSchema map[string]any) (map[string]any, error) {
	classes, enums := m.extractClassesAndEnums(rawSchema)
	normalizedSchema := make(map[string]any)

	for className, val := range rawSchema {
		if enums[className] {
			continue
		}

		classMap, ok := val.(map[string]any)
		if !ok {
			m.logger.Printf("[BUILD] Invalid class structure for class %s detected", className)
			return nil, gvwErrors.NewError("ModelLoder_Invalid_Class_Structure")
		}

		objType, _ := classMap["objType"].(string)
		members, _ := classMap["members"].(map[string]any)

		newMembers, err := m.normalizeMembers(className, members, classes, enums)
		if err != nil {
			return nil, err
		}

		normalizedSchema[className] = map[string]any{
			"objType": objType,
			"members": newMembers,
		}
	}

	return normalizedSchema, nil
}

func (m *ModelLoader) extractClassesAndEnums(rawSchema map[string]any) (map[string]bool, map[string]bool) {
	classes := make(map[string]bool)
	enums := make(map[string]bool)

	for name, val := range rawSchema {
		if classMap, ok := val.(map[string]any); ok {
			if objType, ok := classMap["objType"].(string); ok {
				if objType == "class" {
					classes[name] = true
				} else {
					enums[name] = true
				}
			}
		}
	}
	return classes, enums
}

func (m *ModelLoader) normalizeMembers(className string, members map[string]any, classes, enums map[string]bool) (map[string]any, error) {
	newMembers := make(map[string]any)
	for fieldName, fieldTypeRaw := range members {
		fieldType, ok := fieldTypeRaw.(string)
		if !ok {
			m.logger.Printf("[BUILD] Invalid type representation for class %s, field %s detected", className, fieldName)
			return nil, gvwErrors.NewError("ModelLoader_Invalid_Type_Representation")
		}

		normType, err := normalizeType(fieldType, classes, enums)
		if err != nil {
			m.logger.Printf("[BUILD] Error normalizing type for class %s, field %s: %v", className, fieldName, err)
			return nil, gvwErrors.NewError("ModelLoader_Normalize_Type")
		}

		newMembers[fieldName] = normType
	}
	return newMembers, nil
}

func normalizeType(typeName string, classes map[string]bool, enums map[string]bool) (any, error) {
	typeName = strings.TrimSpace(typeName)

	// Primitive and Wrapper Mappings
	switch typeName {
	case "String", "char", "Character", "Instant", "LocalDate", "LocalDateTime", "LocalTime":
		return "string", nil
	case "byte", "Byte", "short", "Short", "int", "Integer", "long", "Long", "BigInteger":
		return "int", nil
	case "float", "Float", "double", "Double", "BigDecimal":
		return "float", nil
	case "boolean", "Boolean":
		return "bool", nil
	}

	// Resolve enums to "string"
	if enums[typeName] {
		return "string", nil
	}

	// Known model classes resolve to their model reference name
	if classes[typeName] {
		return typeName, nil
	}

	// Collections: List<T>, Set<T>, or arrays T[]
	if (strings.HasPrefix(typeName, "List<") && strings.HasSuffix(typeName, ">")) ||
		(strings.HasPrefix(typeName, "Set<") && strings.HasSuffix(typeName, ">")) {
		inner := typeName[strings.Index(typeName, "<")+1 : len(typeName)-1]
		normInner, err := normalizeType(inner, classes, enums)
		if err != nil {
			return nil, gvwErrors.NewError("ModelLoder_Normalizing_Collections")
		}
		return map[string]any{
			"type":  "array",
			"items": normInner,
		}, nil
	}

	if strings.HasSuffix(typeName, "[]") {
		inner := strings.TrimSuffix(typeName, "[]")
		normInner, err := normalizeType(inner, classes, enums)
		if err != nil {
			return nil, gvwErrors.NewError("ModelLoder_Normalizing_Arrays")
		}
		return map[string]any{
			"type":  "array",
			"items": normInner,
		}, nil
	}

	if strings.HasPrefix(typeName, "Map<") && strings.HasSuffix(typeName, ">") {
		inner := typeName[4 : len(typeName)-1]
		parts := strings.SplitN(inner, ",", 2)
		if len(parts) != 2 {
			// Unlikely to happen as this would be a java error
			// "How did we get here"
			return nil, gvwErrors.NewError("ModelLoder_Invalid_Map_Format")
		}

		valType := strings.TrimSpace(parts[1])

		normVal, err := normalizeType(valType, classes, enums)
		if err != nil {
			return nil, gvwErrors.NewError("ModelLoder_Normalize_Map")
		}

		return map[string]any{
			"type":   "object",
			"values": normVal,
		}, nil
	}

	return nil, gvwErrors.NewError("ModelLoder_Unsupported_Type")
}

func (m *ModelLoader) MarkEmbeddedClasses(schema map[string]any) {
	referenced := m.collectReferencedClasses(schema)

	for className, val := range schema {
		classMap, ok := val.(map[string]any)
		if !ok {
			continue
		}
		if objType, ok := classMap["objType"].(string); ok && objType == "class" {
			classMap["embedded"] = referenced[className]
		}
	}
}

func (m *ModelLoader) collectReferencedClasses(schema map[string]any) map[string]bool {
	classNames := m.extractClassNamesFromSchema(schema)
	referenced := make(map[string]bool)

	for _, val := range schema {
		classMap, ok := val.(map[string]any)
		if !ok {
			continue
		}
		members, ok := classMap["members"].(map[string]any)
		if !ok {
			continue
		}

		for _, fieldType := range members {
			m.collectReferences(fieldType, referenced, classNames)
		}
	}
	return referenced
}

func (m *ModelLoader) extractClassNamesFromSchema(schema map[string]any) map[string]bool {
	classNames := make(map[string]bool)
	for className, val := range schema {
		if classMap, ok := val.(map[string]any); ok {
			if objType, ok := classMap["objType"].(string); ok && objType == "class" {
				classNames[className] = true
			}
		}
	}
	return classNames
}

func (m *ModelLoader) collectReferences(fieldType any, referenced map[string]bool, classNames map[string]bool) {
	switch t := fieldType.(type) {
	case string:
		if classNames[t] {
			referenced[t] = true
		}
	case map[string]any:
		if typeVal, ok := t["type"].(string); ok {
			switch typeVal {
			case "array":
				m.collectReferences(t["items"], referenced, classNames)
			case "object":
				m.collectReferences(t["values"], referenced, classNames)
			}
		}
	}
}
