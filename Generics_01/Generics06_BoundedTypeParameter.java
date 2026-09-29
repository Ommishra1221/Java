/*
 * ==========================================================================
 * Java Generics - Step 6 : BOUNDED TYPE PARAMETERS (upper bound with extends)
 * ==========================================================================
 *
 * PROBLEM:
 * Plain <T> means "T can be ANYTHING". Inside the class you may therefore only
 * call the methods that belong to Object - and Object has no doubleValue().
 *
 *       class Box<T> {
 *           T value;
 *           void printDouble() {
 *               System.out.println(value.doubleValue());  // COMPILE ERROR
 *           }
 *       }
 *
 * SOLUTION - UPPER BOUND:
 * Restrict what T may be, using the extends keyword:
 *
 *       class Box<T extends Number> { ... }
 *                     ^^^^^^^^^^^^^^^ T is AT LEAST a Number (Number or a
 *                                     subtype of Number).
 *
 * Because Number declares doubleValue(), the compiler now knows EVERY
 * possible T has that method, so the call compiles.
 *
 * "extends" is used for classes AND interfaces
 * (there is no "implements" for a bound):
 *
 *       <T extends Comparable<T>>     // Comparable is an interface
 *       <T extends Number>            // Number is an abstract class
 *
 * WHAT THE BOUND BUYS US:
 *   1. More methods are callable on T (everything declared by the bound).
 *   2. Callers cannot misuse the class: new Box<String>() is rejected at
 *      COMPILE TIME, because String is not a subtype of Number.
 *
 * See Generics07_MultipleBounds.java for the case of a class bound PLUS
 * interface bounds at the same time.
 * ==========================================================================
 */
public class Generics06_BoundedTypeParameter {
    public static void main(String[] args) {
        // T is inferred/bound to Integer, which passes the Number test.
        NumberBox<Integer> b1 = new NumberBox<>();
        b1.value = 5;
        b1.printDouble();          // 5.0   - doubleValue() is available on Number

        // The bound is enforced by the compiler:
        // NumberBox<String> b2 = new NumberBox<>();  // !! COMPILE ERROR !!
        // error: type argument String is not within bounds of type-variable T
    }
}

/**
 * Generic class whose type parameter is BOUNDED.
 *
 * @param <T> must be Number or a subtype of Number (Integer, Double, Float,
 *            Long, Short, Byte, BigInteger, BigDecimal, ...).
 */
class NumberBox<T extends Number> {
    T value;

    public void printDouble() {
        // Legal only because of the bound: every T here IS-A Number.
        System.out.println(value.doubleValue());
    }
}

// Generics --> T can be anything.
// Bounds in Generics:
// Upper bound --> T is at least Number, or its subtype.
