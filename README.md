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
| **#47** | Multithreading 1 — process vs thread, shared memory, concurrency, race conditions | [`Multithreading_01_Thread_And_Process_Basics/`](Multithreading_01_Thread_And_Process_Basics/) | [`…Process_Basics.md`](Multithreading_01_Thread_And_Process_Basics/Multithreading_01_Thread_And_Process_Basics.md) | <https://youtu.be/fyAW0W526RM> |
| **#48** | Multithreading 2 — thread creation (`Thread`/`Runnable`/lambda) & the lifecycle states | [`Multithreading_02_Thread_Creation_And_Lifecycle/`](Multithreading_02_Thread_Creation_And_Lifecycle/) | [`…Lifecycle.md`](Multithreading_02_Thread_Creation_And_Lifecycle/Multithreading_02_Thread_Creation_And_Lifecycle.md) | <https://youtu.be/cVRdeQFP5IM> |
| **#49** | Multithreading 3 — `sleep`, `join`, `yield`, `interrupt`, `isAlive`, priority & daemon | [`Multithreading_03_Thread_Methods_And_Control/`](Multithreading_03_Thread_Methods_And_Control/) | [`…Control.md`](Multithreading_03_Thread_Methods_And_Control/Multithreading_03_Thread_Methods_And_Control.md) | <https://youtu.be/ZPxJby0GeOQ> |
| **#50** | Multithreading 4 — race conditions, visibility, ordering, deadlock | [`Multithreading_04_Concurrency_Problems/`](Multithreading_04_Concurrency_Problems/) | [`…Problems.md`](Multithreading_04_Concurrency_Problems/Multithreading_04_Concurrency_Problems.md) | <https://youtu.be/lrrdN_c0HQ4> |
| **#51** | Multithreading 5 — monitors & synchronization, static sync, custom locks | [`Multithreading_05_Synchronization_And_Monitors/`](Multithreading_05_Synchronization_And_Monitors/) | [`…Monitors.md`](Multithreading_05_Synchronization_And_Monitors/Multithreading_05_Synchronization_And_Monitors.md) | <https://youtu.be/k9sURAu0xT8> |
| **#52** | Multithreading 6 — inter-thread communication, `wait()`, `notify()`, `notifyAll()` | [`Multithreading_06_Inter_Thread_Communication/`](Multithreading_06_Inter_Thread_Communication/) | [`…Communication.md`](Multithreading_06_Inter_Thread_Communication/Multithreading_06_Inter_Thread_Communication.md) | <https://youtu.be/EZS19NLnsvc> |
| **#53** | Multithreading 7 — `ReentrantLock`, `ReadWriteLock`, `StampedLock`, `Semaphore`, `Condition` | [`Multithreading_07_Advanced_Locking/`](Multithreading_07_Advanced_Locking/) | [`…Locking.md`](Multithreading_07_Advanced_Locking/Multithreading_07_Advanced_Locking.md) | <https://youtu.be/JW6-TCU0iS4> |
| **#54** | Multithreading 8 — atomic variables & lock-free updates (`AtomicInteger`, `AtomicReference`, `LongAdder`) | [`Multithreading_08_Atomic_Variables_And_Lock_Free/`](Multithreading_08_Atomic_Variables_And_Lock_Free/) | [`…Lock_Free.md`](Multithreading_08_Atomic_Variables_And_Lock_Free/Multithreading_08_Atomic_Variables_And_Lock_Free.md) | <https://youtu.be/ujF2gNsCfBE> |
| **#55** | Multithreading 9 — CAS retry & the ABA problem (`AtomicStampedReference`) | [`Multithreading_09_CAS_And_ABA_Problem/`](Multithreading_09_CAS_And_ABA_Problem/) | [`…ABA_Problem.md`](Multithreading_09_CAS_And_ABA_Problem/Multithreading_09_CAS_And_ABA_Problem.md) | <https://youtu.be/2nBJPpERul4> |
| **#56** | Multithreading 10 — Executor framework: thread pools, `Callable` & `Future`, rejection, shutdown | [`Multithreading_10_Executor_Framework/`](Multithreading_10_Executor_Framework/) | [`…Executor_Framework.md`](Multithreading_10_Executor_Framework/Multithreading_10_Executor_Framework.md) | <https://youtu.be/VPtaTUSaBOM> |
| **#57** | Multithreading 11 — `CompletableFuture`, `ForkJoinPool`, `ThreadLocal` & virtual threads | [`Multithreading_11_CompletableFuture_ForkJoin_VirtualThreads/`](Multithreading_11_CompletableFuture_ForkJoin_VirtualThreads/) | [`…VirtualThreads.md`](Multithreading_11_CompletableFuture_ForkJoin_VirtualThreads/Multithreading_11_CompletableFuture_ForkJoin_VirtualThreads.md) | <https://youtu.be/FGN225TiXaE> |

---

## Revision notes

| Collection | File | Covers |
|------------|------|--------|
| Multithreading — interview revision | [`Revision Notes/Multithreading_Interview_Revision.md`](Revision%20Notes/Multithreading_Interview_Revision.md) | all of Parts **#47–#57** in one digest: the one-page mental model, per-topic cheat tables, rapid-fire traps, and an interview Q&A section |

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
