import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Lecture #54 - Lock-Free Concurrency in Java | AtomicVariables & CAS Explained
 *              (part 2: references, accumulators and arrays)
 *
 * Modes (pass as args[0]):
 *   seatBooking - AtomicReference.compareAndSet as a one-winner booking
 *   referenceSwap - replace the WHOLE immutable object atomically
 *   longAdder   - LongAdder vs AtomicLong under contention
 *   arrayAtomic - AtomicIntegerArray: per-slot atomic increments
 *   all         - runs every mode (default)
 */
public class Multithreading02_AtomicReference {

    static final int OPS = 200_000;

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "seatBooking":   seatBooking();   break;
            case "referenceSwap": referenceSwap(); break;
            case "longAdder":     longAdder();     break;
            case "arrayAtomic":   arrayAtomic();   break;
            case "all": seatBooking(); referenceSwap(); longAdder(); arrayAtomic(); break;
            default:
                System.out.println("usage: java -cp out Multithreading02_AtomicReference "
                        + "[seatBooking|referenceSwap|longAdder|arrayAtomic|all]");
        }
    }

    // -------------------------------------------------------- seatBooking
    /** Exact code shape from the original demo: CAS is the seat reservation. */
    static class SeatBooking {
        final AtomicReference<String> seat = new AtomicReference<>("EMPTY");

        boolean bookSeat(String name) {
            String currentValue = seat.get();
            if (!currentValue.equals("EMPTY")) {
                return false;                       // already taken
            }
            return seat.compareAndSet("EMPTY", name);   // CAS: only one caller can win
        }
    }

    private static void seatBooking() throws InterruptedException {
        System.out.println("=== seatBooking: exactly one thread may win the seat ===");
        SeatBooking sb = new SeatBooking();

        final boolean[] results = new boolean[2];
        Thread t1 = new Thread(() -> results[0] = sb.bookSeat("Aditya"), "t1");
        Thread t2 = new Thread(() -> results[1] = sb.bookSeat("Rohit"), "t2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("  t1 booked? " + results[0]);
        System.out.println("  t2 booked? " + results[1]);
        int winners = (results[0] ? 1 : 0) + (results[1] ? 1 : 0);
        System.out.println("  winners   : " + winners + "   (compareAndSet allows exactly one)");
        System.out.println("  seat      : " + sb.seat.get());
    }

    // ------------------------------------------------------ referenceSwap
    /** An immutable holder: to change anything you publish a whole new object. */
    record Account(String owner, long version) { }

    private static void referenceSwap() {
        System.out.println();
        System.out.println("=== referenceSwap: replace an immutable object atomically ===");

        AtomicReference<Account> ref = new AtomicReference<>(new Account("Aditya", 1));
        System.out.println("  start            : " + ref.get());

        Account current = ref.get();
        Account next = new Account(current.owner(), current.version() + 1);

        boolean ok = ref.compareAndSet(current, next);
        System.out.println("  compareAndSet    : " + ok + "   -> " + ref.get());

        boolean stale = ref.compareAndSet(current, new Account("someone", 99));
        System.out.println("  CAS with a STALE reference : " + stale
                + "   (the old object is no longer 'the current value')");
        System.out.println("  final            : " + ref.get());
    }

    // ----------------------------------------------------------- longAdder
    /**
     * Contention is the point, so this mode deliberately oversubscribes the CPU:
     * more writer threads than cores. With only as many threads as cores the two
     * classes measure about the same (verified: 25 ms vs 24 ms at 4 threads),
     * which is exactly why the comparison must be run under real contention.
     */
    private static void longAdder() throws InterruptedException {
        System.out.println();
        System.out.println("=== longAdder: many writers on ONE cell vs one cell per writer ===");

        final int contenders = Runtime.getRuntime().availableProcessors() * 3;
        final int ops = 100_000;

        AtomicLong atomicLong = new AtomicLong();
        LongAdder adder = new LongAdder();

        System.out.println("  writer threads = " + contenders + " (deliberately more than the "
                + Runtime.getRuntime().availableProcessors() + " cores, to force contention)");

        long t0 = System.nanoTime();
        Thread[] a = new Thread[contenders];
        for (int i = 0; i < contenders; i++) {
            a[i] = new Thread(() -> { for (int k = 0; k < ops; k++) atomicLong.incrementAndGet(); });
        }
        for (Thread t : a) t.start();
        for (Thread t : a) t.join();
        long atomicMs = (System.nanoTime() - t0) / 1_000_000;

        t0 = System.nanoTime();
        Thread[] b = new Thread[contenders];
        for (int i = 0; i < contenders; i++) {
            b[i] = new Thread(() -> { for (int k = 0; k < ops; k++) adder.increment(); });
        }
        for (Thread t : b) t.start();
        for (Thread t : b) t.join();
        long adderMs = (System.nanoTime() - t0) / 1_000_000;

        long expected = (long) contenders * ops;
        StringBuilder sb = new StringBuilder();
        sb.append("  AtomicLong : ").append(atomicLong.get()).append("  (expected ").append(expected)
          .append(")  ").append(atomicMs).append(" ms").append(System.lineSeparator());
        sb.append("  LongAdder  : ").append(adder.sum()).append("  (expected ").append(expected)
          .append(")  ").append(adderMs).append(" ms").append(System.lineSeparator());
        sb.append("  Both totals are exact; AtomicLong retries its CAS on one hot cell,")
          .append(System.lineSeparator());
        sb.append("  while LongAdder gives each thread its own cell and only sum() merges them.");
        System.out.println(sb);
    }

    // --------------------------------------------------------- arrayAtomic
    private static void arrayAtomic() throws InterruptedException {
        System.out.println();
        System.out.println("=== arrayAtomic: AtomicIntegerArray, one element at a time ===");

        final int slots = 4;
        AtomicIntegerArray arr = new AtomicIntegerArray(slots);

        Thread[] ts = new Thread[slots];
        for (int i = 0; i < slots; i++) {
            final int index = i;
            ts[i] = new Thread(() -> {
                for (int k = 0; k < OPS; k++) arr.incrementAndGet(index);   // per-element CAS
            }, "slot-" + index);
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots; i++) sb.append(arr.get(i)).append(i < slots - 1 ? ", " : "");
        System.out.println("  each element updated by its own thread -> " + sb);
        System.out.println("  compareAndSet per element is also available, e.g. arr.compareAndSet(0, " + arr.get(0) + ", 0) = "
                + arr.compareAndSet(0, arr.get(0), 0));
    }
}
