package org.orkim;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

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
        discoverInZookeeper(delegatePort);

    }

    private static void startRestServer(Integer workerPort) {
        System.setProperty("org.jboss.resteasy.port", workerPort.toString());
        server = new UndertowJaxrsServer().start();
        server.deploy(SeniorService.class);
    }

    private static void discoverInZookeeper(Integer workerPort) {
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

        serviceProvider = serviceDiscovery.serviceProviderBuilder().serviceName("junior").build();

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
        String response;
        try(BufferedReader in = new BufferedReader(
                new InputStreamReader(new URL(address + "/junior/services/go").openStream()))) {
         response = in.readLine();

        } catch (IOException exception) {
            log.error("Could not read from url: " + address);
            return "";
        }

        log.error("Call call call: " + response);
        return response;
    }
}
