package migration

import (
	"context"
	"fmt"
	gvwErrors "gvw/gvw-deployment/internal/utils/errors"
	"reflect"
	"strconv"
	"strings"
)

// TASK: Improve migration validation by returning the id of the bad migration

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

	if migration.Field == "_id" || migration.Field == "_rev" || migration.Value == "_id" || migration.Value == "_rev" {
		return false
	}

	switch migration.Action {
	case MigrationAdd, MigrationDelete:
		return migration.Field != ""
	case MigrationRename:
		return migration.Field != "" && migration.Value != ""
	case MigrationConditional:
		return v.isValidCondition(migration.Conditional.Condition)
	default:
		return false
	}
}

func (v *MigrationValidator) isValidCondition(branch Branch) bool {
	return branch.Field != ""
}

func (v *MigrationValidator) IsMigrationSequenceValid(migrations []Migration) bool {
	if len(migrations) <= 1 {
		return true
	}

	for i, candidate := range migrations {
		if candidate.Database == "" {
			continue
		}

		candidateField := v.extractAllReferencedFields(candidate)
		if len(candidateField) == 0 {
			continue
		}

		if v.hasConflictingPriorMigration(migrations[:i], candidate, candidateField) {
			return false
		}
	}

	return true
}

func (v *MigrationValidator) hasConflictingPriorMigration(priorMigrations []Migration, candidate Migration, candidateFields map[string]struct{}) bool {
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

func (v *MigrationValidator) extractAllReferencedFields(m Migration) map[string]struct{} {
	fields := make(map[string]struct{})

	if m.Action == MigrationConditional {
		cond := m.Conditional
		branches := []Branch{cond.Condition, cond.TrueBranch, cond.FalseBranch}
		for _, b := range branches {
			field := strings.TrimSpace(b.Field)
			if field != "" {
				fields[field] = struct{}{}
			}
		}
		return fields
	}

	field := strings.TrimSpace(m.Field)
	if field != "" {
		fields[field] = struct{}{}
	}

	return fields
}

func (v *MigrationValidator) hasOverlap(a, b map[string]struct{}) bool {
	for field := range a {
		if _, exists := b[field]; exists {
			return true
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
	if _, existed := originalDoc[migration.Field]; existed {
		return fail("add: field %q already existed", migration.Field)
	}
	if val, ok := updatedDoc[migration.Field]; !ok || val != nil {
		return fail("add: field %q should exist with value null", migration.Field)
	}
	return onlyKeysChanged(originalDoc, updatedDoc, migration.Field)
}

func (v *MigrationValidator) validateDeleteAction(originalDoc, updatedDoc map[string]any, migration Migration) error {
	if _, ok := updatedDoc[migration.Field]; ok {
		return fail("delete: field %q still present", migration.Field)
	}
	return onlyKeysChanged(originalDoc, updatedDoc, migration.Field)
}

func (v *MigrationValidator) validateRenameAction(originalDoc, updatedDoc map[string]any, migration Migration) error {
	oldVal, ok := originalDoc[migration.Field]
	if !ok {
		return fail("rename: source field %q missing in original", migration.Field)
	}
	if migration.Field != migration.Value {
		if _, still := updatedDoc[migration.Field]; still {
			return fail("rename: old field %q still present", migration.Field)
		}
	}
	newVal, ok := updatedDoc[migration.Value]
	if !ok {
		return fail("rename: new field %q missing", migration.Value)
	}
	if !reflect.DeepEqual(oldVal, newVal) {
		return fail("rename: value changed while renaming %q to %q", migration.Field, migration.Value)
	}
	return onlyKeysChanged(originalDoc, updatedDoc, migration.Field, migration.Value)
}

func (v *MigrationValidator) validateConditionalAction(originalDoc, updatedDoc map[string]any, migration Migration) error {
	c := migration.Conditional
	allowed := []string{}
	if c.TrueBranch.Field != "" {
		allowed = append(allowed, c.TrueBranch.Field)
	}
	if c.FalseBranch.Field != "" {
		allowed = append(allowed, c.FalseBranch.Field)
	}
	if err := onlyKeysChanged(originalDoc, updatedDoc, allowed...); err != nil {
		return err
	}
	// Any field that did change must hold the value of a branch that targets it.
	for _, k := range allowed {
		if !keyChanged(originalDoc, updatedDoc, k) {
			continue
		}
		got := updatedDoc[k]
		okTrue := k == c.TrueBranch.Field && matchesRaw(got, c.TrueBranch.Value)
		okFalse := k == c.FalseBranch.Field && matchesRaw(got, c.FalseBranch.Value)
		if !okTrue && !okFalse {
			return fail("conditional: field %q has unexpected value %v", k, got)
		}
	}
	return nil
}

func fail(format string, args ...any) error {
	return gvwErrors.Wrap(gvwErrors.CodeMigrationEngineErrorApplicationValidationFailed, fmt.Errorf(format, args...))
}

func keyChanged(before, after map[string]any, k string) bool {
	b, bOk := before[k]
	a, aOk := after[k]
	return bOk != aOk || !reflect.DeepEqual(b, a)
}

func onlyKeysChanged(before, after map[string]any, allowed ...string) error {
	ok := make(map[string]struct{}, len(allowed))
	for _, k := range allowed {
		ok[k] = struct{}{}
	}
	for k := range before {
		if _, a := ok[k]; !a && keyChanged(before, after, k) {
			return fail("unexpected change to field %q", k)
		}
	}
	for k := range after {
		if _, inBefore := before[k]; !inBefore {
			if _, a := ok[k]; !a {
				return fail("unexpected new field %q", k)
			}
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
