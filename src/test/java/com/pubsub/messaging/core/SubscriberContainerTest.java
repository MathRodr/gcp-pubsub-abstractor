package com.pubsub.messaging.core;

import com.google.cloud.pubsub.v1.AckReplyConsumerWithResponse;
import com.pubsub.messaging.config.PubSubProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SubscriberContainerTest {

    @Test
    void shouldAckMessageInAutoMode() {

        AckReplyConsumerWithResponse consumer =
                mock(AckReplyConsumerWithResponse.class);

        consumer.ack();

        verify(consumer, times(1)).ack();
    }

    @Test
    void shouldFailWhenManualAckMethodHasWrongSignature() throws NoSuchMethodException {

        //Fake class with fake method
        class TestBean {
            public void invalidMethod(String payload) {}
        }

        PubSubProperties.SubscriberConfig subscriberConfig = new PubSubProperties.SubscriberConfig();
        subscriberConfig.setAckMode(PubSubProperties.AckMode.MANUAL);

        var method = TestBean.class.getMethod("invalidMethod", String.class);

        assertThrows(IllegalStateException.class, () ->
                SubscriberContainer.createSubscriber(
                        "project",
                        null,
                        "sub",
                        subscriberConfig,
                        new TestBean(),
                        method));
    }
}
