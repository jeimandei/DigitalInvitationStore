package id.baundang.invitation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import id.baundang.common.exception.NotFoundException;
import id.baundang.common.exception.ValidationException;
import id.baundang.invitation.domain.Gift;
import id.baundang.invitation.domain.GiftAccount;
import id.baundang.invitation.domain.GiftConfirmation;
import id.baundang.invitation.domain.Guest;
import id.baundang.invitation.domain.GuestbookEntry;
import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.domain.Invitation.InvitationStatus;
import id.baundang.invitation.domain.RsvpResponse;
import id.baundang.invitation.dto.AdminGuestbookEntryDTO;
import id.baundang.invitation.dto.AttendanceDTO;
import id.baundang.invitation.dto.CheckInRequest;
import id.baundang.invitation.dto.EventDTO;
import id.baundang.invitation.dto.ExpiringInvitationDTO;
import id.baundang.invitation.dto.GiftAccountDTO;
import id.baundang.invitation.dto.GiftAccountRequest;
import id.baundang.invitation.dto.GiftConfirmRequest;
import id.baundang.invitation.dto.GiftEntryDTO;
import id.baundang.invitation.dto.GiftSummaryDTO;
import id.baundang.invitation.dto.GuestDTO;
import id.baundang.invitation.dto.GuestRequest;
import id.baundang.invitation.dto.GuestbookEntryDTO;
import id.baundang.invitation.dto.GuestbookRequest;
import id.baundang.invitation.dto.InvitationSummaryDTO;
import id.baundang.invitation.dto.RsvpRequest;
import id.baundang.invitation.dto.RsvpResponseDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Content keys that identify or own the invitation. They are stripped from every
     * inbound patch so an admin content update cannot reassign a tenant or repoint a slug.
     */
    private static final Set<String> RESERVED_CONTENT_KEYS =
            Set.of("buyerId", "orderId", "slug", "coupleSlug");

    /**
     * The content the couple may edit themselves. Everything outside this set stays
     * owner-controlled — notably {@code stylePreset} and {@code colorPalette}, which
     * protect the visual quality that is the product, and {@code accessPin}, which is
     * a security control rather than content.
     */
    private static final Set<String> CLIENT_EDITABLE_CONTENT_KEYS = Set.of(
            "coupleName", "groomFullName", "brideFullName",
            "matrimonyDate", "matrimonyTime", "matrimonyVenue",
            "receptionDate", "receptionTime", "receptionVenue",
            "loveStory", "coverPhotoUrl", "mapsEmbedUrl");

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
        return applyContentPatch(id, patch, null);
    }

    /**
     * The couple editing their own invitation. Same merge as the admin path, but the
     * patch is first narrowed to {@link #CLIENT_EDITABLE_CONTENT_KEYS}, so a client
     * cannot reach theme, PIN or any other owner-controlled setting by hand-crafting a
     * request. Unknown keys are dropped silently rather than rejected: the portal only
     * ever sends the allowlisted fields, so a stray key is a client bug, not an attack
     * worth failing an otherwise valid save for.
     */
    @Transactional
    @CacheEvict(value = {"invitations", "guestbooks"}, allEntries = true)
    public Invitation updateContentAsClient(UUID id, JsonNode patch) {
        return applyContentPatch(id, patch, CLIENT_EDITABLE_CONTENT_KEYS);
    }

    /** The client-editable slice of an invitation's content, for the portal's editor. */
    @Transactional(readOnly = true)
    public JsonNode editableContent(UUID id) {
        Invitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + id));
        JsonNode content = inv.getContent();
        ObjectNode editable = JsonNodeFactory.instance.objectNode();
        if (content != null && content.isObject()) {
            editable.setAll((ObjectNode) content.deepCopy());
            editable.retain(CLIENT_EDITABLE_CONTENT_KEYS);
        }
        return editable;
    }

    private Invitation applyContentPatch(UUID id, JsonNode patch, Set<String> allowedKeys) {
        // Validated before the lookup so a malformed patch costs no database round-trip.
        if (patch == null || !patch.isObject()) {
            throw new ValidationException("Konten undangan harus berupa objek JSON");
        }
        ObjectNode sanitized = (ObjectNode) patch.deepCopy();
        sanitized.remove(RESERVED_CONTENT_KEYS);
        if (allowedKeys != null) {
            sanitized.retain(allowedKeys);
        }

        Invitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + id));
        JsonNode existing = inv.getContent();
        ObjectNode merged = existing != null && existing.isObject()
                ? (ObjectNode) existing.deepCopy()
                : JsonNodeFactory.instance.objectNode();
        merged.setAll(sanitized);
        inv.setContent(merged);
        return invitationRepository.save(inv);
    }

    @Transactional
    public Invitation updateStatus(UUID id, InvitationStatus status) {
        Invitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + id));
        inv.setStatus(status);
        return invitationRepository.save(inv);
    }

    @Transactional
    public Invitation updateSlug(UUID id, String rawSlug) {
        if (rawSlug == null) {
            throw new IllegalArgumentException("Slug kosong");
        }
        String slug = rawSlug.trim().toLowerCase();
        if (!slug.matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException("Slug hanya boleh huruf kecil, angka, dan tanda hubung");
        }
        invitationRepository.findByCoupleSlug(slug).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Slug sudah dipakai undangan lain");
            }
        });
        Invitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + id));
        inv.setCoupleSlug(slug);
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

    // ── Admin listings ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<InvitationSummaryDTO> listInvitations(Pageable pageable) {
        return invitationRepository.findAll(pageable).map(InvitationSummaryDTO::from);
    }

    @Transactional(readOnly = true)
    public InvitationSummaryDTO getInvitation(UUID id) {
        Invitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + id));
        return InvitationSummaryDTO.from(inv);
    }

    @Transactional(readOnly = true)
    public List<AdminGuestbookEntryDTO> listAllGuestbook(UUID invitationId) {
        return guestbookRepository.findAllByInvitationIdOrderByCreatedAtDesc(invitationId)
                .stream().map(AdminGuestbookEntryDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RsvpResponseDTO> listRsvp(UUID invitationId) {
        return rsvpRepository.findByInvitationIdOrderBySubmittedAtDesc(invitationId)
                .stream().map(RsvpResponseDTO::from).toList();
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
