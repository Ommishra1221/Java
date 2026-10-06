# Java Full Course — Lecture Notes & Runnable Demos

A study repo for the **Coder Army "Java Full Course"**. Each lecture lives in **its own flat folder** and has the exact same shape:

```
<Lecture_Folder>/
├── <Topic>NN_*.java     # 1–2 runnable demo files (default package)
├── <Lecture_Folder>.md  # the consolidated, verified note
└── notes.pdf            # the handwritten source pages
```

Every claim in a note (outputs, exit codes, bytecode, compiler errors) was reproduced with **`javac`/`java` 22.0.1**. Compile and run any folder with:

```bash
cd <Lecture_Folder>
javac -Xlint:all -d out *.java      # 0 errors, 0 warnings
java  -cp out <MainClass> [mode]
```

---

## Index

| Lecture | Topic | Folder | Note | Video |
|---------|-------|--------|------|-------|
| **#24** | Interfaces — default/static/private methods, diamonds, functional & marker interfaces | [`Interface/`](Interface/) | [`Interface.md`](Interface/Interface.md) | <https://youtu.be/YLLFHuStkW8> |
| **#25** (Part 1) | Strings — pool, identity, constant folding, `intern()`, immutability | [`String_01_Pool_Immutability_Internals/`](String_01_Pool_Immutability_Internals/) | [`…Internals.md`](String_01_Pool_Immutability_Internals/String_01_Pool_Immutability_Internals.md) | <https://youtu.be/N0b8lRXtK_Y> |
| **#25** (Part 2) | Strings — constructors, method tour, `StringBuilder` vs `StringBuffer` | [`String_02_Methods_And_Builders/`](String_02_Methods_And_Builders/) | [`…Builders.md`](String_02_Methods_And_Builders/String_02_Methods_And_Builders.md) | <https://youtu.be/JcAD9a22Rks> |
| **#27** | Generics — type safety, casting, generic classes/methods, bounded types | [`Generics_01_Type_Safety_And_Bounded_Types/`](Generics_01_Type_Safety_And_Bounded_Types/) | [`…Bounded_Types.md`](Generics_01_Type_Safety_And_Bounded_Types/Generics_01_Type_Safety_And_Bounded_Types.md) | Coder Army |
| **#28** | Generics — invariance, array covariance, wildcards (`?`, `? extends`, `? super`) | [`Generics_02_Variance_And_Wildcards/`](Generics_02_Variance_And_Wildcards/) | [`…Wildcards.md`](Generics_02_Variance_And_Wildcards/Generics_02_Variance_And_Wildcards.md) | Coder Army |
| **#45** | Memory management — runtime data areas, heap, GC, `OutOfMemoryError` | [`Memory_Management/`](Memory_Management/) | [`Memory_Management.md`](Memory_Management/Memory_Management.md) | <https://youtu.be/kjETbH63Pco> |

---

## Conventions

- **One folder per lecture.** A topic split across two videos gets two folders (`…_01_…`, `…_02_…`).
- **Uniform leaf contract.** Every lecture folder contains `*.java` + exactly one `<Folder>.md` note + `notes.pdf`, side by side. No `notes/` subfolder, no separate notes directory.
- **Default package.** All `.java` in a folder compile together (helper type names are made unique per folder).
- **Verified notes.** Notes quote real program output; intentional failures are marked 💥 and machine-dependent numbers 📏.
- **Cross-links** between companion lectures are relative (`../Other_Folder/Other_Folder.md`).

## Legend used inside the notes

| Marker | Meaning |
|--------|---------|
| ✅ | valid code — compiles and runs |
| 💥 | intentional compile/runtime error; the failure *is* the lesson |
| 📏 | machine/JVM-dependent value (timing, identity hash, thread race) |
