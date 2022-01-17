package br.com.apipass.component.okhttp.helper;

import br.com.apipass.component.okhttp.OkHttpEndpoint;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeExchangeException;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.URISupport;
import org.apache.camel.util.UnsafeUriCharactersEncoder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

public class OkHttpHelper {

    private OkHttpHelper() {}

    public static String createURL(Exchange exchange, OkHttpEndpoint endpoint) throws URISyntaxException, UnsupportedEncodingException {
        String url = doCreateURL(exchange, endpoint);
        return URISupport.normalizeUri(url);
    }

    private static String doCreateURL(Exchange exchange, OkHttpEndpoint endpoint) {
        String uri = endpoint.getHttpUri().toASCIIString();

        // resolve placeholders in uri
        try {
            uri = exchange.getContext().resolvePropertyPlaceholders(uri);
        } catch (Exception e) {
            throw new RuntimeExchangeException("Cannot resolve property placeholders with uri: " + uri, exchange, e);
        }

        // append HTTP_PATH to HTTP_URI if it is provided in the header
        String path = exchange.getIn().getHeader(Exchange.HTTP_PATH, String.class);
        if (path != null) {
            if (path.startsWith("/")) {
                URI baseURI;
                String baseURIString = exchange.getIn().getHeader(Exchange.HTTP_BASE_URI, String.class);
                try {
                    if (baseURIString == null) {
                        if (exchange.getFromEndpoint() != null) {
                            baseURIString = exchange.getFromEndpoint().getEndpointUri();
                        } else {
                            // will set a default one for it
                            baseURIString = "/";
                        }
                    }
                    baseURI = new URI(baseURIString);
                    String basePath = baseURI.getPath();
                    if (path.startsWith(basePath)) {
                        path = path.substring(basePath.length());
                        if (path.startsWith("/")) {
                            path = path.substring(1);
                        }
                    } else {
                        throw new RuntimeExchangeException("Cannot analyze the Exchange.HTTP_PATH header, due to: cannot find the right HTTP_BASE_URI", exchange);
                    }
                } catch (Throwable t) {
                    throw new RuntimeExchangeException("Cannot analyze the Exchange.HTTP_PATH header, due to: " + t.getMessage(), exchange, t);
                }
            }
            if (path.length() > 0) {
                // make sure that there is exactly one "/" between HTTP_URI and
                // HTTP_PATH
                if (!uri.endsWith("/")) {
                    uri = uri + "/";
                }
                uri = uri.concat(path);
            }
        }

        // ensure uri is encoded to be valid
        uri = UnsafeUriCharactersEncoder.encodeHttpURI(uri);

        return uri;
    }

    public static URI createURI(Exchange exchange, String url, OkHttpEndpoint endpoint) throws URISyntaxException {
        URI uri = new URI(url);
        // is a query string provided in the endpoint URI or in a header (header overrules endpoint)
        String queryString = exchange.getIn().getHeader(Exchange.HTTP_QUERY, String.class);
        if (queryString == null) {
            queryString = endpoint.getHttpUri().getRawQuery();
        }
        // We should user the query string from the HTTP_URI header
        if (queryString == null) {
            queryString = uri.getQuery();
        }
        if (queryString != null) {
            // need to encode query string
            queryString = UnsafeUriCharactersEncoder.encodeHttpURI(queryString);
            uri = URISupport.createURIWithQuery(uri, queryString);
        }
        return uri;
    }

    public static void setCharsetFromContentType(String contentType, Exchange exchange) {
        if (contentType != null) {
            exchange.setProperty(Exchange.CHARSET_NAME, IOHelper.getCharsetNameFromContentType(contentType));
        }
    }
}
