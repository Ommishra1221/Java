/**
 * Java Memory Management — Lecture #45 (Coder Army) — Demo 03
 *
 * Method Area (Metaspace) & static variables
 * -----------------------------------------------------------------
 * The METHOD AREA holds, per loaded class:
 *   - the class's bytecode and metadata (methods, fields, constant pool)
 *   - the java.lang.Class object
 *   - all STATIC variables (they belong to the class, not to an instance)
 *
 * Since Java 8 this metadata lives in native memory called METASPACE (before
 * Java 8 it was the "PermGen" part of the heap). It is sized with
 * -XX:MaxMetaspaceSize, not -Xmx.
 *
 * What this demo shows:
 *   - a static field is shared by every instance of the class
 *   - a static initializer runs exactly once, the first time the class is used
 *   - all instances share one Class object
 */
public class Memory03_MethodAreaStaticFields {

    static class Counter {
        // STATIC field: one copy per class, stored in the method area.
        static int total;

        static {
            System.out.println("[static initializer] Counter was initialized (runs once)");
        }

        // INSTANCE field: one copy per object, stored on the heap.
        final int id;

        Counter() {
            id = ++total;   // bumps the shared (method-area) counter
        }
    }

    public static void main(String[] args) {
        System.out.println("About to touch Counter for the very first time ...");
        Counter a = new Counter();
        Counter b = new Counter();
        Counter c = new Counter();

        System.out.println("a.id=" + a.id + "  b.id=" + b.id + "  c.id=" + c.id);
        System.out.println("Counter.total (shared, lives in the method area) = " + Counter.total);
        System.out.println("a.getClass() == b.getClass() : " + (a.getClass() == b.getClass()));
        System.out.println("the Class object's name      : " + a.getClass().getName());
    }
}
