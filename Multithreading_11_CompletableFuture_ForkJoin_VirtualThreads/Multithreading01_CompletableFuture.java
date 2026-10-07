import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lecture #57 - CompletableFuture, Fork-Join Pool, ThreadLocal & Virtual Threads
 *              (part 1: CompletableFuture - the asynchronous pipeline)
 *
 * Modes (pass as args[0]):
 *   runAndSupply  - runAsync (no value) vs supplyAsync (value)
 *   chaining      - thenApply -> thenAccept -> thenRun, and that stages are immutable
 *   compose       - thenApply (map, nests) vs thenCompose (flatMap, flattens)
 *   combine       - thenCombine two independent futures, and its speed-up
 *   errors        - exceptionally / whenComplete / handle
 *   getVsJoin     - get() throws checked, join() throws unchecked
 *   asyncVsSync   - thenApply vs thenApplyAsync: which thread runs the stage?
 *   allAny        - allOf / anyOf
 *   all           - runs every mode (default)
 */
public class Multithreading01_CompletableFuture {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "runAndSupply": runAndSupply(); break;
            case "chaining":     chaining();     break;
            case "compose":      compose();      break;
            case "combine":      combine();      break;
            case "errors":       errors();       break;
            case "getVsJoin":    getVsJoin();    break;
            case "asyncVsSync":  asyncVsSync();  break;
            case "allAny":       allAny();       break;
            case "all": runAndSupply(); chaining(); compose(); combine();
                        errors(); getVsJoin(); asyncVsSync(); allAny(); break;
            default:
                System.out.println("usage: java -cp out Multithreading01_CompletableFuture "
                        + "[runAndSupply|chaining|compose|combine|errors|getVsJoin|asyncVsSync|allAny|all]");
        }
    }

    private static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // --------------------------------------------------------- runAndSupply
    private static void runAndSupply() {
        System.out.println("=== runAsync vs supplyAsync ===");

        CompletableFuture<Void> noValue = CompletableFuture.runAsync(() ->
                System.out.println("    runAsync    on " + Thread.currentThread().getName()
                        + "  (no value)"));
        noValue.join();

        CompletableFuture<Integer> withValue = CompletableFuture.supplyAsync(() -> {
            System.out.println("    supplyAsync on " + Thread.currentThread().getName()
                    + "  (returns a value)");
            return 40 + 2;
        });
        System.out.println("  supplyAsync result = " + withValue.join());
        System.out.println("  no executor given, so both used ForkJoinPool.commonPool() - see the thread name.");
    }

    // ------------------------------------------------------------- chaining
    private static void chaining() {
        System.out.println();
        System.out.println("=== chaining: thenApply -> thenAccept -> thenRun ===");

        CompletableFuture<Integer> original = CompletableFuture.supplyAsync(() -> 5);

        CompletableFuture<Integer> transformed = original.thenApply(n -> n * 2);            // returns a value
        CompletableFuture<Void> consumed = transformed.thenAccept(v ->                       // returns void
                System.out.println("    thenAccept got " + v));
        CompletableFuture<Void> finished = consumed.thenRun(() ->                            // no input, no output
                System.out.println("    thenRun: pipeline finished"));

        finished.join();
        System.out.println("  the original future is unchanged: " + original.join());
        System.out.println("  every stage returns a NEW CompletableFuture - the chain IS the pipeline.");
    }

    // -------------------------------------------------------------- compose
    private static void compose() {
        System.out.println();
        System.out.println("=== thenCompose (flatMap) vs thenApply (map) ===");

        CompletableFuture<CompletableFuture<Integer>> nested =
                CompletableFuture.supplyAsync(() -> "user-42")
                        .thenApply(id -> CompletableFuture.supplyAsync(() -> id.length()));
        System.out.println("  thenApply   -> a future of a future; needs two joins: " + nested.join().join());

        CompletableFuture<Integer> flat =
                CompletableFuture.supplyAsync(() -> "user-42")
                        .thenCompose(id -> CompletableFuture.supplyAsync(() -> id.length()));
        System.out.println("  thenCompose -> one future, one join:                 " + flat.join());
        System.out.println("  use thenCompose when the next step is ITSELF asynchronous.");
    }

    // -------------------------------------------------------------- combine
    private static void combine() {
        System.out.println();
        System.out.println("=== thenCombine: two independent futures ===");

        long t0 = System.nanoTime();
        CompletableFuture<Integer> price = CompletableFuture.supplyAsync(() -> { nap(150); return 100; });
        CompletableFuture<Integer> tax = CompletableFuture.supplyAsync(() -> { nap(150); return 18; });
        int total = price.thenCombine(tax, Integer::sum).join();
        long combineMs = (System.nanoTime() - t0) / 1_000_000;

        long t1 = System.nanoTime();
        int p = 100; nap(150);
        int q = 18;  nap(150);
        long sequentialMs = (System.nanoTime() - t1) / 1_000_000;

        System.out.printf("  thenCombine    total = %d  in %d ms  (both ran at the same time)%n", total, combineMs);
        System.out.printf("  same work done sequentially    in %d ms%n", sequentialMs);
        System.out.println("  thenCombine does not START anything - it just merges two futures you already have.");
    }

    // --------------------------------------------------------------- errors
    private static void errors() {
        System.out.println();
        System.out.println("=== errors: exceptionally / whenComplete / handle ===");

        // 1. exceptionally - recover and produce a fallback value
        CompletableFuture<Integer> recovered =
                CompletableFuture.<Integer>supplyAsync(() -> { throw new IllegalStateException("boom"); })
                        .exceptionally(ex -> {
                            System.out.println("    exceptionally saw " + ex.getClass().getSimpleName()
                                    + " cause=" + ex.getCause().getClass().getSimpleName());
                            return -1;
                        });
        System.out.println("  exceptionally -> " + recovered.join() + "  (pipeline recovered)");

        // 2. whenComplete - observe the outcome, then let the failure continue
        CompletableFuture<Integer> observed =
                CompletableFuture.<Integer>supplyAsync(() -> { throw new IllegalStateException("boom2"); })
                        .whenComplete((res, ex) -> System.out.println("    whenComplete: res=" + res
                                + " ex=" + (ex == null ? "null" : ex.getClass().getSimpleName())));
        try {
            observed.join();
        } catch (CompletionException e) {
            System.out.println("  whenComplete re-threw -> " + e.getClass().getSimpleName()
                    + " cause=" + e.getCause().getClass().getSimpleName());
        }

        // 3. handle - runs for BOTH outcomes and can change the value
        CompletableFuture<Integer> handledFailure =
                CompletableFuture.<Integer>supplyAsync(() -> { throw new IllegalStateException("boom3"); })
                        .handle((res, ex) -> ex != null ? -1 : res + 1);
        CompletableFuture<Integer> handledSuccess =
                CompletableFuture.supplyAsync(() -> 10)
                        .handle((res, ex) -> ex != null ? -1 : res + 1);
        System.out.println("  handle(failure) -> " + handledFailure.join());
        System.out.println("  handle(success) -> " + handledSuccess.join());
    }

    // ------------------------------------------------------------ getVsJoin
    private static void getVsJoin() throws Exception {
        System.out.println();
        System.out.println("=== get() vs join() ===");

        CompletableFuture<Integer> ok = CompletableFuture.completedFuture(7);
        System.out.println("  completed future: get() = " + ok.get() + ", join() = " + ok.join());

        CompletableFuture<Integer> failed =
                CompletableFuture.supplyAsync(() -> { throw new IllegalStateException("nope"); });

        try {
            failed.join();
        } catch (CompletionException e) {
            System.out.println("  join() -> UNCHECKED " + e.getClass().getSimpleName()
                    + ", cause = " + e.getCause().getClass().getSimpleName());
        }
        try {
            failed.get();
        } catch (ExecutionException e) {
            System.out.println("  get()  -> CHECKED   " + e.getClass().getSimpleName()
                    + ", cause = " + e.getCause().getClass().getSimpleName());
        }
        System.out.println("  get() forces you to handle InterruptedException/ExecutionException; join() does not.");
    }

    // ---------------------------------------------------------- asyncVsSync
    private static void asyncVsSync() {
        System.out.println();
        System.out.println("=== thenApply vs thenApplyAsync: which thread runs the stage? ===");
        System.out.println("  main thread = " + Thread.currentThread().getName());

        CompletableFuture<Integer> already = CompletableFuture.completedFuture(21);

        AtomicReference<String> syncThread = new AtomicReference<>();
        int syncValue = already
                .thenApply(v -> { syncThread.set(Thread.currentThread().getName()); return v * 2; })
                .join();
        System.out.println("  thenApply      ran on " + syncThread.get() + "  -> " + syncValue);

        AtomicReference<String> asyncThread = new AtomicReference<>();
        int asyncValue = already
                .thenApplyAsync(v -> { asyncThread.set(Thread.currentThread().getName()); return v * 2; })
                .join();
        System.out.println("  thenApplyAsync ran on " + asyncThread.get() + "  -> " + asyncValue);
        System.out.println("  ...Async hands the stage to the common pool; the plain form may run it on the caller.");
    }

    // ---------------------------------------------------------------- allAny
    private static void allAny() {
        System.out.println();
        System.out.println("=== allOf / anyOf ===");

        long t0 = System.nanoTime();
        CompletableFuture<String> a = CompletableFuture.supplyAsync(() -> { nap(120); return "A"; });
        CompletableFuture<String> b = CompletableFuture.supplyAsync(() -> { nap(60);  return "B"; });
        CompletableFuture<String> c = CompletableFuture.supplyAsync(() -> { nap(200); return "C"; });
        CompletableFuture.allOf(a, b, c).join();
        System.out.printf("  allOf waited for all three in %d ms -> %s%s%s%n",
                (System.nanoTime() - t0) / 1_000_000, a.join(), b.join(), c.join());

        long t1 = System.nanoTime();
        CompletableFuture<Object> fast = CompletableFuture.supplyAsync(() -> { nap(40);  return "fast"; });
        CompletableFuture<Object> slow = CompletableFuture.supplyAsync(() -> { nap(300); return "slow"; });
        Object winner = CompletableFuture.anyOf(fast, slow).join();
        System.out.printf("  anyOf returned '%s' in %d ms - the other one keeps running%n",
                winner, (System.nanoTime() - t1) / 1_000_000);
        slow.join();
        System.out.println("  allOf returns CompletableFuture<Void>; you read the values from the original futures.");
    }
}
