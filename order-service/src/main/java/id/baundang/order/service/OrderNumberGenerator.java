package id.baundang.order.service;

import id.baundang.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RNG = new SecureRandom();
    private static final int MAX_ATTEMPTS = 5;

    private final OrderRepository orderRepository;

    public String generate() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String candidate = "BND-" + LocalDate.now().format(DATE_FMT) + "-" + hexSuffix();
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate unique order number after " + MAX_ATTEMPTS + " attempts");
    }

    private String hexSuffix() {
        return String.format("%04X", RNG.nextInt(0xFFFF));
    }
}
