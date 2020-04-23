package io.fulmicotone.fqueue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FQueueRegistry {


    private final Map<Class, Set<FQueue<?>>> serviceMap = new ConcurrentHashMap<>();


    /** Build FQueue and register it into registry
     * */
    public <E>FQueue<E> buildFQueue(Class<E> clazz){
        FQueue<E> obj = new FQueue<>(clazz, this);
        registerObject(obj);
        return obj;
    }

    /** Get global stats of all registered FQueue */
    public List<String> getStatuses(){
       return serviceMap.values()
               .stream()
               .flatMap(Collection::stream)
               .flatMap(q -> q.getStats().stream()).collect(Collectors.toList());
    }

    /** Send element to all FQueue which listening for it's class */
    public <E>void sendBroadcast(E obj) {

        getMapSet(obj.getClass())
                .ifPresent(l -> l.forEach(FQueue -> {
                    FQueue<E> q = (FQueue<E>) FQueue;
                    q.getQueue().add(obj);
                }));
    }

    /** Send elements to all FQueue which listening for it's class */
    public <E>void sendBroadcast(Class<E> clazz, List<E> obj) {


        getMapSet(clazz)
                .ifPresent(l -> l.forEach(FQueue -> {
                    FQueue<E> q = (FQueue<E>) FQueue;
                    q.getQueue().addAll(obj);
                }));
    }


    private  <E> void registerObject(FQueue<E> obj) {

        Set<FQueue<?>> set = serviceMap.getOrDefault(obj.getInputClass(), new HashSet<>());
        set.add(obj);
        serviceMap.putIfAbsent(obj.getInputClass(), set);
    }

    private Optional<Set<FQueue<?>>> getMapSet(Class clazz)
    {
        Set<FQueue<?>> services = serviceMap.get(clazz);
        return Optional.ofNullable(services);
    }
}
