package in.projecteka.dataflow.kafkaStream.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import in.projecteka.dataflow.ConsentManagerClient;
import in.projecteka.dataflow.DataRequestNotifier;
import in.projecteka.dataflow.kafkaStream.stream.IHipDataflowRequestStream;
import in.projecteka.dataflow.model.DataFlowRequestMessage;
import in.projecteka.dataflow.model.hip.DataRequest;
import in.projecteka.dataflow.model.hip.HiRequest;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.Constants;
import in.projecteka.library.common.TraceableMessage;
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

import static in.projecteka.dataflow.model.HipConsentArtefactNotificationStatus.NOTIFIED;
import static in.projecteka.library.common.Constants.CORRELATION_ID;

@AllArgsConstructor
public class HipDataflowRequestConsumer {
	private final Logger log = LoggerFactory.getLogger(HipDataflowRequestConsumer.class);

	private final DataRequestNotifier dataRequestNotifier;
	private final ConsentManagerClient consentManagerClient;

	@StreamListener(IHipDataflowRequestStream.INPUT)
	public void process(String message) {
		try {
			log.info("Message from queue: {} ", message);
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			TraceableMessage traceableMessage = mapper.readValue(message, TraceableMessage.class);
			DataFlowRequestMessage dataFlowRequestMessage = mapper.convertValue(traceableMessage.getMessage(), DataFlowRequestMessage.class);
			MDC.put(Constants.CORRELATION_ID, traceableMessage.getCorrelationId());
			log.debug("Received dataFlowRequest message: {}", dataFlowRequestMessage);

			var dataFlowRequest = dataFlowRequestMessage.getDataFlowRequest();
			DataRequest dataRequest = DataRequest.builder()
					.transactionId(UUID.fromString(dataFlowRequestMessage.getTransactionId()))
					.requestId(UUID.randomUUID())
					.timestamp(LocalDateTime.now(ZoneOffset.UTC))
					.hiRequest(HiRequest.builder()
							.consent(dataFlowRequest.getConsent())
							.dataPushUrl(dataFlowRequest.getDataPushUrl())
							.dateRange(dataFlowRequest.getDateRange())
							.keyMaterial(dataFlowRequest.getKeyMaterial())
							.build())
					.build();
			configureAndSendDataRequestFor(dataRequest);
		} catch (Exception e) {
            log.debug("Kafka consumer: error in processing message");
            e.printStackTrace();
        }
	}

	public void configureAndSendDataRequestFor(DataRequest dataFlowRequest) {
		String consentId = dataFlowRequest.getHiRequest().getConsent().getId();
		consentManagerClient.getConsentArtefact(consentId)
				.flatMap(caRep ->
						consentManagerClient.getConsentArtefactStatus(consentId)
								.flatMap(status -> status.getStatus().equals(NOTIFIED.toString()) ?
										dataRequestNotifier.notifyHip(
												dataFlowRequest, caRep.getConsentDetail().getHip().getId()) :
										Mono.error(ClientError.consentArtefactsYetToReachHIP())))
				.subscriberContext(ctx -> {
					Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
					return correlationId.map(id -> ctx.put(CORRELATION_ID, id))
							.orElseGet(() -> ctx.put(CORRELATION_ID, UUID.randomUUID().toString()));
				}).block();
	}
}
