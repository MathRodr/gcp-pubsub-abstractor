package com.pubsub.messaging.core;

import com.pubsub.messaging.config.PubSubProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PubSubManagerTest {

    @Test
    void shouldThrowExceptionWhenPublishingAndNotRunning() {

        PubSubProperties props = new PubSubProperties();
        props.setProjectId("test-project");

        PubSubManager manager = new PubSubManager(props);

        assertThrows(IllegalStateException.class, () ->
                manager.publish("any", "payload"));
    }

    @Test
    void shouldThrowExceptionWhenPublisherNotFound() {

        PubSubProperties props = new PubSubProperties();
        props.setProjectId("test-project");

        PubSubManager manager = new PubSubManager(props);

        manager.start();

        assertThrows(IllegalArgumentException.class, () ->
                manager.publish("unknown", "payload"));
    }

    @Test
    void shouldFailWhenProjectIdMissing() {

        PubSubProperties props = new PubSubProperties();

        PubSubManager manager = new PubSubManager(props);

        assertThrows(IllegalStateException.class, manager::start);
    }

}
