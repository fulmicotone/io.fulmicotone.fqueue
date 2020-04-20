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


### Usage

#### Instatiate only one registry!
- Make sure that you have only one instance of FQueueRegistry in your project.
- If using Spring please annotate it as @Bean

```java
        FQueueRegistry registry = new FQueueRegistry();
```



#### Standard consuming
- Simple consuming, by default consumes 1 element at time.

```java
        registry.buildFQueue(String.class)
                .consume(() -> (broadcaster, elms) -> System.out.println("CASE 1 - Elements batched are: "+elms.size()));

```


#### Batching consuming
- Consumes data and aggregate them in chunks of 5 elements.
- If data are less than chunk size it will flush them every 1 second.
```java
        registry.buildFQueue(String.class)
                .batch()
                .withChunkSize(5)
                .withFlushTimeout(1)
                .withFlushTimeUnit(TimeUnit.SECONDS)
                .done()
                .consume(() -> (broadcaster, elms) -> System.out.println("CASE 2 - Elements batched are: "+elms.size()));
```


#### Batching consuming with custom accumulation function
- Batching consuming with a custom accumulation function.
- It consumes data and aggregate them in chunks of 15 bytes elements (2 "Sample" string will fit in).
- If data are less than chunk size it will flush them every 1 second.
```java
        registry.buildFQueue(String.class)
                .batch()
                .withChunkSize(5)
                .withFlushTimeout(1)
                .withFlushTimeUnit(TimeUnit.SECONDS)
                .done()
                .consume(() -> (broadcaster, elms) -> System.out.println("CASE 2 - Elements batched are: "+elms.size()));
```


#### Batching consuming with fanOut (parallelism)
- fanOut(3) creates three nested FQueue, while the first defined acts as round-robin dispatcher.
- Every nested FQueue consume data and aggregate them in chunks of 5 elements.
- If data are less than chunk size every nested FQueue will flush them every 1 second.
```java
        registry.buildFQueue(String.class)
                .fanOut(3)
                .batch()
                .withChunkSize(5)
                .withFlushTimeout(1)
                .withFlushTimeUnit(TimeUnit.SECONDS)
                .done()
                .consume(() -> (broadcaster, elms) -> {

                    System.out.println("CASE 4 - currentThread is: "+Thread.currentThread().getName()+ " - Elements batched are: "+elms.size());
                });
```

#### Push datas into FQueue
- Use the registry to push datas into FQueue.
- Registry will send your object only to FQueue's which consume it's class.
```java
        registry.sendBroadcast("Sample");
        registry.sendBroadcast(2);
        registry.sendBroadcast(new AnyObjectYouWant());
```
- If multiple FQueue consumes the same class, every object will be sent to them.
```java
        FQueue<String> one = registry.buildFQueue(String.class)
                .consume(() -> (broadcaster, elms) -> System.out.println("ONE - Elements received are: " + elms.size()));

        FQueue<String> two = registry.buildFQueue(String.class)
                .consume(() -> (broadcaster, elms) -> System.out.println("TWO - Elements batched are: " + elms.size()));
        
        /** This will received by one and two  */
        registry.sendBroadcast("Sample");
```

- In the case you have multiple FQueue that receive the same class, and you want to send an object to a specific FQueue, you need to push into it's queue. 
```java
        FQueue<String> one = registry.buildFQueue(String.class)
                .consume(() -> (broadcaster, elms) -> System.out.println("ONE - Elements received are: " + elms.size()));

        FQueue<String> two = registry.buildFQueue(String.class)
                .consume(() -> (broadcaster, elms) -> System.out.println("TWO - Elements batched are: " + elms.size()));

        /** This will received by one  */
        one.getQueue().add("Sample");
```

#### Push data to another FQueue from consuming function
- Sometimes you want to pass datas between FQueue
- It's possible by calling the "brodacaster" object injected into the consuming function

Please checkout all these examples under:
https://github.com/fulmicotone/io.fulmicotone.fqueue/blob/master/src/test/java/io/fulmicotone/fqueue/examples/GithubExample.java