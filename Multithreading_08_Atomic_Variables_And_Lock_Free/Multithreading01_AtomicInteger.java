import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * Lecture #54 - Lock-Free Concurrency in Java | AtomicVariables & CAS Explained
 *              (part 1: AtomicInteger)
 *
 * Modes (pass as args[0]):
 *   race          - plain count++ loses updates
 *   atomic        - the same work with AtomicInteger: exact
 *   operations    - the whole AtomicInteger method family, with real values
 *   retryEvidence - proves CAS retries: the update lambda runs MORE than once per update
 *   all           - runs every mode (default)
 */
public class Multithreading01_AtomicInteger {

    static final int THREADS = 4;
    static final int OPS = 100_000;

    static int plainCount;                              // not atomic
    static final AtomicInteger atomicCount = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "race":          race();          break;
            case "atomic":        atomic();        break;
            case "operations":    operations();    break;
            case "retryEvidence": retryEvidence(); break;
            case "all": race(); atomic(); operations(); retryEvidence(); break;
            default:
                System.out.println("usage: java -cp out Multithreading01_AtomicInteger "
                        + "[race|atomic|operations|retryEvidence|all]");
        }
    }

    private static long runThreads(IntConsumer op) throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        long t0 = System.nanoTime();
        for (int i = 0; i < THREADS; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> { for (int k = 0; k < OPS; k++) op.accept(k); }, "worker-" + id);
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        return (System.nanoTime() - t0) / 1_000_000;
    }

    // ---------------------------------------------------------------- race
    private static void race() throws InterruptedException {
        System.out.println("=== race: plain int++ from " + THREADS + " threads ===");
        plainCount = 0;
        long ms = runThreads(k -> plainCount++);
        System.out.println("  expected : " + (THREADS * OPS));
        System.out.println("  actual   : " + plainCount);
        System.out.println("  lost     : " + (THREADS * OPS - plainCount) + "   (" + ms + " ms)");
    }

    // -------------------------------------------------------------- atomic
    private static void atomic() throws InterruptedException {
        System.out.println();
        System.out.println("=== atomic: AtomicInteger.incrementAndGet() ===");
        atomicCount.set(0);
        long ms = runThreads(k -> atomicCount.incrementAndGet());
        System.out.println("  expected : " + (THREADS * OPS));
        System.out.println("  actual   : " + atomicCount.get());
        System.out.println("  lost     : " + (THREADS * OPS - atomicCount.get()) + "   (" + ms + " ms, no lock taken)");
    }

    // ---------------------------------------------------------- operations
    private static void operations() {
        System.out.println();
        System.out.println("=== operations: the AtomicInteger API, step by step ===");
        AtomicInteger a = new AtomicInteger(10);

        System.out.println("  new AtomicInteger(10)");
        System.out.println("  get()                              = " + a.get());
        System.out.println("  getAndIncrement()                  = " + a.getAndIncrement() + "   (OLD value)");
        System.out.println("  incrementAndGet()                  = " + a.incrementAndGet() + "   (NEW value)");
        System.out.println("  getAndAdd(5)                       = " + a.getAndAdd(5));
        System.out.println("  addAndGet(-3)                      = " + a.addAndGet(-3));
        System.out.println("  updateAndGet(v -> v * 2)           = " + a.updateAndGet(v -> v * 2));
        System.out.println("  accumulateAndGet(7, Integer::sum)  = " + a.accumulateAndGet(7, Integer::sum));
        System.out.println("  compareAndSet(-999, 0)             = " + a.compareAndSet(-999, 0)
                + "   (expected mismatch -> value stays " + a.get() + ")");
        System.out.println("  compareAndSet(" + a.get() + ", 0)             = " + a.compareAndSet(a.get(), 0)
                + "   -> " + a.get());
        System.out.println("  getAndSet(99)                      = " + a.getAndSet(99) + "   -> " + a.get());
    }

    // ------------------------------------------------------- retryEvidence
    private static void retryEvidence() throws InterruptedException {
        System.out.println();
        System.out.println("=== retryEvidence: one update may run the lambda several times ===");

        final AtomicInteger value = new AtomicInteger();
        final AtomicInteger lambdaCalls = new AtomicInteger();

        Thread[] ts = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            final int id = i + 1;
            ts[i] = new Thread(() -> {
                for (int k = 0; k < OPS; k++) {
                    value.updateAndGet(v -> {
                        lambdaCalls.incrementAndGet();      // counts every ATTEMPT
                        return v + 1;
                    });
                }
            }, "worker-" + id);
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();

        int successes = THREADS * OPS;
        int calls = lambdaCalls.get();
        System.out.println("  successful updates : " + successes);
        System.out.println("  value              : " + value.get());
        System.out.println("  lambda invocations : " + calls);
        System.out.println("  wasted attempts    : " + (calls - successes)
                + "   <- each one is a failed CAS that was retried");
        System.out.println("  => the update function MUST be side-effect free, because it can run more than once.");
    }
}
