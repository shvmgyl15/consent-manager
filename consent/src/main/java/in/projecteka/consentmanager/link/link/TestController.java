package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.kafkaStreams.producer.LinkEventProducer;
import in.projecteka.consentmanager.link.link.model.CCLinkEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class TestController {

    private final LinkEventProducer linkEventProducer;

    public TestController(LinkEventProducer linkEventProducer) {
        this.linkEventProducer = linkEventProducer;
    }

    @PostMapping("/test/producer")
    public Mono<Void> testProducer(@RequestBody CCLinkEvent message) {
        return linkEventProducer.publish(message);
    }

}
