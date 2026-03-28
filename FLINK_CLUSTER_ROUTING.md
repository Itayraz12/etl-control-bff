# Flink Cluster Routing Logic

This document describes how a pipeline is automatically assigned to a Flink cluster based on its data characteristics and preferred scaling behaviour.

---

## Overview

Three inputs drive the decision:

| Input | Type | Description |
|---|---|---|
| `StreamingContinuity` | enum | How frequently the pipeline runs |
| `RecordsPerDay` | enum | How many records are processed per day |
| `ClusterScalingType` | enum | Whether to prefer dynamic or fixed parallelism |

The outputs are:

| Output | Type | Description |
|---|---|---|
| `VolumeGroup` | enum | Intermediate classification: `LOW`, `MEDIUM`, or `HIGH` |
| `FlinkCluster` | enum | The resolved cluster to deploy on |

---

## Step 1 — Calculate Volume Group

Each input is converted to a numeric score. The scores are summed to produce a total, which maps to a `VolumeGroup`.

### Frequency score (`StreamingContinuity`)

| Value | Score |
|---|---|
| `ONCE` | 1 |
| `EVERY_DAY` | 2 |
| `EVERY_FEW_HOURS` | 3 |
| `EVERY_HOUR` | 4 |
| `CONTINUOUS` | 5 |

### Volume score (`RecordsPerDay`)

| Value | Score |
|---|---|
| `HUNDREDS` | 1 |
| `THOUSANDS` | 2 |
| `HUN_THOUSANDS` | 3 |
| `MILLIONS` | 4 |
| `TENS_MILLIONS` | 5 |
| `HUNDREDS_MILLIONS` | 6 |

### Total score → Volume group

| Total score | Volume group |
|---|---|
| ≤ 4 | `LOW` |
| 5 – 7 | `MEDIUM` |
| ≥ 8 | `HIGH` |

### Full scoring matrix

|  | HUNDREDS (1) | THOUSANDS (2) | HUN_THOUSANDS (3) | MILLIONS (4) | TENS_MILLIONS (5) | HUNDREDS_MILLIONS (6) |
|---|---|---|---|---|---|---|
| **ONCE (1)** | 2 → **LOW** | 3 → **LOW** | 4 → **LOW** | 5 → **MEDIUM** | 6 → **MEDIUM** | 7 → **MEDIUM** |
| **EVERY_DAY (2)** | 3 → **LOW** | 4 → **LOW** | 5 → **MEDIUM** | 6 → **MEDIUM** | 7 → **MEDIUM** | 8 → **HIGH** |
| **EVERY_FEW_HOURS (3)** | 4 → **LOW** | 5 → **MEDIUM** | 6 → **MEDIUM** | 7 → **MEDIUM** | 8 → **HIGH** | 9 → **HIGH** |
| **EVERY_HOUR (4)** | 5 → **MEDIUM** | 6 → **MEDIUM** | 7 → **MEDIUM** | 8 → **HIGH** | 9 → **HIGH** | 10 → **HIGH** |
| **CONTINUOUS (5)** | 6 → **MEDIUM** | 7 → **MEDIUM** | 8 → **HIGH** | 9 → **HIGH** | 10 → **HIGH** | 11 → **HIGH** |

---

## Step 2 — Resolve Flink Cluster

### Available clusters

There are six Flink clusters across two families:

| Cluster | Scaling | Parallelism |
|---|---|---|
| `DYNAMIC_SMALL` | Dynamic | max 20 |
| `DYNAMIC_MEDIUM` | Dynamic | max 40 |
| `DYNAMIC_LARGE` | Dynamic | max 80 |
| `FIXED_SMALL` | Fixed | 20 |
| `FIXED_MEDIUM` | Fixed | 40 |
| `FIXED_LARGE` | Fixed | 80 |

**Dynamic** clusters scale parallelism up to their maximum automatically and release resources when idle.  
**Fixed** clusters always hold their full parallelism slot, even when there is no traffic.

