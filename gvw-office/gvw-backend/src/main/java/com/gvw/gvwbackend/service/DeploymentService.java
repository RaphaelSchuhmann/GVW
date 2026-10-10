package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.AddDeploymentRequestDTO;
import com.gvw.gvwbackend.dto.request.AddMigrationRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateDeploymentInformationRequestDTO;
import com.gvw.gvwbackend.dto.response.DeploymentResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.model.Deployment;
import com.gvw.gvwbackend.model.DeploymentStatus;
import com.gvw.gvwbackend.model.Migration;
import com.gvw.gvwbackend.util.DeploymentValidator;
import com.gvw.gvwbackend.util.HashUtil;
import com.gvw.gvwbackend.util.MigrationValidator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
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
  private static final Logger log = LoggerFactory.getLogger(DeploymentService.class);

  public DeploymentService(
      DbService dbService,
      SseService sseService,
      DeploymentValidator deploymentValidator,
      MigrationValidator migrationValidator,
      HashUtil hashUtil) {
    this.dbService = dbService;
    this.sseService = sseService;
    this.deploymentValidator = deploymentValidator;
    this.migrationValidator = migrationValidator;
    this.hashUtil = hashUtil;
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
      LocalDate inputDate = OffsetDateTime.parse(deployment.getDate()).toLocalDate();

      boolean isToday = inputDate.equals(LocalDate.now());
      boolean isBeforeTargetTime = LocalTime.now().isBefore(deployment.getStartTime());

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
                    m.getError()))
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
        deployment.getError());
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

    Migration newMigration =
        Migration.builder()
            .id(UUID.randomUUID().toString())
            .database(request.database())
            .action(request.action())
            .field(request.field())
            .value(request.value())
            .conditional(request.conditional())
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

    if (deployment.getStatus() != DeploymentStatus.SCHEDULED
        && deployment.getStatus() != DeploymentStatus.CANCELLED
        && deployment.getStatus() != DeploymentStatus.FAILED) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.DEPLOYMENT.createCode(ErrorAction.UPDATE, 400)));
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

    deployment.setRev(request.rev());
    deployment.setTitle(request.title());
    deployment.setAppVersion(request.version());
    deployment.setDate(request.date());
    deployment.setStartTime(request.startTime());
    deployment.setEndTime(request.endTime());

    if (request.commit() != null
        && !request.commit().isBlank()
        && deploymentValidator.isCommitHashValid(request.commit())) {
      // TODO: If the commit was validated automatically run a build

      deployment.setCommit(request.commit());
    }

    dbService.update("deployments", deployment.getId(), deployment);
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

    List<Migration> mutableMigrations = new ArrayList<>(deployment.getMigrations());
    mutableMigrations.removeIf(m -> m.getId().equals(migrationId));
    deployment.setMigrations(mutableMigrations);

    dbService.update("deployments", deployment.getId(), deployment);
  }

  /** Executes a deployment process. (Placeholder implementation) -> TODO */
  public void runDeployment() {}

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
