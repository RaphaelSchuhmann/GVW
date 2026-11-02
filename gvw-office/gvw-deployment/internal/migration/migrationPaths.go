package migration

import (
	"sort"
	"strings"
)

// Field paths use dots to descend into nested objects: "recurrence.weekDay".
// Arrays are transparent (the rest of the path is applied to every element)
// and "*" matches every value of a map with dynamic keys: "feedbackCategories.*".

func splitPath(path string) []string {
	return strings.Split(path, ".")
}

// parentPath returns everything before the last segment ("" for top-level fields).
func parentPath(path string) string {
	i := strings.LastIndex(path, ".")
	if i < 0 {
		return ""
	}
	return path[:i]
}

// leafOf returns the last segment of a path.
func leafOf(path string) string {
	return path[strings.LastIndex(path, ".")+1:]
}

func joinPath(parent, name string) string {
	if parent == "" {
		return name
	}
	return parent + "." + name
}

// siblingPath replaces the last segment of path with newLeaf.
func siblingPath(path, newLeaf string) string {
	return joinPath(parentPath(path), newLeaf)
}

func sortedKeys[V any](m map[string]V) []string {
	keys := make([]string, 0, len(m))
	for k := range m {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	return keys
}

// asMaps flattens a value into the objects it contains (a map is itself, an array
// is every object element, anything else yields nothing).
func asMaps(v any) []map[string]any {
	switch t := v.(type) {
	case map[string]any:
		return []map[string]any{t}
	case []any:
		var out []map[string]any
		for _, el := range t {
			out = append(out, asMaps(el)...)
		}
		return out
	}
	return nil
}

// resolveParents walks the given segments from root and returns every object reached.
// Missing or null intermediate values simply produce no results. The order is
// deterministic, so resolving the same path on a document and on its clone lines up.
func resolveParents(root map[string]any, segments []string) []map[string]any {
	current := []map[string]any{root}
	for _, seg := range segments {
		var next []map[string]any
		for _, m := range current {
			if seg == "*" {
				for _, k := range sortedKeys(m) {
					next = append(next, asMaps(m[k])...)
				}
				continue
			}
			if child, ok := m[seg]; ok {
				next = append(next, asMaps(child)...)
			}
		}
		current = next
	}
	return current
}

// resolvePath returns every object that directly contains the leaf of path, plus the leaf name.
func resolvePath(doc map[string]any, path string) ([]map[string]any, string) {
	segs := splitPath(path)
	return resolveParents(doc, segs[:len(segs)-1]), segs[len(segs)-1]
}

func deepCloneMap(m map[string]any) map[string]any {
	if m == nil {
		return nil
	}
	out := make(map[string]any, len(m))
	for k, v := range m {
		out[k] = deepCloneValue(v)
	}
	return out
}

func deepCloneValue(v any) any {
	switch t := v.(type) {
	case map[string]any:
		return deepCloneMap(t)
	case []any:
		out := make([]any, len(t))
		for i := range t {
			out[i] = deepCloneValue(t[i])
		}
		return out
	}
	return v
}

// flattenDoc maps every leaf path of a document to the values found there.
// Arrays are transparent, so "helpCenterCategories.icon" holds one value per element.
func flattenDoc(doc map[string]any) map[string][]any {
	out := make(map[string][]any)
	collectLeaves("", doc, out)
	return out
}

func collectLeaves(prefix string, v any, out map[string][]any) {
	switch t := v.(type) {
	case map[string]any:
		if len(t) == 0 {
			out[prefix] = append(out[prefix], t)
			return
		}
		for _, k := range sortedKeys(t) {
			collectLeaves(joinPath(prefix, k), t[k], out)
		}
	case []any:
		if len(t) == 0 {
			out[prefix] = append(out[prefix], t)
			return
		}
		for _, el := range t {
			collectLeaves(prefix, el, out)
		}
	default:
		out[prefix] = append(out[prefix], v)
	}
}

// pathAllowed reports whether p equals, or lies below, one of the allowed patterns ("*" matches any segment).
func pathAllowed(p string, patterns []string) bool {
	ps := splitPath(p)
	for _, pat := range patterns {
		pp := splitPath(pat)
		if len(pp) > len(ps) {
			continue
		}
		match := true
		for i := range pp {
			if pp[i] != "*" && pp[i] != ps[i] {
				match = false
				break
			}
		}
		if match {
			return true
		}
	}
	return false
}

// pathsOverlap reports whether two paths are equal or one lies below the other.
func pathsOverlap(a, b string) bool {
	as, bs := splitPath(a), splitPath(b)
	n := min(len(as), len(bs))
	for i := 0; i < n; i++ {
		if as[i] != bs[i] && as[i] != "*" && bs[i] != "*" {
			return false
		}
	}
	return true
}
