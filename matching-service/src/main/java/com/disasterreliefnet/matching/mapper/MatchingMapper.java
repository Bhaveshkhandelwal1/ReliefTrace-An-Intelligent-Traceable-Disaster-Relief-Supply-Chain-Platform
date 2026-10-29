package com.disasterreliefnet.matching.mapper;

import com.disasterreliefnet.matching.domain.Allocation;
import com.disasterreliefnet.matching.dto.AllocationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MatchingMapper {

    AllocationResponse toResponse(Allocation allocation);
}
