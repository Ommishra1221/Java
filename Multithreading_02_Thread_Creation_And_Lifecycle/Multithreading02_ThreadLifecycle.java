import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lecture #48 - Java Thread Creation & Lifecycle Explained from Scratch (part 2: the LIFECYCLE)
 *
 * Modes (pass as args[0]):
 *   states       - NEW -> RUNNABLE -> TIMED_WAITING -> TERMINATED
 *   blocked      - a thread waiting for a monitor lock is BLOCKED
 *   waiting      - join() with no timeout leaves the joiner in WAITING
 *   timedWaiting - join(ms) leaves the joiner in TIMED_WAITING
 *   order        - thread scheduling is non-deterministic (no fixed order)
 *   all          - runs every mode (default)
 */
public class Multithreading02_ThreadLifecycle {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "states":       states();       break;
            case "blocked":      blocked();      break;
            case "waiting":      waiting();      break;
            case "timedWaiting": timedWaiting(); break;
            case "order":        order();        break;
            case "all":          states(); blocked(); waiting(); timedWaiting(); order(); break;
            default:
                System.out.println("usage: java -cp out Multithreading02_ThreadLifecycle "
                        + "[states|blocked|waiting|timedWaiting|order|all]");
        }
    }

    private static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ---------------------------------------------------------------- states
    private static void states() throws InterruptedException {
        System.out.println("=== states: NEW -> RUNNABLE -> TIMED_WAITING -> TERMINATED ===");

        AtomicBoolean go = new AtomicBoolean(false);
        Thread worker = new Thread(() -> {
            while (!go.get()) { /* busy spin: stays RUNNABLE */ }
            nap(600);           // -> TIMED_WAITING
        }, "worker");

        System.out.println("before start()          : " + worker.getState());  // NEW

        worker.start();
        nap(100);                                    // give the OS time to schedule it
        System.out.println("after start(), spinning : " + worker.getState());  // RUNNABLE

        go.set(true);                                // let it fall into sleep()
        nap(100);
        System.out.println("while in sleep(600)     : " + worker.getState());  // TIMED_WAITING

        worker.join();
        System.out.println("after join()            : " + worker.getState());  // TERMINATED
    }

    // --------------------------------------------------------------- blocked
    private static void blocked() throws InterruptedException {
        System.out.println();
        System.out.println("=== blocked: waiting for a monitor lock ===");

        final Object lock = new Object();
        CountDownLatch holderHasLock = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            synchronized (lock) {
                holderHasLock.countDown();   // tell main: "I own the lock now"
                nap(600);
            }
        }, "holder");

        Thread waiter = new Thread(() -> {
            try { holderHasLock.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            synchronized (lock) { }          // cannot enter until holder releases
        }, "waiter");

        holder.start();
        waiter.start();

        holderHasLock.await();   // only proceed once holder really holds the lock
        nap(150);
        System.out.println("holder state : " + holder.getState());   // TIMED_WAITING (sleeping inside the lock)
        System.out.println("waiter state : " + waiter.getState());   // BLOCKED (wants the lock)

        holder.join();
        waiter.join();
        System.out.println("after both finish: holder=" + holder.getState() + ", waiter=" + waiter.getState());
    }

    // --------------------------------------------------------------- waiting
    private static void waiting() throws InterruptedException {
        System.out.println();
        System.out.println("=== waiting: join() with NO timeout -> WAITING ===");

        Thread worker = new Thread(() -> nap(600), "worker");
        Thread joiner = new Thread(() -> {
            try { worker.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "joiner");

        worker.start();
        joiner.start();

        nap(200);
        System.out.println("joiner state while inside join():     " + joiner.getState());  // WAITING

        worker.join();
        joiner.join();
    }

    // ---------------------------------------------------------- timedWaiting
    private static void timedWaiting() throws InterruptedException {
        System.out.println();
        System.out.println("=== timedWaiting: join(ms) -> TIMED_WAITING ===");

        Thread worker = new Thread(() -> nap(900), "worker");
        Thread joiner = new Thread(() -> {
            try { worker.join(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "joiner");

        worker.start();
        joiner.start();

        nap(200);
        System.out.println("joiner state inside join(5000):       " + joiner.getState());  // TIMED_WAITING

        worker.join();
        joiner.join();
    }

    // ----------------------------------------------------------------- order
    private static void order() throws InterruptedException {
        System.out.println();
        System.out.println("=== order: no guaranteed execution order ===");

        Thread t1 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) System.out.println("  T1 -> " + i);
        }, "T1");

        Thread t2 = new Thread(() -> {
            for (int i = 1; i <= 3; i++) System.out.println("  T2 -> " + i);
        }, "T2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("both finished. The interleaving above changes from run to run.");
    }
}
