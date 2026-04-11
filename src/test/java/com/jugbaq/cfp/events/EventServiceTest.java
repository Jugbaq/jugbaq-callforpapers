package com.jugbaq.cfp.events;

import com.jugbaq.cfp.TestcontainersConfiguration;
import com.jugbaq.cfp.events.domain.Event;
import com.jugbaq.cfp.events.domain.EventStatus;
import com.jugbaq.cfp.shared.domain.TenantRepository;
import com.jugbaq.cfp.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EventServiceTest {

    @Autowired EventService service;
    @Autowired TenantRepository tenantRepository;

    @BeforeEach
    void setUp() {
        var tenant = tenantRepository.findBySlug("jugbaq").orElseThrow();
        TenantContext.set(tenant.getId(), "jugbaq");
    }

    @AfterEach
    void cleanup() { TenantContext.clear(); }

    @Test
    void should_create_event_with_default_track_and_format() {
        Event e = service.createEvent(
                "test-" + UUID.randomUUID(),
                "Test Event",
                Instant.now().plusSeconds(86400),
                UUID.fromString("a0000000-0000-0000-0000-000000000001")
        );
        assertThat(e.getId()).isNotNull();
        assertThat(e.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(e.getTracks()).hasSize(1);
        assertThat(e.getFormats()).hasSize(2);
    }

    @Test
    void should_transition_draft_to_cfp_open() {
        Event e = service.createEvent(
                "trans-" + UUID.randomUUID(), "Trans", Instant.now().plusSeconds(86400),
                UUID.fromString("a0000000-0000-0000-0000-000000000001")
        );
        service.updateStatus(e.getId(), EventStatus.CFP_OPEN);
        assertThat(service.findById(e.getId()).orElseThrow().getStatus())
                .isEqualTo(EventStatus.CFP_OPEN);
    }

    @Test
    void should_reject_invalid_transition() {
        Event e = service.createEvent(
                "invalid-" + UUID.randomUUID(), "Invalid", Instant.now().plusSeconds(86400),
                UUID.fromString("a0000000-0000-0000-0000-000000000001")
        );
        assertThatThrownBy(() -> service.updateStatus(e.getId(), EventStatus.PUBLISHED))
                .isInstanceOf(IllegalStateException.class);
    }
}
