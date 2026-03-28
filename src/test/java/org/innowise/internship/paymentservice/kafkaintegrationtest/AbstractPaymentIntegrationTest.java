package org.innowise.internship.paymentservice.kafkaintegrationtest;

import org.innowise.internship.paymentservice.repository.PaymentInboxRepository;
import org.innowise.internship.paymentservice.repository.PaymentLogsRepository;
import org.innowise.internship.paymentservice.repository.PaymentOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractPaymentIntegrationTest {

    @Container
    static final MongoDBContainer mongoDB = new MongoDBContainer("mongo:6.0")
            .withExposedPorts(27017);

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @Autowired
    private PaymentInboxRepository inboxRepository;
    @Autowired
    private PaymentLogsRepository paymentLogsRepository;
    @Autowired
    private PaymentOutboxRepository outboxRepository;

    @BeforeEach
    void cleanDatabase() {
        inboxRepository.deleteAll();
        paymentLogsRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    @Autowired
    protected MongoTemplate mongoTemplate;

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDB::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
