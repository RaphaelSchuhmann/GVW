package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.AddDeploymentRequestDTO;
import com.gvw.gvwbackend.dto.request.AddMigrationRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateDeploymentInformationRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateDeploymentStatusRequestDTO;
import com.gvw.gvwbackend.dto.response.DeploymentResponseDTO;
import com.gvw.gvwbackend.dto.response.HealthResponseDTO;
import com.gvw.gvwbackend.exception.BadGatewayException;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ForbiddenException;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.Deployment;
import com.gvw.gvwbackend.model.DeploymentStatus;
import com.gvw.gvwbackend.model.Migration;
import com.gvw.gvwbackend.model.MigrationAction;
import com.gvw.gvwbackend.model.deploymentSystem.HealthStatus;
import com.gvw.gvwbackend.model.helper.GVWHttpResponse;
import com.gvw.gvwbackend.state.MaintenanceState;
import com.gvw.gvwbackend.util.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

  @Mock private DbService dbService;

  @Mock private SseService sseService;

  @Mock private DeploymentValidator deploymentValidator;

  @Mock private MigrationValidator migrationValidator;

  @Mock private HashUtil hashUtil;

  @Mock private HttpUtil httpUtil;

  @Mock private AppSettingsService appSettingsService;

  @Mock private MaintenanceState maintenanceState;

  private DeploymentService deploymentService;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2024-01-15T12:00:00Z"), ZoneOffset.UTC);
    deploymentService =
        new DeploymentService(
            dbService,
            sseService,
            deploymentValidator,
            migrationValidator,
            hashUtil,
            fixedClock,
            httpUtil,
            appSettingsService,
            maintenanceState);
  }

  @Test
  void getTodayDeployments_NullDeployments_ReturnsEmptyList() {
    when(dbService.findByQuery(anyString(), anyMap(), eq(Deployment.class))).thenReturn(null);

    List<String> result = deploymentService.getTodayDeployments();

    assertTrue(result.isEmpty());
  }

  @Test
  void getTodayDeployments_EmptyDeployments_ReturnsEmptyList() {
    when(dbService.findByQuery(anyString(), anyMap(), eq(Deployment.class))).thenReturn(List.of());

    List<String> result = deploymentService.getTodayDeployments();

    assertTrue(result.isEmpty());
  }

  @Test
  void getTodayDeployments_TodayDeploymentBeforeStartTime_ReturnsFormattedTime() {
    Deployment deployment =
        Deployment.builder()
            .date(OffsetDateTime.now(fixedClock).toString())
            .startTime(LocalTime.now(fixedClock).plusHours(1))
            .endTime(LocalTime.now(fixedClock).plusHours(2))
            .build();

    when(dbService.findByQuery(anyString(), anyMap(), eq(Deployment.class)))
        .thenReturn(List.of(deployment));

    List<String> result = deploymentService.getTodayDeployments();

    assertEquals(1, result.size());
    assertTrue(result.getFirst().contains(" - "));
  }

  @Test
  void getTodayDeployments_PastDateDeployment_ReturnsEmptyList() {
    Deployment deployment =
        Deployment.builder()
            .date(OffsetDateTime.now(fixedClock).minusDays(1).toString())
            .startTime(LocalTime.now(fixedClock).plusHours(1))
            .endTime(LocalTime.now(fixedClock).plusHours(2))
            .build();

    when(dbService.findByQuery(anyString(), anyMap(), eq(Deployment.class)))
        .thenReturn(List.of(deployment));

    List<String> result = deploymentService.getTodayDeployments();

    assertTrue(result.isEmpty());
  }

  @Test
  void getTodayDeployments_TodayDeploymentAfterStartTime_ReturnsEmptyList() {
    Deployment deployment =
        Deployment.builder()
            .date(OffsetDateTime.now(fixedClock).toString())
            .startTime(LocalTime.now(fixedClock).minusHours(1))
            .endTime(LocalTime.now(fixedClock).plusHours(1))
            .build();

    when(dbService.findByQuery(anyString(), anyMap(), eq(Deployment.class)))
        .thenReturn(List.of(deployment));

    List<String> result = deploymentService.getTodayDeployments();

    assertTrue(result.isEmpty());
  }

  @Test
  void getAllDeployments_NullDeployments_ReturnsEmptyList() {
    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(null);

    List<DeploymentResponseDTO> result = deploymentService.getAllDeployments();

    assertTrue(result.isEmpty());
  }

  @Test
  void getAllDeployments_EmptyDeployments_ReturnsEmptyList() {
    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of());

    List<DeploymentResponseDTO> result = deploymentService.getAllDeployments();

    assertTrue(result.isEmpty());
  }

  @Test
  void getAllDeployments_ExistingDeployments_ReturnsDTOs() {
    Deployment deployment =
        Deployment.builder()
            .id("test-id")
            .rev("1-rev")
            .title("Test Deployment")
            .status(DeploymentStatus.SCHEDULED)
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .commit("abc123")
            .hash("hash123")
            .appVersion("1.0.0")
            .updates(List.of())
            .build();

    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of(deployment));

    List<DeploymentResponseDTO> result = deploymentService.getAllDeployments();

    assertEquals(1, result.size());
    assertEquals("test-id", result.getFirst().id());
    assertEquals("Test Deployment", result.getFirst().title());
  }

  @Test
  void getDeployment_ValidId_ReturnsDTO() {
    Deployment deployment =
        Deployment.builder()
            .id("test-id")
            .rev("1-rev")
            .title("Test Deployment")
            .status(DeploymentStatus.SCHEDULED)
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .commit("abc123")
            .hash("hash123")
            .appVersion("1.0.0")
            .updates(List.of())
            .build();

    when(dbService.findById(anyString(), eq("test-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    DeploymentResponseDTO result = deploymentService.getDeployment("test-id");

    assertEquals("test-id", result.id());
    assertEquals("Test Deployment", result.title());
  }

  @Test
  void getDeployment_NullId_ThrowsBadRequestException() {
    assertThrows(BadRequestException.class, () -> deploymentService.getDeployment(null));
  }

  @Test
  void getDeployment_BlankId_ThrowsBadRequestException() {
    assertThrows(BadRequestException.class, () -> deploymentService.getDeployment("   "));
  }

  @Test
  void getDeployment_NotFound_ThrowsNotFoundException() {
    when(dbService.findById(anyString(), eq("test-id"), eq(Deployment.class))).thenReturn(null);

    assertThrows(NotFoundException.class, () -> deploymentService.getDeployment("test-id"));
  }

  @Test
  void checkDeployment_ValidId_DoesNotThrow() {
    Deployment deployment =
        Deployment.builder()
            .id("test-id")
            .title("Test Deployment")
            .status(DeploymentStatus.SCHEDULED)
            .build();

    when(dbService.findById(anyString(), eq("test-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    assertDoesNotThrow(() -> deploymentService.checkDeployment("test-id"));
  }

  @Test
  void checkDeployment_NullId_ThrowsBadRequestException() {
    assertThrows(BadRequestException.class, () -> deploymentService.checkDeployment(null));
  }

  @Test
  void checkDeployment_NotFound_ThrowsNotFoundException() {
    when(dbService.findById(anyString(), eq("test-id"), eq(Deployment.class))).thenReturn(null);

    assertThrows(NotFoundException.class, () -> deploymentService.checkDeployment("test-id"));
  }

  @Test
  void addDeployment_TimeAlreadyStarted_ThrowsBadRequestException() {
    AddDeploymentRequestDTO request =
        new AddDeploymentRequestDTO(
            "Test", "1.0.0", "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(11, 0));

    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of());
    when(deploymentValidator.isTodayAndAlreadyStarted(anyString(), any(LocalTime.class)))
        .thenReturn(true);

    assertThrows(BadRequestException.class, () -> deploymentService.addDeployment(request));
  }

  @Test
  void addDeployment_HasOverlap_ThrowsBadRequestException() {
    AddDeploymentRequestDTO request =
        new AddDeploymentRequestDTO(
            "Test", "1.0.0", "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(11, 0));

    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of());
    when(deploymentValidator.isTodayAndAlreadyStarted(anyString(), any(LocalTime.class)))
        .thenReturn(false);
    when(deploymentValidator.hasOverlap(
            anyList(), anyString(), any(LocalTime.class), any(LocalTime.class)))
        .thenReturn(true);

    assertThrows(BadRequestException.class, () -> deploymentService.addDeployment(request));
  }

  @Test
  void addDeployment_ValidRequest_InsertsDeploymentAndSendsRefresh() {
    AddDeploymentRequestDTO request =
        new AddDeploymentRequestDTO(
            "Test", "1.0.0", "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(11, 0));

    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of());
    when(deploymentValidator.isTodayAndAlreadyStarted(anyString(), any(LocalTime.class)))
        .thenReturn(false);
    when(deploymentValidator.hasOverlap(
            anyList(), anyString(), any(LocalTime.class), any(LocalTime.class)))
        .thenReturn(false);
    when(hashUtil.generateShortHash()).thenReturn("shortHash");

    deploymentService.addDeployment(request);

    verify(dbService).insert(eq("deployments"), any(Deployment.class));
    verify(sseService).sendRefresh("DEPLOYMENTS");
  }

  @Test
  void addMigration_InvalidFields_ThrowsBadRequestException() {
    AddMigrationRequestDTO request =
        new AddMigrationRequestDTO(
            "dep-id", "1-rev", "test-db", null, MigrationAction.ADD, null, null);

    when(migrationValidator.areMigrationFieldsValid(
            any(MigrationAction.class), any(), any(), any()))
        .thenReturn(false);

    assertThrows(BadRequestException.class, () -> deploymentService.addMigration(request));
  }

  @Test
  void addMigration_InvalidSequence_ThrowsBadRequestException() {
    AddMigrationRequestDTO request =
        new AddMigrationRequestDTO(
            "dep-id", "1-rev", "test-db", "field1", MigrationAction.ADD, null, null);

    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .rev("1-rev")
            .status(DeploymentStatus.SCHEDULED)
            .migrations(List.of())
            .build();

    when(migrationValidator.areMigrationFieldsValid(
            any(MigrationAction.class), anyString(), any(), any()))
        .thenReturn(true);
    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(migrationValidator.validateMigrationSequence(anyList(), any(Migration.class)))
        .thenReturn(MigrationValidator.ValidationResult.invalid("Conflict"));

    assertThrows(BadRequestException.class, () -> deploymentService.addMigration(request));
  }

  @Test
  void addMigration_ValidRequest_AddsMigrationAndUpdatesDeployment() {
    AddMigrationRequestDTO request =
        new AddMigrationRequestDTO(
            "dep-id", "1-rev", "test-db", "field1", MigrationAction.ADD, null, null);

    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .rev("1-rev")
            .status(DeploymentStatus.SCHEDULED)
            .migrations(List.of())
            .build();

    when(migrationValidator.areMigrationFieldsValid(
            any(MigrationAction.class), anyString(), any(), any()))
        .thenReturn(true);
    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    // Stub the validation sequence so it doesn't return null
    when(migrationValidator.validateMigrationSequence(anyList(), any(Migration.class)))
        .thenReturn(MigrationValidator.ValidationResult.valid());

    deploymentService.addMigration(request);

    assertEquals(1, deployment.getMigrations().size());
    verify(dbService).update(eq("deployments"), eq("dep-id"), any(Deployment.class));
  }

  @Test
  void addMigration_RunningDeployment_ThrowsForbiddenException() {
    AddMigrationRequestDTO request =
        new AddMigrationRequestDTO(
            "dep-id", "1-rev", "test-db", "field1", MigrationAction.ADD, null, null);

    Deployment deployment =
        Deployment.builder().id("dep-id").status(DeploymentStatus.RUNNING).build();

    when(migrationValidator.areMigrationFieldsValid(
            any(MigrationAction.class), anyString(), any(), any()))
        .thenReturn(true);
    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    assertThrows(ForbiddenException.class, () -> deploymentService.addMigration(request));
    verify(dbService, never()).update(anyString(), anyString(), any());
  }

  @Test
  void addDeployment_EndTimeNotAfterStartTime_ThrowsBadRequestException() {
    AddDeploymentRequestDTO request =
        new AddDeploymentRequestDTO(
            "Test", "1.0.0", "2024-01-01T00:00:00Z", LocalTime.of(11, 0), LocalTime.of(10, 0));

    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of());
    when(deploymentValidator.isTodayAndAlreadyStarted(anyString(), any(LocalTime.class)))
        .thenReturn(false);
    when(deploymentValidator.hasOverlap(
            anyList(), anyString(), any(LocalTime.class), any(LocalTime.class)))
        .thenReturn(false);

    assertThrows(BadRequestException.class, () -> deploymentService.addDeployment(request));
    verify(dbService, never()).insert(anyString(), any());
  }

  @Test
  void updateDeploymentInformation_InvalidStatus_ThrowsForbiddenException() {
    UpdateDeploymentInformationRequestDTO request =
        new UpdateDeploymentInformationRequestDTO(
            "dep-id",
            "1-rev",
            "Updated Title",
            "1.0.0",
            "2024-01-01T00:00:00Z",
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            null);

    Deployment deployment =
        Deployment.builder().id("dep-id").status(DeploymentStatus.RUNNING).build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of(deployment));

    assertThrows(
        com.gvw.gvwbackend.exception.ForbiddenException.class,
        () -> deploymentService.updateDeploymentInformation(request));
  }

  @Test
  void updateDeploymentInformation_TimeAlreadyStarted_ThrowsBadRequestException() {
    UpdateDeploymentInformationRequestDTO request =
        new UpdateDeploymentInformationRequestDTO(
            "dep-id",
            "1-rev",
            "Updated Title",
            "1.0.0",
            "2024-01-01T00:00:00Z",
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            null);

    Deployment deployment =
        Deployment.builder().id("dep-id").status(DeploymentStatus.SCHEDULED).build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of(deployment));
    when(deploymentValidator.isTodayAndAlreadyStarted(anyString(), any(LocalTime.class)))
        .thenReturn(true);

    assertThrows(
        BadRequestException.class, () -> deploymentService.updateDeploymentInformation(request));
  }

  @Test
  void updateDeploymentInformation_HasOverlap_ThrowsBadRequestException() {
    UpdateDeploymentInformationRequestDTO request =
        new UpdateDeploymentInformationRequestDTO(
            "dep-id",
            "1-rev",
            "Updated Title",
            "1.0.0",
            "2024-01-01T00:00:00Z",
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            null);

    Deployment deployment =
        Deployment.builder().id("dep-id").status(DeploymentStatus.SCHEDULED).build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of(deployment));
    when(deploymentValidator.isTodayAndAlreadyStarted(anyString(), any(LocalTime.class)))
        .thenReturn(false);
    when(deploymentValidator.hasOverlap(
            anyList(), anyString(), any(LocalTime.class), any(LocalTime.class)))
        .thenReturn(true);

    assertThrows(
        BadRequestException.class, () -> deploymentService.updateDeploymentInformation(request));
  }

  @Test
  void updateDeploymentInformation_ValidRequest_UpdatesDeployment() {
    UpdateDeploymentInformationRequestDTO request =
        new UpdateDeploymentInformationRequestDTO(
            "dep-id",
            "1-rev",
            "Updated Title",
            "1.0.0",
            "2024-01-01T00:00:00Z",
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            null);

    Deployment deployment =
        Deployment.builder().id("dep-id").status(DeploymentStatus.SCHEDULED).build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of(deployment));
    when(deploymentValidator.isTodayAndAlreadyStarted(anyString(), any(LocalTime.class)))
        .thenReturn(false);
    when(deploymentValidator.hasOverlap(
            anyList(), anyString(), any(LocalTime.class), any(LocalTime.class)))
        .thenReturn(false);

    deploymentService.updateDeploymentInformation(request);

    assertEquals("Updated Title", deployment.getTitle());
    assertEquals("1.0.0", deployment.getAppVersion());
    verify(dbService).update(eq("deployments"), eq("dep-id"), any(Deployment.class));
  }

  @Test
  void updateDeploymentInformation_EndTimeNotAfterStartTime_ThrowsBadRequestException() {
    UpdateDeploymentInformationRequestDTO request =
        new UpdateDeploymentInformationRequestDTO(
            "dep-id",
            "1-rev",
            "Updated Title",
            "1.0.0",
            "2024-01-01T00:00:00Z",
            LocalTime.of(11, 0),
            LocalTime.of(10, 0),
            null);

    Deployment deployment =
        Deployment.builder().id("dep-id").status(DeploymentStatus.SCHEDULED).build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of(deployment));
    when(deploymentValidator.isTodayAndAlreadyStarted(anyString(), any(LocalTime.class)))
        .thenReturn(false);
    when(deploymentValidator.hasOverlap(
            anyList(), anyString(), any(LocalTime.class), any(LocalTime.class)))
        .thenReturn(false);

    assertThrows(
        BadRequestException.class, () -> deploymentService.updateDeploymentInformation(request));
    verify(dbService, never()).update(anyString(), anyString(), any());
  }

  @Test
  void updateDeploymentInformation_SameCommit_DoesNotTriggerBuild() {
    UpdateDeploymentInformationRequestDTO request =
        new UpdateDeploymentInformationRequestDTO(
            "dep-id",
            "1-rev",
            "Updated Title",
            "1.0.0",
            "2024-01-01T00:00:00Z",
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            "same-commit");

    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.SCHEDULED)
            .commit("same-commit")
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of(deployment));
    when(deploymentValidator.isTodayAndAlreadyStarted(anyString(), any(LocalTime.class)))
        .thenReturn(false);
    when(deploymentValidator.hasOverlap(
            anyList(), anyString(), any(LocalTime.class), any(LocalTime.class)))
        .thenReturn(false);

    deploymentService.updateDeploymentInformation(request);

    verify(httpUtil, never()).sendPostRequestWithoutBody(anyString(), any());
    verify(dbService).update(eq("deployments"), eq("dep-id"), any(Deployment.class));
  }

  @Test
  void updateDeploymentInformation_ValidCommit_UpdatesCommit() {
    UpdateDeploymentInformationRequestDTO request =
        new UpdateDeploymentInformationRequestDTO(
            "dep-id",
            "1-rev",
            "Updated Title",
            "1.0.0",
            "2024-01-01T00:00:00Z",
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            "abc123");

    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .commit("dce456")
            .status(DeploymentStatus.SCHEDULED)
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of(deployment));
    when(deploymentValidator.isTodayAndAlreadyStarted(anyString(), any(LocalTime.class)))
        .thenReturn(false);
    when(deploymentValidator.hasOverlap(
            anyList(), anyString(), any(LocalTime.class), any(LocalTime.class)))
        .thenReturn(false);
    when(deploymentValidator.isCommitHashValid(anyString())).thenReturn(true);
    when(httpUtil.sendPostRequestWithoutBody(anyString(), eq(String.class)))
        .thenReturn(new GVWHttpResponse(200, null, null));

    deploymentService.updateDeploymentInformation(request);

    assertEquals("abc123", deployment.getCommit());
  }

  @Test
  void updateDeploymentInformation_InvalidCommit_ThrowsBadRequestException() {
    UpdateDeploymentInformationRequestDTO request =
        new UpdateDeploymentInformationRequestDTO(
            "dep-id",
            "1-rev",
            "Updated Title",
            "1.0.0",
            "2024-01-01T00:00:00Z",
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            "invalid");

    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.SCHEDULED)
            .commit("old-commit")
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(dbService.findAll(anyString(), eq(Deployment.class))).thenReturn(List.of(deployment));
    when(deploymentValidator.isTodayAndAlreadyStarted(anyString(), any(LocalTime.class)))
        .thenReturn(false);
    when(deploymentValidator.hasOverlap(
            anyList(), anyString(), any(LocalTime.class), any(LocalTime.class)))
        .thenReturn(false);
    when(deploymentValidator.isCommitHashValid(anyString())).thenReturn(false);

    assertThrows(
        BadRequestException.class, () -> deploymentService.updateDeploymentInformation(request));

    assertEquals("old-commit", deployment.getCommit());
  }

  @Test
  void removeDeployment_ValidId_DeletesDeploymentAndSendsRefresh() {
    Deployment deployment =
        Deployment.builder().id("dep-id").rev("1-rev").status(DeploymentStatus.SCHEDULED).build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    deploymentService.removeDeployment("dep-id");

    verify(dbService).delete(eq("deployments"), eq("dep-id"), eq("1-rev"));
    verify(sseService).sendRefresh("DEPLOYMENTS");
  }

  @Test
  void removeDeployment_NullId_ThrowsBadRequestException() {
    assertThrows(BadRequestException.class, () -> deploymentService.removeDeployment(null));
  }

  @Test
  void removeDeployment_NotFound_ThrowsNotFoundException() {
    when(dbService.findById(anyString(), eq("test-id"), eq(Deployment.class))).thenReturn(null);

    assertThrows(NotFoundException.class, () -> deploymentService.removeDeployment("test-id"));
  }

  @Test
  void removeMigration_NullMigrationId_ThrowsBadRequestException() {
    assertThrows(
        BadRequestException.class, () -> deploymentService.removeMigration("dep-id", null));
  }

  @Test
  void removeMigration_BlankMigrationId_ThrowsBadRequestException() {
    assertThrows(
        BadRequestException.class, () -> deploymentService.removeMigration("dep-id", "   "));
  }

  @Test
  void removeMigration_ValidId_RemovesMigrationAndUpdatesDeployment() {
    Migration migration =
        Migration.builder()
            .id("migration-id")
            .database("test-db")
            .action(MigrationAction.ADD)
            .field("field1")
            .build();

    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.SCHEDULED)
            .migrations(List.of(migration))
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    deploymentService.removeMigration("dep-id", "migration-id");

    assertTrue(deployment.getMigrations().isEmpty());
    verify(dbService).update(eq("deployments"), eq("dep-id"), any(Deployment.class));
  }

  @Test
  void removeMigration_NonScheduledStatus_ThrowsBadRequestException() {
    Migration migration =
        Migration.builder()
            .id("migration-id")
            .database("test-db")
            .action(MigrationAction.ADD)
            .field("field1")
            .build();

    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.SUCCESSFUL)
            .migrations(List.of(migration))
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    assertThrows(
        BadRequestException.class,
        () -> deploymentService.removeMigration("dep-id", "migration-id"));
    verify(dbService, never()).update(anyString(), anyString(), any());
  }

  @Test
  void removeMigration_MigrationNotFound_DoesNotRemoveAny() {
    Migration migration =
        Migration.builder()
            .id("other-migration-id")
            .database("test-db")
            .action(MigrationAction.ADD)
            .field("field1")
            .build();

    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.SCHEDULED)
            .migrations(List.of(migration))
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    deploymentService.removeMigration("dep-id", "migration-id");

    assertEquals(1, deployment.getMigrations().size());
    verify(dbService).update(eq("deployments"), eq("dep-id"), any(Deployment.class));
  }

  @Test
  void startMigrationDryRun_NullId_ThrowsBadRequestException() {
    assertThrows(BadRequestException.class, () -> deploymentService.startMigrationDryRun(null));
  }

  @Test
  void startMigrationDryRun_BlankId_ThrowsBadRequestException() {
    assertThrows(BadRequestException.class, () -> deploymentService.startMigrationDryRun("   "));
  }

  @Test
  void startMigrationDryRun_NotFound_ThrowsNotFoundException() {
    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class))).thenReturn(null);

    assertThrows(NotFoundException.class, () -> deploymentService.startMigrationDryRun("dep-id"));
  }

  @Test
  void startMigrationDryRun_NoMigrations_DoesNotCallPipeline() {
    Deployment deployment =
        Deployment.builder().id("dep-id").status(DeploymentStatus.SCHEDULED).build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    assertDoesNotThrow(() -> deploymentService.startMigrationDryRun("dep-id"));
    verify(httpUtil, never()).sendPostRequestWithBody(anyString(), any(), any());
  }

  @Test
  void startMigrationDryRun_Success_SendsRequestToPipeline() {
    Migration migration =
        Migration.builder()
            .id("migration-id")
            .database("test-db")
            .action(MigrationAction.ADD)
            .field("field1")
            .build();

    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.SCHEDULED)
            .migrations(List.of(migration))
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(httpUtil.sendPostRequestWithBody(anyString(), any(), any()))
        .thenReturn(new GVWHttpResponse(200, null, null));

    assertDoesNotThrow(() -> deploymentService.startMigrationDryRun("dep-id"));

    verify(httpUtil)
        .sendPostRequestWithBody(
            argThat(url -> url.contains("/migration/test/dep-id")), any(), isNull());
  }

  @Test
  void startMigrationDryRun_PipelineError_ThrowsBadGatewayException() {
    Migration migration =
        Migration.builder()
            .id("migration-id")
            .database("test-db")
            .action(MigrationAction.ADD)
            .field("field1")
            .build();

    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.SCHEDULED)
            .migrations(List.of(migration))
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(httpUtil.sendPostRequestWithBody(anyString(), any(), any()))
        .thenReturn(new GVWHttpResponse(500, null, null, true));

    assertThrows(BadGatewayException.class, () -> deploymentService.startMigrationDryRun("dep-id"));
  }

  @Test
  void startDeployment_NullId_ThrowsBadRequestException() {
    assertThrows(BadRequestException.class, () -> deploymentService.startDeployment(null));
  }

  @Test
  void startDeployment_BlankId_ThrowsBadRequestException() {
    assertThrows(BadRequestException.class, () -> deploymentService.startDeployment("   "));
  }

  @Test
  void startDeployment_NotFound_ThrowsNotFoundException() {
    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class))).thenReturn(null);

    assertThrows(NotFoundException.class, () -> deploymentService.startDeployment("dep-id"));
  }

  @Test
  void startDeployment_Success_SetsMaintenanceSendsKillSignalAndTriggersDeploy() {
    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .commit("abc123")
            .appVersion("1.0.0")
            .status(DeploymentStatus.SCHEDULED)
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(httpUtil.sendPostRequestWithBody(anyString(), any(), any()))
        .thenReturn(new GVWHttpResponse(200, null, null));

    assertDoesNotThrow(() -> deploymentService.startDeployment("dep-id"));

    verify(maintenanceState).setActive(true);
    verify(sseService).sendKillSignal();
    verify(httpUtil)
        .sendPostRequestWithBody(
            argThat(url -> url.contains("/deploy/dep-id/abc123")), any(), isNull());
  }

  @Test
  void startDeployment_PipelineError_ResetsMaintenanceAndThrowsBadGatewayException() {
    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .commit("abc123")
            .appVersion("1.0.0")
            .status(DeploymentStatus.SCHEDULED)
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(httpUtil.sendPostRequestWithBody(anyString(), any(), any()))
        .thenReturn(new GVWHttpResponse(500, null, null, true));

    assertThrows(BadGatewayException.class, () -> deploymentService.startDeployment("dep-id"));

    verify(maintenanceState).setActive(true);
    verify(maintenanceState).setActive(false);
  }

  @Test
  void healthCheck_AppSettingsSucceeds_ReturnsHealthy() {
    HealthResponseDTO result = deploymentService.healthCheck();

    assertEquals(HealthStatus.HEALTHY, result.app());
    assertEquals(HealthStatus.HEALTHY, result.database());
  }

  @Test
  void healthCheck_AppSettingsThrows_ReturnsUnhealthyDatabase() {
    when(appSettingsService.appSettings(any(), any())).thenThrow(new RuntimeException("db down"));

    HealthResponseDTO result = deploymentService.healthCheck();

    assertEquals(HealthStatus.HEALTHY, result.app());
    assertEquals(HealthStatus.UNHEALTHY, result.database());
  }

  @Test
  void updateDeploymentStatus_NullId_ThrowsBadRequestException() {
    assertThrows(
        BadRequestException.class,
        () ->
            deploymentService.updateDeploymentStatus(
                null, new UpdateDeploymentStatusRequestDTO("cancelled", 0)));
  }

  @Test
  void updateDeploymentStatus_BlankId_ThrowsBadRequestException() {
    assertThrows(
        BadRequestException.class,
        () ->
            deploymentService.updateDeploymentStatus(
                "   ", new UpdateDeploymentStatusRequestDTO("cancelled", 0)));
  }

  @Test
  void updateDeploymentStatus_NotFound_ThrowsNotFoundException() {
    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class))).thenReturn(null);

    assertThrows(
        NotFoundException.class,
        () ->
            deploymentService.updateDeploymentStatus(
                "dep-id", new UpdateDeploymentStatusRequestDTO("cancelled", 0)));
  }

  @Test
  void updateDeploymentStatus_InvalidStatus_ThrowsBadRequestException() {
    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.SCHEDULED)
            .updates(new ArrayList<>())
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    assertThrows(
        BadRequestException.class,
        () ->
            deploymentService.updateDeploymentStatus(
                "dep-id", new UpdateDeploymentStatusRequestDTO("scheduled", 0)));
    verify(dbService, never()).update(anyString(), anyString(), any());
  }

  @Test
  void updateDeploymentStatus_Cancelled_UpdatesStatusAndSendsRefresh() {
    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.RUNNING)
            .updates(new ArrayList<>())
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    deploymentService.updateDeploymentStatus(
        "dep-id", new UpdateDeploymentStatusRequestDTO("cancelled", 0));

    assertEquals(DeploymentStatus.CANCELLED, deployment.getStatus());
    assertEquals(1, deployment.getUpdates().size());
    verify(dbService).update(eq("deployments"), eq("dep-id"), any(Deployment.class));
    verify(sseService).sendRefresh("DEPLOYMENT_UPDATES");
  }

  @Test
  void updateDeploymentStatus_SuccessfulCode_UpdatesStatus() {
    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.RUNNING)
            .updates(new ArrayList<>())
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    deploymentService.updateDeploymentStatus(
        "dep-id", new UpdateDeploymentStatusRequestDTO("successful", 1 << 22));

    assertEquals(DeploymentStatus.SUCCESSFUL, deployment.getStatus());
    verify(dbService).update(eq("deployments"), eq("dep-id"), any(Deployment.class));
  }

  @Test
  void updateDeploymentStatus_SuccessfulWithoutSuccessCode_KeepsStatus() {
    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.RUNNING)
            .updates(new ArrayList<>())
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    deploymentService.updateDeploymentStatus(
        "dep-id", new UpdateDeploymentStatusRequestDTO("successful", 0));

    assertEquals(DeploymentStatus.RUNNING, deployment.getStatus());
    assertEquals(1, deployment.getUpdates().size());
    verify(dbService).update(eq("deployments"), eq("dep-id"), any(Deployment.class));
  }

  @Test
  void updateDeploymentStatus_MaintenanceActive_ResetsMaintenance() {
    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.RUNNING)
            .updates(new ArrayList<>())
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);
    when(maintenanceState.getActive()).thenReturn(true);

    deploymentService.updateDeploymentStatus(
        "dep-id", new UpdateDeploymentStatusRequestDTO("failed", 0));

    verify(maintenanceState).setActive(false);
  }

  @Test
  void getDeploymentUpdates_NullId_ThrowsBadRequestException() {
    assertThrows(BadRequestException.class, () -> deploymentService.getDeploymentUpdates(null));
  }

  @Test
  void getDeploymentUpdates_BlankId_ThrowsBadRequestException() {
    assertThrows(BadRequestException.class, () -> deploymentService.getDeploymentUpdates("   "));
  }

  @Test
  void getDeploymentUpdates_ValidId_ReturnsUpdates() {
    Deployment deployment =
        Deployment.builder()
            .id("dep-id")
            .status(DeploymentStatus.SCHEDULED)
            .updates(new ArrayList<>())
            .build();

    when(dbService.findById(anyString(), eq("dep-id"), eq(Deployment.class)))
        .thenReturn(deployment);

    List<Deployment.Update> result = deploymentService.getDeploymentUpdates("dep-id");

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }
}
