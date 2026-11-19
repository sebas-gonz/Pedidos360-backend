package com.seb.pedidos360.catalog.mappers;

import com.seb.pedidos360.catalog.dto.product.input.CreateProductRequestDTO;
import com.seb.pedidos360.catalog.dto.product.output.ProductResponseDTO;
import com.seb.pedidos360.catalog.models.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toEntity(CreateProductRequestDTO productDTO);
    ProductResponseDTO toDTO(Product product);
}
