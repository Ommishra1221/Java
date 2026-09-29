/*
 * ==========================================================================
 * Java Generics - Step 3 : The Generic Class  Box<T>
 * ==========================================================================
 *
 * CONCEPT (lecture notes, page 3):
 * Generics let a class be written ONCE but used with a SPECIFIC type that the
 * caller chooses. We replace the vague Object with a TYPE PARAMETER <T>:
 *
 *       class Box<T> {          // T is the TYPE PARAMETER (a placeholder)
 *           T value;
 *       }
 *
 *       Box<String> b1 = new Box<>();   // String is the TYPE ARGUMENT
 *
 *   - T is just a name, like a method parameter. Common choices: T (type),
 *     E (element), K/V (key/value), U (a second type).
 *   - Type information is NOT lost any more: the compiler remembers that b1
 *     holds Strings, so no cast is needed when reading and no wrong type can
 *     be stored.
 *
 * COMPILE-TIME vs RUNTIME:
 *   Wrong usage now fails AT COMPILE TIME (free, safe, caught in the IDE)
 *   instead of at runtime (in production, in front of users).
 *
 * --------------------------------------------------------------------------
 * COMPILE-TIME ERROR DEMONSTRATION (line left commented out on purpose):
 *
 *       // String s = (String) b1.getValue();
 *
 * If you uncomment it, the compiler rejects the program:
 *
 *       error: incompatible types: Integer cannot be converted to String
 *
 * Even by removing the cast it still fails:
 *
 *       String s = b1.getValue();     // still a compile error
 *
 * Compare that with Generics02_ObjectAsUniversalType.java, where the same
 * mistake compiled and only failed at RUNTIME. That difference is the whole
 * point of generics.
 * ==========================================================================
 */
public class Generics03_GenericClass {
    public static void main(String[] args) {
        // <> is the "diamond operator" - the compiler infers the type argument
        // from the reference type on the left (Box<Integer> -> Integer).
        GenericBox<Integer> b1 = new GenericBox<>(10);   // Type argument: Integer
        GenericBox<String>  b2 = new GenericBox<>("Hello");
        GenericBox<Boolean> b3 = new GenericBox<>(false);

        // No casting anywhere! getValue() is statically known to return
        // Integer / String / Boolean respectively.
        System.out.println(b1.getValue() + 5);   // 15      - arithmetic is legal
        System.out.println(b2.getValue());       // Hello
        System.out.println(b3.getValue());       // false

        // String s = (String) b1.getValue();    // !! COMPILE ERROR if uncommented !!
    }
}

/**
 * Generic class: Box<T>
 *
 * @param <T> the type of value this box will hold. It is decided by the
 *            caller at the point of use, not here.
 */
class GenericBox<T> {
    private T value;

    GenericBox(T value) {
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}

// Type information is NOT lost any more - that is the benefit of generics.
