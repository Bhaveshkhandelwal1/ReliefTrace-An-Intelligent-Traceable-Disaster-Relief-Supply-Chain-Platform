package com.disasterreliefnet.disaster.mapper;

import com.disasterreliefnet.disaster.domain.Disaster;
import com.disasterreliefnet.disaster.domain.DisasterUpdate;
import com.disasterreliefnet.disaster.dto.CreateDisasterUpdateRequest;
import com.disasterreliefnet.disaster.dto.DisasterUpdateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DisasterUpdateMapper {

    @Mapping(target = "updateId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "disaster", source = "disaster")
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "content", source = "request.content")
    @Mapping(target = "source", source = "request.source")
    @Mapping(target = "postedBy", source = "postedBy")
    @Mapping(target = "createdAt", ignore = true)
    DisasterUpdate toEntity(CreateDisasterUpdateRequest request, Disaster disaster, String postedBy);

    @Mapping(target = "disasterId", source = "update.disaster.disasterId")
    DisasterUpdateResponse toResponse(DisasterUpdate update);
}
