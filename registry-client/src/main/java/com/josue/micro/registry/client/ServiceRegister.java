package com.josue.micro.registry.client;

import com.josue.micro.registry.client.discovery.ServiceConfigHolder;
import com.josue.micro.registry.client.ws.ServiceClientEndpoint;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Josue on 16/06/2016.
 */
@ApplicationScoped
public class ServiceRegister implements Runnable {

    private static final Logger logger = Logger.getLogger(ServiceRegister.class.getName());

    private static final String REGISTRY_PATH = "registry";
    private static final String REGISTRY_URL_KEY = "registry.url";

    private static final Object LOCK = new Object();
    private static final AtomicInteger retryCounter = new AtomicInteger();
    private static final int MAX_RETRY = 999; //fix your system bro !
    private static final int RETRY_INTERVAL = 10;//in seconds
    private Session session;

    public static boolean shutdownSignal = false;

    @Inject
    private ServiceStore store;

    @Resource
    private ManagedScheduledExecutorService mses;

    @PostConstruct
    public void init() {
        synchronized (LOCK) {
            logger.log(Level.INFO, ":: Initialising service register ::");
            if (ServiceConfigHolder.isInitialised()) {
                register();
            } else {
                logger.log(Level.INFO, ":: No services found ::");
            }
        }
    }

    public void register() {
        deregister();

        synchronized (LOCK) {
            if (!shutdownSignal && (session == null || !session.isOpen())) {
                retryCounter.set(0);
                mses.schedule(this, 5, TimeUnit.SECONDS);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        synchronized (LOCK) {
            shutdownSignal = true;
        }
        deregister();
    }

    public void deregister() {
        synchronized (LOCK) {
            try {
                if (session != null && session.isOpen()) {
                    logger.log(Level.INFO, ":: Closing WS session ::");
                    session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Service disconnected"));
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, ":: Error while closing the session ::", e);
            }
        }
    }

    private String getRegistryUrl() {
        logger.log(Level.INFO, ":: Loading registry URL ::");
        String registryUrl = System.getProperty(REGISTRY_URL_KEY);
        if (registryUrl == null || registryUrl.isEmpty()) {
            throw new IllegalStateException(":: Could not find environment property '" + REGISTRY_URL_KEY + "' ::");
        }
        String urlSeparator = registryUrl.endsWith("/") ? "" : "/";
        return registryUrl + urlSeparator + REGISTRY_PATH;
    }

    @Override
    public void run() {
        synchronized (LOCK) {
            try {
                String registryUrl = getRegistryUrl();

                WebSocketContainer container = ContainerProvider.getWebSocketContainer();

                logger.log(Level.INFO, ":: Trying to connect to {0}, attempt {1} of {2} ::", new Object[]{registryUrl, retryCounter.incrementAndGet(), MAX_RETRY});

                ServiceClientEndpoint endpoint = new ServiceClientEndpoint(store, this);
                session = container.connectToServer(endpoint, new URI(registryUrl));

                logger.log(Level.INFO, ":: Connected ! ::", session.getId());

            } catch (Exception e) {
                logger.log(Level.WARNING, ":: Could not connect to the registry, retrying in {0}s ::", RETRY_INTERVAL);
                logger.log(Level.SEVERE, "Connection failure, reason: ", e);
                if (retryCounter.intValue() >= MAX_RETRY) {
                    logger.log(Level.WARNING, ":: Max attempt exceeded ::", RETRY_INTERVAL);
                } else {
                    mses.schedule(this, RETRY_INTERVAL, TimeUnit.SECONDS);
                }
            }
        }
    }
}
