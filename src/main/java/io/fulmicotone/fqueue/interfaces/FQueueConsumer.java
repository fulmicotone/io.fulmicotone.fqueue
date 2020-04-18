package io.fulmicotone.fqueue.interfaces;


import io.fulmicotone.fqueue.FQueueBroadcast;

import java.util.List;

public interface FQueueConsumer<I> {

    void consume(FQueueBroadcast operations, List<I> elms);
}
