public class Main {

    private static final int[] THREAD_COUNTS = {2, 4, 8, 16};
    private static final int OPERATIONS_PER_THREAD = 20000;
    private static final int RUNS_PER_CONFIG = 5;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("Warming up JVM...");
        runCoarse(4, OPERATIONS_PER_THREAD);
        runFine(4, OPERATIONS_PER_THREAD);

        System.out.printf("%-10s %-22s %-22s%n", "Threads", "Coarse-Grained (ms)", "Fine-Grained (ms)");

        for (int threads : THREAD_COUNTS) {
            double coarseAvg = averageOf(RUNS_PER_CONFIG, () -> runCoarse(threads, OPERATIONS_PER_THREAD));
            double fineAvg = averageOf(RUNS_PER_CONFIG, () -> runFine(threads, OPERATIONS_PER_THREAD));

            System.out.printf("%-10d %-22.3f %-22.3f%n", threads, coarseAvg, fineAvg);
        }
    }

    // --- Benchmark plumbing ---

    private interface TimedRun {
        double run() throws InterruptedException;
    }

    private static double averageOf(int runs, TimedRun timedRun) throws InterruptedException {
        double total = 0;
        for (int i = 0; i < runs; i++) {
            total += timedRun.run();
        }
        return total / runs;
    }

    private static double runCoarse(int numberOfThreads, int operationsPerThread) throws InterruptedException {
        CoarseList list = new CoarseList();
        return runWorkload(list::add, list::contains, list::remove, numberOfThreads, operationsPerThread);
    }

    private static double runFine(int numberOfThreads, int operationsPerThread) throws InterruptedException {
        FineList list = new FineList();
        return runWorkload(list::add, list::contains, list::remove, numberOfThreads, operationsPerThread);
    }

    private interface IntPredicateOp {
        boolean apply(int value);
    }

    private static double runWorkload(IntPredicateOp add, IntPredicateOp contains, IntPredicateOp remove,
                                       int numberOfThreads, int operationsPerThread) throws InterruptedException {

        Thread[] threads = new Thread[numberOfThreads];
        long startTime = System.nanoTime();

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadID = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    int value = (threadID * 1000) + (j % 1000);

                    if (j % 3 == 0) {
                        add.apply(value);
                    } else if (j % 3 == 1) {
                        contains.apply(value);
                    } else {
                        remove.apply(value);
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000.0;
    }
}
