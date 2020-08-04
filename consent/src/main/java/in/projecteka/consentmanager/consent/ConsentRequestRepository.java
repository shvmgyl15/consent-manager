package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.ListResult;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import in.projecteka.library.common.DbOperationError;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static in.projecteka.consentmanager.consent.model.ConsentStatus.GRANTED;
import static in.projecteka.library.common.Serializer.from;
import static in.projecteka.library.common.Serializer.to;
import static io.vertx.sqlclient.Tuple.of;
import static reactor.core.publisher.Mono.create;

@AllArgsConstructor
public class ConsentRequestRepository {
    private static final Logger logger = LoggerFactory.getLogger(ConsentRequestRepository.class);
    private static final String SELECT_CONSENT_REQUEST_BY_ID_AND_STATUS;
    private static final String SELECT_CONSENT_REQUEST_BY_ID;
    private static final String SELECT_CONSENT_REQUESTS_BY_STATUS;
    private static final String SELECT_CONSENT_DETAILS_FOR_PATIENT;
    private static final String SELECT_CONSENT_REQUEST_COUNT = "SELECT COUNT(*) FROM consent_request " +
            "WHERE LOWER(patient_id) = $1  and status != $3 and (status= $2 OR $2 IS NULL)";
    private static final String INSERT_CONSENT_REQUEST_QUERY = "INSERT INTO consent_request " +
            "(request_id, patient_id, status, details) VALUES ($1, $2, $3, $4)";
    private static final String UPDATE_CONSENT_REQUEST_STATUS_QUERY = "UPDATE consent_request SET status = $1, " +
            "date_modified= $2 WHERE request_id= $3";
    private static final String FAILED_TO_SAVE_CONSENT_REQUEST = "Failed to save consent request";
    private static final String UNKNOWN_ERROR_OCCURRED = "Unknown error occurred";
    private static final String FAILED_TO_GET_CONSENT_REQUESTS_BY_STATUS = "Failed to get consent requests by status";

    private final PgPool readWriteClient;
    private final PgPool readOnlyClient;

    static {
        String s = "SELECT request_id, status, details, date_created, date_modified FROM consent_request " +
                "where ";
        SELECT_CONSENT_DETAILS_FOR_PATIENT = s + " LOWER(patient_id) = $1 and status!=$5 and (status=$4 OR $4 IS NULL) "
                + "ORDER BY date_modified DESC"
                + " LIMIT $2 OFFSET $3";
        SELECT_CONSENT_REQUEST_BY_ID = s + "request_id=$1";
        SELECT_CONSENT_REQUEST_BY_ID_AND_STATUS = s + "request_id=$1 and status=$2 and patient_id=$3";
        SELECT_CONSENT_REQUESTS_BY_STATUS = s + "status=$1";
    }

    public Mono<Void> insert(RequestedDetail requestedDetail, UUID requestId) {
        return create(monoSink ->
                readWriteClient.preparedQuery(INSERT_CONSENT_REQUEST_QUERY)
                        .execute(of(requestId.toString(),
                                requestedDetail.getPatient().getId(),
                                ConsentStatus.REQUESTED.name(),
                                new JsonObject(from(requestedDetail))),
                                handler -> {
                                    if (handler.failed()) {
                                        logger.error(handler.cause().getMessage(), handler.cause());
                                        monoSink.error(new Exception(FAILED_TO_SAVE_CONSENT_REQUEST));
                                        return;
                                    }
                                    monoSink.success();
                                }));
    }

    public Mono<ListResult<List<ConsentRequestDetail>>> requestsForPatient(String patientId,
                                                                           int limit,
                                                                           int offset,
                                                                           String status) {
        return create(monoSink -> readOnlyClient.preparedQuery(SELECT_CONSENT_DETAILS_FOR_PATIENT)
                .execute(of(patientId.toLowerCase(), limit, offset, status, GRANTED.toString()),
                        handler -> {
                            List<ConsentRequestDetail> requestList = getConsentRequestDetails(handler);
                            readOnlyClient.preparedQuery(SELECT_CONSENT_REQUEST_COUNT)
                                    .execute(of(patientId, status, GRANTED.toString()),
                                            counter -> {
                                                if (handler.failed()) {
                                                    logger.error(handler.cause().getMessage(), handler.cause());
                                                    monoSink.error(new DbOperationError());
                                                    return;
                                                }
                                                var count = counter.result().iterator().next().getInteger("count");
                                                monoSink.success(new ListResult<>(requestList, count));
                                            });
                        }));
    }

