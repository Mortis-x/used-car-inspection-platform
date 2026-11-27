package com.usedcar.platform.intention.dto;

import jakarta.validation.constraints.NotNull;

public record CounterRequest(
        @NotNull(message = "请填写还价") Long price
) {
}
