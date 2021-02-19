package in.projecteka.consentmanager.kafkaStreams.stream;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface IConsentRequestStream {
    String INPUT_CONSENT_REQUEST_QUEUE = "input-consent-request-queue";
    String OUTPUT_CONSENT_REQUEST_QUEUE = "output-consent-request-queue";

    @Input(INPUT_CONSENT_REQUEST_QUEUE)
    SubscribableChannel processConsentRequest();

    @Output(OUTPUT_CONSENT_REQUEST_QUEUE)
    MessageChannel postConsentRequest();

}
