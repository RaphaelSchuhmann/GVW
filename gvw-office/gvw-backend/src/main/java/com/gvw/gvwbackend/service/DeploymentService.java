package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.deploymentService.request.TriggerDeploymentRequestDTO;
import com.gvw.gvwbackend.dto.deploymentService.request.TriggerMigrationDryRunRequestDTO;
import com.gvw.gvwbackend.dto.request.AddDeploymentRequestDTO;
import com.gvw.gvwbackend.dto.request.AddMigrationRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateDeploymentInformationRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateDeploymentStatusRequestDTO;
import com.gvw.gvwbackend.dto.response.DeploymentResponseDTO;
import com.gvw.gvwbackend.dto.response.DeploymentUpdatesResponseDTO;
import com.gvw.gvwbackend.dto.response.HealthResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.model.Deployment;
import com.gvw.gvwbackend.model.DeploymentStatus;
import com.gvw.gvwbackend.model.Migration;
import com.gvw.gvwbackend.model.deploymentSystem.HealthStatus;
import com.gvw.gvwbackend.model.helper.GVWHttpResponse;
import com.gvw.gvwbackend.state.MaintenanceState;
import com.gvw.gvwbackend.util.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service class handling business logic for deployments and their associated migrations, including
 * validation, database persistence, and SSE notification triggers.
 */
@Service
public class DeploymentService {
  private final DbService dbService;
  private final SseService sseService;
  private final DeploymentValidator deploymentValidator;
  private final MigrationValidator migrationValidator;
  private final HashUtil hashUtil;
  private final Clock clock;
  private final HttpUtil httpUtil;
  private final AppSettingsService appSettingsService;
  private final MaintenanceState maintenanceState;
  private static final Logger log = LoggerFactory.getLogger(DeploymentService.class);

  private static final int DEPLOYMENT_SUCCESSFUL_CODE = 1 << 22;
  private static final Set<DeploymentStatus> ALLOWED_UPDATE_STATUSES =
      EnumSet.of(DeploymentStatus.SUCCESSFUL, DeploymentStatus.CANCELLED, DeploymentStatus.FAILED);

  /**
   * Creates the deployment service using the system UTC clock.
   *
   * @param dbService service for database persistence
   * @param sseService service for SSE client notifications
   * @param deploymentValidator validator for deployment time constraints
   * @param migrationValidator validator for migration rules
   * @param hashUtil utility for generating deployment hashes
   * @param httpUtil utility for calling the deployment pipeline
   * @param appSettingsService service for application settings access
   * @param maintenanceState shared maintenance mode state
   */
  @org.springframework.beans.factory.annotation.Autowired
  public DeploymentService(
      DbService dbService,
      SseService sseService,
      DeploymentValidator deploymentValidator,
      MigrationValidator migrationValidator,
      HashUtil hashUtil,
      HttpUtil httpUtil,
      AppSettingsService appSettingsService,
      MaintenanceState maintenanceState) {
    this(
        dbService,
        sseService,
        deploymentValidator,
        migrationValidator,
        hashUtil,
        Clock.systemUTC(),
        httpUtil,
        appSettingsService,
        maintenanceState);
  }

  /**
   * Creates the deployment service with an injectable clock, primarily for testing.
   *
   * @param dbService service for database persistence
   * @param sseService service for SSE client notifications
   * @param deploymentValidator validator for deployment time constraints
   * @param migrationValidator validator for migration rules
   * @param hashUtil utility for generating deployment hashes
   * @param clock clock used for time-based deployment queries
   * @param httpUtil utility for calling the deployment pipeline
   * @param appSettingsService service for application settings access
   * @param maintenanceState shared maintenance mode state
   */
  public DeploymentService(
      DbService dbService,
      SseService sseService,
      DeploymentValidator deploymentValidator,
      MigrationValidator migrationValidator,
      HashUtil hashUtil,
      Clock clock,
      HttpUtil httpUtil,
      AppSettingsService appSettingsService,
      MaintenanceState maintenanceState) {
    this.dbService = dbService;
    this.sseService = sseService;
    this.deploymentValidator = deploymentValidator;
    this.migrationValidator = migrationValidator;
    this.hashUtil = hashUtil;
    this.clock = clock;
    this.httpUtil = httpUtil;
    this.appSettingsService = appSettingsService;
    this.maintenanceState = maintenanceState;
  }

