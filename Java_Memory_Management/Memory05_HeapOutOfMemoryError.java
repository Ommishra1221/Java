import java.util.ArrayList;
import java.util.List;

/**
 * Java Memory Management — Lecture #45 (Coder Army) — Demo 05
 *
 * OutOfMemoryError: Java heap space   (this is the original Demo.java)
 * -----------------------------------------------------------------
 * Every iteration allocates a 250_000-element int[]:
 *
 *     250_000 ints * 4 bytes = 1_000_000 bytes ~= 1 MB
 *
 * and stores it in an ArrayList. Because the list is reachable from main(),
 * every array stays strongly reachable and can NEVER be garbage collected.
 * The heap keeps growing until it cannot grow any more, and the JVM throws
 * java.lang.OutOfMemoryError: Java heap space — which kills the program.
 *
 * Run it with a small heap so the wall is reached quickly and safely:
 *
 *     java -Xmx64m Memory05_HeapOutOfMemoryError
 *
 * -Xmx sets the MAXIMUM heap size. Without the flag the JVM may use gigabytes
 * of RAM, so always cap it when deliberately exhausting the heap.
 *
 * 💥 INTENTIONAL: this program deliberately exhausts the heap.
 *
 * Demo 08 shows why catching this error in a naive try/catch does not work.
 */
public class Memory05_HeapOutOfMemoryError {

    public static void main(String[] args) {
        List<int[]> list = new ArrayList<>();
        int count = 0;

        while (true) {
            // int -> 4 bytes -> ~1 MB per array, kept alive by `list`
            list.add(new int[250_000]);
            count++;
            System.out.println("Allocated Block :" + count);
        }
    }
}
