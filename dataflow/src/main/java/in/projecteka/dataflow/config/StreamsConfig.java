package in.projecteka.dataflow.config;

import in.projecteka.dataflow.kafkaStream.stream.IHipDataflowRequestStream;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;

@EnableBinding(value = {Source.class, IHipDataflowRequestStream.class})
public class StreamsConfig {
}

