import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * Lecture #50 - Problems in Multithreading | Race Condition, Visibility, Ordering
 *              (part 1: the RACE CONDITION and how to close the critical section)
 *
 * Modes (pass as args[0]):
 *   race               - plain count++ from many threads: updates are lost
 *   synchronizedBlock  - the same work inside synchronized (this)
 *   synchronizedMethod - the same work with a synchronized method
 *   atomic             - the same work with an AtomicInteger (lock-free)
 *   deadlock           - two locks taken in opposite order: both threads BLOCKED
 *   all                - runs every mode (default)
 *
 * Run:
 *     javac -Xlint:all -d out *.java
 *     java -cp out Multithreading01_RaceCondition all
 */
public class Multithreading01_RaceCondition {

    static final int THREADS = 4;
    static final int OPS = 100_000;          // 4 x 100 000 = 400 000 expected increments

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "race":               race();               break;
            case "synchronizedBlock":  synchronizedBlock();  break;
            case "synchronizedMethod": synchronizedMethod(); break;
            case "atomic":             atomic();             break;
            case "deadlock":           deadlock();           break;
            case "all": race(); synchronizedBlock(); synchronizedMethod(); atomic(); deadlock(); break;
            default:
                System.out.println("usage: java -cp out Multithreading01_RaceCondition "
                        + "[race|synchronizedBlock|synchronizedMethod|atomic|deadlock|all]");
        }
    }

    // ------------------------------------------------------------ helpers
    private static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** Starts THREADS threads that each run op OPS times and waits for all of them. */
    private static long runThreads(IntConsumer op) throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        long t0 = System.nanoTime();
        for (int i = 0; i < THREADS; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                for (int k = 0; k < OPS; k++) op.accept(k);
            }, "worker-" + id);
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        return (System.nanoTime() - t0) / 1_000_000;
    }

    private static void report(String label, int expected, int actual, long ms) {
        System.out.println("  expected : " + expected);
        System.out.println("  actual   : " + actual);
        System.out.println("  lost     : " + (expected - actual));
        System.out.println("  verdict  : " + (expected == actual ? "EXACT" : "LOST UPDATES")
                + "  (" + ms + " ms)");
    }

    // ------------------------------------------------- shared counter types
    static class PlainCounter {
        int count;
        void increment() { count++; }                       // NOT atomic
    }

    static class BlockCounter {
        int count;
        void increment() {
            synchronized (this) { count++; }                // explicit critical section
        }
    }

    static class MethodCounter {
        int count;
        synchronized void increment() { count++; }          // critical section = whole method
    }

    // ---------------------------------------------------------------- race
    private static void race() throws InterruptedException {
        System.out.println("=== race: plain count++ from " + THREADS + " threads ===");
        PlainCounter c = new PlainCounter();
        long ms = runThreads(k -> c.increment());
        report("plain", THREADS * OPS, c.count, ms);
        System.out.println("  count++ compiles to getfield / iadd / putfield - three steps.");
    }

    // ----------------------------------------------------- synchronizedBlock
    private static void synchronizedBlock() throws InterruptedException {
        System.out.println();
        System.out.println("=== synchronizedBlock: synchronized (this) { count++; } ===");
        BlockCounter c = new BlockCounter();
        long ms = runThreads(k -> c.increment());
        report("synchronized block", THREADS * OPS, c.count, ms);
    }

    // ---------------------------------------------------- synchronizedMethod
    private static void synchronizedMethod() throws InterruptedException {
        System.out.println();
        System.out.println("=== synchronizedMethod: synchronized void increment() ===");
        MethodCounter c = new MethodCounter();
        long ms = runThreads(k -> c.increment());
        report("synchronized method", THREADS * OPS, c.count, ms);
        System.out.println("  Both synchronized forms lock the SAME monitor: 'this'.");
    }

    // -------------------------------------------------------------- atomic
    private static void atomic() throws InterruptedException {
        System.out.println();
        System.out.println("=== atomic: AtomicInteger.incrementAndGet() ===");
        AtomicInteger c = new AtomicInteger();
        long ms = runThreads(k -> c.incrementAndGet());
        report("atomic", THREADS * OPS, c.get(), ms);
        System.out.println("  No lock is taken: the CPU performs one compare-and-swap.");
    }

    // ------------------------------------------------------------ deadlock
    private static void deadlock() throws InterruptedException {
        System.out.println();
        System.out.println("=== deadlock: two locks taken in opposite order ===");

        final Object lockA = new Object();
        final Object lockB = new Object();

        Thread t1 = new Thread(() -> {
            synchronized (lockA) {
                nap(200);                            // give T2 time to grab lockB
                synchronized (lockB) { }             // waits forever for lockB
            }
        }, "T1");

        Thread t2 = new Thread(() -> {
            synchronized (lockB) {
                nap(200);
                synchronized (lockA) { }             // waits forever for lockA
            }
        }, "T2");

        t1.setDaemon(true);                          // a deadlocked daemon must not hang the JVM
        t2.setDaemon(true);
        t1.start();
        t2.start();

        nap(700);
        System.out.println("  " + t1.getName() + " state : " + t1.getState());
        System.out.println("  " + t2.getName() + " state : " + t2.getState());
        System.out.println("  each holds what the other needs -> circular wait, neither can finish");
        System.out.println("  (daemon threads, so the JVM still exits when main returns)");
    }
}
