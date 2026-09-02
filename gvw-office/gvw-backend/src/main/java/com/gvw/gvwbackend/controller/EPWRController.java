package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.UseEmergencyTokenDTO;
import com.gvw.gvwbackend.service.EPWRService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/emergency")
public class EPWRController {
  private final EPWRService epwrService;

  public EPWRController(EPWRService epwrService) {
    this.epwrService = epwrService;
  }

  @PostMapping("/new")
  public Map<String, Object> getNewEmergencyToken() {
    String token = epwrService.getNewEmergencyToken();
    return Map.of("token", token);
  }

  @PostMapping("/use")
  public Map<String, Object> useEmergencyToken(
      @Valid @RequestBody UseEmergencyTokenDTO useEmergencyTokenDTO) {
    String token = epwrService.useEmergencyToken(useEmergencyTokenDTO.token());
    return Map.of("token", token);
  }
}
