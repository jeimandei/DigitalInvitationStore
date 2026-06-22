package id.baundang.invitation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvitationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InvitationServiceApplication.class, args);
    }
}
