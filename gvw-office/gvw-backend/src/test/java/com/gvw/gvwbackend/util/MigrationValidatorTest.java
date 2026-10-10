package com.gvw.gvwbackend.util;

import static org.junit.jupiter.api.Assertions.*;

import com.gvw.gvwbackend.model.Migration;
import com.gvw.gvwbackend.model.MigrationAction;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MigrationValidatorTest {

  private final MigrationValidator validator = new MigrationValidator();

  @Test
  void areMigrationFieldsValid_NullAction_ReturnsFalse() {
    assertFalse(validator.areMigrationFieldsValid(null, "field", "value", null));
  }

  @Test
  void areMigrationFieldsValid_ADD_ValidField_ReturnsTrue() {
    assertTrue(validator.areMigrationFieldsValid(MigrationAction.ADD, "fieldName", null, null));
  }

  @Test
  void areMigrationFieldsValid_ADD_NullField_ReturnsFalse() {
    assertFalse(validator.areMigrationFieldsValid(MigrationAction.ADD, null, null, null));
  }

  @Test
  void areMigrationFieldsValid_ADD_BlankField_ReturnsFalse() {
    assertFalse(validator.areMigrationFieldsValid(MigrationAction.ADD, "   ", null, null));
  }

  @Test
  void areMigrationFieldsValid_ADD_EmptyField_ReturnsFalse() {
    assertFalse(validator.areMigrationFieldsValid(MigrationAction.ADD, "", null, null));
  }

  @Test
  void areMigrationFieldsValid_DELETE_ValidField_ReturnsTrue() {
    assertTrue(validator.areMigrationFieldsValid(MigrationAction.DELETE, "fieldName", null, null));
  }

  @Test
  void areMigrationFieldsValid_DELETE_NullField_ReturnsFalse() {
    assertFalse(validator.areMigrationFieldsValid(MigrationAction.DELETE, null, null, null));
  }

  @Test
  void areMigrationFieldsValid_DELETE_BlankField_ReturnsFalse() {
    assertFalse(validator.areMigrationFieldsValid(MigrationAction.DELETE, "   ", null, null));
  }

  @Test
  void areMigrationFieldsValid_RENAME_ValidFieldAndValue_ReturnsTrue() {
    assertTrue(
        validator.areMigrationFieldsValid(MigrationAction.RENAME, "oldField", "newField", null));
  }

  @Test
  void areMigrationFieldsValid_RENAME_NullField_ReturnsFalse() {
    assertFalse(validator.areMigrationFieldsValid(MigrationAction.RENAME, null, "newField", null));
  }

  @Test
  void areMigrationFieldsValid_RENAME_BlankField_ReturnsFalse() {
    assertFalse(validator.areMigrationFieldsValid(MigrationAction.RENAME, "   ", "newField", null));
  }

  @Test
  void areMigrationFieldsValid_RENAME_NullValue_ReturnsFalse() {
    assertFalse(validator.areMigrationFieldsValid(MigrationAction.RENAME, "oldField", null, null));
  }

  @Test
  void areMigrationFieldsValid_RENAME_BlankValue_ReturnsFalse() {
    assertFalse(validator.areMigrationFieldsValid(MigrationAction.RENAME, "oldField", "   ", null));
  }

  @Test
  void areMigrationFieldsValid_CONDITIONAL_ValidConditional_ReturnsTrue() {
    Migration.Conditional conditional =
        Migration.Conditional.builder()
            .condition(Migration.Branch.builder().field("conditionField").build())
            .truePath(Migration.Branch.builder().field("trueField").build())
            .falsePath(Migration.Branch.builder().field("falseField").build())
            .build();

    assertTrue(
        validator.areMigrationFieldsValid(MigrationAction.CONDITIONAL, null, null, conditional));
  }

  @Test
  void areMigrationFieldsValid_CONDITIONAL_NullConditional_ReturnsFalse() {
    assertFalse(validator.areMigrationFieldsValid(MigrationAction.CONDITIONAL, null, null, null));
  }

  @Test
  void areMigrationFieldsValid_CONDITIONAL_NullConditionBranch_ReturnsFalse() {
    Migration.Conditional conditional =
        Migration.Conditional.builder()
            .truePath(Migration.Branch.builder().field("trueField").build())
            .falsePath(Migration.Branch.builder().field("falseField").build())
            .build();

    assertFalse(
        validator.areMigrationFieldsValid(MigrationAction.CONDITIONAL, null, null, conditional));
  }

  @Test
  void areMigrationFieldsValid_CONDITIONAL_BlankConditionField_ReturnsFalse() {
    Migration.Conditional conditional =
        Migration.Conditional.builder()
            .condition(Migration.Branch.builder().field("   ").build())
            .truePath(Migration.Branch.builder().field("trueField").build())
            .falsePath(Migration.Branch.builder().field("falseField").build())
            .build();

    assertFalse(
        validator.areMigrationFieldsValid(MigrationAction.CONDITIONAL, null, null, conditional));
  }

  @Test
  void areMigrationFieldsValid_CONDITIONAL_NullTruePathBranch_ReturnsFalse() {
    Migration.Conditional conditional =
        Migration.Conditional.builder()
            .condition(Migration.Branch.builder().field("conditionField").build())
            .falsePath(Migration.Branch.builder().field("falseField").build())
            .build();

    assertFalse(
        validator.areMigrationFieldsValid(MigrationAction.CONDITIONAL, null, null, conditional));
  }

  @Test
  void areMigrationFieldsValid_CONDITIONAL_BlankTruePathField_ReturnsFalse() {
    Migration.Conditional conditional =
        Migration.Conditional.builder()
            .condition(Migration.Branch.builder().field("conditionField").build())
            .truePath(Migration.Branch.builder().field("   ").build())
            .falsePath(Migration.Branch.builder().field("falseField").build())
            .build();

    assertFalse(
        validator.areMigrationFieldsValid(MigrationAction.CONDITIONAL, null, null, conditional));
  }

  @Test
  void areMigrationFieldsValid_CONDITIONAL_NullFalsePathBranch_ReturnsFalse() {
    Migration.Conditional conditional =
        Migration.Conditional.builder()
            .condition(Migration.Branch.builder().field("conditionField").build())
            .truePath(Migration.Branch.builder().field("trueField").build())
            .build();

    assertFalse(
        validator.areMigrationFieldsValid(MigrationAction.CONDITIONAL, null, null, conditional));
  }

  @Test
  void areMigrationFieldsValid_CONDITIONAL_BlankFalsePathField_ReturnsFalse() {
    Migration.Conditional conditional =
        Migration.Conditional.builder()
            .condition(Migration.Branch.builder().field("conditionField").build())
            .truePath(Migration.Branch.builder().field("trueField").build())
            .falsePath(Migration.Branch.builder().field("   ").build())
            .build();

    assertFalse(
        validator.areMigrationFieldsValid(MigrationAction.CONDITIONAL, null, null, conditional));
  }

  @Test
  void validateMigrationSequence_NullCandidate_ReturnsValid() {
    MigrationValidator.ValidationResult result =
        validator.validateMigrationSequence(List.of(), null);

    assertTrue(result.isValid());
    assertNull(result.errorMessage());
  }

  @Test
  void validateMigrationSequence_CandidateWithNullDatabase_ReturnsValid() {
    Migration candidate = Migration.builder().database(null).build();

    MigrationValidator.ValidationResult result =
        validator.validateMigrationSequence(List.of(), candidate);

    assertTrue(result.isValid());
    assertNull(result.errorMessage());
  }

  @Test
  void validateMigrationSequence_NullExistingMigrations_ReturnsValid() {
    Migration candidate = Migration.builder().database("testDb").field("field1").build();

    MigrationValidator.ValidationResult result =
        validator.validateMigrationSequence(null, candidate);

    assertTrue(result.isValid());
    assertNull(result.errorMessage());
  }

  @Test
  void validateMigrationSequence_EmptyExistingMigrations_ReturnsValid() {
    Migration candidate = Migration.builder().database("testDb").field("field1").build();

    MigrationValidator.ValidationResult result =
        validator.validateMigrationSequence(List.of(), candidate);

    assertTrue(result.isValid());
    assertNull(result.errorMessage());
  }

  @Test
  void validateMigrationSequence_DifferentDatabase_ReturnsValid() {
    Migration existing = Migration.builder().database("otherDb").field("field1").build();
    Migration candidate = Migration.builder().database("testDb").field("field1").build();

    MigrationValidator.ValidationResult result =
        validator.validateMigrationSequence(List.of(existing), candidate);

    assertTrue(result.isValid());
    assertNull(result.errorMessage());
  }

  @Test
  void validateMigrationSequence_CaseInsensitiveDatabaseMatch_DetectsConflict() {
    Migration existing = Migration.builder().database("testdb").field("field1").build();
    Migration candidate = Migration.builder().database("TESTDB").field("field1").build();

    MigrationValidator.ValidationResult result =
        validator.validateMigrationSequence(List.of(existing), candidate);

    assertFalse(result.isValid());
    assertTrue(result.errorMessage().contains("field1"));
    assertTrue(result.errorMessage().contains("TESTDB"));
  }

  @Test
  void validateMigrationSequence_NoOverlappingFields_ReturnsValid() {
    Migration existing = Migration.builder().database("testDb").field("field1").build();
    Migration candidate = Migration.builder().database("testDb").field("field2").build();

    MigrationValidator.ValidationResult result =
        validator.validateMigrationSequence(List.of(existing), candidate);

    assertTrue(result.isValid());
    assertNull(result.errorMessage());
  }

  @Test
  void validateMigrationSequence_OverlappingFields_CandidateNonConditional_ReturnsInvalid() {
    Migration existing =
        Migration.builder()
            .database("testDb")
            .field("field1")
            .action(MigrationAction.CONDITIONAL)
            .conditional(
                Migration.Conditional.builder()
                    .condition(Migration.Branch.builder().field("field1").build())
                    .truePath(Migration.Branch.builder().field("field2").build())
                    .falsePath(Migration.Branch.builder().field("field3").build())
                    .build())
            .build();
    Migration candidate =
        Migration.builder().database("testDb").field("field1").action(MigrationAction.ADD).build();

    MigrationValidator.ValidationResult result =
        validator.validateMigrationSequence(List.of(existing), candidate);

    assertFalse(result.isValid());
    assertTrue(result.errorMessage().contains("field1"));
    assertTrue(result.errorMessage().contains("CONDITIONAL"));
  }

  @Test
  void validateMigrationSequence_OverlappingFields_ExistingNonConditional_ReturnsInvalid() {
    Migration existing =
        Migration.builder().database("testDb").field("field1").action(MigrationAction.ADD).build();
    Migration candidate =
        Migration.builder()
            .database("testDb")
            .field("field1")
            .action(MigrationAction.CONDITIONAL)
            .conditional(
                Migration.Conditional.builder()
                    .condition(Migration.Branch.builder().field("field1").build())
                    .truePath(Migration.Branch.builder().field("field2").build())
                    .falsePath(Migration.Branch.builder().field("field3").build())
                    .build())
            .build();

    MigrationValidator.ValidationResult result =
        validator.validateMigrationSequence(List.of(existing), candidate);

    assertFalse(result.isValid());
    assertTrue(result.errorMessage().contains("field1"));
    assertTrue(result.errorMessage().contains("ADD"));
  }

  @Test
  void validateMigrationSequence_OverlappingFields_BothConditional_ReturnsValid() {
    Migration existing =
        Migration.builder()
            .database("testDb")
            .action(MigrationAction.CONDITIONAL)
            .conditional(
                Migration.Conditional.builder()
                    .condition(Migration.Branch.builder().field("field1").build())
                    .truePath(Migration.Branch.builder().field("field2").build())
                    .falsePath(Migration.Branch.builder().field("field3").build())
                    .build())
            .build();
    Migration candidate =
        Migration.builder()
            .database("testDb")
            .action(MigrationAction.CONDITIONAL)
            .conditional(
                Migration.Conditional.builder()
                    .condition(Migration.Branch.builder().field("field1").build())
                    .truePath(Migration.Branch.builder().field("field4").build())
                    .falsePath(Migration.Branch.builder().field("field5").build())
                    .build())
            .build();

    MigrationValidator.ValidationResult result =
        validator.validateMigrationSequence(List.of(existing), candidate);

    assertTrue(result.isValid());
    assertNull(result.errorMessage());
  }

  @Test
  void validateMigrationSequence_OverlappingFields_BothNonConditional_ReturnsInvalid() {
    Migration existing =
        Migration.builder().database("testDb").field("field1").action(MigrationAction.ADD).build();
    Migration candidate =
        Migration.builder()
            .database("testDb")
            .field("field1")
            .action(MigrationAction.DELETE)
            .build();

    MigrationValidator.ValidationResult result =
        validator.validateMigrationSequence(List.of(existing), candidate);

    assertFalse(result.isValid());
    assertTrue(result.errorMessage().contains("field1"));
    assertTrue(result.errorMessage().contains("ADD"));
  }

  @Test
  void validateMigrationSequence_MultipleExistingMigrations_OnlySameDbChecked() {
    Migration existing1 = Migration.builder().database("otherDb").field("field1").build();
    Migration existing2 = Migration.builder().database("testDb").field("field1").build();
    Migration candidate = Migration.builder().database("testDb").field("field1").build();

    MigrationValidator.ValidationResult result =
        validator.validateMigrationSequence(List.of(existing1, existing2), candidate);

    assertFalse(result.isValid());
  }

  @Test
  void extractAllReferencedFields_NullMigration_ReturnsEmptySet() {
    Set<String> fields = validator.extractAllReferencedFields(null);

    assertTrue(fields.isEmpty());
  }

  @Test
  void extractAllReferencedFields_NonConditional_ValidField_ReturnsField() {
    Migration migration = Migration.builder().field("field1").action(MigrationAction.ADD).build();

    Set<String> fields = validator.extractAllReferencedFields(migration);

    assertEquals(1, fields.size());
    assertTrue(fields.contains("field1"));
  }

  @Test
  void extractAllReferencedFields_NonConditional_NullField_ReturnsEmptySet() {
    Migration migration = Migration.builder().field(null).action(MigrationAction.ADD).build();

    Set<String> fields = validator.extractAllReferencedFields(migration);

    assertTrue(fields.isEmpty());
  }

  @Test
  void extractAllReferencedFields_NonConditional_BlankField_ReturnsEmptySet() {
    Migration migration = Migration.builder().field("   ").action(MigrationAction.ADD).build();

    Set<String> fields = validator.extractAllReferencedFields(migration);

    assertTrue(fields.isEmpty());
  }

  @Test
  void extractAllReferencedFields_Conditional_AllBranchesPresent_ReturnsAllFields() {
    Migration.Conditional conditional =
        Migration.Conditional.builder()
            .condition(Migration.Branch.builder().field("conditionField").build())
            .truePath(Migration.Branch.builder().field("trueField").build())
            .falsePath(Migration.Branch.builder().field("falseField").build())
            .build();
    Migration migration =
        Migration.builder().action(MigrationAction.CONDITIONAL).conditional(conditional).build();

    Set<String> fields = validator.extractAllReferencedFields(migration);

    assertEquals(3, fields.size());
    assertTrue(fields.contains("conditionField"));
    assertTrue(fields.contains("trueField"));
    assertTrue(fields.contains("falseField"));
  }

  @Test
  void extractAllReferencedFields_Conditional_NullConditional_ReturnsEmptySet() {
    Migration migration =
        Migration.builder().action(MigrationAction.CONDITIONAL).conditional(null).build();

    Set<String> fields = validator.extractAllReferencedFields(migration);

    assertTrue(fields.isEmpty());
  }

  @Test
  void extractAllReferencedFields_Conditional_NullBranches_ReturnsEmptySet() {
    Migration.Conditional conditional =
        Migration.Conditional.builder().condition(null).truePath(null).falsePath(null).build();
    Migration migration =
        Migration.builder().action(MigrationAction.CONDITIONAL).conditional(conditional).build();

    Set<String> fields = validator.extractAllReferencedFields(migration);

    assertTrue(fields.isEmpty());
  }

  @Test
  void extractAllReferencedFields_Conditional_SomeNullBranches_ReturnsNonNullFields() {
    Migration.Conditional conditional =
        Migration.Conditional.builder()
            .condition(Migration.Branch.builder().field("conditionField").build())
            .truePath(null)
            .falsePath(Migration.Branch.builder().field("falseField").build())
            .build();
    Migration migration =
        Migration.builder().action(MigrationAction.CONDITIONAL).conditional(conditional).build();

    Set<String> fields = validator.extractAllReferencedFields(migration);

    assertEquals(2, fields.size());
    assertTrue(fields.contains("conditionField"));
    assertTrue(fields.contains("falseField"));
  }

  @Test
  void extractAllReferencedFields_Conditional_BlankFields_ReturnsEmptySet() {
    Migration.Conditional conditional =
        Migration.Conditional.builder()
            .condition(Migration.Branch.builder().field("   ").build())
            .truePath(Migration.Branch.builder().field("").build())
            .falsePath(Migration.Branch.builder().field(null).build())
            .build();
    Migration migration =
        Migration.builder().action(MigrationAction.CONDITIONAL).conditional(conditional).build();

    Set<String> fields = validator.extractAllReferencedFields(migration);

    assertTrue(fields.isEmpty());
  }

  @Test
  void extractAllReferencedFields_Conditional_DuplicateFields_ReturnsUniqueSet() {
    Migration.Conditional conditional =
        Migration.Conditional.builder()
            .condition(Migration.Branch.builder().field("field1").build())
            .truePath(Migration.Branch.builder().field("field1").build())
            .falsePath(Migration.Branch.builder().field("field1").build())
            .build();
    Migration migration =
        Migration.builder().action(MigrationAction.CONDITIONAL).conditional(conditional).build();

    Set<String> fields = validator.extractAllReferencedFields(migration);

    assertEquals(1, fields.size());
    assertTrue(fields.contains("field1"));
  }

  @Test
  void ValidationResult_valid_ReturnsValidResult() {
    MigrationValidator.ValidationResult result = MigrationValidator.ValidationResult.valid();

    assertTrue(result.isValid());
    assertNull(result.errorMessage());
  }

  @Test
  void ValidationResult_invalid_ReturnsInvalidResult() {
    String errorMessage = "Test error message";
    MigrationValidator.ValidationResult result =
        MigrationValidator.ValidationResult.invalid(errorMessage);

    assertFalse(result.isValid());
    assertEquals(errorMessage, result.errorMessage());
  }
}
