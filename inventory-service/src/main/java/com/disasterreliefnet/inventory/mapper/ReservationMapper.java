package com.disasterreliefnet.inventory.mapper;

import com.disasterreliefnet.inventory.domain.Reservation;
import com.disasterreliefnet.inventory.dto.ReservationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    ReservationResponse toResponse(Reservation entity);
}
