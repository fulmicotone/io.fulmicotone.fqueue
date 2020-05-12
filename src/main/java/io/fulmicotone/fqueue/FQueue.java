package io.fulmicotone.fqueue;



import io.fulmicotone.fqueue.accumulators.FQueueElementLengthFunction;
import io.fulmicotone.fqueue.enums.BatchReason;
import io.fulmicotone.fqueue.interfaces.FQueueConsumer;
import io.fulmicotone.fqueue.interfaces.FQueueConsumerFactory;
import io.fulmicotone.fqueue.options.BatchingOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
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
    protected ExecutorService executorService;

    /** Counters for received, batched objects  */
    private final AtomicLong received = new AtomicLong();
    private final AtomicLong batched = new AtomicLong();

    /** Batching Option  */
    private BatchingOption<E> batchingOption;

    /** FanOut settings. FQueue could be set in fan-out mode which means that current FQueue act as round robin dispatcher
     * while others FQueue child will do the work. This increase parallelism balancing resources.
     * */
    private int fanOut = 1;

    /** Collection that handle child FQueue when fanOut value is greater than 1.
     * */
    private List<FQueue<E>> childFQueues = new ArrayList<>();

    /** Exception handler, by default it prints the stacktrace.
     * */
    private Consumer<Exception> exceptionHandler = Throwable::printStackTrace;

    /** NOOP handler, this is fired when flushing have 0 elements.
     * */
    private Consumer<Void> noopHandler = null;


    public FQueue(Class<E> clazz, FQueueRegistry registry) {
        this.broadcaster = Optional.ofNullable(registry).map(FQueueBroadcast::new).orElse(null);
        this.registry = registry;
        this.clazz = clazz;
        this.queue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newFixedThreadPool(1);
        BatchingOption.newBuilder(this).done();
    }



    /** Start batching option flow.
     * @see BatchingOption
     * */
    public BatchingOption.Builder<E> batch(){
        return BatchingOption.newBuilder(this);
    }


    /** Set batching options */
    public void setBatchingOption(BatchingOption<E> batchingOption) {
        this.batchingOption = batchingOption;
    }

    /** When this is greater than 1, the fan-out flow will build.
     * - N FQueue child objects will be build based on fanOut number.
     * - This instance will act as dispatcher, send objects to FQueue child in round-robin mode.
     * */
    public FQueue<E> fanOut(int num){
        fanOut = num;
        return this;
    }


    /** Start consuming queue
     * - N FQueue child objects will be build based on fanOut number.
     * - This instance will act as dispatcher, send objects to FQueue child in round-robin mode.
     * */
    public FQueue<E> consume(FQueueConsumerFactory<E> factory){

        if(fanOut == 1){
            executorService.submit(Objects.requireNonNull(consumeBatching(factory.build())));
        }else{
            IntStream.range(0, fanOut)
                    .forEach(i -> {
                        FQueue<E> child = new FQueue<>(clazz, null);
                        child.setBatchingOption(this.batchingOption);
                        child.withNoopHandler(noopHandler);
                        child.withRunningExceptionHandler(exceptionHandler);
                        child.consume(factory);
                        childFQueues.add(child);
                    });
            executorService.submit(Objects.requireNonNull(consumeDispatching()));
        }

        return this;
    }


    /** Input class  */
    public Class<E> getInputClass() { return this.clazz; }

    /** Instance queue  */
    public BlockingQueue<E> getQueue(){
        return this.queue;
    }

    /** Get stats  */
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



    /** When Executor thread terminates or is interrupted an exception will be fired, this will react.
     * By default it prints the stacktrace.
     * */
    public FQueue<E> withRunningExceptionHandler(Consumer<Exception> handler){
        exceptionHandler = handler;
        return this;
    }

    /** NOOP handler, this is fired when flushing collection have 0 elements.
     * */
    public FQueue<E> withNoopHandler(Consumer<Void> handler){
        noopHandler = handler;
        return this;
    }


    /** Destroy executor service
     * */
    public void destroy(){
        executorService.shutdownNow();
        if(fanOut != 1){
            childFQueues.forEach(FQueue::destroy);
        }
    }

    /** Destroy executor service and await
     * */
    public void destroyAndAwait(Integer timeout, TimeUnit timeUnit) throws InterruptedException{
        if(fanOut != 1){
            childFQueues.forEach(c -> {
                try {
                    c.destroyAndAwait(timeout, timeUnit);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        executorService.shutdownNow();
        executorService.awaitTermination(timeout, timeUnit);
    }


    private Runnable consumeBatching(FQueueConsumer<E> consumer){

        final int maxSize = batchingOption.getChunkSize();
        final TimeUnit timeUnit = batchingOption.getFlushTimeUnit();
        final int timeout = batchingOption.getFlushTimeout();
        final FQueueElementLengthFunction<E> lengthFunction = batchingOption.getLengthFunction();


        return () -> {

            // This is thread safe.
            while (!Thread.currentThread().isInterrupted())
            {
                try
                {
                    int threshold = 0;
                    E elm;
                    List<E> collection = new ArrayList<>();
                    long deadline = System.nanoTime() + timeUnit.toNanos(timeout);
                    BatchReason reason = BatchReason.MAX_ELEMENT_REACHED;

                    do{
                        elm = queue.poll(1, TimeUnit.NANOSECONDS);

                        if (elm == null) { // not enough elements immediately available; will have to poll
                            elm = queue.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
                            if (elm == null) {
                                reason = BatchReason.TIME_FLUSH;
                                break; // we already waited enough, and there are no more elements in sight
                            }
                            received.incrementAndGet();
                            collection.add(elm);
                            threshold += lengthFunction.apply(elm);
                        }else{
                            received.incrementAndGet();
                            collection.add(elm);
                            threshold += lengthFunction.apply(elm);
                        }
                    }
                    while (threshold < maxSize);

                    if(collection.size() > 0){
                        batched.addAndGet(collection.size());
                        consumer.consume(broadcaster, reason, collection);
                    }else{
                        Optional.ofNullable(noopHandler).ifPresent(handler -> handler.accept(null));
                    }

                }
                catch(Exception ex) {
                    exceptionHandler.accept(ex);
                }
            }



        };
    }


    private Runnable consumeDispatching(){


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
                    exceptionHandler.accept(ex);
                }
            }

        };
    }

}
