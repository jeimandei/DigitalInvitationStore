package id.baundang.order.service;

import id.baundang.common.exception.NotFoundException;
import id.baundang.common.exception.UnauthorizedException;
import id.baundang.common.exception.ValidationException;
import id.baundang.order.domain.Order;
import id.baundang.order.domain.Order.OrderStatusPg;
import id.baundang.order.dto.CreateOrderRequest;
import id.baundang.order.dto.CreateOrderResponse;
import id.baundang.order.dto.OrderDTO;
import id.baundang.order.dto.OrderRevisionDTO;
import id.baundang.order.dto.PublicOrderDTO;
import id.baundang.order.dto.UpdateStatusRequest;
import id.baundang.order.config.PricingProperties;
import id.baundang.order.messaging.OrderEventPublisher;
import id.baundang.order.repository.OrderRepository;
import id.baundang.order.repository.OrderRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final short[] MAX_REVISIONS = {0, 0, 2};

    private final OrderRepository orderRepository;
    private final OrderRevisionRepository revisionRepository;
    private final OrderEventPublisher eventPublisher;
    private final OrderNumberGenerator numberGenerator;
    private final PricingProperties pricing;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest req, UUID buyerId) {
        if (req.tier() < 1 || req.tier() > 3) {
            throw new ValidationException("tier must be 1, 2, or 3");
        }

        PricingProperties.Tier tier = pricing.forTier(req.tier());

        Order order = new Order();
        order.setOrderNumber(numberGenerator.generate());
        order.setBuyerId(buyerId);
        order.setTemplateId(req.templateId());
        order.setTier(req.tier());
        order.setAmount(tier.getPrice());
        order.setCoupleName(req.coupleName());
        order.setContactWhatsapp(req.contactWhatsapp());
        order.setContactEmail(req.contactEmail());
        order.setMaxRevisions(MAX_REVISIONS[req.tier() - 1]);
        order.setNotes(req.notes());
        order.setStatus(OrderStatusPg.PENDING);

        order = orderRepository.save(order);
        eventPublisher.publishOrderCreated(order, tier.getName());

        return new CreateOrderResponse(order.getId(), order.getOrderNumber(), null);
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrder(UUID id, UUID callerId, boolean isAdmin) {
        Order order = findOrThrow(id);
        if (!isAdmin && !order.getBuyerId().equals(callerId)) {
            throw new UnauthorizedException("Access denied");
        }
        return OrderDTO.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> listMyOrders(UUID buyerId, Pageable pageable) {
        return orderRepository.findAllByBuyerId(buyerId, pageable).map(OrderDTO::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> listAllOrders(String status, String search, Pageable pageable) {
        OrderStatusPg statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = OrderStatusPg.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid status: " + status);
            }
        }
        String searchFilter = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<Order> page;
        if (statusFilter != null && searchFilter != null) {
            page = orderRepository.searchByStatus(statusFilter, searchFilter, pageable);
        } else if (statusFilter != null) {
            page = orderRepository.findAllByStatus(statusFilter, pageable);
        } else if (searchFilter != null) {
            page = orderRepository.searchAll(searchFilter, pageable);
        } else {
            page = orderRepository.findAll(pageable);
        }
        return page.map(OrderDTO::from);
    }

    @Transactional
    public void markPaid(UUID orderId, String midtransTransactionId, Instant paidAt) {
        Order order = findOrThrow(orderId);
        if (order.getStatus() == OrderStatusPg.PAID) return; // idempotent
        order.setStatus(OrderStatusPg.PAID);
        order.setPaidAt(paidAt != null ? paidAt : Instant.now());
        if (midtransTransactionId != null && !midtransTransactionId.isBlank()) {
            order.setMidtransTransactionId(midtransTransactionId);
        }
        orderRepository.save(order);
        eventPublisher.publishOrderPaid(order);
    }

    @Transactional
    public OrderDTO updateStatus(UUID id, UpdateStatusRequest req) {
        Order order = findOrThrow(id);
        order.setStatus(req.status());
        if (req.status() == OrderStatusPg.PAID) {
            order.setPaidAt(Instant.now());
            if (req.midtransTransactionId() != null) {
                order.setMidtransTransactionId(req.midtransTransactionId());
            }
            eventPublisher.publishOrderPaid(order);
        }
        return OrderDTO.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderRevisionDTO> listRevisions(UUID orderId, UUID callerId, boolean isAdmin) {
        Order order = findOrThrow(orderId);
        if (!isAdmin && !order.getBuyerId().equals(callerId)) {
            throw new UnauthorizedException("Access denied");
        }
        return revisionRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId)
                .stream().map(OrderRevisionDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public PublicOrderDTO getPublicOrder(UUID id) {
        return PublicOrderDTO.from(findOrThrow(id));
    }

    /** Public order tracking — by order number + the email or WhatsApp used on the order. */
    @Transactional(readOnly = true)
    public PublicOrderDTO lookupPublic(String orderNumber, String contact) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new NotFoundException("Pesanan tidak ditemukan");
        }
        Order o = orderRepository.findByOrderNumberIgnoreCase(orderNumber.trim())
                .orElseThrow(() -> new NotFoundException("Pesanan tidak ditemukan"));
        String c = contact != null ? contact.trim() : "";
        boolean emailMatch = o.getContactEmail() != null && o.getContactEmail().equalsIgnoreCase(c);
        String waDigits = digitsOnly(o.getContactWhatsapp());
        String cDigits = digitsOnly(c);
        boolean waMatch = !waDigits.isEmpty() && !cDigits.isEmpty()
                && (waDigits.equals(cDigits) || waDigits.endsWith(cDigits) || cDigits.endsWith(waDigits));
        if (!emailMatch && !waMatch) {
            throw new NotFoundException("Pesanan tidak ditemukan");
        }
        return PublicOrderDTO.from(o);
    }

    private String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private Order findOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }
}
