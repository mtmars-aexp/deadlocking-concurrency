import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


//Oh boy howdy that's a lot of words that I do not understand.

public class App {

    public static void main(String[] args) throws InterruptedException {

        ExecutorService pool = Executors.newFixedThreadPool(4);
        //Higher thread count seems to more return the correct final value (20) more frequently and consistently?
        //Possibly because more threads being dedicated means less threads are jumping all over the place and getting confused.
        //Possibly higher thread count = less work per thread, meaning each thread can be terminated quicker (and before the final value is printed?)

        Counter macroCounter = new Counter();

        for (int i = 0; i < 10; i++) {

            Counter microCounter = new Counter();

            Counter deadCounter = new Counter();
            Counter lockCounter = new Counter();

            CompletableFuture<Void> increment1 = CompletableFuture.runAsync(microCounter::increment, pool);
            CompletableFuture<Void> increment2 = CompletableFuture.runAsync(microCounter::increment, pool);
            CompletableFuture<Void> increment3 = CompletableFuture.runAsync(macroCounter::increment, pool);
            CompletableFuture<Void> increment4 = CompletableFuture.runAsync(macroCounter::increment, pool);

            CompletableFuture<Void> all = CompletableFuture.<Integer>allOf(increment1, increment2, increment3, increment4);
            all.thenApply((v) -> {
                if (microCounter.get() != 2) {
                    System.out.println("Incorrect counter value: " + Integer.toString(microCounter.get()));
                } else {
                    System.out.println("Hopefully correct value: " + Integer.toString(microCounter.get()));
                }
                return null;
            });


        }

        waitForThreadpoolShutdown(pool);
        System.out.println("Macro counter value: " + macroCounter.get());
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
        private AtomicInteger val = new AtomicInteger(0);
        private AtomicBoolean lock = new AtomicBoolean(false);

        synchronized public void increment() {

            while(lock.get()){
                //System.out.println("Increment failed! Counter is locked");
            }

            int valueBeforeIncrementation = val.get();

            System.out.println(this.hashCode() + " is attempting to increment on thread " + Thread.currentThread().getName());
            //I printed out the hashcodes of each counter here, and noticed 11 unique hashcodes.
            //This is strange, as I only have 2 counter instances, and 4 different CompleteableFutures
            //It's truly a mystery where 11 counters could have come from.


            while(val.get() != valueBeforeIncrementation + 1){
                val.getAndIncrement();
                System.out.println("Incrementing!");
            }

            toggleLock();
            //This causes a much more deadlocky deadlock than simply trapping the threads in an infinite while statement below.
            //The literal "lock" is locked/unlocked when this statement is reached, and any other threads will be left waiting for the lock to be toggled by another thread.
            //I realize this isn't(?) a true deadlock, since there's no specific resource access involved.

            //while(true){}
            //The above while statement causes a deadlock, the other threads will be left waiting to be able to increment.

        }


        synchronized public void toggleLock() {
            lock.set(!lock.get());
        }

        synchronized public int get() {
            return val.get();
        }
    }
}
