package com.mindmap.model;

import java.time.LocalDateTime;

/**
 * One recorded review event. Every rating creates a log entry for analytics.
 */
public class ReviewLog {

    private int id;
    private int reviewId;
    private int quality;
    private LocalDateTime reviewedAt;

    public ReviewLog() { }

    public ReviewLog(int reviewId, int quality) {
        this.reviewId = reviewId;
        this.quality = quality;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReviewId() { return reviewId; }
    public void setReviewId(int r) { this.reviewId = r; }

    public int getQuality() { return quality; }
    public void setQuality(int q) { this.quality = q; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime t) { this.reviewedAt = t; }
}