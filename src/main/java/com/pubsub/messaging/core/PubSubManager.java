package com.pubsub.messaging.core;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import com.pubsub.messaging.config.PubSubProperties;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PubSubManager implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(PubSubManager.class);

    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();
    private final Map<String, SubscriberContainer> subscribers = new ConcurrentHashMap<>();
    private final PubSubProperties props;
    private volatile boolean running = false;

    public PubSubManager(PubSubProperties props) {
        this.props = props;
        initPublishers();
    }

    // =====================================================
    // Publisher Initialization
    // =====================================================
    private void initPublishers() {
        props.getPublishers().forEach((name, config) -> publishers.computeIfAbsent(name, key -> {
            try {
                TopicName topicName =
                        TopicName.of(props.getProjectId(), config.getTopic());

                Publisher.Builder builder = Publisher.newBuilder(topicName);

                if (props.getEmulatorHost() != null) {
                    createEmulatorChannel(builder);
                }

                return builder.build();

            } catch (Exception e) {
                throw new RuntimeException("Error creating publisher: " + name, e);
            }
        }));
    }

    private void createEmulatorChannel(Publisher.Builder builder) {
        ManagedChannel channel =
                ManagedChannelBuilder
                        .forTarget(props.getEmulatorHost())
                        .usePlaintext()
                        .build();

        builder.setChannelProvider(
                        FixedTransportChannelProvider.create(
                                GrpcTransportChannel.create(channel)))
                .setCredentialsProvider(NoCredentialsProvider.create());
    }

    // =====================================================
    // Safe Publish
    // =====================================================
    public void publish(String publisherName, String payload) {

        if (!running) {
            throw new IllegalStateException("[AbstractorPubSub] PubSub Manager is not executing.");
        }

        Publisher publisher = publishers.get(publisherName);

        if (publisher == null) {
            throw new IllegalArgumentException("[AbstractorPubSub] Publisher not found: " + publisherName);
        }

        PubsubMessage message =
                PubsubMessage.newBuilder()
                        .setData(ByteString.copyFromUtf8(payload))
                        .build();

        ApiFuture<String> future = publisher.publish(message);

        ApiFutures.addCallback(future, new ApiFutureCallback<>() {

            @Override
            public void onFailure(Throwable t) {
                log.error("[AbstractorPubSub] Failed to publish message. Publisher={}", publisherName, t);
            }

            @Override
            public void onSuccess(String messageId) {
                log.debug("[AbstractorPubSub] Message published successfully. Publisher={}, messageId={}",
                        publisherName, messageId);
            }
        }, MoreExecutors.directExecutor());
    }

    // =========================
    // Subscriber Registration
    // =========================
    public void registerSubscriber(String name, SubscriberContainer subscriber) {
        subscribers.put(name, subscriber);
    }

    // =========================
    // Lifecycle
    // =========================
    @Override
    public synchronized void start() {
        if (running) { return;}

        validateConfiguration();

        log.debug("[AbstractorPubSub] Starting {} subscribers...", subscribers.size());

        subscribers.forEach((name, container) -> {
            log.debug("[AbstractorPubSub] Starting subscriber: {}", name);
            container.start();
        });

        running = true;
    }

    @Override
    public synchronized void stop() {
        if (!running) { return;}

        log.debug("[AbstractorPubSub] Starting graceful shutdown...");

        // Stop Subscribers
        subscribers.forEach((name, container) -> {
            log.debug("[AbstractorPubSub] Stopping subscriber: {}", name);
            container.gracefulStop();

        });

        log.debug("[AbstractorPubSub] Finishing publishers...");

        // Shutdown Publishers
        publishers.forEach((name,publisher) -> {
            log.debug("[AbstractorPubSub] Shutdown publisher: {}", name);
            try {
                publisher.shutdown();
                publisher.awaitTermination(30, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        });

        publishers.clear();
        subscribers.clear();

        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE; // inicia por último, para garantir contexto pronto
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    public Map<String, SubscriberContainer> getSubscribers() {
        return subscribers;
    }

    //Validate Props Configuration
    private void validateConfiguration() {

        if (props.getProjectId() == null || props.getProjectId().isBlank()) {
            throw new  IllegalStateException("[AbstractorPubSub] Project ID is required.");
        }

        props.getPublishers().forEach((name, config) -> {
            if (config.getTopic() == null || config.getTopic().isBlank()) {
                throw new IllegalStateException(
                        "Publisher '" + name + "' must define a topic");
            }
        });

        props.getSubscribers().forEach((name, config) -> {
            if (config.getSubscription() == null || config.getSubscription().isBlank()) {
                throw new IllegalStateException(
                        "Subscriber '" + name + "' must define a subscription");
            }
        });

    }



}
