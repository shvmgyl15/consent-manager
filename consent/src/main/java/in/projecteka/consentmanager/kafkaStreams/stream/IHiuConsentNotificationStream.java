package in.projecteka.consentmanager.kafkaStreams.stream;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface IHiuConsentNotificationStream {
    String INPUT_HIU_CONSENT_NOTIFICATION_QUEUE = "input-hiu-consent-notification-queue";
    String OUTPUT_HIU_CONSENT_NOTIFICATION_QUEUE = "output-hiu-consent-notification-queue";

    @Input(INPUT_HIU_CONSENT_NOTIFICATION_QUEUE)
    SubscribableChannel processHiuConsentNotification();

    @Output(OUTPUT_HIU_CONSENT_NOTIFICATION_QUEUE)
    MessageChannel sendToHiu();

}
