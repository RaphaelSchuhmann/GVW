package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.response.AdminDashboardResponseDTO;
import com.gvw.gvwbackend.dto.response.DashboardResponseDTO;
import com.gvw.gvwbackend.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/user")
  public DashboardResponseDTO getUserDashboard() {
    return dashboardService.getUserDashboardData();
  }

  @GetMapping("/admin")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public AdminDashboardResponseDTO getAdminDashboard() {
    return dashboardService.getAdminDashboardData();
  }
}
