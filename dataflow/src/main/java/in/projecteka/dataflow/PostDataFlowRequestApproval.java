package in.projecteka.dataflow;

import in.projecteka.dataflow.kafkaStream.producer.HipDataflowRequestProducer;
import in.projecteka.dataflow.model.DataFlowRequestMessage;
import in.projecteka.library.common.TraceableMessage;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static in.projecteka.library.common.Constants.CORRELATION_ID;

@AllArgsConstructor
@Slf4j
@Service
public class PostDataFlowRequestApproval {
    private final HipDataflowRequestProducer hipDataflowRequestProducer;

    @SneakyThrows
    public Mono<Void> broadcastDataFlowRequest(
            String transactionId,
            in.projecteka.dataflow.model.DataFlowRequest dataFlowRequest) {
        DataFlowRequestMessage dataFlowRequestMessage = DataFlowRequestMessage.builder()
                .transactionId(transactionId)
                .dataFlowRequest(dataFlowRequest)
                .build();
        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(MDC.get(CORRELATION_ID))
                .message(dataFlowRequestMessage)
                .build();

        return Mono.create(monoSink -> {
            hipDataflowRequestProducer.produce(traceableMessage);
            log.info("Broadcasting data flow request with transaction id : " + transactionId);
            monoSink.success();
        });
    }
}
