package io.fulmicotone.fqueue.accumulators;

import java.util.function.Function;

@FunctionalInterface
public interface FQueueElementLengthFunction<I> extends Function<I, Integer> {
}
