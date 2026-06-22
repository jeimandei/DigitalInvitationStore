package id.baundang.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record RsvpRequest(
        @NotBlank String guestName,
        String phone,
        @NotBlank @Pattern(regexp = "hadir|tidak_hadir") String attendance,
        @Positive short guestCount,
        String message
) {}
