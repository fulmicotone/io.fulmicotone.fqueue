package io.fulmicotone.fqueue.interfaces;


import io.fulmicotone.fqueue.FQueueBroadcast;
import io.fulmicotone.fqueue.enums.BatchReason;

import java.util.List;

public interface FQueueConsumer<I> {

    void consume(FQueueBroadcast broadcaster, BatchReason batchReason, List<I> elms);
}
