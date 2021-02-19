package in.projecteka.dataflow.kafkaStream.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.dataflow.kafkaStream.stream.IProducerStream;
import in.projecteka.library.common.TraceableMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {
    private final Logger log = LoggerFactory.getLogger(MessageProducer.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private IProducerStream iProducerStream;

    public void produce(TraceableMessage message) {
        log.info("In produce message: {}", message);
        try {
            MessageChannel messageChannel = iProducerStream.produce();
            messageChannel.send(MessageBuilder.withPayload(mapper.writeValueAsString(message)).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
