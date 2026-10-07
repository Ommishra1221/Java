/**
 * Lecture #49 - Thread Methods | sleep, join, yield, interrupt, isAlive, priority & more
 *              (part 1: the waiting / control methods)
 *
 * Modes (pass as args[0]):
 *   sleep        - Thread.sleep(ms) pauses the CURRENT thread (it is a static method)
 *   join         - the caller waits for another thread to die
 *   joinTimeout  - join(ms) waits at most ms, then gives up
 *   isAlive      - a thread is "alive" only between start() and its death
 *   yield        - Thread.yield() is only a hint to the OS scheduler
 *   names        - currentThread() + setName()
 *   all          - runs every mode (default)
 */
public class Multithreading01_SleepJoinYieldAlive {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "sleep":       sleep();       break;
            case "join":        join();        break;
            case "joinTimeout": joinTimeout(); break;
            case "isAlive":     isAlive();     break;
            case "yield":       yieldDemo();   break;
            case "names":       names();       break;
            case "all":         sleep(); join(); joinTimeout(); isAlive(); yieldDemo(); names(); break;
            default:
                System.out.println("usage: java -cp out Multithreading01_SleepJoinYieldAlive "
                        + "[sleep|join|joinTimeout|isAlive|yield|names|all]");
        }
    }

    private static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static long msSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    // ----------------------------------------------------------------- sleep
    private static void sleep() throws InterruptedException {
        System.out.println("=== sleep: Thread.sleep(1000) pauses the CURRENT thread ===");
        long t0 = System.nanoTime();
        System.out.println("  current thread: " + Thread.currentThread().getName());
        Thread.sleep(1000);                 // static! main -> TIMED_WAITING for ~1 s
        System.out.println("  resumed after : " + msSince(t0) + " ms (>= 1000)");
    }

    // ------------------------------------------------------------------ join
    private static void join() throws InterruptedException {
        System.out.println();
        System.out.println("=== join: the caller waits until the other thread dies ===");
        Thread t1 = new Thread(() -> {
            nap(600);
            System.out.println("  T1 finished its work");
        }, "T1");

        long t0 = System.nanoTime();
        t1.start();
        t1.join();                          // main -> WAITING until T1 ends
        System.out.println("  main continues after T1, elapsed = " + msSince(t0) + " ms");
    }

    // ----------------------------------------------------------- joinTimeout
    private static void joinTimeout() throws InterruptedException {
        System.out.println();
        System.out.println("=== joinTimeout: join(200) waits at most 200 ms ===");
        Thread t1 = new Thread(() -> nap(800), "T1");

        long t0 = System.nanoTime();
        t1.start();
        t1.join(200);                       // give up after 200 ms
        System.out.println("  join(200) returned after " + msSince(t0) + " ms");
        System.out.println("  is T1 still alive? " + t1.isAlive());

        t1.join();                          // now wait for real
        System.out.println("  after a full join, is T1 alive? " + t1.isAlive());
    }

    // --------------------------------------------------------------- isAlive
    private static void isAlive() throws InterruptedException {
        System.out.println();
        System.out.println("=== isAlive: true only between start() and death ===");
        Thread t1 = new Thread(() -> nap(600), "T1");

        System.out.println("  before start() : " + t1.isAlive());
        t1.start();
        System.out.println("  just after start() : " + t1.isAlive());
        t1.join();
        System.out.println("  after it finished  : " + t1.isAlive());
    }

    // ----------------------------------------------------------------- yield
    private static void yieldDemo() throws InterruptedException {
        System.out.println();
        System.out.println("=== yield: a hint, not a command ===");
        Thread yielder = new Thread(() -> {
            for (int i = 1; i <= 4; i++) {
                System.out.println("  yielder -> " + i);
                Thread.yield();             // "I am willing to give up my slice"
            }
        }, "yielder");

        Thread plain = new Thread(() -> {
            for (int i = 1; i <= 4; i++) System.out.println("  plain   -> " + i);
        }, "plain");

        yielder.start();
        plain.start();
        yielder.join();
        plain.join();
        System.out.println("  both finished; yield() never guarantees anything (order varies).");
    }

    // ----------------------------------------------------------------- names
    private static void names() throws InterruptedException {
        System.out.println();
        System.out.println("=== names: currentThread() and setName() ===");
        Thread t1 = new Thread(() -> System.out.println("  inside the thread, name = "
                + Thread.currentThread().getName()), "worker-1");

        System.out.println("  before start, name = " + t1.getName());
        t1.setName("renamed-worker");
        System.out.println("  after setName, name = " + t1.getName());
        t1.start();
        t1.join();
        System.out.println("  main thread name    = " + Thread.currentThread().getName());
    }
}
