package io.fulmicotone.fqueue.interfaces;

@FunctionalInterface
public interface FQueueConsumerFactory<I> {

    FQueueConsumer<I> build();
}
