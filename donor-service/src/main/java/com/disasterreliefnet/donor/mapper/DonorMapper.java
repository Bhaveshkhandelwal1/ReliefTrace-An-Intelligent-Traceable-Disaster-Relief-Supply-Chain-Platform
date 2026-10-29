package com.disasterreliefnet.donor.mapper;

import com.disasterreliefnet.donor.domain.Donor;
import com.disasterreliefnet.donor.dto.AddressDto;
import com.disasterreliefnet.donor.dto.CreateDonorRequest;
import com.disasterreliefnet.donor.dto.DonorResponse;
import com.disasterreliefnet.donor.dto.UpdateDonorRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DonorMapper {

    @Mapping(target = "donorId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "street", source = "request.address.street")
    @Mapping(target = "city", source = "request.address.city")
    @Mapping(target = "state", source = "request.address.state")
    @Mapping(target = "country", source = "request.address.country")
    @Mapping(target = "zip", source = "request.address.zip")
    @Mapping(target = "status", expression = "java(com.disasterreliefnet.common.enums.DonorStatus.ACTIVE)")
    @Mapping(target = "totalDonatedAmount", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "impactDeliveriesCount", constant = "0L")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "donations", ignore = true)
    Donor toEntity(CreateDonorRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "donorId", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "street", source = "request.address.street")
    @Mapping(target = "city", source = "request.address.city")
    @Mapping(target = "state", source = "request.address.state")
    @Mapping(target = "country", source = "request.address.country")
    @Mapping(target = "zip", source = "request.address.zip")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalDonatedAmount", ignore = true)
    @Mapping(target = "impactDeliveriesCount", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "donations", ignore = true)
    void updateEntityFromDto(UpdateDonorRequest request, @MappingTarget Donor entity);

    default DonorResponse toResponse(Donor donor) {
        if (donor == null) {
            return null;
        }
        AddressDto address = null;
        if (donor.getStreet() != null || donor.getCity() != null || donor.getState() != null || donor.getCountry() != null || donor.getZip() != null) {
            address = new AddressDto(
                donor.getStreet(),
                donor.getCity(),
                donor.getState(),
                donor.getCountry(),
                donor.getZip()
            );
        }
        return new DonorResponse(
            donor.getDonorId(),
            donor.getFirstName(),
            donor.getLastName(),
            donor.getEmail(),
            donor.getPhone(),
            donor.getDonorType(),
            address,
            donor.getTaxId(),
            donor.getStatus(),
            donor.getTotalDonatedAmount(),
            donor.getImpactDeliveriesCount(),
            donor.getCreatedAt(),
            donor.getUpdatedAt()
        );
    }
}
