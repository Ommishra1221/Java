/**
 * Java Interfaces - Lecture #24 (Coder Army) - Part II
 *
 * JAVA 8+ DEFAULT / STATIC / PRIVATE METHODS, THE DIAMOND PROBLEM,
 * AND THE RESOLUTION-PRIORITY RULES
 * ==================================================================
 * Before Java 8 an interface could only declare abstract methods. That
 * changed:
 *
 *   Java 8  ->  default methods (a BODY in the interface, so interfaces can
 *               be evolved without breaking existing implementers) and
 *               static methods (helpers that belong to the interface).
 *   Java 9  ->  private methods (shared code used by the interface's own
 *               default/static methods, hidden from implementers).
 *
 * Evolve an interface and you can create the DIAMOND problem: two unrelated
 * interfaces each provide a default for the same signature and a class
 * implements both. Java then needs clear rules for which body wins.
 *
 * Run it (the first argument selects the demo):
 *
 *   java -cp out Interface02_DefaultMethodsResolution                 # all
 *   java -cp out Interface02_DefaultMethodsResolution defaultMethods  # Demo5.java
 *   java -cp out Interface02_DefaultMethodsResolution diamond         # Demo6.java
 *   java -cp out Interface02_DefaultMethodsResolution priority        # Demo7.java
 */
public class Interface02_DefaultMethodsResolution {

    // =================================================================
    // 5. default, static and private interface methods  (Demo5.java)
    // =================================================================
    //
    //   default void drive()      -> inherited by implementers; may be overridden
    //   static  void brake()      -> belongs to the INTERFACE, called on its NAME
    //   private void accelerate() -> used only INSIDE Vehicle; not inherited

    interface Vehicle {
        default void drive() {                     // Java 8: a body in an interface
            System.out.println("[default] Vehicle is driving");
            accelerate();                          // call the private helper
        }

        static void brake() {                      // Java 8: interface-level utility
            System.out.println("[static ] Vehicle is applying brake");
        }

        private void accelerate() {                // Java 9: internal helper
            System.out.println("[private] Vehicle is Accelerating");
        }
    }

    /** Inherits drive() unchanged - no method of its own. */
    static class Car implements Vehicle { }

    /** Overrides the default, and explicitly re-invokes the interface's own body. */
    static class SportsCar implements Vehicle {
        @Override public void drive() {
            System.out.println("[default] SportsCar overrides drive()");
            Vehicle.super.drive();                 // InterfaceName.super.method()
        }
    }

    static void defaultMethods() {
        Vehicle car = new Car();
        car.drive();              // invokeinterface -> inherited default -> calls private

        Vehicle.brake();          // static: on the interface NAME, never on an instance

        new SportsCar().drive();  // override + InterfaceName.super delegation
    }

    // =================================================================
    // 6. The diamond problem  (Demo6.java)
    // =================================================================
    //
    // B and C both extend A and both provide THEIR OWN `default void fun()`.
    // They are unrelated, so a class implementing both inherits two
    // conflicting defaults. Java REFUSES to guess: the class MUST override
    // fun() (or pick a parent with `X.super.fun()`). That is the diamond
    // problem, and interfaces let a class solve it explicitly.

    interface A {
        void fun();                                // abstract
    }

    interface B extends A {
        @Override default void fun() { System.out.println("[diamond] B"); }
    }

    interface C extends A {
        @Override default void fun() { System.out.println("[diamond] C"); }
    }

    /** Resolves by providing its own body. */
    static class D implements B, C {
        @Override public void fun() { System.out.println("[diamond] D"); }
    }

    /** Resolves by delegating to ONE parent's default. */
    static class DelegatingD implements B, C {
        @Override public void fun() { B.super.fun(); }   // choose B's body
    }

    static void diamond() {
        new D().fun();            // D
        new DelegatingD().fun();  // B  (explicit choice)

        // The diamond is only a problem for BODIES; the TYPE diamond is fine:
        A asA = new D();
        asA.fun();                // -> D.fun()
        System.out.println("[diamond] type diamond through A works: "
                + (asA instanceof B) + " / " + (asA instanceof C));
    }

    // =================================================================
    // 7. Resolution priority: class beats interface  (Demo7.java)
    // =================================================================
    //
    // When a class inherits a CONCRETE method from a superclass AND a default
    // method of the SAME signature from an interface, the SUPERCLASS method
    // wins - no override needed, no ambiguity (JLS 8.4.8.1). Only if the
    // superclass method is absent do defaults get considered, and then the
    // most specific override wins.

    interface Greeter {
        default void hello() { System.out.println("[priority] default from interface Greeter"); }
    }

    static class Base {
        public void hello() { System.out.println("[priority] method from superclass Base"); }
    }

    /** No override: Base.hello() wins over Greeter's default. */
    static class Sub extends Base implements Greeter { }

    /** Own override wins over everything (the original Demo7 body). */
    static class Sub2 extends Base implements Greeter {
        @Override public void hello() { System.out.println("[priority] method from Sub2 itself"); }
    }

    static void resolutionPriority() {
        new Sub().hello();        // class beats interface -> Base
        new Sub2().hello();       // most specific -> Sub2
    }

    // =================================================================
    // Runner
    // =================================================================

    public static void main(String[] args) {
        String mode = (args.length == 0) ? "all" : args[0];
        System.out.println("=== Part II - default methods & resolution ===\n");
        switch (mode) {
            case "all"            -> { defaultMethods(); diamond(); resolutionPriority(); }
            case "defaultMethods" -> defaultMethods();
            case "diamond"        -> diamond();
            case "priority"       -> resolutionPriority();
            default               -> System.out.println(
                    "usage: all | defaultMethods | diamond | priority");
        }
    }
}
