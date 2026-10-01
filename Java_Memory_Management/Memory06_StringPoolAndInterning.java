/**
 * Java Memory Management — Lecture #45 (Coder Army) — Demo 06
 *
 * The String Pool and intern()
 * -----------------------------------------------------------------
 * String literals are stored in the STRING POOL. The pool lived in the method
 * area (PermGen) before Java 7 and has lived on the HEAP since Java 7. Two
 * identical literals therefore point at the SAME object.
 *
 *   "java" == "java"                       -> true   (one pooled object)
 *   new String("java") == "java"           -> false  (new always makes a copy)
 *   new String("java").intern() == "java"  -> true   (intern() returns the pool copy)
 *   ("ja" + "va") == "java"                -> true   (folded by the compiler)
 *   ("ja" + suffix) == "java"              -> false  (built at runtime)
 *
 * Comparing strings with == is therefore a bug in real code: it compares
 * references, not characters. Always use equals().
 */
public class Memory06_StringPoolAndInterning {

    public static void main(String[] args) {
        String literal        = "java";              // pooled literal
        String literal2       = "java";              // same pooled object
        String heapCopy       = new String("java");  // brand-new heap object
        String interned       = heapCopy.intern();   // asks the pool for its copy
        String folded         = "ja" + "va";         // constant folding at compile time
        String suffix         = "va";
        String runtimeConcat  = "ja" + suffix;       // concatenated at runtime

        System.out.println("literal == literal2        : " + (literal == literal2));
        System.out.println("heapCopy == literal        : " + (heapCopy == literal));
        System.out.println("interned == literal        : " + (interned == literal));
        System.out.println("folded == literal          : " + (folded == literal));
        System.out.println("runtimeConcat == literal   : " + (runtimeConcat == literal));
        System.out.println();
        System.out.println("heapCopy.equals(literal)   : " + heapCopy.equals(literal)
                + "   // this is the comparison you actually want");
        System.out.println("literal identity hash      : " + System.identityHashCode(literal));
        System.out.println("literal2 identity hash     : " + System.identityHashCode(literal2));
        System.out.println("heapCopy identity hash     : " + System.identityHashCode(heapCopy));
    }
}
