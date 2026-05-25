package org.innowise.internship.paymentservice.kafkaintegrationtest;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.innowise.internship.paymentservice.model.entity.log.PaymentLog;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxRequest;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractPaymentIntegrationTest {

    @Container
    static final MongoDBContainer mongoDB = new MongoDBContainer("mongo:6.0")
            .withExposedPorts(27017);

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.3.0")
    );

    @Autowired
    protected MongoTemplate mongoTemplate;

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    private static boolean topicsCreated = false;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDB::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "payment_service_db");

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.key-deserializer",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add("spring.kafka.consumer.value-deserializer",
                () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages",
                () -> "org.innowise.internship.paymentservice.model.dto.messagerequest");
        registry.add("spring.kafka.consumer.properties.spring.json.value.default.type",
                () -> "org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentInboxRequestDto");

        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");

        registry.add("app.kafka.topics.payment-requests", () -> "payment-requests-topic");
        registry.add("app.kafka.topics.payment-results", () -> "payment-results-topic");
        registry.add("app.kafka.outbox.publish-delay", () -> "1000");
        registry.add("app.kafka.outbox.batch-size", () -> "10");
        registry.add("app.kafka.outbox.retries", () -> "3");
    }

    @BeforeAll
    static void createTopics() {
        if (!topicsCreated) {
            try (AdminClient admin = AdminClient.create(Map.of(
                    "bootstrap.servers", kafka.getBootstrapServers()
            ))) {
                List<NewTopic> topics = List.of(
                        new NewTopic("payment-requests-topic", 1, (short) 1),
                        new NewTopic("payment-results-topic", 1, (short) 1)
                );

                CreateTopicsResult result = admin.createTopics(topics);
                result.all().get(10, TimeUnit.SECONDS);
                topicsCreated = true;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create Kafka topics", e);
            }
        }
    }

    protected void clearMongoCollections() {
        mongoTemplate.remove(new Query(), PaymentInboxRequest.class);
        mongoTemplate.remove(new Query(), PaymentLog.class);
        mongoTemplate.remove(new Query(), PaymentOutboxRequest.class);
    }
}
