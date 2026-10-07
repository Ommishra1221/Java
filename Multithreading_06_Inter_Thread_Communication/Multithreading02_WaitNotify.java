import java.util.concurrent.CountDownLatch;

/**
 * Lecture #52 - Inter Thread Communication in Java | wait(), notify(), notifyAll() Deep Dive
 *              (part 2: using wait()/notify() correctly)
 *
 * Modes (pass as args[0]):
 *   waitNotify         - the correct single-slot producer/consumer handshake
 *   waitReleasesLock   - wait() RELEASES the monitor (sleep() does not)
 *   notifyVsNotifyAll  - notify() wakes ONE waiter; notifyAll() wakes all
 *   ifVsWhile          - a spurious wake-up breaks 'if'; 'while' survives it
 *   all                - runs every mode (default)
 */
public class Multithreading02_WaitNotify {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "waitNotify":        waitNotify();        break;
            case "waitReleasesLock":  waitReleasesLock();  break;
            case "notifyVsNotifyAll": notifyVsNotifyAll(); break;
            case "ifVsWhile":         ifVsWhile();         break;
            case "all": waitNotify(); waitReleasesLock(); notifyVsNotifyAll(); ifVsWhile(); break;
            default:
                System.out.println("usage: java -cp out Multithreading02_WaitNotify "
                        + "[waitNotify|waitReleasesLock|notifyVsNotifyAll|ifVsWhile|all]");
        }
    }

    static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ----------------------------------------------------------- waitNotify
    /** Single-slot box guarded by wait()/notify(). The 'while' loops are essential. */
    static class Box {
        private int item;
        private boolean full = false;

        synchronized void produce(int value) throws InterruptedException {
            while (full) wait();               // 'while', never 'if'
            item = value;
            full = true;
            System.out.println("    producer put  " + value);
            notify();
        }

        synchronized int consume() throws InterruptedException {
            while (!full) wait();              // 'while', never 'if'
            int value = item;
            full = false;
            System.out.println("    consumer took " + value);
            notify();
            return value;
        }
    }

    private static void waitNotify() throws InterruptedException {
        System.out.println("=== waitNotify: the correct handshake ===");

        final int items = 6;
        Box box = new Box();

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= items; i++) box.produce(i);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "producer");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= items; i++) box.consume();
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("  every item produced was consumed exactly once, in order.");
    }

    // ----------------------------------------------------- waitReleasesLock
    private static void waitReleasesLock() throws InterruptedException {
        System.out.println();
        System.out.println("=== waitReleasesLock: wait() gives the monitor away ===");

        final Object lock = new Object();

        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                System.out.println("    waiter: I hold the lock and now call wait()");
                try { lock.wait(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.println("    waiter: resumed and re-acquired the lock");
            }
        }, "waiter");

        waiter.start();
        nap(300);                              // the waiter is now parked inside wait()

        synchronized (lock) {                   // proves the monitor was released
            System.out.println("    main  : got the lock WHILE the waiter is inside wait()");
            lock.notify();
        }
        waiter.join();
        System.out.println("  (a synchronized thread that calls sleep() would still hold the lock here.)");
    }

    // ---------------------------------------------------- notifyVsNotifyAll
    private static void notifyVsNotifyAll() throws InterruptedException {
        System.out.println();
        System.out.println("=== notifyVsNotifyAll: one vs all waiters ===");

        final Object lock = new Object();
        final int waiters = 3;
        final CountDownLatch allWaiting = new CountDownLatch(waiters);
        Thread[] threads = new Thread[waiters];

        for (int i = 0; i < waiters; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                synchronized (lock) {
                    allWaiting.countDown();
                    try { lock.wait(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
                System.out.println("    w" + id + " woke up");
            }, "w" + id);
        }

        for (Thread t : threads) t.start();
        allWaiting.await();
        nap(200);                              // make sure they are all inside wait()

        System.out.println("    main: lock.notify()  ->");
        synchronized (lock) { lock.notify(); }
        nap(300);

        int stillWaiting = 0;
        for (Thread t : threads) {
            System.out.println("    " + t.getName() + " state: " + t.getState());
            if (t.getState() == Thread.State.WAITING) stillWaiting++;
        }
        System.out.println("    " + stillWaiting + " waiter(s) still WAITING: notify() woke only one");

        System.out.println("    main: lock.notifyAll() ->");
        synchronized (lock) { lock.notifyAll(); }
        for (Thread t : threads) t.join();
        System.out.println("  all waiters finished.");
    }

    // -------------------------------------------------------------- ifVsWhile
    private static void ifVsWhile() throws InterruptedException {
        System.out.println();
        System.out.println("=== ifVsWhile: a wake-up that does not mean 'there is data' ===");

        // (a) the bug: 'if' does not re-check the condition after waking up
        final Object lockA = new Object();
        final boolean[] fullA = { false };

        Thread bad = new Thread(() -> {
            synchronized (lockA) {
                try { if (!fullA[0]) lockA.wait(); }        // BUG: should be 'while'
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.println("    [if]    consumer read item = "
                        + (fullA[0] ? "a real item" : "NULL  <-- the bug"));
            }
        }, "bad-consumer");

        bad.start();
        nap(200);
        System.out.println("    main: nothing was produced, but we call notify() (a spurious wake-up)");
        synchronized (lockA) { lockA.notify(); }
        bad.join();

        // (b) the fix: 'while' re-checks, ignores the spurious wake-up, and waits again
        final Object lockB = new Object();
        final int[] itemB = { -1 };
        final boolean[] fullB = { false };

        Thread good = new Thread(() -> {
            synchronized (lockB) {
                try {
                    while (!fullB[0]) lockB.wait();          // CORRECT
                    System.out.println("    [while] consumer read item = " + itemB[0] + "  (correct)");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }, "good-consumer");

        good.start();
        nap(200);
        synchronized (lockB) { lockB.notify(); }             // spurious wake-up: ignored
        nap(200);
        System.out.println("    main: the spurious wake-up was ignored; now producing for real");
        synchronized (lockB) { itemB[0] = 7; fullB[0] = true; lockB.notify(); }
        good.join();
    }
}
