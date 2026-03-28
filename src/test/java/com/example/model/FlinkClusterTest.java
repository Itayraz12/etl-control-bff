package com.example.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class FlinkClusterTest {

    // ------------------------------------------------------------------
    // Cluster metadata
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} → scalingType={1}, parallelism={2}")
    @CsvSource({
        "DYNAMIC_SMALL,  DYNAMIC, 20",
        "DYNAMIC_MEDIUM, DYNAMIC, 40",
        "DYNAMIC_LARGE,  DYNAMIC, 80",
        "FIXED_SMALL,    FIXED,   20",
        "FIXED_MEDIUM,   FIXED,   40",
        "FIXED_LARGE,    FIXED,   80",
    })
    void clusterMetadata_isCorrect(FlinkCluster cluster, ClusterScalingType expectedScaling, int expectedParallelism) {
        assertEquals(expectedScaling,    cluster.getScalingType());
        assertEquals(expectedParallelism, cluster.getParallelism());
    }

    // ------------------------------------------------------------------
    // resolve() — full matrix
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @CsvSource({
        "LOW,    DYNAMIC, DYNAMIC_SMALL",
        "MEDIUM, DYNAMIC, DYNAMIC_MEDIUM",
        "HIGH,   DYNAMIC, DYNAMIC_LARGE",
        "LOW,    FIXED,   FIXED_SMALL",
        "MEDIUM, FIXED,   FIXED_MEDIUM",
        "HIGH,   FIXED,   FIXED_LARGE",
    })
    void resolve_fullMatrix(VolumeGroup volumeGroup, ClusterScalingType scalingType, FlinkCluster expected) {
        assertEquals(expected, FlinkCluster.resolve(volumeGroup, scalingType));
    }

    // ------------------------------------------------------------------
    // resolve() — end-to-end (VolumeGroup.calculate + FlinkCluster.resolve)
    // ------------------------------------------------------------------

    @Test
    void endToEnd_lowVolumeDynamic_returnsDynamicSmall() {
        VolumeGroup group = VolumeGroup.calculate(StreamingContinuity.ONCE, RecordsPerDay.HUNDREDS);
        FlinkCluster cluster = FlinkCluster.resolve(group, ClusterScalingType.DYNAMIC);
        assertEquals(FlinkCluster.DYNAMIC_SMALL, cluster);
        assertEquals(20, cluster.getParallelism());
    }

    @Test
    void endToEnd_mediumVolumeFixed_returnsFixedMedium() {
        VolumeGroup group = VolumeGroup.calculate(StreamingContinuity.EVERY_FEW_HOURS, RecordsPerDay.MILLIONS);
        FlinkCluster cluster = FlinkCluster.resolve(group, ClusterScalingType.FIXED);
        assertEquals(FlinkCluster.FIXED_MEDIUM, cluster);
        assertEquals(40, cluster.getParallelism());
    }

    @Test
    void endToEnd_highVolumeDynamic_returnsDynamicLarge() {
        VolumeGroup group = VolumeGroup.calculate(StreamingContinuity.CONTINUOUS, RecordsPerDay.HUNDREDS_MILLIONS);
        FlinkCluster cluster = FlinkCluster.resolve(group, ClusterScalingType.DYNAMIC);
        assertEquals(FlinkCluster.DYNAMIC_LARGE, cluster);
        assertEquals(80, cluster.getParallelism());
    }

    @Test
    void endToEnd_highVolumeFixed_returnsFixedLarge() {
        VolumeGroup group = VolumeGroup.calculate(StreamingContinuity.EVERY_HOUR, RecordsPerDay.MILLIONS);
        FlinkCluster cluster = FlinkCluster.resolve(group, ClusterScalingType.FIXED);
        assertEquals(FlinkCluster.FIXED_LARGE, cluster);
        assertEquals(80, cluster.getParallelism());
    }

    // ------------------------------------------------------------------
    // Dynamic vs Fixed decision — same volume group, different scaling type
    // ------------------------------------------------------------------

    @Test
    void sameVolume_lowGroup_dynamicAndFixedPickDifferentClusters() {
        // ONCE + HUNDREDS → LOW (score 2)
        VolumeGroup group = VolumeGroup.calculate(StreamingContinuity.ONCE, RecordsPerDay.HUNDREDS);
        assertEquals(VolumeGroup.LOW, group);

        FlinkCluster dynamic = FlinkCluster.resolve(group, ClusterScalingType.DYNAMIC);
        FlinkCluster fixed   = FlinkCluster.resolve(group, ClusterScalingType.FIXED);

        assertEquals(FlinkCluster.DYNAMIC_SMALL, dynamic);
        assertEquals(FlinkCluster.FIXED_SMALL,   fixed);
        // Same parallelism cap, different scaling behaviour
        assertEquals(dynamic.getParallelism(), fixed.getParallelism());
        assertEquals(ClusterScalingType.DYNAMIC, dynamic.getScalingType());
        assertEquals(ClusterScalingType.FIXED,   fixed.getScalingType());
    }

    @Test
    void sameVolume_mediumGroup_dynamicAndFixedPickDifferentClusters() {
        // EVERY_FEW_HOURS + THOUSANDS → MEDIUM (score 5)
        VolumeGroup group = VolumeGroup.calculate(StreamingContinuity.EVERY_FEW_HOURS, RecordsPerDay.THOUSANDS);
        assertEquals(VolumeGroup.MEDIUM, group);

        FlinkCluster dynamic = FlinkCluster.resolve(group, ClusterScalingType.DYNAMIC);
        FlinkCluster fixed   = FlinkCluster.resolve(group, ClusterScalingType.FIXED);

        assertEquals(FlinkCluster.DYNAMIC_MEDIUM, dynamic);
        assertEquals(FlinkCluster.FIXED_MEDIUM,   fixed);
        assertEquals(40, dynamic.getParallelism());
        assertEquals(40, fixed.getParallelism());
        assertEquals(ClusterScalingType.DYNAMIC, dynamic.getScalingType());
        assertEquals(ClusterScalingType.FIXED,   fixed.getScalingType());
    }

    @Test
    void sameVolume_highGroup_dynamicAndFixedPickDifferentClusters() {
        // CONTINUOUS + MILLIONS → HIGH (score 9)
        VolumeGroup group = VolumeGroup.calculate(StreamingContinuity.CONTINUOUS, RecordsPerDay.MILLIONS);
        assertEquals(VolumeGroup.HIGH, group);

        FlinkCluster dynamic = FlinkCluster.resolve(group, ClusterScalingType.DYNAMIC);
        FlinkCluster fixed   = FlinkCluster.resolve(group, ClusterScalingType.FIXED);

        assertEquals(FlinkCluster.DYNAMIC_LARGE, dynamic);
        assertEquals(FlinkCluster.FIXED_LARGE,   fixed);
        assertEquals(80, dynamic.getParallelism());
        assertEquals(80, fixed.getParallelism());
        assertEquals(ClusterScalingType.DYNAMIC, dynamic.getScalingType());
        assertEquals(ClusterScalingType.FIXED,   fixed.getScalingType());
    }

    // ------------------------------------------------------------------
    // Dynamic vs Fixed — boundary scores where volume group flips
    // ------------------------------------------------------------------

    @Test
    void boundary_lastLowScore_dynamicGoesToSmall_fixedGoesToSmall() {
        // score = 2 + 2 = 4 → last LOW
        VolumeGroup group = VolumeGroup.calculate(StreamingContinuity.EVERY_DAY, RecordsPerDay.THOUSANDS);
        assertEquals(VolumeGroup.LOW, group);
        assertEquals(FlinkCluster.DYNAMIC_SMALL, FlinkCluster.resolve(group, ClusterScalingType.DYNAMIC));
        assertEquals(FlinkCluster.FIXED_SMALL,   FlinkCluster.resolve(group, ClusterScalingType.FIXED));
    }

    @Test
    void boundary_firstMediumScore_dynamicGoesToMedium_fixedGoesToMedium() {
        // score = 2 + 3 = 5 → first MEDIUM
        VolumeGroup group = VolumeGroup.calculate(StreamingContinuity.EVERY_DAY, RecordsPerDay.HUN_THOUSANDS);
        assertEquals(VolumeGroup.MEDIUM, group);
        assertEquals(FlinkCluster.DYNAMIC_MEDIUM, FlinkCluster.resolve(group, ClusterScalingType.DYNAMIC));
        assertEquals(FlinkCluster.FIXED_MEDIUM,   FlinkCluster.resolve(group, ClusterScalingType.FIXED));
    }

    @Test
    void boundary_lastMediumScore_dynamicGoesToMedium_fixedGoesToMedium() {
        // score = 4 + 3 = 7 → last MEDIUM
        VolumeGroup group = VolumeGroup.calculate(StreamingContinuity.EVERY_HOUR, RecordsPerDay.HUN_THOUSANDS);
        assertEquals(VolumeGroup.MEDIUM, group);
        assertEquals(FlinkCluster.DYNAMIC_MEDIUM, FlinkCluster.resolve(group, ClusterScalingType.DYNAMIC));
        assertEquals(FlinkCluster.FIXED_MEDIUM,   FlinkCluster.resolve(group, ClusterScalingType.FIXED));
    }

    @Test
    void boundary_firstHighScore_dynamicGoesToLarge_fixedGoesToLarge() {
        // score = 4 + 4 = 8 → first HIGH
        VolumeGroup group = VolumeGroup.calculate(StreamingContinuity.EVERY_HOUR, RecordsPerDay.MILLIONS);
        assertEquals(VolumeGroup.HIGH, group);
        assertEquals(FlinkCluster.DYNAMIC_LARGE, FlinkCluster.resolve(group, ClusterScalingType.DYNAMIC));
        assertEquals(FlinkCluster.FIXED_LARGE,   FlinkCluster.resolve(group, ClusterScalingType.FIXED));
    }

    // ------------------------------------------------------------------
    // Parallelism comparison — dynamic cap equals fixed value per tier
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "For {0} volume: dynamic max parallelism == fixed parallelism")
    @CsvSource({
        "LOW,    DYNAMIC_SMALL,  FIXED_SMALL",
        "MEDIUM, DYNAMIC_MEDIUM, FIXED_MEDIUM",
        "HIGH,   DYNAMIC_LARGE,  FIXED_LARGE",
    })
    void parallelism_dynamicMaxEqualsFixedValue_perTier(VolumeGroup volumeGroup,
                                                        FlinkCluster expectedDynamic,
                                                        FlinkCluster expectedFixed) {
        FlinkCluster dynamic = FlinkCluster.resolve(volumeGroup, ClusterScalingType.DYNAMIC);
        FlinkCluster fixed   = FlinkCluster.resolve(volumeGroup, ClusterScalingType.FIXED);

        assertEquals(expectedDynamic, dynamic);
        assertEquals(expectedFixed,   fixed);
        assertEquals(dynamic.getParallelism(), fixed.getParallelism(),
                "Dynamic max and fixed parallelism should be equal for the same tier");
    }

    // ------------------------------------------------------------------
    // Merged: continuity + recordsPerDay + scalingType → volumeGroup + cluster + parallelism
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "{0} + {1} + {2} → {3} → {4} (parallelism={5})")
    @CsvSource({
        // LOW volume — ONCE always goes DYNAMIC regardless of requested scaling
        "ONCE,            HUNDREDS,   DYNAMIC, LOW,    DYNAMIC_SMALL,  20",
        "ONCE,            HUNDREDS,   FIXED,   LOW,    DYNAMIC_SMALL,  20",
        // MEDIUM volume (non-ONCE, so scaling type is honoured)
        "EVERY_FEW_HOURS, THOUSANDS,  DYNAMIC, MEDIUM, DYNAMIC_MEDIUM, 40",
        "EVERY_FEW_HOURS, THOUSANDS,  FIXED,   MEDIUM, FIXED_MEDIUM,   40",
        // HIGH volume (non-ONCE, so scaling type is honoured)
        "CONTINUOUS,      MILLIONS,   DYNAMIC, HIGH,   DYNAMIC_LARGE,  80",
        "CONTINUOUS,      MILLIONS,   FIXED,   HIGH,   FIXED_LARGE,    80",
    })
    void merged_continuityAndRecordsAndScaling_resolvesToCorrectCluster(
            StreamingContinuity continuity,
            RecordsPerDay recordsPerDay,
            ClusterScalingType scalingType,
            VolumeGroup expectedVolumeGroup,
            FlinkCluster expectedCluster,
            int expectedParallelism) {

        VolumeGroup volumeGroup = VolumeGroup.calculate(continuity, recordsPerDay);
        FlinkCluster cluster    = FlinkCluster.resolve(volumeGroup, scalingType, continuity);

        assertEquals(expectedVolumeGroup,  volumeGroup,               "volume group");
        assertEquals(expectedCluster,      cluster,                   "cluster");
        assertEquals(expectedParallelism,  cluster.getParallelism(),  "parallelism");
    }

    // ------------------------------------------------------------------
    // ONCE always routes to DYNAMIC — never wastes a fixed slot
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "ONCE + {0} records, requested FIXED → still DYNAMIC (cluster={1})")
    @CsvSource({
        "HUNDREDS,          DYNAMIC_SMALL",
        "THOUSANDS,         DYNAMIC_SMALL",
        "HUN_THOUSANDS,     DYNAMIC_SMALL",
        "MILLIONS,          DYNAMIC_MEDIUM",
        "TENS_MILLIONS,     DYNAMIC_MEDIUM",
        "HUNDREDS_MILLIONS, DYNAMIC_MEDIUM",
    })
    void once_alwaysRoutesToDynamic_evenWhenFixedRequested(RecordsPerDay recordsPerDay, FlinkCluster expectedCluster) {
        VolumeGroup volumeGroup = VolumeGroup.calculate(StreamingContinuity.ONCE, recordsPerDay);
        FlinkCluster cluster    = FlinkCluster.resolve(volumeGroup, ClusterScalingType.FIXED, StreamingContinuity.ONCE);

        assertEquals(expectedCluster,           cluster,                  "cluster");
        assertEquals(ClusterScalingType.DYNAMIC, cluster.getScalingType(), "scaling type must be DYNAMIC for ONCE");
    }

    @ParameterizedTest(name = "Non-ONCE {0} + requested FIXED → FIXED cluster")
    @CsvSource({
        "EVERY_HOUR,      MILLIONS,          FIXED_LARGE",
        "EVERY_DAY,       THOUSANDS,         FIXED_SMALL",
        "EVERY_FEW_HOURS, HUNDREDS_MILLIONS, FIXED_LARGE",
        "CONTINUOUS,      HUNDREDS,          FIXED_MEDIUM",
    })
    void nonOnce_fixedRequestHonoured(StreamingContinuity continuity, RecordsPerDay recordsPerDay, FlinkCluster expectedCluster) {
        VolumeGroup volumeGroup = VolumeGroup.calculate(continuity, recordsPerDay);
        FlinkCluster cluster    = FlinkCluster.resolve(volumeGroup, ClusterScalingType.FIXED, continuity);

        assertEquals(expectedCluster,          cluster,                  "cluster");
        assertEquals(ClusterScalingType.FIXED, cluster.getScalingType(), "scaling type must be FIXED for non-ONCE");
    }

    // ------------------------------------------------------------------
    // Scaling type sanity
    // ------------------------------------------------------------------

    @Test
    void dynamicClusters_allHaveDynamicScalingType() {
        for (FlinkCluster cluster : FlinkCluster.values()) {
            if (cluster.name().startsWith("DYNAMIC")) {
                assertEquals(ClusterScalingType.DYNAMIC, cluster.getScalingType(),
                        cluster + " should be DYNAMIC");
            }
        }
    }

    @Test
    void fixedClusters_allHaveFixedScalingType() {
        for (FlinkCluster cluster : FlinkCluster.values()) {
            if (cluster.name().startsWith("FIXED")) {
                assertEquals(ClusterScalingType.FIXED, cluster.getScalingType(),
                        cluster + " should be FIXED");
            }
        }
    }
}



