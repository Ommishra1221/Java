/*
 * ==========================================================================
 * Java Generics - Step 2 : Object as a "Universal Type" and its LIMITATIONS
 * ==========================================================================
 *
 * CONCEPT (lecture notes, page 3):
 * Before generics, programmers used Object as a universal container, because
 * every class in Java IS-A Object. So a single class could hold anything:
 *
 *       Object value;             // can store Integer, String, Boolean, ...
 *
 * LIMITATIONS OF USING Object AS A UNIVERSAL TYPE (page 3 of the notes):
 *   1. Type information is lost.          -> you no longer know what is inside
 *   2. Wrong Object could be inserted.    -> nothing stops you at compile time
 *   3. Casting becomes necessary when reading. -> (String) box.getValue()
 *   4. Many errors shift to Runtime.      -> they escape the compiler
 *
 * Those four points are precisely what GENERICS fix (see the next file,
 * Generics03_GenericClass.java).
 *
 * --------------------------------------------------------------------------
 * !!  INTENTIONAL RUNTIME ERROR IN THIS FILE  !!
 * --------------------------------------------------------------------------
 * b1 was created with the Integer 10. Reading it back and casting it to String
 * compiles (Object -> String is a legal downcast syntax) but throws
 * ClassCastException at RUNTIME, because the stored object is an Integer.
 *
 * Expected output when you run this file:
 *
 *       Exception in thread "main" java.lang.ClassCastException:
 *           class java.lang.Integer cannot be cast to class java.lang.String
 * ==========================================================================
 */
public class Generics02_ObjectAsUniversalType {
    public static void main(String[] args) {
        // The SAME class is used for three completely different data types.
        // The compiler happily accepts all of them - that is the problem.
        ObjectBox b1 = new ObjectBox(10);
        ObjectBox b2 = new ObjectBox("Hello");
        ObjectBox b3 = new ObjectBox(true);

        // ---------- What the correct (but ugly) usage looks like ----------
        // Casting is forced on us EVERY time we read the value, because the
        // type information was lost when it was stored as Object.
        // Integer x = (Integer) b1.getValue();
        // String  s = (String)  b2.getValue();
        // Boolean b = (Boolean) b3.getValue();

        // System.out.println(x + 5);   // 15        - needed the cast to do maths
        // System.out.println(s + 5);   // Hello5    - works, but only by luck
        // System.out.println(b);       // true

        // ---------- The mistake this design allows (INTENTIONAL ERROR) ----------
        // b1 holds an Integer (10), not a String. Nothing warned us at compile
        // time. The JVM only discovers the mistake at RUNTIME.
        String s = (String) b1.getValue();   // !! INTENTIONAL ClassCastException !!

        System.out.println(s);               // never reached
    }
}

/**
 * A "box" that can store anything, because everything IS-A Object.
 *
 * Object --> too generic: the type information is lost the moment you store
 * a value, so every read has to be cast back by hand.
 */
class ObjectBox {
    private Object value;

    ObjectBox(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return this.value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
