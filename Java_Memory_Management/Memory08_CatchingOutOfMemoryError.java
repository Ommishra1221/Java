import java.util.ArrayList;
import java.util.List;

/**
 * Java Memory Management — Lecture #45 (Coder Army) — Demo 08
 *
 * Can you catch an OutOfMemoryError?
 * -----------------------------------------------------------------
 * You might expect this to work:
 *
 *     try { ...fill the heap... }
 *     catch (OutOfMemoryError e) { System.out.println("caught"); }
 *
 * It usually FAILS. Once the heap is full, the catch block must itself allocate
 * memory: building the message string, creating the println arguments, even the
 * exception's own stack trace all need heap. The handler throws a SECOND OOM
 * that is no longer covered by any catch, and the program dies. (Demo 05 shows
 * that uncaught OOM; try wrapping its loop in a try/catch and watch it escape.)
 *
 * The standard workaround is a RESERVE: hold a block of memory you can drop the
 * instant you catch the error, so the handler has room to report it.
 *
 * Run with:  java -Xmx64m Memory08_CatchingOutOfMemoryError
 */
public class Memory08_CatchingOutOfMemoryError {

    // The safety net: 8 MB pre-allocated, and dropped in the catch block.
    // volatile so the JIT cannot optimise the field away.
    static volatile byte[] reserve = new byte[8 * 1024 * 1024];

    public static void main(String[] args) {
        List<int[]> list = new ArrayList<>();
        int count = 0;

        try {
            while (true) {
                list.add(new int[250_000]);   // ~1 MB per array
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
}
