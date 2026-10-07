import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Lecture #56 - Executor Framework Deep Dive | ThreadPool, Future & Callable
 *              (part 1: the executor flavours and the Future contract)
 *
 * Modes (pass as args[0]):
 *   fixedPool     - 5 tasks on a 2-thread pool: threads are REUSED
 *   singleThread  - one worker: tasks run in submission order
 *   cachedPool    - a pool that grows on demand and reuses idle threads
 *   scheduled     - schedule() and scheduleAtFixedRate()
 *   futureGet     - Future.isDone() then get()
 *   futureTimeout - get(timeout) gives up with a TimeoutException
 *   futureCancel  - cancel(true) then get() throws CancellationException
 *   all           - runs every mode (default)
 */
public class Multithreading01_ExecutorServiceAndFuture {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "fixedPool":     fixedPool();     break;
            case "singleThread":  singleThread();  break;
            case "cachedPool":    cachedPool();    break;
            case "scheduled":     scheduled();     break;
            case "futureGet":     futureGet();     break;
            case "futureTimeout": futureTimeout(); break;
            case "futureCancel":  futureCancel();  break;
            case "all": fixedPool(); singleThread(); cachedPool(); scheduled();
                        futureGet(); futureTimeout(); futureCancel(); break;
            default:
                System.out.println("usage: java -cp out Multithreading01_ExecutorServiceAndFuture "
                        + "[fixedPool|singleThread|cachedPool|scheduled|futureGet|futureTimeout|futureCancel|all]");
        }
    }

    private static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ------------------------------------------------------------ fixedPool
    private static void fixedPool() throws InterruptedException {
        System.out.println("=== fixedPool: 5 tasks on 2 worker threads ===");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Set<String> workers = java.util.Collections.synchronizedSet(new TreeSet<>());

        try {
            for (int i = 1; i <= 5; i++) {
                final int taskId = i;
                executor.execute(() -> {
                    workers.add(Thread.currentThread().getName());
                    System.out.println("    task " + taskId + " performed by " + Thread.currentThread().getName());
                    nap(150);                       // long enough to force queueing
                });
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        System.out.println("  5 tasks, but only " + workers.size() + " distinct worker thread(s): " + workers);
    }

    // --------------------------------------------------------- singleThread
    private static void singleThread() throws InterruptedException {
        System.out.println();
        System.out.println("=== singleThread: one worker, submission order preserved ===");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        StringBuilder order = new StringBuilder();

        try {
            for (int i = 1; i <= 5; i++) {
                final int taskId = i;
                executor.execute(() -> order.append(taskId).append(' '));
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        System.out.println("  execution order: " + order.toString().trim() + "   (always 1 2 3 4 5)");
        System.out.println("  (a pool of 2 gives NO such guarantee - see fixedPool above.)");
    }

    // ----------------------------------------------------------- cachedPool
    private static void cachedPool() throws InterruptedException {
        System.out.println();
        System.out.println("=== cachedPool: grows on demand, reuses idle threads ===");
        ExecutorService executor = Executors.newCachedThreadPool();
        Set<String> workers = java.util.Collections.synchronizedSet(new TreeSet<>());

        try {
            for (int i = 1; i <= 6; i++) {
                executor.execute(() -> {
                    workers.add(Thread.currentThread().getName());
                    nap(200);                       // all six overlap -> six threads
                });
            }
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // now use one single task again: the pool reuses an idle thread
            Set<String> second = java.util.Collections.synchronizedSet(new TreeSet<>());
            ExecutorService pool2 = Executors.newCachedThreadPool();
            pool2.execute(() -> second.add(Thread.currentThread().getName()));
            pool2.shutdown();
            pool2.awaitTermination(5, TimeUnit.SECONDS);
            System.out.println("  6 overlapping tasks -> " + workers.size() + " threads created: " + workers);
            System.out.println("  a later single task in a fresh cached pool -> " + second.size() + " thread (created on demand)");
        } finally {
            executor.shutdownNow();
        }
    }

    // ------------------------------------------------------------ scheduled
    private static void scheduled() throws InterruptedException {
        System.out.println();
        System.out.println("=== scheduled: delay, and a repeating task that we cancel ===");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        long t0 = System.nanoTime();

        try {
            scheduler.schedule(
                    () -> System.out.println("    one-shot ran after "
                            + (System.nanoTime() - t0) / 1_000_000 + " ms"),
                    300, TimeUnit.MILLISECONDS);

            final int[] beats = { 0 };
            Future<?> repeating = scheduler.scheduleAtFixedRate(() -> {
                beats[0]++;
                System.out.println("    repeating beat " + beats[0]);
            }, 0, 200, TimeUnit.MILLISECONDS);

            nap(700);
            repeating.cancel(false);
            System.out.println("    repeating cancelled after ~700 ms; beats so far = " + beats[0]);
            nap(150);
        } finally {
            scheduler.shutdownNow();
        }
    }

    // ------------------------------------------------------------ futureGet
    private static void futureGet() throws InterruptedException, ExecutionException {
        System.out.println();
        System.out.println("=== futureGet: submit a Callable, then collect the result ===");

        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            Future<Integer> f = executor.submit((Callable<Integer>) () -> {
                nap(400);
                return 42;
            });

            System.out.println("  right after submit : isDone() = " + f.isDone());
            nap(600);
            System.out.println("  after it finished  : isDone() = " + f.isDone());
            System.out.println("  f.get()            = " + f.get());
            System.out.println("  f.get() again      = " + f.get() + "   (a Future caches its result)");
        } finally {
            executor.shutdownNow();
        }
    }

    // -------------------------------------------------------- futureTimeout
    private static void futureTimeout() throws InterruptedException, ExecutionException {
        System.out.println();
        System.out.println("=== futureTimeout: get(timeout) does not wait forever ===");

        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            Future<String> f = executor.submit(() -> {
                nap(800);
                return "slow result";
            });

            try {
                f.get(200, TimeUnit.MILLISECONDS);
                System.out.println("  unexpected: got a value in time");
            } catch (TimeoutException e) {
                System.out.println("  get(200 ms) threw TimeoutException - the task is still running");
            }
            System.out.println("  isDone() after the timeout = " + f.isDone());
            System.out.println("  the task was NOT cancelled; get() now = " + f.get());
        } finally {
            executor.shutdownNow();
        }
    }

    // --------------------------------------------------------- futureCancel
    private static void futureCancel() throws InterruptedException {
        System.out.println();
        System.out.println("=== futureCancel: cancel(true) interrupts a running task ===");

        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            Future<String> f = executor.submit(() -> {
                try {
                    Thread.sleep(5000);          // sleep DIRECTLY so the interrupt surfaces here
                    return "finished normally";
                } catch (InterruptedException e) {
                    System.out.println("    task: I was interrupted, so I am giving up");
                    Thread.currentThread().interrupt();
                    return "interrupted";
                }
            });

            nap(200);
            boolean cancelled = f.cancel(true);
            System.out.println("  cancel(true) returned " + cancelled);
            System.out.println("  isCancelled() = " + f.isCancelled() + ", isDone() = " + f.isDone());

            try {
                f.get();
                System.out.println("  unexpected: get() returned");
            } catch (CancellationException e) {
                System.out.println("  get() on a cancelled Future throws CancellationException");
            } catch (ExecutionException e) {
                System.out.println("  unexpected: " + e);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
