import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lecture #52 - Inter Thread Communication in Java | wait(), notify(), notifyAll() Deep Dive
 *              (part 1: the two BROKEN producer/consumer designs)
 *
 * Modes (pass as args[0]):
 *   noSync           - no synchronization at all: the consumer reads nothing / stale values
 *   busyWaitDeadlock - a busy-wait INSIDE a synchronized method -> deadlock
 *   all              - runs both modes (default)
 *
 * Run:
 *     javac -Xlint:all -d out *.java
 *     java -cp out Multithreading01_ProducerConsumerBroken all
 */
public class Multithreading01_ProducerConsumerBroken {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        switch (mode) {
            case "noSync":           noSync();           break;
            case "busyWaitDeadlock": busyWaitDeadlock(); break;
            case "all":              noSync(); busyWaitDeadlock(); break;
            default:
                System.out.println("usage: java -cp out Multithreading01_ProducerConsumerBroken "
                        + "[noSync|busyWaitDeadlock|all]");
        }
    }

    static void nap(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ------------------------------------------------------------- noSync
    static class UnsyncBox {
        Integer item;                                  // nobody synchronizes anything

        void producer(int value) {
            item = value;
            System.out.println("    producer produces " + value);
        }

        Integer consumer() {
            Integer got = item;
            System.out.println("    consumer consumes " + got);
            item = null;
            return got;
        }
    }

    private static void noSync() throws InterruptedException {
        System.out.println("=== noSync: producer and consumer, no synchronization ===");

        final int rounds = 8;
        UnsyncBox box = new UnsyncBox();
        List<Integer> seen = Collections.synchronizedList(new ArrayList<>());

        // producer is SLOWER (100 ms) than the consumer (70 ms) so the consumer outruns it
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= rounds; i++) { nap(100); box.producer(i); }
        }, "producer");

        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= rounds; i++) { nap(70); seen.add(box.consumer()); }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        int nulls = 0, duplicates = 0;
        Integer previous = null;
        for (Integer v : seen) {
            if (v == null) nulls++;
            if (v != null && v.equals(previous)) duplicates++;
            previous = v;
        }
        System.out.println("  consumed " + seen.size() + " times");
        System.out.println("    " + nulls + " of them found NOTHING (null)");
        System.out.println("    " + duplicates + " duplicated the previous item");
        System.out.println("  Nothing told either thread when the other was ready.");
    }

    // ---------------------------------------------------- busyWaitDeadlock
    static class BusyWaitBox {
        Integer item;
        boolean flag = false;

        // The whole method is synchronized, so the waiting loop holds the ONLY key.
        synchronized void producer(int value) {
            while (flag) { /* busy wait */ }
            item = value;
            flag = true;
            System.out.println("    producer produces " + value);
        }

        synchronized void consumer() {
            while (!flag) { /* busy wait INSIDE the monitor - the other thread can never enter */ }
            System.out.println("    consumer consumes " + item);
            item = null;
            flag = false;
        }
    }

    private static void busyWaitDeadlock() throws InterruptedException {
        System.out.println();
        System.out.println("=== busyWaitDeadlock: waiting inside a synchronized method ===");

        BusyWaitBox box = new BusyWaitBox();

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 20; i++) { nap(100); box.producer(i); }
        }, "producer");

        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= 20; i++) { nap(70); box.consumer(); }
        }, "consumer");

        producer.setDaemon(true);             // a deadlock must not keep the JVM alive
        consumer.setDaemon(true);
        producer.start();
        consumer.start();

        nap(800);                             // give the deadlock time to set in
        System.out.println("  producer state : " + producer.getState());
        System.out.println("  consumer state : " + consumer.getState());
        System.out.println("  consumer is spinning INSIDE the monitor; producer waits for that monitor");
        System.out.println("  -> the flag can never flip: a busy wait inside synchronized is a deadlock");
    }
}
