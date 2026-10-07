import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Lecture #55 - Lock-Free Concurrency in Java - 2 | CAS Retry, Compare-and-Swap & ABA Problem
 *              (part 2: the ABA problem and its fix)
 *
 * Modes (pass as args[0]):
 *   abaPlain       - a plain AtomicReference cannot see 1 -> 2 -> 1, so the CAS wrongly succeeds
 *   abaStamped     - AtomicStampedReference adds a version, so the same CAS correctly fails
 *   mutablePayload - CAS compares REFERENCES, so a mutated object sails straight through
 *   all            - runs every mode (default)
 */
public class Multithreading02_ABAProblem {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "abaPlain":       abaPlain();       break;
            case "abaStamped":     abaStamped();     break;
            case "mutablePayload": mutablePayload(); break;
            case "all": abaPlain(); abaStamped(); mutablePayload(); break;
            default:
                System.out.println("usage: java -cp out Multithreading02_ABAProblem "
                        + "[abaPlain|abaStamped|mutablePayload|all]");
        }
    }

    // ------------------------------------------------------------- abaPlain
    private static void abaPlain() throws InterruptedException {
        System.out.println("=== abaPlain: the value goes 1 -> 2 -> 1 and the CAS still succeeds ===");

        final AtomicReference<Integer> ref = new AtomicReference<>(1);
        final CountDownLatch aHasRead = new CountDownLatch(1);
        final CountDownLatch bHasFinished = new CountDownLatch(1);

        Thread a = new Thread(() -> {
            Integer seen = ref.get();                 // A reads 1
            System.out.println("  A: I read the value " + seen);
            aHasRead.countDown();
            try { bHasFinished.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            System.out.println("  A: the value is still " + ref.get() + ", so I will CAS(" + seen + " -> 3)");
            boolean ok = ref.compareAndSet(seen, 3);
            System.out.println("  A: CAS succeeded? " + ok);
        }, "A");

        Thread b = new Thread(() -> {
            try { aHasRead.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            ref.set(2);
            System.out.println("  B: 1 -> 2");
            ref.set(1);
            System.out.println("  B: 2 -> 1   (back to where it started!)");
            bHasFinished.countDown();
        }, "B");

        a.start();
        b.start();
        a.join();
        b.join();

        System.out.println("  final value: " + ref.get());
        System.out.println("  A never knew the value left and came back -> this is the ABA problem.");
    }

    // ----------------------------------------------------------- abaStamped
    private static void abaStamped() throws InterruptedException {
        System.out.println();
        System.out.println("=== abaStamped: a version stamp makes the same CAS fail ===");

        final AtomicStampedReference<Integer> ref = new AtomicStampedReference<>(1, 0);
        final CountDownLatch aHasRead = new CountDownLatch(1);
        final CountDownLatch bHasFinished = new CountDownLatch(1);

        Thread a = new Thread(() -> {
            int[] stamp = new int[1];
            Integer seen = ref.get(stamp);            // A reads value 1, stamp 0
            System.out.println("  A: I read value " + seen + " with stamp " + stamp[0]);
            aHasRead.countDown();
            try { bHasFinished.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            System.out.println("  A: value is " + ref.getReference() + " again, so I will CAS using stamp " + stamp[0]);
            boolean ok = ref.compareAndSet(seen, 3, stamp[0], stamp[0] + 1);
            System.out.println("  A: CAS succeeded? " + ok + "   (the stamp moved)");
        }, "A");

        Thread b = new Thread(() -> {
            try { aHasRead.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            ref.set(2, 1);
            System.out.println("  B: 1 -> 2   (stamp 0 -> 1)");
            ref.set(1, 2);
            System.out.println("  B: 2 -> 1   (stamp 1 -> 2)");
            bHasFinished.countDown();
        }, "B");

        a.start();
        b.start();
        a.join();
        b.join();

        System.out.println("  final value: " + ref.getReference() + " with stamp " + ref.getStamp());
        System.out.println("  The value looks the same, but the STAMP changed - so the stale CAS is rejected.");
    }

    // ------------------------------------------------------- mutablePayload
    private static void mutablePayload() {
        System.out.println();
        System.out.println("=== mutablePayload: AtomicReference compares the REFERENCE, not the contents ===");

        AtomicReference<StringBuilder> ref = new AtomicReference<>(new StringBuilder("A"));
        StringBuilder seen = ref.get();

        seen.append("B");                             // mutate the object IN PLACE

        boolean ok = ref.compareAndSet(seen, new StringBuilder("C"));
        System.out.println("  the reference never changed, but the object became \"" + seen + "\"");
        System.out.println("  compareAndSet(seen, new) = " + ok
                + "   (same object, so the CAS cannot tell anything changed)");
        System.out.println("  value now: " + ref.get());
        System.out.println("  => put IMMUTABLE objects inside an AtomicReference.");
    }
}
