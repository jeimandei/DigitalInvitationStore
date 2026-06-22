package id.baundang.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestbookRequest(
        @NotBlank String guestName,
        @NotBlank @Size(max = 500) String message
) {}
