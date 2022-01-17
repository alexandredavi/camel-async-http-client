package br.com.apipass.component.okhttp;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class OkHttpProducePostTest extends BaseOkHttpTest {

    @Test
    public void testOkHttpProduce() throws Exception {
        getMockEndpoint("mock:result").expectedBodiesReceived("Bye World");

        template.sendBody("direct:start", "World");

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testOkHttpProduceGetHeader() throws Exception {
        getMockEndpoint("mock:result").expectedBodiesReceived("Bye World");

        template.sendBodyAndHeader("direct:start", "World", Exchange.HTTP_METHOD, "POST");

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {

            @Override
            public Set<String> updateRoutesToCamelContext(CamelContext context) {
                return null;
            }

            @Override
            public void configure() {
                from("direct:start")
                        .to(getOkHttpEndpointUri())
                        .to("mock:result");

                from(getTestServerEndpointUri())
                        .transform(simple("Bye ${body}"));
            }
        };
    }
}
