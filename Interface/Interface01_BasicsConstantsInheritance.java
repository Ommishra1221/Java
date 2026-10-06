/**
 * Java Interfaces - Lecture #24 (Coder Army) - Part I
 *
 * INTERFACE BASICS: CONTRACTS, CONSTANTS, MULTIPLE + MULTI-LEVEL INHERITANCE
 * =========================================================================
 * An interface is a pure CONTRACT: a list of method signatures that an
 * implementing class promises to supply. It is NOT a class - it has no
 * constructor, it cannot be instantiated with new, and before Java 8 it
 * contained no executable code at all.
 *
 * Run it (the first argument selects the demo):
 *
 *   java -cp out Interface01_BasicsConstantsInheritance                # all
 *   java -cp out Interface01_BasicsConstantsInheritance polymorphism   # Demo.java
 *   java -cp out Interface01_BasicsConstantsInheritance constants      # Demo2.java
 *   java -cp out Interface01_BasicsConstantsInheritance multiple       # Demo3.java
 *   java -cp out Interface01_BasicsConstantsInheritance inheritance    # Demo4.java
 *
 * Demonstrates:
 *   1. polymorphism()         - one interface variable, many behaviours (Demo.java)
 *   2. constants()            - interface fields are public static final (Demo2.java)
 *   3. multipleInheritance()  - a class may IMPLEMENT many interfaces (Demo3.java)
 *   4. interfaceInheritance() - an interface may EXTEND another interface (Demo4.java)
 *
 * Part II (Interface02_DefaultMethodsResolution.java) covers Java 8+ default,
 * static and private methods, the diamond problem, and the resolution rules.
 */
public class Interface01_BasicsConstantsInheritance {

    // =================================================================
    // 1. Polymorphism through an interface  (Demo.java)
    // =================================================================
    //
    // `Payment` names only WHAT can be done, never HOW. CreditCard and
    // DebitCard each supply their own HOW. The caller holds the interface
    // type, so the SAME call site runs different code depending on the
    // real object -> dynamic (runtime) dispatch.

    interface Payment {
        void pay();                      // implicitly public abstract
    }

    static class CreditCard implements Payment {
        @Override public void pay() {
            System.out.println("[polymorphism] Paying via credit card");
        }
    }

    static class DebitCard implements Payment {
        @Override public void pay() {
            System.out.println("[polymorphism] Paying via debit card");
        }
    }

    static void polymorphism() {
        // The variable type is the INTERFACE, the object type is the class.
        Payment p = new DebitCard();
        p.pay();                         // dispatches to DebitCard.pay()

        System.out.println("[polymorphism] p.getClass() = " + p.getClass().getName());
        System.out.println("[polymorphism] p instanceof Payment = " + (p instanceof Payment));

        // One loop, one call site, two different behaviours.
        Payment[] wallet = { new CreditCard(), new DebitCard() };
        for (Payment each : wallet) {
            each.pay();
        }
    }

    // =================================================================
    // 2. Interface variables are constants  (Demo2.java)
    // =================================================================
    //
    // A field declared in an interface is implicitly
    //     public static final
    // even though none of those keywords is written. It is therefore a
    // COMPILE-TIME CONSTANT, shared by every implementer, and it can never
    // be reassigned (see the note for the exact compiler error).

    interface MathConstant {
        double PI_VALUE = 3.14;          // public static final double
        int VALUE = 10;                  // public static final int
    }

    static class Circle implements MathConstant {
        void show() {
            // The constant is in scope INSIDE any implementing class.
            System.out.println("[constants] PI_VALUE seen unqualified inside Circle = " + PI_VALUE);
        }
    }

    static void constants() {
        System.out.println("[constants] MathConstant.PI_VALUE = " + MathConstant.PI_VALUE);
        System.out.println("[constants] MathConstant.VALUE    = " + MathConstant.VALUE);

        // Constants are inherited by implementers (and by sub-interfaces).
        System.out.println("[constants] Circle.PI_VALUE        = " + Circle.PI_VALUE);
        new Circle().show();
    }

    // =================================================================
    // 3. Multiple inheritance of TYPE through interfaces  (Demo3.java)
    // =================================================================
    //
    // A Java class may extend only ONE class, but it may implement ANY
    // number of interfaces. This is how Java gets multiple inheritance of
    // TYPE (a Performer IS-A Singer and IS-A Dancer) while keeping a single
    // implementation chain, which avoids the C++ diamond-of-state mess.

    interface Singer { void sing(); }

    interface Dancer { void dance(); }

    static class Performer implements Singer, Dancer {
        @Override public void sing()  { System.out.println("[multiple] singing a song"); }
        @Override public void dance() { System.out.println("[multiple] dancing a step"); }
    }

    static void multipleInheritance() {
        Performer performer = new Performer();
        performer.sing();
        performer.dance();

        // The same object is reachable through each of its interface types.
        Singer asSinger = performer;
        Dancer asDancer = performer;
        asSinger.sing();
        asDancer.dance();
        System.out.println("[multiple] one object, two contracts: "
                + (asSinger == asDancer));
    }

    // =================================================================
    // 4. Interface inheritance (an interface EXTENDS an interface)  (Demo4.java)
    // =================================================================
    //
    // `Dog` extends `Animal`, so Dog's contract = Animal's methods + its own.
    // A class implementing Dog must satisfy BOTH. Interface-to-interface
    // reuse uses `extends` (and may list several parents); class-to-interface
    // reuse uses `implements`.

    interface Animal {
        void eat();
    }

    interface Dog extends Animal {       // Dog IS-A Animal
        void bark();
    }

    static class StreetDog implements Dog {
        @Override public void eat()  { System.out.println("[inheritance] Eating"); }
        @Override public void bark() { System.out.println("[inheritance] Barking"); }
    }

    static void interfaceInheritance() {
        StreetDog dog = new StreetDog();
        dog.eat();
        dog.bark();

        // StreetDog is-a Dog is-a Animal: all three views work.
        Dog asDog = dog;
        Animal asAnimal = dog;
        asDog.bark();
        asAnimal.eat();
    }

    // =================================================================
    // Runner
    // =================================================================

    public static void main(String[] args) {
        String mode = (args.length == 0) ? "all" : args[0];
        System.out.println("=== Part I - interface basics ===\n");
        switch (mode) {
            case "all"         -> { polymorphism(); constants();
                                    multipleInheritance(); interfaceInheritance(); }
            case "polymorphism" -> polymorphism();
            case "constants"   -> constants();
            case "multiple"    -> multipleInheritance();
            case "inheritance" -> interfaceInheritance();
            default            -> System.out.println(
                    "usage: all | polymorphism | constants | multiple | inheritance");
        }
    }
}
