package io.fulmicotone.fqueue.examples;

import io.fulmicotone.fqueue.FQueueRegistry;
import io.fulmicotone.fqueue.examples.function.FromPageViewsToDomainCount;
import io.fulmicotone.fqueue.examples.models.DomainCount;
import io.fulmicotone.fqueue.examples.models.Intent;
import io.fulmicotone.fqueue.examples.models.PageView;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class GithubExample {


    @Test
    public void test() throws InterruptedException {

        int flushTimeoutInMilliSeconds = 500;
        int resultTimeoutInMilliSeconds = 2_000;
        int chunkSize = 5;

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

    }
}
