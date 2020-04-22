package io.fulmicotone.fqueue.options;

import io.fulmicotone.fqueue.FQueue;
import io.fulmicotone.fqueue.accumulators.FQueueAccumulatorLengthFunction;

import java.util.concurrent.TimeUnit;

public class BatchingOption<E> {

    private FQueue<E> caller;

    /** Flush chunkSize  */
    private int chunkSize;
    /** Flush timeout  */
    private int flushTimeout;
    /** Flush timeunit  */
    private TimeUnit flushTimeUnit;
    /** Custom length function, by default is 1 which means that count is based on elements  */
    private FQueueAccumulatorLengthFunction<E> lengthFunction;

    private BatchingOption(Builder builder) {
        chunkSize = builder.chunkSize;
        flushTimeout = builder.flushTimeout;
        flushTimeUnit = builder.flushTimeUnit;
        lengthFunction = builder.lengthFunction;
    }

    public static <E>Builder<E> newBuilder(FQueue<E> caller) {
        return new Builder<>(caller);
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getFlushTimeout() {
        return flushTimeout;
    }

    public TimeUnit getFlushTimeUnit() {
        return flushTimeUnit;
    }

    public FQueueAccumulatorLengthFunction<E> getLengthFunction() {
        return lengthFunction;
    }

    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    public static final class Builder<E> {
        private FQueue<E> caller;
        private int chunkSize = 1;
        private int flushTimeout = 10;
        private TimeUnit flushTimeUnit = TimeUnit.MILLISECONDS;
        private FQueueAccumulatorLengthFunction<E> lengthFunction;

        private Builder(FQueue<E> caller) {
            this.caller = caller;
        }

        public Builder<E> withChunkSize(int val) {
            chunkSize = val;
            return this;
        }

        public Builder<E> withFlushTimeout(int val) {
            flushTimeout = val;
            return this;
        }

        public Builder<E> withFlushTimeUnit(TimeUnit val) {
            flushTimeUnit = val;
            return this;
        }

        public Builder<E> withLengthFunction(FQueueAccumulatorLengthFunction<E> val) {
            lengthFunction = val;
            return this;
        }

        public FQueue<E> done() {
            caller.setBatchingOption(new BatchingOption<>(this));
            return caller;
        }
    }
}
