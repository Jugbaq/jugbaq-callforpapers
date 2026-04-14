package com.jugbaq.cfp.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

    private final RateLimitService service = new RateLimitService();

    @Test
    void should_allow_first_5_registration_attempts() {
        String ip = "192.168.1.100";
        for (int i = 0; i < 5; i++) {
            assertThat(service.registrationBucket(ip).tryConsume(1)).isTrue();
        }
    }

    @Test
    void should_block_6th_registration_attempt() {
        String ip = "192.168.1.101";
        for (int i = 0; i < 5; i++) {
            service.registrationBucket(ip).tryConsume(1);
        }
        assertThat(service.registrationBucket(ip).tryConsume(1)).isFalse();
    }

    @Test
    void should_separate_buckets_by_ip() {
        service.registrationBucket("1.1.1.1").tryConsume(5);
        assertThat(service.registrationBucket("2.2.2.2").tryConsume(1)).isTrue();
    }

    @Test
    void should_allow_20_submissions_per_user() {
        String userId = "user-abc";
        for (int i = 0; i < 20; i++) {
            assertThat(service.submissionBucket(userId).tryConsume(1)).isTrue();
        }
        assertThat(service.submissionBucket(userId).tryConsume(1)).isFalse();
    }
}
