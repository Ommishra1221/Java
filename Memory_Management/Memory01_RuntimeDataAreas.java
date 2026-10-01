/**
 * Java Memory Management — Lecture #45 (Coder Army) — Part I
 *
 * The JVM's RUNTIME DATA AREAS
 * =================================================================
 * Run it:   java -cp out Memory01_RuntimeDataAreas
 *
 * This single program demonstrates, in order:
 *
 *   1. stackFramesAndPc()   — the Java Stack (one FRAME per method call), the
 *                             PC register (the bytecode offsets jalap -c shows),
 *                             and that each THREAD has its own stack.
 *                             A reference lives on the stack; the object it
 *                             points at lives on the heap.
 *
 *   2. stackOverflow()      — the stack running out of room -> StackOverflowError.
 *                             Runnable with different stack sizes:
 *                                 java -Xss256k -cp out Memory01_RuntimeDataAreas
 *                                 java -Xss4m   -cp out Memory01_RuntimeDataAreas
 *                             -Xmx does NOT change this: it is the HEAP flag, not
 *                             the stack flag.
 *
 *   3. methodAreaAndStatics() — the METHOD AREA / METASPACE: class metadata,
 *                             the Class object, and STATIC variables (one copy
 *                             per class, shared by every instance).
 *
 *   4. heapObjects()        — the HEAP: every object and every array is created
 *                             there with new; a local variable holds only a
 *                             REFERENCE (on the stack) to it.
 *
 * The companion program Memory02_HeapGCAndOutOfMemory.java covers Part II:
 * the String pool, garbage collection, and OutOfMemoryError.
 */
public class Memory01_RuntimeDataAreas {

    // =================================================================
    // 1. Stack frames, the PC register, and per-thread stacks
    // =================================================================

    /** main -> level1 -> level2 -> level3 ; each arrow is one pushed frame. */
    static void level3() {
        System.out.println("[level3] frames on this thread, innermost first:");
        for (StackTraceElement f : Thread.currentThread().getStackTrace()) {
            System.out.println("        " + f.getClassName() + "." + f.getMethodName()
                    + " (line " + f.getLineNumber() + ")");
        }
    }

    static void level2() { level3(); }
    static void level1() { level2(); }

    static void acceptReference(StringBuilder ref) {
        System.out.println("[acceptReference] same object (identity hash): "
                + System.identityHashCode(ref));
    }

    static void stackFramesAndPc() throws InterruptedException {
        // 1a) The call chain IS the stack of frames.
        level1();

        // 1b) `sb` is a reference stored in THIS method's frame (the stack);
        //     the StringBuilder object itself is on the heap. Passing it to
        //     another method copies the reference, not the object.
        StringBuilder sb = new StringBuilder("heap object");
        System.out.println();
        System.out.println("[frames] sb (a reference in this frame) -> identity hash "
                + System.identityHashCode(sb));
        acceptReference(sb);

        // 1c) Each thread has its own stack -> its own frames.
        Thread worker = new Thread(() -> {
            System.out.println();
            System.out.println("[worker thread] its own independent stack:");
            for (StackTraceElement f : Thread.currentThread().getStackTrace()) {
                System.out.println("        " + f.getClassName() + "." + f.getMethodName());
            }
        }, "worker");
        worker.start();
        worker.join();
    }

    // =================================================================
    // 2. StackOverflowError — the stack running out of room
    // =================================================================

    static int depth = 0;

    /** Pushes a frame and never returns, so frames are never popped. */
    static void recurse() {
        depth++;
        recurse();
    }

    static void stackOverflow() {
        System.out.println();
        try {
            recurse();
        } catch (StackOverflowError e) {
            // Error, not Exception — but the JVM unwinds the stack, so main
            // can catch it and keep running.
            System.out.println("[stack] StackOverflowError at depth = " + depth);
        }
        System.out.println("[stack] survived; the depth counter is still " + depth);
        System.out.println("[stack] (change the stack with -Xss, not the heap with -Xmx)");
    }

    // =================================================================
    // 3. Method Area / Metaspace — class metadata and static variables
    // =================================================================

    static class Counter {
        // STATIC field: ONE copy per class, stored in the Method Area.
        static int total;

        static {
            System.out.println("[method area] static initializer: runs ONCE, on first use");
        }

        // INSTANCE field: one copy per object, stored on the heap.
        final int id;

        Counter() {
            id = ++total;   // bumps the shared (method-area) counter
        }
    }

    static void methodAreaAndStatics() {
        System.out.println();
        Counter a = new Counter();
        Counter b = new Counter();
        Counter c = new Counter();

        System.out.println("[method area] a.id=" + a.id + "  b.id=" + b.id + "  c.id=" + c.id);
        System.out.println("[method area] Counter.total (shared) = " + Counter.total);
        System.out.println("[method area] a.getClass() == b.getClass() : "
                + (a.getClass() == b.getClass()));
        System.out.println("[method area] the Class object's name : " + a.getClass().getName());
    }

    // =================================================================
    // 4. The Heap — objects and arrays
    // =================================================================

    static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }

    static void heapObjects() {
        System.out.println();
        Point p1 = new Point(1, 2);
        Point p2 = new Point(1, 2);
        System.out.println("[heap] p1 == p2      : " + (p1 == p2) + "   // two distinct heap objects");
        System.out.println("[heap] p1.equals(p2) : " + p1.equals(p2)
                + "   // default Object.equals is identity");

        // An array is an object: `newarray` allocates it on the heap.
        int[] numbers = new int[250_000];
        System.out.println("[heap] numbers.getClass(): " + numbers.getClass().getName()
                + "   // the JVM name for int[]");
        System.out.println("[heap] numbers.length    : " + numbers.length);

        Runtime rt = Runtime.getRuntime();
        System.out.printf("[heap] heap max : %,d bytes (%.1f MB)%n",
                rt.maxMemory(), rt.maxMemory() / 1_048_576.0);

        long usedBefore = rt.totalMemory() - rt.freeMemory();
        byte[] big = new byte[10 * 1024 * 1024];   // 10 MB on the heap
        long usedAfter = rt.totalMemory() - rt.freeMemory();
        System.out.printf("[heap] used before allocating 10 MB : %,d bytes%n", usedBefore);
        System.out.printf("[heap] used after  allocating 10 MB : %,d bytes%n", usedAfter);
        System.out.println("[heap] big.length: " + big.length + " (kept alive so it is not collected)");
    }

    // =================================================================
    // Runner
    // =================================================================

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Part I - JVM runtime data areas ===\n");
        stackFramesAndPc();
        stackOverflow();
        methodAreaAndStatics();
        heapObjects();
    }
}
