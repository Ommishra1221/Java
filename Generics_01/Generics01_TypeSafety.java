/*
 * ==========================================================================
 * Java Generics - Step 1 : Type Safety, Upcasting & Downcasting
 * ==========================================================================
 *
 * CONCEPT (from the lecture notes, page 1):
 *   Java is a TYPED LANGUAGE: every value belongs to a data type, and every
 *   type comes with its own set of legal rules (operations) for that value.
 *
 *       value        ->  data type
 *       ---------------------------
 *       10           ->  Integer    (arithmetic operators)
 *       "Hello"      ->  String     (String methods)
 *       true         ->  Boolean
 *       Student obj  ->  Student    (Student methods)
 *
 *   Because the compiler knows these rules, it can stop illegal code BEFORE
 *   the program ever runs:
 *
 *       int x = 10;          // OK
 *       String s = "Hello";
 *       int y = s;           // COMPILE ERROR - a String is not an int
 *
 * CONCEPT (page 1 + page 2): Upcasting & Downcasting
 *   Upcasting   : Specific -> General   (Dog  -> Animal ,  String -> Object)
 *                 Happens automatically. NO cast is required.
 *   Downcasting : General  -> Specific  (Object -> String)
 *                 Must be written EXPLICITLY with a cast, because the
 *                 compiler cannot prove the object really is a String.
 *
 * --------------------------------------------------------------------------
 * !!  INTENTIONAL RUNTIME ERROR IN THIS FILE  !!
 * --------------------------------------------------------------------------
 * The LAST statement of main() is a deliberate, unsafe downcast:
 *
 *       Object obj3 = 10;             // Integer object
 *       String s3 = (String) obj3;    // <-- ClassCastException at RUNTIME
 *
 * It COMPILES fine (the cast is legal Java syntax - we are just telling the
 * compiler "trust me"), but it CRASHES on execution because the object in
 * memory is an Integer, not a String.
 *
 * Expected output when you run this file:
 *
 *       Aditya
 *       Exception in thread "main" java.lang.ClassCastException:
 *           class java.lang.Integer cannot be cast to class java.lang.String
 *
 * This is exactly why generics exist: generics move such errors from
 * RUNTIME back to COMPILE TIME.
 * ==========================================================================
 */
public class Generics01_TypeSafety {
    public static void main(String[] args) {

        // ---------- 1. UPCASTING (Specific -> General) ----------
        // A String IS-A Object, so the reference widens automatically.
        String s = "Hello";
        Object obj = s;              // upcasting - no cast written by us

        //System.out.println(obj);

        // ---------- 2. DOWNCASTING (General -> Specific), the SAFE case ----------
        // Here the object really IS a String, so the cast succeeds.
        Object obj2 = "Aditya";
        String s2 = (String) obj2;   // explicit cast is mandatory

        System.out.println(s2);      // prints: Aditya

        // ---------- 3. DOWNCASTING, the UNSAFE case (INTENTIONAL ERROR) ----------
        // The object on the heap is an Integer, but we promise the compiler it
        // is a String. The compiler believes us -> compiles fine.
        // At runtime the JVM checks the real type and throws ClassCastException.
        Object obj3 = 10;
        String s3 = (String) obj3;   // !! INTENTIONAL ClassCastException !!

        System.out.println(s3);      // never reached
    }
}
