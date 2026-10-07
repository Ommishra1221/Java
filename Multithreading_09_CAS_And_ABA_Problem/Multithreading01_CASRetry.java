import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lecture #55 - Lock-Free Concurrency in Java - 2 | CAS Retry, Compare-and-Swap & ABA Problem
 *              (part 1: the CAS RETRY loop, written by hand)
 *
 * Modes (pass as args[0]):
 *   likeCounter     - the original demo: 10 threads x 10 likes via a manual CAS loop
 *   manualVsLibrary - the hand-written CAS loop compared with incrementAndGet
 *   retryGrowth     - how the number of retries grows as threads are added
 *   all             - runs every mode (default)
 */
public class Multithreading01_CASRetry {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "likeCounter":     likeCounter();     break;
            case "manualVsLibrary": manualVsLibrary(); break;
            case "retryGrowth":     retryGrowth();     break;
            case "all": likeCounter(); manualVsLibrary(); retryGrowth(); break;
            default:
                System.out.println("usage: java -cp out Multithreading01_CASRetry "
                        + "[likeCounter|manualVsLibrary|retryGrowth|all]");
        }
    }

    /**
     * The classic CAS loop, spelled out. This is EXACTLY what incrementAndGet()
     * does internally - it is the commented-out code from the original demo.
     */
    static class LikeCounter {
        private final AtomicInteger totalCount = new AtomicInteger(0);
        private final AtomicInteger retries = new AtomicInteger(0);

        void like() {
            while (true) {
                // 1. capture the latest value
                int currentCount = totalCount.get();
                // 2. work out the new value
                int finalCount = currentCount + 1;
                // 3. publish it ONLY if nobody moved the value since step 1
                if (totalCount.compareAndSet(currentCount, finalCount)) {
                    return;                       // we won
                }
                // 4. someone else got there first -> count it and retry
                retries.incrementAndGet();
            }
        }

        int getTotalLikes() { return totalCount.get(); }
        int getRetries()    { return retries.get(); }
    }

    // --------------------------------------------------------- likeCounter
    private static void likeCounter() throws InterruptedException {
        System.out.println("=== likeCounter: 10 threads x 10 likes, manual CAS loop ===");
        LikeCounter counter = new LikeCounter();

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                for (int k = 0; k < 10; k++) counter.like();
            }, "user-" + id);
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("  total likes : " + counter.getTotalLikes() + "   (expected 100)");
        System.out.println("  CAS retries : " + counter.getRetries() + "   (failed attempts that were retried)");
    }

    // ---------------------------------------------------- manualVsLibrary
    private static void manualVsLibrary() throws InterruptedException {
        System.out.println();
        System.out.println("=== manualVsLibrary: the same 400 000 updates, two ways ===");

        final int threads = 4;
        final int ops = 100_000;

        LikeCounter manual = new LikeCounter();
        final AtomicInteger library = new AtomicInteger();

        Thread[] t1 = new Thread[threads];
        long t0 = System.nanoTime();
        for (int i = 0; i < threads; i++) {
            t1[i] = new Thread(() -> { for (int k = 0; k < ops; k++) manual.like(); });
        }
        for (Thread t : t1) t.start();
        for (Thread t : t1) t.join();
        long manualMs = (System.nanoTime() - t0) / 1_000_000;

        Thread[] t2 = new Thread[threads];
        t0 = System.nanoTime();
        for (int i = 0; i < threads; i++) {
            t2[i] = new Thread(() -> { for (int k = 0; k < ops; k++) library.incrementAndGet(); });
        }
        for (Thread t : t2) t.start();
        for (Thread t : t2) t.join();
        long libraryMs = (System.nanoTime() - t0) / 1_000_000;

        System.out.println("  manual CAS loop   : total " + manual.getTotalLikes()
                + ", retries " + manual.getRetries() + ", " + manualMs + " ms");
        System.out.println("  incrementAndGet() : total " + library.get() + ", " + libraryMs + " ms");
        System.out.println("  Same answer - the library method is just this loop, without a user lambda.");
    }

    // --------------------------------------------------------- retryGrowth
    private static void retryGrowth() throws InterruptedException {
        System.out.println();
        System.out.println("=== retryGrowth: more contending threads -> more retries ===");
        System.out.println("  threads |   updates |  retries | retries per 1000 updates");

        final int ops = 50_000;
        for (int threads : new int[] { 1, 4, 8, 16 }) {
            LikeCounter counter = new LikeCounter();
            Thread[] ts = new Thread[threads];
            for (int i = 0; i < threads; i++) {
                ts[i] = new Thread(() -> { for (int k = 0; k < ops; k++) counter.like(); });
            }
            for (Thread t : ts) t.start();
            for (Thread t : ts) t.join();

            int updates = threads * ops;
            int retries = counter.getRetries();
            System.out.printf("  %7d | %9d | %8d | %.2f%n",
                    threads, updates, retries, retries * 1000.0 / updates);
        }
        System.out.println("  Every retry is wasted CPU: the thread re-reads and tries the CAS again.");
        System.out.println("  (The exact per-1000 figure is machine dependent.)");
    }
}
