package com.pubsub.messaging.core;

import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumerWithResponse;
import com.google.cloud.pubsub.v1.MessageReceiverWithAckResponse;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.pubsub.messaging.config.PubSubProperties;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SubscriberContainer {

    private static final Logger log = LoggerFactory.getLogger(SubscriberContainer.class);

    private final Subscriber subscriber;
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private SubscriberContainer(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public static SubscriberContainer createSubscriber(
            String projectId,
            String emulatorHost,
            String subscription,
            PubSubProperties.SubscriberConfig config,
            Object bean,
            Method method) {

        ProjectSubscriptionName subName = ProjectSubscriptionName.of(projectId, subscription);

        // manualAck
        boolean manualAck = config.getAckMode() == PubSubProperties.AckMode.MANUAL;
        validateManualAck(method, manualAck);

        // Create empty container, subscriber will be added later
        SubscriberContainer[] holder = new SubscriberContainer[1];

        Subscriber.Builder builder = Subscriber.newBuilder(subName,
                (MessageReceiverWithAckResponse) (message, consumer) -> {

                    SubscriberContainer container = holder[0];
                    // If shutdown was started nack new messages
                    if (container.shuttingDown.get()) {
                        consumer.nack();
                        return;
                    }

                    // Count messages that are processing
                    container.inFlight.incrementAndGet();

                    try {
                        String payload = message.getData().toStringUtf8();

                        if (manualAck) {
                            TrackingAckContext ackContext = new TrackingAckContext(consumer);
                            method.invoke(bean, payload, ackContext);
                            if (!ackContext.hasResponded()) {
                                log.warn("[AbstractorPubSub] Message processed in MANUAL MODE without ack/nack. Subscription: {}", subscription);
                            }

                        } else {
                            method.invoke(bean, payload);
                            consumer.ack();
                        }

                    } catch (Exception e) {
                        log.error("[AbstractorPubSub] Error processing message. NACK sent.", e);
                        //MANUAL MODE the consumer control ACK and NACK
                        if (!manualAck) {
                            consumer.nack();
                        }

                    } finally {
                        container.inFlight.decrementAndGet();
                    }
                });

        if (emulatorHost != null) {
            createEmulatorChannel(emulatorHost, builder);
        }

        // Add FlowControl for Parallel Threads
        addFlowControlSettings(config, builder);

        Subscriber subscriber = builder.build();
        SubscriberContainer container = new SubscriberContainer(subscriber);
        holder[0] = container;

        return container;
    }

    private static void validateManualAck(Method method, boolean manualAck) {
        if (manualAck && method.getParameterCount() != 2) {
            throw new IllegalStateException("AckMode MANUAL requires 2 parameters: (payload, AckContext)");
        }
        if (!manualAck && method.getParameterCount() != 1) {
            throw new IllegalStateException("AckMode AUTO requires only 1 parameter: (payload)");
        }
    }

    private static void addFlowControlSettings(PubSubProperties.SubscriberConfig config, Subscriber.Builder builder) {
        FlowControlSettings flowControlSettings = FlowControlSettings.newBuilder()
                .setMaxOutstandingElementCount(config.getMaxOutstandingMessages())
                .setMaxOutstandingRequestBytes(config.getMaxOUtstandingBytes())
                .build();
        builder.setFlowControlSettings(flowControlSettings);
        if (config.getParallelPullCount() != null && config.getParallelPullCount() > 1) {
            builder.setParallelPullCount(config.getParallelPullCount());
        }
    }

    private static void createEmulatorChannel(String emulatorHost, Subscriber.Builder builder) {
        ManagedChannel channel =
                ManagedChannelBuilder
                        .forTarget(emulatorHost)
                        .usePlaintext()
                        .build();

        builder.setChannelProvider(
                        FixedTransportChannelProvider.create(
                                GrpcTransportChannel.create(channel)))
                .setCredentialsProvider(NoCredentialsProvider.create());
    }

    public void start() {
        log.info("[AbstractorPubSub] Subscriber started: {}", subscriber.getSubscriptionNameString());
        subscriber.startAsync();
    }

    public void gracefulStop() {
        log.debug("[AbstractorPubSub] Graceful shutdown started for {}", subscriber.getSubscriptionNameString());

        shuttingDown.set(true);
        subscriber.stopAsync();

        long timeoutMs = 30000;
        long start = System.currentTimeMillis();

        while (inFlight.get() > 0 && (System.currentTimeMillis() - start) < timeoutMs) {
            log.debug("[AbstractorPubSub] Waiting {} processing messages...", inFlight.get());
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        try {
            subscriber.awaitTerminated(30, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }

        log.debug("[AbstractorPubSub] Subscriber stopped: {}", subscriber.getSubscriptionNameString());
    }

    public boolean isRunning() {
        return subscriber.isRunning();
    }

    // ==============================
    // AckContext with Internal Tracking for messages with manual Ack
    // ==============================
    private static class TrackingAckContext extends AckContext {

        private final AtomicBoolean responded = new AtomicBoolean(false);

        public TrackingAckContext(AckReplyConsumerWithResponse consumer) {
            super(consumer);
        }

        @Override
        public void ack() {
            responded.set(true);
            super.ack();
        }

        @Override
        public void nack() {
            responded.set(true);
            super.nack();
        }

        public boolean hasResponded() {
            return responded.get();
        }
    }

}
