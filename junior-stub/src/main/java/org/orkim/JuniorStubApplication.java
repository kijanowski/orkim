package org.orkim;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class JuniorStubApplication {

    private static Logger log = LoggerFactory.getLogger(JuniorStubApplication.class);

    public static void main(String[] args) {

        if (args.length != 4) {
            throw new IllegalArgumentException("Invalid Arguments");
        }

        String zooKeeperBasePath = args[0];
        int zookeperPort = Integer.parseInt(args[1]);
        String stubName = args[2];
        int stubPort = Integer.parseInt(args[3]);


        registerInZooKeeper(zooKeeperBasePath, zookeperPort, stubName, stubPort);
        startWireMock(stubPort);
        registerTestStubs();
        log.info("JuniorStub started.");

    }

    private static void registerTestStubs() {

        Reflections reflections = new Reflections(
                new ConfigurationBuilder().setUrls(
                        ClasspathHelper.forPackage("org.orkim") ).setScanners(
                        new MethodAnnotationsScanner() ) );
        Set<Method> methods = reflections.getMethodsAnnotatedWith(Stub.class);
        methods.forEach((method) -> {
            try {
                method.invoke(null);
            } catch (IllegalAccessException e) {
                log.error("Method Access Error: " + method.getName());
            } catch (InvocationTargetException e) {
                log.error("Method Invocation Error: " + method.getName());
            }
            log.info("Stub method " + method.getName() + " called.");
        });


    }

    private static void startWireMock(int stubPort) {

        WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(stubPort));
        wireMockServer.start();
        WireMock.configureFor("localhost", stubPort);

    }

    private static void registerInZooKeeper(String zooKeeperBasePath, int zookeperPort, String stubName, int stubPort) {

        ServiceInstance serviceInstance;

        try {

            serviceInstance = ServiceInstance.builder().uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                    .address("localhost")
                    .port(stubPort)
                    .name(stubName)
                    .build();
        } catch (Exception e) {
            log.error("Could not build service instance.");
            return;
        }

        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("localhost:" + zookeperPort, new RetryNTimes(5, 1000));
        curatorFramework.start();


        try {
            ServiceDiscoveryBuilder.builder(Void.class)
                    .basePath(zooKeeperBasePath)
                    .client(curatorFramework)
                    .thisInstance(serviceInstance)
                    .build()
                    .start();
        } catch (Exception e) {
            log.error("Could not start service discovery");
        }

    }


}
