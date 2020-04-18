package io.fulmicotone.fqueue.accumulators;


public class FQueueAccumulatorFactory<E> {

    private final double sizeLimit;
    private final FQueueAccumulatorLengthFunction<E> accumulatorLengthFunction;

    public FQueueAccumulatorFactory(double sizeLimit, FQueueAccumulatorLengthFunction<E> accumulatorLengthFunction){
        this.sizeLimit = sizeLimit;
        this.accumulatorLengthFunction = accumulatorLengthFunction;
    }

    public FQueueAccumulator<E> build(){
        return new FQueueAccumulator<>(sizeLimit, accumulatorLengthFunction);
    }
}
