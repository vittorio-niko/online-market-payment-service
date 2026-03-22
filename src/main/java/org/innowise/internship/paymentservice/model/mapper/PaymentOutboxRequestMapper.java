package org.innowise.internship.paymentservice.model.mapper;

import lombok.NonNull;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentOutboxRequestDto;
import org.innowise.internship.paymentservice.model.dto.messagerequest.PaymentResultEventDto;
import org.innowise.internship.paymentservice.model.entity.log.PaymentLog;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentOutboxRequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    PaymentOutboxRequest toPaymentOutboxRequest(
            @NonNull CreatePaymentOutboxRequestDto dto
    );

    CreatePaymentOutboxRequestDto toCreatePaymentOutboxRequestDto(
            @NonNull PaymentLog paymentLog
    );

    PaymentResultEventDto toPaymentResultEventDto(
            @NonNull PaymentOutboxRequest request
    );
}
