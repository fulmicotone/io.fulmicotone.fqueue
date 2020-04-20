package io.fulmicotone.fqueue.examples;

import io.fulmicotone.fqueue.FQueueRegistry;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class GithubExample {


    @Test
    public void case1() throws InterruptedException {

        int resultTimeoutInMilliSeconds = 2_000;

        /**
         * Make sure that you have only one instance of FQueueRegistry in your project.
         * If using Spring please annotate it as @Bean
         */
        FQueueRegistry registry = new FQueueRegistry();


        /**
         * CASE 1: Simple consuming, by default it consumes 1 element with flushing every 10 ms.
         */
        registry.buildFQueue(String.class)
                .consume(() -> (operations, elms) -> System.out.println("CASE 1 - Elements batched are: "+elms.size()));

        /**
         * Send fake objects
         */
        for(int i = 0; i < 10; i++){
            registry.sendBroadcast("Sample");
        }

        Thread.sleep(resultTimeoutInMilliSeconds);



    }

    @Test
    public void case2() throws InterruptedException {

        int resultTimeoutInMilliSeconds = 2_000;

        /**
         * Make sure that you have only one instance of FQueueRegistry in your project.
         * If using Spring please annotate it as @Bean
         */
        FQueueRegistry registry = new FQueueRegistry();


        /**
         * CASE 2: Batching consuming, it consumes data and aggregate them in chunks of 5 elements.
         * If data are less than chunk size it will flush them every 1 second.
         */
        registry.buildFQueue(String.class)
                .batch()
                .withChunkSize(5)
                .withFlushTimeout(1)
                .withFlushTimeUnit(TimeUnit.SECONDS)
                .done()
                .consume(() -> (operations, elms) -> System.out.println("CASE 2 - Elements batched are: "+elms.size()));


        /**
         * Send fake objects
         */
        for(int i = 0; i < 10; i++){
            registry.sendBroadcast("Sample");
        }

        Thread.sleep(resultTimeoutInMilliSeconds);



    }

    @Test
    public void case3() throws InterruptedException {

        int resultTimeoutInMilliSeconds = 2_000;

        /**
         * Make sure that you have only one instance of FQueueRegistry in your project.
         * If using Spring please annotate it as @Bean
         */
        FQueueRegistry registry = new FQueueRegistry();


        /**
         * CASE 3: Batching consuming with a custom accumulation function.
         * it consumes data and aggregate them in chunks of 15 bytes elements (2 "Sample" string will fit in).
         * If data are less than chunk size it will flush them every 1 second.
         */
        registry.buildFQueue(String.class)
                .batch()
                .withChunkSize(15)
                .withLengthFunction(s -> (long)s.getBytes().length)
                .withFlushTimeout(1)
                .withFlushTimeUnit(TimeUnit.SECONDS)
                .done()
                .consume(() -> (operations, elms) -> System.out.println("CASE 3 - Elements batched are: "+elms.size()));


        /**
         * Send fake objects
         */
        for(int i = 0; i < 10; i++){
            registry.sendBroadcast("Sample");
        }

        Thread.sleep(resultTimeoutInMilliSeconds);



    }

    @Test
    public void case4() throws InterruptedException {

        int resultTimeoutInMilliSeconds = 2_000;

        /**
         * Make sure that you have only one instance of FQueueRegistry in your project.
         * If using Spring please annotate it as @Bean
         */
        FQueueRegistry registry = new FQueueRegistry();


        /**
         * CASE 4: Batching consuming with FanOut,
         * fanOut(3) creates three nested FQueue, while the first defined acts as round-robin dispatcher
         * Every nested FQueue consume data and aggregate them in chunks of 5 elements.
         * If data are less than chunk size every nested FQueue will flush them every 1 second.
         */
        registry.buildFQueue(String.class)
                .fanOut(3)
                .batch()
                .withChunkSize(5)
                .withFlushTimeout(1)
                .withFlushTimeUnit(TimeUnit.SECONDS)
                .done()
                .consume(() -> (operations, elms) -> {

                    System.out.println("CASE 4 - currentThread is: "+Thread.currentThread().getName()+ " - Elements batched are: "+elms.size());
                });


        /**
         * Send fake objects
         */
        for(int i = 0; i < 10; i++){
            registry.sendBroadcast("Sample");
        }

        Thread.sleep(resultTimeoutInMilliSeconds);



    }
}
