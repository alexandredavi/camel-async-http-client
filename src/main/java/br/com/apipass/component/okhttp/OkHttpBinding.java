package br.com.apipass.component.okhttp;

import okhttp3.Request;
import okhttp3.Response;
import org.apache.camel.CamelExchangeException;
import org.apache.camel.Exchange;

public interface OkHttpBinding {

    Request prepareRequest(OkHttpEndpoint endpoint, Exchange exchange) throws CamelExchangeException;

    void onComplete(OkHttpEndpoint endpoint, Exchange exchange, Request request, Response response) throws Exception;
}
