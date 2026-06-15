package com.base.client;

import com.base.client.properties.ClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * WebClient 过滤器：发请求前自动透传上下文请求头到下游。
  * @author base
 * @since 2026-06-11
 */
@RequiredArgsConstructor
public class ClientTokenFilter implements ExchangeFilterFunction {

    private final ClientProperties properties;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        ClientRequest modified = ClientRequest.from(request)
                .headers(h -> ContextPropagator.propagate(h, properties.getPropagateHeaders()))
                .build();
        return next.exchange(modified);
    }
}
