package com.pubsub.messaging.autoconfigure;

import com.pubsub.messaging.config.PubSubProperties;
import com.pubsub.messaging.core.PubSubManager;
import com.pubsub.messaging.health.PubSubHealthIndicator;
import com.pubsub.messaging.processor.ListenerBeanPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PubSubProperties.class})
public class PubSubAutoConfiguration {

    @Bean
    public PubSubManager pubSubManager(PubSubProperties props) {
        return new PubSubManager(props);
    }

    @Bean
    public static ListenerBeanPostProcessor listenerBeanPostProcessor() {
        return new ListenerBeanPostProcessor();
    }

    @Bean
    public PubSubHealthIndicator pubSubHealthIndicator(PubSubManager manager) {
        return new PubSubHealthIndicator(manager);
    }

}
