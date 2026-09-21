package com.seb.pedidos360.catalog.mappers;

import com.seb.pedidos360.catalog.dto.CreateProductRequestDTO;
import com.seb.pedidos360.catalog.models.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toEntity(CreateProductRequestDTO productDTO);
    CreateProductRequestDTO toDTO(Product product);
}
