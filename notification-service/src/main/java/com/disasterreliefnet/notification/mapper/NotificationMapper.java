package com.disasterreliefnet.notification.mapper;

import com.disasterreliefnet.notification.domain.NotificationLog;
import com.disasterreliefnet.notification.domain.NotificationPreference;
import com.disasterreliefnet.notification.dto.NotificationLogResponse;
import com.disasterreliefnet.notification.dto.NotificationPreferenceResponse;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for Notification Service entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationLogResponse toLogResponse(NotificationLog log);

    NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference);
}
