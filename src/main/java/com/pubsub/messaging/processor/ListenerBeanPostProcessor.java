package com.pubsub.messaging.processor;

import com.pubsub.messaging.annotation.PubSubListener;
import com.pubsub.messaging.config.PubSubProperties;
import com.pubsub.messaging.core.PubSubManager;
import com.pubsub.messaging.core.SubscriberContainer;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;

public class ListenerBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public Object postProcessAfterInitialization(Object bean, @NonNull String beanName) {

        for (Method method : bean.getClass().getMethods()) {

            if (method.isAnnotationPresent(PubSubListener.class)) {

                PubSubManager manager = applicationContext.getBean(PubSubManager.class);
                PubSubProperties props = applicationContext.getBean(PubSubProperties.class);

                String logicalName = method.getAnnotation(PubSubListener.class).value();

                PubSubProperties.SubscriberConfig config = props.getSubscribers().get(logicalName);

                if (config == null) {
                    throw new IllegalStateException(
                            "No configuration found for subscriber logical name: " + logicalName);
                }

                SubscriberContainer container =
                        SubscriberContainer.createSubscriber(
                                props.getProjectId(),
                                props.getEmulatorHost(),
                                config.getSubscription(),
                                config,
                                bean,
                                method);

                manager.registerSubscriber(logicalName, container);
            }
        }
        return bean;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

}
