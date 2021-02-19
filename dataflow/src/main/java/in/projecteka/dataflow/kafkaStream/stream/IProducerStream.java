package in.projecteka.dataflow.kafkaStream.stream;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface IProducerStream {

    String OUTPUT = "output-hip-data-flow-request-queue";

    @Output(OUTPUT)
    MessageChannel produce();
}
