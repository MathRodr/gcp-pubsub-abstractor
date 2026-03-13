package com.pubsub.messaging.health;

import com.pubsub.messaging.core.PubSubManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PubSubHealthIndicatorTest {

    @Test
    void shouldReturnDownWhenManagerNotRunning() {

        PubSubManager manager = mock(PubSubManager.class);
        when(manager.isRunning()).thenReturn(false);

        PubSubHealthIndicator indicator =
                new PubSubHealthIndicator(manager);

        assertEquals(Status.DOWN, indicator.health().getStatus());
    }
}
