package in.projecteka.consentmanager.kafkaStreams.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import in.projecteka.consentmanager.kafkaStreams.stream.IConsentNotificationStream;
import in.projecteka.library.common.TraceableMessage;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static in.projecteka.library.common.Constants.CORRELATION_ID;

@Service
public class ConsentNotificationProducer {
    private static final Logger logger = LoggerFactory.getLogger(ConsentNotificationProducer.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    IConsentNotificationStream iConsentNotificationStream;

    public Mono<Void> publish(ConsentArtefactsMessage message) {
        return Mono.create(monoSink -> {
            broadcastArtefactsToHiu(message);
            broadcastArtefactsToHips(message);
            monoSink.success();
        });
    }

    @SneakyThrows
    private void broadcastArtefactsToHiu(ConsentArtefactsMessage message) {

        try {
            MessageChannel messageChannel = iConsentNotificationStream.sendToHiu();
            messageChannel.send(MessageBuilder.withPayload(getMessage(message)).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("Broadcasting consent artefact notification for Request Id: {}",
                message.getConsentRequestId());
    }

    @SneakyThrows
    private void broadcastArtefactsToHips(ConsentArtefactsMessage message) {

        message.getConsentArtefacts()
                .forEach(consentArtefact -> {
                    try {
                        MessageChannel messageChannel = iConsentNotificationStream.sendToHip();
                        messageChannel.send(MessageBuilder.withPayload(getMessage(consentArtefact)).build());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    logger.info(
                            "Broadcasting consent artefact notification to hip for consent artefact: {}",
                            consentArtefact.getConsentId()
                    );
                });
    }

    @SneakyThrows
    public Mono<Void> broadcastConsentRequestNotification(ConsentRequest consentRequest) {
        return Mono.create(monoSink -> {
            try {
                MessageChannel messageChannel = iConsentNotificationStream.postConsentRequest();
                messageChannel.send(MessageBuilder.withPayload(getMessage(consentRequest)).build());
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.info("Broadcasting consent request with request id : {}", consentRequest.getId());
            monoSink.success();
        });
    }

    private String getMessage(Object message) throws JsonProcessingException {
        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(MDC.get(CORRELATION_ID))
                .message(message)
                .build();
        return mapper.writeValueAsString(traceableMessage);
    }
}
