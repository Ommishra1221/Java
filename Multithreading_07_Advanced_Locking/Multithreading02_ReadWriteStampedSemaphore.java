import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * Lecture #53 - Java Locks | ReentrantLock, ReadWriteLock, StampedLock, Semaphore & Condition
 *              (part 2: the specialised lock types)
 *
 * Modes (pass as args[0]):
 *   readWriteLock    - many readers at once, writers exclusive
 *   stampedOptimistic- an optimistic read that validates, and falls back when it does not
 *   semaphore        - limit how many threads may run a section at once
 *   condition        - two Conditions on one lock: a real bounded buffer
 *   all              - runs every mode (default)
 */
public class Multithreading02_ReadWriteStampedSemaphore {

    static final long WORK_MS = 300;

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "readWriteLock":     readWriteLock();     break;
            case "stampedOptimistic": stampedOptimistic(); break;
            case "semaphore":         semaphore();         break;
            case "condition":         condition();         break;
            case "all": readWriteLock(); stampedOptimistic(); semaphore(); condition(); break;
            default:
                System.out.println("usage: java -cp out Multithreading02_ReadWriteStampedSemaphore "
                        + "[readWriteLock|stampedOptimistic|semaphore|condition|all]");
        }
    }

    static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    static String me() { return Thread.currentThread().getName(); }

    /** Starts every thread, joins all of them, returns the elapsed milliseconds. */
    static long runAll(Thread[] threads) throws InterruptedException {
        long t0 = System.nanoTime();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        return (System.nanoTime() - t0) / 1_000_000;
    }

    // ------------------------------------------------------- readWriteLock
    static class RwResource {
        private int value = 0;
        private final ReadWriteLock rw = new ReentrantReadWriteLock();
        private final Lock readLock = rw.readLock();        // shared
        private final Lock writeLock = rw.writeLock();      // exclusive

        int read() {
            readLock.lock();
            try {
                System.out.println("    " + me() + " reading...");
                nap(WORK_MS);
                System.out.println("    " + me() + " read value = " + value);
                return value;
            } finally { readLock.unlock(); }
        }

        void write(int newValue) {
            writeLock.lock();
            try {
                System.out.println("    " + me() + " writing...");
                nap(WORK_MS);
                value = newValue;
                System.out.println("    " + me() + " wrote value = " + value);
            } finally { writeLock.unlock(); }
        }
    }

    private static void readWriteLock() throws InterruptedException {
        System.out.println("=== readWriteLock: readers share, writers exclude ===");
        RwResource r = new RwResource();

        Thread[] readers = { new Thread(r::read, "r1"), new Thread(r::read, "r2"), new Thread(r::read, "r3") };
        long readMs = runAll(readers);
        System.out.println("  3 readers x " + WORK_MS + " ms -> " + readMs + " ms  (CONCURRENT ~" + WORK_MS + " ms)");

        Thread[] writers = {
                new Thread(() -> r.write(5), "w1"),
                new Thread(() -> r.write(7), "w2"),
                new Thread(() -> r.write(9), "w3") };
        long writeMs = runAll(writers);
        System.out.println("  3 writers x " + WORK_MS + " ms -> " + writeMs + " ms  (SERIALIZED ~"
                + (3 * WORK_MS) + " ms)");
        System.out.println("  Reads never conflict with each other; a write conflicts with everything.");
    }

    // --------------------------------------------------- stampedOptimistic
    static class StampedResource {
        private int value = 0;
        private final StampedLock lock = new StampedLock();

        int optimisticRead(String tag) {
            long stamp = lock.tryOptimisticRead();      // NO lock taken
            int current = value;
            nap(200);                                   // simulate some work on the copy
            if (!lock.validate(stamp)) {
                System.out.println("    " + tag + ": stamp is INVALID -> pessimistic readLock()");
                stamp = lock.readLock();
                try { current = value; } finally { lock.unlockRead(stamp); }
            } else {
                System.out.println("    " + tag + ": optimistic read validated (no lock was needed)");
            }
            System.out.println("    " + tag + ": value = " + current);
            return current;
        }

        void write(int newValue) {
            long stamp = lock.writeLock();
            try {
                value = newValue;
                System.out.println("    " + me() + " wrote value = " + value + "  (invalidates optimistic reads)");
            } finally {
                lock.unlockWrite(stamp);
            }
        }
    }

    private static void stampedOptimistic() throws InterruptedException {
        System.out.println();
        System.out.println("=== stampedOptimistic: a read that may not need the lock at all ===");

        // (a) nobody writes while we read -> the optimistic read is validated
        StampedResource r1 = new StampedResource();
        r1.optimisticRead("quiet-read");

        // (b) a writer changes the value mid-read -> validation fails -> fall back
        System.out.println();
        StampedResource r2 = new StampedResource();
        Thread writer = new Thread(() -> { nap(50); r2.write(99); }, "writer");
        writer.start();
        r2.optimisticRead("racy-read");
        writer.join();
    }

    // ------------------------------------------------------------ semaphore
    private static void semaphore() throws InterruptedException {
        System.out.println();
        System.out.println("=== semaphore: only N threads at a time ===");

        final Semaphore permits = new Semaphore(2);         // 2 at a time
        final int tasks = 4;

        Thread[] ts = new Thread[tasks];
        for (int i = 0; i < tasks; i++) {
            final int id = i + 1;
            ts[i] = new Thread(() -> {
                try {
                    permits.acquire();
                    try {
                        System.out.println("    task-" + id + " acquired (free permits = "
                                + permits.availablePermits() + ")");
                        nap(WORK_MS);
                    } finally {
                        permits.release();
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }, "task-" + id);
        }

        long ms = runAll(ts);
        System.out.println("  " + tasks + " tasks x " + WORK_MS + " ms with 2 permits -> " + ms
                + " ms  (2 batches ~" + (2 * WORK_MS) + " ms)");
    }

    // ------------------------------------------------------------ condition
    static class BoundedBuffer {
        private final Lock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();
        private final int[] items = new int[2];            // capacity 2
        private int count, putIndex, takeIndex;

        void put(int value) throws InterruptedException {
            lock.lock();
            try {
                while (count == items.length) notFull.await();   // 'while', never 'if'
                items[putIndex] = value;
                putIndex = (putIndex + 1) % items.length;
                count++;
                System.out.println("    producer put " + value + "   (size = " + count + ")");
                notEmpty.signalAll();
            } finally { lock.unlock(); }
        }

        int take() throws InterruptedException {
            lock.lock();
            try {
                while (count == 0) notEmpty.await();
                int value = items[takeIndex];
                takeIndex = (takeIndex + 1) % items.length;
                count--;
                System.out.println("    consumer took " + value + "  (size = " + count + ")");
                notFull.signalAll();
                return value;
            } finally { lock.unlock(); }
        }
    }

    private static void condition() throws InterruptedException {
        System.out.println();
        System.out.println("=== condition: two wait sets on ONE lock ===");

        final int items = 6;
        BoundedBuffer buffer = new BoundedBuffer();

        Thread producer = new Thread(() -> {
            try { for (int i = 1; i <= items; i++) buffer.put(i); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "producer");

        Thread consumer = new Thread(() -> {
            try { for (int i = 1; i <= items; i++) buffer.take(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("  Producers wait on notFull, consumers on notEmpty - separate wait sets.");
    }
}
