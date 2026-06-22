package org.innowise.internship.paymentservice.config.observability;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("k8s")
@Configuration
public class ObservabilityFilterConfig {

    @Bean
    ObservationPredicate noSchedulerSpans() {
        return (name, context) -> {
            if (name.equals("tasks.scheduled.execution")) {
                return false;
            }

            if (context != null && context.getContextualName() != null) {
                if (context.getContextualName().contains("payment-outbox")) {
                    return false;
                }
            }

            return true;
        };
    }
}
