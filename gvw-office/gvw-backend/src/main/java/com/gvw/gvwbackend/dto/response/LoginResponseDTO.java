package com.gvw.gvwbackend.dto.response;

public record LoginResponseDTO(
    String authToken, boolean changePassword, boolean firstLogin, String rev) {}
