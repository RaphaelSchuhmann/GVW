package com.gvw.gvwbackend.exception;

/**
 * Defines the primary domain/category of an application error.
 *
 * <p>The domain forms the first section of the GVW error code and identifies the affected
 * application area.
 *
 * <p>Error codes are generated using the following format:
 *
 * <pre>
 * DD AA HHH
 * </pre>
 *
 * where:
 *
 * <ul>
 *   <li>{@code DD} - error domain
 *   <li>{@code AA} - error action
 *   <li>{@code HHH} - HTTP status code
 * </ul>
 *
 * <p>When a resource is provided, the extended format is:
 *
 * <pre>
 * DD AA HHH RRR
 * </pre>
 *
 * where {@code RRR} identifies a specific resource inside the domain.
 */
public enum ErrorDomain {
  AUTH(10),
  APP_SETTINGS(20),
  USER(30),
  DASHBOARD(40), // Currently unused, reserved for future dashboard-specific errors
  MEMBER(50),
  EVENTS(60),
  REPORT(70),
  LIBRARY(80),
  FEEDBACK(90),
  BUG_REPORT(11),
  CHANGELOG(12),
  FILE_VALIDATOR(13), // Currently only used by hard-coded file validation responses
  TEXT_EDITOR(14),
  HELP_CENTER(15);

  private final int id;

  ErrorDomain(int id) {
    this.id = id;
  }

  /**
   * Creates an error code without a specific resource.
   *
   * @param action operation that caused the error
   * @param httpStatus HTTP status associated with the error
   * @return generated numeric error code
   */
  public int createCode(ErrorAction action, int httpStatus) {
    String formattedCode = String.format("%02d%02d%03d", this.id, action.getId(), httpStatus);
    return Integer.parseInt(formattedCode);
  }

  /**
   * Creates an error code including a specific resource identifier.
   *
   * @param action operation that caused the error
   * @param httpStatus HTTP status associated with the error
   * @param resource affected resource
   * @return generated numeric error code
   */
  public long createCode(ErrorAction action, int httpStatus, ErrorResource resource) {
    String formattedCode =
        String.format("%02d%02d%03d%03d", this.id, action.getId(), httpStatus, resource.getId());
    return Long.parseLong(formattedCode);
  }
}
