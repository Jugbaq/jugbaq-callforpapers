package com.jugbaq.cfp.shared.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class AvatarStorageHealthIndicator implements HealthIndicator {

    private final Path storagePath;

    public AvatarStorageHealthIndicator(
            @Value("${cfp.storage.avatars-path:./data/avatars}") String path) {
        this.storagePath = Paths.get(path).toAbsolutePath();
    }

    @Override
    public Health health() {
        if (!Files.exists(storagePath)) {
            return Health.down()
                    .withDetail("reason", "Storage path does not exist")
                    .withDetail("path", storagePath.toString())
                    .build();
        }
        if (!Files.isWritable(storagePath)) {
            return Health.down()
                    .withDetail("reason", "Storage path is not writable")
                    .build();
        }
        return Health.up()
                .withDetail("path", storagePath.toString())
                .build();
    }
}
