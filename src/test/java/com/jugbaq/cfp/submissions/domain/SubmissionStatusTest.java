package com.jugbaq.cfp.submissions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubmissionStatusTest {

    @Test
    void draft_can_go_to_submitted_or_withdrawn() {
        assertThat(SubmissionStatus.DRAFT.canTransitionTo(SubmissionStatus.SUBMITTED))
                .isTrue();
        assertThat(SubmissionStatus.DRAFT.canTransitionTo(SubmissionStatus.WITHDRAWN))
                .isTrue();
        assertThat(SubmissionStatus.DRAFT.canTransitionTo(SubmissionStatus.ACCEPTED))
                .isFalse();
    }

    @Test
    void submitted_can_go_to_under_review_or_withdrawn_or_draft() {
        assertThat(SubmissionStatus.SUBMITTED.canTransitionTo(SubmissionStatus.UNDER_REVIEW))
                .isTrue();
        assertThat(SubmissionStatus.SUBMITTED.canTransitionTo(SubmissionStatus.WITHDRAWN))
                .isTrue();
        assertThat(SubmissionStatus.SUBMITTED.canTransitionTo(SubmissionStatus.DRAFT))
                .isTrue();
        assertThat(SubmissionStatus.SUBMITTED.canTransitionTo(SubmissionStatus.ACCEPTED))
                .isFalse();
    }

    @Test
    void under_review_can_only_go_to_accepted_or_rejected() {
        assertThat(SubmissionStatus.UNDER_REVIEW.canTransitionTo(SubmissionStatus.ACCEPTED))
                .isTrue();
        assertThat(SubmissionStatus.UNDER_REVIEW.canTransitionTo(SubmissionStatus.REJECTED))
                .isTrue();
        assertThat(SubmissionStatus.UNDER_REVIEW.canTransitionTo(SubmissionStatus.WITHDRAWN))
                .isFalse();
    }

    @Test
    void accepted_can_go_to_confirmed_or_withdrawn() {
        assertThat(SubmissionStatus.ACCEPTED.canTransitionTo(SubmissionStatus.CONFIRMED))
                .isTrue();
        assertThat(SubmissionStatus.ACCEPTED.canTransitionTo(SubmissionStatus.WITHDRAWN))
                .isTrue();
        assertThat(SubmissionStatus.ACCEPTED.canTransitionTo(SubmissionStatus.SUBMITTED))
                .isFalse();
    }

    @Test
    void terminal_states_cannot_transition() {
        for (SubmissionStatus s : SubmissionStatus.values()) {
            assertThat(SubmissionStatus.REJECTED.canTransitionTo(s)).isFalse();
            assertThat(SubmissionStatus.CONFIRMED.canTransitionTo(s)).isFalse();
            assertThat(SubmissionStatus.WITHDRAWN.canTransitionTo(s)).isFalse();
        }
    }

    @Test
    void editable_states_are_draft_and_submitted() {
        assertThat(SubmissionStatus.DRAFT.isEditableBySpeaker()).isTrue();
        assertThat(SubmissionStatus.SUBMITTED.isEditableBySpeaker()).isTrue();
        assertThat(SubmissionStatus.UNDER_REVIEW.isEditableBySpeaker()).isFalse();
        assertThat(SubmissionStatus.ACCEPTED.isEditableBySpeaker()).isFalse();
        assertThat(SubmissionStatus.REJECTED.isEditableBySpeaker()).isFalse();
        assertThat(SubmissionStatus.CONFIRMED.isEditableBySpeaker()).isFalse();
        assertThat(SubmissionStatus.WITHDRAWN.isEditableBySpeaker()).isFalse();
    }

    @Test
    void isTerminal_identifies_terminal_states() {
        assertThat(SubmissionStatus.REJECTED.isTerminal()).isTrue();
        assertThat(SubmissionStatus.CONFIRMED.isTerminal()).isTrue();
        assertThat(SubmissionStatus.WITHDRAWN.isTerminal()).isTrue();
        assertThat(SubmissionStatus.DRAFT.isTerminal()).isFalse();
        assertThat(SubmissionStatus.SUBMITTED.isTerminal()).isFalse();
        assertThat(SubmissionStatus.UNDER_REVIEW.isTerminal()).isFalse();
        assertThat(SubmissionStatus.ACCEPTED.isTerminal()).isFalse();
    }
}
