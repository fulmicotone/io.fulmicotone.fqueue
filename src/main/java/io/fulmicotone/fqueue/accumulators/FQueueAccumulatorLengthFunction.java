package io.fulmicotone.fqueue.accumulators;

import java.util.function.Function;

@FunctionalInterface
public interface FQueueAccumulatorLengthFunction<I> extends Function<I, Long> {
}
