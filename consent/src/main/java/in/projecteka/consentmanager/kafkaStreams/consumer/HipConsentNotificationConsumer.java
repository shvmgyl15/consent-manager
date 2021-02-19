package in.projecteka.consentmanager.kafkaStreams.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.consent.ConsentArtefactRepository;
import in.projecteka.consentmanager.consent.ConsentNotificationReceiver;
import in.projecteka.consentmanager.consent.model.ConsentNotificationStatus;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.request.HIPNotificationRequest;
import in.projecteka.consentmanager.kafkaStreams.stream.IHipConsentNotificationStream;
import in.projecteka.library.common.TraceableMessage;
import in.projecteka.library.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.stream.annotation.StreamListener;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static in.projecteka.consentmanager.consent.model.ConsentStatus.*;
import static in.projecteka.consentmanager.consent.model.HipConsentArtefactNotificationStatus.NOTIFYING;
import static in.projecteka.library.common.Constants.CORRELATION_ID;

@AllArgsConstructor
public class HipConsentNotificationConsumer {
    private static final Logger logger = LoggerFactory.getLogger(HipConsentNotificationConsumer.class);
    private final ConsentArtefactNotifier consentArtefactNotifier;
    private final ConsentArtefactRepository consentArtefactRepository;
    private final CacheAdapter<String, String> cache;

    @StreamListener(IHipConsentNotificationStream.INPUT_HIP_CONSENT_NOTIFICATION_QUEUE)
    public void subscribe(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            TraceableMessage traceableMessage = mapper.readValue(message, TraceableMessage.class);
            mapper.registerModule(new JavaTimeModule());
            HIPConsentArtefactRepresentation consentArtefact = mapper.convertValue(traceableMessage.getMessage()
                    , HIPConsentArtefactRepresentation.class);
            MDC.put(CORRELATION_ID, traceableMessage.getCorrelationId());
            logger.info("Received notify consent to hip for consent artefact: {}",
                    consentArtefact.getConsentId());

            sendConsentArtefactToHIP(consentArtefact)
                    .subscriberContext(ctx -> {
                        Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
                        return correlationId.map(id -> ctx.put(CORRELATION_ID, id))
                                .orElseGet(() -> ctx.put(CORRELATION_ID, UUID.randomUUID().toString()));
                    })
                    .block();
            MDC.clear();
        } catch (Exception e) {
            logger.debug("Exception in HIP Consent Notification Consumer");
            e.printStackTrace();
        }
    }


    private Mono<Void> sendConsentArtefactToHIP(HIPConsentArtefactRepresentation consentArtefact) {
        try {
            String hipId = consentArtefact.getConsentDetail().getHip().getId();
            HIPNotificationRequest notificationRequest = hipNotificationRequest(consentArtefact);

            if (consentArtefact.getStatus() == REVOKED) {
                return consentArtefactNotifier.sendConsentArtefactToHIP(notificationRequest, hipId)
                        .then(consentArtefactRepository.saveConsentNotification(
                                consentArtefact.getConsentId(),
                                ConsentNotificationStatus.SENT,
                                ConsentNotificationReceiver.HIP));
            }
            var artefactPublisher = Mono.defer(() -> consentArtefactNotifier.sendConsentArtefactToHIP(notificationRequest, hipId));
            if (consentArtefact.getStatus() == GRANTED) {
                return cache.put(consentArtefact.getConsentId(), NOTIFYING.toString())
                        .then(artefactPublisher);
            }
            return artefactPublisher;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Mono.empty();
        }
    }

    private HIPNotificationRequest hipNotificationRequest(HIPConsentArtefactRepresentation consentArtefact) {
        var requestId = UUID.randomUUID();
        var timestamp = LocalDateTime.now(ZoneOffset.UTC);

        if (consentArtefact.getStatus() == EXPIRED || consentArtefact.getStatus() == REVOKED) {
            return HIPNotificationRequest.builder()
                    .requestId(requestId)
                    .timestamp(timestamp)
                    .notification(HIPConsentArtefactRepresentation.builder()
                            .status(consentArtefact.getStatus())
                            .consentId(consentArtefact.getConsentId())
                            .build())
                    .build();
        }
        return HIPNotificationRequest.builder()
                .notification(consentArtefact)
                .requestId(requestId)
                .timestamp(timestamp)
                .build();
    }
}
