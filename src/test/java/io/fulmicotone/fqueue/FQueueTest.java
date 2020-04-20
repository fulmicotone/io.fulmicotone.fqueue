package io.fulmicotone.fqueue;

import io.fulmicotone.fqueue.accumulators.FQueueAccumulatorLengthFunction;
import io.fulmicotone.fqueue.interfaces.FQueueConsumer;
import io.fulmicotone.fqueue.model.ComplexObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;



@RunWith(JUnit4.class)
public class FQueueTest {


    @Test
    public void testSizeBatching_WITH_FAN_OUT() throws InterruptedException {

        /**
         * Test round robin fan out.
         * Every child should receive the same amount of objects.
         * */
        int elements = 100;
        int chunkSize = 5;
        int flushTimeoutInMilliSeconds = 1_000;
        int resultTimeoutInMilliSeconds = 2_000;
        int fanOut = 5;
        AtomicInteger children = new AtomicInteger();
        Map<Integer, Integer> results = new ConcurrentHashMap<>();

        FQueueRegistry registry = new FQueueRegistry();


        registry.buildFQueue(String.class)
                .fanOut(fanOut)
                .batch()
                .withFlushTimeUnit(TimeUnit.MILLISECONDS)
                .withFlushTimeout(flushTimeoutInMilliSeconds)
                .withChunkSize(chunkSize)
                .done()
                .consume(() -> new FQueueConsumer<String>() {

                    int childNum = children.incrementAndGet();

                    @Override
                    public void consume(FQueueBroadcast operations, List<String> elms) {
                        Integer sum = results.getOrDefault(childNum, 0);
                        results.put(childNum, sum+elms.size());
                    }
                });


        for(int i = 0; i < elements; i++){
            registry.sendBroadcast("a"+1);
        }

        Thread.sleep(resultTimeoutInMilliSeconds);

        Assert.assertEquals(results.size(), fanOut);
        Assert.assertTrue(results.values().stream().allMatch(s -> s == (elements/fanOut)));


    }


    @Test
    public void testSizeBatching_NO_FAN_OUT() throws InterruptedException {

        /**
         * Test size batching.
         * With 100 elements and 5 chunks size I expect 20 calls with 5 elements each.
         * */
        int elements = 100;
        int chunkSize = 5;
        int flushTimeoutInMilliSeconds = 1_000;
        int resultTimeoutInMilliSeconds = 2_000;
        AtomicInteger calls = new AtomicInteger();
        Map<Integer, Integer> results = new HashMap<>();

        FQueueRegistry registry = new FQueueRegistry();


        registry.buildFQueue(String.class)
                .batch()
                .withFlushTimeUnit(TimeUnit.MILLISECONDS)
                .withFlushTimeout(flushTimeoutInMilliSeconds)
                .withChunkSize(chunkSize)
                .done()
                .consume( () -> (FQueueConsumer<String>) (operations, elms) -> {

                    int call = calls.incrementAndGet();

                    Integer sum = results.getOrDefault(call, 0);
                    results.put(call, sum+elms.size());
                });


        for(int i = 0; i < elements; i++){
            registry.sendBroadcast("a"+1);
        }

        Thread.sleep(resultTimeoutInMilliSeconds);

        Assert.assertEquals(results.size(), (elements/chunkSize));
        Assert.assertTrue(results.values().stream().allMatch(s -> s == chunkSize));


    }




