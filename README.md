# FQueue - Simple thread-safe queue consumer
&nbsp;[![Build Status](https://travis-ci.org/fulmicotone/io.fulmicotone.fqueue.svg?branch=master)](https://travis-ci.org/fulmicotone/io.fulmicotone.fqueue) &nbsp;[![](https://jitpack.io/v/fulmicotone/io.fulmicotone.fqueue.svg)](https://jitpack.io/#fulmicotone/io.fulmicotone.fqueue)


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


#### FQueue Architecture

![alt tag](https://raw.githubusercontent.com/fulmicotone/io.fulmicotone.fqueue/develop/res/drawioImg/fqueue_single.png)

#### FQueue with fan-out

![alt tag](https://raw.githubusercontent.com/fulmicotone/io.fulmicotone.fqueue/develop/res/drawioImg/round_robin.png)

#### FQueueRegistry

![alt tag](https://raw.githubusercontent.com/fulmicotone/io.fulmicotone.fqueue/develop/res/drawioImg/fqueue_registry.png)

### Installation:

Gradle, Maven etc.. from jitpack
https://jitpack.io/#fulmicotone/io.fulmicotone.fqueue



### Example

Imagine a pageview data stream coming from your website and you want to:
- Count every PageView request that comes from the same domain, sum them and printing result (just for fun).
- Transform one PageView object into a Intent object if some conditions occurs on database.
- Pass Intent objects to another stream that will save them on database of fired intents.

#### Example - services overview

![alt tag](https://raw.githubusercontent.com/fulmicotone/com.fulmicotone.qio/develop/misc/qio_example.jpg)

#### Example - services implementation
```java


        FQueueRegistry registry = new FQueueRegistry();


        List<Intent> intentStore = new ArrayList<>();
        Set<DomainCount> domainCounts = new HashSet<>();



        // Receive PageView and transform them in Domain counts
        registry.buildFQueue(PageView.class)
                .consume(100, 1_000, TimeUnit.MILLISECONDS, () -> ((operations, pageViews) -> {

                    domainCounts.addAll(new FromPageViewsToDomainCount()
                            .apply(pageViews));
                }));

        // Receive PageView and transform them in Intents
        registry.buildFQueue(PageView.class)
                .consume(chunkSize, flushTimeoutInMilliSeconds, TimeUnit.MILLISECONDS, () -> ((operations, pageViews) -> {

                    // Get random intent function for test
                    Function<PageView, Intent> fn = pageView -> new Intent(pageView.getUserId(), UUID.randomUUID().toString());

                    // Get intent for page views and broadcast them to Intent receiver.
                    pageViews.stream().map(fn).forEach(operations::sendBroadcast);

                }));


        // Intent receiver
        registry.buildFQueue(Intent.class)
                .consume(chunkSize, flushTimeoutInMilliSeconds, TimeUnit.MILLISECONDS, () -> ((operations, intents) -> {

                    // store intents somewhere, now just print them.
                    intentStore.addAll(intents);

                }));


        // Send fake objects
        for(int i = 0; i < 10; i++){
            PageView pv = new PageView("http://www.google.com", "uid"+i);
            PageView pv2 = new PageView("http://www.yahoo.com", "uid"+i);
            registry.sendBroadcast(pv);
            registry.sendBroadcast(pv2);
        }

        Thread.sleep(resultTimeoutInMilliSeconds);


        // Print stats
        System.out.println("Domain counts");
        domainCounts.forEach(System.out::println);

        System.out.println("Queue statuses");
        registry.getStatuses().forEach(System.out::println);


