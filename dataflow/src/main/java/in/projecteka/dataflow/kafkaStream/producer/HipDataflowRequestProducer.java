package in.projecteka.dataflow.kafkaStream.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.dataflow.kafkaStream.stream.IHipDataflowRequestStream;
import in.projecteka.library.common.TraceableMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class HipDataflowRequestProducer {
    private final Logger log = LoggerFactory.getLogger(HipDataflowRequestProducer.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private IHipDataflowRequestStream iHipDataFlowRequestStream;

    public void produce(TraceableMessage message) {
        log.info("In produce message: {}", message);
        try {
            MessageChannel messageChannel = iHipDataFlowRequestStream.produce();
            messageChannel.send(MessageBuilder.withPayload(mapper.writeValueAsString(message)).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
