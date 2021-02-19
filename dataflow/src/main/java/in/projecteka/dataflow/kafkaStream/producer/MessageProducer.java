package in.projecteka.dataflow.kafkaStream.producer;

import in.projecteka.dataflow.kafkaStream.stream.IProducerStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {
    private final Logger log = LoggerFactory.getLogger(MessageProducer.class);

    @Autowired
    private IProducerStream iProducerStream;

    public void produce(String message) {
        log.info("In produce message: {}", message);
        try {
            MessageChannel messageChannel = iProducerStream.produce();
            messageChannel.send(MessageBuilder.withPayload(message).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