  /**
   * Retrieves a list of formatted time intervals for scheduled deployments occurring today where
   * the start time has not yet passed.
   *
   * @return A list of strings representing deployment time ranges (e.g. "14:00 - 15:00").
   */
  public List<String> getTodayDeployments() {
    Map<String, Object> query = Map.of("selector", Map.of("status", "SCHEDULED"));
    List<Deployment> deployments = dbService.findByQuery("deployments", query, Deployment.class);

    if (deployments == null || deployments.isEmpty()) {
      return List.of();
    }

    List<String> times = new ArrayList<>();

    for (Deployment deployment : deployments) {
      LocalDate inputDate =
          OffsetDateTime.parse(deployment.getDate())
              .atZoneSameInstant(ZoneOffset.UTC)
              .toLocalDate();

      boolean isToday = inputDate.equals(LocalDate.now(clock.withZone(ZoneOffset.UTC)));
      boolean isBeforeTargetTime =
          LocalTime.now(clock.withZone(ZoneOffset.UTC)).isBefore(deployment.getStartTime());

      if (!isToday || !isBeforeTargetTime) continue;

      String startTime = deployment.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
      String endTime = deployment.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));

      times.add(startTime + " - " + endTime);
    }

    return times;
  }

  /**
   * Retrieves all deployments stored in the database mapped to response DTOs.
   *
   * @return A list of all deployment response DTOs, or an empty list if none exist.
   */
  public List<DeploymentResponseDTO> getAllDeployments() {
    List<Deployment> deployments = dbService.findAll("deployments", Deployment.class);

    if (deployments == null || deployments.isEmpty()) {
      return List.of();
    }

    return deployments.stream()
        .map(
            m ->
                new DeploymentResponseDTO(
                    m.getId(),
                    m.getRev(),
                    m.getTitle(),
                    m.getStatus(),
                    m.getDate(),
                    m.getStartTime(),
                    m.getEndTime(),
                    m.getCommit(),
                    m.getHash(),
                    m.getMigrations(),
                    m.getAppVersion(),
                    m.getUpdates()))
        .toList();
  }

  /**
   * Retrieves a specific deployment by its unique identifier.
   *
   * @param id The unique identifier of the deployment.
   * @return The deployment response DTO.
   * @throws NotFoundException if the deployment does not exist.
   */
  public DeploymentResponseDTO getDeployment(String id) {
    Deployment deployment = findDeploymentById(id);

    return new DeploymentResponseDTO(
        deployment.getId(),
        deployment.getRev(),
        deployment.getTitle(),
        deployment.getStatus(),
        deployment.getDate(),
        deployment.getStartTime(),
        deployment.getEndTime(),
        deployment.getCommit(),
        deployment.getHash(),
        deployment.getMigrations(),
        deployment.getAppVersion(),
        deployment.getUpdates());
  }

  /**
   * Validates the existence of a deployment by its ID without returning it.
   *
   * @param id The unique identifier of the deployment.
   * @throws NotFoundException if the deployment does not exist.
   */
  public void checkDeployment(String id) {
    findDeploymentById(id);
  }

  /**
   * Creates and persists a new deployment after validating time constraints and overlaps. Triggers
   * an SSE refresh event upon successful creation.
   *
   * @param request The deployment creation request DTO.
   * @throws BadRequestException if the deployment time is invalid, already started, or overlaps
   *     with another.
   */
  public void addDeployment(AddDeploymentRequestDTO request) {
    List<Deployment> deployments = dbService.findAll("deployments", Deployment.class);

    if (deploymentValidator.isTodayAndAlreadyStarted(request.date(), request.startTime())) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.CREATE, 400)));
    }

    // Check if the time of the new deployment has any time overlaps with other deployments
    if (deploymentValidator.hasOverlap(
        deployments, request.date(), request.startTime(), request.endTime())) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.CREATE, 400)));
    }

    if (!request.endTime().isAfter(request.startTime())) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.UPDATE, 400)));
    }

    Deployment deployment =
        Deployment.builder()
            .title(request.title())
            .appVersion(request.version())
            .date(request.date())
            .startTime(request.startTime())
            .endTime(request.endTime())
            .status(DeploymentStatus.SCHEDULED)
            .hash(hashUtil.generateShortHash())
            .build();

    dbService.insert("deployments", deployment);
    sseService.sendRefresh("DEPLOYMENTS");
  }

  /**
   * Adds a migration item to an existing deployment after field and sequence validation.
   *
   * @param request The migration creation request DTO.
   * @throws BadRequestException if the migration fields or sequence are invalid.
   */
  public void addMigration(AddMigrationRequestDTO request) {
    if (!migrationValidator.areMigrationFieldsValid(
        request.action(), request.field(), request.value(), request.conditional())) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.DEPLOYMENT.createCode(ErrorAction.CREATE, 400, ErrorResource.MIGRATION)));
    }

    Deployment deployment = findDeploymentById(request.deploymentId());

    if (deployment.getStatus() == DeploymentStatus.RUNNING) {
      throw new ForbiddenException(
          String.valueOf(
              ErrorDomain.DEPLOYMENT.createCode(ErrorAction.CREATE, 403, ErrorResource.MIGRATION)));
    }

    Migration newMigration =
        Migration.builder()
            .id(UUID.randomUUID().toString())
            .database(request.database())
            .action(request.action())
            .field(request.field())
            .value(request.value())
            .conditional(request.conditional())
                .defaultValue(request.defaultValue())
            .build();

    MigrationValidator.ValidationResult validationResult =
        migrationValidator.validateMigrationSequence(deployment.getMigrations(), newMigration);
    if (!validationResult.isValid()) {
      log.error("Invalid migration request: {}", validationResult.errorMessage());
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.DEPLOYMENT.createCode(ErrorAction.UPDATE, 400, ErrorResource.MIGRATION)));
    }

    deployment.setRev(request.rev());

    List<Migration> migrations = new ArrayList<>(deployment.getMigrations());
    migrations.add(newMigration);
    deployment.setMigrations(migrations);

    dbService.update("deployments", deployment.getId(), deployment);

    sseService.sendRefresh("DEPLOYMENTS");
  }

  /**
   * Updates an existing deployment's information if its status allows modification and time overlap
   * constraints are satisfied.
   *
   * @param request The update deployment information request DTO.
   * @throws BadRequestException if the deployment status prohibits editing, time is invalid, or
   *     overlaps exist.
   */
  public void updateDeploymentInformation(UpdateDeploymentInformationRequestDTO request) {
    Deployment deployment = findDeploymentById(request.id());

    List<Deployment> allDeployments = dbService.findAll("deployments", Deployment.class);
    List<Deployment> filteredDeployments =
        allDeployments.stream()
            .filter(dep -> !Objects.equals(dep.getId(), deployment.getId()))
            .toList();

    if (deployment.getStatus() == DeploymentStatus.RUNNING) {
      throw new ForbiddenException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.UPDATE, 403)));
    }

    if (deploymentValidator.isTodayAndAlreadyStarted(request.date(), request.startTime())) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.UPDATE, 400)));
    }

    // Check if the time of the new deployment has any time overlaps with other deployments
    if (deploymentValidator.hasOverlap(
        filteredDeployments, request.date(), request.startTime(), request.endTime())) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.UPDATE, 400)));
    }

    if (!request.endTime().isAfter(request.startTime())) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.UPDATE, 400)));
    }

    deployment.setRev(request.rev());
    deployment.setTitle(request.title());
    deployment.setAppVersion(request.version());
    deployment.setDate(request.date());
    deployment.setStartTime(request.startTime());
    deployment.setEndTime(request.endTime());

    boolean shouldStartBuild = false;

    // Only do build when saved commit and new commit differ!
    if (request.commit() != null
        && !request.commit().isBlank()
        && !Objects.equals(deployment.getCommit(), request.commit())) {
      if (!deploymentValidator.isCommitHashValid(request.commit())) {
        log.debug("Invalid commit hash found {}", request.commit());
        throw new BadRequestException(
            String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.UPDATE, 400)));
      }

      shouldStartBuild = true;
    }

    if (request.commit() != null && !request.commit().isBlank()) {
      deployment.setCommit(request.commit());
    }

    dbService.update("deployments", deployment.getId(), deployment);

    // Only trigger builds after the database update finished successfully to avoid races
    if (shouldStartBuild) {
      GVWHttpResponse resp =
          httpUtil.sendPostRequestWithoutBody(
              "http://localhost:3501/build/" + deployment.getId() + "/" + request.commit(),
              String.class);
      if (resp.isError()) {
        // Not a problem as the deployment pipeline would run the build pipeline if no build was
        // found
        log.debug("Build request responded with error code {} instead of accepted", resp.status());
      }
    }

    sseService.sendRefresh("DEPLOYMENTS");
  }

  /**
   * Deletes a deployment by its identifier and triggers an SSE refresh event.
   *
   * @param id The unique identifier of the deployment to remove.
   */
  public void removeDeployment(String id) {
    Deployment deployment = findDeploymentById(id);

    dbService.delete("deployments", deployment.getId(), deployment.getRev());
    sseService.sendRefresh("DEPLOYMENTS");
  }

  /**
   * Removes a specific migration from a deployment.
   *
   * @param deploymentId The unique identifier of the parent deployment.
   * @param migrationId The unique identifier of the migration to remove.
   * @throws BadRequestException if the migration ID is blank or missing.
   */
  public void removeMigration(String deploymentId, String migrationId) {
    if (migrationId == null || migrationId.isBlank()) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.DEPLOYMENT.createCode(ErrorAction.DELETE, 400, ErrorResource.MIGRATION)));
    }

    Deployment deployment = findDeploymentById(deploymentId);

    if (deployment.getStatus() == DeploymentStatus.RUNNING) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.DEPLOYMENT.createCode(ErrorAction.DELETE, 400, ErrorResource.MIGRATION)));
    }

    List<Migration> mutableMigrations = new ArrayList<>(deployment.getMigrations());
    mutableMigrations.removeIf(m -> m.getId().equals(migrationId));
    deployment.setMigrations(mutableMigrations);

    dbService.update("deployments", deployment.getId(), deployment);

    sseService.sendRefresh("DEPLOYMENTS");
  }

  /**
   * Triggers a migration dry-run on the deployment pipeline for the given deployment.
   *
   * <p>Does nothing if the deployment has no migrations. Throws {@link BadGatewayException} if the
   * pipeline request fails.
   *
   * @param deploymentId The unique identifier of the deployment.
   * @throws BadRequestException if the deployment ID is null or blank.
   * @throws NotFoundException if the deployment does not exist.
   * @throws BadGatewayException if the pipeline rejects the dry-run request.
   */
  public void startMigrationDryRun(String deploymentId) {
    if (deploymentId == null || deploymentId.isBlank()) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.DEPLOYMENT.createCode(
                  ErrorAction.UTILITY, 400, ErrorResource.MIGRATION)));
    }

    Deployment deployment = findDeploymentById(deploymentId);

    List<Migration> migrations = deployment.getMigrations();

    if (migrations == null || migrations.isEmpty()) {
      // No migrations found, don't trigger a dry run
      return;
    }

    GVWHttpResponse resp =
        httpUtil.sendPostRequestWithBody(
            "http://localhost:3501/migration/test/" + deploymentId,
            new TriggerMigrationDryRunRequestDTO(deployment.getMigrations()),
            null);

    if (resp.isError()) {
      log.debug("Failed to trigger migration dry run. Responded with {}", resp.status());

      throw new BadGatewayException(
          String.valueOf(
              ErrorDomain.DEPLOYMENT.createCode(
                  ErrorAction.UTILITY, 502, ErrorResource.MIGRATION)));
    }
  }

  /**
   * Starts a deployment by activating maintenance mode, notifying connected clients via a kill
   * signal, and triggering the deployment pipeline.
   *
   * <p>Maintenance mode is deactivated again if the pipeline request fails.
   *
   * @param deploymentId The unique identifier of the deployment to start.
   * @throws BadRequestException if the deployment ID is null or blank.
   * @throws NotFoundException if the deployment does not exist.
   * @throws BadGatewayException if the pipeline rejects the deployment request.
   */
  public void startDeployment(String deploymentId) {
    if (deploymentId == null || deploymentId.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.UTILITY, 400)));
    }

    Deployment deployment = findDeploymentById(deploymentId);

    // Send kill signal for shutdown
    maintenanceState.setActive(true);
    sseService.sendKillSignal();

    TriggerDeploymentRequestDTO body =
        new TriggerDeploymentRequestDTO(deployment.getMigrations(), deployment.getAppVersion());
    GVWHttpResponse resp =
        httpUtil.sendPostRequestWithBody(
            "http://localhost:3501/deploy/" + deploymentId + "/" + deployment.getCommit(),
            body,
            null);

    if (resp.isError()) {
      log.debug(
          "Failed to start deployment for deploymentId {}. Responded with {}",
          deployment,
          resp.status());
      maintenanceState.setActive(false);
      throw new BadGatewayException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.UTILITY, 502)));
    }
  }

  /**
   * Performs a health check of the application and its database dependency.
   *
   * @return A {@link HealthResponseDTO} indicating the health status of the app and database.
   */
  public HealthResponseDTO healthCheck() {
    try {
      appSettingsService.appSettings(ErrorAction.UTILITY, ErrorResource.NONE);
    } catch (Exception e) {
      // Any exception means that the database action was unsuccessful so database is unhealthy
      return new HealthResponseDTO(HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
    }

    return new HealthResponseDTO(HealthStatus.HEALTHY, HealthStatus.HEALTHY);
  }

  /**
   * Updates the status of a deployment based on a pipeline status callback and appends the
   * corresponding status messages as deployment updates.
   *
   * <p>Only {@code SUCCESSFUL}, {@code CANCELLED}, and {@code FAILED} are accepted as target
   * statuses. {@code CANCELLED} and {@code FAILED} always apply, while {@code SUCCESSFUL} only
   * changes the deployment status when the reason code signals a successful deployment. Maintenance
   * mode is always deactivated after a status update.
   *
   * @param deploymentId The unique identifier of the deployment.
   * @param request The status update request containing the new status and reason code.
   * @throws BadRequestException if the deployment ID is invalid or the status is not allowed.
   * @throws NotFoundException if the deployment does not exist.
   */
  public void updateDeploymentStatus(
      String deploymentId, UpdateDeploymentStatusRequestDTO request) {
    if (deploymentId == null || deploymentId.isBlank()) {
      throw invalidUpdateRequest();
    }

    Deployment deployment = findDeploymentById(deploymentId);

    DeploymentStatus requestedStatus = DeploymentStatus.fromString(request.status());
    if (!ALLOWED_UPDATE_STATUSES.contains(requestedStatus)) {
      log.warn(
          "Received deployment status update with status {}, which is not valid for this endpoint",
          request.status());
      throw invalidUpdateRequest();
    }

    applyStatusUpdate(deployment, requestedStatus, request.reason());

    // The maintenance state is always reset after a deployment update
    maintenanceState.setActive(false);

    deployment.addUpdate(ErrorCodeMapper.getMessages(request.reason()));

    dbService.update("deployments", deployment.getId(), deployment);
    sseService.sendRefresh("DEPLOYMENT_UPDATES");
  }

  /**
   * Applies a requested status update to a deployment according to the pipeline status rules.
   *
   * @param deployment The deployment to update.
   * @param requested The requested target status.
   * @param reason The Reason code accompanying the status update.
   */
  private void applyStatusUpdate(Deployment deployment, DeploymentStatus requested, int reason) {
    boolean successful = requested == DeploymentStatus.SUCCESSFUL;

    if (successful && deployment.getStatus() != DeploymentStatus.RUNNING) {
      deployment.setStatus(DeploymentStatus.SCHEDULED);
    } else if (!successful || reason == DEPLOYMENT_SUCCESSFUL_CODE) {
      // CANCELLED/FAILED always apply, SUCCESSFUL only with the success reason code
      deployment.setStatus(requested);
    }
  }

  /**
   * Creates a {@link BadRequestException} for invalid deployment status update requests.
   *
   * @return A BadRequestException with the deployment update error code.
   */
  private BadRequestException invalidUpdateRequest() {
    return new BadRequestException(
        String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.UPDATE, 400)));
  }

  /**
   * Retrieves all recorded status updates for a deployment along with its current revision.
   *
   * @param deploymentId The unique identifier of the deployment.
   * @return A {@link DeploymentUpdatesResponseDTO} containing the deployment's updates and rev.
   * @throws BadRequestException if the deployment ID is null or blank.
   * @throws NotFoundException if the deployment does not exist.
   */
  public DeploymentUpdatesResponseDTO getDeploymentUpdates(String deploymentId) {
    if (deploymentId == null || deploymentId.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.READ_ONE, 400)));
    }

    Deployment deployment = findDeploymentById(deploymentId);

    return new DeploymentUpdatesResponseDTO(deployment.getUpdates(), deployment.getRev());
  }

  /**
   * Helper method to find a deployment by its ID or throw appropriate exceptions if invalid or not
   * found.
   *
   * @param id The unique identifier of the deployment.
   * @return The found {@link Deployment} entity.
   * @throws BadRequestException if the ID is null or blank.
   * @throws NotFoundException if no deployment matches the given ID.
   */
  private Deployment findDeploymentById(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.READ_ONE, 400)));
    }

    Deployment deployment = dbService.findById("deployments", id, Deployment.class);
    if (deployment == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.READ_ONE, 404)));
    }

    return deployment;
  }
}
