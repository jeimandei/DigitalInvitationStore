package id.baundang.invitation.dto;

import jakarta.validation.constraints.Positive;

public record CheckInRequest(
        @Positive short actualCount
) {}
