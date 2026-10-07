import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Lecture #57 - CompletableFuture, Fork-Join Pool, ThreadLocal & Virtual Threads
 *              (part 2: ForkJoinPool, ThreadLocal, Virtual Threads)
 *
 * Modes (pass as args[0]):
 *   forkJoinSum     - divide-and-conquer array sum with RecursiveTask
 *   forkJoinAction  - the same idea with RecursiveAction (no result)
 *   commonPool      - the shared pool, parallelism, and a custom pool
 *   threadLocal     - one value per thread, isolated
 *   threadLocalPool - the pool trap: stale state leaks, remove() fixes it
 *   virtualThreads  - startVirtualThread / ofVirtual / isVirtual / the carrier
 *   virtualExecutor - virtual-thread-per-task executor vs a platform pool
 *   all             - runs every mode (default)
 */
public class Multithreading02_ForkJoin_ThreadLocal_VirtualThreads {

    private static final int THRESHOLD = 100_000;

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "forkJoinSum":     forkJoinSum();     break;
            case "forkJoinPrimes":  forkJoinPrimes();  break;
            case "forkJoinAction":  forkJoinAction();  break;
            case "commonPool":      commonPool();      break;
            case "threadLocal":     threadLocal();     break;
            case "threadLocalPool": threadLocalPool(); break;
            case "virtualThreads":  virtualThreads();  break;
            case "virtualExecutor": virtualExecutor(); break;
            case "all": forkJoinSum(); forkJoinPrimes(); forkJoinAction(); commonPool(); threadLocal();
                        threadLocalPool(); virtualThreads(); virtualExecutor(); break;
            default:
                System.out.println("usage: java -cp out Multithreading02_ForkJoin_ThreadLocal_VirtualThreads "
                        + "[forkJoinSum|forkJoinPrimes|forkJoinAction|commonPool|threadLocal|threadLocalPool|virtualThreads|virtualExecutor|all]");
        }
    }

    private static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // =========================================================== ForkJoinPool

    /** Splits [lo,hi) in half until a chunk is small enough to add up directly. */
    static final class SumTask extends RecursiveTask<Long> {
        private static final long serialVersionUID = 1L;   // ForkJoinTask is Serializable
        static final Set<String> workers = ConcurrentHashMap.newKeySet();
        private final int[] data;
        private final int lo, hi;

        SumTask(int[] data, int lo, int hi) { this.data = data; this.lo = lo; this.hi = hi; }

        @Override protected Long compute() {
            workers.add(Thread.currentThread().getName());
            if (hi - lo <= THRESHOLD) {                 // BASE CASE: small enough -> just add it
                long sum = 0;
                for (int i = lo; i < hi; i++) sum += data[i];
                return sum;
            }
            int mid = (lo + hi) >>> 1;                  // DIVIDE
            SumTask left = new SumTask(data, lo, mid);
            SumTask right = new SumTask(data, mid, hi);
            left.fork();                                // push LEFT to the pool (it may be stolen)
            long rightSum = right.compute();            // do RIGHT on this thread
            long leftSum = left.join();                 // CONQUER: wait for LEFT
            return leftSum + rightSum;
        }
    }

    private static void forkJoinSum() {
        System.out.println("=== ForkJoin: parallel array sum with RecursiveTask ===");

        int n = 10_000_000;
        int[] data = new int[n];
        for (int i = 0; i < n; i++) data[i] = (i % 10) + 1;      // sum = n/10 * 55

        long t0 = System.nanoTime();
        long sequential = 0;
        for (int v : data) sequential += v;
        long sequentialMs = (System.nanoTime() - t0) / 1_000_000;

        long t1 = System.nanoTime();
        long parallel = ForkJoinPool.commonPool().invoke(new SumTask(data, 0, data.length));
        long parallelMs = (System.nanoTime() - t1) / 1_000_000;

        System.out.printf("  array length      = %d%n", n);
        System.out.printf("  sequential sum    = %d   in %d ms%n", sequential, sequentialMs);
        System.out.printf("  fork/join sum     = %d   in %d ms   (threshold = %d)%n", parallel, parallelMs, THRESHOLD);
        System.out.println("  same answer, split across the common pool -> worker threads used:");
        System.out.println("    " + SumTask.workers);
    }

    /** Counts primes in [lo,hi) by trial division - a CPU-bound leaf, so splitting actually pays. */
    static final class PrimeCountTask extends RecursiveTask<Long> {
        private static final long serialVersionUID = 1L;
        private static final int LEAF = 20_000;
        private final int lo, hi;

        PrimeCountTask(int lo, int hi) { this.lo = lo; this.hi = hi; }

        @Override protected Long compute() {
            if (hi - lo <= LEAF) {                      // BASE CASE: real CPU work per chunk
                long count = 0;
                for (int n = Math.max(2, lo); n < hi; n++) if (isPrime(n)) count++;
                return count;
            }
            int mid = (lo + hi) >>> 1;
            PrimeCountTask left = new PrimeCountTask(lo, mid);
            PrimeCountTask right = new PrimeCountTask(mid, hi);
            left.fork();
            long rightCount = right.compute();
            return left.join() + rightCount;
        }

        static boolean isPrime(int n) {
            if (n < 2) return false;
            if (n % 2 == 0) return n == 2;
            for (int d = 3; (long) d * d <= n; d += 2) if (n % d == 0) return false;
            return true;
        }
    }

    private static void forkJoinPrimes() {
        System.out.println();
        System.out.println("=== ForkJoin on a CPU-bound task: counting primes below 3,000,000 ===");
        int limit = 3_000_000;

        long t0 = System.nanoTime();
        long sequential = 0;
        for (int n = 2; n < limit; n++) if (PrimeCountTask.isPrime(n)) sequential++;
        long sequentialMs = (System.nanoTime() - t0) / 1_000_000;

        long t1 = System.nanoTime();
        long parallel = ForkJoinPool.commonPool().invoke(new PrimeCountTask(2, limit));
        long parallelMs = (System.nanoTime() - t1) / 1_000_000;

        System.out.printf("  sequential = %d primes in %d ms%n", sequential, sequentialMs);
        System.out.printf("  fork/join  = %d primes in %d ms   (%.1fx)%n", parallel, parallelMs,
                sequentialMs / (double) Math.max(1, parallelMs));
        System.out.println("  the same divide-and-conquer, but now each leaf is real CPU work.");
    }

    /** The same shape, but it mutates the array in place and returns nothing. */
    static final class IncrementTask extends RecursiveAction {
        private static final long serialVersionUID = 1L;   // ForkJoinTask is Serializable
        private final int[] data;
        private final int lo, hi;

        IncrementTask(int[] data, int lo, int hi) { this.data = data; this.lo = lo; this.hi = hi; }

        @Override protected void compute() {
            if (hi - lo <= THRESHOLD) {                 // BASE CASE
                for (int i = lo; i < hi; i++) data[i]++;
                return;
            }
            int mid = (lo + hi) >>> 1;                  // DIVIDE
            invokeAll(new IncrementTask(data, lo, mid), // fork BOTH and join both
                      new IncrementTask(data, mid, hi));
        }
    }

    private static void forkJoinAction() {
        System.out.println();
        System.out.println("=== RecursiveAction: same divide-and-conquer, no return value ===");

        int n = 400_000;
        int[] data = new int[n];
        System.out.println("  before: all elements are " + data[0] + ", " + data[n / 2] + ", " + data[n - 1]);

        ForkJoinPool.commonPool().invoke(new IncrementTask(data, 0, data.length));

        System.out.println("  after : all elements are " + data[0] + ", " + data[n / 2] + ", " + data[n - 1]);
        System.out.println("  RecursiveTask<Long> returns a value; RecursiveAction returns void.");
    }

    private static void commonPool() {
        System.out.println();
        System.out.println("=== the common pool is SHARED by everything ===");
        System.out.println("  Runtime.availableProcessors()                 = "
                + Runtime.getRuntime().availableProcessors());
        System.out.println("  ForkJoinPool.getCommonPoolParallelism()       = "
                + ForkJoinPool.getCommonPoolParallelism());
        System.out.println("  CompletableFuture.runAsync() also defaults to this pool.");

        ForkJoinPool custom = new ForkJoinPool(2);
        try {
            System.out.println("  a private new ForkJoinPool(2) parallelism     = " + custom.getParallelism());
        } finally {
            custom.shutdown();
        }
        System.out.println("  work stealing = an idle worker takes tasks from a busy worker's queue.");
    }

    // ============================================================ ThreadLocal

    private static void threadLocal() throws InterruptedException {
        System.out.println();
        System.out.println("=== ThreadLocal: every thread gets its OWN copy ===");

        ThreadLocal<Integer> counter = ThreadLocal.withInitial(() -> 0);
        Runnable work = () -> {
            for (int i = 0; i < 3; i++) counter.set(counter.get() + 1);
            System.out.println("    " + Thread.currentThread().getName() + " ends at " + counter.get());
        };

        Thread first = new Thread(work, "T1");
        Thread second = new Thread(work, "T2");
        first.start(); second.start();
        first.join(); second.join();

        System.out.println("  main's own copy is still " + counter.get() + " - untouched by T1/T2");
        System.out.println("  ThreadLocalRandom is a ThreadLocal too: "
                + ThreadLocalRandom.current().nextInt(100));
    }

    private static void threadLocalPool() throws Exception {
        System.out.println();
        System.out.println("=== the pool trap: a pooled thread REUSES its ThreadLocal ===");

        ThreadLocal<String> currentUser = new ThreadLocal<>();
        ExecutorService pool = Executors.newSingleThreadExecutor();   // ONE thread, reused by every task
        try {
            pool.submit(() -> currentUser.set("alice")).get();
            pool.submit(() -> System.out.println("    next task on the SAME thread sees currentUser = "
                    + currentUser.get() + "  <-- leaked from the previous task")).get();
            pool.submit(currentUser::remove).get();
            pool.submit(() -> System.out.println("    after remove(), currentUser = "
                    + currentUser.get())).get();
        } finally {
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
        System.out.println("  with a pool, ALWAYS remove() in a finally block - the thread outlives your request.");
    }

    // ========================================================= Virtual Threads

    private static void virtualThreads() throws InterruptedException {
        System.out.println();
        System.out.println("=== virtual threads ===");
        System.out.println("  main: " + Thread.currentThread()
                + "   isVirtual=" + Thread.currentThread().isVirtual());

        Thread plain = Thread.startVirtualThread(() ->
                System.out.println("    startVirtualThread -> " + Thread.currentThread()));
        plain.join();
        System.out.println("    plain.isVirtual = " + plain.isVirtual());

        Thread named = Thread.ofVirtual().name("my-virtual").start(() ->
                System.out.println("    builder (virtual)  -> " + Thread.currentThread()));
        named.join();

        Thread platform = Thread.ofPlatform().name("my-platform").start(() ->
                System.out.println("    builder (platform) -> " + Thread.currentThread()));
        platform.join();
        System.out.println("    platform.isVirtual = " + platform.isVirtual());
        System.out.println("  the part after '@' in a virtual thread's toString is its CARRIER thread.");
    }

    private static void virtualExecutor() throws Exception {
        System.out.println();
        System.out.println("=== virtual-thread-per-task executor vs a platform pool ===");

        int tasks = 200;
        int sleepMs = 100;
        int cores = Runtime.getRuntime().availableProcessors();

        long t0 = System.nanoTime();
        ExecutorService platform = Executors.newFixedThreadPool(cores);
        for (int i = 0; i < tasks; i++) {
            platform.submit(() -> nap(sleepMs));
        }
        platform.shutdown();
        platform.awaitTermination(1, TimeUnit.MINUTES);
        long platformMs = (System.nanoTime() - t0) / 1_000_000;

        long t1 = System.nanoTime();
        try (ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                virtual.submit(() -> nap(sleepMs));
            }
        }
        long virtualMs = (System.nanoTime() - t1) / 1_000_000;

        java.util.concurrent.atomic.AtomicBoolean allVirtual =
                new java.util.concurrent.atomic.AtomicBoolean(true);
        try (ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                virtual.submit(() -> { if (!Thread.currentThread().isVirtual()) allVirtual.set(false); });
            }
        }

        System.out.printf("  %d tasks x %d ms of blocking sleep:%n", tasks, sleepMs);
        System.out.printf("    platform pool (%d threads) : %5d ms%n", cores, platformMs);
        System.out.printf("    virtual threads            : %5d ms%n", virtualMs);
        System.out.println("    every task ran on a virtual thread = " + allVirtual.get());
        System.out.println("  a sleeping virtual thread UNMOUNTS from its carrier, so all of them wait at once.");
    }
}
