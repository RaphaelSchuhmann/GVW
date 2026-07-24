package com.gvw.gvwbackend.dto.response;

public record UserResponseDTO(
    String email, String role, String name, String address, String phone, String rev) {}
