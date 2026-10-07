import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lecture #47 - Introduction to Multithreading in Java | Process vs Thread
 *
 * Modes (pass as args[0]):
 *   sequential   - two 400 ms tasks run one after another
 *   concurrent   - the same two tasks run on two threads at once
 *   race         - many threads do plain ++ on a shared int  -> lost updates
 *   raceFixed    - the same work with synchronized + AtomicInteger -> exact
 *   perThread    - static field is SHARED, ThreadLocal is PER THREAD (own stack)
 *   all          - runs everything (default, race included)
 */
public class Multithreading02_ConcurrencyAndRaces {

    private static final Object LOCK = new Object();

    private static int plain = 0;                       // read-modify-write, NOT atomic
    private static int locked = 0;                      // guarded by LOCK
    private static final AtomicInteger atomic = new AtomicInteger(0);

    private static int sharedStatic = 0;                // one copy for the whole JVM
    private static final ThreadLocal<Integer> perThread =
            ThreadLocal.withInitial(() -> 0);           // one copy per thread

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "sequential": sequential(); break;
            case "concurrent": concurrent(); break;
            case "race":       race();       break;
            case "raceFixed":  raceFixed();  break;
            case "perThread":  perThread();  break;
            case "all":
                sequential(); concurrent(); race(); raceFixed(); perThread();
                break;
            default:
                System.out.println("usage: java -cp out Multithreading02_ConcurrencyAndRaces "
                        + "[sequential|concurrent|race|raceFixed|perThread|all]");
        }
    }

    /** A task that keeps a CPU/thread busy for about 400 ms. */
    private static void task() {
        try { Thread.sleep(400); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static long msSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    // ---------------------------------------------------------- sequential
    private static void sequential() {
        System.out.println("=== sequential: one thread, two tasks ===");
        long t0 = System.nanoTime();
        task();
        task();
        System.out.println("two 400 ms tasks on ONE thread : " + msSince(t0) + " ms");
    }

    // ---------------------------------------------------------- concurrent
    private static void concurrent() {
        System.out.println();
        System.out.println("=== concurrent: two threads, two tasks ===");
        Thread a = new Thread(Multithreading02_ConcurrencyAndRaces::task, "task-A");
        Thread b = new Thread(Multithreading02_ConcurrencyAndRaces::task, "task-B");

        long t0 = System.nanoTime();
        a.start();
        b.start();
        try { a.join(); b.join(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        System.out.println("two 400 ms tasks on TWO threads: " + msSince(t0) + " ms");
        System.out.println("(about half the sequential time - the two sleeps overlap)");
    }

    // ---------------------------------------------------------------- race
    private static void race() throws InterruptedException {
        System.out.println();
        System.out.println("=== race: plain ++ from many threads ===");

        final int threadCount = 8;
        final int perThreadOps = 100_000;
        plain = 0;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int k = 0; k < perThreadOps; k++) plain++;
            }, "racer-" + (i + 1));
        }

        long t0 = System.nanoTime();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        int expected = threadCount * perThreadOps;
        System.out.println("expected : " + expected);
        System.out.println("actual   : " + plain);
        System.out.println("lost     : " + (expected - plain)
                + "   (plain ++ is read-modify-write, not atomic)");
        System.out.println("elapsed  : " + msSince(t0) + " ms");
    }

    // ----------------------------------------------------------- raceFixed
    private static void raceFixed() throws InterruptedException {
        System.out.println();
        System.out.println("=== raceFixed: synchronized block and AtomicInteger ===");

        final int threadCount = 8;
        final int perThreadOps = 100_000;
        locked = 0;
        atomic.set(0);

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int k = 0; k < perThreadOps; k++) {
                    synchronized (LOCK) { locked++; }     // mutual exclusion
                    atomic.incrementAndGet();             // CAS loop, no lock
                }
            }, "fixer-" + (i + 1));
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        int expected = threadCount * perThreadOps;
        System.out.println("expected              : " + expected);
        System.out.println("synchronized locked   : " + locked + "   -> exact");
        System.out.println("AtomicInteger         : " + atomic.get() + "   -> exact");
    }

    // ------------------------------------------------------------ perThread
    private static void perThread() throws InterruptedException {
        System.out.println();
        System.out.println("=== perThread: shared static vs ThreadLocal ===");

        final int threadCount = 2;
        final int steps = 3;
        sharedStatic = 0;

        // Indexed result so the transcript order does not depend on scheduling.
        int[] localResults = new int[threadCount];
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                perThread.set(0);                          // this thread's OWN slot
                for (int k = 0; k < steps; k++) {
                    synchronized (LOCK) { sharedStatic++; } // one shared slot
                    perThread.set(perThread.get() + 1);     // private slot
                }
                localResults[index] = perThread.get();
            }, "worker-" + (i + 1));
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("shared static field after both threads: " + sharedStatic);
        System.out.println("worker-1 private ThreadLocal          : " + localResults[0]);
        System.out.println("worker-2 private ThreadLocal          : " + localResults[1]);
        System.out.println("(same code, same field name - the ThreadLocal kept them apart)");
    }
}
