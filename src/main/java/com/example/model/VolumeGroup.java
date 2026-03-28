package com.example.model;

/**
 * Classifies a pipeline's data volume into three groups based on
 * how frequently it runs ({@link StreamingContinuity}) and how many
 * records it processes per day ({@link RecordsPerDay}).
 *
 * <p>Scoring table (frequencyScore + volumeScore):
 * <pre>
 *                  HUNDREDS THOUSANDS HUN_THOUSANDS MILLIONS TENS_MILLIONS HUNDREDS_MILLIONS
 * ONCE             2(L)     3(L)      4(L)          5(M)     6(M)          7(M)
 * EVERY_DAY        3(L)     4(L)      5(M)          6(M)     7(M)          8(H)
 * EVERY_FEW_HOURS  4(L)     5(M)      6(M)          7(M)     8(H)          9(H)
 * EVERY_HOUR       5(M)     6(M)      7(M)          8(H)     9(H)          10(H)
 * CONTINUOUS       6(M)     7(M)      8(H)          9(H)     10(H)         11(H)
 * </pre>
 * Score ≤ 4 → LOW, 5–7 → MEDIUM, ≥ 8 → HIGH
 */
public enum VolumeGroup {
    LOW,
    MEDIUM,
    HIGH;

    private static final int LOW_MAX    = 4;
    private static final int MEDIUM_MAX = 7;

    /**
     * Calculates the volume group for the given streaming continuity and records-per-day.
     *
     * @param continuity   how frequently the pipeline runs
     * @param recordsPerDay how many records are processed per day
     * @return LOW, MEDIUM, or HIGH
     */
    public static VolumeGroup calculate(StreamingContinuity continuity, RecordsPerDay recordsPerDay) {
        int total = continuity.frequencyScore() + recordsPerDay.volumeScore();
        if (total <= LOW_MAX)    return LOW;
        if (total <= MEDIUM_MAX) return MEDIUM;
        return HIGH;
    }
}

