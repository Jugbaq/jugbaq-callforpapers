package com.jugbaq.cfp.events.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EventStatusTest {
    @Test
    void draft_can_go_to_cfp_open_or_cancelled() {
        assertThat(EventStatus.DRAFT.canTransitionTo(EventStatus.CFP_OPEN)).isTrue();
        assertThat(EventStatus.DRAFT.canTransitionTo(EventStatus.CANCELLED)).isTrue();
        assertThat(EventStatus.DRAFT.canTransitionTo(EventStatus.PUBLISHED)).isFalse();
    }

    @Test
    void completed_is_terminal() {
        for (EventStatus s : EventStatus.values()) {
            assertThat(EventStatus.COMPLETED.canTransitionTo(s)).isFalse();
        }
    }
}
