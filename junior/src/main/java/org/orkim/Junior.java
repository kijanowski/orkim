package org.orkim;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/services")
public class Junior {

    private static String workerName;
    private static Integer workerPort;
    private static UndertowJaxrsServer server;

    public static void main(String[] args) {

        if (args.length != 2) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        workerName = args[0];
        workerPort = Integer.parseInt(args[1]);

        startRestServer(workerPort);
        registerInZookeeper(workerPort);

        // TODO logger
        System.out.println("Worker " + workerName + " started on port $workerPort");
    }

    private static void startRestServer(Integer workerPort) {
        System.setProperty("org.jboss.resteasy.port", workerPort.toString());
        server = new UndertowJaxrsServer().start();
        server.deploy(JuniorService.class);
    }

    private static void registerInZookeeper(Integer workerPort) {
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("localhost:2181", new RetryNTimes(5, 1000));
        curatorFramework.start();

        ServiceInstance<Void> serviceInstance;
        try {
            serviceInstance = ServiceInstance.<Void>builder()
                    .uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                    .address(("localhost"))
                    .port(workerPort)
                    .name("junior")
                    .build();
        } catch (Exception e) {
            // TODO logger
            System.out.println("Could not create Service Instance.");
            return;
        }


        try {
            ServiceDiscoveryBuilder.builder(Void.class)
                    .basePath("orkim")
                    .client(curatorFramework)
                    .thisInstance(serviceInstance)
                    .build()
                    .start();
        } catch (Exception e) {
            // TODO logger
            System.out.println("Could not start Service Discovery.");
            return;
        }
    }

    @GET
    @Path("/go")
    public String go() {
        //TODO logger
        System.out.println("Go go go!");
        return "Finished";
    }

}
