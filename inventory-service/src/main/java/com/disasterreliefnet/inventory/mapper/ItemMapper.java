package com.disasterreliefnet.inventory.mapper;

import com.disasterreliefnet.inventory.domain.Item;
import com.disasterreliefnet.inventory.dto.CreateItemRequest;
import com.disasterreliefnet.inventory.dto.ItemResponse;
import com.disasterreliefnet.inventory.dto.UpdateItemRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "itemId", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "quantityReserved", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "quantityOnHand", defaultExpression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Item toEntity(CreateItemRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "itemId", ignore = true)
    @Mapping(target = "warehouseId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "quantityOnHand", ignore = true)
    @Mapping(target = "quantityReserved", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(UpdateItemRequest request, @MappingTarget Item entity);

    @Mapping(target = "quantityAvailable", expression = "java(entity.getQuantityAvailable())")
    ItemResponse toResponse(Item entity);
}
