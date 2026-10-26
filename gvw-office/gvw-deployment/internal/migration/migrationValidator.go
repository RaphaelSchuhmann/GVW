package migration

import "strings"

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

	switch migration.Action {
	case Add, Delete:
		return migration.Field != ""
	case Rename:
		return migration.Field != "" && migration.Value != ""
	case Conditional:
		return v.isValidConditional(migration.Conditional)
	default:
		return false
	}
}

func (v *MigrationValidator) isValidConditional(conditional ConditionalMigration) bool {
	return v.isValidBranch(conditional.Condition) && v.isValidBranch(conditional.TrueBranch) && v.isValidBranch(conditional.FalseBranch)
}

func (v *MigrationValidator) isValidBranch(branch Branch) bool {
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
        if candidate.Action != Conditional || existing.Action != Conditional {
            return true
        }
    }
    return false
}

func (v *MigrationValidator) extractAllReferencedFields(m Migration) map[string]struct{} {
	fields := make(map[string]struct{})

	if m.Action == Conditional {
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
