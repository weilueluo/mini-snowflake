import java.time.InstantSource;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class GeneratorTest {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);

        Generator generator = new Generator();

        int amount = 10_000;
        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            futures.add(executor.submit(generator::next));
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        List<Long> ids = futures.stream().map(future -> {
            try {
                return future.get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        System.out.printf("generated: %d%n", ids.size());
        System.out.printf("actual: %d%n", new HashSet<>(ids).size());
        System.out.printf("expected: %d%n", amount);
    }
}
