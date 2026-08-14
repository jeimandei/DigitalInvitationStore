package id.baundang.order.service;

import id.baundang.common.exception.NotFoundException;
import id.baundang.common.exception.UnauthorizedException;
import id.baundang.common.exception.ValidationException;
import id.baundang.order.config.PricingProperties;
import id.baundang.order.domain.Order;
import id.baundang.order.domain.Order.OrderStatusPg;
import id.baundang.order.messaging.OrderEventPublisher;
import id.baundang.order.repository.OrderRepository;
import id.baundang.order.repository.OrderRevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the claim guard: an anonymous order can be bound to an account exactly once,
 * and only by someone who can prove the contact details on it.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String NUMBER = "BND-20260814-1234";
    private static final String EMAIL = "budi@example.com";
    private static final String WHATSAPP = "6281234567890";

    private static final UUID BUYER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    OrderRepository orderRepository;
    @Mock
    OrderRevisionRepository revisionRepository;
    @Mock
    OrderEventPublisher eventPublisher;
    @Mock
    OrderNumberGenerator numberGenerator;
    @Mock
    PricingProperties pricing;

    @InjectMocks
    OrderService orderService;

    private Order anonymousOrder;

    @BeforeEach
    void setUp() {
        anonymousOrder = new Order();
        anonymousOrder.setId(UUID.randomUUID());
        anonymousOrder.setOrderNumber(NUMBER);
        anonymousOrder.setBuyerId(null);
        anonymousOrder.setContactEmail(EMAIL);
        anonymousOrder.setContactWhatsapp(WHATSAPP);
        anonymousOrder.setStatus(OrderStatusPg.PENDING);
        anonymousOrder.setTier((short) 1);
        anonymousOrder.setRevisionCount((short) 0);
        anonymousOrder.setMaxRevisions((short) 0);
    }

    private void givenOrderExists() {
        when(orderRepository.findByOrderNumberIgnoreCase(NUMBER))
                .thenReturn(Optional.of(anonymousOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void claim_byEmail_bindsOrderAndPublishesEvent() {
        givenOrderExists();

        orderService.claimOrder(NUMBER, EMAIL, BUYER);

        assertEquals(BUYER, anonymousOrder.getBuyerId());
        verify(eventPublisher, times(1)).publishOrderClaimed(anonymousOrder);
    }

    @Test
    void claim_byWhatsappLocalFormat_isAccepted() {
        givenOrderExists();

        // The order stores 628…; the buyer types the 08… form they know.
        orderService.claimOrder(NUMBER, "081234567890", BUYER);

        assertEquals(BUYER, anonymousOrder.getBuyerId());
    }

    @Test
    void claim_wrongContact_isRejectedAndLeavesOrderUnclaimed() {
        when(orderRepository.findByOrderNumberIgnoreCase(NUMBER))
                .thenReturn(Optional.of(anonymousOrder));

        assertThrows(NotFoundException.class,
                () -> orderService.claimOrder(NUMBER, "someone@else.com", BUYER));

        assertNull(anonymousOrder.getBuyerId());
        verify(eventPublisher, never()).publishOrderClaimed(any());
    }

    @Test
    void claim_unknownOrderNumber_isRejected() {
        when(orderRepository.findByOrderNumberIgnoreCase(NUMBER)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> orderService.claimOrder(NUMBER, EMAIL, BUYER));
    }

    @Test
    void claim_alreadyOwnedByAnotherAccount_cannotBeStolen() {
        anonymousOrder.setBuyerId(OTHER);
        when(orderRepository.findByOrderNumberIgnoreCase(NUMBER))
                .thenReturn(Optional.of(anonymousOrder));

        assertThrows(ValidationException.class,
                () -> orderService.claimOrder(NUMBER, EMAIL, BUYER));

        assertEquals(OTHER, anonymousOrder.getBuyerId());
        verify(eventPublisher, never()).publishOrderClaimed(any());
    }

    @Test
    void claim_repeatedBySameBuyer_isIdempotent() {
        anonymousOrder.setBuyerId(BUYER);
        when(orderRepository.findByOrderNumberIgnoreCase(NUMBER))
                .thenReturn(Optional.of(anonymousOrder));

        orderService.claimOrder(NUMBER, EMAIL, BUYER);

        assertEquals(BUYER, anonymousOrder.getBuyerId());
        // No second save or event for a no-op re-claim.
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishOrderClaimed(any());
    }

    @Test
    void claim_withoutAuthenticatedBuyer_isRejected() {
        assertThrows(UnauthorizedException.class,
                () -> orderService.claimOrder(NUMBER, EMAIL, null));

        verify(orderRepository, never()).findByOrderNumberIgnoreCase(any());
    }

    @Test
    void claim_blankOrderNumber_isRejected() {
        assertThrows(NotFoundException.class,
                () -> orderService.claimOrder("  ", EMAIL, BUYER));
    }

    // ── Ownership checks tolerate an unclaimed order ──────────────────────────

    @Test
    void getOrder_unclaimedOrder_deniesBuyerRatherThanThrowingNpe() {
        UUID id = anonymousOrder.getId();
        when(orderRepository.findById(id)).thenReturn(Optional.of(anonymousOrder));

        assertThrows(UnauthorizedException.class,
                () -> orderService.getOrder(id, BUYER, false));
    }

    @Test
    void getOrder_unclaimedOrder_isStillVisibleToAdmin() {
        UUID id = anonymousOrder.getId();
        when(orderRepository.findById(id)).thenReturn(Optional.of(anonymousOrder));

        assertEquals(NUMBER, orderService.getOrder(id, null, true).orderNumber());
    }
}
