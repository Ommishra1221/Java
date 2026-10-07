import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Lecture #53 - Java Locks | ReentrantLock, ReadWriteLock, StampedLock, Semaphore & Condition
 *              (part 1: ReentrantLock - the explicit lock)
 *
 * Modes (pass as args[0]):
 *   lockUnlock       - lock()/unlock() instead of synchronized
 *   tryLock          - tryLock() never waits: it just returns false
 *   tryLockTimeout   - tryLock(ms) gives up after a deadline
 *   lockInterruptibly- a thread waiting for the lock CAN be interrupted
 *   reentrant        - the same thread may lock again (getHoldCount)
 *   unlockGuard      - only the owner may unlock
 *   all              - runs every mode (default)
 */
public class Multithreading01_ReentrantLock {

    static final long WORK_MS = 300;

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "lockUnlock":        lockUnlock();        break;
            case "tryLock":           tryLock();           break;
            case "tryLockTimeout":    tryLockTimeout();    break;
            case "lockInterruptibly": lockInterruptibly(); break;
            case "reentrant":         reentrant();         break;
            case "unlockGuard":       unlockGuard();       break;
            case "all": lockUnlock(); tryLock(); tryLockTimeout(); lockInterruptibly(); reentrant(); unlockGuard(); break;
            default:
                System.out.println("usage: java -cp out Multithreading01_ReentrantLock "
                        + "[lockUnlock|tryLock|tryLockTimeout|lockInterruptibly|reentrant|unlockGuard|all]");
        }
    }

    static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    static String me() { return Thread.currentThread().getName(); }

    static class LockResource {
        final ReentrantLock lock = new ReentrantLock();

        void work() {
            lock.lock();
            try {
                System.out.println("    " + me() + " entered");
                nap(WORK_MS);
                System.out.println("    " + me() + " exited");
            } finally {
                lock.unlock();                 // ALWAYS in a finally
            }
        }

        void holdFor(long ms) {
            lock.lock();
            try { nap(ms); } finally { lock.unlock(); }
        }

        void nested() {
            lock.lock();
            try {
                System.out.println("    holdCount after 1st lock() = " + lock.getHoldCount());
                lock.lock();
                try {
                    System.out.println("    holdCount after 2nd lock() = " + lock.getHoldCount());
                } finally {
                    lock.unlock();
                }
                System.out.println("    holdCount after 1 unlock  = " + lock.getHoldCount());
            } finally {
                lock.unlock();
            }
        }
    }

    // -------------------------------------------------------- lockUnlock
    private static void lockUnlock() throws InterruptedException {
        System.out.println("=== lockUnlock: three threads, one ReentrantLock ===");
        LockResource r = new LockResource();
        Thread[] ts = { new Thread(r::work, "t1"), new Thread(r::work, "t2"), new Thread(r::work, "t3") };

        long t0 = System.nanoTime();
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        long ms = (System.nanoTime() - t0) / 1_000_000;

        System.out.println("  3 x " + WORK_MS + " ms sections -> " + ms + " ms  (SERIALIZED ~"
                + (3 * WORK_MS) + " ms)");
    }

    // ------------------------------------------------------------ tryLock
    private static void tryLock() throws InterruptedException {
        System.out.println();
        System.out.println("=== tryLock: acquire or give up immediately ===");
        LockResource r = new LockResource();

        Thread holder = new Thread(() -> r.holdFor(600), "holder");
        holder.start();
        nap(100);                              // the holder really owns the lock now

        boolean got = r.lock.tryLock();
        System.out.println("    tryLock() while another thread holds it -> " + got);
        if (got) r.lock.unlock();
        System.out.println("    (tryLock never blocks, so the caller can do something else)");

        holder.join();
        System.out.println("    after the holder finished, tryLock() -> "
                + (r.lock.tryLock() ? "true" : "false"));
        r.lock.unlock();
    }

    // ----------------------------------------------------- tryLockTimeout
    private static void tryLockTimeout() throws InterruptedException {
        System.out.println();
        System.out.println("=== tryLockTimeout: waiting with a deadline ===");
        LockResource r = new LockResource();

        Thread holder = new Thread(() -> r.holdFor(900), "holder");
        holder.start();
        nap(100);

        long t0 = System.nanoTime();
        boolean got = r.lock.tryLock(200, TimeUnit.MILLISECONDS);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("    tryLock(200 ms) while busy -> " + got + "  (gave up after " + ms + " ms)");

        t0 = System.nanoTime();
        got = r.lock.tryLock(2000, TimeUnit.MILLISECONDS);
        ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("    tryLock(2000 ms)            -> " + got + "  (waited " + ms + " ms)");
        if (got) r.lock.unlock();

        holder.join();
    }

    // -------------------------------------------------- lockInterruptibly
    private static void lockInterruptibly() throws InterruptedException {
        System.out.println();
        System.out.println("=== lockInterruptibly: a queued thread can be cancelled ===");
        LockResource r = new LockResource();

        Thread holder = new Thread(() -> r.holdFor(1000), "holder");
        Thread waiter = new Thread(() -> {
            try {
                System.out.println("    waiter: calling lockInterruptibly()");
                r.lock.lockInterruptibly();
                try { System.out.println("    waiter: got the lock"); }
                finally { r.lock.unlock(); }
            } catch (InterruptedException e) {
                System.out.println("    waiter: InterruptedException - gave up waiting for the lock");
            }
        }, "waiter");

        holder.start();
        nap(100);
        waiter.start();
        nap(200);
        waiter.interrupt();

        holder.join();
        waiter.join();
        System.out.println("  (synchronized cannot do this - a blocked thread cannot be interrupted.)");
    }

    // ---------------------------------------------------------- reentrant
    private static void reentrant() {
        System.out.println();
        System.out.println("=== reentrant: the owner may lock again ===");
        LockResource r = new LockResource();
        r.nested();
        System.out.println("    final holdCount = " + r.lock.getHoldCount());
    }

    // -------------------------------------------------------- unlockGuard
    private static void unlockGuard() throws InterruptedException {
        System.out.println();
        System.out.println("=== unlockGuard: only the owning thread may unlock ===");
        LockResource r = new LockResource();

        Thread thief = new Thread(() -> {
            try {
                r.lock.unlock();               // this thread never locked it
                System.out.println("    no exception?!");
            } catch (IllegalMonitorStateException e) {
                System.out.println("    unlock() from a non-owner threw: " + e);
            }
        }, "thief");

        thief.start();
        thief.join();
        System.out.println("  (ReentrantLock is still better than a bare monitor here: it at least tells you.)");
    }
}
