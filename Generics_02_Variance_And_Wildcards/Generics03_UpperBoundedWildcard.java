import java.util.ArrayList;
import java.util.List;

/*
 * ============================================================================
 * LECTURE STEP 3/5 — UPPER-BOUNDED WILDCARD  <? extends Animal>
 * ============================================================================
 * `List<? extends Animal>` means: SOME unknown type that IS-A Animal.
 * It matches List<Animal>, List<Dog>, List<Cat>, List<Labrador> ... but NOT
 * List<Integer> / List<String>.
 *
 * This is the PRODUCER side: "give me any list I can READ Animals out of".
 *
 *   READ  -> an Animal (we know every element is at least an Animal)  OK
 *   WRITE -> forbidden (the list may really be a List<Dog>, so adding an
 *            Animal, or even a Dog, is not provably safe)             BLOCKED
 *
 * Nothing may be added except null. This is where "covariance" is reintroduced
 * safely: safe to read, impossible to corrupt.
 * ============================================================================
 */
public class Generics03_UpperBoundedWildcard {

    public static void main(String[] args) {
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog());
        dogs.add(new Dog());

        // List<Animal> animals = new ArrayList<>();
        // animals.add(new Animal());
        // animals.add(new Animal());

        // List<Integer> l = new ArrayList<>();
        // fun(l);   // COMPILE-TIME ERROR: Integer is not <? extends Animal>

        fun(dogs);    // List<Dog> is accepted by List<? extends Animal>
    }

    static void fun(List<? extends Animal> values) {
        // Reading is safe and returns a proper Animal:
        // for(Animal a : values) {
        //     a.eat();
        // }

        // Writing is rejected by the compiler:
        // values.add(new Dog());   // COMPILE-TIME ERROR: cannot add to a
        //                          // <? extends ...> list (only null is allowed)
    }

    static class Animal {
        void eat() {
            System.out.println("Animal Eating");
        }

        void walk() {
            System.out.println("Walking");
        }
    }

    static class Dog extends Animal {
        @Override
        void eat() {
            System.out.println("Dog Eating");
        }

        void bark() {
            System.out.println("Barking");
        }
    }

    static class Cat extends Animal {

    }
}
