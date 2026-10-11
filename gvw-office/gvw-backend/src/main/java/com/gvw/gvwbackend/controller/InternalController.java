package com.gvw.gvwbackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class InternalController {
  // TODO: Remove this temporary test endpoint
  @GetMapping("/health")
  public String health() {
    return "OK";
  }
}
