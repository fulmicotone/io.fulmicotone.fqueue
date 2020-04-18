package io.fulmicotone.fqueue.examples.function;


import java.net.URI;
import java.util.Optional;
import java.util.function.Function;


public class DomainExtractor implements Function<String, Optional<String>> {

    @Override
    public Optional<String> apply(String uri) {
        URI u = URI.create(uri);
        return Optional.ofNullable(u.getHost());
    }

}
