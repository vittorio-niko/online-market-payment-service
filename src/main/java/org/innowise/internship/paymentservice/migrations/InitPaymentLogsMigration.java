package org.innowise.internship.paymentservice.migrations;

import io.mongock.api.annotations.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;

import java.time.Duration;

@ChangeUnit(
        id = "init-payment-logs-collection",
        order = "001",
        author = "vittorio-niko"
)
public class InitPaymentLogsMigration {

    private static final String[] statusValues = { "PENDING", "SUCCESS", "FAILURE", "REFUNDED" };
    private static final Integer daysForPaymentToLive = 1825; // 5 years

    @BeforeExecution
    public void beforeExecution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("payments")) {
            mongoTemplate.createCollection("payments",
                    CollectionOptions.empty().schema(setupSchema()));
        }
        setupIndices(mongoTemplate);
    }

    @Execution
    public void execution(MongoTemplate mongoTemplate) { }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) { }

    @RollbackBeforeExecution
    public void rollbackBeforeExecution(MongoTemplate mongoTemplate) {
        mongoTemplate.dropCollection("payments");
    }

    private void setupIndices(MongoTemplate mongoTemplate) {
        var indexOps = mongoTemplate.indexOps("payments");

        indexOps.ensureIndex(
                new Index()
                        .named("idx_payments_payment_id")
                        .on("payment_id", Sort.Direction.ASC)
                        .unique()
        );
        indexOps.ensureIndex(
                new Index()
                        .named("idx_payments_user_id_timestamp_status")
                        .on("user_id", Sort.Direction.ASC)
                        .on("timestamp", Sort.Direction.DESC)
                        .on("status", Sort.Direction.ASC)
        );
        indexOps.ensureIndex(
                new Index()
                        .named("idx_payments_order_id")
                        .on("order_id", Sort.Direction.ASC)
        );
        indexOps.ensureIndex(
                new Index()
                        .named("idx_payments_timestamp_index")
                        .on("timestamp", Sort.Direction.DESC)
                        .on("status", Sort.Direction.ASC)
        );
        indexOps.ensureIndex(
                new Index()
                        .named("idx_payments_ttl")
                        .on("timestamp", Sort.Direction.ASC)
                        .expire(Duration.ofDays(daysForPaymentToLive))
        );
    }

    private MongoJsonSchema setupSchema() {
        return MongoJsonSchema.builder()
                .required("payment_id", "order_id", "user_id", "status", "timestamp", "payment_amount")
                .properties(
                        JsonSchemaProperty.string("payment_id"),
                        JsonSchemaProperty.int64("order_id"),
                        JsonSchemaProperty.string("user_id"),
                        JsonSchemaProperty.string("status").possibleValues(statusValues),
                        JsonSchemaProperty.date("timestamp"),
                        JsonSchemaProperty.decimal128("payment_amount")
                ).build();
    }
}