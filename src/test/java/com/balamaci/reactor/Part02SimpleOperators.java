package com.balamaci.reactor;

import com.balamaci.reactor.util.Helpers;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @author sbalamaci
 */
public class Part02SimpleOperators implements BaseTestFlux {

    @Test
    public void doOnXXXOperators() {
        Flux<Integer> flux = Flux.create(subscriber -> {
            log.info("Started emitting");

            log.info("Emitting 1st");
            subscriber.next(1);

            log.info("Emitting 2nd");
            subscriber.next(2);

            subscriber.complete();
        });

        flux
                .log()
                .map(val -> val * 10)
//                .log(reqVal -> log.info("OnRequest {}", reqVal))
                .filter(val -> val > 10)
//                .doOnNext(val -> log.info("After Filter {}", val))
                .subscribe(val -> log.info("Subscriber received: {}", val));

    }

    /**
     * Delay operator - the Thread.sleep of the reactive world, it's pausing for a particular increment of time
     * before emitting the events which are thus shifted by the specified time amount.
     * <p>
     * The delay operator uses a Scheduler {@see Part07Schedulers} by default, which actually means it's
     * running the operators and the subscribe operations on a different thread, which means the test method
     * will terminate before we see the text from the log.
     * <p>
     * To prevent this we use the CountDownLatch that we decrement on onComplete
     */
    @Test
    public void delayElements() {
        CountDownLatch latch = new CountDownLatch(1);

        log.info("Starting");
        Flux.range(0, 5)
                .doOnNext(val -> log.info("Emitted {}", val))
                .delayElements(Duration.of(2, ChronoUnit.SECONDS))
                .subscribe(
                        tick -> log.info("Tick {}", tick),
                        (ex) -> log.info("Error emitted"),
                        () -> {
                            log.info("Completed");
                            latch.countDown();
                        });

        Helpers.wait(latch);
    }

    @Test
    public void delaySubscription() {
        log.info("Starting");
        Flux<Integer> flux = Flux.range(0, 3)
                .doOnSubscribe(subscription -> log.info("Subscribed"))
                .delaySubscription(Duration.of(5, ChronoUnit.SECONDS));

        subscribeWithLogWaiting(flux);
    }

    /**
     * Periodically emits a number starting from 0 and then increasing the value on each emission
     */
    @Test
    public void intervalOperator() {
        log.info("Starting");

        Flux<Long> flux = Flux.interval(Duration.of(1, ChronoUnit.SECONDS))
                .take(5);

        subscribeWithLogWaiting(flux);
    }

    /**
     * scan operator - takes an initial value and a function(accumulator, currentValue). It goes through the events
     * sequence and combines the current event value with the previous result(accumulator) emitting downstream the
     * The initial value is used for the first event
     */
    @Test
    public void scanOperator() {
        Flux<Integer> numbers = Flux.just(3, 5, -2, 9)
                .scan(0, (totalSoFar, currentValue) -> {
                    log.info("totalSoFar={}, emitted={}", totalSoFar, currentValue);
                    return totalSoFar + currentValue;
                });

        subscribeWithLog(numbers);
    }

    /**
     * reduce operator acts like the scan operator but it only passes downstream the final result
     * (doesn't pass the intermediate results downstream) so the subscriber receives just one event
     */
    @Test
    public void reduceOperator() {
        Mono<Integer> numbers = Flux.just(3, 5, -2, 9)
                                   .reduce(0, (totalSoFar, val) -> {
                                                log.info("totalSoFar={}, emitted={}", totalSoFar, val);
                                                return totalSoFar + val;
                                   });
        subscribeWithLog(numbers);
    }

    /**
     * collect operator acts similar to the reduce() operator, but while the reduce() operator uses a reduce function
     * which returns a value, the collect() operator takes a container supplie and a function which doesn't return
     * anything(a consumer). The mutable container is passed for every event and thus you get a chance to modify it
     * in this collect consumer function
     */
    @Test
    public void collectOperator() {
        Mono<List<Integer>> numbers = Flux.just(3, 5, -2, 9)
                                    .collect(ArrayList::new, (container, value) -> {
                                        log.info("Adding {} to container", value);
                                        container.add(value);
                                        //notice we don't need to return anything
                                    });
        subscribeWithLog(numbers);
    }

    /**
     * repeat resubscribes to the observable after it receives onComplete
     */
    @Test
    public void repeat() {
        Flux<Integer> random = Flux.defer(() -> {
                                    Random rand = new Random();
                                    return Mono.just(rand.nextInt(20));
                                })
                          .repeat(5);

        subscribeWithLogWaiting(random);
    }

    /**
     * generate
     */

}
