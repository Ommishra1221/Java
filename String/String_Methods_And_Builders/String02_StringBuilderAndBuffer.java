/**
 * Java Strings - Lecture #25 Part 2 (Coder Army) - file 2 of 2
 *
 * StringBuilder & StringBuffer - MUTABLE, GROWABLE TEXT
 * ====================================================
 * String is immutable, so heavy text building copies the whole string every
 * step. StringBuilder is the mutable alternative: append/insert/delete/
 * replace/reverse edit the SAME buffer in place, with an internal char array
 * that grows geometrically (default capacity 16; grows to 2*cap + 2).
 *
 *   StringBuilder - fast, NOT synchronized  -> use for single-threaded work
 *   StringBuffer  - the same API, methods are synchronized -> thread-safe,
 *                   slower -> use only when several threads share the buffer
 *
 * Run it (the first argument selects the demo):
 *
 *   java -cp out String02_StringBuilderAndBuffer            # all
 *   java -cp out String02_StringBuilderAndBuffer ops        # mutation methods
 *   java -cp out String02_StringBuilderAndBuffer capacity   # growth & tuning
 *   java -cp out String02_StringBuilderAndBuffer buffer     # StringBuffer / threads
 */
public class String02_StringBuilderAndBuffer {

    // =================================================================
    // 1. Mutation methods  (was Demo3.java)
    // =================================================================

    static void operations() {
        System.out.println("[ops] start    : " + new StringBuilder("Aditya"));
        System.out.println("[ops] append   : " + new StringBuilder("Aditya").append(" Tandon"));
        System.out.println("[ops] insert   : " + new StringBuilder("Aditya").insert(2, 'o'));
        System.out.println("[ops] delete   : " + new StringBuilder("Aditya").delete(0, 2));
        System.out.println("[ops] deleteCharAt : " + new StringBuilder("Aditya").deleteCharAt(1));
        System.out.println("[ops] replace  : " + new StringBuilder("Aditya").replace(1, 3, "XY"));
        System.out.println("[ops] reverse  : " + new StringBuilder("Aditya").reverse());

        StringBuilder edit = new StringBuilder("Aditya");
        edit.setCharAt(3, 'r');
        System.out.println("[ops] setCharAt(3, 'r') : " + edit);
        System.out.println("[ops] charAt(1)         : " + edit.charAt(1));

        // The whole point: these mutate in place and return the SAME object.
        StringBuilder sb = new StringBuilder("Aditya");
        System.out.println("[ops] append returns the same object : "
                + (sb.append("!") == sb) + "   -> \"!\" was added in place");
    }

    // =================================================================
    // 2. Capacity: default 16, grows as 2*cap + 2, trimmable
    // =================================================================

    static void capacity() {
        StringBuilder grow = new StringBuilder();
        System.out.println("[cap] new StringBuilder()             length=" + grow.length()
                + " capacity=" + grow.capacity());

        grow.append("Aditya").append("Tandon").append("aaaaa");   // 6 + 6 + 5 = 17 chars
        System.out.println("[cap] after appending 17 chars        length=" + grow.length()
                + " capacity=" + grow.capacity() + "   (16 -> 34 = 2*16 + 2)");

        grow.ensureCapacity(100);
        System.out.println("[cap] after ensureCapacity(100)       capacity=" + grow.capacity());

        grow.trimToSize();
        System.out.println("[cap] after trimToSize()              length=" + grow.length()
                + " capacity=" + grow.capacity());

        // Pre-size when you know the size, to avoid re-allocations.
        StringBuilder sized = new StringBuilder(64);
        System.out.println("[cap] new StringBuilder(64) capacity=" + sized.capacity());
    }

    // =================================================================
    // 3. StringBuffer: the synchronized twin
    // =================================================================

    static void bufferTest() throws InterruptedException {
        StringBuffer buffer = new StringBuffer();     // synchronized methods
        StringBuilder builder = new StringBuilder();  // not synchronized
        int threads = 4, perThread = 20_000;

        Thread[] tb = new Thread[threads];
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            tb[i] = new Thread(() -> { for (int j = 0; j < perThread; j++) buffer.append("x"); });
            ts[i] = new Thread(() -> { for (int j = 0; j < perThread; j++) builder.append("x"); });
        }
        for (Thread t : tb) t.start();
        for (Thread t : tb) t.join();
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();

        System.out.println("[buffer] StringBuffer  4 x " + perThread + " appends -> length = "
                + buffer.length() + "   (expected " + (threads * perThread) + ")");
        System.out.println("[buffer] StringBuilder 4 x " + perThread + " appends -> length = "
                + builder.length() + "   (expected " + (threads * perThread)
                + "; may be LOWER because append is not synchronized)");
    }

    // =================================================================
    // Runner
    // =================================================================

    public static void main(String[] args) throws InterruptedException {
        String mode = (args.length == 0) ? "all" : args[0];
        System.out.println("=== String part II.2 - builders ===\n");
        switch (mode) {
            case "all"      -> { operations(); capacity(); bufferTest(); }
            case "ops"      -> operations();
            case "capacity" -> capacity();
            case "buffer"   -> bufferTest();
            default         -> System.out.println(
                    "usage: all | ops | capacity | buffer");
        }
    }
}
