package com.disasterreliefnet.donor.mapper;

import com.disasterreliefnet.donor.domain.Donation;
import com.disasterreliefnet.donor.domain.DonationItem;
import com.disasterreliefnet.donor.domain.Donor;
import com.disasterreliefnet.donor.dto.CreateDonationRequest;
import com.disasterreliefnet.donor.dto.DonationItemDto;
import com.disasterreliefnet.donor.dto.DonationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DonationMapper {

    @Mapping(target = "donationId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "donor", source = "donor")
    @Mapping(target = "status", expression = "java(request.donationType() == com.disasterreliefnet.common.enums.DonationType.IN_KIND ? com.disasterreliefnet.common.enums.DonationStatus.PLEDGED : com.disasterreliefnet.common.enums.DonationStatus.PENDING)")
    @Mapping(target = "receiptNumber", ignore = true)
    @Mapping(target = "receiptIssuedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "items", ignore = true)
    Donation toEntity(CreateDonationRequest request, Donor donor);

    @Mapping(target = "itemLineId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "donation", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    DonationItem toItemEntity(DonationItemDto dto);

    DonationItemDto toItemDto(DonationItem entity);

    List<DonationItemDto> toItemDtoList(List<DonationItem> entities);

    @Mapping(target = "donorId", source = "donation.donor.donorId")
    @Mapping(target = "items", source = "items")
    DonationResponse toResponse(Donation donation);
}
