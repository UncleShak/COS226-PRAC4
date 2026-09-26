import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

public class ListCorrectnessTest
{
    private static final int THREADS = 8;
    private static final int ROUNDS = 20;

    // Thread t works on keys [t * STEP, t * STEP + SPAN). SPAN > STEP, so
    // neighbouring threads overlap and duplicate add/remove calls are guaranteed.
    private static final int STEP = 50;
    private static final int SPAN = 200;
    private static final int MAX_KEY = (THREADS - 1) * STEP + SPAN; // exclusive

    // Only even numbers are ever inserted (value = 2 * key), so odd numbers
    // lie between inserted values and must never be reported as present.
    private static int valueOf(int key)
    {
        return key * 2;
    }

    public static void main(String[] args)
    {
        check("CoarseList", () -> {
            CoarseList list = new CoarseList();
            return runRound(list::add, list::remove, list::contains);
        });

        // Enable once FineList is implemented (it currently returns false everywhere,
        // so it would fail immediately).
        /*
        check("FineList", () -> {
            FineList list = new FineList();
            return runRound(list::add, list::remove, list::contains);
        });
        */
    }

    // Runs ROUNDS rounds, each on a fresh list. A round returns null on success,
    // or a failure reason.
    private static void check(String name, Supplier<String> round)
    {
        for (int r = 1; r <= ROUNDS; r++)
        {
            String failure = round.get();
            if (failure != null)
            {
                System.out.println(name + ": FAIL (round " + r + "): " + failure);
                return;
            }
        }
        System.out.println(name + ": PASS (" + ROUNDS + " rounds, " + THREADS + " threads)");
    }

    private static String runRound(IntPredicate add, IntPredicate remove, IntPredicate contains)
    {
        int distinct = MAX_KEY;

        // 1 + 2. Concurrent adds with overlapping ranges.
        int added = runConcurrently(add);
        if (added < 0)
        {
            return "exception thrown during concurrent add()";
        }
        if (added != distinct)
        {
            return "successful add() calls = " + added + ", expected " + distinct
                    + (added > distinct ? " (duplicates inserted)" : " (values lost)");
        }

        // 3. contains() is true for every inserted value, false for everything else.
        for (int k = 0; k < MAX_KEY; k++)
        {
            if (!contains.test(valueOf(k)))
            {
                return "contains(" + valueOf(k) + ") = false after it was added";
            }
        }
        String bad = findPresentNonMember(contains);
        if (bad != null)
        {
            return bad;
        }

        // 4. Concurrent removes with overlapping ranges.
        int removed = runConcurrently(remove);
        if (removed < 0)
        {
            return "exception thrown during concurrent remove()";
        }
        if (removed != distinct)
        {
            return "successful remove() calls = " + removed + ", expected " + distinct;
        }
        for (int k = 0; k < MAX_KEY; k++)
        {
            if (contains.test(valueOf(k)))
            {
                return "contains(" + valueOf(k) + ") = true after all values were removed";
            }
        }
        return findPresentNonMember(contains);
    }

    // Values that were never inserted: odd values in between, plus values outside the range.
    private static String findPresentNonMember(IntPredicate contains)
    {
        int[] outside = { -1, -2, -100, valueOf(MAX_KEY), valueOf(MAX_KEY) + 1, valueOf(MAX_KEY) + 100 };
        for (int v : outside)
        {
            if (contains.test(v))
            {
                return "contains(" + v + ") = true but it was never inserted";
            }
        }
        for (int k = 0; k < MAX_KEY; k++)
        {
            int between = valueOf(k) + 1;
            if (contains.test(between))
            {
                return "contains(" + between + ") = true but it was never inserted";
            }
        }
        return null;
    }

    // Each thread applies op to its (overlapping) key range; returns the total
    // number of calls that returned true, or -1 if any thread threw.
    private static int runConcurrently(IntPredicate op)
    {
        AtomicInteger successes = new AtomicInteger();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch startGate = new CountDownLatch(1);
        Thread[] threads = new Thread[THREADS];

        for (int t = 0; t < THREADS; t++)
        {
            final int threadID = t;
            threads[t] = new Thread(() -> {
                try
                {
                    startGate.await();
                    int local = 0;
                    int from = threadID * STEP;
                    int to = from + SPAN;
                    // Alternate direction so threads collide from both ends of their ranges.
                    if (threadID % 2 == 0)
                    {
                        for (int k = from; k < to; k++)
                        {
                            if (op.test(valueOf(k))) local++;
                        }
                    }
                    else
                    {
                        for (int k = to - 1; k >= from; k--)
                        {
                            if (op.test(valueOf(k))) local++;
                        }
                    }
                    successes.addAndGet(local);
                }
                catch (Throwable e)
                {
                    error.compareAndSet(null, e);
                }
            });
            threads[t].start();
        }

        startGate.countDown(); // release all threads at once to maximise contention
        for (Thread thread : threads)
        {
            try
            {
                thread.join();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return -1;
            }
        }

        if (error.get() != null)
        {
            error.get().printStackTrace();
            return -1;
        }
        return successes.get();
    }
}
