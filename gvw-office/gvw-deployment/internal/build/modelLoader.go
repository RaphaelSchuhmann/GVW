package build

import (
	"fmt"
	"gvw/gvw-deployment/internal/utils"
	"strings"
)

type ModelLoader struct {
	logger    *utils.Logger
	fsWrapper *utils.FileSytemWrapper
}

type ClassScope struct {
	name       string
	startDepth int
}

func NewModelLoader(logger *utils.Logger, fsWrapper *utils.FileSytemWrapper) *ModelLoader {
	return &ModelLoader{
		logger:    logger,
		fsWrapper: fsWrapper,
	}
}

func (m *ModelLoader) LoadModel(path string) (map[string]any, error) {
	if path == "" || !m.fsWrapper.DirOrFileExists(path) {
		return nil, fmt.Errorf("INVALID_PATH")
	}

	data, err := m.fsWrapper.ReadFile(path)
	if err != nil {
		return nil, err
	}

	modelString := string(data)

	var modelLines []string = strings.Split(modelString, "\n")

	return m.ParseLines(modelLines)
}

func (m *ModelLoader) ParseLines(lines []string) (map[string]any, error) {
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
			if char == '{' {
				braceDepth++
			} else if char == '}' {
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

			name := m.ExtractClassName(line)
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
			if strings.Contains(line, "(") || strings.Contains(line, "return") || strings.HasPrefix(line, "@") {
				continue
			}

			line = strings.TrimSuffix(line, ";")
			parts := strings.Fields(line)

			var cleanParts []string
			for _, part := range parts {
				switch part {
				case "private", "public", "protected", "static", "final", "transient", "volatile":
					continue
				default:
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

	return result, nil
}

func (m *ModelLoader) ExtractClassName(line string) string {
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
