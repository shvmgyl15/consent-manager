package in.projecteka.consentmanager.kafkaStreams.stream;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface IHipConsentNotificationStream {
    String INPUT_HIP_CONSENT_NOTIFICATION_QUEUE = "input-hip-consent-notification-queue";
    String OUTPUT_HIP_CONSENT_NOTIFICATION_QUEUE = "output-hip-consent-notification-queue";

    @Input(INPUT_HIP_CONSENT_NOTIFICATION_QUEUE)
    SubscribableChannel processHipConsentNotification();

    @Output(OUTPUT_HIP_CONSENT_NOTIFICATION_QUEUE)
    MessageChannel sendToHip();

}
