package com.gvw.gvwbackend.dto.response;

public record UserManagerResponseDTO(
    String id,
    String rev,
    String name,
    String email,
    String phone,
    String address,
    String type,
    boolean isOrphan) {}
