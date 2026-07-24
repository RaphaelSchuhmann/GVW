package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.AddUserAdminRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateUserAdminRequestDTO;
import com.gvw.gvwbackend.dto.response.UserManagerResponsesDTO;
import com.gvw.gvwbackend.dto.response.UserResponseDTO;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.ErrorResource;
import com.gvw.gvwbackend.exception.handler.ErrorContext;
import com.gvw.gvwbackend.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/data")
  public UserResponseDTO getUser(@RequestAttribute("userId") String userId) {
    return userService.getUser(userId);
  }

  @PostMapping("/reset/password/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER')")
  public Map<String, Object> resetPassword(@PathVariable String id) {
    String rev = userService.resetPasswordUsingMemberId(id);
    return Map.of("rev", rev);
  }

  @GetMapping("/admin/users")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public UserManagerResponsesDTO getUsers() {
    return userService.getUsers();
  }

  @PostMapping("/admin/addUser")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN')")
  @ErrorContext(
      domain = ErrorDomain.USER,
      action = ErrorAction.CREATE,
      resource = ErrorResource.NONE)
  public void addUser(@Valid @RequestBody AddUserAdminRequestDTO request) {
    userService.addUser(request);
  }

  @GetMapping("/admin/check/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN')")
  public void checkUser(@PathVariable String id) {
    userService.checkUser(id);
  }

  @PostMapping("/admin/reset/password/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN')")
  public Map<String, Object> adminResetPassword(@PathVariable String id) {
    String rev = userService.resetPasswordUsingUserId(id);
    return Map.of("rev", rev);
  }

  @DeleteMapping("/admin/delete/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN')")
  public void deleteUser(@PathVariable String id) {
    userService.deleteUser(id);
  }

  @PatchMapping("/admin/update")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN')")
  @ErrorContext(
      domain = ErrorDomain.USER,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.NONE)
  public Map<String, Object> updateUser(@Valid @RequestBody UpdateUserAdminRequestDTO requestDTO) {
    String rev = userService.updateUser(requestDTO);
    return Map.of("rev", rev);
  }
}
