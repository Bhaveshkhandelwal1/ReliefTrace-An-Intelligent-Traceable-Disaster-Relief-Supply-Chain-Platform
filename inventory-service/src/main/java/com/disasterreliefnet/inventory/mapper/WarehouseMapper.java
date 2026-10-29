package com.disasterreliefnet.inventory.mapper;

import com.disasterreliefnet.inventory.domain.Warehouse;
import com.disasterreliefnet.inventory.dto.AddressDto;
import com.disasterreliefnet.inventory.dto.CreateWarehouseRequest;
import com.disasterreliefnet.inventory.dto.UpdateWarehouseRequest;
import com.disasterreliefnet.inventory.dto.WarehouseResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    @Mapping(target = "warehouseId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "street", source = "address.street")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "state", source = "address.state")
    @Mapping(target = "country", source = "address.country")
    @Mapping(target = "zip", source = "address.zip")
    @Mapping(target = "capacityCbm", source = "capacity")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Warehouse toEntity(CreateWarehouseRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "warehouseId", ignore = true)
    @Mapping(target = "street", source = "address.street")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "state", source = "address.state")
    @Mapping(target = "country", source = "address.country")
    @Mapping(target = "zip", source = "address.zip")
    @Mapping(target = "capacityCbm", source = "capacity")
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(UpdateWarehouseRequest request, @MappingTarget Warehouse entity);

    @Mapping(target = "capacity", source = "capacityCbm")
    @Mapping(target = "address", expression = "java(toAddressDto(entity))")
    WarehouseResponse toResponse(Warehouse entity);

    default AddressDto toAddressDto(Warehouse entity) {
        if (entity == null) {
            return null;
        }
        return new AddressDto(
            entity.getStreet(),
            entity.getCity(),
            entity.getState(),
            entity.getCountry(),
            entity.getZip()
        );
    }
}
