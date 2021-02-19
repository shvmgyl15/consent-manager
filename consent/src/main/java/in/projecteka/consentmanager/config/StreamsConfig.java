package in.projecteka.consentmanager.config;

import in.projecteka.consentmanager.kafkaStreams.stream.IConsentNotificationStream;
import in.projecteka.consentmanager.kafkaStreams.stream.IHipLinkStream;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;

@EnableBinding(value = {Source.class, IHipLinkStream.class, IConsentNotificationStream.class})
public class StreamsConfig {
}

