import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Java Memory Management — Lecture #45 (Coder Army) — Part II
 *
 * THE HEAP, GARBAGE COLLECTION & OutOfMemoryError
 * =================================================================
 * Modes (the first argument selects one):
 *
 *   java -cp out Memory02_HeapGCAndOutOfMemory              # "safe" — the String
 *                                                           #   pool + GC demos
 *   java -cp out Memory02_HeapGCAndOutOfMemory stringPool   # String pool only
 *   java -cp out Memory02_HeapGCAndOutOfMemory gc           # GC only
 *   java -Xmx64m  -cp out Memory02_HeapGCAndOutOfMemory oom     # fill the heap (💥)
 *   java -Xmx64m  -cp out Memory02_HeapGCAndOutOfMemory naive   # naive catch (💥)
 *   java -Xmx64m  -cp out Memory02_HeapGCAndOutOfMemory catch   # catch correctly
 *
 * What each mode shows:
 *
 *   stringPool() — identical string LITERALS share one object in the String pool
 *                  (on the heap since Java 7); new String(...) always makes a
 *                  copy; intern() rejoins the pool. Use equals(), not ==.
 *
 *   gc()         — an object is reclaimed when no STRONG reference reaches it.
 *                  A WeakReference is cleared on the next GC, and the collector
 *                  proves it by ENQUEUEING the reference into a ReferenceQueue.
 *                  System.gc() is only a hint.
 *
 *   oom()        — the original Demo.java: allocate ~1 MB int[] in a loop, keep
 *                  them strongly reachable in an ArrayList, and the heap fills
 *                  until OutOfMemoryError: Java heap space kills the program.
 *
 *   naive()      — the same loop wrapped in try/catch. It STILL dies: once the
 *                  heap is full, the catch handler's own println allocates and
 *                  throws a SECOND OOM that nothing catches.
 *
 *   catchOom()   — the fix: hold a RESERVE block, drop it the instant the error
 *                  is caught, and the handler has room to report the failure.
 */
public class Memory02_HeapGCAndOutOfMemory {

    // =================================================================
    // String pool & interning
    // =================================================================

    static void stringPool() {
        System.out.println("[string pool]");
        String literal       = "java";              // pooled literal
        String literal2      = "java";              // same pooled object
        String heapCopy      = new String("java");  // brand-new heap object
        String interned      = heapCopy.intern();   // asks the pool for its copy
        String folded        = "ja" + "va";         // constant-folded at compile time
        String suffix        = "va";
        String runtimeConcat = "ja" + suffix;       // built at runtime

        System.out.println("  literal == literal2        : " + (literal == literal2));
        System.out.println("  heapCopy == literal        : " + (heapCopy == literal));
        System.out.println("  interned == literal        : " + (interned == literal));
        System.out.println("  folded == literal          : " + (folded == literal));
        System.out.println("  runtimeConcat == literal   : " + (runtimeConcat == literal));
        System.out.println("  heapCopy.equals(literal)   : " + heapCopy.equals(literal)
                + "   // use equals(), never ==");
        System.out.println("  literal identity hash      : " + System.identityHashCode(literal));
        System.out.println("  literal2 identity hash     : " + System.identityHashCode(literal2)
                + "   // same number => same object");
        System.out.println("  heapCopy identity hash     : " + System.identityHashCode(heapCopy));
    }

    // =================================================================
    // Garbage collection & reference strengths
    // =================================================================

    static class Blob {
        final String name;
        final byte[] payload = new byte[1_000_000];   // 1 MB, just to be heavy
        Blob(String name) { this.name = name; }
        @Override public String toString() { return "Blob(" + name + ")"; }
    }

    static void gc() throws InterruptedException {
        System.out.println();
        System.out.println("[gc]");
        Blob strong = new Blob("strong");   // strongly reachable from this frame

        // The Blob("weak-inline") object has NO strong reference: only a weak
        // reference, plus the ReferenceQueue the collector will enqueue it into.
        ReferenceQueue<Blob> queue = new ReferenceQueue<>();
        WeakReference<Blob> weak = new WeakReference<>(new Blob("weak-inline"), queue);

        System.out.println("  strong object                : " + strong);
        System.out.println("  weak object before any GC    : " + weak.get());
        System.out.println("  reference already enqueued?  : " + (queue.poll() != null));

        System.gc();                 // a HINT, not a command
        Thread.sleep(50);
        System.gc();
        Thread.sleep(50);

        System.out.println("  weak object after System.gc()  : " + weak.get()
                + "   // null => reclaimed");
        System.out.println("  reference enqueued by the GC?  : " + (queue.poll() != null)
                + "   // proof the collector cleared it");
        System.out.println("  the strong object is untouched : " + strong);

        strong = null;               // now even the strong object is eligible
        System.gc();
        System.out.println("  strong reference dropped; the 1 MB Blob may now be reclaimed");
    }

    // =================================================================
    // OutOfMemoryError
    // =================================================================

    /** Fills the heap and DIES — this is the original Demo.java. */
    static void oom() {
        List<int[]> list = new ArrayList<>();
        int count = 0;
        while (true) {
            // int -> 4 bytes -> ~1 MB per array, kept alive by `list`
            list.add(new int[250_000]);
            count++;
            System.out.println("Allocated Block :" + count);
        }
    }

    /** 💥 Wrapping it in try/catch does NOT save you — the handler itself OOMs. */
    static void naive() {
        List<int[]> list = new ArrayList<>();
        int count = 0;
        try {
            while (true) {
                list.add(new int[250_000]);
                count++;
            }
        } catch (OutOfMemoryError e) {
            // These println() calls must allocate: the heap is full, so this
            // throws a SECOND OutOfMemoryError that no catch clause covers.
            System.out.println("--- heap exhausted ---");
            System.out.println("blocks that fit: " + count);
            System.out.println("caught: " + e);
        }
    }

    /** ✅ The fix: a RESERVE block dropped the moment the error is caught. */
    static volatile byte[] reserve = new byte[8 * 1024 * 1024];   // 8 MB safety net

    static void catchOom() {
        List<int[]> list = new ArrayList<>();
        int count = 0;
        try {
            while (true) {
                list.add(new int[250_000]);
                count++;
            }
        } catch (OutOfMemoryError e) {
            reserve = null;   // free the safety net FIRST, before allocating anything
            System.out.println("caught OutOfMemoryError after " + count + " blocks");
            System.out.println("the handler had room because we dropped the reserve");
            System.out.println("error: " + e);
        }
        System.out.println("main() survived to the end");
    }

    // =================================================================
    // Runner
    // =================================================================

    public static void main(String[] args) throws InterruptedException {
        String mode = (args.length == 0) ? "safe" : args[0];
        switch (mode) {
            case "safe"       -> { stringPool(); gc(); }
            case "stringPool" -> stringPool();
            case "gc"         -> gc();
            case "oom"        -> oom();
            case "naive"      -> naive();
            case "catch"      -> catchOom();
            default           -> System.out.println(
                    "usage: safe | stringPool | gc | oom | naive | catch");
        }
    }
}
