package com.gvw.gvwbackend.model;

import java.nio.file.Path;

public record StoredFile(String id, Path path, String originalName, String extension) {}
