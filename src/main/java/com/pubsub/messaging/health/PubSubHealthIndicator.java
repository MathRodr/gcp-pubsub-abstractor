package com.pubsub.messaging.health;

import com.pubsub.messaging.core.PubSubManager;
import com.pubsub.messaging.core.SubscriberContainer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.Map;

public class PubSubHealthIndicator implements HealthIndicator {

    private final PubSubManager pubSubManager;

    public PubSubHealthIndicator(PubSubManager pubSubManager) {
        this.pubSubManager = pubSubManager;
    }

    @Override
    public Health getHealth(boolean includeDetails) {
        return HealthIndicator.super.getHealth(includeDetails);
    }

    @Override
    public Health health() {

        if (!pubSubManager.isRunning()) {
            return Health.down()
                    .withDetail("manager", "NOT_RUNNING")
                    .build();
        }

        Map<String, SubscriberContainer> subscribers = pubSubManager.getSubscribers();

        Health.Builder builder = Health.up().withDetail("subscribersCount", subscribers.size());

        boolean anyDown = false;

        for (Map.Entry<String, SubscriberContainer> entry : subscribers.entrySet()) {

            boolean running = entry.getValue().isRunning();

            builder.withDetail(entry.getKey(), running? "RUNNING" : "NOT RUNNING");

            if (!running) {
                anyDown = true;
            }
        }

        if  (anyDown) {
            builder.down();
        }

        return builder.build();
    }
}
