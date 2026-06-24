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
import id.baundang.order.dto.UpdateStatusRequest;
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

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest req, UUID buyerId) {
        if (req.tier() < 1 || req.tier() > 3) {
            throw new ValidationException("tier must be 1, 2, or 3");
        }

        Order order = new Order();
        order.setOrderNumber(numberGenerator.generate());
        order.setBuyerId(buyerId);
        order.setTemplateId(req.templateId());
        order.setTier(req.tier());
        order.setCoupleName(req.coupleName());
        order.setContactWhatsapp(req.contactWhatsapp());
        order.setContactEmail(req.contactEmail());
        order.setMaxRevisions(MAX_REVISIONS[req.tier() - 1]);
        order.setNotes(req.notes());
        order.setStatus(OrderStatusPg.PENDING);

        order = orderRepository.save(order);
        eventPublisher.publishOrderCreated(order);

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
    public Page<OrderDTO> listAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderDTO::from);
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

    private Order findOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }
}