### Cluster selection matrix

| Volume group | `DYNAMIC` requested | `FIXED` requested |
|---|---|---|
| `LOW` | `DYNAMIC_SMALL` (max 20) | `FIXED_SMALL` (20) |
| `MEDIUM` | `DYNAMIC_MEDIUM` (max 40) | `FIXED_MEDIUM` (40) |
| `HIGH` | `DYNAMIC_LARGE` (max 80) | `FIXED_LARGE` (80) |

---

## Special rule — `ONCE` always uses Dynamic

> **`ONCE` pipelines are always routed to a `DYNAMIC` cluster, regardless of the requested `ClusterScalingType`.**

**Rationale:** A `ONCE` pipeline runs briefly and then stops. If it were placed on a `FIXED` cluster it would hold a parallelism slot permanently but produce no traffic between runs — a direct waste of cluster resources. A `DYNAMIC` cluster releases those resources as soon as the job finishes.

| Continuity | Requested scaling | Effective scaling |
|---|---|---|
| `ONCE` | `FIXED` | → overridden to `DYNAMIC` |
| `ONCE` | `DYNAMIC` | `DYNAMIC` (unchanged) |
| Any other | `FIXED` | `FIXED` (honoured) |
| Any other | `DYNAMIC` | `DYNAMIC` (honoured) |

---

## End-to-end examples

| Continuity | Records/day | Requested scaling | Volume group | Resolved cluster | Parallelism |
|---|---|---|---|---|---|
| `ONCE` | `HUNDREDS` | `FIXED` | LOW | `DYNAMIC_SMALL` ⚠️ overridden | 20 |
| `ONCE` | `MILLIONS` | `DYNAMIC` | MEDIUM | `DYNAMIC_MEDIUM` | 40 |
| `EVERY_DAY` | `THOUSANDS` | `FIXED` | LOW | `FIXED_SMALL` | 20 |
| `EVERY_FEW_HOURS` | `THOUSANDS` | `DYNAMIC` | MEDIUM | `DYNAMIC_MEDIUM` | 40 |
| `EVERY_FEW_HOURS` | `THOUSANDS` | `FIXED` | MEDIUM | `FIXED_MEDIUM` | 40 |
| `EVERY_HOUR` | `MILLIONS` | `FIXED` | HIGH | `FIXED_LARGE` | 80 |
| `CONTINUOUS` | `HUNDREDS_MILLIONS` | `DYNAMIC` | HIGH | `DYNAMIC_LARGE` | 80 |

---

## Transformer input types

Transformers declare how many input fields they consume via the `inputType` field.

| `inputType` | Meaning |
|---|---|
| `NONE` | Transformer produces a value with no input field (e.g. a constant). Rare. |
| `SINGLE` | Transformer operates on one input field. Most common case. |
| `MULTI` | Transformer operates on multiple input fields simultaneously. |

> Legacy pipelines may use the boolean `isMultipleInput` field instead. It is automatically mapped: `true` → `MULTI`, `false` → `SINGLE`.

---

## Java API

```java
// 1. Calculate volume group
VolumeGroup group = VolumeGroup.calculate(StreamingContinuity.CONTINUOUS, RecordsPerDay.MILLIONS);
// → HIGH

// 2. Resolve cluster (preferred — pass continuity so ONCE rule is applied)
FlinkCluster cluster = FlinkCluster.resolve(group, ClusterScalingType.FIXED, StreamingContinuity.CONTINUOUS);
// → FIXED_LARGE (parallelism 80)

// ONCE override example
FlinkCluster onceCluster = FlinkCluster.resolve(
    VolumeGroup.calculate(StreamingContinuity.ONCE, RecordsPerDay.HUNDREDS),
    ClusterScalingType.FIXED,   // requested FIXED …
    StreamingContinuity.ONCE    // … but ONCE overrides to DYNAMIC
);
// → DYNAMIC_SMALL (parallelism 20)
```

