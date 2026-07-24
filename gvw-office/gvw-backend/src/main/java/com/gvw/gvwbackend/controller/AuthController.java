package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.ChangePwRequestDTO;
import com.gvw.gvwbackend.dto.request.LoginRequestDTO;
import com.gvw.gvwbackend.dto.response.AutoLoginResponseDTO;
import com.gvw.gvwbackend.dto.response.LoginResponseDTO;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.ErrorResource;
import com.gvw.gvwbackend.exception.handler.ErrorContext;
import com.gvw.gvwbackend.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  @ErrorContext(domain = ErrorDomain.AUTH, action = ErrorAction.AUTH, resource = ErrorResource.NONE)
  public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO loginRequest) {
    return authService.login(loginRequest);
  }

  @PostMapping("/changePw")
  @ResponseStatus(HttpStatus.OK)
  @ErrorContext(
      domain = ErrorDomain.AUTH,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.NONE)
  public Map<String, Object> changePw(@Valid @RequestBody ChangePwRequestDTO changePwRequest) {
    String rev = authService.changePassword(changePwRequest);
    return Map.of("rev", rev);
  }

  @GetMapping("/auto")
  @ErrorContext(domain = ErrorDomain.AUTH, action = ErrorAction.AUTH, resource = ErrorResource.NONE)
  public AutoLoginResponseDTO autoLogin(@RequestAttribute("userId") String userId) {
    return authService.autoLogin(userId);
  }
}
