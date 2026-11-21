package com.gvw.gvwbackend.dto.response;

import com.gvw.gvwbackend.model.Deployment;
import java.util.List;

public record DeploymentUpdatesResponseDTO(List<Deployment.Update> updates, String rev) {}
