package in.projecteka.consentmanager.kafkaStreams.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.consent.model.ConsentArtefactsMessage;
import in.projecteka.consentmanager.consent.model.request.ConsentArtefactReference;
import in.projecteka.consentmanager.consent.model.request.ConsentNotifier;
import in.projecteka.consentmanager.consent.model.request.HIUNotificationRequest;
import in.projecteka.consentmanager.kafkaStreams.stream.IHiuConsentNotificationStream;
import in.projecteka.consentmanager.properties.ListenerProperties;
import in.projecteka.library.common.TraceableMessage;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static in.projecteka.library.common.Constants.CORRELATION_ID;

@Service
@AllArgsConstructor
public class HiuConsentNotificationConsumer {
    private static final Logger logger = LoggerFactory.getLogger(HiuConsentNotificationConsumer.class);
    private final ConsentArtefactNotifier consentArtefactNotifier;
    private final ListenerProperties listenerProperties;

    @StreamListener(IHiuConsentNotificationStream.INPUT_HIU_CONSENT_NOTIFICATION_QUEUE)
    public void subscribe(String message) {
        try {
            //This is NOT a generic solution. Based on the context, it either needs to retry, or it might also need to propagate the error to the upstream systems.
            //TODO be revisited during Gateway development
            //                if (hasExceededRetryCount(message)) {
            //                    amqpTemplate.convertAndSend(PARKING_EXCHANGE,
            //                            message.getMessageProperties().getReceivedRoutingKey(),
            //                            message);
            //                    return;
            //                }
            ObjectMapper mapper = new ObjectMapper();
            TraceableMessage traceableMessage = mapper.readValue(message, TraceableMessage.class);
            mapper.registerModule(new JavaTimeModule());
            ConsentArtefactsMessage consentArtefactsMessage = mapper.convertValue(
                    traceableMessage.getMessage(),
                    ConsentArtefactsMessage.class);
            MDC.put(CORRELATION_ID, traceableMessage.getCorrelationId());
            logger.info("Received message for Request id : {}", consentArtefactsMessage.getConsentRequestId());
            notifyHiu(consentArtefactsMessage);
            MDC.clear();
        } catch (Exception e) {
            logger.debug("Exception in HIU Consent Notification Consumer");
            e.printStackTrace();
        }
    }

//    private boolean hasExceededRetryCount(Message in) {
//        List<Map<String, ?>> xDeathHeader = in.getMessageProperties().getXDeathHeader();
//        if (xDeathHeader != null && !xDeathHeader.isEmpty()) {
//            Long count = (Long) xDeathHeader.get(0).get("count");
//            logger.info("[HIU] Number of attempts {}", count);
//            return count >= listenerProperties.getMaximumRetries();
//        }
//        return false;
//    }

    private void notifyHiu(ConsentArtefactsMessage consentArtefactsMessage) {
        HIUNotificationRequest hiuNotificationRequest = hiuNotificationRequest(consentArtefactsMessage);
        String hiuId = consentArtefactsMessage.getHiuId();
        consentArtefactNotifier.sendConsentArtifactToHIU(hiuNotificationRequest, hiuId)
                .subscriberContext(ctx -> {
                    Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
                    return correlationId.map(id -> ctx.put(CORRELATION_ID, id))
                            .orElseGet(() -> ctx.put(CORRELATION_ID, UUID.randomUUID().toString()));
                }).block();
    }

    private HIUNotificationRequest hiuNotificationRequest(ConsentArtefactsMessage consentArtefactsMessage) {
        List<ConsentArtefactReference> consentArtefactReferences = consentArtefactReferences(consentArtefactsMessage);
        return HIUNotificationRequest
                .builder()
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .requestId(UUID.randomUUID())
                .notification(ConsentNotifier
                        .builder()
                        .consentRequestId(consentArtefactsMessage.getConsentRequestId())
                        .status(consentArtefactsMessage.getStatus())
                        .consentArtefacts(consentArtefactReferences)
                        .build())
                .build();
    }

    private List<ConsentArtefactReference> consentArtefactReferences(ConsentArtefactsMessage consentArtefactsMessage) {
        return consentArtefactsMessage
                .getConsentArtefacts()
                .stream()
                .map(consentArtefact -> ConsentArtefactReference
                        .builder()
                        .id(consentArtefact.getConsentId())
                        .build())
                .collect(Collectors.toList());
    }
}