    @Test
    public void testCustomAccumulatorBatching_WITH_FAN_OUT() throws InterruptedException {

        /**
         * Test round robin fan out with custom accumulator.
         * Sometimes we need some sophisticated accumulator which accumulate objects not in term of size but on other
         * specifications.
         * For example I could have a custom Java object and I'm interested in producing a Json representation of this
         * object which should group objects in list of 150 bytes.
         *
         * Every Complex Object will have a Json representation of 30 bytes.
         * */
        int elements = 50;
        int chunkSizeInBytes = 150; // Bytes
        int singleObjectSizeInBytes = 30;
        int flushTimeoutInMilliSeconds = 1_000;
        int resultTimeoutInMilliSeconds = 2_000;
        int fanOut = 5;
        AtomicInteger children = new AtomicInteger();
        Map<Integer, Integer> results = new ConcurrentHashMap<>();

        FQueueRegistry registry = new FQueueRegistry();


        registry.buildFQueue(ComplexObject.class)
                .fanOut(fanOut)
                .batch()
                .withFlushTimeUnit(TimeUnit.MILLISECONDS)
                .withFlushTimeout(flushTimeoutInMilliSeconds)
                .withChunkSize(chunkSizeInBytes)
                .withLengthFunction(complexObject -> (long) complexObject.getJson().getBytes().length)
                .done()
                .consume(() -> new FQueueConsumer<ComplexObject>() {

                    int childNum = children.getAndIncrement();

                    @Override
                    public void consume(FQueueBroadcast operations, List<ComplexObject> elms) {

                        Assert.assertEquals(elms.size(), (chunkSizeInBytes / singleObjectSizeInBytes));

                        Integer sum = results.getOrDefault(childNum, 0);
                        results.put(childNum, sum + elms.size());
                    }
                });


        for(int i = 0; i < elements; i++){

            ComplexObject complexObject = ComplexObject.newBuilder()
                    .withName("Anna")
                    .withAge(22)
                    .build();

            registry.sendBroadcast(complexObject);
        }

        Thread.sleep(resultTimeoutInMilliSeconds);

        Assert.assertEquals(results.size(), fanOut);
        Assert.assertTrue(results.values().stream().allMatch(s -> s == ((elements*singleObjectSizeInBytes)/chunkSizeInBytes)));


    }


    @Test
    public void testCustomAccumulatorBatching_NO_FAN_OUTA() throws InterruptedException {

        /**
         * Test custom accumulator.
         * Sometimes we need some sophisticated accumulator which accumulate objects not in term of size but on other
         * specifications.
         * For example I could have a custom Java object and I'm interested in producing a Json representation of this
         * object which should group objects in list of 150 bytes.
         *
         * Every Complex Object will have a Json representation of 30 bytes.
         * */
        int elements = 50;
        int chunkSizeInBytes = 150; // Bytes
        int singleObjectSizeInBytes = 30;
        int flushTimeoutInMilliSeconds = 1_000;
        int resultTimeoutInMilliSeconds = 2_000;
        AtomicInteger calls = new AtomicInteger();
        Map<Integer, Integer> results = new ConcurrentHashMap<>();

        FQueueRegistry registry = new FQueueRegistry();


        registry.buildFQueue(ComplexObject.class)
                .batch()
                .withFlushTimeUnit(TimeUnit.MILLISECONDS)
                .withFlushTimeout(flushTimeoutInMilliSeconds)
                .withChunkSize(chunkSizeInBytes)
                .withLengthFunction(complexObject -> (long) complexObject.getJson().getBytes().length)
                .done()
                .consume(() -> (operations, elms) -> {

                            int call = calls.incrementAndGet();

                            Assert.assertEquals(elms.size(), (chunkSizeInBytes / singleObjectSizeInBytes));

                            Integer sum = results.getOrDefault(call, 0);
                            results.put(call, sum+elms.size());
                        });


        for(int i = 0; i < elements; i++){

            ComplexObject complexObject = ComplexObject.newBuilder()
                    .withName("Anna")
                    .withAge(22)
                    .build();

            registry.sendBroadcast(complexObject);
        }

        Thread.sleep(resultTimeoutInMilliSeconds);

        Assert.assertEquals(results.size(), ((elements*singleObjectSizeInBytes)/chunkSizeInBytes));
        Assert.assertTrue(results.values().stream().allMatch(s -> s == (chunkSizeInBytes/singleObjectSizeInBytes)));


    }

}
