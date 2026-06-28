package id.baundang.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import id.baundang.common.exception.NotFoundException;
import id.baundang.common.exception.UnauthorizedException;
import id.baundang.order.domain.IntakeQuestion;
import id.baundang.order.domain.Order;
import id.baundang.order.domain.OrderIntake;
import id.baundang.order.dto.IntakeQuestionDTO;
import id.baundang.order.dto.IntakeQuestionRequest;
import id.baundang.order.dto.OrderIntakeDTO;
import id.baundang.order.dto.OrderIntakeRequest;
import id.baundang.order.repository.IntakeQuestionRepository;
import id.baundang.order.repository.OrderIntakeRepository;
import id.baundang.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntakeService {

    private final IntakeQuestionRepository questionRepository;
    private final OrderIntakeRepository intakeRepository;
    private final OrderRepository orderRepository;

    // ── Questionnaire definition (admin) ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<IntakeQuestionDTO> listAllQuestions() {
        return questionRepository.findAllByOrderBySortOrderAsc()
                .stream().map(IntakeQuestionDTO::from).toList();
    }

    @Transactional
    public IntakeQuestionDTO createQuestion(IntakeQuestionRequest req) {
        IntakeQuestion q = new IntakeQuestion();
        apply(q, req);
        return IntakeQuestionDTO.from(questionRepository.save(q));
    }

    @Transactional
    public IntakeQuestionDTO updateQuestion(UUID id, IntakeQuestionRequest req) {
        IntakeQuestion q = questionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Question not found: " + id));
        apply(q, req);
        return IntakeQuestionDTO.from(questionRepository.save(q));
    }

    @Transactional
    public void deleteQuestion(UUID id) {
        questionRepository.deleteById(id);
    }

    private void apply(IntakeQuestion q, IntakeQuestionRequest req) {
        if (req.section() != null) q.setSection(req.section().isBlank() ? "Umum" : req.section());
        q.setLabel(req.label());
        q.setFieldKey(req.fieldKey());
        if (req.inputType() != null && !req.inputType().isBlank()) q.setInputType(req.inputType());
        q.setOptions(req.options() != null ? req.options() : JsonNodeFactory.instance.arrayNode());
        if (req.minTier() != null) q.setMinTier(req.minTier());
        if (req.required() != null) q.setRequired(req.required());
        if (req.sortOrder() != null) q.setSortOrder(req.sortOrder());
        if (req.active() != null) q.setActive(req.active());
    }

    // ── Per-order intake (client) ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<IntakeQuestionDTO> questionsForOrder(UUID orderId, UUID callerId, boolean isAdmin) {
        Order order = requireOrderAccess(orderId, callerId, isAdmin);
        short tier = order.getTier() != null ? order.getTier() : 1;
        return questionRepository
                .findAllByActiveTrueAndMinTierLessThanEqualOrderBySortOrderAsc(tier)
                .stream().map(IntakeQuestionDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderIntakeDTO getIntake(UUID orderId, UUID callerId, boolean isAdmin) {
        requireOrderAccess(orderId, callerId, isAdmin);
        return intakeRepository.findById(orderId)
                .map(OrderIntakeDTO::from)
                .orElse(new OrderIntakeDTO(orderId, JsonNodeFactory.instance.objectNode(), false));
    }

    @Transactional
    public OrderIntakeDTO saveIntake(UUID orderId, OrderIntakeRequest req, UUID callerId, boolean isAdmin) {
        requireOrderAccess(orderId, callerId, isAdmin);
        OrderIntake intake = intakeRepository.findById(orderId).orElseGet(() -> {
            OrderIntake i = new OrderIntake();
            i.setOrderId(orderId);
            return i;
        });
        if (req.answers() != null) intake.setAnswers(req.answers());
        if (req.submitted() != null) intake.setSubmitted(req.submitted());
        intake.setUpdatedAt(Instant.now());
        return OrderIntakeDTO.from(intakeRepository.save(intake));
    }

    private Order requireOrderAccess(UUID orderId, UUID callerId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        if (!isAdmin && (callerId == null || !order.getBuyerId().equals(callerId))) {
            throw new UnauthorizedException("Access denied");
        }
        return order;
    }
}
