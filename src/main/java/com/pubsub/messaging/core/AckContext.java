package com.pubsub.messaging.core;

import com.google.cloud.pubsub.v1.AckReplyConsumerWithResponse;

public class AckContext {

    private final AckReplyConsumerWithResponse ackReplyConsumer;

    public AckContext(AckReplyConsumerWithResponse ackReplyConsumer) {
        this.ackReplyConsumer = ackReplyConsumer;
    }

    public void ack() {
        ackReplyConsumer.ack();
    }

    public void nack() {
        ackReplyConsumer.nack();
    }

}

