package org.orkim;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class Junior {

    @Stub
    public static void go() {
        stubFor(
                get(urlEqualTo("/junior/services/go"))
                        //.withHeader("Content-Type", equalTo("application/vnd.com.ofg.twitter-places-analyzer.v1+json"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody("<response>Success from Junior Stub</response>")
                        ));

    }
}
