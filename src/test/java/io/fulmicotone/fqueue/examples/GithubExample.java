package io.fulmicotone.fqueue.examples;

import io.fulmicotone.fqueue.FQueue;
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
                .consume(() -> (broadcaster, reason, elms) -> System.out.println("CASE 1 - Elements batched are: "+elms.size()));

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
                .consume(() -> (broadcaster, reason, elms) -> System.out.println("CASE 2 - Elements batched are: " + elms.size()));



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
                .withLengthFunction(s -> s.getBytes().length)
                .withFlushTimeout(1)
                .withFlushTimeUnit(TimeUnit.SECONDS)
                .done()
                .consume(() -> (broadcaster, reason, elms) -> System.out.println("CASE 3 - Elements batched are: "+elms.size()));


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
                .consume(() -> (broadcaster, reason, elms) -> {

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


    @Test
    public void caseBroadcast() throws InterruptedException {

        int resultTimeoutInMilliSeconds = 1_000;

        /**
         * Make sure that you have only one instance of FQueueRegistry in your project.
         * If using Spring please annotate it as @Bean
         */
        FQueueRegistry registry = new FQueueRegistry();


        /**
         * CASE Broadcast
         * fanOut(3) creates three nested FQueue, while the first defined acts as round-robin dispatcher
         * Every nested FQueue consume data and aggregate them in chunks of 5 elements.
         * If data are less than chunk size every nested FQueue will flush them every 1 second.
         */
        FQueue<String> one = registry.buildFQueue(String.class)
                .consume(() -> (broadcaster, reason, elms) -> System.out.println("ONE - Elements received are: " + elms.size()));

        FQueue<String> two = registry.buildFQueue(String.class)
                .consume(() -> (broadcaster, reason, elms) -> System.out.println("TWO - Elements batched are: " + elms.size()));

        /** This will received by one and two  */
        registry.sendBroadcast("Sample");


        Thread.sleep(resultTimeoutInMilliSeconds);



    }

    @Test
    public void caseSpecific() throws InterruptedException {

        int resultTimeoutInMilliSeconds = 1_000;

        /**
         * Make sure that you have only one instance of FQueueRegistry in your project.
         * If using Spring please annotate it as @Bean
         */
        FQueueRegistry registry = new FQueueRegistry();


        /**
         * CASE Specific
         * fanOut(3) creates three nested FQueue, while the first defined acts as round-robin dispatcher
         * Every nested FQueue consume data and aggregate them in chunks of 5 elements.
         * If data are less than chunk size every nested FQueue will flush them every 1 second.
         */
        FQueue<String> one = registry.buildFQueue(String.class)
                .consume(() -> (broadcaster, reason,elms) -> System.out.println("ONE - Elements received are: " + elms.size()));

        FQueue<String> two = registry.buildFQueue(String.class)
                .consume(() -> (broadcaster, reason,elms) -> System.out.println("TWO - Elements batched are: " + elms.size()));

        /** This will received by one  */
        one.getQueue().add("Sample");


        Thread.sleep(resultTimeoutInMilliSeconds);



    }

    @Test
    public void caseFromFQueueToAnother() throws InterruptedException {

        int resultTimeoutInMilliSeconds = 1_000;

        /**
         * Make sure that you have only one instance of FQueueRegistry in your project.
         * If using Spring please annotate it as @Bean
         */
        FQueueRegistry registry = new FQueueRegistry();


        /**
         * CASE PASS DATAS
         * - Sometimes you want to pass datas between FQueue
         * - It's possible by calling the "brodacaster" object injected into the consuming function
         */
        FQueue<String> one = registry.buildFQueue(String.class)
                .consume(() -> (broadcaster, reason,elms) -> {
                    // count all characters and send them to "two" FQueue
                    elms.stream()
                            .map(String::length)
                            .forEach(broadcaster::sendBroadcast);
                });

        FQueue<Integer> two = registry.buildFQueue(Integer.class)
                .consume(() -> (broadcaster, reason, elms) -> {
                    elms.forEach(ch -> System.out.println("Character size is:" + ch));
                });

        /** This will received by one  */
        one.getQueue().add("Sample");


        Thread.sleep(resultTimeoutInMilliSeconds);



    }
}
