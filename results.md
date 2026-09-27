# Task 3 – Lock Granularity Experiment Results

## Setup
- Operations per thread: 20,000
- Workload mix: ~1/3 `add`, ~1/3 `contains`, ~1/3 `remove`
- Each configuration run 5 times (after 1 untimed warm-up run) and averaged

## Results

| Threads | Coarse-Grained Time (ms) | Fine-Grained Time (ms) |
|---|---|---|
| 2  | 146.266   | 404.201   |
| 4  | 605.011   | 1541.292  |
| 8  | 3200.968  | 7755.618  |
| 16 | 14638.504 | 49068.501 |

## Discussion

`FineList` is consistently slower than `CoarseList` across every thread count, and the gap widens as threads increase (roughly 2.8x slower at 2 threads, growing to over 3.3x at 16 threads).

This is the opposite of the speedup fine-grained locking is meant to provide, and the reason comes down to the workload rather than a bug: every operation still has to start at `head` and walk the sorted list node by node, acquiring and releasing a lock at each hop (hand-over-hand locking). With a modest list size, that per-node lock/unlock overhead outweighs the benefit of letting threads operate on different parts of the list concurrently. `CoarseList` pays one lock/unlock per operation, so its overhead stays low and predictable even as thread count rises.

The runtime growth also outpaces the growth in total work: going from 2 to 8 threads multiplies the total operations by 4x, but coarse-grained time grows ~22x and fine-grained ~19x. This reflects lock contention — as more threads compete for the same lock(s), more time is spent blocked or context-switching rather than doing useful work, so overhead scales worse than linearly with thread count.
