package io.fulmicotone.fqueue.accumulators;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class FQueueAccumulator<E> {

    private double sizeLimit;
    private List<E> accumulator = new ArrayList<>();
    private AtomicLong accumulatorSize = new AtomicLong(0);
    private FQueueAccumulatorLengthFunction<E> accumulatorLengthFunction;

    public FQueueAccumulator(double sizeLimit, FQueueAccumulatorLengthFunction<E> accumulatorLengthFunction){
        this.sizeLimit = sizeLimit;
        this.accumulatorLengthFunction = accumulatorLengthFunction;
    }


    public boolean add(E obj) {

        long size = objectSize(obj);

        if(shouldBecomeFull(size)){
            return false;
        }

        accumulator.add(obj);
        accumulatorSize.addAndGet(size);
        return true;
    }

    public List<E> getElements(){
        return accumulator;
    }


    private long objectSize(E obj)
    {
        return accumulatorLengthFunction.apply(obj);
    }

    private boolean shouldBecomeFull(double size){
        return (accumulatorSize.get() + size > sizeLimit);
    }
}
