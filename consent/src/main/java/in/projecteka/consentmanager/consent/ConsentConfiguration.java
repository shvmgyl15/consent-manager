package in.projecteka.consentmanager.consent;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.clients.ConsentArtefactNotifier;
import in.projecteka.consentmanager.clients.ConsentManagerClient;
import in.projecteka.consentmanager.clients.PatientServiceClient;
import in.projecteka.consentmanager.kafkaStreams.consumer.HipConsentNotificationConsumer;
import in.projecteka.consentmanager.kafkaStreams.producer.ConsentNotificationProducer;
import in.projecteka.consentmanager.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.properties.KeyPairConfig;
import in.projecteka.consentmanager.properties.LinkServiceProperties;
import in.projecteka.library.clients.UserServiceClient;
import in.projecteka.library.common.CentralRegistry;
import in.projecteka.library.common.IdentityService;
import in.projecteka.library.common.ServiceAuthentication;
import in.projecteka.library.common.cache.CacheAdapter;
import io.vertx.pgclient.PgPool;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.KeyPair;
import java.security.PublicKey;

@Configuration
public class ConsentConfiguration {

    @Bean
    public ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper()
                .findAndRegisterModules();
        return mapper;
    }

    @Bean
    public ConsentRequestRepository consentRequestRepository(PgPool pgPool) {
        return new ConsentRequestRepository(pgPool);
    }

    @Bean
    public ConsentArtefactRepository consentArtefactRepository(PgPool pgPool) {
        return new ConsentArtefactRepository(pgPool);
    }

    @Bean
    public PatientServiceClient patientServiceClient(
            @Qualifier("customBuilder") WebClient.Builder builder,
            @Value("${consentmanager.authorization.header}") String authorizationHeader,
            IdentityService identityService,
            LinkServiceProperties linkServiceProperties) {
        return new PatientServiceClient(builder.build(),
                identityService::authenticate,
                linkServiceProperties.getUrl(),
                authorizationHeader);
    }

    @Bean
    public ConsentManager consentManager(
            UserServiceClient userServiceClient,
            ConsentServiceProperties consentServiceProperties,
            ConsentRequestRepository repository,
            ConsentArtefactRepository consentArtefactRepository,
            KeyPair keyPair,
            ConsentNotificationProducer consentNotificationProducer,
            CentralRegistry centralRegistry,
            ConceptValidator conceptValidator,
            GatewayServiceProperties gatewayServiceProperties,
            PatientServiceClient patientServiceClient,
            ConsentManagerClient consentManagerClient) {
        return new ConsentManager(userServiceClient,
                consentServiceProperties,
                repository,
                consentArtefactRepository,
                keyPair,
                consentNotificationProducer,
                centralRegistry,
                patientServiceClient,
                new CMProperties(gatewayServiceProperties.getClientId()),
                conceptValidator,
                new ConsentArtefactQueryGenerator(),
                consentManagerClient);
    }

    @Bean
    public ConsentManagerClient consentManagerClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                     ServiceAuthentication serviceAuthentication,
                                                     GatewayServiceProperties gatewayServiceProperties) {
        return new ConsentManagerClient(builder,
                gatewayServiceProperties.getBaseUrl(),
                gatewayServiceProperties,
                serviceAuthentication);
    }

    @Bean
    @ConditionalOnProperty(prefix = "consentmanager.scheduler",
            value = "consent-artefact-expiry-enabled",
            havingValue = "true")
    public ConsentScheduler consentScheduler(
            ConsentArtefactRepository consentArtefactRepository,
            ConsentNotificationProducer consentNotificationProducer) {
        return new ConsentScheduler(consentArtefactRepository, consentNotificationProducer);
    }

    @Bean
    public HipConsentNotificationConsumer hipConsentNotificationConsumer(
            ObjectMapper mapper,
            ConsentArtefactNotifier consentArtefactNotifier,
            ConsentArtefactRepository consentArtefactRepository,
            CacheAdapter<String, String> hipConsentArtefactStatus) {
        return new HipConsentNotificationConsumer(
                mapper,
                consentArtefactNotifier,
                consentArtefactRepository,
                hipConsentArtefactStatus);
    }

    @Bean
    @ConditionalOnProperty(prefix = "consentmanager.scheduler",
            value = "consent-request-expiry-enabled",
            havingValue = "true")
    ConsentRequestScheduler consentRequestScheduler(ConsentRequestRepository repository,
                                                    ConsentServiceProperties consentServiceProperties,
                                                    ConsentNotificationProducer consentNotificationProducer) {
        return new ConsentRequestScheduler(repository, consentServiceProperties, consentNotificationProducer);
    }

    @SneakyThrows
    @Bean
    public KeyPair keyPair(KeyPairConfig keyPairConfig) {
        return keyPairConfig.createSignArtefactKeyPair();
    }

    @Bean
    public PinVerificationTokenService pinVerificationTokenService(@Qualifier("keySigningPublicKey") PublicKey key,
                                                                   CacheAdapter<String, String> usedTokens) {
        return new PinVerificationTokenService(key, usedTokens);
    }

    @Bean
    public ConsentArtefactNotifier consentArtefactClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                         ServiceAuthentication serviceAuthentication,
                                                         GatewayServiceProperties gatewayServiceProperties) {
        return new ConsentArtefactNotifier(builder, serviceAuthentication::authenticate, gatewayServiceProperties);
    }
}