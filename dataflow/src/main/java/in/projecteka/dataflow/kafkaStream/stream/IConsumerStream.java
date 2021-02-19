package in.projecteka.dataflow.kafkaStream.stream;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface IConsumerStream {
	// PS: this is not the topic name
	String INPUT = "input-hip-data-flow-request-queue";
	
	@Input(INPUT)
    SubscribableChannel process();
}
