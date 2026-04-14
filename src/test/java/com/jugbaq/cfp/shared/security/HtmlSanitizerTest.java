package com.jugbaq.cfp.shared.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void should_strip_script_tags_from_basic_content() {
        String malicious = "Hola <script>alert('xss')</script> mundo";
        String clean = sanitizer.sanitizeBasic(malicious);
        assertThat(clean).doesNotContain("<script>");
        assertThat(clean).contains("Hola");
        assertThat(clean).contains("mundo");
    }

    @Test
    void should_allow_basic_formatting_tags() {
        String input = "Soy <b>Geovanny</b> y trabajo con <i>Kotlin</i>";
        String clean = sanitizer.sanitizeBasic(input);
        assertThat(clean).contains("<b>Geovanny</b>");
        assertThat(clean).contains("<i>Kotlin</i>");
    }

    @Test
    void should_strip_all_tags_in_plain_text_mode() {
        String input = "<b>Bold</b> and <script>evil()</script>";
        String clean = sanitizer.sanitizePlainText(input);
        assertThat(clean).doesNotContain("<");
        assertThat(clean).doesNotContain(">");
        assertThat(clean).contains("Bold");
    }

    @Test
    void should_strip_event_handlers() {
        String input = "<a href='javascript:alert(1)' onclick='evil()'>click</a>";
        String clean = sanitizer.sanitizeBasic(input);
        assertThat(clean).doesNotContain("javascript:");
        assertThat(clean).doesNotContain("onclick");
    }

    @Test
    void should_handle_null_input() {
        assertThat(sanitizer.sanitizeBasic(null)).isNull();
        assertThat(sanitizer.sanitizePlainText(null)).isNull();
    }
}
