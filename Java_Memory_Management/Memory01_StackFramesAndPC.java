/**
 * Java Memory Management — Lecture #45 (Coder Army) — Demo 01
 *
 * Stack frames & the PC (program counter) register
 * -----------------------------------------------------------------
 * Every method call pushes a NEW FRAME onto the current thread's Java Stack.
 * A frame stores:
 *   - the local variable array  (primitives + object references)
 *   - the operand stack         (scratch space for bytecode instructions)
 *   - a reference to the runtime constant pool of its class
 *   - the return address        (where execution continues after the return)
 *
 * The PROGRAM COUNTER (PC) register is per-thread and points at the bytecode
 * instruction currently being executed inside the top frame. You can see the
 * exact instruction sequence the PC walks through with:
 *
 *     javap -c Memory01_StackFramesAndPC
 *
 * What this demo shows:
 *   1. a normal call chain is literally a stack of frames
 *   2. a reference variable lives ON THE STACK (in main's frame) while the
 *      object it points to lives ON THE HEAP
 *   3. each thread has its OWN stack, so threads never share frames
 */
public class Memory01_StackFramesAndPC {

    // main -> level1 -> level2 -> level3 ; each arrow is one pushed frame
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

    public static void main(String[] args) throws InterruptedException {
        // 1) The call chain is visible as the stack of frames.
        level1();

        // 2) The reference `sb` occupies a slot in main's frame;
        //    the StringBuilder object itself is on the heap.
        StringBuilder sb = new StringBuilder("heap object");
        System.out.println();
        System.out.println("[main] sb (reference in main's frame) -> identity hash "
                + System.identityHashCode(sb));
        acceptReference(sb);

        // 3) Each thread gets its own stack -> its own frames.
        Thread worker = new Thread(() -> {
            System.out.println();
            System.out.println("[worker thread] its own independent stack:");
            for (StackTraceElement f : Thread.currentThread().getStackTrace()) {
                System.out.println("        " + f.getClassName() + "." + f.getMethodName());
            }
        }, "worker");
        worker.start();
        worker.join();

        System.out.println();
        System.out.println("[main] done");
    }
}
