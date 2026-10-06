/*
 * ==========================================================================
 * Java Generics - Step 4 : Generic Classes with MULTIPLE Type Parameters
 * ==========================================================================
 *
 * CONCEPT:
 * A generic class may declare more than one type parameter, separated by
 * commas. Each one is an independent placeholder:
 *
 *       class Pair<T, U> {     // T and U are two DIFFERENT types
 *           T first;
 *           U second;
 *       }
 *
 *       Pair<Integer, String> p1 = new Pair<>(23, "Aditya");
 *       //   ^T       ^U
 *
 * The compiler checks the constructor arguments against the declared type
 * arguments, so this would NOT compile:
 *
 *       Pair<Integer, String> p2 = new Pair<>("Aditya", 23);  // compile error
 *
 * Java's own libraries use the same idea everywhere - e.g.
 * java.util.Map<K, V>, java.util.HashMap<K, V>, Map.Entry<K, V>.
 * ==========================================================================
 */
public class Generics04_MultipleTypeParameters {
    public static void main(String[] args) {
        // T = Integer, U = String, both fixed at compile time.
        Pair<Integer, String> p1 = new Pair<>(23, "Aditya");

        // Accessing the fields keeps their real types (no casting needed):
        // p1.first is an Integer, p1.second is a String.
        System.out.println(p1.first + " , " + p1.second);   // 23 , Aditya
    }
}

/**
 * Generic class with two type parameters.
 *
 * @param <T> type of the first value
 * @param <U> type of the second value
 */
class Pair<T, U> {
    T first;
    U second;

    Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }
}
