package in.projecteka.dataflow.config;

import in.projecteka.dataflow.kafkaStream.stream.IConsumerStream;
import in.projecteka.dataflow.kafkaStream.stream.IProducerStream;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;

@EnableBinding(value = {Source.class, IProducerStream.class, IConsumerStream.class})
public class StreamsConfig {
}

