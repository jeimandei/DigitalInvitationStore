package id.baundang.order.service;

import id.baundang.common.exception.NotFoundException;
import id.baundang.common.exception.ValidationException;
import id.baundang.order.domain.Order;
import id.baundang.order.domain.Order.OrderStatusPg;
import id.baundang.order.domain.OrderRevision;
import id.baundang.order.domain.RevisionStatus;
import id.baundang.order.dto.OrderRevisionDTO;
import id.baundang.order.dto.RevisionRequest;
import id.baundang.order.messaging.OrderEventPublisher;
import id.baundang.order.repository.OrderRepository;
import id.baundang.order.repository.OrderRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevisionService {

    private final OrderRepository orderRepository;
    private final OrderRevisionRepository revisionRepository;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public OrderRevisionDTO requestRevision(UUID orderId, RevisionRequest req, UUID requestedBy) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatusPg.PAID && order.getStatus() != OrderStatusPg.IN_REVISION) {
            throw new ValidationException("Revisions can only be requested on PAID or IN_REVISION orders");
        }
        if (order.getRevisionCount() >= order.getMaxRevisions()) {
            throw new ValidationException("Maximum revisions (" + order.getMaxRevisions() + ") reached");
        }

        OrderRevision revision = new OrderRevision();
        revision.setOrder(order);
        revision.setRequestedBy(requestedBy);
        revision.setChanges(req.changes());
        revision.setStatus(RevisionStatus.REQUESTED);
        revision = revisionRepository.save(revision);

        order.setRevisionCount((short) (order.getRevisionCount() + 1));
        order.setStatus(OrderStatusPg.IN_REVISION);
        orderRepository.save(order);

        eventPublisher.publishOrderRevised(order);

        return OrderRevisionDTO.from(revision);
    }

    @Transactional
    public OrderRevisionDTO completeRevision(UUID revisionId) {
        OrderRevision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new NotFoundException("Revision not found: " + revisionId));

        if (revision.getStatus() == RevisionStatus.COMPLETED) {
            throw new ValidationException("Revision already completed");
        }

        revision.setStatus(RevisionStatus.COMPLETED);
        revision = revisionRepository.save(revision);

        Order order = revision.getOrder();
        order.setStatus(OrderStatusPg.PAID);
        orderRepository.save(order);

        eventPublisher.publishRevisionCompleted(order, revision);

        return OrderRevisionDTO.from(revision);
    }

    private Order findOrderOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }
}
