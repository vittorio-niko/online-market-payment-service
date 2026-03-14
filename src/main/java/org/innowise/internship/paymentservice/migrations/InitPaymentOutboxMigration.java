package org.innowise.internship.paymentservice.migrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;

import java.time.Duration;

@ChangeUnit(
        id = "init-payment-outbox-collection",
        order = "004",
        author = "vittorio-niko"
)
public class InitPaymentOutboxMigration {

    @Execution
    public void execute(MongoTemplate mongoTemplate) {
        MongoJsonSchema schema = setupSchema();

        if (!mongoTemplate.collectionExists("outbox_payment_requests")) {
            mongoTemplate.createCollection("outbox_payment_requests",
                    CollectionOptions.empty().schema(schema));
        }

        setupIndices(mongoTemplate);
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        mongoTemplate.dropCollection("outbox_payment_requests");
    }

    private MongoJsonSchema setupSchema() {
        return MongoJsonSchema.builder()
                .required("payment_id", "order_id", "user_id",
                        "timestamp", "status", "attempts")
                .properties(
                        JsonSchemaProperty.string("payment_id"),
                        JsonSchemaProperty.int64("order_id"),
                        JsonSchemaProperty.string("user_id"),
                        JsonSchemaProperty.date("timestamp"),
                        JsonSchemaProperty.string("status"),
                        JsonSchemaProperty.int32("attempts")
                ).build();
    }

    private void setupIndices(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps("outbox_payment_requests").ensureIndex(
                new Index()
                        .named("idx_outbox_payment_requests_payment_id")
                        .on("payment_id", Sort.Direction.ASC)
                        .unique()
        );

        mongoTemplate.indexOps("outbox_payment_requests").ensureIndex(
                new Index()
                        .named("idx_outbox_payment_requests_status_timestamp")
                        .on("status", Sort.Direction.ASC)
                        .on("timestamp", Sort.Direction.ASC)
        );

        mongoTemplate.indexOps("outbox_payment_requests").ensureIndex(
                new Index()
                        .named("idx_outbox_payment_requests_ttl")
                        .on("timestamp", Sort.Direction.ASC)
                        .expire(Duration.ofDays(daysForMessageToLive))
        );
    }

    private static final Integer daysForMessageToLive = 7;
}
