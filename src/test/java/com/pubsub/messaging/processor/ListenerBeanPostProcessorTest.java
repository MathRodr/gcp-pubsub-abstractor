package com.pubsub.messaging.processor;

import com.pubsub.messaging.annotation.PubSubListener;
import com.pubsub.messaging.config.PubSubProperties;
import com.pubsub.messaging.core.PubSubManager;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ListenerBeanPostProcessorTest {

    @Test
    void shouldFailWhenSubscriberConfigMissing() {

        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();

        context.registerBean(PubSubProperties.class);
        context.registerBean(PubSubManager.class,
                () -> new PubSubManager(new PubSubProperties()));
        context.registerBean(ListenerBeanPostProcessor.class);

        class TestBean {
            @PubSubListener("missing")
            public void handle(String payload) {}
        }

        context.registerBean(TestBean.class);

        assertThrows(Exception.class, context::refresh);
    }
}
