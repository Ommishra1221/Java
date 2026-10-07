/**
 * Lecture #51 - Monitor Locks in Java | Synchronized Keyword, Static Sync & Custom Locks
 *              (part 1: the INSTANCE monitor == "this")
 *
 * Modes (pass as args[0]):
 *   synchronizedMethod - two threads, one object, one synchronized method: serialized
 *   twoMethods         - two DIFFERENT synchronized methods on the same object: still serialized
 *   twoObjects         - the same method on two objects: independent locks, concurrent
 *   reentrant          - a synchronized method calling another one: the lock is re-entrant
 *   blockedState       - the loser of a lock race is BLOCKED
 *   all                - runs every mode (default)
 *
 * Run:
 *     javac -Xlint:all -d out *.java
 *     java -cp out Multithreading01_InstanceLocks all
 */
public class Multithreading01_InstanceLocks {

    static final long WORK_MS = 300;

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "synchronizedMethod": synchronizedMethod(); break;
            case "twoMethods":         twoMethods();         break;
            case "twoObjects":         twoObjects();         break;
            case "reentrant":          reentrant();          break;
            case "blockedState":       blockedState();       break;
            case "all":
                synchronizedMethod(); twoMethods(); twoObjects(); reentrant(); blockedState();
                break;
            default:
                System.out.println("usage: java -cp out Multithreading01_InstanceLocks "
                        + "[synchronizedMethod|twoMethods|twoObjects|reentrant|blockedState|all]");
        }
    }

    // ------------------------------------------------------------- helpers
    static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    static String me() { return Thread.currentThread().getName(); }

    /** Starts both threads, joins both, returns the elapsed milliseconds. */
    static long runBoth(Thread a, Thread b) throws InterruptedException {
        long t0 = System.nanoTime();
        a.start();
        b.start();
        a.join();
        b.join();
        return (System.nanoTime() - t0) / 1_000_000;
    }

    static void verdict(String what, long ms, boolean serialized) {
        System.out.println("  " + what + " -> " + ms + " ms  ("
                + (serialized ? "SERIALIZED ~" + (2 * WORK_MS) + " ms" : "CONCURRENT ~" + WORK_MS + " ms") + ")");
    }

    // ------------------------------------------------------ the resource
    static class InstanceLockResource {

        // all three synchronized methods share ONE monitor: this
        synchronized void show(String tag) {
            System.out.println("    " + me() + " entered " + tag);
            nap(WORK_MS);
            System.out.println("    " + me() + " exited  " + tag);
        }

        synchronized void m1() { show("m1()"); }

        synchronized void m2() { show("m2()"); }

        // re-entrancy: the same thread can take the same monitor again
        synchronized void outer() {
            System.out.println("    " + me() + " in outer(), holdsLock(this) = " + Thread.holdsLock(this));
            inner();
            System.out.println("    " + me() + " back in outer()");
        }

        synchronized void inner() {
            System.out.println("    " + me() + " in inner(), holdsLock(this) = " + Thread.holdsLock(this));
        }
    }

    // ------------------------------------------------- synchronizedMethod
    private static void synchronizedMethod() throws InterruptedException {
        System.out.println("=== synchronizedMethod: two threads, ONE object, one synchronized method ===");
        InstanceLockResource r = new InstanceLockResource();
        long ms = runBoth(new Thread(() -> r.show("show()"), "t1"),
                          new Thread(() -> r.show("show()"), "t2"));
        verdict("same object, same method", ms, true);
    }

    // ------------------------------------------------------------ twoMethods
    private static void twoMethods() throws InterruptedException {
        System.out.println();
        System.out.println("=== twoMethods: m1() and m2() are different methods, same lock ===");
        InstanceLockResource r = new InstanceLockResource();
        long ms = runBoth(new Thread(r::m1, "t1"),
                          new Thread(r::m2, "t2"));
        verdict("same object, different methods", ms, true);
        System.out.println("  Both methods are 'synchronized', so both take the monitor 'this'.");
    }

    // ------------------------------------------------------------ twoObjects
    private static void twoObjects() throws InterruptedException {
        System.out.println();
        System.out.println("=== twoObjects: the SAME method, but two different objects ===");
        InstanceLockResource r1 = new InstanceLockResource();
        InstanceLockResource r2 = new InstanceLockResource();
        long ms = runBoth(new Thread(() -> r1.show("r1.show()"), "t1"),
                          new Thread(() -> r2.show("r2.show()"), "t2"));
        verdict("two objects", ms, false);
        System.out.println("  Locking 'this' of r1 does not block anyone locking 'this' of r2.");
    }

    // ------------------------------------------------------------ reentrant
    private static void reentrant() throws InterruptedException {
        System.out.println();
        System.out.println("=== reentrant: outer() calls inner(), both synchronized ===");
        InstanceLockResource r = new InstanceLockResource();
        Thread t = new Thread(r::outer, "t1");
        t.start();
        t.join();
        System.out.println("  The same thread took the same monitor twice - no self-deadlock.");
    }

    // --------------------------------------------------------- blockedState
    private static void blockedState() throws InterruptedException {
        System.out.println();
        System.out.println("=== blockedState: the thread that cannot get the lock is BLOCKED ===");
        InstanceLockResource r = new InstanceLockResource();

        Thread t1 = new Thread(() -> r.show("holder"), "t1");
        Thread t2 = new Thread(() -> r.show("waiter"), "t2");

        t1.start();
        nap(100);                       // let t1 own the monitor first
        t2.start();
        nap(100);                       // give t2 time to try and fail

        System.out.println("  t1 state: " + t1.getState());
        System.out.println("  t2 state: " + t2.getState());

        t1.join();
        t2.join();
    }
}
