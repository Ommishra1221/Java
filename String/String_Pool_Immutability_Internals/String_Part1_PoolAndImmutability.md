# Java Strings (Part 1) — **String Pool, Identity, Constant Folding, `intern()` & Immutability**

> **Source material:** the 2 runnable files in this folder — [`String01_PoolAndIdentity.java`](String01_PoolAndIdentity.java) and [`String02_ImmutabilityAndCost.java`](String02_ImmutabilityAndCost.java) — plus the handwritten pages in [`notes.pdf`](notes.pdf).
> **Lecture:** *String Pool, Immutability, Internals | Java Full Course* **#25** (Coder Army). <https://youtu.be/N0b8lRXtK_Y>
> **Scope:** how a String is stored (pool vs heap), what `==` really compares, compile-time **constant folding** vs runtime concatenation, reference copying and reassignment, `intern()`, and why **immutability** turns `s += …` in a loop into an `O(n²)` trap.
> **File layout:** the original scratch files (`Demo.java`, `Demo2.java`, `Demo3.java`) were replaced by **exactly two** programs — `String01_…` (pool & identity, 4 demos) and `String02_…` (immutability & cost, 3 demos) — so the whole part compiles as one package.
> **Part 2** lives in the sibling folder `../String_Methods_And_Builders/` (constructors, the method tour, `StringBuilder`/`StringBuffer`).
> **Verification footprint:** every output, exit code, bytecode listing and compiler-error message printed here was reproduced with **`javac`/`java` 22.0.1**. The exact commands are in [Appendix B](#appendix-b--how-this-note-was-verified).

---

## 0. How to use this note

### 0.1 Program map (original demo ➜ new home)

| Original | New home | Demo | Concept it teaches |
|----------|----------|------|--------------------|
| `Demo.java` | **`String01_PoolAndIdentity`** | `pool()` | literals share the pool; `new` makes fresh heap objects |
| `Demo2.java` | `String01_PoolAndIdentity` | `folding()`, `alias()` | constant folding; reference copy & reassignment; `intern()` |
| `Demo3.java` | **`String02_ImmutabilityAndCost`** | `loopBuild()` | the cost of immutability: a new object every `+=` |

### 0.2 Compile and run everything

```bash
cd String/String_Pool_Immutability_Internals

# compile BOTH files together (they live in the default package)
javac -Xlint:all -d out *.java          # clean: no errors, no warnings

# file 1 - pool & identity
java -cp out String01_PoolAndIdentity            # all four
java -cp out String01_PoolAndIdentity pool
java -cp out String01_PoolAndIdentity folding
java -cp out String01_PoolAndIdentity alias
java -cp out String01_PoolAndIdentity intern

# file 2 - immutability & cost
java -cp out String02_ImmutabilityAndCost        # all three
java -cp out String02_ImmutabilityAndCost loop
java -cp out String02_ImmutabilityAndCost proof
java -cp out String02_ImmutabilityAndCost cost
```

**Legend**

| Marker | Meaning |
|--------|---------|
| ✅ | Valid code — compiles and runs |
| 💥 `INTENTIONAL COMPILE ERROR` | The compiler rejects it; the *rejection* is the lesson |
| 💥 `INTENTIONAL RUNTIME ERROR` | Compiles, but must crash at runtime |
| 📏 | A machine/JVM-dependent number (identity hash, timing) — *direction* is the lesson |

---

## 1. The whole lecture on one page

Two strings can hold the **same text** and still be **different objects**. Where the object lives decides which one you get.

```mermaid
flowchart TB
    LIT["\"Hello\"  (a literal)"] --> POOL["String POOL<br/>one shared object"]
    NEW["new String(\"Hello\")"] --> HEAP["HEAP<br/>a brand-new object"]
    LIT -.->|"compile-time concat folds into a literal"| POOL
    VAR["\"Ja\" + variable"] -->|"built at runtime"| HEAP
    POOL --> EQ{"== ?"}
    HEAP --> EQ
    EQ -->|"same object"| T["true"]
    EQ -->|"different object"| F["false"]
    POOL --> INT["intern() returns the pooled object"]
    HEAP --> INT
```

**The one sentence that matters:** `==` compares **identities**, `.equals()` compares **text**; a literal is pooled and reused, `new String(...)` is not. And because a String is **immutable**, every "modifying" call returns a new object.

| Expression | Where it lives | `==` its literal twin? |
|------------|----------------|------------------------|
| `"Hello"` | String pool (shared) | `true` |
| `"Ja" + "va"` | folded to the pooled `"Java"` | `true` |
| `"Ja" + variable` | heap (built at runtime) | `false` |
| `new String("Hello")` | heap | `false` |
| `heap.intern()` | the pool object | `true` |

---

## 2. Strings in one paragraph

A `String` is an **immutable object**, final class, backed by a byte array. Because it can never change, identical literals can safely **share one instance** in a pool, and hashing/caching is cheap. The pool is populated by literals and by `intern()`; `new String(...)` deliberately bypasses it to guarantee a fresh object. The compiler **folds** concatenations of constants, so `"Ja" + "va"` *is* the pooled `"Java"`, but any variable in the expression forces a runtime build. Immutability means `s += x`, `substring`, `toUpperCase` etc. all allocate new objects — cheap once, catastrophic in a loop.

---

## 3. The pool vs the heap  (was `Demo.java`)

### 3.1 Walkthrough — `pool()`

```java
String literal1 = "Hello";            // pooled
String literal2 = "Hello";            // the SAME pooled object
String heap1 = new String("Hello");   // a fresh heap object
String heap2 = new String("Hello");   // another one
```

```text
[pool] literal1 == literal2  : true
[pool] heap1    == heap2      : false
[pool] literal1 == heap1      : false
[pool] literal1.equals(heap1) : true   // same text, different object
[pool] identity hashes        : literal1=2001049719 literal2=2001049719  (same number -> same object)   ← 📏
[pool]                            heap1=303563356  (a different object)                                   ← 📏
```

The two literals print the **same identity hash** — one object, two names. Each `new String` gets its own.

### 3.2 Proved by bytecode

A literal compiles to `ldc` (load a pooled constant); `new String(...)` compiles to an actual allocation:

```text
       0: ldc           #7                  // String Hello        (literal -> pool)
       3: ldc           #7                  // String Hello        (same constant)
       6: new           #9                  // class java/lang/String
      10: ldc           #7                  // String Hello        (the argument)
      12: invokespecial #11                 // Method java/lang/String."<init>":(Ljava/lang/String;)V
```

## 4. Constant folding vs runtime concatenation  (was `Demo2.java`)

### 4.1 Walkthrough — `folding()`

```java
String folded  = "Ja" + "va";     // the compiler folds this to "Java"
String suffix  = "va";
String runtime = "Ja" + suffix;   // a variable forces a runtime build
```

```text
[folding] ("Ja" + "va")  == "Java" : true   // folded to the pooled literal
[folding] ("Ja" + suffix) == "Java" : false   // built at runtime -> new object
[folding] runtime.equals(literal)     : true   // content is equal either way
```

### 4.2 Proved by bytecode

Folding leaves **no concatenation at all** — just one `ldc "Java"`; the runtime form emits `invokedynamic makeConcatWithConstants`:

```text
       0: ldc           #46                 // String Java          <- folded constant
       3: ldc           #46                 // String Java
       6: ldc           #48                 // String va
      10: invokedynamic #50,  0             // InvokeDynamic #6:makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;
```

So the *same-looking* expression produces different objects depending only on whether every operand was a compile-time constant.

## 5. Reference copy and reassignment  (was `Demo2.java`)

```java
String a = "Hello";
String b = a;      // copies the REFERENCE (both names -> one object)
b = "World";       // REPOINTS b; the "Hello" object is untouched
```

```text
[alias] b = a; then a == b : true   // both names point at one pooled object
[alias] after b = "World": a = Hello, b = World
[alias] a == b now         : false
[alias] a is still the old text -> String is immutable
```

Assigning to a String variable **repoints the name**; it never edits the object. This is why "changing" a String always produces a new one.

## 6. `intern()` — pulling a heap String back into the pool

```java
String heap = new String("Hello");   // not the pooled object
String pooled = "Hello";             // the pooled object
String interned = heap.intern();     // hand me the pool's copy
```

```text
[intern] heap     == pooled   : false
[intern] interned == pooled   : true
[intern] heap     == interned : false
[intern] all three .equals() each other : true
```

`intern()` returns the **unique pooled** String equal to the receiver — useful when you have millions of repeated strings and want one canonical instance (a common memory trick, though it still risks bloating the pool).

---

## 7. Immutability — nothing mutates, everything copies  (was `Demo3.java` context)

### 7.1 Walkthrough — `immutabilityProof()`

```text
[proof] original           = Hello
[proof] toUpperCase()      = HELLO
[proof] substring(0, 2)    = He
[proof] original + "!"     = Hello!
[proof] replace('l', 'L')  = HeLLo
[proof] original afterwards= Hello   <- unchanged
[proof] every result was a NEW object : true
```

Call `toUpperCase`, `substring`, `replace`, `+` — the original is **always** `Hello`. That guarantee is what lets strings be shared and hashed safely.

### 7.2 Why `String` cannot be extended

```java
public class S1_FinalClass extends String { }
```

```text
S1_FinalClass.java:1: error: cannot inherit from final String
```

`String` is `final` — no subclass can break the immutability contract.

## 8. The cost of immutability: `s += x` in a loop  (was `Demo3.java`)

### 8.1 Walkthrough — `loopBuild()`

```java
String s = "";
for (int i = 0; i < 5; i++) {
    String before = s;
    s += i;                     // s = s + i; a NEW String every time
    ...
}
```

```text
[loop] s = "0"   new object this step? true
[loop] s = "01"   new object this step? true
[loop] s = "012"   new object this step? true
[loop] s = "0123"   new object this step? true
[loop] s = "01234"   new object this step? true
[loop] the pieces "0", "01", "012", ... accumulate as separate objects
```

Every step allocates a new String **and copies the whole text built so far**. Over `n` steps that is `1 + 2 + … + n = n(n+1)/2` character copies — **O(n²)**.

### 8.2 Measured — `cost()`

Building 20 000 characters two ways (📏 — times vary by machine/JIT; the *ratio* is the lesson):

```text
[cost] +=             built 20,000 chars in   90.922 ms   (quadratic)
[cost] StringBuilder  built 20,000 chars in    1.226 ms   (linear)
[cost] identical text: true
```

Bytecode confirms the two paths: `s += "x"` is `invokedynamic makeConcatWithConstants` (allocates a result each time), while the builder is `new StringBuilder` + `invokevirtual StringBuilder.append` + `toString()`:

```text
      14: invokedynamic #9,  0    // InvokeDynamic #0:makeConcatWithConstants:(Ljava/lang/String;I)Ljava/lang/String;
      ...
      38: new           #63     // class java/lang/StringBuilder
      42: invokespecial #65     // Method java/lang/StringBuilder."<init>":()V
      60: invokevirtual #68     // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
      72: invokevirtual #72     // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
```

> ✅ **Rule:** build long text in a loop with **`StringBuilder`** (Part 2), never with `+=`.

## 9. Compile-time and runtime errors you will actually hit

All reproduced on JDK 22.0.1; text is verbatim.

| # | Wrong code | Java says |
|---|-----------|-----------|
| S1 | `class X extends String {}` | 💥 `error: cannot inherit from final String` |
| S2 | `"x".append("y")` | 💥 `error: cannot find symbol` / `method append(String)` / `location: variable s of type String` |
| S3 | `s[0]` on a String | 💥 `error: array required, but String found` |
| S4 | `"Aditya".charAt(100)` | 💥 runtime `StringIndexOutOfBoundsException: Index 100 out of bounds for length 6` |
| S5 | `"Aditya".substring(3, 1)` | 💥 runtime `StringIndexOutOfBoundsException: Range [3, 1) out of bounds for length 6` |
| S6 | mutate a `char[]` after `new String(arr)` | ✅ no error — the constructor **copied** the array: `after mutating arr[0], s = Hi` |

## 10. Common mistakes

1. **Comparing text with `==`.** Use `.equals()`. `new String("a") == "a"` is `false`.
2. **Thinking `s += x` mutates.** It allocates; in a loop it is O(n²).
3. **Assuming a `char[]` passed to `new String` stays linked.** The array is copied at construction (S6).
4. **Off-by-one on `substring`.** The end index is **exclusive**: `"Aditya".substring(0, 2)` is `"Ad"`.
5. **Over-`intern`-ing.** `intern()` fills a global table; interning unbounded/dynamic strings can leak memory.
6. **Using `==` on interned results by accident** and it "works" — then breaks with non-interned input.

## 11. Interview Q&A

**Q1. Why does `"a" == "a"` print true but `new String("a") == "a"` false?** Both literals refer to the same pooled object; `new` creates a distinct heap object.

**Q2. Is `"Ja" + "va" == "Java"` true?** Yes — constant folding turns it into the single pooled literal `"Java"`.

**Q3. Why are strings immutable?** Safety (shareable across threads), cacheability of `hashCode`, and cheap pooling. The price: every change allocates.

**Q4. What does `intern()` do?** Returns the canonical pooled String with the same contents.

**Q5. Where does the String pool live?** On the **heap** (since Java 7); the pool itself is a table of unique strings.

**Q6. Why is String `final`?** So no subclass can violate immutability (and so `String` switches and pooling stay sound).

**Q7. `==` vs `.equals()` in one line?** `==` compares references; `.equals()` compares contents.

**Q8. Can a String be added to itself in a loop efficiently?** No — use `StringBuilder`; `+=` in a loop is quadratic.

---

## 12. Cheat sheet

| Task | Use |
|------|-----|
| compare text | `a.equals(b)` |
| compare identity (rare) | `a == b` |
| canonicalize a string | `s.intern()` |
| build text in a loop | `StringBuilder` (Part 2) |
| literal vs forced copy | `"x"` vs `new String("x")` |
| safe comparisons | `Objects.equals(a, b)` for nullable |

### 12.1 The whole part in six lines

1. Literals live in the **pool** and are shared; `new String(...)` forces a fresh heap object.
2. `==` compares identity; `.equals()` compares text.
3. The compiler **folds** constant concatenations; a variable forces a runtime build.
4. `b = a` copies the **reference**; assigning to a String variable repoints the name, never edits the object.
5. `intern()` returns the pooled copy.
6. Strings are **immutable**, so `+=` in a loop is O(n²) — use `StringBuilder`.

## 13. Ten-minute revision checklist

- [ ] I can explain why `"Hello" == "Hello"` but `new String("Hello") != "Hello"`.
- [ ] I can say what `ldc` vs `new …String` means in bytecode.
- [ ] I can predict `"Ja" + "va" == "Java"` and `"Ja" + suffix == "Java"`.
- [ ] I can explain reference copy vs object copy.
- [ ] I can describe what `intern()` returns.
- [ ] I can prove `String` is immutable with one method call.
- [ ] I can explain why `+=` in a loop is O(n²) and what to use instead.
- [ ] I know `substring`'s end index is exclusive.
- [ ] I know `class X extends String` is a compile error.

---

## Appendix A — verified outputs (JDK 22.0.1)

| Command | Result | Exit |
|---------|--------|------|
| `javac -Xlint:all -d out *.java` | 0 errors, **0 warnings** | 0 |
| `java -cp out String01_PoolAndIdentity` | the `[pool]`/`[folding]`/`[alias]`/`[intern]` transcript in §3–§6 | 0 |
| `java -cp out String01_PoolAndIdentity <mode>` | `pool`, `folding`, `alias`, `intern` — all clean | 0 |
| `java -cp out String02_ImmutabilityAndCost` | the `[loop]`/`[proof]`/`[cost]` transcript in §7–§8 | 0 |
| `java -cp out String02_ImmutabilityAndCost <mode>` | `loop`, `proof`, `cost` — all clean | 0 |
| `javap -c -p` | `ldc` literals, folded `ldc "Java"`, `new`+`invokespecial`, `invokedynamic makeConcatWithConstants`, `StringBuilder.append` | — |
| probes S1–S3 | the exact compile errors in §9 | 1 💥 |
| probes S4–S5 | `StringIndexOutOfBoundsException` (Index / Range) | 1 💥 |
| probe S6 | `after mutating arr[0], s = Hi` (constructor copied the array) | 0 |

## Appendix B — how this note was verified

```bash
cd String/String_Pool_Immutability_Internals
javac -Xlint:all -d out *.java        # both files: 0 errors, 0 warnings
java  -cp out <ClassName> [mode]      # each entry point / mode run individually
```

* **Transcripts** → real `java` stdout, `\r` stripped; every exit code recorded.
* **Pool/heap & folding claims** (§3–§4) → `javap -c -p` (`ldc` vs `new`/`invokespecial`; folded `ldc "Java"` vs `invokedynamic`).
* **Immutability & cost** (§7–§8) → run output plus the `invokedynamic` / `StringBuilder` bytecode.
* **Error text** (§9) → standalone probes, quoted verbatim.
* **Version** → `javac`/`java` 22.0.1 (build `22.0.1+8-16`, HotSpot 64-Bit).
* Identity hashes and timings are machine-dependent (📏).
