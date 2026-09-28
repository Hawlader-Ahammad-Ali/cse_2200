package com.mindmap.algorithm;

import java.time.LocalDate;
import java.util.Objects;

/**
 * SuperMemo 2 (SM-2) spaced repetition algorithm.
 *
 * <p>Java 11-compatible version (no records). The API is identical to the
 * record-based version — {@code Schedule} is a plain immutable class.</p>
 */
public final class SM2Scheduler {

    public static final double DEFAULT_EF = 2.5;
    public static final double MIN_EF     = 1.3;

    public static final int QUALITY_AGAIN = 1;
    public static final int QUALITY_HARD  = 3;
    public static final int QUALITY_GOOD  = 4;
    public static final int QUALITY_EASY  = 5;

    private SM2Scheduler() { }

    public static Schedule calculate(int quality,
                                     int repetitions,
                                     int intervalDays,
                                     double easeFactor) {

        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("Quality must be 0-5, was " + quality);
        }

        double newEF = easeFactor
                + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        if (newEF < MIN_EF) newEF = MIN_EF;

        int newReps;
        int newInterval;

        if (quality < 3) {
            newReps = 0;
            newInterval = 1;
        } else {
            newReps = repetitions + 1;
            if (newReps == 1)      newInterval = 1;
            else if (newReps == 2) newInterval = 6;
            else                   newInterval = (int) Math.round(intervalDays * newEF);
            if (newInterval < 1) newInterval = 1;
            if (newInterval > 365 * 5) newInterval = 365 * 5;
        }

        LocalDate next = LocalDate.now().plusDays(newInterval);
        return new Schedule(newReps, newInterval, newEF, next);
    }

    /** Immutable result holder (Java 11-compatible). */
    public static final class Schedule {
        private final int repetitions;
        private final int intervalDays;
        private final double easeFactor;
        private final LocalDate nextReviewDate;

        public Schedule(int repetitions, int intervalDays,
                        double easeFactor, LocalDate nextReviewDate) {
            this.repetitions = repetitions;
            this.intervalDays = intervalDays;
            this.easeFactor = easeFactor;
            this.nextReviewDate = nextReviewDate;
        }

        public int repetitions()    { return repetitions; }
        public int intervalDays()   { return intervalDays; }
        public double easeFactor()  { return easeFactor; }
        public LocalDate nextReviewDate() { return nextReviewDate; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Schedule)) return false;
            Schedule s = (Schedule) o;
            return repetitions == s.repetitions
                    && intervalDays == s.intervalDays
                    && Double.compare(s.easeFactor, easeFactor) == 0
                    && Objects.equals(nextReviewDate, s.nextReviewDate);
        }

        @Override public int hashCode() {
            return Objects.hash(repetitions, intervalDays, easeFactor, nextReviewDate);
        }

        @Override public String toString() {
            return "Schedule{reps=" + repetitions + ", interval=" + intervalDays
                    + "d, EF=" + easeFactor + ", next=" + nextReviewDate + "}";
        }
    }
}