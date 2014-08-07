package org.orkim;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/services")
public class Senior {

    private static Integer delegatePort;
    private static UndertowJaxrsServer server;
    private static ServiceProvider serviceProvider;

    private static Logger log = LoggerFactory.getLogger(Senior.class);

    public static void main(String[] args) {

        if (args.length != 1) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        delegatePort = Integer.parseInt(args[0]);

        startRestServer(delegatePort);
        discoverInZookeeper();

    }

    private static void startRestServer(Integer workerPort) {
        System.setProperty("org.jboss.resteasy.port", workerPort.toString());
        server = new UndertowJaxrsServer().start();
        server.deploy(SeniorService.class);
    }

    private static void discoverInZookeeper() {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("localhost:2181", new RetryNTimes(5, 1000));
        curatorFramework.start();

        ServiceDiscovery<Void> serviceDiscovery = ServiceDiscoveryBuilder.builder(Void.class)
                .basePath("orkim")
                .client(curatorFramework)
                .build();

        try {
            serviceDiscovery.start();
        } catch (Exception e) {
            log.error("Discovery failed.");
            return;
        }

        serviceProvider = serviceDiscovery.serviceProviderBuilder()
                .serviceName("junior")
                .providerStrategy(new RoundRobinStrategy<>())
                .build();

        try {
            serviceProvider.start();
        } catch (Exception e) {
            log.error("Provider failed.");
            return;
        }
        log.info("Delegate started on port " + delegatePort);
    }

    @GET
    @Path("/delegate")
    public String delegate() {

        ServiceInstance instance;

        try {
            instance = serviceProvider.getInstance();
        } catch (Exception e) {
            log.error("Could not find service instance.");
            return "";
        }


        String address = instance.buildUriSpec();
        Client client = ClientBuilder.newClient();

        Response response = client.target(address)
                .path("/junior/services/go")
                //.queryParam("greeting", "Hi World!")
                .request(MediaType.TEXT_HTML)
                //.header("some-header", "true")
                .get(Response.class);
        log.debug("Gor response with status: " + response.getStatus());
        String output = response.readEntity(String.class);
        log.info("Call call call: " + output);
        return output;
    }
}
