package com.seb.pedidos360.catalog.mappers;

import com.seb.pedidos360.catalog.dto.product.input.CreateProductRequestDTO;
import com.seb.pedidos360.catalog.dto.product.output.ProductResponseDTO;
import com.seb.pedidos360.catalog.models.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toEntity(CreateProductRequestDTO productDTO);
    @Mapping(source = "uuid", target = "productUuid")
    ProductResponseDTO toDTO(Product product);
}
