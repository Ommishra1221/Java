import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Java Memory Management — Lecture #45 (Coder Army) — Demo 07
 *
 * Garbage collection & reference strengths
 * -----------------------------------------------------------------
 * An object becomes ELIGIBLE FOR COLLECTION when no strong reference points to
 * it. The garbage collector reclaims that memory automatically -- Java has no
 * free()/delete.
 *
 * Reference strengths, from strongest to weakest:
 *   strong  (Blob b)            ordinary reference: keeps the object alive
 *   soft    (SoftReference)     cleared only when memory is low (good for caches)
 *   weak    (WeakReference)     cleared on the next GC even if memory is plentiful
 *   phantom (PhantomReference)  lets you observe that the object is about to go
 *
 * System.gc() is only a HINT: the JVM may ignore it. Collection timing is the
 * JVM's business, so never make program correctness depend on a GC running
 * "now". To prove a weak reference was cleared we watch a ReferenceQueue,
 * which the collector enqueues the reference into once the object is gone.
 */
public class Memory07_GarbageCollectionAndReferences {

    static class Blob {
        final String name;
        final byte[] payload = new byte[1_000_000];   // 1 MB, just to be heavy
        Blob(String name) { this.name = name; }
        @Override public String toString() { return "Blob(" + name + ")"; }
    }

    public static void main(String[] args) throws InterruptedException {
        Blob strong = new Blob("strong");   // strongly reachable from main's frame

        // The Blob("weak-inline") object has NO strong reference at all: the only
        // reference to it is this weak reference, plus the queue.
        ReferenceQueue<Blob> queue = new ReferenceQueue<>();
        WeakReference<Blob> weak = new WeakReference<>(new Blob("weak-inline"), queue);

        System.out.println("strong object                : " + strong);
        System.out.println("weak object before any GC    : " + weak.get());
        System.out.println("reference already enqueued?  : " + (queue.poll() != null));

        System.gc();                        // hint: run the collector
        Thread.sleep(50);
        System.gc();
        Thread.sleep(50);

        System.out.println();
        System.out.println("weak object after System.gc()  : " + weak.get()
                + "   // null => it was reclaimed");
        System.out.println("reference enqueued by the GC?  : " + (queue.poll() != null)
                + "   // proof the collector cleared it");
        System.out.println("the strong object is untouched : " + strong);

        strong = null;   // now even the strong object is eligible for collection
        System.gc();
        System.out.println();
        System.out.println("after dropping the strong reference, the JVM may reclaim that 1 MB");
    }
}
