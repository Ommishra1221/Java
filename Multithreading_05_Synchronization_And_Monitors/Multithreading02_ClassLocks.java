/**
 * Lecture #51 - Monitor Locks in Java | Synchronized Keyword, Static Sync & Custom Locks
 *              (part 2: the CLASS monitor, and "custom" lock objects)
 *
 * Modes (pass as args[0]):
 *   staticSyncAndClassLiteral - static synchronized and synchronized(X.class) use ONE lock
 *   staticVsInstance          - the class lock and the instance lock are DIFFERENT -> concurrent
 *   newObjectLockBug          - synchronized (new Object()) protects nothing (each call is a new lock)
 *   customLockObject          - two different lock objects guard two different sections -> concurrent
 *   all                       - runs every mode (default)
 */
public class Multithreading02_ClassLocks {

    static final long WORK_MS = 300;

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "staticSyncAndClassLiteral": staticSyncAndClassLiteral(); break;
            case "staticVsInstance":          staticVsInstance();          break;
            case "newObjectLockBug":          newObjectLockBug();          break;
            case "customLockObject":          customLockObject();          break;
            case "all":
                staticSyncAndClassLiteral(); staticVsInstance(); newObjectLockBug(); customLockObject();
                break;
            default:
                System.out.println("usage: java -cp out Multithreading02_ClassLocks "
                        + "[staticSyncAndClassLiteral|staticVsInstance|newObjectLockBug|customLockObject|all]");
        }
    }

    static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    static String me() { return Thread.currentThread().getName(); }

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

    static class ClassLockResource {

        static int counter = 0;

        // (1) a STATIC synchronized method -> the monitor is ClassLockResource.class
        static synchronized void staticSync(String tag) {
            System.out.println("    " + me() + " entered " + tag);
            nap(WORK_MS);
            System.out.println("    " + me() + " exited  " + tag);
        }

        // (2) explicitly the SAME monitor
        static void staticBlock(String tag) {
            synchronized (ClassLockResource.class) {
                System.out.println("    " + me() + " entered " + tag);
                nap(WORK_MS);
                System.out.println("    " + me() + " exited  " + tag);
            }
        }

        // (3) an INSTANCE synchronized method -> the monitor is this instance
        synchronized void instanceSync(String tag) {
            System.out.println("    " + me() + " entered " + tag);
            nap(WORK_MS);
            System.out.println("    " + me() + " exited  " + tag);
        }

        // (4) THE BUG: a brand-new object every call locks nothing
        void brokenLock(String tag) {
            Object brandNewLock = new Object();
            synchronized (brandNewLock) {
                System.out.println("    " + me() + " entered " + tag + " (lock = " + System.identityHashCode(brandNewLock) + ")");
                nap(WORK_MS);
                System.out.println("    " + me() + " exited  " + tag);
            }
        }
    }

    // ------------------------------------------- staticSyncAndClassLiteral
    private static void staticSyncAndClassLiteral() throws InterruptedException {
        System.out.println("=== staticSync vs synchronized(X.class): the SAME monitor ===");
        long ms = runBoth(new Thread(() -> ClassLockResource.staticSync("staticSync()"), "t1"),
                          new Thread(() -> ClassLockResource.staticBlock("staticBlock()"), "t2"));
        verdict("static method vs class literal", ms, true);
        System.out.println("  Both use the monitor ClassLockResource.class.");
    }

    // --------------------------------------------------------- staticVsInstance
    private static void staticVsInstance() throws InterruptedException {
        System.out.println();
        System.out.println("=== staticVsInstance: the CLASS lock and the INSTANCE lock are different ===");
        ClassLockResource r = new ClassLockResource();
        long ms = runBoth(new Thread(() -> ClassLockResource.staticSync("staticSync()"), "t1"),
                          new Thread(() -> r.instanceSync("instanceSync()"), "t2"));
        verdict("static vs instance", ms, false);
        System.out.println("  Two monitors: ClassLockResource.class and r. No mutual exclusion between them.");
    }

    // --------------------------------------------------------- newObjectLockBug
    private static void newObjectLockBug() throws InterruptedException {
        System.out.println();
        System.out.println("=== newObjectLockBug: synchronized (new Object()) protects NOTHING ===");
        ClassLockResource r = new ClassLockResource();
        long ms = runBoth(new Thread(() -> r.brokenLock("brokenLock()"), "t1"),
                          new Thread(() -> r.brokenLock("brokenLock()"), "t2"));
        verdict("a fresh object per call", ms, false);
        System.out.println("  Each call locked a DIFFERENT object, so both entered at once.");
    }

    // -------------------------------------------------------- customLockObject
    private static void customLockObject() throws InterruptedException {
        System.out.println();
        System.out.println("=== customLockObject: one shared lock object, per purpose ===");

        final Object moneyLock = new Object();
        final Object logLock = new Object();

        Thread t1 = new Thread(() -> {
            synchronized (moneyLock) {
                System.out.println("    t1 entered money section");
                nap(WORK_MS);
                System.out.println("    t1 exited  money section");
            }
        }, "t1");

        Thread t2 = new Thread(() -> {
            synchronized (logLock) {                 // a DIFFERENT section -> different lock
                System.out.println("    t2 entered log section");
                nap(WORK_MS);
                System.out.println("    t2 exited  log section");
            }
        }, "t2");

        long ms = runBoth(t1, t2);
        verdict("two independent lock objects", ms, false);
        System.out.println("  Using one lock per independent resource lets unrelated work run at once.");
    }
}
