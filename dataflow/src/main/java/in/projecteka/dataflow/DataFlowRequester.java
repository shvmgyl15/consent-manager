package in.projecteka.dataflow;

import in.projecteka.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.dataflow.model.ConsentArtefactsStatusResponse;
import in.projecteka.dataflow.model.ConsentStatus;
import in.projecteka.dataflow.model.DataFlowRequest;
import in.projecteka.dataflow.model.DataFlowRequestResponse;
import in.projecteka.dataflow.model.DataFlowRequestResult;
import in.projecteka.dataflow.model.DateRange;
import in.projecteka.dataflow.model.GatewayDataFlowRequest;
import in.projecteka.dataflow.model.HIRequest;
import in.projecteka.dataflow.model.HealthInfoNotificationRequest;
import in.projecteka.dataflow.model.HealthInformationResponse;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.GatewayResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static in.projecteka.dataflow.model.RequestStatus.REQUESTED;
import static in.projecteka.library.clients.model.ClientError.from;

@AllArgsConstructor
public class DataFlowRequester {
    private static final Logger logger = LoggerFactory.getLogger(DataFlowRequester.class);
    private final ConsentManagerClient consentManagerClient;
    private final DataFlowRequestRepository dataFlowRequestRepository;
    private final PostDataFlowRequestApproval postDataFlowrequestApproval;
    private final DataFlowRequestClient dataFlowRequestClient;

    public Mono<DataFlowRequestResponse> requestHealthData(DataFlowRequest dataFlowRequest) {
        final String transactionId = UUID.randomUUID().toString();
        return fetchConsentArtefact(dataFlowRequest.getConsent().getId())
                .flatMap(caRep -> saveNotificationRequest(dataFlowRequest, caRep))
                .flatMap(flowRequest -> dataFlowRequestRepository.addDataFlowRequest(transactionId, flowRequest)
                        .thenReturn(flowRequest))
                .flatMap(flowRequest -> notifyHIP(transactionId, flowRequest))
                .thenReturn(DataFlowRequestResponse.builder().transactionId(transactionId).build());
    }

    public Mono<Void> updateDataflowRequestStatus(HealthInformationResponse healthInformationResponse) {
        if (healthInformationResponse.getHiRequest() != null) {
            logger.info("DataFlowRequest response came for transactionId {}", healthInformationResponse.getHiRequest().getTransactionId());
            return dataFlowRequestRepository.updateDataFlowRequestStatus(
                    healthInformationResponse.getHiRequest().getTransactionId(),
                    healthInformationResponse.getHiRequest().getSessionStatus());
        }

        logger.error("DataFlowRequest failed for request id {}", healthInformationResponse.getResp().getRequestId());
        return Mono.empty();

    }

    public Mono<Void> requestHealthDataInfo(GatewayDataFlowRequest dataFlowRequest) {
        final UUID transactionId = UUID.randomUUID();
        final UUID gatewayRequestId = dataFlowRequest.getRequestId();
        logger.info("[DataFlowRequester] Received data flow request for Gateway requestId={}", gatewayRequestId);
        AtomicReference<String> hiuId = new AtomicReference<>("");
        return fetchConsentArtefact(dataFlowRequest.getHiRequest().getConsent().getId())
                .flatMap(caRep -> {
                    if (caRep == null) {
                        logger.info("[DataFlowRequester] No consent artefact found for Gateway requestId={}", gatewayRequestId);
                        return Mono.error(ClientError.consentArtefactNotFound());
                    }
                    if (caRep.getConsentDetail() != null
                            && caRep.getConsentDetail().getHiu() != null
                            && caRep.getConsentDetail().getHiu().getId() != null) {
                        hiuId.set(caRep.getConsentDetail().getHiu().getId());
                    }
                    return saveNotificationRequest(dataFlowRequest.getHiRequest(), caRep);
                })
                .flatMap(flowRequest -> dataFlowRequestRepository.addDataFlowRequest(transactionId.toString(), flowRequest)
                        .thenReturn(flowRequest))
                .flatMap(flowRequest -> notifyHIP(transactionId.toString(), flowRequest)
                        .thenReturn(flowRequest))
                .map(result -> {
                    var hiRequest = HIRequest.builder()
                            .transactionId(transactionId)
                            .sessionStatus(REQUESTED)
                            .build();
                    logger.info("[DataFlowRequester] Building Data flow request result for Gateway " +
                            "requestId={} with transactionId={}", gatewayRequestId, transactionId);
                    return DataFlowRequestResult.builder()
                            .requestId(UUID.randomUUID())
                            .timestamp(LocalDateTime.now())
                            .hiRequest(hiRequest)
                            .resp(GatewayResponse.builder().
                                    requestId(dataFlowRequest.getRequestId().toString())
                                    .build())
                            .build();
                })
                .onErrorResume(ClientError.class, exception -> {
                    logger.info("[DataFlowRequester] Building Data flow request error response for Gateway " +
                            "requestId={}", gatewayRequestId);
                    var dataFlowRequestResult = DataFlowRequestResult.builder()
                            .requestId(UUID.randomUUID())
                            .timestamp(LocalDateTime.now())
                            .error(from(exception))
                            .resp(GatewayResponse.builder().
                                    requestId(dataFlowRequest.getRequestId().toString())
                                    .build())
                            .build();
                    return Mono.just(dataFlowRequestResult);
                })
                .flatMap(dataFlowRequestResult -> sendHealthInformationResponseToGateway(dataFlowRequestResult, hiuId.get()));
    }

