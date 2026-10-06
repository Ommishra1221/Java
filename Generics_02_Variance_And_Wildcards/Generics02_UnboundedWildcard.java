import java.util.ArrayList;
import java.util.List;

/*
 * ============================================================================
 * LECTURE STEP 2/5 — UNBOUNDED WILDCARD  <?>
 * ============================================================================
 * Problem from step 1: `fun(List<Animal>)` cannot accept a List<Dog>,
 * because generics are invariant.
 *
 * Solution: `List<?>` ("list of unknown type"). It accepts List<Dog>,
 * List<Animal>, List<String>, ... i.e. it hands back the reusability that
 * invariance took away — while keeping type safety.
 *
 * The price (the "get/put" principle):
 *   READ  -> only as Object          (the real element type is unknown)
 *   WRITE -> impossible, except null (we cannot prove an add is safe)
 *
 * This is the first half of PECS: an unbounded wildcard is a pure PRODUCER
 * of Objects.
 * ============================================================================
 */
public class Generics02_UnboundedWildcard {

    public static void main(String[] args) {
        List<Dog> dogs = new ArrayList<>();
        dogs.add(new Dog());
        dogs.add(new Dog());

        fun(dogs);

        // List<Animal> animals = new ArrayList<>();
        // animals.add(new Animal());
        // animals.add(new Animal());

        // fun(animals);   // fine too: <?> accepts a List<Animal> as well
    }

    /*
     * Why this DOES NOT work (invariance):
     *
     * static void fun(List<Animal> animals) {
     *     for (Animal animal : animals) {
     *         animal.eat();
     *     }
     * }
     * // fun(dogs) -> COMPILE-TIME ERROR: List<Dog> is not List<Animal>
     */

    static void fun(List<?> values) {
        // for(Object obj : values) {
        //     System.out.println(obj.getClass().getName());
        // }

        // values.add(new Dog());   // COMPILE-TIME ERROR: nothing but null can
        //                          // be added to a List<?> - the element type
        //                          // is unknown, so no value is provably safe.

        Object obj = values.get(0);           // READ: always comes back as Object
        Animal a = (Animal) obj;              // explicit downcast is your responsibility
        System.out.println(obj.getClass().getName());
    }

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
