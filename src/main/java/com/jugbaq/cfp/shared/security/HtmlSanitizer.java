package com.jugbaq.cfp.shared.security;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class HtmlSanitizer {

    private final Safelist basicSafelist;
    private final Safelist plainTextSafelist;

    public HtmlSanitizer() {
        // Permite texto básico con <b>, <i>, <a>, etc. Pero sin <script>, <iframe>, eventos.
        this.basicSafelist = Safelist.basic();
        // Solo texto plano — strips everything
        this.plainTextSafelist = Safelist.none();
    }

    /**
     * Para campos que permiten texto con formato mínimo (bio, abstract).
     */
    public String sanitizeBasic(String input) {
        if (input == null) return null;
        return Jsoup.clean(input, basicSafelist);
    }

    /**
     * Para campos que deben ser 100% texto plano (título, tagline).
     */
    public String sanitizePlainText(String input) {
        if (input == null) return null;
        return Jsoup.clean(input, plainTextSafelist);
    }
}
