package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.UseEmergencyTokenDTO;
import com.gvw.gvwbackend.dto.response.NewEmergencyTokenDTO;
import com.gvw.gvwbackend.service.EPWRService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/emergency")
public class EPWRController {
  private final EPWRService epwrService;

  public EPWRController(EPWRService epwrService) {
    this.epwrService = epwrService;
  }

  @PostMapping("/new")
  public NewEmergencyTokenDTO getNewEmergencyToken() {
    return epwrService.getNewEmergencyToken();
  }

  @PostMapping("/use")
  public NewEmergencyTokenDTO useEmergencyToken(
      @Valid @RequestBody UseEmergencyTokenDTO useEmergencyTokenDTO) {
    return epwrService.useEmergencyToken(useEmergencyTokenDTO.token());
  }
}
