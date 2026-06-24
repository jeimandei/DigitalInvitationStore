package id.baundang.order.controller;

import id.baundang.common.ApiResponse;
import id.baundang.common.PagedResponse;
import id.baundang.order.dto.CreateOrderRequest;
import id.baundang.order.dto.CreateOrderResponse;
import id.baundang.order.dto.OrderDTO;
import id.baundang.order.dto.OrderRevisionDTO;
import id.baundang.order.dto.RevisionRequest;
import id.baundang.order.dto.UpdateStatusRequest;
import id.baundang.order.service.OrderService;
import id.baundang.order.service.RevisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final RevisionService revisionService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponse>> create(
            @Valid @RequestBody CreateOrderRequest req,
            Authentication auth) {
        UUID buyerId = auth != null ? UUID.fromString(auth.getName()) : UUID.randomUUID();
        CreateOrderResponse resp = orderService.createOrder(req, buyerId);
        return ResponseEntity
                .created(URI.create("/api/v1/orders/" + resp.orderId()))
                .body(ApiResponse.ok(resp));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDTO> getOrder(@PathVariable UUID id, Authentication auth) {
        UUID callerId = UUID.fromString(auth.getName());
        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");
        return ApiResponse.ok(orderService.getOrder(id, callerId, isAdmin));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PagedResponse<OrderDTO>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ApiResponse.ok(PagedResponse.from(orderService.listAllOrders(status, search, pageable)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrderDTO> updateStatus(@PathVariable UUID id,
                                              @Valid @RequestBody UpdateStatusRequest req) {
        return ApiResponse.ok(orderService.updateStatus(id, req));
    }

    @PostMapping("/{id}/revisions")
    public ApiResponse<OrderRevisionDTO> requestRevision(@PathVariable UUID id,
                                                         @Valid @RequestBody RevisionRequest req,
                                                         Authentication auth) {
        UUID callerId = UUID.fromString(auth.getName());
        return ApiResponse.ok(revisionService.requestRevision(id, req, callerId));
    }

    @PutMapping("/revisions/{revisionId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrderRevisionDTO> completeRevision(@PathVariable UUID revisionId) {
        return ApiResponse.ok(revisionService.completeRevision(revisionId));
    }

    @GetMapping("/{id}/revisions")
    public ApiResponse<List<OrderRevisionDTO>> listRevisions(@PathVariable UUID id,
                                                             Authentication auth) {
        UUID callerId = UUID.fromString(auth.getName());
        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");
        return ApiResponse.ok(orderService.listRevisions(id, callerId, isAdmin));
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority(role));
    }
}
