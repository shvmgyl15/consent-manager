package in.projecteka.consentmanager.kafkaStreams.stream;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface IConsentNotificationStream {
    String INPUT_CONSENT_REQUEST_QUEUE = "input-consent-request-queue";
    String INPUT_HIU_CONSENT_NOTIFICATION_QUEUE = "input-hiu-consent-notification-queue";
    String INPUT_HIP_CONSENT_NOTIFICATION_QUEUE = "input-hip-consent-notification-queue";
    String OUTPUT_CONSENT_REQUEST_QUEUE = "output-consent-request-queue";
    String OUTPUT_HIU_CONSENT_NOTIFICATION_QUEUE = "output-hiu-consent-notification-queue";
    String OUTPUT_HIP_CONSENT_NOTIFICATION_QUEUE = "output-hip-consent-notification-queue";

    @Input(INPUT_CONSENT_REQUEST_QUEUE)
    SubscribableChannel processConsentRequest();

    @Input(INPUT_HIU_CONSENT_NOTIFICATION_QUEUE)
    SubscribableChannel processHiuConsentNotification();

    @Input(INPUT_HIP_CONSENT_NOTIFICATION_QUEUE)
    SubscribableChannel processHipConsentNotification();

    @Output(OUTPUT_CONSENT_REQUEST_QUEUE)
    MessageChannel postConsentRequest();

    @Output(OUTPUT_HIU_CONSENT_NOTIFICATION_QUEUE)
    MessageChannel sendToHiu();

    @Output(OUTPUT_HIP_CONSENT_NOTIFICATION_QUEUE)
    MessageChannel sendToHip();

}
