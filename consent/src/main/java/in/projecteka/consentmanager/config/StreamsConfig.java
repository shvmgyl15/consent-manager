package in.projecteka.consentmanager.config;

import in.projecteka.consentmanager.kafkaStreams.stream.IConsentRequestStream;
import in.projecteka.consentmanager.kafkaStreams.stream.IHipConsentNotificationStream;
import in.projecteka.consentmanager.kafkaStreams.stream.IHipLinkStream;
import in.projecteka.consentmanager.kafkaStreams.stream.IHiuConsentNotificationStream;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;

@EnableBinding(value = {Source.class, IHipLinkStream.class, IConsentRequestStream.class,
        IHiuConsentNotificationStream.class, IHipConsentNotificationStream.class})
public class StreamsConfig {
}

