package io.fulmicotone.fqueue.examples.function;

import io.fulmicotone.fqueue.examples.models.DomainCount;
import io.fulmicotone.fqueue.examples.models.PageView;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FromPageViewsToDomainCount implements Function<List<PageView>, List<DomainCount>> {
    @Override
    public List<DomainCount> apply(List<PageView> pageViews) {
        return pageViews.stream()
                .map(pv -> new DomainExtractor().apply(pv.getUrl()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()))
                .entrySet()
                .stream()
                .map(e -> new DomainCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
