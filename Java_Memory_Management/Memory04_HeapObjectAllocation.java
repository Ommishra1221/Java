/**
 * Java Memory Management — Lecture #45 (Coder Army) — Demo 04
 *
 * The Heap — where objects and arrays live
 * -----------------------------------------------------------------
 * Every object and every array in Java is allocated on the HEAP with `new`.
 * A variable of a reference type only stores a REFERENCE (a pointer): the
 * variable's slot lives in a stack frame, the object itself lives on the heap.
 *
 * What this demo shows:
 *   - two `new` objects are always two distinct objects (== is false)
 *   - a primitive array int[] is an OBJECT too, so it lives on the heap
 *   - the heap really did grow when we allocated a 10 MB array
 *
 * NOTE: the byte counts printed by Runtime vary from machine to machine,
 * because the max heap size depends on the RAM of the host (see -Xmx).
 */
public class Memory04_HeapObjectAllocation {

    static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }

    public static void main(String[] args) {
        Point p1 = new Point(1, 2);
        Point p2 = new Point(1, 2);
        System.out.println("p1 == p2        : " + (p1 == p2));
        System.out.println("p1.equals(p2)   : " + p1.equals(p2)
                + "   // default Object.equals is identity");

        // An array is an object: it is created on the heap with `new`.
        int[] numbers = new int[250_000];
        System.out.println("numbers.getClass(): " + numbers.getClass().getName());
        System.out.println("numbers.length    : " + numbers.length);

        Runtime rt = Runtime.getRuntime();
        System.out.printf("heap max : %,d bytes (%.1f MB)%n",
                rt.maxMemory(), rt.maxMemory() / 1_048_576.0);

        long usedBefore = rt.totalMemory() - rt.freeMemory();
        byte[] big = new byte[10 * 1024 * 1024];   // 10 MB on the heap
        long usedAfter = rt.totalMemory() - rt.freeMemory();

        System.out.printf("heap used before allocating 10 MB : %,d bytes%n", usedBefore);
        System.out.printf("heap used after  allocating 10 MB : %,d bytes%n", usedAfter);
        System.out.println("big.length: " + big.length + " (kept alive so it is not collected)");
    }
}
