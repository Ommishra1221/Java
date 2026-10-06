/**
 * Java Strings - Lecture #25 Part 2 (Coder Army) - file 1 of 2
 *
 * STRING CONSTRUCTORS AND THE METHOD TOUR
 * ======================================
 * Create a String from a literal, another String, a char[] (or a slice of
 * one), a byte[] (or a slice), or a StringBuilder/StringBuffer.
 *
 * Then the working methods: length/isEmpty/isBlank, charAt/toCharArray,
 * equals/equalsIgnoreCase/compareTo, contains/indexOf/lastIndexOf/startsWith,
 * substring, toUpperCase/trim/strip, repeat/replace/replaceAll, split/join,
 * valueOf/getBytes, and format/printf.
 *
 * Run it (the first argument selects the demo):
 *
 *   java -cp out String01_ConstructorsAndMethods               # all
 *   java -cp out String01_ConstructorsAndMethods constructors  # the constructors
 *   java -cp out String01_ConstructorsAndMethods methods       # the method tour
 *   java -cp out String01_ConstructorsAndMethods format        # text formatting
 */
public class String01_ConstructorsAndMethods {

    // =================================================================
    // 1. Ways to build a String  (was Demo.java)
    // =================================================================

    static void constructors() {
        String empty = new String();
        System.out.println("[ctor] new String()               = \"" + empty + "\"   (length " + empty.length() + ")");

        String fromLiteral = new String("Hello");
        System.out.println("[ctor] new String(\"Hello\")        = " + fromLiteral);

        String source = "Aditya";
        String copy = new String(source);
        System.out.println("[ctor] new String(source)         = " + copy
                + "   (equal text, but a NEW object: " + (copy != source) + ")");

        char[] chars = "Aditya Tandon".toCharArray();
        System.out.println("[ctor] new String(char[])         = " + new String(chars));
        System.out.println("[ctor] new String(char[], 0, 6)   = " + new String(chars, 0, 6));

        byte[] bytes = {97, 98, 99};                 // ASCII a, b, c
        System.out.println("[ctor] new String(byte[], 0, 2)   = " + new String(bytes, 0, 2));

        StringBuffer buffer = new StringBuffer("Hello");
        StringBuilder builder = new StringBuilder("Hello");
        System.out.println("[ctor] new String(StringBuffer)   = " + new String(buffer));
        System.out.println("[ctor] new String(StringBuilder)  = " + new String(builder));
    }

    // =================================================================
    // 2. The method tour  (was Demo2.java)
    // =================================================================
    //
    // Reminder: every one of these returns a value; NONE changes the
    // receiver. String is immutable.

    static void methods() {
        String s = "Aditya";
        System.out.println("[m] s = \"" + s + "\"");

        // length / emptiness
        System.out.println("[m] length()                  = " + s.length());
        System.out.println("[m] isEmpty()                 = " + s.isEmpty());
        System.out.println("[m] isBlank()                 = " + s.isBlank());

        // character access
        System.out.println("[m] charAt(2)                 = " + s.charAt(2));
        System.out.println("[m] toCharArray()             = " + java.util.Arrays.toString(s.toCharArray()));

        // comparison
        System.out.println("[m] equals(\"abc\")             = " + s.equals("abc"));
        System.out.println("[m] equalsIgnoreCase(\"ADITYA\")= " + s.equalsIgnoreCase("ADITYA"));
        System.out.println("[m] compareTo(\"Adit\")         = " + s.compareTo("Adit")
                + "   (positive: s is longer)");
        System.out.println("[m] compareTo(\"Zebra\")        = " + s.compareTo("Zebra")
                + "   (negative: 'A' < 'Z')");

        // searching
        System.out.println("[m] contains(\"ity\")           = " + s.contains("ity"));
        System.out.println("[m] indexOf(\"ity\")            = " + s.indexOf("ity"));
        System.out.println("[m] lastIndexOf(\"ity\")        = " + s.lastIndexOf("ity"));
        System.out.println("[m] startsWith(\"Ad\")          = " + s.startsWith("Ad"));

        // extraction / transformation
        System.out.println("[m] substring(1)              = " + s.substring(1));
        System.out.println("[m] substring(1, 4)           = " + s.substring(1, 4)
                + "   (start inclusive, end exclusive)");
        System.out.println("[m] toUpperCase()             = " + s.toUpperCase());
        System.out.println("[m] repeat(3)                 = " + s.repeat(3));

        String padded = "   hi   ";
        System.out.println("[m] \"   hi   \".trim()         = \"" + padded.trim() + "\"");
        System.out.println("[m] \"   hi   \".strip()        = \"" + padded.strip() + "\"");

        System.out.println("[m] replace(\"ity\", \"XYZ\")    = " + s.replace("ity", "XYZ"));
        System.out.println("[m] replaceAll(\"i\", \"*\")     = " + s.replaceAll("i", "*")
                + "   (regex)");

        // split / join
        String csv = "Aditya-Rohit-Rohan";
        System.out.println("[m] csv.split(\"-\")           = " + java.util.Arrays.toString(csv.split("-")));
        System.out.println("[m] String.join(\"-\",a,b,c)    = " + String.join("-", "a", "b", "c"));

        // conversion
        System.out.println("[m] String.valueOf(10)        = " + String.valueOf(10));
        System.out.println("[m] s.getBytes()              = " + java.util.Arrays.toString(s.getBytes()));
    }

    // =================================================================
    // 3. Formatting  (was Demo2.java, the uncommented tail)
    // =================================================================

    static void formatting() {
        String name = "Aditya";
        int age = 28;

        System.out.println("[fmt] concat = " + "Hello " + name + ", your age is " + age);
        System.out.println("[fmt] format = " + String.format("Hello %s, your age is %d", name, age));
        System.out.printf("[fmt] printf = Hello %s, your age is %d%n", name, age);
    }

    // =================================================================
    // Runner
    // =================================================================

    public static void main(String[] args) {
        String mode = (args.length == 0) ? "all" : args[0];
        System.out.println("=== String part II.1 - constructors & methods ===\n");
        switch (mode) {
            case "all"          -> { constructors(); methods(); formatting(); }
            case "constructors" -> constructors();
            case "methods"      -> methods();
            case "format"       -> formatting();
            default             -> System.out.println(
                    "usage: all | constructors | methods | format");
        }
    }
}
