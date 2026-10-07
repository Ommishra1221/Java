import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lecture #56 - Executor Framework Deep Dive | ThreadPool, Future & Callable
 *              (part 2: what is inside a thread pool - and how it fails)
 *
 * Modes (pass as args[0]):
 *   customPool          - watch core -> queue -> max grow, step by step
 *   rejection           - a full pool + full queue rejects the next task
 *   executeVsSubmit     - execute() leaks the exception, submit() captures it
 *   shutdownVsShutdownNow - graceful shutdown vs draining the queue
 *   all                 - runs every mode (default)
 */
public class Multithreading02_ThreadPoolInternals {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "customPool":          customPool();          break;
            case "rejection":           rejection();           break;
            case "executeVsSubmit":     executeVsSubmit();     break;
            case "shutdownVsShutdownNow": shutdownVsShutdownNow(); break;
            case "all": customPool(); rejection(); executeVsSubmit(); shutdownVsShutdownNow(); break;
            default:
                System.out.println("usage: java -cp out Multithreading02_ThreadPoolInternals "
                        + "[customPool|rejection|executeVsSubmit|shutdownVsShutdownNow|all]");
        }
    }

    private static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** Names the pool's threads so the output is readable. */
    private static ThreadFactory namedFactory(String prefix, AtomicInteger counter) {
        return r -> new Thread(r, prefix + "-" + counter.incrementAndGet());
    }

    private static ThreadPoolExecutor build(ThreadFactory factory) {
        return new ThreadPoolExecutor(
                2,                                   // corePoolSize
                5,                                   // maximumPoolSize
                2, TimeUnit.SECONDS,                 // keep-alive for extra threads
                new ArrayBlockingQueue<>(2),         // the work queue holds only 2
                factory);
    }

    // ----------------------------------------------------------- customPool
    private static void customPool() throws InterruptedException {
        System.out.println("=== customPool: core 2 / max 5 / queue capacity 2 ===");
        final AtomicInteger seq = new AtomicInteger();
        ThreadPoolExecutor pool = build(namedFactory("worker", seq));
        System.out.println("  core=2  max=5  queue=2   -> 7 tasks fit; the 8th cannot");

        try {
            for (int i = 1; i <= 7; i++) {
                final int taskId = i;
                pool.execute(() -> { nap(300); });
                System.out.printf("  after submit #%d : poolSize=%d active=%d queue=%d%n",
                        taskId, pool.getPoolSize(), pool.getActiveCount(), pool.getQueue().size());
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
        System.out.println("  Order of events: fill the 2 CORE threads, then the QUEUE, then grow to MAX.");
        System.out.println("  completed tasks = " + pool.getCompletedTaskCount());
    }

    // ------------------------------------------------------------ rejection
    private static void rejection() throws InterruptedException {
        System.out.println();
        System.out.println("=== rejection: more work than the pool can accept ===");
        final AtomicInteger seq = new AtomicInteger();
        ThreadPoolExecutor pool = build(namedFactory("worker", seq));

        try {
            for (int i = 1; i <= 8; i++) {
                final int taskId = i;
                try {
                    pool.execute(() -> { nap(300); });
                    System.out.println("  submit #" + taskId + " accepted");
                } catch (RejectedExecutionException e) {
                    System.out.println("  submit #" + taskId + " REJECTED: " + e);
                }
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
        System.out.println("  Default policy is AbortPolicy: it throws instead of silently dropping the task.");
    }

    // ------------------------------------------------------ executeVsSubmit
    private static void executeVsSubmit() throws InterruptedException {
        System.out.println();
        System.out.println("=== executeVsSubmit: where does the exception go? ===");

        final AtomicInteger seq = new AtomicInteger();
        final AtomicInteger escaped = new AtomicInteger();
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "worker-" + seq.incrementAndGet());
            t.setUncaughtExceptionHandler((thread, ex) -> {
                System.out.println("    [uncaught handler] " + thread.getName() + " died with " + ex);
                escaped.incrementAndGet();
            });
            return t;
        };
        ExecutorService pool = Executors.newFixedThreadPool(1, factory);

        try {
            System.out.println("  execute(() -> throw ...):");
            pool.execute(() -> { throw new RuntimeException("boom from execute()"); });
            nap(300);

            System.out.println("  submit(() -> throw ...):");
            Future<?> f = pool.submit(() -> { throw new RuntimeException("boom from submit()"); });
            nap(300);
            System.out.println("    the pool is still alive; the failure is inside the Future");
            try {
                f.get();
            } catch (ExecutionException e) {
                System.out.println("    f.get() threw ExecutionException, cause = " + e.getCause());
            }
        } finally {
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
        System.out.println("  exceptions that escaped to the thread = " + escaped.get()
                + "   (execute() only; submit() captured its own into the Future)");
    }

    // -------------------------------------------------- shutdownVsShutdownNow
    private static void shutdownVsShutdownNow() throws InterruptedException {
        System.out.println();
        System.out.println("=== shutdown() vs shutdownNow() ===");

        // (a) graceful
        ThreadPoolExecutor pool1 = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
        for (int i = 1; i <= 5; i++) {
            pool1.execute(() -> nap(150));
        }
        System.out.println("  before shutdown()  : queue=" + pool1.getQueue().size());
        pool1.shutdown();
        boolean finished = pool1.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("  shutdown()         : terminated=" + finished
                + ", completed=" + pool1.getCompletedTaskCount() + "  (queued work still ran)");

        // (b) immediate
        ThreadPoolExecutor pool2 = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
        for (int i = 1; i <= 5; i++) {
            pool2.execute(() -> nap(150));
        }
        nap(30);                                     // let the first task start
        List<Runnable> dropped = pool2.shutdownNow();
        boolean finished2 = pool2.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("  shutdownNow()      : terminated=" + finished2
                + ", queued tasks returned=" + dropped.size()
                + ", completed=" + pool2.getCompletedTaskCount() + "  (queued work was DISCARDED)");
        System.out.println("  shutdown()  = 'finish what you have';  shutdownNow() = 'interrupt and give me the backlog'.");
    }
}
