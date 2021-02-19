package in.projecteka.consentmanager.kafkaStreams.stream;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface IHipLinkStream {

    String OUTPUT = "output-cm-hip-link-queue";

    @Output(OUTPUT)
    MessageChannel send();
}
