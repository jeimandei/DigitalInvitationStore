package id.baundang.invitation.service;

import id.baundang.invitation.domain.Guest;
import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.dto.GuestImportResultDTO;
import id.baundang.invitation.dto.GuestRequest;
import id.baundang.invitation.repository.GiftAccountRepository;
import id.baundang.invitation.repository.GiftConfirmationRepository;
import id.baundang.invitation.repository.GiftRepository;
import id.baundang.invitation.repository.GuestRepository;
import id.baundang.invitation.repository.GuestbookEntryRepository;
import id.baundang.invitation.repository.InvitationRepository;
import id.baundang.invitation.repository.RsvpResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Guest lists arrive as spreadsheets and get corrected and re-imported, so the
 * de-duplication behaviour is the part that matters here.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuestImportTest {

    private static final UUID INVITATION_ID = UUID.randomUUID();

    @Mock
    InvitationRepository invitationRepository;
    @Mock
    RsvpResponseRepository rsvpRepository;
    @Mock
    GuestbookEntryRepository guestbookRepository;
    @Mock
    GiftAccountRepository giftAccountRepository;
    @Mock
    GiftConfirmationRepository giftConfirmationRepository;
    @Mock
    GuestRepository guestRepository;
    @Mock
    GiftRepository giftRepository;
    @Mock
    RabbitTemplate rabbitTemplate;
    @Mock
    GiftFeeCalculator giftFeeCalculator;

    @InjectMocks
    InvitationService invitationService;

    private final List<Guest> existing = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Invitation inv = new Invitation();
        inv.setId(INVITATION_ID);
        when(invitationRepository.findById(INVITATION_ID)).thenReturn(Optional.of(inv));
        when(guestRepository.findAllByInvitationIdOrderByNameAsc(INVITATION_ID)).thenReturn(existing);
        when(guestRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Guest guestNamed(String name) {
        Guest g = new Guest();
        g.setId(UUID.randomUUID());
        g.setName(name);
        g.setInviteCode("code-" + name);
        g.setAllottedCount((short) 1);
        return g;
    }

    private GuestRequest req(String name) {
        return new GuestRequest(name, null, null, (short) 1);
    }

    @SuppressWarnings("unchecked")
    private List<Guest> captureSaved() {
        ArgumentCaptor<List<Guest>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(guestRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void importsEveryNewGuest() {
        GuestImportResultDTO result = invitationService.importGuests(
                INVITATION_ID, List.of(req("Budi Santoso"), req("Sari Dewi")));

        assertEquals(2, result.imported());
        assertTrue(result.skippedDuplicates().isEmpty());
        assertEquals(2, captureSaved().size());
    }

    @Test
    void everyImportedGuestGetsItsOwnInviteCode() {
        invitationService.importGuests(INVITATION_ID, List.of(req("Budi"), req("Sari")));

        List<Guest> saved = captureSaved();
        assertEquals(2, saved.stream().map(Guest::getInviteCode).distinct().count());
    }

    @Test
    void skipsNamesAlreadyOnTheInvitation() {
        // Re-importing a corrected spreadsheet must not double the guest list.
        existing.add(guestNamed("Budi Santoso"));

        GuestImportResultDTO result = invitationService.importGuests(
                INVITATION_ID, List.of(req("Budi Santoso"), req("Sari Dewi")));

        assertEquals(1, result.imported());
        assertEquals(List.of("Budi Santoso"), result.skippedDuplicates());
    }

    @Test
    void duplicateMatchingIgnoresCaseAndSpacing() {
        existing.add(guestNamed("Budi Santoso"));

        GuestImportResultDTO result = invitationService.importGuests(
                INVITATION_ID, List.of(req("  budi   santoso ")));

        assertEquals(0, result.imported());
        assertEquals(1, result.skippedDuplicates().size());
    }

    @Test
    void repeatedRowsWithinOneBatchAreCollapsed() {
        // A spreadsheet that lists someone twice should still yield one guest.
        GuestImportResultDTO result = invitationService.importGuests(
                INVITATION_ID, List.of(req("Budi"), req("Budi"), req("Sari")));

        assertEquals(2, result.imported());
        assertEquals(List.of("Budi"), result.skippedDuplicates());
    }

    @Test
    void blankNamesAreDroppedWithoutBeingReportedAsDuplicates() {
        GuestImportResultDTO result = invitationService.importGuests(
                INVITATION_ID, List.of(req("   "), req("Sari")));

        assertEquals(1, result.imported());
        assertTrue(result.skippedDuplicates().isEmpty());
    }

    @Test
    void nonPositiveAllotmentFallsBackToOneSeat() {
        invitationService.importGuests(INVITATION_ID,
                List.of(new GuestRequest("Budi", null, null, (short) 0)));

        assertEquals((short) 1, captureSaved().get(0).getAllottedCount());
    }

    @Test
    void namesAreStoredTrimmed() {
        invitationService.importGuests(INVITATION_ID, List.of(req("  Budi Santoso  ")));

        assertEquals("Budi Santoso", captureSaved().get(0).getName());
    }
}
