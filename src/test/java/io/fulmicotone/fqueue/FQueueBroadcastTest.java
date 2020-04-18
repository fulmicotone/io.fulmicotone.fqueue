package io.fulmicotone.fqueue;

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

        registry.buildFQueue(String.class)
                .consume(1, 100, TimeUnit.MILLISECONDS, () ->
                        (operations, elms) -> objectReceived.incrementAndGet());

        registry.buildFQueue(String.class)
                .consume(1, 100, TimeUnit.MILLISECONDS, () ->
                        (operations, elms) -> objectReceived.incrementAndGet());

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

        registry.buildFQueue(String.class)
                .consume(1, 100, TimeUnit.MILLISECONDS, () ->
                        (operations, elms) -> objectReceived.incrementAndGet());

        registry.buildFQueue(String.class)
                .consume(1, 100, TimeUnit.MILLISECONDS, () ->
                        (operations, elms) -> objectReceived.incrementAndGet());

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
