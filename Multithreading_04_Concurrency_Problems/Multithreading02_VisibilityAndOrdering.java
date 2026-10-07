/**
 * Lecture #50 - Problems in Multithreading | Race Condition, Visibility, Ordering
 *              (part 2: VISIBILITY and ORDERING)
 *
 * Modes (pass as args[0]):
 *   volatileFlag      - a volatile flag is seen promptly by a spinning reader
 *   plainFlag         - the same loop with a NON-volatile flag
 *   volatileNotAtomic - volatile int++ from many threads STILL loses updates
 *   publishData       - a plain field published behind a volatile flag is always seen
 *   all               - runs every mode (default)
 *
 * Run:
 *     javac -Xlint:all -d out *.java
 *     java -cp out Multithreading02_VisibilityAndOrdering all
 */
public class Multithreading02_VisibilityAndOrdering {

    // ---- the two flags used by the flag experiments --------------------
    static volatile boolean volatileFlag = false;
    static boolean plainFlag = false;                 // NOT volatile

    // ---- used by publishData -------------------------------------------
    static int data = 0;                              // plain field
    static volatile boolean ready = false;            // the hand-off flag

    // ---- used by volatileNotAtomic -------------------------------------
    static volatile int volatileCounter = 0;

    private static final long SPIN_LIMIT_NS = 3_000_000_000L;   // 3 seconds

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "volatileFlag":      volatileFlag();      break;
            case "plainFlag":         plainFlag();         break;
            case "volatileNotAtomic": volatileNotAtomic(); break;
            case "publishData":       publishData();       break;
            case "all": volatileFlag(); plainFlag(); volatileNotAtomic(); publishData(); break;
            default:
                System.out.println("usage: java -cp out Multithreading02_VisibilityAndOrdering "
                        + "[volatileFlag|plainFlag|volatileNotAtomic|publishData|all]");
        }
    }

    private static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * Reader spins on the flag until it flips or the 3 s budget runs out.
     * Returns the number of spins (negative if it never saw the change).
     */
    private static long spinUntilSet(java.util.function.BooleanSupplier observed) {
        long spins = 0;
        long deadline = System.nanoTime() + SPIN_LIMIT_NS;
        while (!observed.getAsBoolean()) {
            spins++;
            if (System.nanoTime() > deadline) return -spins;
        }
        return spins;
    }

    // --------------------------------------------------------- volatileFlag
    private static void volatileFlag() throws InterruptedException {
        System.out.println("=== volatileFlag: the writer flips a VOLATILE flag after 300 ms ===");
        volatileFlag = false;

        Thread writer = new Thread(() -> { nap(300); volatileFlag = true; }, "writer");
        Thread reader = new Thread(() -> {
            long t0 = System.nanoTime();
            long spins = spinUntilSet(() -> volatileFlag);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("  reader: " + (spins >= 0 ? "SAW the change" : "TIMED OUT")
                    + " after " + Math.abs(spins) + " spins / " + ms + " ms");
        }, "reader");

        reader.start();
        writer.start();
        reader.join();
        writer.join();
    }

    // ------------------------------------------------------------ plainFlag
    private static void plainFlag() throws InterruptedException {
        System.out.println();
        System.out.println("=== plainFlag: the SAME experiment, but the flag is NOT volatile ===");
        plainFlag = false;

        Thread writer = new Thread(() -> { nap(300); plainFlag = true; }, "writer");
        Thread reader = new Thread(() -> {
            long t0 = System.nanoTime();
            long spins = spinUntilSet(() -> plainFlag);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("  reader: " + (spins >= 0 ? "SAW the change" : "TIMED OUT (never saw it)")
                    + " after " + Math.abs(spins) + " spins / " + ms + " ms");
        }, "reader");

        reader.start();
        writer.start();
        reader.join();
        writer.join();
        System.out.println("  No guarantee either way: the write may never become visible, or be seen late.");
    }

    // ----------------------------------------------------- volatileNotAtomic
    private static void volatileNotAtomic() throws InterruptedException {
        System.out.println();
        System.out.println("=== volatileNotAtomic: volatile gives visibility, NOT atomicity ===");

        final int threads = 4;
        final int ops = 100_000;
        volatileCounter = 0;

        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            ts[i] = new Thread(() -> {
                for (int k = 0; k < ops; k++) volatileCounter++;   // still read-modify-write
            }, "worker-" + (i + 1));
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();

        int expected = threads * ops;
        System.out.println("  expected : " + expected);
        System.out.println("  actual   : " + volatileCounter);
        System.out.println("  lost     : " + (expected - volatileCounter));
        System.out.println("  volatile fixes visibility; ++ is still getfield/iadd/putfield.");
    }

    // ---------------------------------------------------------- publishData
    private static void publishData() throws InterruptedException {
        System.out.println();
        System.out.println("=== publishData: a volatile flag ORDERS the plain writes before it ===");
        data = 0;
        ready = false;

        Thread producer = new Thread(() -> {
            data = 42;               // plain write ...
            ready = true;            // ... then the volatile write
        }, "producer");

        Thread consumer = new Thread(() -> {
            while (!ready) { }       // volatile read
            System.out.println("  consumer read data = " + data
                    + "   (always 42: the volatile write/read pair created a happens-before edge)");
        }, "consumer");

        consumer.start();
        producer.start();
        consumer.join();
        producer.join();
    }
}
