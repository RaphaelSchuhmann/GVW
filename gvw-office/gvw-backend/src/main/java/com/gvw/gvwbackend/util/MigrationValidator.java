package com.gvw.gvwbackend.util;

import com.gvw.gvwbackend.model.Migration;
import com.gvw.gvwbackend.model.MigrationAction;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class MigrationValidator {
  public boolean areMigrationFieldsValid(
      MigrationAction action, String field, String value, Migration.Conditional conditional) {
    if (action == null) {
      return false;
    }

    return switch (action) {
      case ADD, DELETE -> isNonBlank(field);
      case RENAME -> isNonBlank(field) && isNonBlank(value);
      case CONDITIONAL -> isValidConditional(conditional);
    };
  }

  private boolean isValidConditional(Migration.Conditional conditional) {
    if (conditional == null) {
      return false;
    }

    return isValidBranch(conditional.getCondition())
        && isValidBranch(conditional.getTruePath())
        && isValidBranch(conditional.getFalsePath());
  }

  private boolean isValidBranch(Migration.Branch branch) {
    if (branch == null) {
      return false;
    }
    return isNonBlank(branch.getField()) && isNonBlank(branch.getValue());
  }

  private boolean isNonBlank(String str) {
    return str != null && !str.isBlank();
  }

  public ValidationResult validateMigrationSequence(
      List<Migration> existingMigrations, Migration candidate) {
    if (candidate == null || candidate.getDatabase() == null) {
      return ValidationResult.valid();
    }

    if (existingMigrations == null || existingMigrations.isEmpty()) {
      return ValidationResult.valid();
    }

    String targetDatabase = candidate.getDatabase();

    // Filter existing migrations to ONLY those targeting the exact same database
    List<Migration> sameDbMigrations =
        existingMigrations.stream()
            .filter(m -> targetDatabase.equalsIgnoreCase(m.getDatabase()))
            .toList();

    if (sameDbMigrations.isEmpty()) {
      return ValidationResult.valid();
    }

    Set<String> candidateFields = extractAllReferencedFields(candidate);

    for (Migration existing : sameDbMigrations) {
      Set<String> existingFields = extractAllReferencedFields(existing);

      // Find overlapping fields between candidate and existing migration
      Set<String> overlappingFields = new HashSet<>(candidateFields);
      overlappingFields.retainAll(existingFields);

      if (overlappingFields.isEmpty()) {
        continue;
      }

      boolean isCandidateConditional = candidate.getAction() == MigrationAction.CONDITIONAL;
      boolean isExistingConditional = existing.getAction() == MigrationAction.CONDITIONAL;

      // Conflict occurs if EITHER side is NOT conditional (ADD, DELETE, RENAME locks exclusively)
      if (!isCandidateConditional || !isExistingConditional) {
        String conflictingField = overlappingFields.iterator().next();
        return ValidationResult.invalid(
            String.format(
                "Field '%s' in database '%s' conflicts with existing %s migration.",
                conflictingField, targetDatabase, existing.getAction()));
      }
    }

    return ValidationResult.valid();
  }

  /**
   * REL-037: Extracts all fields involved in a migration. For CONDITIONAL migrations, extracts
   * fields from condition, truePath, and falsePath.
   */
  public Set<String> extractAllReferencedFields(Migration migration) {
    if (migration == null) {
      return Collections.emptySet();
    }

    if (migration.getAction() == MigrationAction.CONDITIONAL) {
      Migration.Conditional cond = migration.getConditional();
      if (cond == null) {
        return Collections.emptySet();
      }

      return Stream.of(cond.getCondition(), cond.getTruePath(), cond.getFalsePath())
          .filter(Objects::nonNull)
          .map(Migration.Branch::getField)
          .filter(f -> f != null && !f.isBlank())
          .collect(Collectors.toSet());
    }

    if (migration.getField() != null && !migration.getField().isBlank()) {
      return Set.of(migration.getField());
    }

    return Collections.emptySet();
  }

  public record ValidationResult(boolean isValid, String errorMessage) {
    public static ValidationResult valid() {
      return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String message) {
      return new ValidationResult(false, message);
    }
  }
}
