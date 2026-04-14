package com.jugbaq.cfp.users.profile;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AvatarController {

    private final AvatarStorageService storageService;

    public AvatarController(AvatarStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/avatars/{filename}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) {
        Path path = storageService.resolvePath(filename);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        MediaType contentType = guessContentType(filename);
        return ResponseEntity.ok()
                .contentType(contentType)
                .header("Cache-Control", "public, max-age=3600")
                .body(new PathResource(path));
    }

    private MediaType guessContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
