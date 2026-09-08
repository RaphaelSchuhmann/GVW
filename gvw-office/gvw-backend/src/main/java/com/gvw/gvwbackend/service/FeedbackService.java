package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.AddFeedbackRequestDTO;
import com.gvw.gvwbackend.dto.response.FeedbackDetailsResponseDTO;
import com.gvw.gvwbackend.dto.response.FeedbackResponseDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.UserFeedback;
import com.gvw.gvwbackend.model.UserReportMetaData;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing user feedback submissions.
 *
 * <p>Handles retrieving feedback entries, resolving feedback details, creating new feedback
 * reports, and deleting existing feedback.
 *
 * <p>Changes to feedback data are broadcast through {@link SseService} so connected clients can
 * refresh their data automatically.
 */
@Service
public class FeedbackService {
  private final DbService dbService;
  private final SseService sseService;
  private final UserService userService;
  private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

  public FeedbackService(DbService dbService, SseService sseService, UserService userService) {
    this.dbService = dbService;
    this.sseService = sseService;
    this.userService = userService;
  }

  /**
   * Retrieves all feedback entries.
   *
   * <p>Returns only summary information intended for feedback list views.
   *
   * @return response containing all available feedback summaries
   */
  public List<FeedbackResponseDTO> getFeedbacks() {
    List<UserFeedback> feedbacks = dbService.findAll("feedbacks", UserFeedback.class);

    if (feedbacks.isEmpty()) {
      return List.of();
    }

    return feedbacks.stream()
        .map(m -> new FeedbackResponseDTO(m.getId(), m.getTitle(), m.getCategory()))
        .toList();
  }

  /**
   * Retrieves detailed information about a single feedback entry.
   *
   * <p>The stored user ID is resolved into an email address before returning the response.
   *
   * @param id database ID of the feedback entry
   * @return detailed feedback information
   * @throws BadRequestException if the ID is missing
   * @throws NotFoundException if no feedback exists with the given ID
   */
  public FeedbackDetailsResponseDTO getFeedbackDetails(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.FEEDBACK.createCode(ErrorAction.READ_ONE, 400)));
    }

    UserFeedback feedback = dbService.findById("feedbacks", id, UserFeedback.class);
    if (feedback == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.FEEDBACK.createCode(ErrorAction.READ_ONE, 404)));
    }

    return new FeedbackDetailsResponseDTO(
        feedback.getTitle(),
        feedback.getCategory(),
        feedback.getMessage(),
        feedback.getSentiment(),
        userService.resolveUserIdToEmail(feedback.getMetaData().getUserId()),
        feedback.getMetaData().getTimestamp(),
        feedback.getMetaData().getAppVersion(),
        feedback.getMetaData().getRoute());
  }

  /**
   * Creates a new feedback entry from a user submission.
   *
   * <p>The current user ID and additional application metadata are stored together with the
   * feedback content. After creation, connected clients are notified through SSE.
   *
   * @param request feedback data submitted by the user
   * @param userId ID of the user creating the feedback
   * @throws BadRequestException if no user ID is provided
   */
  public void addFeedback(AddFeedbackRequestDTO request, String userId) {
    if (userId == null || userId.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.FEEDBACK.createCode(ErrorAction.CREATE, 400)));
    }

    UserFeedback feedback = new UserFeedback();
    feedback.setTitle(request.title());
    feedback.setCategory(request.category());
    feedback.setMessage(request.message());
    feedback.setSentiment(request.sentiment());

    UserReportMetaData metaData = new UserReportMetaData();
    metaData.setUserId(userId);
    metaData.setRoute(request.route());
    metaData.setAppVersion(request.appVersion());
    metaData.setTimestamp(LocalDateTime.now());

    feedback.setMetaData(metaData);

    dbService.insert("feedbacks", feedback);

    sseService.sendRefresh("FEEDBACK");
  }

  /**
   * Deletes an existing feedback entry.
   *
   * <p>After successful deletion, connected clients are notified through SSE.
   *
   * @param id database ID of the feedback entry
   * @throws BadRequestException if the ID is missing
   * @throws NotFoundException if no feedback exists with the given ID
   */
  public void deleteFeedback(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.FEEDBACK.createCode(ErrorAction.DELETE, 400)));
    }

    UserFeedback feedback = dbService.findById("feedbacks", id, UserFeedback.class);
    if (feedback == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.FEEDBACK.createCode(ErrorAction.DELETE, 404)));
    }

    dbService.delete("feedbacks", feedback.getId(), feedback.getRev());

    sseService.sendRefresh("FEEDBACK");
  }
}
