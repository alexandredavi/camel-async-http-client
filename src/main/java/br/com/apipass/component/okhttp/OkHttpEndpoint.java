package br.com.apipass.component.okhttp;

import okhttp3.OkHttpClient;
import org.apache.camel.*;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.util.ObjectHelper;

import java.net.URI;

/**
 * To call external HTTP services using <a href="https://github.com/square/okhttp">OkHttp Client</a>.
 */
@UriEndpoint(scheme = "okhttp", title = "OkHttp", firstVersion = "3.14.0", syntax = "okhttp:httpUri", producerOnly = true, category = Category.HTTP, lenientProperties = true)
public class OkHttpEndpoint extends DefaultEndpoint implements AsyncEndpoint {

    private OkHttpClient client;

    /**
     * The http URI
     */
    @UriPath @Metadata(required = true)
    private final URI httpUri;

    /**
     * The http binding
     */
    @UriParam(label = "advanced")
    private final OkHttpBinding binding;

    public OkHttpEndpoint(String uri, OkHttpComponent component, URI httpUri, OkHttpBinding binding) {
        super(uri, component);
        this.httpUri = httpUri;
        this.binding = binding;
    }

    @Override
    public Producer createProducer() {
        ObjectHelper.notNull(client, "HttpClient", this);
        ObjectHelper.notNull(httpUri, "HttpUri", this);
        return new OkHttpProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        throw new UnsupportedOperationException("This component does not support consuming from this endpoint");
    }

    @Override
    public boolean isLenientProperties() {
        return true;
    }

    public OkHttpBinding getBinding() {
        return binding;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public URI getHttpUri() {
        return httpUri;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (client == null) {
            client = new OkHttpClient.Builder().build();
        }
    }
}
