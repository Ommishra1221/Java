/*
 * ==========================================================================
 * Java Generics - Step 5 : GENERIC METHODS and TYPE INFERENCE
 * ==========================================================================
 *
 * CONCEPT:
 * Generics are not limited to classes. A single METHOD can declare its own
 * type parameter, right in front of the return type:
 *
 *       <T> returnType methodName(T parameter) { ... }
 *       ^^^ the type-parameter declaration (mandatory, it is NOT optional)
 *
 * Note the position carefully:
 *
 *       public static <T> T getResult(T x) { ... }
 *                     ^^^  <- declared here, before the return type T
 *
 * Without that leading <T> the compiler would look for a class named T and
 * fail with "cannot find symbol".
 *
 * TYPE INFERENCE:
 * You normally do NOT write the type argument; the compiler infers it from
 * the arguments you pass:
 *
 *       getResult(23)        -> T is inferred as Integer
 *       getResult("Aditya")  -> T is inferred as String
 *
 * Every call is still type-checked, so this will never compile:
 *
 *       String bad = getResult(23);   // compile error: Integer -> String
 *
 * Type parameters belong to the method, so a generic method and a generic
 * class are independent features: you can have a generic method inside a
 * plain (non-generic) class, which is exactly what this file shows.
 * ==========================================================================
 */
public class Generics05_GenericMethods {
    public static void main(String[] args) {
        // Integer y = getResult(23);   // inferred T = Integer, returns Integer
        // System.out.println(y);

        printPair(11, 23);              // inferred T = Integer, U = Integer

        // Type inference - the compiler works the type arguments out for us.
        // You may also write them explicitly if you wish:
        // Generics05_GenericMethods.<Integer, String>printPair(11, "Eleven");
    }

    /**
     * A generic method with a single type parameter.
     * <T> is the type-parameter declaration; T is the return type.
     */
    public static <T> T getResult(T x) {   // <T> = type parameter
        return x;
    }

    /**
     * A generic method with two independent type parameters.
     */
    public static <T, U> void printPair(T first, U second) {
        System.out.println(first + " , " + second);   // 11 , 23
    }
}

// Generic methods:
//   <T> returnType methodName(T parameter) {
//   }
