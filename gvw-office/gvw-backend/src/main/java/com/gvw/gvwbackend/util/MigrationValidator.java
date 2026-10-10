package com.gvw.gvwbackend.util;

import com.gvw.gvwbackend.model.Migration;
import com.gvw.gvwbackend.model.MigrationAction;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Utility component for validating individual migration fields, structures, and sequence-based
 * field conflicts across database migrations.
 */
@Component
public class MigrationValidator {

  /**
   * Validates whether the required fields for a given migration action are present and non-blank.
   *
   * @param action The migration action type (e.g. ADD, DELETE, RENAME, CONDITIONAL).
   * @param field The field targeted by the migration.
   * @param value The value associated with the migration.
   * @param conditional The conditional branch definitions if the action is conditional.
   * @return True if all required fields for the action are valid, false otherwise.
   */
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

  /**
   * Validates that all constituent branches of a conditional migration structure are valid.
   *
   * @param conditional The conditional structure containing condition, true path, and false path.
   * @return True if all non-null branches reference a valid field, false otherwise.
   */
  private boolean isValidConditional(Migration.Conditional conditional) {
    if (conditional == null) {
      return false;
    }

    return isValidBranch(conditional.getCondition())
        && isValidBranch(conditional.getTruePath())
        && isValidBranch(conditional.getFalsePath());
  }

  /**
   * Validates an individual migration branch.
   *
   * @param branch The branch to validate.
   * @return True if the branch is non-null and has a non-blank field, false otherwise.
   */
  private boolean isValidBranch(Migration.Branch branch) {
    if (branch == null) {
      return false;
    }
    return isNonBlank(branch.getField());
  }

  /**
   * Checks if a string is neither null nor blank.
   *
   * @param str The string to check.
   * @return True if the string has content, false otherwise.
   */
  private boolean isNonBlank(String str) {
    return str != null && !str.isBlank();
  }

  /**
   * Validates a candidate migration against a list of existing migrations targeting the same
   * database to ensure there are no overlapping field conflicts.
   *
   * @param existingMigrations The list of already registered migrations.
   * @param candidate The candidate migration to validate.
   * @return A {@link ValidationResult} indicating success or containing an error message on
   *     conflict.
   */
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
   *
   * @param migration The migration object to inspect.
   * @return A set of all distinct referenced field names.
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

  /**
   * Represents the outcome of a validation check, holding a validity flag and optional error
   * message.
   */
  public record ValidationResult(boolean isValid, String errorMessage) {

    /**
     * Creates a successful validation result.
     *
     * @return A valid {@link ValidationResult}.
     */
    public static ValidationResult valid() {
      return new ValidationResult(true, null);
    }

    /**
     * Creates a failed validation result with an error message.
     *
     * @param message The error description.
     * @return An invalid {@link ValidationResult}.
     */
    public static ValidationResult invalid(String message) {
      return new ValidationResult(false, message);
    }
  }
}