    private Mono<Void> sendHealthInformationResponseToGateway(DataFlowRequestResult dataFlowRequest, String hiuId) {
        logger.info("[DataFlowRequester] About to send data flow request response for gateway " +
                "requestId={}", dataFlowRequest.getResp().getRequestId());
        return dataFlowRequestClient.sendHealthInformationResponseToGateway(dataFlowRequest, hiuId);
    }

    private Mono<Void> notifyHIP(String transactionId, DataFlowRequest dataFlowRequest) {
        return postDataFlowrequestApproval.broadcastDataFlowRequest(transactionId, dataFlowRequest);
    }

    private Mono<DataFlowRequest> saveNotificationRequest(
            DataFlowRequest dataFlowRequest,
            ConsentArtefactRepresentation consentArtefactRepresentation) {
        if (isConsentExpired(consentArtefactRepresentation)) {
            return Mono.error(ClientError.consentExpired());
        }
        if (!isConsentGranted(consentArtefactRepresentation)) {
            return Mono.error(ClientError.consentNotGranted());
        }
        if (dataFlowRequest.getDateRange() != null &&
                !isValidHIDateRange(dataFlowRequest, consentArtefactRepresentation)) {
            return Mono.error(ClientError.invalidDateRange());
        }
        var flowRequestBuilder = dataFlowRequest.toBuilder();
        if (dataFlowRequest.getDateRange() == null) {
            flowRequestBuilder.dateRange(defaultDateRange(consentArtefactRepresentation));
        }
        return Mono.just(flowRequestBuilder.build());
    }

    private boolean isConsentGranted(ConsentArtefactRepresentation consentArtefactRepresentation) {
        return consentArtefactRepresentation.getStatus().equals(ConsentStatus.GRANTED);
    }

    private DateRange defaultDateRange(ConsentArtefactRepresentation consentArtefactRepresentation) {
        return DateRange.builder()
                .from(consentArtefactRepresentation
                        .getConsentDetail()
                        .getPermission()
                        .getDateRange()
                        .getFromDate())
                .to(consentArtefactRepresentation
                        .getConsentDetail()
                        .getPermission()
                        .getDateRange()
                        .getToDate())
                .build();
    }

    private Mono<ConsentArtefactRepresentation> fetchConsentArtefact(String consentArtefactId) {
        return consentManagerClient.getConsentArtefact(consentArtefactId);
    }

    private boolean isValidHIDateRange(DataFlowRequest dataFlowRequest,
                                       ConsentArtefactRepresentation consentArtefactRepresentation) {
        boolean equalOrAfter = isEqualOrAfter(
                dataFlowRequest.getDateRange().getFrom(),
                consentArtefactRepresentation.fromDate());
        boolean equalOrBefore = isEqualOrBefore(
                dataFlowRequest.getDateRange().getTo(),
                consentArtefactRepresentation.toDate());
        boolean before = dataFlowRequest.getDateRange().getFrom().isBefore(dataFlowRequest.getDateRange().getTo());
        return equalOrAfter && equalOrBefore && before;
    }

    private boolean isEqualOrBefore(LocalDateTime requestDate,
                                    LocalDateTime permissionDate) {
        return requestDate.equals(permissionDate) || requestDate.isBefore(permissionDate);
    }

    private boolean isEqualOrAfter(LocalDateTime requestDate, LocalDateTime permissionDate) {
        return requestDate.equals(permissionDate) || requestDate.isAfter(permissionDate);
    }

    private boolean isConsentExpired(ConsentArtefactRepresentation consentArtefactRepresentation) {
        return consentArtefactRepresentation.getConsentDetail().getPermission().getDataEraseAt().isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private Mono<Boolean> validateRequest(UUID requestId) {
        return dataFlowRequestRepository.getIfPresent(requestId)
                .map(Objects::isNull)
                .switchIfEmpty(Mono.just(true));
    }

    public Mono<Void> notifyHealthInformationStatus(HealthInfoNotificationRequest notificationRequest) {
        return Mono.just(notificationRequest.getRequestId())
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(val -> dataFlowRequestRepository.saveHealthNotificationRequest(notificationRequest));
    }
}
