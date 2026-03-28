package com.example.model;

/**
 * Represents the six available Flink clusters.
 *
 * <p>There are two cluster families:
 * <ul>
 *   <li><b>DYNAMIC</b> – parallelism scales up to the stated maximum automatically.</li>
 *   <li><b>FIXED</b>   – parallelism is pinned to the stated value at all times.</li>
 * </ul>
 *
 * <pre>
 * Cluster              | Scaling  | Parallelism
 * ---------------------|----------|-----------
 * DYNAMIC_SMALL  (D20) | DYNAMIC  | max 20
 * DYNAMIC_MEDIUM (D40) | DYNAMIC  | max 40
 * DYNAMIC_LARGE  (D80) | DYNAMIC  | max 80
 * FIXED_SMALL    (F20) | FIXED    | 20
 * FIXED_MEDIUM   (F40) | FIXED    | 40
 * FIXED_LARGE    (F80) | FIXED    | 80
 * </pre>
 */
public enum FlinkCluster {

    DYNAMIC_SMALL (ClusterScalingType.DYNAMIC, 20),
    DYNAMIC_MEDIUM(ClusterScalingType.DYNAMIC, 40),
    DYNAMIC_LARGE (ClusterScalingType.DYNAMIC, 80),

    FIXED_SMALL   (ClusterScalingType.FIXED,   20),
    FIXED_MEDIUM  (ClusterScalingType.FIXED,   40),
    FIXED_LARGE   (ClusterScalingType.FIXED,   80);

    private final ClusterScalingType scalingType;
    private final int parallelism;

    FlinkCluster(ClusterScalingType scalingType, int parallelism) {
        this.scalingType = scalingType;
        this.parallelism = parallelism;
    }

    public ClusterScalingType getScalingType() { return scalingType; }
    public int getParallelism()                { return parallelism; }

    /**
     * Resolves the most appropriate cluster given a volume group, a preferred
     * scaling type, and the pipeline's streaming continuity.
     *
     * <p><b>Special rule:</b> {@link StreamingContinuity#ONCE} pipelines are always
     * routed to a <em>DYNAMIC</em> cluster regardless of the requested scaling type.
     * A ONCE pipeline runs briefly then stops — holding a slot on a FIXED cluster
     * between runs wastes resources with no traffic.
     *
     * <p>Mapping (all non-ONCE continuities):
     * <pre>
     * VolumeGroup | DYNAMIC        | FIXED
     * ------------|----------------|---------------
     * LOW         | DYNAMIC_SMALL  | FIXED_SMALL
     * MEDIUM      | DYNAMIC_MEDIUM | FIXED_MEDIUM
     * HIGH        | DYNAMIC_LARGE  | FIXED_LARGE
     * </pre>
     *
     * @param volumeGroup  the calculated volume group of the pipeline
     * @param scalingType  preferred scaling type (ignored for ONCE pipelines)
     * @param continuity   the pipeline's streaming continuity
     * @return the recommended {@link FlinkCluster}
     */
    public static FlinkCluster resolve(VolumeGroup volumeGroup, ClusterScalingType scalingType, StreamingContinuity continuity) {
        // ONCE pipelines are bursty — always use dynamic to avoid wasting a fixed slot
        ClusterScalingType effectiveScaling = (continuity == StreamingContinuity.ONCE)
                ? ClusterScalingType.DYNAMIC
                : scalingType;

        return switch (volumeGroup) {
            case LOW    -> effectiveScaling == ClusterScalingType.DYNAMIC ? DYNAMIC_SMALL  : FIXED_SMALL;
            case MEDIUM -> effectiveScaling == ClusterScalingType.DYNAMIC ? DYNAMIC_MEDIUM : FIXED_MEDIUM;
            case HIGH   -> effectiveScaling == ClusterScalingType.DYNAMIC ? DYNAMIC_LARGE  : FIXED_LARGE;
        };
    }

    /**
     * Convenience overload — derives the continuity from the same inputs used to
     * calculate the volume group.
     */
    public static FlinkCluster resolve(VolumeGroup volumeGroup, ClusterScalingType scalingType) {
        // Without continuity context, honour the requested scaling type as-is.
        // Prefer the three-argument overload when continuity is known.
        return switch (volumeGroup) {
            case LOW    -> scalingType == ClusterScalingType.DYNAMIC ? DYNAMIC_SMALL  : FIXED_SMALL;
            case MEDIUM -> scalingType == ClusterScalingType.DYNAMIC ? DYNAMIC_MEDIUM : FIXED_MEDIUM;
            case HIGH   -> scalingType == ClusterScalingType.DYNAMIC ? DYNAMIC_LARGE  : FIXED_LARGE;
        };
    }
}

