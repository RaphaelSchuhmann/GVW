package build

import (
	"gvw/gvw-deployment/internal/utils/errors"
	"gvw/gvw-deployment/internal/utils/filesystem"
	"gvw/gvw-deployment/internal/utils/log"
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
		return nil, errors.NewError("ModelLoader_Invalid_Path")
	}

	data, err := m.fsWrapper.ReadFile(path)
	if err != nil {
		return nil, errors.Wrap("ModelLoder_Failed_Reading_File", err)
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

		if line == "" || line == "*/" || strings.HasPrefix(line, "//") || strings.HasPrefix(line, "/*") || strings.HasPrefix(line, "*") || strings.HasSuffix(line, "*/") {
			continue
		}

		for _, char := range line {
			switch char {
			case '{':
				braceDepth++
			case '}':
				braceDepth--

				if len(stack) > 0 && braceDepth < stack[len(stack)-1].startDepth {
					stack = stack[:len(stack)-1]
				}
			}
		}

		var currentClass string
		if len(stack) > 0 {
			currentClass = stack[len(stack)-1].name
		}

		// Capture @JsonProperty overrides
		if strings.Contains(line, "@JsonProperty") {
			if start := strings.Index(line, "\""); start != -1 {
				if end := strings.LastIndex(line, "\""); end != -1 && end > start {
					pendingJsonProperty = line[start+1 : end]
				}
			}
			continue
		}

		// Check for class or enum declarations
		if strings.Contains(line, " class ") || strings.Contains(line, " enum ") ||
			strings.HasPrefix(line, "public class") || strings.HasPrefix(line, "public static class") ||
			strings.HasPrefix(line, "class ") || strings.HasPrefix(line, "enum ") {

			classType := "class"
			if strings.Contains(line, " enum ") || strings.HasPrefix(line, "enum ") {
				classType = "enum"
			}

			name := extractClassName(line)
			if name != "" {
				if classType == "enum" {
					result[name] = map[string]any{
						"objType": classType,
					}
				} else {
					result[name] = map[string]any{
						"objType": classType,
						"members": make(map[string]any),
					}
				}

				stack = append(stack, ClassScope{
					name:       name,
					startDepth: braceDepth,
				})
			}
			continue
		}

		// Parse field declarations inside the active class scope
		if currentClass != "" && strings.HasSuffix(line, ";") {
			if strings.Contains(line, "{") || strings.Contains(line, "return") || strings.Contains(line, "this") {
				continue
			}

			line = strings.TrimSuffix(line, ";")
			line = fixGenericSpacing(line)
			parts := strings.Fields(line)

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

			if len(cleanParts) >= 2 {
				fieldType := cleanParts[0]
				fieldName := cleanParts[1]

				keyName := fieldName
				if pendingJsonProperty != "" {
					keyName = pendingJsonProperty
					pendingJsonProperty = ""
				}

				if classMap, ok := result[currentClass].(map[string]any); ok {
					if classMap["objType"] == "enum" {
						continue
					}

					if members, ok := classMap["members"].(map[string]any); ok {
						members[keyName] = fieldType
					}
				}
			}
		}
	}

	return result
}

func (m *ModelLoader) NormalizeAndValidateSchema(rawSchema map[string]any) (map[string]any, error) {
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

	normalizedSchema := make(map[string]any)

	for className, val := range rawSchema {
		if enums[className] {
			continue
		}

		classMap, ok := val.(map[string]any)
		if !ok {
			m.logger.Printf("[BUILD] Invalid class structure for class %s detected", className)
			return nil, errors.NewError("ModelLoder_Invalid_Class_Structure")
		}

		objType, _ := classMap["objType"].(string)
		members, _ := classMap["members"].(map[string]any)

		newMembers := make(map[string]any)
		for fieldName, fieldTypeRaw := range members {
			fieldType, ok := fieldTypeRaw.(string)
			if !ok {
				m.logger.Printf("[BUILD] Invalid type representation for class %s, field %s detected", className, fieldName)
				return nil, errors.NewError("ModelLoader_Invalid_Type_Representation")
			}

			normType, err := normalizeType(fieldType, classes, enums)
			if err != nil {
				m.logger.Printf("[BUILD] Error normalizing type for class %s, field %s: %v", className, fieldName, err)
				return nil, errors.NewError("ModelLoader_Normalize_Type")
			}

			newMembers[fieldName] = normType
		}

		normalizedSchema[className] = map[string]any{
			"objType": objType,
			"members": newMembers,
		}
	}

	return normalizedSchema, nil
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
			return nil, errors.NewError("ModelLoder_Normalizing_Collections")
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
			return nil, errors.NewError("ModelLoder_Normalizing_Arrays")
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
			return nil, errors.NewError("ModelLoder_Invalid_Map_Format")
		}

		valType := strings.TrimSpace(parts[1])

		normVal, err := normalizeType(valType, classes, enums)
		if err != nil {
			return nil, errors.NewError("ModelLoder_Normalize_Map")
		}

		return map[string]any{
			"type":   "object",
			"values": normVal,
		}, nil
	}

	return nil, errors.NewError("ModelLoder_Unsupported_Type")
}

func (m *ModelLoader) MarkEmbeddedClasses(schema map[string]any) {
	classNames := make(map[string]bool)
	for className, val := range schema {
		if classMap, ok := val.(map[string]any); ok {
			if objType, ok := classMap["objType"].(string); ok && objType == "class" {
				classNames[className] = true
			}
		}
	}

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
			collectReferences(fieldType, referenced, classNames)
		}
	}

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

func extractClassName(line string) string {
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

func fixGenericSpacing(s string) string {
	return strings.ReplaceAll(s, ", ", ",")
}

func collectReferences(fieldType any, referenced map[string]bool, classNames map[string]bool) {
	switch t := fieldType.(type) {
	case string:
		if classNames[t] {
			referenced[t] = true
		}
	case map[string]any:
		if typeVal, ok := t["type"].(string); ok {
			if typeVal == "array" {
				collectReferences(t["items"], referenced, classNames)
			} else if typeVal == "object" {
				collectReferences(t["values"], referenced, classNames)
			}
		}
	}
}
