package id.baundang.invitation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A batch of guests pasted or imported from a spreadsheet. Capped so one request
 * cannot be used to insert an unbounded number of rows; a real guest list fits
 * comfortably inside the limit.
 */
public record GuestImportRequest(
        @NotEmpty @Size(max = 1000) @Valid List<GuestRequest> guests
) {}
