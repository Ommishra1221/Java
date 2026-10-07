/**
 * Lecture #49 - Thread Methods | sleep, join, yield, interrupt, isAlive, priority & more
 *              (part 2: interrupt, priority, daemon)
 *
 * Modes (pass as args[0]):
 *   interrupt         - cooperative cancellation with the interrupt flag
 *   interruptSleep    - interrupting a sleeping thread throws InterruptedException
 *   flagClearing      - isInterrupted() vs the flag-clearing Thread.interrupted()
 *   priority          - priority constants and get/setPriority
 *   priorityRange     - a priority outside 1..10 throws IllegalArgumentException
 *   daemon            - a daemon thread dies with the main thread
 *   daemonAfterStart  - setDaemon() after start() throws IllegalThreadStateException
 *   all               - runs every mode (default)
 */
public class Multithreading02_InterruptPriorityDaemon {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "interrupt":        interrupt();        break;
            case "interruptSleep":   interruptSleep();   break;
            case "flagClearing":     flagClearing();     break;
            case "priority":         priority();         break;
            case "priorityRange":    priorityRange();    break;
            case "daemon":           daemon();           break;
            case "daemonAfterStart": daemonAfterStart(); break;
            case "all":
                interrupt(); interruptSleep(); flagClearing(); priority(); priorityRange();
                daemon(); daemonAfterStart();
                break;
            default:
                System.out.println("usage: java -cp out Multithreading02_InterruptPriorityDaemon "
                        + "[interrupt|interruptSleep|flagClearing|priority|priorityRange|daemon|daemonAfterStart|all]");
        }
    }

    private static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static long msSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    // ------------------------------------------------------------- interrupt
    private static void interrupt() throws InterruptedException {
        System.out.println("=== interrupt: cooperative cancellation (a request, not a kill) ===");

        Thread worker = new Thread(() -> {
            long spins = 0;
            while (!Thread.currentThread().isInterrupted()) {   // keep checking the flag
                spins++;
            }
            System.out.println("  worker stopped after " + spins + " spins");
        }, "spinner");

        long t0 = System.nanoTime();
        worker.start();
        nap(300);
        worker.interrupt();                 // set the flag; the loop notices and exits
        worker.join();
        System.out.println("  main interrupted the worker after " + msSince(t0) + " ms");
    }

    // -------------------------------------------------------- interruptSleep
    private static void interruptSleep() throws InterruptedException {
        System.out.println();
        System.out.println("=== interruptSleep: interrupting a sleeping thread ===");

        Thread sleeper = new Thread(() -> {
            try {
                Thread.sleep(10_000);       // would sleep 10 s
                System.out.println("  sleeper: slept the full 10 s");
            } catch (InterruptedException e) {
                System.out.println("  sleeper: caught " + e);
                System.out.println("  sleeper: flag is now cleared -> isInterrupted() = "
                        + Thread.currentThread().isInterrupted());
            }
        }, "sleeper");

        long t0 = System.nanoTime();
        sleeper.start();
        nap(300);
        sleeper.interrupt();               // -> InterruptedException inside sleep()
        sleeper.join();
        System.out.println("  sleeper finished after " + msSince(t0) + " ms (not 10 s)");
    }

    // ---------------------------------------------------------- flagClearing
    private static void flagClearing() throws InterruptedException {
        System.out.println();
        System.out.println("=== flagClearing: isInterrupted() vs Thread.interrupted() ===");

        Thread t = new Thread(() -> {
            System.out.println("  flag at the start           : " + Thread.currentThread().isInterrupted());
            Thread.currentThread().interrupt();
            System.out.println("  after this.interrupt()      : " + Thread.currentThread().isInterrupted());
            System.out.println("  Thread.interrupted() returns: " + Thread.interrupted());   // true AND clears
            System.out.println("  flag right after that call  : " + Thread.currentThread().isInterrupted());
        }, "flag-thread");

        t.start();
        t.join();
    }

    // -------------------------------------------------------------- priority
    private static void priority() throws InterruptedException {
        System.out.println();
        System.out.println("=== priority: a hint to the OS scheduler ===");
        System.out.println("  MIN_PRIORITY  = " + Thread.MIN_PRIORITY);
        System.out.println("  NORM_PRIORITY = " + Thread.NORM_PRIORITY);
        System.out.println("  MAX_PRIORITY  = " + Thread.MAX_PRIORITY);

        Thread t1 = new Thread(() -> { }, "t1");
        System.out.println("  a new thread starts at priority: " + t1.getPriority());

        t1.setPriority(Thread.MAX_PRIORITY);
        System.out.println("  after setPriority(MAX)         : " + t1.getPriority());
        System.out.println("  main thread priority           : " + Thread.currentThread().getPriority());
        System.out.println("  (whether the OS actually honours it is platform dependent)");
    }

    // --------------------------------------------------------- priorityRange
    private static void priorityRange() {
        System.out.println();
        System.out.println("=== priorityRange: values outside 1..10 are rejected ===");
        Thread t = new Thread(() -> { }, "t");
        try {
            t.setPriority(11);
        } catch (IllegalArgumentException e) {
            System.out.println("  setPriority(11) threw: " + e);
        }
        try {
            t.setPriority(0);
        } catch (IllegalArgumentException e) {
            System.out.println("  setPriority(0)  threw: " + e);
        }
    }

    // ---------------------------------------------------------------- daemon
    private static void daemon() throws InterruptedException {
        System.out.println();
        System.out.println("=== daemon: a background thread the JVM does NOT wait for ===");

        Thread daemon = new Thread(() -> {
            int beat = 0;
            while (true) {
                System.out.println("  daemon heartbeat " + (++beat));
                nap(200);
            }
        }, "daemon");

        System.out.println("  before setDaemon, isDaemon() = " + daemon.isDaemon());
        daemon.setDaemon(true);
        System.out.println("  after  setDaemon, isDaemon() = " + daemon.isDaemon());

        daemon.start();
        nap(700);
        System.out.println("  main is about to return - the JVM exits and kills the daemon");
    }

    // ------------------------------------------------------- daemonAfterStart
    private static void daemonAfterStart() throws InterruptedException {
        System.out.println();
        System.out.println("=== daemonAfterStart: setDaemon() must happen BEFORE start() ===");

        Thread t = new Thread(() -> nap(50), "late-daemon");
        t.start();
        try {
            t.setDaemon(true);
        } catch (IllegalThreadStateException e) {
            System.out.println("  setDaemon(true) after start() threw: " + e);
        }
        t.join();
    }
}
