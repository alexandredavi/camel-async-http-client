package br.com.apipass.component.okhttp;

import okhttp3.*;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultAsyncProducer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OkHttpProducer extends DefaultAsyncProducer {

    private static final Logger LOG = LoggerFactory.getLogger(OkHttpProducer.class);

    private final OkHttpClient client;

    public OkHttpProducer(OkHttpEndpoint endpoint) {
        super(endpoint);
        this.client = endpoint.getClient();
    }

    @Override
    public OkHttpEndpoint getEndpoint() {
        return (OkHttpEndpoint) super.getEndpoint();
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        try {
            Request request = getEndpoint().getBinding().prepareRequest(getEndpoint(), exchange);
            LOG.debug("Executing request {}", request);
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("{} onResponse", exchange.getExchangeId());
                    }
                    try {
                        getEndpoint().getBinding().onComplete(getEndpoint(), exchange, request, response);
                    } catch (Exception e) {
                        exchange.setException(e);
                    } finally {
                        // signal we are done
                        callback.done(false);
                    }
                }

                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("{} onFailure {}", exchange.getExchangeId(), e);
                    }
                    exchange.setException(e);
                    callback.done(false);
                }
            });
            return false;
        } catch (Exception e) {
            exchange.setException(e);
            callback.done(true);
            return true;
        }

    }
}
