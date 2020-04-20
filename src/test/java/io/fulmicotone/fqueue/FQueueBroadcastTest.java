package io.fulmicotone.fqueue;

import io.fulmicotone.fqueue.interfaces.FQueueConsumerFactory;
import io.fulmicotone.fqueue.options.BatchingOption;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@RunWith(JUnit4.class)
public class FQueueBroadcastTest {


    @Test
    public void fQueueBroadcast_sendBroadcast() throws InterruptedException {

        FQueueRegistry registry = new FQueueRegistry();

        AtomicInteger objectReceived = new AtomicInteger();

        FQueueConsumerFactory<String> stringFQueueConsumerFactory = () ->
                (broadcaster, elms) -> objectReceived.incrementAndGet();

        registry.buildFQueue(String.class)
                .batch()
                .withFlushTimeUnit(TimeUnit.MILLISECONDS)
                .withFlushTimeout(100)
                .withChunkSize(1)
                .done()
                .consume(
                        stringFQueueConsumerFactory);

        registry.buildFQueue(String.class)
                .batch()
                .withFlushTimeUnit(TimeUnit.MILLISECONDS)
                .withFlushTimeout(100)
                .withChunkSize(1)
                .done()
                .consume(stringFQueueConsumerFactory);

        FQueueBroadcast broadcast = new FQueueBroadcast(registry);

        broadcast.sendBroadcast("test");


        Thread.sleep(500);


        Assert.assertEquals(objectReceived.get(), 2);
        Assert.assertEquals(1, (long) broadcast.getProduced());


    }


    @Test
    public void fQueueBroadcast_sendAllBroadcast() throws InterruptedException {

        FQueueRegistry registry = new FQueueRegistry();

        AtomicInteger objectReceived = new AtomicInteger();

        FQueueConsumerFactory<String> stringFQueueConsumerFactory = () ->
                (broadcaster, elms) -> objectReceived.incrementAndGet();

        registry.buildFQueue(String.class)
                .batch()
                .withFlushTimeUnit(TimeUnit.MILLISECONDS)
                .withFlushTimeout(100)
                .withChunkSize(1)
                .done()
                .consume(
                        stringFQueueConsumerFactory);

        registry.buildFQueue(String.class)
                .batch()
                .withFlushTimeUnit(TimeUnit.MILLISECONDS)
                .withFlushTimeout(100)
                .withChunkSize(1)
                .done()
                .consume(stringFQueueConsumerFactory);

        FQueueBroadcast broadcast = new FQueueBroadcast(registry);

        broadcast.sendBroadcast(String.class, Collections.singletonList("test"));


        Thread.sleep(500);


        Assert.assertEquals(objectReceived.get(), 2);
        Assert.assertEquals(1, (long) broadcast.getProduced());


    }


    @Test
    public void fQueueBroadcast_incrementCounter_() {

        FQueueRegistry registry = new FQueueRegistry();

        FQueueBroadcast broadcast = new FQueueBroadcast(registry);
        broadcast.incrementCounter(1);


        Assert.assertEquals(1L, (long) broadcast.getProduced());


    }

}
