import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class App {

    public static void main(String[] args) throws InterruptedException {

        ExecutorService pool = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 10; i++) {

            Counter deadCounter = new Counter();
            Counter lockCounter = new Counter();

            CompletableFuture<Void> increment1 = CompletableFuture.runAsync( () -> deadCounter.increment(lockCounter), pool);
            CompletableFuture<Void> increment2 = CompletableFuture.runAsync( () -> deadCounter.increment(lockCounter), pool);
            CompletableFuture<Void> increment3 = CompletableFuture.runAsync( () -> lockCounter.increment(deadCounter), pool);
            CompletableFuture<Void> increment4 = CompletableFuture.runAsync( () -> lockCounter.increment(deadCounter), pool);

            CompletableFuture<Void> all = CompletableFuture.<Integer>allOf(increment1, increment2, increment3, increment4);
            all.thenApply((v) -> {
                if (deadCounter.get() != 2) {
                    System.out.println("Incorrect counter value: " + Integer.toString(deadCounter.get()));
                }
                return null;
            });
        }



        waitForThreadpoolShutdown(pool);
    }

    private static void waitForThreadpoolShutdown(ExecutorService pool) throws InterruptedException {
        pool.shutdownNow();
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            System.err.println("Pool did not complete within 10 seconds");
            pool.shutdownNow();
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("Pool did not terminate");
            }
        }
    }

    public static class Counter {

        private int val = 0;

        synchronized public void increment(Counter counter) {

            //By doing the most stupid and unnecessary thing possible (making the counters interact with each other),
            //I have successfully(????) caused a deadlock.

            if(counter == this){
                val += 1;
            } else {
                try {
                    Thread.sleep(216);
                } catch (Exception e) {

                }
                counter.increment(this);
            }

        }

        public int get() {
            return val;
        }
    }
}
