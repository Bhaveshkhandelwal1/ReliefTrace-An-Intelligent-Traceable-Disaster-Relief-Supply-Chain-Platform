package com.disasterreliefnet.disaster.mapper;

import com.disasterreliefnet.disaster.domain.Disaster;
import com.disasterreliefnet.disaster.dto.CreateDisasterRequest;
import com.disasterreliefnet.disaster.dto.DisasterResponse;
import com.disasterreliefnet.disaster.dto.GeoAreaDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DisasterMapper {

    @Mapping(target = "disasterId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "latitude", source = "request.affectedArea.latitude")
    @Mapping(target = "longitude", source = "request.affectedArea.longitude")
    @Mapping(target = "radiusKm", source = "request.affectedArea.radiusKm")
    @Mapping(target = "regionName", source = "request.affectedArea.regionName")
    @Mapping(target = "country", source = "request.affectedArea.country")
    @Mapping(target = "estimatedAffectedPeople", constant = "0L")
    @Mapping(target = "status", expression = "java(com.disasterreliefnet.common.enums.DisasterStatus.ACTIVE)")
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "closureReason", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "updates", ignore = true)
    Disaster toEntity(CreateDisasterRequest request);

    default DisasterResponse toResponse(Disaster disaster) {
        if (disaster == null) {
            return null;
        }
        GeoAreaDto geoArea = new GeoAreaDto(
            disaster.getLatitude(),
            disaster.getLongitude(),
            disaster.getRadiusKm(),
            disaster.getRegionName(),
            disaster.getCountry()
        );
        return new DisasterResponse(
            disaster.getDisasterId(),
            disaster.getName(),
            disaster.getType(),
            disaster.getSeverityLevel(),
            disaster.getDescription(),
            geoArea,
            disaster.getEstimatedAffectedPeople(),
            disaster.getStatus(),
            disaster.getOccurredAt(),
            disaster.getClosedAt(),
            disaster.getCreatedAt(),
            disaster.getUpdatedAt()
        );
    }
}
