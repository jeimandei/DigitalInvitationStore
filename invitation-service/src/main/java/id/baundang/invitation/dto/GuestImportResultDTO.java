package id.baundang.invitation.dto;

import java.util.List;

/**
 * Outcome of a bulk import. Duplicates are reported rather than inserted, so
 * re-importing a corrected spreadsheet does not silently double a guest list.
 */
public record GuestImportResultDTO(
        int imported,
        List<String> skippedDuplicates,
        List<GuestDTO> guests
) {}
