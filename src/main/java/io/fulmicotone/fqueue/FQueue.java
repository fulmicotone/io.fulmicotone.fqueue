package io.fulmicotone.fqueue;




import io.fulmicotone.fqueue.accumulators.FQueueAccumulator;
import io.fulmicotone.fqueue.accumulators.FQueueAccumulatorFactory;
import io.fulmicotone.fqueue.accumulators.FQueueAccumulatorLengthFunction;
import io.fulmicotone.fqueue.interfaces.FQueueConsumer;
import io.fulmicotone.fqueue.interfaces.FQueueConsumerFactory;
import io.fulmicotone.fqueue.options.BatchingOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FQueue<E> {

    /** FQueue global registry */
    private FQueueRegistry registry;

    /** Explicit class object */
    private Class<E> clazz;

    /** Receiving queue of FQueue */
    private LinkedBlockingQueue<E> queue;

    /** Broadcaster object, useful to allow FQueueConsumer push objects in other FQueues */
    private FQueueBroadcast broadcaster;

    /** Internal executor service. Every FQueue has it's own executor service  */
    private ExecutorService executorService;

    /** Accumulator factory  */
    private FQueueAccumulatorFactory<E> accumulatorFactory;


    /** Counters for received, batched objects  */
    private final AtomicLong received = new AtomicLong();
    private final AtomicLong batched = new AtomicLong();

    /** Batching Option  */
    private BatchingOption<E> batchingOption;

    /** FanOut settings. FQueue could be set in fan-out mode which means that current FQueue act as round robin dispatcher
     * while others FQueue child will do the work. This increase parallelism balancing resources.
     * */
    private int fanOut = 1;
    private List<FQueue<E>> childFQueues = new ArrayList<>();




    public FQueue(Class<E> clazz, FQueueRegistry registry) {
        this.broadcaster = Optional.ofNullable(registry).map(FQueueBroadcast::new).orElse(null);
        this.registry = registry;
        this.clazz = clazz;
        this.queue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newFixedThreadPool(1);
        BatchingOption.newBuilder(this).done();
    }



    public BatchingOption.Builder<E> batch(){
        return BatchingOption.newBuilder(this);
    }

    public void setBatchingOption(BatchingOption<E> batchingOption) {
        this.batchingOption = batchingOption;
    }

    public FQueue<E> fanOut(int num){
        fanOut = num;
        return this;
    }



    public FQueue<E> consume(FQueueConsumerFactory<E> factory){
        this.accumulatorFactory = new FQueueAccumulatorFactory<>(batchingOption.getChunkSize(), batchingOption.getLengthFunction());


        if(fanOut == 1){
            if(batchingOption.getLengthFunction() == null){
                executorService.submit(Objects.requireNonNull(consumeBatchingSize(factory.build())));
            }else{
                executorService.submit(Objects.requireNonNull(consumeBatchingAccumulator(factory.build())));
            }
        }else{
            IntStream.range(0, fanOut)
                    .forEach(i -> {
                        FQueue<E> child = new FQueue<>(clazz, null);
                        child.setBatchingOption(this.batchingOption);
                        child.consume(factory);
                        childFQueues.add(child);
                    });
            executorService.submit(Objects.requireNonNull(consumeDispatcher()));
        }

        return this;
    }







    public Class<E> getInputClass() { return this.clazz; }
    public BlockingQueue<E> getQueue(){
        return this.queue;
    }

    public List<String> getStats(){

        List<String> base = new ArrayList<>();
        base.add("FQueue<" + clazz.getSimpleName() + ">");
        base.add("FQueue<" + clazz.getSimpleName() + "> a) QueueSize: "+queue.size());
        base.add("FQueue<" + clazz.getSimpleName() + "> b) Received: " + received.get());
        base.add("FQueue<" + clazz.getSimpleName() + "> c) Batched: " + batched.get());
        base.add("FQueue<" + clazz.getSimpleName() + "> d) Produced: "+ Optional.ofNullable(broadcaster).map(FQueueBroadcast::getProduced).orElse(0L));

        if(fanOut != 1){
            base.addAll(childFQueues.stream().map(f -> "|-----> "+f.getStats()).collect(Collectors.toList()));
        }

        return base;
    }






    private Runnable consumeBatchingAccumulator(FQueueConsumer<E> consumer){


        return () -> {

            FQueueAccumulator<E> accumulator = accumulatorFactory.build();

            while (!Thread.currentThread().isInterrupted())
            {
                try
                {

                    long deadline = System.nanoTime() + batchingOption.getFlushTimeUnit().toNanos(batchingOption.getFlushTimeout());
                    boolean isAccumulatorAvailable;
                    E elm;

                    do{
                        elm = queue.poll(1, TimeUnit.NANOSECONDS);

                        if (elm == null) { // not enough elements immediately available; will have to poll
                            elm = queue.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
                            if (elm == null) {
                                break; // we already waited enough, and there are no more elements in sight
                            }
                            received.incrementAndGet();
                            isAccumulatorAvailable = accumulator.add(elm);
                        }else{
                            received.incrementAndGet();
                            isAccumulatorAvailable = accumulator.add(elm);
                        }
                    }
                    while (isAccumulatorAvailable);

                    List<E> records = accumulator.getElements();

                    if(records.size() > 0){
                        batched.addAndGet(records.size());
                        consumer.consume(broadcaster, records);
                    }

                    accumulator = accumulatorFactory.build();
                    if(elm != null){ accumulator.add(elm); }

                }
                catch(Exception ex) {
                    ex.printStackTrace();
                }
            }

            return;
        };
    }


    private Runnable consumeBatchingSize(FQueueConsumer<E> consumer){

        final int maxSize = batchingOption.getChunkSize();
        final TimeUnit timeUnit = batchingOption.getFlushTimeUnit();
        final int timeout = batchingOption.getFlushTimeout();

        return () -> {
            while (!Thread.currentThread().isInterrupted())
            {
                try
                {
                    E elm;
                    List<E> collection = new ArrayList<>();
                    long deadline = System.nanoTime() + timeUnit.toNanos(timeout);

                    do{
                        elm = queue.poll(1, TimeUnit.NANOSECONDS);

                        if (elm == null) { // not enough elements immediately available; will have to poll
                            elm = queue.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
                            if (elm == null) {
                                break; // we already waited enough, and there are no more elements in sight
                            }
                            received.incrementAndGet();
                            collection.add(elm);
                        }else{
                            received.incrementAndGet();
                            collection.add(elm);
                        }
                    }
                    while (collection.size() < maxSize);

                    if(collection.size() > 0){
                        batched.addAndGet(collection.size());
                        consumer.consume(broadcaster, collection);
                    }

                }
                catch(Exception ex) {
                    ex.printStackTrace();
                }
            }

            return;
        };
    }


    private Runnable consumeDispatcher(){


        return () -> {

            int counter = 0;

            while (!Thread.currentThread().isInterrupted())
            {
                try
                {
                    E elm = queue.take();
                    received.incrementAndGet();
                    childFQueues.get(counter++ % fanOut).getQueue().add(elm);
                    batched.incrementAndGet();

                }
                catch(Exception ex) {
                    ex.printStackTrace();
                }
            }

        };
    }
}
