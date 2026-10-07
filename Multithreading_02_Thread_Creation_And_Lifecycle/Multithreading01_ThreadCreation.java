/**
 * Lecture #48 - Java Thread Creation & Lifecycle Explained from Scratch (part 1: how to CREATE a thread)
 *
 * Modes (pass as args[0]):
 *   extend       - a class extends Thread and overrides run()
 *   runnable     - a class implements Runnable, handed to a Thread
 *   lambda       - Runnable as a lambda (Java 8+)
 *   startVsRun   - run() executes on the CALLER, start() creates a new thread
 *   startTwice   - calling start() twice throws IllegalThreadStateException
 *   nameAndId    - default thread names, ids and setName()
 *   all          - runs every mode (default)
 */
public class Multithreading01_ThreadCreation {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "extend":     extend();     break;
            case "runnable":   runnable();   break;
            case "lambda":     lambda();     break;
            case "startVsRun": startVsRun(); break;
            case "startTwice": startTwice(); break;
            case "nameAndId":  nameAndId();  break;
            case "all":        extend(); runnable(); lambda(); startVsRun(); startTwice(); nameAndId(); break;
            default:
                System.out.println("usage: java -cp out Multithreading01_ThreadCreation "
                        + "[extend|runnable|lambda|startVsRun|startTwice|nameAndId|all]");
        }
    }

    // ---- way 1: extend Thread -------------------------------------------
    static class ExtendingThread extends Thread {
        ExtendingThread() { super("extender-1"); }

        @Override
        public void run() {
            System.out.println("  run() is executing on: " + Thread.currentThread().getName());
        }
    }

    // ---- way 2: implement Runnable --------------------------------------
    static class ImplementingRunnable implements Runnable {
        @Override
        public void run() {
            System.out.println("  run() is executing on: " + Thread.currentThread().getName());
        }
    }

    private static void extend() throws InterruptedException {
        System.out.println("=== extend: class MyThread extends Thread ===");
        Thread t = new ExtendingThread();
        System.out.println("  state before start(): " + t.getState());
        t.start();               // NEW -> RUNNABLE, JVM asks the OS for a real thread
        t.join();                // wait until this thread dies
        System.out.println("  state after join()  : " + t.getState());
    }

    private static void runnable() throws InterruptedException {
        System.out.println();
        System.out.println("=== runnable: class MyRunnable implements Runnable ===");
        Runnable job = new ImplementingRunnable();
        Thread t = new Thread(job, "runnable-thread");   // same job, our own Thread object
        t.start();
        t.join();
    }

    private static void lambda() throws InterruptedException {
        System.out.println();
        System.out.println("=== lambda: Runnable as a lambda (Java 8+) ===");
        Runnable job = () -> System.out.println("  run() is executing on: " + Thread.currentThread().getName());
        Thread t = new Thread(job, "lambda-thread");
        t.start();
        t.join();
    }

    private static void startVsRun() throws InterruptedException {
        System.out.println();
        System.out.println("=== startVsRun: run() vs start() ===");
        Thread t = new Thread(
                () -> System.out.println("  inside run(), current thread = " + Thread.currentThread().getName()),
                "my-thread");

        System.out.println("  calling t.run() directly, current thread = " + Thread.currentThread().getName());
        t.run();     // plain method call on the CALLER's stack - NO new thread
        System.out.println("  calling t.start(), current thread = " + Thread.currentThread().getName());
        t.start();   // NOW a new thread exists and runs run()
        t.join();
    }

    private static void startTwice() throws InterruptedException {
        System.out.println();
        System.out.println("=== startTwice: a thread can be started only once ===");
        Thread t = new Thread(() -> System.out.println("  ran once on " + Thread.currentThread().getName()), "twice-thread");
        t.start();
        t.join();
        try {
            t.start();
        } catch (IllegalThreadStateException e) {
            System.out.println("  second start() threw: " + e);
        }
    }

    private static void nameAndId() {
        System.out.println();
        System.out.println("=== nameAndId: default names, ids, setName() ===");
        Thread t1 = new Thread(() -> { });
        Thread t2 = new Thread(() -> { });

        System.out.println("  default name of 1st thread : " + t1.getName());
        System.out.println("  default name of 2nd thread : " + t2.getName());
        System.out.println("  threadId of 1st thread     : " + t1.threadId());
        System.out.println("  threadId of 2nd thread     : " + t2.threadId());
        System.out.println("  threadId of main           : " + Thread.currentThread().threadId());

        t1.setName("renamed-1");
        System.out.println("  after t1.setName(\"renamed-1\") : " + t1.getName());
    }
}
