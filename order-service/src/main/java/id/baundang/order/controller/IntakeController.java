package id.baundang.order.controller;

import id.baundang.common.ApiResponse;
import id.baundang.order.dto.IntakeQuestionDTO;
import id.baundang.order.dto.IntakeQuestionRequest;
import id.baundang.order.dto.OrderIntakeDTO;
import id.baundang.order.dto.OrderIntakeRequest;
import id.baundang.order.service.IntakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class IntakeController {

    private final IntakeService intakeService;

    // ── Questionnaire definition (admin only) ─────────────────────────────────

    @GetMapping("/api/v1/admin/intake/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<IntakeQuestionDTO>> listQuestions() {
        return ApiResponse.ok(intakeService.listAllQuestions());
    }

    @PostMapping("/api/v1/admin/intake/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IntakeQuestionDTO> createQuestion(@Valid @RequestBody IntakeQuestionRequest req) {
        return ApiResponse.ok(intakeService.createQuestion(req), "Pertanyaan dibuat");
    }

    @PutMapping("/api/v1/admin/intake/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IntakeQuestionDTO> updateQuestion(@PathVariable UUID id,
                                                         @Valid @RequestBody IntakeQuestionRequest req) {
        return ApiResponse.ok(intakeService.updateQuestion(id, req), "Pertanyaan diperbarui");
    }

    @DeleteMapping("/api/v1/admin/intake/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteQuestion(@PathVariable UUID id) {
        intakeService.deleteQuestion(id);
        return ApiResponse.ok(null, "Pertanyaan dihapus");
    }

    // ── Per-order intake (buyer or admin) ─────────────────────────────────────

    @GetMapping("/api/v1/orders/{orderId}/intake/questions")
    public ApiResponse<List<IntakeQuestionDTO>> questionsForOrder(@PathVariable UUID orderId,
                                                                  Authentication auth) {
        return ApiResponse.ok(intakeService.questionsForOrder(orderId, callerId(auth), isAdmin(auth)));
    }

    @GetMapping("/api/v1/orders/{orderId}/intake")
    public ApiResponse<OrderIntakeDTO> getIntake(@PathVariable UUID orderId, Authentication auth) {
        return ApiResponse.ok(intakeService.getIntake(orderId, callerId(auth), isAdmin(auth)));
    }

    @PutMapping("/api/v1/orders/{orderId}/intake")
    public ApiResponse<OrderIntakeDTO> saveIntake(@PathVariable UUID orderId,
                                                  @RequestBody OrderIntakeRequest req,
                                                  Authentication auth) {
        return ApiResponse.ok(intakeService.saveIntake(orderId, req, callerId(auth), isAdmin(auth)),
                "Tersimpan");
    }

    private UUID callerId(Authentication auth) {
        try {
            return auth != null ? UUID.fromString(auth.getName()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
