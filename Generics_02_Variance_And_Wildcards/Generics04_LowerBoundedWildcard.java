import java.util.ArrayList;
import java.util.List;

/*
 * ============================================================================
 * LECTURE STEP 4/5 — LOWER-BOUNDED WILDCARD  <? super Animal>
 * ============================================================================
 * `List<? super Animal>` means: SOME unknown type that IS-A SUPERtype of
 * Animal. It matches List<Animal>, List<Object> ... but NOT List<Dog>.
 *
 * This is the CONSUMER side: "give me a list I can safely PUT Animals into".
 *
 *   WRITE -> allowed, for Animal and every subclass. Because the list is at
 *            least a List<Animal>, an Animal (and any Dog/Cat/Labrador) is
 *            always a legal element.                                  OK
 *
 *   READ  -> only as Object. The list might really be List<Object>, so the
 *            compiler cannot promise an Animal comes back; you must cast.
 *
 * This is exactly the mirror image of step 3.
 * ============================================================================
 */
public class Generics04_LowerBoundedWildcard {

    public static void main(String[] args) {
        List<Animal> animals = new ArrayList<>();
        animals.add(new Animal());
        animals.add(new Animal());

        fun(animals);   // List<Animal> is accepted by List<? super Animal>
        // List<Object> would also be accepted.
        // List<Dog>    would NOT be accepted.
    }

    public static void fun(List<? super Animal> values) {
        // --- writing: this is what the lower bound buys us ---------------
        values.add(new Animal());
        values.add(new Dog());
        values.add(new Cat());
        values.add(new Labrador());

        // --- reading: only Object, we lost the type info -----------------
        for (Object obj : values) {
            Animal a = (Animal) obj;   // explicit downcast required
            a.eat();
        }

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

    static class Labrador extends Dog {

    }

    static class Cat extends Animal {

    }
}
