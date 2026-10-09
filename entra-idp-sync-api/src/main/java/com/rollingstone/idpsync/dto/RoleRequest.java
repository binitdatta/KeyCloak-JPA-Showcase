package com.rollingstone.idpsync.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleRequest(@NotBlank String roleName) {
}
