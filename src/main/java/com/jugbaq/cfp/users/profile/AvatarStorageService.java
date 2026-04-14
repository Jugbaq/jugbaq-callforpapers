package com.jugbaq.cfp.users.profile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AvatarStorageService {

    private static final Logger log = LoggerFactory.getLogger(AvatarStorageService.class);

    private final Path storagePath;

    public AvatarStorageService(@Value("${cfp.storage.avatars-path:./data/avatars}") String path) {
        this.storagePath = Paths.get(path).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(storagePath);
        log.info("Avatar storage initialized at: {}", storagePath);
    }

    /**
     * Guarda el avatar y retorna el path relativo para guardar en DB.
     */
    public String save(UUID userId, String originalFilename, InputStream content) throws IOException {
        String extension = extractExtension(originalFilename);
        if (!isAllowedExtension(extension)) {
            throw new IllegalArgumentException("Formato no permitido. Usa JPG, PNG o WebP.");
        }

        // Leer a memoria con límite
        byte[] bytes = content.readAllBytes();
        if (bytes.length > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("El archivo excede 2 MB");
        }

        // Validar magic bytes (no confiar solo en la extensión)
        if (!isValidImageMagicBytes(bytes)) {
            throw new IllegalArgumentException("El archivo no es una imagen válida");
        }

        String filename = userId + "." + extension;
        Path target = storagePath.resolve(filename);
        Files.write(target, bytes);
        log.info("Avatar guardado para usuario {}: {}", userId, filename);
        return "/avatars/" + filename;
    }

    private boolean isValidImageMagicBytes(byte[] bytes) {
        if (bytes.length < 12) return false;

        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) return true;

        // PNG: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') return true;

        // WebP: RIFF....WEBP
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return true;

        return false;
    }

    public Path resolvePath(String relativePath) {
        String filename =
                relativePath.startsWith("/avatars/") ? relativePath.substring("/avatars/".length()) : relativePath;
        return storagePath.resolve(filename);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private boolean isAllowedExtension(String ext) {
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp");
    }
}
