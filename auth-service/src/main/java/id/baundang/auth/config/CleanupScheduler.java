package id.baundang.auth.config;

import id.baundang.auth.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@EnableScheduling
public class CleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CleanupScheduler.class);

    private final RefreshTokenRepository refreshRepo;

    public CleanupScheduler(RefreshTokenRepository refreshRepo) {
        this.refreshRepo = refreshRepo;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeStaleTokens() {
        refreshRepo.deleteExpiredAndRevoked(Instant.now());
        log.info("Purged expired and revoked refresh tokens");
    }
}
