package com.mindmap.algorithm;

import java.time.LocalDate;

/**
 * SuperMemo 2 (SM-2) spaced repetition algorithm.
 *
 * <p>Given a quality rating (0–5) and the current state, computes the
 * next interval, ease factor, and review date.</p>
 *
 * <p>Quality mapping in the UI:
 * <ul>
 *   <li><b>Again</b> = 1 (failed recall)</li>
 *   <li><b>Hard</b>  = 3</li>
 *   <li><b>Good</b>  = 4</li>
 *   <li><b>Easy</b>  = 5</li>
 * </ul></p>
 */
public final class SM2Scheduler {

    public static final double DEFAULT_EF = 2.5;
    public static final double MIN_EF     = 1.3;

    // UI quality constants
    public static final int QUALITY_AGAIN = 1;
    public static final int QUALITY_HARD  = 3;
    public static final int QUALITY_GOOD  = 4;
    public static final int QUALITY_EASY  = 5;

    private SM2Scheduler() { }

    /**
     * Computes the new schedule.
     *
     * @param quality      0–5. Below 3 resets repetitions.
     * @param repetitions  number of successful reviews so far
     * @param intervalDays current interval in days
     * @param easeFactor   current ease factor (>= MIN_EF)
     * @return new schedule; never null
     */
    public static Schedule calculate(int quality,
                                     int repetitions,
                                     int intervalDays,
                                     double easeFactor) {

        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("Quality must be 0–5, was " + quality);
        }

        // Update ease factor
        double newEF = easeFactor
                + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        if (newEF < MIN_EF) newEF = MIN_EF;

        int newReps;
        int newInterval;

        if (quality < 3) {
            // Failed recall — reset
            newReps = 0;
            newInterval = 1;
        } else {
            newReps = repetitions + 1;
            if (newReps == 1)      newInterval = 1;
            else if (newReps == 2) newInterval = 6;
            else                   newInterval = (int) Math.round(intervalDays * newEF);
            if (newInterval < 1) newInterval = 1;
            if (newInterval > 365 * 5) newInterval = 365 * 5;   // cap at 5 years
        }

        LocalDate next = LocalDate.now().plusDays(newInterval);
        return new Schedule(newReps, newInterval, newEF, next);
    }

    /** Immutable result holder. */
    public record Schedule(int repetitions,
                           int intervalDays,
                           double easeFactor,
                           LocalDate nextReviewDate) { }
}