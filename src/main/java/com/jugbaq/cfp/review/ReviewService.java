package com.jugbaq.cfp.review;

import com.jugbaq.cfp.review.domain.Review;
import com.jugbaq.cfp.review.domain.ReviewDiscussion;
import com.jugbaq.cfp.review.domain.ReviewDiscussionRepository;
import com.jugbaq.cfp.review.domain.ReviewNotAllowedInStateException;
import com.jugbaq.cfp.review.domain.ReviewRepository;
import com.jugbaq.cfp.review.domain.SelfReviewNotAllowedException;
import com.jugbaq.cfp.submissions.domain.Submission;
import com.jugbaq.cfp.submissions.domain.SubmissionRepository;
import com.jugbaq.cfp.submissions.domain.SubmissionStatus;
import com.jugbaq.cfp.submissions.events.SubmissionAcceptedEvent;
import com.jugbaq.cfp.submissions.events.SubmissionRejectedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jugbaq.cfp.shared.tenant.TenantContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewDiscussionRepository discussionRepository;
    private final SubmissionRepository submissionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReviewService(ReviewRepository reviewRepository,
                         ReviewDiscussionRepository discussionRepository,
                         SubmissionRepository submissionRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.reviewRepository = reviewRepository;
        this.discussionRepository = discussionRepository;
        this.submissionRepository = submissionRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Mueve la submission de SUBMITTED a UNDER_REVIEW si aplica.
     * Idempotente: si ya está UNDER_REVIEW, no hace nada.
     */
    public void startReview(UUID submissionId) {
        Submission submission = loadSubmission(submissionId);
        if (submission.getStatus() == SubmissionStatus.SUBMITTED) {
            submission.transitionTo(SubmissionStatus.UNDER_REVIEW);
            submissionRepository.save(submission);
        }
    }

    public Review submitOrUpdateScore(UUID submissionId, UUID reviewerId, int score, String comment) {
        Submission submission = loadSubmission(submissionId);

        if (submission.getSpeakerId().equals(reviewerId)) {
            throw new SelfReviewNotAllowedException();
        }

        if (submission.getStatus() != SubmissionStatus.SUBMITTED
                && submission.getStatus() != SubmissionStatus.UNDER_REVIEW) {
            throw new ReviewNotAllowedInStateException(submission.getStatus());
        }

        // Pasar a UNDER_REVIEW automáticamente al primer score
        if (submission.getStatus() == SubmissionStatus.SUBMITTED) {
            submission.transitionTo(SubmissionStatus.UNDER_REVIEW);
            submissionRepository.save(submission);
        }

        Optional<Review> existing = reviewRepository.findBySubmissionIdAndReviewerId(
                submissionId, reviewerId);

        if (existing.isPresent()) {
            existing.get().update(score, comment);
            return existing.get();
        }

        UUID tenantId = TenantContext.requireTenantId();
        return reviewRepository.save(new Review(
                tenantId, submissionId, reviewerId, score, comment));
    }

    public void accept(UUID submissionId) {
        Submission submission = loadSubmission(submissionId);
        submission.transitionTo(SubmissionStatus.ACCEPTED);
        submissionRepository.save(submission);

        eventPublisher.publishEvent(new SubmissionAcceptedEvent(
                submission.getId(),
                submission.getEvent().getId(),
                submission.getSpeakerId(),
                submission.getTenantId(),
                submission.getTitle()
        ));
    }

    public void reject(UUID submissionId, String feedback) {
        Submission submission = loadSubmission(submissionId);
        submission.transitionTo(SubmissionStatus.REJECTED);
        submissionRepository.save(submission);

        eventPublisher.publishEvent(new SubmissionRejectedEvent(
                submission.getId(),
                submission.getEvent().getId(),
                submission.getSpeakerId(),
                submission.getTenantId(),
                submission.getTitle(),
                feedback
        ));
    }

    public ReviewDiscussion postDiscussion(UUID submissionId, UUID authorId, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }
        return discussionRepository.save(new ReviewDiscussion(submissionId, authorId, message));
    }

    @Transactional(readOnly = true)
    public List<ReviewSummary> listReviewsForSubmission(UUID submissionId) {
        return reviewRepository.findBySubmissionId(submissionId).stream()
                .map(ReviewSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Double> averageScore(UUID submissionId) {
        return Optional.ofNullable(reviewRepository.averageScoreForSubmission(submissionId));
    }

    @Transactional(readOnly = true)
    public List<DiscussionMessage> listDiscussion(UUID submissionId) {
        return discussionRepository.findBySubmissionIdOrderByCreatedAtAsc(submissionId).stream()
                .map(DiscussionMessage::from)
                .toList();
    }

    private Submission loadSubmission(UUID id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission no encontrada"));
    }
}
