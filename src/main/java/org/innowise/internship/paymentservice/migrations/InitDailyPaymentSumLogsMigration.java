package org.innowise.internship.paymentservice.migrations;

import io.mongock.api.annotations.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.schema.JsonSchemaProperty;
import org.springframework.data.mongodb.core.schema.MongoJsonSchema;

@ChangeUnit(
        id = "init-daily-payment-sums-collection",
        order = "002",
        author = "vittorio-niko"
)
public class InitDailyPaymentSumLogsMigration {

    private static final Integer daysForPaymentSumToLive = 1825; // 5 years

    @BeforeExecution
    public void beforeExecution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("daily_payment_sums")) {
            mongoTemplate.createCollection("daily_payment_sums",
                    CollectionOptions.empty().schema(setupSchema()));
        }
        setupIndices(mongoTemplate);
    }

    @Execution
    public void execute(MongoTemplate mongoTemplate) { }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        mongoTemplate.dropCollection("daily_payment_sums");
    }

    @RollbackBeforeExecution
    public void rollbackBeforeExecution(MongoTemplate mongoTemplate) {
        mongoTemplate.dropCollection("daily_payment_sums");
    }

    private MongoJsonSchema setupSchema() {
        return MongoJsonSchema.builder()
                .required("date", "payment_sum")
                .properties(
                        JsonSchemaProperty.date("date"),
                        JsonSchemaProperty.decimal128("payment_sum")
                ).build();
    }

    private void setupIndices(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps("daily_payment_sums").ensureIndex(
                new Index()
                        .named("idx_daily_payment_sums_date")
                        .on("date", Sort.Direction.DESC)
                        .unique()
        );

        mongoTemplate.indexOps("daily_payment_sums").ensureIndex(
                new Index()
                        .named("idx_daily_payment_sums_ttl")
                        .on("date", Sort.Direction.ASC)
                        .expire(java.time.Duration.ofDays(daysForPaymentSumToLive))
        );
    }
}