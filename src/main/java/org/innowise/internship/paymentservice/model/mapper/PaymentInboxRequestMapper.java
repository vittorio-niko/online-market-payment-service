package org.innowise.internship.paymentservice.model.mapper;

import lombok.NonNull;
import org.innowise.internship.paymentservice.model.dto.log.request.CreatePaymentLogRequestDto;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentInboxRequestDto;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentInboxRequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    @Mapping(target = "status", ignore = true)
    PaymentInboxRequest toPaymentInboxRequest(
            @NonNull CreatePaymentInboxRequestDto dto
    );

    @Mapping(target = "status", ignore = true)
    CreatePaymentLogRequestDto toCreatePaymentLogRequestDto(
            @NonNull CreatePaymentInboxRequestDto dto
    );
}
