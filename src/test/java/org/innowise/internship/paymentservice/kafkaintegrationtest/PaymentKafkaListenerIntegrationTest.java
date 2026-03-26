package org.innowise.internship.paymentservice.kafkaintegrationtest;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentInboxRequestDto;
import org.innowise.internship.paymentservice.model.dto.messagerequest.PaymentResultEventDto;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxStatus;
import org.innowise.internship.paymentservice.model.entity.log.PaymentLog;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxRequest;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxStatus;
import org.innowise.internship.paymentservice.repository.PaymentInboxRepository;
import org.innowise.internship.paymentservice.repository.PaymentLogsRepository;
import org.innowise.internship.paymentservice.repository.PaymentOutboxRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class PaymentKafkaListenerIntegrationTest extends AbstractPaymentIntegrationTest {

    @Autowired
    private PaymentInboxRepository inboxRepository;

    @Autowired
    private PaymentOutboxRepository outboxRepository;

    @Autowired
    private PaymentLogsRepository paymentLogsRepository;

    @Value("${app.kafka.topics.payment-requests}")
    private String paymentRequestsTopic;

    @Value("${app.kafka.topics.payment-results}")
    private String paymentResultsTopic;

    private Consumer<String, PaymentResultEventDto> resultConsumer;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        setupResultConsumer();
    }

    @AfterEach
    void tearDown() {
        if (resultConsumer != null) {
            resultConsumer.close();
        }
    }

    @Test
    void inboxSchema_shouldRejectInvalidDocumentFormat() {
        org.bson.Document invalidDoc = new org.bson.Document()
                .append("payment_id", UUID.randomUUID().toString())
                .append("order_id", 123L)
                .append("user_id", "user-1")
                .append("payment_amount", "not-a-number")
                .append("status", "PROCESSING")
                .append("timestamp", java.time.Instant.now());

        assertThatThrownBy(() -> mongoTemplate.getCollection("inbox_payment_requests").insertOne(invalidDoc))
                .isInstanceOf(com.mongodb.MongoWriteException.class)
                .hasMessageContaining("Document failed validation");
    }

    @Test
    void idempotency_shouldNotDuplicateLogIfOutboxWasMissing() throws Exception {
        String paymentId = "pay-ment-id";
        Long orderId = 999L;

        // inbox
        inboxRepository.save(PaymentInboxRequest.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .userId("user-1")
                .status(PaymentInboxStatus.PROCESSED)
                .paymentAmount(BigDecimal.TEN)
                .timestamp(java.time.Instant.now())
                .build());

        // log
        paymentLogsRepository.save(PaymentLog.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .userId("user-1")
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(BigDecimal.TEN)
                .timestamp(java.time.Instant.now())
                .build());

        // action: message duplicate
        CreatePaymentInboxRequestDto dto = CreatePaymentInboxRequestDto.builder()
                .paymentId(paymentId)
                .userId("user-1")
                .orderId(orderId)
                .paymentAmount(BigDecimal.TEN)
                .build();

        kafkaTemplate.send(paymentRequestsTopic, paymentId, dto).get();

        // extra log has not been created
        Thread.sleep(2000);
        assertThat(paymentLogsRepository.findByOrderId(orderId)).hasSize(1);
    }

    @Test
    void shouldProcessPaymentSuccessfully() throws Exception {
        String paymentId = UUID.randomUUID().toString();
        CreatePaymentInboxRequestDto dto = CreatePaymentInboxRequestDto.builder()
                .paymentId(paymentId)
                .userId("user-123")
                .orderId(456L)
                .paymentAmount(BigDecimal.valueOf(99.99))
                .build();

        kafkaTemplate.send(paymentRequestsTopic, paymentId, dto).get(5, TimeUnit.SECONDS);

        // scheduler should process inbox message
        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    PaymentInboxRequest inboxRequest = inboxRepository.findByPaymentId(paymentId).orElse(null);
                    assertThat(inboxRequest).isNotNull();
                    assertThat(inboxRequest.getStatus()).isEqualTo(PaymentInboxStatus.PROCESSED);
                });

        // payment log should be created
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<PaymentLog> logs = paymentLogsRepository.findByOrderId(456L);
                    assertThat(logs).isNotEmpty();

                    PaymentLog paymentLog = logs.getFirst();
                    assertThat(paymentLog.getUserId()).isEqualTo("user-123");
                    assertThat(paymentLog.getStatus()).isIn(PaymentStatus.SUCCESS, PaymentStatus.FAILURE);
                });

        // outbox message
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<PaymentLog> logs = paymentLogsRepository.findByOrderId(456L);
                    String gotPaymentId = logs.getFirst().getPaymentId();

                    assertThat(gotPaymentId).isEqualTo(paymentId);
                    PaymentOutboxRequest outboxRequest = outboxRepository.findByPaymentId(gotPaymentId).orElse(null);
                    assertThat(outboxRequest).isNotNull();
                    assertThat(outboxRequest.getStatus()).isEqualTo(PaymentOutboxStatus.SENT);
                });

        // result topic
        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    List<ConsumerRecord<String, PaymentResultEventDto>> records = pollRecords(Duration.ofSeconds(2));

                    boolean found = records.stream()
                            .anyMatch(record -> record.value().getOrderId().equals(456L));

                    assertThat(found).isTrue();
                });
    }

    @Test
    void shouldHandleDuplicateMessageAndSkipProcessing() throws Exception {
        String paymentId = UUID.randomUUID().toString();
        CreatePaymentInboxRequestDto dto = CreatePaymentInboxRequestDto.builder()
                .paymentId(paymentId)
                .userId("user-789")
                .orderId(101L)
                .paymentAmount(BigDecimal.valueOf(75.00))
                .build();

        // first message
        kafkaTemplate.send(paymentRequestsTopic, paymentId, dto).get(5, TimeUnit.SECONDS);

        Thread.sleep(2000);

        // duplicate
        kafkaTemplate.send(paymentRequestsTopic, paymentId, dto).get(5, TimeUnit.SECONDS);

        // only one payment log
        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(paymentLogsRepository.findByOrderId(101L)).hasSize(1));

        List<PaymentInboxRequest> inboxRequests = inboxRepository.findAllByPaymentId(paymentId);
        assertThat(inboxRequests).hasSize(1);
    }

    @Test
    void shouldProcessMultipleMessagesConcurrently() throws Exception {
        int messageCount = 5;

        List<PaymentResultEventDto> receivedEvents = new ArrayList<>();
        List<Long> expectedOrderIds = new ArrayList<>();

        for (int i = 1; i <= messageCount; i++) {
            long orderId = 200L + i;
            expectedOrderIds.add(orderId);

            CreatePaymentInboxRequestDto dto = CreatePaymentInboxRequestDto.builder()
                    .paymentId(UUID.randomUUID().toString())
                    .userId("user-" + i)
                    .orderId(orderId)
                    .paymentAmount(BigDecimal.valueOf(i * 10.0))
                    .build();

            kafkaTemplate.send(paymentRequestsTopic, dto.getPaymentId(), dto).get(5, TimeUnit.SECONDS);
        }

        // check if our messages are in result topic
        await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    ConsumerRecords<String, PaymentResultEventDto> polled =
                            resultConsumer.poll(Duration.ofMillis(200));

                    polled.forEach(record -> {
                        if (expectedOrderIds.contains(record.value().getOrderId())) {
                            receivedEvents.add(record.value());
                        }
                    });

                    assertThat(receivedEvents).hasSize(messageCount);
                });
    }

    private void setupResultConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafka.getBootstrapServers(),
                "test-result-group-" + UUID.randomUUID(),
                "false"
        );

        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES,
                "org.innowise.internship.paymentservice.model.dto.messagerequest");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentResultEventDto.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        var consumerFactory = new DefaultKafkaConsumerFactory<String, PaymentResultEventDto>(consumerProps);
        resultConsumer = consumerFactory.createConsumer();

        resultConsumer.subscribe(List.of(paymentResultsTopic));
        resultConsumer.poll(Duration.ofMillis(0));
    }

    private List<ConsumerRecord<String, PaymentResultEventDto>> pollRecords(Duration timeout) {

        List<ConsumerRecord<String, PaymentResultEventDto>> records = new ArrayList<>();
        long end = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < end) {

            ConsumerRecords<String, PaymentResultEventDto> polled =
                    resultConsumer.poll(Duration.ofMillis(200));

            polled.forEach(records::add);
        }

        return records;
    }
}