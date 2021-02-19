package in.projecteka.dataflow.kafkaStream.stream;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface IHipDataflowRequestStream {
	// PS: this is not the topic name
	String INPUT = "input-hip-data-flow-request-queue";
	
	@Input(INPUT)
    SubscribableChannel process();

	String OUTPUT = "output-hip-data-flow-request-queue";

	@Output(OUTPUT)
	MessageChannel produce();
}
