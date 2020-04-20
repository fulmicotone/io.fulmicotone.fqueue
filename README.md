# FQueue - Simple thread-safe queue consumer
&nbsp;[![Build Status](https://travis-ci.org/fulmicotone/io.fulmicotone.fqueue.svg?branch=master)](https://travis-ci.org/fulmicotone/io.fulmicotone.fqueue) &nbsp;[![](https://jitpack.io/v/fulmicotone/io.fulmicotone.fqueue.svg)](https://jitpack.io/#fulmicotone/io.fulmicotone.fqueue) &nbsp; [![Coverage Status](https://coveralls.io/repos/github/fulmicotone/io.fulmicotone.fqueue/badge.svg?branch=master)](https://coveralls.io/github/fulmicotone/io.fulmicotone.fqueue?branch=master)


### Goal - Build a simple thread-safe queue consumer in pure java8

- Makes simple handling different data flows.
- Expose one simple interface easy to implement and thread-safe (because only one thread will execute the user code)
- Micro-batches elements based on count or custom accumulators with periodic flushing timeout.
- Uses all the power of the machine/instance (with java FixedThreadPool based on machine cores)


### How is designed:

- Every FQueue process objects of certain class applying a user-defined task.
- FQueue could be single thread or fan-out in multi thread.
- FanOut is supported in order to process with multiples core a stream of objects dispatched in round-robin mode.
- FQueueRegistry is the class which maintains all FQueue's objects and is able to send datas to them.


#### Usage

##### Instatiate only one registry!
Make sure that you have only one instance of FQueueRegistry in your project.
If using Spring please annotate it as @Bean

```java
        FQueueRegistry registry = new FQueueRegistry();
```



##### Standard consuming
Simple consuming, by default consumes 1 element at time.

```java
        registry.buildFQueue(String.class)
                .consume(() -> (operations, elms) -> System.out.println("CASE 1 - Elements batched are: "+elms.size()));

```


##### Batching consuming
Consumes data and aggregate them in chunks of 5 elements.
If data are less than chunk size it will flush them every 1 second.
```java
        registry.buildFQueue(String.class)
                .batch()
                .withChunkSize(5)
                .withFlushTimeout(1)
                .withFlushTimeUnit(TimeUnit.SECONDS)
                .done()
                .consume(() -> (operations, elms) -> System.out.println("CASE 2 - Elements batched are: "+elms.size()));
```


##### Batching consuming with custom accumulation function
Batching consuming with a custom accumulation function.
it consumes data and aggregate them in chunks of 15 bytes elements (2 "Sample" string will fit in).
If data are less than chunk size it will flush them every 1 second.
```java
        registry.buildFQueue(String.class)
                .batch()
                .withChunkSize(5)
                .withFlushTimeout(1)
                .withFlushTimeUnit(TimeUnit.SECONDS)
                .done()
                .consume(() -> (operations, elms) -> System.out.println("CASE 2 - Elements batched are: "+elms.size()));
```


##### Batching consuming with fanOut (parallelism)
fanOut(3) creates three nested FQueue, while the first defined acts as round-robin dispatcher
Every nested FQueue consume data and aggregate them in chunks of 5 elements.
If data are less than chunk size every nested FQueue will flush them every 1 second.
```java
        registry.buildFQueue(String.class)
                .fanOut(3)
                .batch()
                .withChunkSize(5)
                .withFlushTimeout(1)
                .withFlushTimeUnit(TimeUnit.SECONDS)
                .done()
                .consume(() -> (operations, elms) -> {

                    System.out.println("CASE 4 - currentThread is: "+Thread.currentThread().getName()+ " - Elements batched are: "+elms.size());
                });
```


Please checkout all these examples under:
https://github.com/fulmicotone/io.fulmicotone.fqueue/blob/master/src/test/java/io/fulmicotone/fqueue/examples/GithubExample.java