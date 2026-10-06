/**
 * Java Strings - Lecture #25 (Coder Army) - Part I, file 2 of 2
 *
 * IMMUTABILITY, AND WHY MUTATING A STRING IN A LOOP IS EXPENSIVE
 * =============================================================
 * A String can never be changed after construction. Every "modifying"
 * method - concat, substring, toUpperCase, replace, ... - returns a NEW
 * String and leaves the original untouched.
 *
 * That is what makes strings safe to share and cheap to hash, but it turns
 *     s = s + ch;
 * in a loop into an O(n^2) trap: every iteration copies the WHOLE string
 * built so far into a fresh object. StringBuilder (Part II) fixes it.
 *
 * Run it (the first argument selects the demo):
 *
 *   java -cp out String02_ImmutabilityAndCost              # all
 *   java -cp out String02_ImmutabilityAndCost loop         # the += loop (was Demo3.java)
 *   java -cp out String02_ImmutabilityAndCost proof        # methods never mutate the original
 *   java -cp out String02_ImmutabilityAndCost cost         # += vs StringBuilder timing
 */
public class String02_ImmutabilityAndCost {

    // =================================================================
    // 1. The += loop - a new object every iteration  (was Demo3.java)
    // =================================================================

    static void loopBuild() {
        String s = "";
        for (int i = 0; i < 5; i++) {
            String before = s;           // keep the old reference alive to compare
            s += i;                      // s = s + i; -> a BRAND-NEW String each time
            System.out.println("[loop] s = \"" + s + "\"   new object this step? " + (before != s));
        }
        System.out.println("[loop] the pieces \"0\", \"01\", \"012\", ... accumulate as separate objects");
    }

    // =================================================================
    // 2. Proof that "modifying" methods return NEW Strings
    // =================================================================

    static void immutabilityProof() {
        String original = "Hello";

        String upper = original.toUpperCase();
        String sub = original.substring(0, 2);
        String concat = original + "!";
        String replaced = original.replace('l', 'L');

        System.out.println("[proof] original           = " + original);
        System.out.println("[proof] toUpperCase()      = " + upper);
        System.out.println("[proof] substring(0, 2)    = " + sub);
        System.out.println("[proof] original + \"!\"     = " + concat);
        System.out.println("[proof] replace('l', 'L')  = " + replaced);
        System.out.println("[proof] original afterwards= " + original + "   <- unchanged");
        boolean allNew = (upper != original) && (sub != original)
                && (concat != original) && (replaced != original);
        System.out.println("[proof] every result was a NEW object : " + allNew);
    }

    // =================================================================
    // 3. Cost: += copies the whole string each iteration (quadratic)
    // =================================================================

    static void costComparison() {
        int n = 20_000;

        long t0 = System.nanoTime();
        String s = "";
        for (int i = 0; i < n; i++) {
            s += "x";                    // copies up to n chars -> ~n^2/2 copies
        }
        long t1 = System.nanoTime();

        StringBuilder sb = new StringBuilder();   // a growable, mutable buffer
        for (int i = 0; i < n; i++) {
            sb.append("x");
        }
        String s2 = sb.toString();
        long t2 = System.nanoTime();

        System.out.printf("[cost] +=             built %,d chars in %8.3f ms   (quadratic)%n",
                s.length(), (t1 - t0) / 1_000_000.0);
        System.out.printf("[cost] StringBuilder  built %,d chars in %8.3f ms   (linear)%n",
                s2.length(), (t2 - t1) / 1_000_000.0);
        System.out.println("[cost] identical text: " + s.equals(s2));
    }

    // =================================================================
    // Runner
    // =================================================================

    public static void main(String[] args) {
        String mode = (args.length == 0) ? "all" : args[0];
        System.out.println("=== String part I.2 - immutability & cost ===\n");
        switch (mode) {
            case "all"   -> { loopBuild(); immutabilityProof(); costComparison(); }
            case "loop"  -> loopBuild();
            case "proof" -> immutabilityProof();
            case "cost"  -> costComparison();
            default      -> System.out.println("usage: all | loop | proof | cost");
        }
    }
}
