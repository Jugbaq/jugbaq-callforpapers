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
    void cfp_open_can_go_to_cfp_closed_or_cancelled() {
        assertThat(EventStatus.CFP_OPEN.canTransitionTo(EventStatus.CFP_CLOSED)).isTrue();
        assertThat(EventStatus.CFP_OPEN.canTransitionTo(EventStatus.CANCELLED)).isTrue();
        assertThat(EventStatus.CFP_OPEN.canTransitionTo(EventStatus.REVIEW)).isFalse();
    }

    @Test
    void cfp_closed_can_go_to_review_or_cfp_open() {
        assertThat(EventStatus.CFP_CLOSED.canTransitionTo(EventStatus.REVIEW)).isTrue();
        assertThat(EventStatus.CFP_CLOSED.canTransitionTo(EventStatus.CFP_OPEN)).isTrue();
        assertThat(EventStatus.CFP_CLOSED.canTransitionTo(EventStatus.PUBLISHED))
                .isFalse();
    }

    @Test
    void review_can_go_to_published_or_cfp_closed() {
        assertThat(EventStatus.REVIEW.canTransitionTo(EventStatus.PUBLISHED)).isTrue();
        assertThat(EventStatus.REVIEW.canTransitionTo(EventStatus.CFP_CLOSED)).isTrue();
        assertThat(EventStatus.REVIEW.canTransitionTo(EventStatus.COMPLETED)).isFalse();
    }

    @Test
    void published_can_go_to_completed_or_cancelled() {
        assertThat(EventStatus.PUBLISHED.canTransitionTo(EventStatus.COMPLETED)).isTrue();
        assertThat(EventStatus.PUBLISHED.canTransitionTo(EventStatus.CANCELLED)).isTrue();
        assertThat(EventStatus.PUBLISHED.canTransitionTo(EventStatus.REVIEW)).isFalse();
    }

    @Test
    void completed_is_terminal() {
        for (EventStatus s : EventStatus.values()) {
            assertThat(EventStatus.COMPLETED.canTransitionTo(s)).isFalse();
        }
    }

    @Test
    void cancelled_is_terminal() {
        for (EventStatus s : EventStatus.values()) {
            assertThat(EventStatus.CANCELLED.canTransitionTo(s)).isFalse();
        }
    }
}
