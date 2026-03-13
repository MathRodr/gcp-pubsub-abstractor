package com.pubsub.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "gcp-abstractor.pubsub")
public class PubSubProperties {

    private String projectId;
    private String emulatorHost;

    private Map<String, PublisherConfig> publishers = new HashMap<>();
    private Map<String, SubscriberConfig> subscribers = new HashMap<>();

    public static class PublisherConfig {
        private String topic;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }

    public static class SubscriberConfig {
        private String subscription;
        private AckMode ackMode = AckMode.AUTO;
        private Integer parallelPullCount = 1;
        private Long maxOutstandingMessages = 100L;
        private Long maxOUtstandingBytes = 10 * 1024 * 1024L; // 10MB Default

        public String getSubscription() {
            return subscription;
        }

        public void setSubscription(String subscription) {
            this.subscription = subscription;
        }

        public AckMode getAckMode() {
            return ackMode;
        }

        public void setAckMode(AckMode ackMode) {
            this.ackMode = ackMode;
        }

        public Integer getParallelPullCount() {
            return parallelPullCount;
        }

        public void setParallelPullCount(Integer parallelPullCount) {
            this.parallelPullCount = parallelPullCount;
        }

        public Long getMaxOutstandingMessages() {
            return maxOutstandingMessages;
        }

        public void setMaxOutstandingMessages(Long maxOutstandingMessages) {
            this.maxOutstandingMessages = maxOutstandingMessages;
        }

        public Long getMaxOUtstandingBytes() {
            return maxOUtstandingBytes;
        }

        public void setMaxOUtstandingBytes(Long maxOUtstandingBytes) {
            this.maxOUtstandingBytes = maxOUtstandingBytes;
        }
    }

    public enum AckMode {
        AUTO, MANUAL
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getEmulatorHost() {
        return emulatorHost;
    }

    public void setEmulatorHost(String emulatorHost) {
        this.emulatorHost = emulatorHost;
    }

    public Map<String, PublisherConfig> getPublishers() {
        return publishers;
    }

    public void setPublishers(Map<String, PublisherConfig> publishers) {
        this.publishers = publishers;
    }

    public Map<String, SubscriberConfig> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(Map<String, SubscriberConfig> subscribers) {
        this.subscribers = subscribers;
    }
}
