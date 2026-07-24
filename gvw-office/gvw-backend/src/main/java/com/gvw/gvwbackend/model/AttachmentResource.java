package com.gvw.gvwbackend.model;

import java.io.File;
import lombok.Data;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@Data
public class AttachmentResource {
  private final Resource resource;
  private final String contentType;

  // Change the parameter from InputStream to File
  public AttachmentResource(File file, String contentType) {
    this.resource = new FileSystemResource(file);
    this.contentType = contentType;
  }
}
