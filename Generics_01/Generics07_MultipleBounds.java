/*
 * ==========================================================================
 * Java Generics - Step 7 : MULTIPLE BOUNDS  (T extends Class & Interface)
 * ==========================================================================
 *
 * A type parameter can satisfy SEVERAL bounds at once. Separate them with the
 * ampersand & (NOT with a comma - a comma would declare another, independent
 * type parameter):
 *
 *       class Box<T extends Animal & Swimmable> { ... }
 *                                         ^ one ampersand per extra bound
 *
 * RULES FOR MULTIPLE BOUNDS (interview favourite):
 *   1. A class bound may appear AT MOST ONCE, and must come FIRST.
 *      <T extends Animal & Swimmable>    // OK  - class, then interface
 *      <T extends Swimmable & Animal>    // COMPILE ERROR - class must be first
 *      <T extends Dog & Animal>          // COMPILE ERROR - two classes
 *   2. Any number of interface bounds may follow, each separated by &.
 *   3. Combining the bounds means T must be a subtype of ALL of them.
 *
 * WHY BOTHER:
 * T now exposes the members of every bound - display() from Animal and
 * swim() from Swimmable - while still rejecting anything that is not both.
 *
 * SIZE (a detail worth remembering):
 * With several bounds, the erasure of T is the FIRST bound (Animal), not
 * Object. That is exactly why Java forces the class bound to come first.
 * ==========================================================================
 */
public class Generics07_MultipleBounds {
    public static void main(String[] args) {
        // Fish extends Animal AND implements Swimmable -> satisfies both bounds.
        SwimmableBox<Fish> b1 = new SwimmableBox<>();

        // The bounds are checked at COMPILE TIME, so these are rejected:
        // SwimmableBox<Dog> b2 = new SwimmableBox<>();
        //   !! COMPILE ERROR - Dog is an Animal but does NOT implement Swimmable !!
        // SwimmableBox<String> b3 = new SwimmableBox<>();
        //   !! COMPILE ERROR - String is neither Animal nor Swimmable !!

        // The bounds also determine which members T exposes. These two calls
        // are legal for EVERY possible T (compile-time guarantee), so you may
        // uncomment them to see the effect:
        // b1.value = new Fish();
        // b1.value.display();   // from the Animal bound   -> "Displaying Animal"
        // b1.value.swim();      // from the Swimmable bound -> "Fish is swimming"
    }
}

/**
 * Generic class with a class bound followed by an interface bound.
 *
 * @param <T> must be an Animal AND Swimmable (e.g. Fish).
 */
class SwimmableBox<T extends Animal & Swimmable> {
    T value;
}

class Animal {
    void display() {
        System.out.println("Displaying Animal");
    }
}

interface Swimmable {
    void swim();
}

/** Dog is an Animal but NOT Swimmable - so it cannot be used as T above. */
class Dog extends Animal {

}

/** Fish is both an Animal and Swimmable - the valid choice for T. */
class Fish extends Animal implements Swimmable {
    @Override
    public void swim() {
        System.out.println("Fish is swimming");
    }
}
