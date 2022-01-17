package br.com.apipass.component.okhttp;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.util.URISupport;
import org.apache.camel.util.UnsafeUriCharactersEncoder;

import java.net.URI;
import java.util.Map;

@Component("okhttp")
public class OkHttpComponent extends DefaultComponent {

    @Metadata(label = "advanced")
    private OkHttpBinding binding;

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        String httpRemainingUri = UnsafeUriCharactersEncoder.encodeHttpURI(remaining);
        URI httpUri = URISupport.createRemainingURI(new URI(httpRemainingUri), parameters);
        OkHttpBinding binding = getBinding();
        return new OkHttpEndpoint(uri, this, httpUri, binding);
    }

    public OkHttpBinding getBinding() {
        if (binding == null) {
            binding = new DefaultOkHttpBinding();
        }
        return binding;
    }
}
