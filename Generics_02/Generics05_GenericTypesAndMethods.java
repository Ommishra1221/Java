/*
 * ============================================================================
 * LECTURE STEP 5/5 — TYPE PARAMETERS: GENERIC CLASSES AND GENERIC METHODS
 * ============================================================================
 * The wildcards of steps 2-4 answer "what may I pass in?".
 * A type parameter <T> answers "let me write code that is generic once and
 * checked everywhere".
 *
 *   class Box<T>        -> generic CLASS   : T is fixed per instance
 *   <T> void fun(T a,T b) -> generic METHOD: T is inferred per call
 *
 * <T> vs <?>  (the classic interview question)
 *   <T>  gives the type a NAME, so you can use it more than once
 *        (accept T, return T, store T, declare new T's).
 *   <?>  is anonymous: "some unknown type", read-only by nature.
 *
 * <T> also LINKS two parameters to one type: `fun(T a, T b)` is still fully
 * checked, because both arguments must fit a COMMON supertype T (the compiler
 * infers the least upper bound). Two `?` parameters, by contrast, would each
 * be independently unknown and therefore unlinked.
 * ============================================================================
 */
public class Generics05_GenericTypesAndMethods {

    public static void main(String[] args) {
        Box<String> b1 = new Box<>();   // T = String for this instance
        b1.value = "hello";
        System.out.println(b1.value);

        Box<Integer> b2 = new Box<>();
        b2.value = 42;
        System.out.println(b2.value);

        // Type parameter is INFERRED from the arguments:
        fun("a", "b");     // T = String
        fun(1, 2);         // T = Integer
        // fun("a", 1);    // COMPILES: String and Integer do share a supertype,
        //                 // so T is inferred as that least upper bound (an
        //                 // intersection type). Prints:  T = java.lang.String , 1
        //                 // NOTE: the "T = " label shows the ERASED RUNTIME
        //                 // class, not the inferred T - generics are gone at runtime.
    }

    public static <T> void fun(T a, T b) { // type parameter
        System.out.println("T = " + a.getClass().getName() + " , " + b);
    }

    static class Box<T> {
        T value;
    }
}

// <T>  vs  <?>
// super and extends
