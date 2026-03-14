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
        id = "init-payment-inbox-collection",
        order = "003",
        author = "vittorio-niko"
)
public class InitPaymentInboxMigration {

    @Execution
    public void execute(MongoTemplate mongoTemplate) {
        MongoJsonSchema schema = setupSchema();

        if (!mongoTemplate.collectionExists("inbox_payment_requests")) {
            mongoTemplate.createCollection("inbox_payment_requests",
                    CollectionOptions.empty().schema(schema));
        }

        setupIndices(mongoTemplate);
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        mongoTemplate.dropCollection("inbox_payment_requests");
    }

    private MongoJsonSchema setupSchema() {
        return MongoJsonSchema.builder()
                .required("msg_id", "order_id", "user_id",
                        "timestamp", "status", "payment_amount")
                .properties(
                        JsonSchemaProperty.string("msg_id"),
                        JsonSchemaProperty.int64("order_id"),
                        JsonSchemaProperty.string("user_id"),
                        JsonSchemaProperty.date("timestamp"),
                        JsonSchemaProperty.string("status"),
                        JsonSchemaProperty.decimal128("payment_amount")
                ).build();
    }

    private void setupIndices(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps("inbox_payment_requests").ensureIndex(
                new Index()
                        .named("idx_inbox_payment_requests_msg_id")
                        .on("msg_id", Sort.Direction.ASC)
                        .unique()
        );

        mongoTemplate.indexOps("inbox_payment_requests").ensureIndex(
                new Index()
                        .named("idx_inbox_payment_requests_status_timestamp")
                        .on("status", Sort.Direction.ASC)
                        .on("timestamp", Sort.Direction.ASC)
        );

        mongoTemplate.indexOps("inbox_payment_requests").ensureIndex(
                new Index()
                        .named("idx_inbox_payment_requests_ttl")
                        .on("timestamp", Sort.Direction.ASC)
                        .expire(Duration.ofDays(daysForMessageToLive))
        );
    }

    private static final Integer daysForMessageToLive = 7;
}