    private List<ConsentRequestDetail> getConsentRequestDetails(AsyncResult<RowSet<Row>> handler) {
        if (handler.failed()) {
            return new ArrayList<>();
        }
        List<ConsentRequestDetail> requestList = new ArrayList<>();
        RowSet<Row> results = handler.result();
        for (Row result : results) {
            ConsentRequestDetail aDetail = mapToConsentRequestDetail(result);
            requestList.add(aDetail);
        }
        return requestList;
    }

    public Mono<ConsentRequestDetail> requestOf(String requestId, String status, String patientId) {
        return create(monoSink -> readOnlyClient.preparedQuery(SELECT_CONSENT_REQUEST_BY_ID_AND_STATUS)
                .execute(of(requestId, status, patientId), consentRequestHandler(monoSink)));
    }

    public Mono<ConsentRequestDetail> requestOf(String requestId) {
        return create(monoSink -> readOnlyClient.preparedQuery(SELECT_CONSENT_REQUEST_BY_ID)
                .execute(of(requestId), consentRequestHandler(monoSink)));
    }

    private Handler<AsyncResult<RowSet<Row>>> consentRequestHandler(MonoSink<ConsentRequestDetail> monoSink) {
        return handler -> {
            if (handler.failed()) {
                logger.error(handler.cause().getMessage(), handler.cause());
                monoSink.error(new RuntimeException(UNKNOWN_ERROR_OCCURRED));
                return;
            }
            RowSet<Row> results = handler.result();
            ConsentRequestDetail consentRequestDetail = null;
            for (Row result : results) {
                consentRequestDetail = mapToConsentRequestDetail(result);
            }
            monoSink.success(consentRequestDetail);
        };
    }

    private ConsentRequestDetail mapToConsentRequestDetail(Row result) {
        RequestedDetail details = to(result.getValue("details").toString(), RequestedDetail.class);
        return ConsentRequestDetail
                .builder()
                .requestId(result.getString("request_id"))
                .status(getConsentStatus(result.getString("status")))
                .createdAt(result.getLocalDateTime("date_created"))
                .hip(details.getHip())
                .hiu(details.getHiu())
                .hiTypes(details.getHiTypes())
                .patient(details.getPatient())
                .permission(details.getPermission())
                .purpose(details.getPurpose())
                .requester(details.getRequester())
                .consentNotificationUrl(details.getConsentNotificationUrl())
                .lastUpdated(result.getLocalDateTime("date_modified"))
                .build();
    }

    public Mono<Void> updateStatus(String id, ConsentStatus status) {
        return create(monoSink -> readWriteClient.preparedQuery(UPDATE_CONSENT_REQUEST_STATUS_QUERY)
                .execute(of(status.toString(), LocalDateTime.now(ZoneOffset.UTC), id),
                        updateHandler -> {
                            if (updateHandler.failed()) {
                                monoSink.error(new Exception("Failed to update status"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    private ConsentStatus getConsentStatus(String status) {
        return ConsentStatus.valueOf(status);
    }

    public Flux<ConsentRequestDetail> getConsentsByStatus(ConsentStatus status) {
        return Flux.create(fluxSink -> readOnlyClient.preparedQuery(SELECT_CONSENT_REQUESTS_BY_STATUS)
                .execute(of(status.toString()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                fluxSink.error(new Exception(FAILED_TO_GET_CONSENT_REQUESTS_BY_STATUS));
                                return;
                            }
                            RowSet<Row> results = handler.result();
                            if (results.iterator().hasNext()) {
                                results.forEach(row -> fluxSink.next(mapToConsentRequestDetail(row)));
                            }
                            fluxSink.complete();
                        }));
    }
}
