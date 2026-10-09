package com.rollingstone.idpsync.dto;

import jakarta.validation.constraints.NotBlank;

public record GroupRequest(@NotBlank String groupName) {
}
