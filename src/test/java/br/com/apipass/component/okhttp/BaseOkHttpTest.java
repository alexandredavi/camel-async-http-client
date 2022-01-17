package br.com.apipass.component.okhttp;

import org.apache.camel.BindToRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeAll;

import java.util.Properties;

public class BaseOkHttpTest extends CamelTestSupport {

    private static int port;

    @BeforeAll
    public static void initPort() {
        port = AvailablePortFinder.getNextAvailable();
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();
        context.getPropertiesComponent().setLocation("ref:prop");
        return context;
    }

    @BindToRegistry("prop")
    public Properties addProperties() {
        Properties prop = new Properties();
        prop.setProperty("port", "" + getPort());
        return prop;
    }

    protected int getPort() {
        return port;
    }

    protected boolean isHttps() {
        return false;
    }

    protected String getProtocol() {
        String protocol = "http";
        if (isHttps()) {
            protocol = protocol + "s";
        }

        return protocol;
    }

    protected String getOkHttpEndpointUri() {
        return "okhttp:" + getProtocol() + "://localhost:{{port}}/foo";
    }

    protected String getTestServerEndpointUri() {
        return "jetty:" + getTestServerEndpointUrl();
    }

    protected String getTestServerEndpointUrl() {
        return getProtocol() + "://localhost:{{port}}/foo";
    }
}
