package br.com.apipass.component.okhttp;

import br.com.apipass.component.okhttp.helper.OkHttpHelper;
import okhttp3.*;
import org.apache.camel.CamelExchangeException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.support.ExchangeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class DefaultOkHttpBinding implements OkHttpBinding {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public Request prepareRequest(OkHttpEndpoint endpoint, Exchange exchange) throws CamelExchangeException {
        Request.Builder builder = new Request.Builder();
        try {
            String url = OkHttpHelper.createURL(exchange, endpoint);
            URI uri = OkHttpHelper.createURI(exchange, url, endpoint);
            // get the url from the uri
            url = uri.toASCIIString();

            log.trace("Setting url {}", url);
            builder.url(url);
        } catch (Exception e) {
            throw new CamelExchangeException("Error creating URL", exchange, e);
        }

        log.trace("Populating headers");
        populateHeaders(exchange, builder);

        String method = extractMethod(exchange);
        log.trace("Setting method {}", method);
        builder.method(method, generateBody(exchange));

        return builder.build();
    }

    @Override
    public void onComplete(OkHttpEndpoint endpoint, Exchange exchange, Request request, Response response) throws Exception {

        // Honor the character encoding
        String contentType = exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class);
        if (contentType != null) {
            // find the charset and set it to the Exchange
            OkHttpHelper.setCharsetFromContentType(contentType, exchange);
        }
        if (response.code() >= 100 && response.code() < 300) {
            // only populate response for OK response
            populateResponse(exchange, Objects.requireNonNull(response.body()));
        } else {
            // operation failed so populate exception to throw
            throw populateHttpOperationFailedException(exchange, request, response);
        }
    }

    private Exception populateHttpOperationFailedException(Exchange exchange, Request request, Response response) throws Exception {
        Exception answer;
        // make a defensive copy of the response body in the exception, so it's detached from the cache
        String copy = null;
        if (response.body() != null) {
            copy = exchange.getContext().getTypeConverter().convertTo(String.class, exchange, Objects.requireNonNull(response.body()).string());
        }
        Map<String, String> headers = extractResponseHeaders(exchange);

        if (response.code() >= 300 && response.code() < 400) {
            String redirectLocation = exchange.getIn().getHeader("Location", String.class);
            if (redirectLocation != null) {
                answer = new OkHttpOperationFailedException(request.url().toString(), response.code(), response.message(), redirectLocation, headers, copy);
            } else {
                // no redirect location
                answer = new OkHttpOperationFailedException(request.url().toString(), response.code(), response.message(), null, headers, copy);
            }
        } else {
            // internal server error (error code 500)
            answer = new OkHttpOperationFailedException(request.url().toString(), response.code(), response.message(), null, headers, copy);
        }

        return answer;
    }

    private Map<String, String> extractResponseHeaders(Exchange exchange) {
        Map<String, String> answer = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : exchange.getIn().getHeaders().entrySet()) {
            String key = entry.getKey();
            String value = exchange.getContext().getTypeConverter().convertTo(String.class, entry.getValue());
            if (value != null) {
                answer.put(key, value);
            }
        }
        return answer;
    }

    private void populateResponse(Exchange exchange, ResponseBody body) throws CamelExchangeException {
        try {
            exchange.getIn().setBody(body.string());
        } catch (IOException e) {
            throw new CamelExchangeException("Error populatinng body from response", exchange, e);
        }
        exchange.getIn().setHeader(Exchange.CONTENT_LENGTH, body.contentLength());
    }

    protected String extractMethod(Exchange exchange) {
        // prefer method from header
        String method = exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class);
        if (method != null) {
            return method;
        }

        // if there is a body then do a POST otherwise a GET
        boolean hasBody = exchange.getIn().getBody() != null;
        return hasBody ? "POST" : "GET";
    }

    protected void populateHeaders(Exchange exchange, Request.Builder builder) {
        // propagate headers as HTTP headers
        for (Map.Entry<String, Object> entry : exchange.getIn().getHeaders().entrySet()) {
            String headerValue = exchange.getIn().getHeader(entry.getKey(), String.class);
            builder.header(entry.getKey(), headerValue);
        }
    }

    protected RequestBody generateBody(Exchange exchange) throws CamelExchangeException {
        Message in = exchange.getIn();
        if (in.getBody() == null) {
            return null;
        }

        String contentType = ExchangeHelper.getContentType(exchange);
        // Set default content-type as text/plain
        contentType = contentType != null ? contentType : "text/plain";
        RequestBody body = in.getBody(RequestBody.class);
        String charset = ExchangeHelper.getCharsetName(exchange, false);

        if (body == null) {
            try {
                Object data = in.getBody();
                if (data != null) {
                   if (data instanceof String) {
                        if (charset != null) {
                            body = RequestBody.create(((String) data).getBytes(charset), MediaType.parse(contentType));
                        } else {
                            body = RequestBody.create(((String) data).getBytes(), MediaType.parse(contentType));
                        }
                    }
                }
            } catch (UnsupportedEncodingException e) {
                throw new CamelExchangeException("Error creating RequestBody from message body", exchange, e);
            }
        }
        return body;
    }
}
