package io.fulmicotone.fqueue;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@RunWith(JUnit4.class)
public class FQueueRegistryTest {


    @Test
    public void fQueueBroadcast_getStats() {

        FQueueRegistry registry = new FQueueRegistry();

        AtomicInteger objectReceived = new AtomicInteger();

        registry.buildFQueue(String.class)
                .batch()
                .withFlushTimeUnit(TimeUnit.MILLISECONDS)
                .withFlushTimeout(100)
                .withChunkSize(1)
                .done()
                .consume(() ->
                        (operations, elms) -> objectReceived.incrementAndGet());

        List<String> list = registry.getStatuses();

        Assert.assertTrue(list.size() > 0);
        Assert.assertTrue(list.stream().allMatch(s -> s.contains("FQueue")));
        Assert.assertTrue(list.stream().filter(s -> s.contains("Received")).allMatch(s -> s.contains("0")));
        Assert.assertTrue(list.stream().filter(s -> s.contains("Produced")).allMatch(s -> s.contains("0")));
        Assert.assertTrue(list.stream().filter(s -> s.contains("Batched")).allMatch(s -> s.contains("0")));


    }


    @Test
    public void fQueueBroadcast_getStats_for_FanOuts() {

        int fanOut = 5;
        FQueueRegistry registry = new FQueueRegistry();

        AtomicInteger objectReceived = new AtomicInteger();

        registry.buildFQueue(String.class)
                .fanOut(fanOut)
                .batch()
                .withFlushTimeUnit(TimeUnit.MILLISECONDS)
                .withFlushTimeout(100)
                .withChunkSize(1)
                .done()
                .consume( () ->
                        (operations, elms) -> objectReceived.incrementAndGet());


        List<String> list = registry.getStatuses();

        Assert.assertTrue(list.size() > 0);
        Assert.assertTrue(list.stream().allMatch(s -> s.contains("FQueue")));
        Assert.assertTrue(list.stream().filter(s -> s.contains("Received")).allMatch(s -> s.contains("0")));
        Assert.assertTrue(list.stream().filter(s -> s.contains("Produced")).allMatch(s -> s.contains("0")));
        Assert.assertTrue(list.stream().filter(s -> s.contains("Batched")).allMatch(s -> s.contains("0")));
        Assert.assertEquals(list.stream().filter(s -> s.contains("|----->")).count(), fanOut);


    }




}
