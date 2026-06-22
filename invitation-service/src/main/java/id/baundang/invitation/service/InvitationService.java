package id.baundang.invitation.service;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.common.exception.NotFoundException;
import id.baundang.invitation.domain.GiftAccount;
import id.baundang.invitation.domain.GiftConfirmation;
import id.baundang.invitation.dto.EventDTO;
import id.baundang.invitation.domain.GuestbookEntry;
import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.domain.Invitation.InvitationStatus;
import id.baundang.invitation.domain.RsvpResponse;
import id.baundang.invitation.dto.*;
import id.baundang.invitation.repository.GiftAccountRepository;
import id.baundang.invitation.repository.GiftConfirmationRepository;
import id.baundang.invitation.repository.GuestbookEntryRepository;
import id.baundang.invitation.repository.InvitationRepository;
import id.baundang.invitation.repository.RsvpResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final RsvpResponseRepository rsvpRepository;
    private final GuestbookEntryRepository guestbookRepository;
    private final GiftAccountRepository giftAccountRepository;
    private final GiftConfirmationRepository giftConfirmationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.rsvp-exchange}")
    private String rsvpExchange;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public Invitation getBySlug(String slug) {
        return invitationRepository.findByCoupleSlug(slug)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + slug));
    }

    @Transactional
    public Invitation getBySlugAndIncrementView(String slug) {
        Invitation inv = getBySlug(slug);
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
    public List<GuestbookEntryDTO> listApprovedGuestbook(String slug) {
        Invitation inv = getBySlug(slug);
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
    public Invitation updateContent(UUID id, JsonNode content) {
        Invitation inv = invitationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invitation not found: " + id));
        inv.setContent(content);
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
                .orElseGet(() -> { GiftAccount a = new GiftAccount(); a.setInvitation(inv); return a; });
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
}
