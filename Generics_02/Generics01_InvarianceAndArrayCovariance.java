import java.util.ArrayList;
import java.util.List;

/*
 * ============================================================================
 * LECTURE STEP 1/5 — GENERICS ARE INVARIANT, ARRAYS ARE COVARIANT
 * ============================================================================
 * Key idea:
 *   Dog  IS-A  Animal   (inheritance / subtyping)
 *   BUT  List<Dog> is NOT a List<Animal>   -> generics are INVARIANT
 *   AND  Dog[]     IS-A  Animal[]          -> arrays are COVARIANT
 *
 * Covariance buys flexibility but breaks type safety, so arrays must be
 * checked at RUNTIME (ArrayStoreException). Generics close that hole at
 * COMPILE time by refusing the assignment in the first place.
 *
 * !! THIS EXAMPLE IS INTENTIONALLY DESIGNED TO THROW AT RUNTIME !!
 *    `animals[4] = new Animal();` compiles fine (Animal[] is a legal target)
 *    but the real object behind `animals` is a Dog[], so the JVM throws
 *    java.lang.ArrayStoreException. That throw IS the lesson, not a bug.
 * ============================================================================
 */
public class Generics01_InvarianceAndArrayCovariance {

    public static void main(String[] args) {

        // ---- Invariance in generics -------------------------------------
        // Animal animal = new Dog();   // fine: a Dog is-an Animal
        // animal.eat();
        // animal.walk();

        // List<Dog> dogs = new ArrayList<>();
        // List<Animal> animals = dogs;  // COMPILE-TIME ERROR: invariant!
        //                               // List<Dog> is NOT a List<Animal>

        // ---- Arrays are covariant (the dangerous convenience) ------------
        Dog[] dogs = new Dog[10];
        Animal[] animals = dogs;      // COMPILES: Dog[] IS-A Animal[]

        animals[0] = new Dog();       // fine
        animals[1] = new Dog();
        animals[2] = new Dog();
        animals[3] = new Dog();
        animals[4] = new Animal();    // >> ARRAYSTOREEXCEPTION << (runtime)

        for (Animal animal : animals) {

            if (animal == null) {
                continue;
            }

            animal.eat();
        }
    }

    // Helper hierarchy kept as nested classes so every lecture file is
    // self-contained and the whole folder compiles as one unit.
    static class Animal {
        void eat() {
            System.out.println("Eating");
        }

        void walk() {
            System.out.println("Walking");
        }
    }

    static class Dog extends Animal {
        void bark() {
            System.out.println("Barking");
        }
    }
}
