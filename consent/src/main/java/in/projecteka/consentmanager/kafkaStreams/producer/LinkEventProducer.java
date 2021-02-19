package in.projecteka.consentmanager.kafkaStreams.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.kafkaStreams.stream.IHipLinkStream;
import in.projecteka.consentmanager.link.link.model.CCLinkEvent;
import in.projecteka.library.common.TraceableMessage;
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
public class LinkEventProducer {
    private static final Logger logger = LoggerFactory.getLogger(LinkEventProducer.class);

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    IHipLinkStream iHipLinkStream;

    public Mono<Void> publish(CCLinkEvent message) {
        return Mono.create(monoSink -> {
            broadcastLinkEvent(message);
            monoSink.success();
        });
    }

    private void broadcastLinkEvent(CCLinkEvent message) {
        TraceableMessage traceableMessage = TraceableMessage.builder()
                .correlationId(MDC.get(CORRELATION_ID))
                .message(message)
                .build();
        logger.debug("Raising LINK Event: " +
                        "correlation id: {}, " +
                        "patient: {}, hip: {}",
                traceableMessage.getCorrelationId(),
                message.getHealthNumber(), message.getHipId());
        try {
            MessageChannel messageChannel = iHipLinkStream.send();
            messageChannel.send(MessageBuilder.withPayload(mapper.writeValueAsString(traceableMessage)).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("Raised LINK Event: correlation Id: {}",  traceableMessage.getCorrelationId());
    }
}
