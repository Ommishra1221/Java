import java.io.File;

/**
 * Lecture #47 - Introduction to Multithreading in Java | Process vs Thread
 *
 * Modes (pass as args[0]):
 *   basics           - identity of the "main" thread + machine info
 *   sharedMemory     - threads of ONE process share the same heap/method area
 *   separateProcess  - a child JVM is a SEPARATE process: its writes are invisible here
 *   all              - runs all three (default)
 *
 * Every mode is a separate JVM run:
 *     javac -Xlint:all -d out *.java
 *     java -cp out Multithreading01_ProcessVsThread all
 */
public class Multithreading01_ProcessVsThread {

    // Lives in the method area of THIS process, shared by every thread of this process.
    static int sharedCounter = 0;

    // Sentinel so the parent can re-launch this same class as a *child process*.
    private static final String CHILD_MARKER = "__child";

    public static void main(String[] args) throws Exception {
        // Re-entrant entry point: we are the child JVM that main() spawned.
        if (args.length > 0 && args[0].equals(CHILD_MARKER)) {
            runAsChild();
            return;
        }

        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "basics":          basics();          break;
            case "sharedMemory":    sharedMemory();    break;
            case "separateProcess": separateProcess(); break;
            case "all":             basics(); sharedMemory(); separateProcess(); break;
            default:                usage();
        }
    }

    private static void usage() {
        System.out.println("usage: java -cp out Multithreading01_ProcessVsThread [basics|sharedMemory|separateProcess|all]");
    }

    // ---------------------------------------------------------------- basics
    private static void basics() {
        System.out.println("=== basics: the main thread and the machine ===");
        Thread main = Thread.currentThread();

        System.out.println("Process (this JVM) pid   : " + ProcessHandle.current().pid());
        System.out.println("Main thread name         : " + main.getName());
        // NOTE: Thread.getId() is deprecated since Java 19 - use threadId() instead.
        System.out.println("Main thread id           : " + main.threadId());
        System.out.println("Main thread priority     : " + main.getPriority());
        System.out.println("Main thread group        : " + main.getThreadGroup().getName());
        System.out.println("Main thread daemon       : " + main.isDaemon());
        System.out.println("Main thread alive        : " + main.isAlive());
        System.out.println("Available processors     : " + Runtime.getRuntime().availableProcessors());
        System.out.println("Live threads in this JVM : " + Thread.activeCount());
    }

    // -------------------------------------------------------- sharedMemory
    private static void sharedMemory() {
        System.out.println();
        System.out.println("=== sharedMemory: threads of ONE process share memory ===");

        final int workers = 3;
        final int perWorker = 5;
        Thread[] threads = new Thread[workers];

        for (int i = 0; i < workers; i++) {
            threads[i] = new Thread(() -> {
                for (int k = 0; k < perWorker; k++) {
                    synchronized (Multithreading01_ProcessVsThread.class) {
                        sharedCounter++;              // same static field for every thread
                    }
                }
            }, "worker-" + (i + 1));
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        System.out.println(workers + " threads x " + perWorker + " increments = " + sharedCounter
                + "   (one shared counter, no copies)");
    }

    // ----------------------------------------------------- separateProcess
    private static void separateProcess() {
        System.out.println();
        System.out.println("=== separateProcess: a child JVM gets its OWN memory ===");

        long parentPid = ProcessHandle.current().pid();
        sharedCounter = 0; // reset the parent's copy

        String javaBin = System.getProperty("java.home")
                + File.separator + "bin" + File.separator + "java";

        ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-cp", System.getProperty("java.class.path"),
                "Multithreading01_ProcessVsThread",
                CHILD_MARKER);
        pb.redirectErrorStream(true);

        System.out.println("Parent process pid    : " + parentPid);
        System.out.println("Parent sharedCounter  : " + sharedCounter);

        try {
            Process child = pb.start();
            String childOutput = new String(child.getInputStream().readAllBytes()).trim();
            int exit = child.waitFor();

            System.out.println("Child process said    : " + childOutput);
            System.out.println("Child exit code       : " + exit);
            System.out.println("Parent sharedCounter  : " + sharedCounter
                    + "   (child wrote its own field - the parent never sees it)");
        } catch (Exception e) {
            System.out.println("Could not launch a child JVM here: " + e);
        }
    }

    // ----------------------------------------------------------- the child
    private static void runAsChild() {
        sharedCounter = 0;
        for (int i = 0; i < 1000; i++) sharedCounter++;
        System.out.println("pid " + ProcessHandle.current().pid()
                + " counted its OWN sharedCounter up to " + sharedCounter);
    }
}
