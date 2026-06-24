package id.baundang.invitation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import id.baundang.common.exception.NotFoundException;
import id.baundang.invitation.domain.Gift;
import id.baundang.invitation.domain.GiftAccount;
import id.baundang.invitation.domain.GiftConfirmation;
import id.baundang.invitation.domain.Guest;
import id.baundang.invitation.domain.GuestbookEntry;
import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.domain.Invitation.InvitationStatus;
import id.baundang.invitation.domain.RsvpResponse;
import id.baundang.invitation.dto.AttendanceDTO;
import id.baundang.invitation.dto.CheckInRequest;
import id.baundang.invitation.dto.EventDTO;
import id.baundang.invitation.dto.ExpiringInvitationDTO;
import id.baundang.invitation.dto.GiftAccountDTO;
import id.baundang.invitation.dto.GiftAccountRequest;
import id.baundang.invitation.dto.GiftConfirmRequest;
import id.baundang.invitation.dto.GuestDTO;
import id.baundang.invitation.dto.GuestRequest;
import id.baundang.invitation.dto.GuestbookEntryDTO;
import id.baundang.invitation.dto.GuestbookRequest;
import id.baundang.invitation.dto.RsvpRequest;
import id.baundang.invitation.repository.GiftAccountRepository;
import id.baundang.invitation.repository.GiftConfirmationRepository;
import id.baundang.invitation.repository.GiftRepository;
import id.baundang.invitation.repository.GuestbookEntryRepository;
import id.baundang.invitation.repository.GuestRepository;
import id.baundang.invitation.repository.InvitationRepository;
import id.baundang.invitation.repository.RsvpResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final RsvpResponseRepository rsvpRepository;
    private final GuestbookEntryRepository guestbookRepository;
    private final GiftAccountRepository giftAccountRepository;
    private final GiftConfirmationRepository giftConfirmationRepository;
    private final GuestRepository guestRepository;
    private final GiftRepository giftRepository;
    private final RabbitTemplate rabbitTemplate;

    // Self-injection so @Cacheable on getBySlug is honoured when called internally
    @Autowired @Lazy
    private InvitationService self;

    @Value("${app.rabbitmq.rsvp-exchange}")
    private String rsvpExchange;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional(readOnly = true)
    @Cacheable(value = "invitations", key = "'inv:' + #slug")
    public Invitation getBySlug(String slug) {
        return invitationRepository.findByCoupleSlug(slug)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + slug));
    }

    @Transactional
    public Invitation getBySlugAndIncrementView(String slug) {
        Invitation inv = self.getBySlug(slug); // via proxy to hit cache
        invitationRepository.incrementViewCount(inv.getId());
        return inv;
    }

    @Transactional
    public void submitRsvp(String slug, RsvpRequest req) {
        Invitation inv = getBySlug(slug);

        RsvpResponse rsvp = new RsvpResponse();
        rsvp.setInvitation(inv);
        rsvp.setGuestName(req.guestName());
        rsvp.setPhone(req.phone());
        rsvp.setAttendance(req.attendance());
        rsvp.setGuestCount(req.guestCount());
        rsvp.setMessage(req.message());
        rsvpRepository.save(rsvp);

        // Build event for notification-service
        JsonNode content = inv.getContent();
        String coupleWa  = content != null && content.hasNonNull("contactWhatsapp")
                ? content.get("contactWhatsapp").asText("") : "";
        String title     = content != null && content.hasNonNull("coupleName")
                ? content.get("coupleName").asText(slug) : slug;

        try {
            rabbitTemplate.convertAndSend(rsvpExchange, "rsvp.submitted", Map.of(
                    "guestName", req.guestName(),
                    "attendance", req.attendance(),
                    "guestCount", (int) req.guestCount(),
                    "invitationTitle", title,
                    "coupleWhatsapp", coupleWa,
                    "occurredAt", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to publish rsvp.submitted: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "guestbooks", key = "'gb:' + #slug")
    public List<GuestbookEntryDTO> listApprovedGuestbook(String slug) {
        Invitation inv = self.getBySlug(slug);
        return guestbookRepository
                .findAllByInvitationIdAndApprovedTrueOrderByCreatedAtDesc(inv.getId())
                .stream().map(GuestbookEntryDTO::from).toList();
    }

    @Transactional
    public void submitGuestbook(String slug, GuestbookRequest req) {
        Invitation inv = getBySlug(slug);
        GuestbookEntry entry = new GuestbookEntry();
        entry.setInvitation(inv);
        entry.setGuestName(req.guestName());
        entry.setMessage(req.message());
        entry.setApproved(false);
        guestbookRepository.save(entry);
    }

    @Transactional
    @CacheEvict(value = "guestbooks", allEntries = true)
    public void approveGuestbook(UUID invitationId, UUID entryId) {
        GuestbookEntry entry = guestbookRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Guestbook entry not found: " + entryId));
        if (!entry.getInvitation().getId().equals(invitationId)) {
            throw new NotFoundException("Entry does not belong to this invitation");
        }
        entry.setApproved(true);
        guestbookRepository.save(entry);
    }

    @Transactional
    @CacheEvict(value = {"invitations", "guestbooks"}, allEntries = true)
    public Invitation updateContent(UUID id, JsonNode patch) {
        Invitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + id));
        JsonNode existing = inv.getContent();
        if (existing != null && existing.isObject() && patch.isObject()) {
            ObjectNode merged = (ObjectNode) existing.deepCopy();
            merged.setAll((ObjectNode) patch);
            inv.setContent(merged);
        } else {
            inv.setContent(patch);
        }
        return invitationRepository.save(inv);
    }

    @Transactional
    public Invitation updateStatus(UUID id, InvitationStatus status) {
        Invitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + id));
        inv.setStatus(status);
        return invitationRepository.save(inv);
    }

    @Transactional(readOnly = true)
    public List<ExpiringInvitationDTO> findExpiring(int days) {
        LocalDate from = LocalDate.now();
        LocalDate to   = from.plusDays(days);
        return invitationRepository.findExpiringBetween(from, to)
                .stream().map(ExpiringInvitationDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EventDTO> getEvents(String slug) {
        Invitation inv = getBySlug(slug);
        JsonNode content = inv.getContent();
        if (content == null || !content.hasNonNull("events") || !content.get("events").isArray()) {
            return List.of();
        }
        List<EventDTO> events = new ArrayList<>();
        for (JsonNode node : content.get("events")) {
            events.add(EventDTO.from(node));
        }
        return events;
    }

    @Transactional(readOnly = true)
    public GiftAccountDTO getGiftAccount(String slug) {
        Invitation inv = getBySlug(slug);
        return giftAccountRepository.findByInvitationId(inv.getId())
                .map(GiftAccountDTO::from)
                .orElse(new GiftAccountDTO(null, null, null, null, null, null));
    }

    @Transactional
    public void setGiftAccount(UUID invitationId, GiftAccountRequest req) {
        Invitation inv = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + invitationId));
        GiftAccount account = giftAccountRepository.findByInvitationId(invitationId)
                .orElseGet(() -> {
                    GiftAccount a = new GiftAccount();
                    a.setInvitation(inv);
                    return a;
                });
        account.setBankName(req.bankName());
        account.setAccountNumber(req.accountNumber());
        account.setAccountHolder(req.accountHolder());
        account.setGopayNumber(req.gopayNumber());
        account.setOvoNumber(req.ovoNumber());
        account.setQrisImageUrl(req.qrisImageUrl());
        giftAccountRepository.save(account);
    }

    @Transactional
    public void submitGiftConfirmation(String slug, GiftConfirmRequest req) {
        Invitation inv = getBySlug(slug);

        GiftConfirmation confirmation = new GiftConfirmation();
        confirmation.setInvitation(inv);
        confirmation.setSenderName(req.senderName());
        confirmation.setAmount(req.amount());
        confirmation.setBankFrom(req.bankFrom());
        confirmation.setTransferProofUrl(req.proofUrl());
        confirmation.setMessage(req.message());
        giftConfirmationRepository.save(confirmation);

        JsonNode content  = inv.getContent();
        String coupleWa   = content != null && content.hasNonNull("contactWhatsapp")
                ? content.get("contactWhatsapp").asText("") : "";
        String coupleName = content != null && content.hasNonNull("coupleName")
                ? content.get("coupleName").asText(slug) : slug;

        try {
            rabbitTemplate.convertAndSend("baundang.rsvp", "gift.confirmed", Map.of(
                    "invitationSlug", slug,
                    "coupleName", coupleName,
                    "coupleWhatsapp", coupleWa,
                    "senderName", req.senderName(),
                    "amount", req.amount(),
                    "bankFrom", req.bankFrom() != null ? req.bankFrom() : "",
                    "occurredAt", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to publish gift.confirmed: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<String> listActivePhones() {
        return invitationRepository.findByStatus(InvitationStatus.ACTIVE).stream()
                .map(inv -> {
                    JsonNode c = inv.getContent();
                    return c != null && c.hasNonNull("contactWhatsapp")
                            ? c.get("contactWhatsapp").asText("") : "";
                })
                .filter(wa -> !wa.isBlank())
                .distinct()
                .toList();
    }

    // ── Guest list & check-in ─────────────────────────────────────────────────

    @Transactional
    public GuestDTO addGuest(UUID invitationId, GuestRequest req) {
        Invitation inv = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + invitationId));
        Guest guest = new Guest();
        guest.setInvitation(inv);
        guest.setName(req.name());
        guest.setInviteCode(generateInviteCode());
        guest.setGroupLabel(req.groupLabel());
        guest.setTableNo(req.tableNo());
        guest.setAllottedCount(req.allottedCount());
        return GuestDTO.from(guestRepository.save(guest));
    }

    @Transactional(readOnly = true)
    public List<GuestDTO> listGuests(UUID invitationId) {
        return guestRepository.findAllByInvitationIdOrderByNameAsc(invitationId)
                .stream().map(GuestDTO::from).toList();
    }

    @Transactional
    public void removeGuest(UUID invitationId, UUID guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new NotFoundException("Guest not found: " + guestId));
        if (!guest.getInvitation().getId().equals(invitationId)) {
            throw new NotFoundException("Guest does not belong to this invitation");
        }
        guestRepository.delete(guest);
    }

    @Transactional(readOnly = true)
    public GuestDTO getGuestByCode(String inviteCode) {
        return GuestDTO.from(guestRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new NotFoundException("Guest not found: " + inviteCode)));
    }

    @Transactional
    public GuestDTO checkIn(String inviteCode, CheckInRequest req) {
        Guest guest = guestRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new NotFoundException("Guest not found: " + inviteCode));
        guest.setCheckedInAt(Instant.now());
        guest.setCheckedInCount(req.actualCount());
        return GuestDTO.from(guestRepository.save(guest));
    }

    @Transactional(readOnly = true)
    public AttendanceDTO getAttendance(UUID invitationId) {
        long totalInvited = guestRepository.countByInvitationId(invitationId);
        long totalAllotted = guestRepository.sumAllottedByInvitationId(invitationId);
        long checkedInGuests = guestRepository.countCheckedInByInvitationId(invitationId);
        long checkedInCount = guestRepository.sumCheckedInCountByInvitationId(invitationId);
        return AttendanceDTO.of(totalInvited, totalAllotted, checkedInGuests, checkedInCount);
    }

    private String generateInviteCode() {
        byte[] bytes = new byte[12];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    // ── Digital gift payments (Phase 2) ──────────────────────────────────────

    @Transactional
    public void recordGiftPaid(UUID invitationId, String senderName, long amount,
                               String message, String midtransOrderId) {
        Invitation inv = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + invitationId));
        Gift gift = new Gift();
        gift.setInvitation(inv);
        gift.setSenderName(senderName);
        gift.setAmount(amount);
        gift.setMessage(message);
        gift.setMidtransOrderId(midtransOrderId);
        giftRepository.save(gift);
        log.info("Recorded digital gift {} for invitation {}", midtransOrderId, invitationId);
    }

    @Transactional(readOnly = true)
    public GiftSummaryDTO getGiftSummary(UUID invitationId) {
        long count = giftRepository.countByInvitationId(invitationId);
        long total = giftRepository.sumAmountByInvitationId(invitationId);
        List<GiftEntryDTO> entries = giftRepository.findAllByInvitationIdOrderByCreatedAtDesc(invitationId)
                .stream().map(GiftEntryDTO::from).toList();
        return new GiftSummaryDTO(count, total, entries);
    }
}
