package com.jugbaq.cfp.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jugbaq.cfp.TestcontainersConfiguration;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventStatus;
import com.jugbaq.cfp.shared.domain.TenantRepository;
import com.jugbaq.cfp.shared.tenant.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EventServiceTest {

    @Autowired
    EventService service;

    @Autowired
    TenantRepository tenantRepository;

    @BeforeEach
    void setUp() {
        var tenant = tenantRepository.findBySlug("jugbaq").orElseThrow();
        TenantContext.set(tenant.getId(), "jugbaq");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void should_create_event_with_default_track_and_format() {
        Event e = service.createEvent(
                "test-" + UUID.randomUUID(),
                "Test Event",
                Instant.now().plusSeconds(86400),
                UUID.fromString("a0000000-0000-0000-0000-000000000001"));
        assertThat(e.getId()).isNotNull();
        assertThat(e.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(e.getTracks()).hasSize(1);
        assertThat(e.getFormats()).hasSize(2);
    }

    @Test
    void should_transition_draft_to_cfp_open() {
        Event e = service.createEvent(
                "trans-" + UUID.randomUUID(),
                "Trans",
                Instant.now().plusSeconds(86400),
                UUID.fromString("a0000000-0000-0000-0000-000000000001"));
        service.updateStatus(e.getId(), EventStatus.CFP_OPEN);
        assertThat(service.findById(e.getId()).orElseThrow().getStatus()).isEqualTo(EventStatus.CFP_OPEN);
    }

    @Test
    void should_reject_invalid_transition() {
        Event e = service.createEvent(
                "invalid-" + UUID.randomUUID(),
                "Invalid",
                Instant.now().plusSeconds(86400),
                UUID.fromString("a0000000-0000-0000-0000-000000000001"));
        assertThatThrownBy(() -> service.updateStatus(e.getId(), EventStatus.PUBLISHED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_list_cfp_open_events() {
        Event open = service.createEvent(
                "open-" + UUID.randomUUID(),
                "Open Event",
                Instant.now().plusSeconds(86400),
                UUID.fromString("a0000000-0000-0000-0000-000000000001"));
        service.updateStatus(open.getId(), EventStatus.CFP_OPEN);

        List<Event> result = service.listCfpOpen();
        assertThat(result).anyMatch(e -> e.getId().equals(open.getId()));
    }

    @Test
    void should_return_null_when_slug_not_found_for_tracks() {
        Event result = service.getEventWithTracksBySlug("nonexistent-slug-" + UUID.randomUUID());
        assertThat(result).isNull();
    }

    @Test
    void should_find_by_slug_with_details() {
        String slug = "details-" + UUID.randomUUID();
        Event e = service.createEvent(
                slug,
                "Details Event",
                Instant.now().plusSeconds(86400),
                UUID.fromString("a0000000-0000-0000-0000-000000000001"));

        var result = service.findBySlugWithDetails(slug);
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Details Event");
    }

    @Test
    void should_return_empty_when_slug_not_found_for_details() {
        var result = service.findBySlugWithDetails("nonexistent-slug-" + UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    void should_list_all_summaries() {
        String slug = "summary-" + UUID.randomUUID();
        service.createEvent(
                slug,
                "Summary Event",
                Instant.now().plusSeconds(86400),
                UUID.fromString("a0000000-0000-0000-0000-000000000001"));

        List<EventSummary> summaries = service.listAllSummaries();
        assertThat(summaries).isNotEmpty();
        assertThat(summaries).anyMatch(s -> s.slug().equals(slug));

        EventSummary match = summaries.stream()
                .filter(s -> s.slug().equals(slug))
                .findFirst()
                .orElseThrow();
        assertThat(match.name()).isEqualTo("Summary Event");
        assertThat(match.status()).isEqualTo(EventStatus.DRAFT);
        assertThat(match.id()).isNotNull();
        assertThat(match.eventDate()).isNotNull();
    }
}
