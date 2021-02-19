package in.projecteka.consentmanager.kafkaStreams.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.projecteka.consentmanager.clients.PatientServiceClient;
import in.projecteka.consentmanager.consent.ConsentManager;
import in.projecteka.consentmanager.consent.ConsentServiceProperties;
import in.projecteka.consentmanager.consent.NHSProperties;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import in.projecteka.consentmanager.consent.model.Content;
import in.projecteka.consentmanager.consent.model.GrantedContext;
import in.projecteka.consentmanager.consent.model.HIType;
import in.projecteka.consentmanager.consent.model.request.GrantedConsent;
import in.projecteka.consentmanager.consent.policies.NhsPolicyCheck;
import in.projecteka.consentmanager.kafkaStreams.stream.IConsentRequestStream;
import in.projecteka.library.clients.OtpServiceClient;
import in.projecteka.library.clients.UserServiceClient;
import in.projecteka.library.clients.model.Action;
import in.projecteka.library.clients.model.Communication;
import in.projecteka.library.clients.model.CommunicationType;
import in.projecteka.library.clients.model.Notification;
import in.projecteka.library.common.TraceableMessage;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static in.projecteka.library.common.Constants.CORRELATION_ID;

@Service
@AllArgsConstructor
public class ConsentRequestNotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ConsentRequestNotificationConsumer.class);

    private final OtpServiceClient consentNotificationClient;
    private final UserServiceClient userServiceClient;
    private final ConsentServiceProperties consentServiceProperties;
    private final ConsentManager consentManager;
    private final PatientServiceClient patientServiceClient;
    private final NHSProperties nhsProperties;

    @StreamListener(IConsentRequestStream.INPUT_CONSENT_REQUEST_QUEUE)
    public void subscribe(String message) {
        logger.info("Consumer received message: {}", message);
        try {
            ObjectMapper mapper = new ObjectMapper();
            TraceableMessage traceableMessage = mapper.readValue(message, TraceableMessage.class);
            mapper.registerModule(new JavaTimeModule());
            ConsentRequest consentRequest = mapper.convertValue(traceableMessage.getMessage(), ConsentRequest.class);
            MDC.put(CORRELATION_ID, traceableMessage.getCorrelationId());
            logger.info("Received message for Request id : {}", consentRequest.getId());
            processConsentRequest(consentRequest);
            MDC.clear();
        } catch (Exception e) {
            logger.error("Exception in Consent Request Notification Consumer");
            e.printStackTrace();
        }
    }

    public Mono<Void> notifyUserWith(Notification<Content> notification) {
        return consentNotificationClient.send(notification);
    }

    private void processConsentRequest(ConsentRequest consentRequest) {
        try {
            if (isAutoApproveConsentRequest(consentRequest)) {
                autoApproveFor(consentRequest)
                        .subscriberContext(ctx -> {
                            Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
                            return correlationId.map(id -> ctx.put(CORRELATION_ID, id))
                                    .orElseGet(() -> ctx.put(CORRELATION_ID, UUID.randomUUID().toString()));
                        }).subscribe();
                return;
            }
            createNotificationMessage(consentRequest).flatMap(this::notifyUserWith)
                    .subscriberContext(ctx -> {
                        Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
                        return correlationId.map(id -> ctx.put(CORRELATION_ID, id))
                                .orElseGet(() -> ctx.put(CORRELATION_ID, UUID.randomUUID().toString()));
                    }).block();
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }
    }

    private boolean isAutoApproveConsentRequest(ConsentRequest consentRequest) {
        return new NhsPolicyCheck().checkPolicyFor(consentRequest, nhsProperties.getHiuId());
    }

    private Mono<Notification<Content>> createNotificationMessage(ConsentRequest consentRequest) {
        return userServiceClient.userOf(consentRequest.getDetail().getPatient().getId())
                .map(user -> new Notification<>(consentRequest.getId().toString(),
                        Communication.builder()
                                .communicationType(CommunicationType.MOBILE)
                                .value(user.getPhone())
                                .build(),
                        Content.builder()
                                .requester(consentRequest.getDetail().getRequester().getName())
                                .consentRequestId(consentRequest.getId())
                                .hiTypes(Arrays.stream(consentRequest.getDetail().getHiTypes())
                                        .map(HIType::getValue)
                                        .collect(Collectors.joining(",")))
                                .deepLinkUrl(String.format("%s/consent/%s",
                                        consentServiceProperties.getUrl(),
                                        consentRequest.getId()))
                                .build(),
                        Action.CONSENT_REQUEST_CREATED));
    }

    private Mono<Void> autoApproveFor(ConsentRequest consentRequest) {
        List<GrantedContext> grantedContexts = new ArrayList<>();
        List<GrantedConsent> grantedConsents = new ArrayList<>();
        return patientServiceClient.retrievePatientLinks(consentRequest.getDetail().getPatient().getId())
                .map(linkedCareContexts -> linkedCareContexts.getCareContext(consentRequest.getDetail().getHip().getId()))
                .flatMap(linkedCareContexts -> {
                    linkedCareContexts.forEach(careContext -> {
                        grantedContexts.add(GrantedContext.builder()
                                .patientReference(careContext.getPatientRefNo())
                                .careContextReference(careContext.getCareContextRefNo())
                                .build());
                    });

                    grantedConsents.add(GrantedConsent.builder()
                            .careContexts(grantedContexts)
                            .hip(consentRequest.getDetail().getHip())
                            .hiTypes(consentRequest.getDetail().getHiTypes())
                            .permission(consentRequest.getDetail().getPermission())
                            .build());
                    return consentManager.approveConsent(consentRequest.getDetail().getPatient().getId(),
                            consentRequest.getId().toString(),
                            grantedConsents);
                }).then();
    }
}
