/**
 * Java Strings - Lecture #25 (Coder Army) - Part I, file 1 of 2
 *
 * THE STRING POOL, IDENTITY, CONSTANT FOLDING AND intern()
 * =======================================================
 * A String is an object. Where it lives decides whether two equal-looking
 * strings are the SAME object:
 *
 *   "Hello"           -> a LITERAL: one shared object in the STRING POOL
 *   new String("Hello") -> a BRAND-NEW object on the HEAP
 *
 * `==` compares references (identities); `.equals()` compares contents.
 * Use `.equals()` for text.
 *
 * Run it (the first argument selects the demo):
 *
 *   java -cp out String01_PoolAndIdentity            # all
 *   java -cp out String01_PoolAndIdentity pool       # literals vs new
 *   java -cp out String01_PoolAndIdentity folding     # compile-time concat
 *   java -cp out String01_PoolAndIdentity alias       # reference copy + reassignment
 *   java -cp out String01_PoolAndIdentity intern       # intern() rejoins the pool
 *
 * File 2 (String02_ImmutabilityAndCost.java) covers immutability and why
 * mutating a String in a loop is expensive.
 */
public class String01_PoolAndIdentity {

    // =================================================================
    // 1. Pool vs heap  (was Demo.java)
    // =================================================================

    static void pool() {
        String literal1 = "Hello";            // goes into the String pool
        String literal2 = "Hello";            // reuses the SAME pooled object
        String heap1 = new String("Hello");   // forces a new heap object
        String heap2 = new String("Hello");   // and another one

        System.out.println("[pool] literal1 == literal2  : " + (literal1 == literal2));
        System.out.println("[pool] heap1    == heap2      : " + (heap1 == heap2));
        System.out.println("[pool] literal1 == heap1      : " + (literal1 == heap1));
        System.out.println("[pool] literal1.equals(heap1) : " + literal1.equals(heap1)
                + "   // same text, different object");
        System.out.println("[pool] identity hashes        : literal1="
                + System.identityHashCode(literal1) + " literal2=" + System.identityHashCode(literal2)
                + "  (same number -> same object)");
        System.out.println("[pool]                            heap1="
                + System.identityHashCode(heap1) + "  (a different object)");
    }

    // =================================================================
    // 2. Compile-time folding vs runtime concatenation  (was Demo2.java)
    // =================================================================
    //
    // If the compiler can see every piece at compile time it FOLDS the
    // concatenation into one pooled constant. If any piece is a variable,
    // the result is built at RUNTIME (a new heap String).

    static void folding() {
        String folded = "Ja" + "va";     // both operands constant -> ldc "Java"
        String literal = "Java";
        String suffix = "va";
        String runtime = "Ja" + suffix;  // suffix is a variable -> built at runtime

        System.out.println("[folding] (\"Ja\" + \"va\")  == \"Java\" : " + (folded == literal)
                + "   // folded to the pooled literal");
        System.out.println("[folding] (\"Ja\" + suffix) == \"Java\" : " + (runtime == literal)
                + "   // built at runtime -> new object");
        System.out.println("[folding] runtime.equals(literal)     : " + runtime.equals(literal)
                + "   // content is equal either way");
    }

    // =================================================================
    // 3. Reference copy and reassignment - Strings are immutable  (was Demo2.java)
    // =================================================================

    static void alias() {
        String a = "Hello";
        String b = a;                    // copies the REFERENCE, not the text
        System.out.println("[alias] b = a; then a == b : " + (a == b)
                + "   // both names point at one pooled object");

        b = "World";                     // REPOINTS b; the "Hello" object is untouched
        System.out.println("[alias] after b = \"World\": a = " + a + ", b = " + b);
        System.out.println("[alias] a == b now         : " + (a == b));
        System.out.println("[alias] a is still the old text -> String is immutable");
    }

    // =================================================================
    // 4. intern() - pulling a heap String back into the pool
    // =================================================================

    static void intern() {
        String heap = new String("Hello");   // not the pooled object
        String pooled = "Hello";             // the pooled object
        String interned = heap.intern();     // ask the pool for ITS copy

        System.out.println("[intern] heap     == pooled   : " + (heap == pooled));
        System.out.println("[intern] interned == pooled   : " + (interned == pooled));
        System.out.println("[intern] heap     == interned : " + (heap == interned));
        System.out.println("[intern] all three .equals() each other : "
                + (heap.equals(pooled) && pooled.equals(interned)));
    }

    // =================================================================
    // Runner
    // =================================================================

    public static void main(String[] args) {
        String mode = (args.length == 0) ? "all" : args[0];
        System.out.println("=== String part I.1 - pool & identity ===\n");
        switch (mode) {
            case "all"     -> { pool(); folding(); alias(); intern(); }
            case "pool"    -> pool();
            case "folding" -> folding();
            case "alias"   -> alias();
            case "intern"  -> intern();
            default        -> System.out.println(
                    "usage: all | pool | folding | alias | intern");
        }
    }
}
