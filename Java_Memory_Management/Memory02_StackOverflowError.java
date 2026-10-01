/**
 * Java Memory Management — Lecture #45 (Coder Army) — Demo 02
 *
 * StackOverflowError — the Java Stack running out of room
 * -----------------------------------------------------------------
 * Every call to recurse() pushes another frame. Because recurse() never
 * returns, frames are never popped, so the stack keeps growing until it hits
 * its limit and the JVM throws StackOverflowError.
 *
 * StackOverflowError is an Error, NOT an Exception. The JVM unwinds the stack
 * as it propagates, which is why main() can catch it and keep running.
 *
 * The stack size is per thread and is controlled with -Xss:
 *     java -Xss256k Memory02_StackOverflowError   -> fewer frames
 *     java -Xss4m   Memory02_StackOverflowError   -> many more frames
 *
 * IMPORTANT: this is a STACK problem. Raising the heap (-Xmx) will NOT help;
 * only -Xss (or removing the unbounded recursion) will.
 *
 * 💥 INTENTIONAL: this program intentionally overflows the stack.
 */
public class Memory02_StackOverflowError {

    static int depth = 0;

    static void recurse() {
        depth++;      // one more frame on the stack
        recurse();    // never returns
    }

    public static void main(String[] args) {
        try {
            recurse();
        } catch (StackOverflowError e) {
            System.out.println("StackOverflowError at depth = " + depth);
        }
        System.out.println("main() survived; the depth counter is still " + depth);
    }
}
