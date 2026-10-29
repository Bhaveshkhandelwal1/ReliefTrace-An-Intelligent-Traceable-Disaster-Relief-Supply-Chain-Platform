package com.disasterreliefnet.analytics.mapper;

import com.disasterreliefnet.analytics.domain.DisasterSummary;
import com.disasterreliefnet.analytics.dto.DisasterSummaryResponse;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for Analytics Service domain models and response DTOs.
 */
@Mapper(componentModel = "spring")
public interface AnalyticsMapper {

    DisasterSummaryResponse toDisasterSummaryResponse(DisasterSummary summary);
}
