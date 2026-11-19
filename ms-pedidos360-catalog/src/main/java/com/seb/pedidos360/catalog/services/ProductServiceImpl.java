package com.seb.pedidos360.catalog.services;

import com.seb.pedidos360.catalog.dto.product.input.CreateProductRequestDTO;
import com.seb.pedidos360.catalog.dto.product.input.DeductStockRequestDTO;
import com.seb.pedidos360.catalog.dto.product.input.UpdateProductRequestDTO;
import com.seb.pedidos360.catalog.dto.audit.AuditDTO;
import com.seb.pedidos360.catalog.dto.product.output.ProductResponseDTO;
import com.seb.pedidos360.catalog.dto.report.ProductQuantityReportDTO;
import com.seb.pedidos360.catalog.dto.report.ReportDTO;
import com.seb.pedidos360.catalog.mappers.ProductMapper;
import com.seb.pedidos360.catalog.models.Product;
import com.seb.pedidos360.catalog.repositories.ProductRepository;
import com.seb.pedidos360.catalog.services.uploadimage.UploadImage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final R2dbcEntityTemplate r2Template;
    private final UploadImage cloudinaryClient;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.orders}")
    private String reportTopicName;

    @Value("${kafka.topics.audit}")
    private String auditEventTopicName;

    @Override
    @Transactional
    public Mono<ProductResponseDTO> saveProduct(CreateProductRequestDTO productDTO, Flux<FilePart> imageFiles, String userEmail, String userName, String userRole) {
        return imageFiles
                .flatMap(cloudinaryClient::uploadImage)
                .collectList()
                .flatMap(uploadedUrls -> {
                    Product product = productMapper.toEntity(productDTO);
                    product.setImagesUrl(new HashSet<>(uploadedUrls));
                    return productRepository.save(product)
                            .map(productSaved -> {
                                AuditDTO auditDTO = new AuditDTO(productSaved.getUuid(), "New Product Created", userEmail, userRole,
                                        "Product Created by: " + userName);
                                ReportDTO reportDTO = new ReportDTO(productSaved.getUuid(), "Product Created", productSaved.getPrice(), Instant.now(),
                                        List.of(new ProductQuantityReportDTO(productSaved.getUuid(), productSaved.getStock().intValue())));

                                kafkaTemplate.send(reportTopicName, productSaved.getUuid().toString(), reportDTO);
                                kafkaTemplate.send(auditEventTopicName, productSaved.getUuid().toString(), auditDTO);

                                return productMapper.toDTO(productSaved);
                            });
                });
    }

    @Override
    @Transactional
    public Mono<ProductResponseDTO> updateProduct(UpdateProductRequestDTO updatedProduct, UUID productUuid, String userEmail, String userName, String userRole) {
        return productRepository.findByUuid(productUuid)
                .flatMap(product -> {
                    product.setPrice(updatedProduct.price());
                    product.setStock(updatedProduct.stock());
                    return r2Template.update(product)
                            .map(productUpdated -> {
                                AuditDTO auditDTO = new AuditDTO(productUpdated.getUuid(), "Product Updated", userEmail, userRole,
                                        "Product Updated By: " + userName);
                                ReportDTO reportDTO = new ReportDTO(productUpdated.getUuid(), "Product Created", productUpdated.getPrice(), Instant.now(),
                                        List.of(new ProductQuantityReportDTO(productUpdated.getUuid(), productUpdated.getStock().intValue())));

                                kafkaTemplate.send(reportTopicName, productUpdated.getUuid().toString(), reportDTO);
                                kafkaTemplate.send(auditEventTopicName, productUpdated.getUuid().toString(), auditDTO);
                                return productMapper.toDTO(productUpdated);
                            });
                })
                .switchIfEmpty(Mono.error(new NoSuchElementException("Product with id: " + productUuid + " does not exist")));
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<ProductResponseDTO> getProductByUuid(UUID productUuid) {
        return productRepository.findByUuid(productUuid)
                .map(productMapper::toDTO)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Product with id: " + productUuid + " does not exist")));
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll()
                .map(productMapper::toDTO);
    }

    @Override
    public Flux<ProductResponseDTO> deductStock(List<DeductStockRequestDTO> deductStockRequests, String userEmail, String userName, String userRole) {
        return Flux.fromIterable(deductStockRequests)
                .flatMap(deductStockRequestDTO -> productRepository.findByUuid(deductStockRequestDTO.productUuid())
                        .switchIfEmpty(Mono.error(new NoSuchElementException("Product with uuid: " + deductStockRequestDTO.productUuid() + " not found")))
                        .flatMap(product -> {
                            if (product.getStock() >= deductStockRequestDTO.quantity()) {
                                Long stockRestante = product.getStock() - deductStockRequestDTO.quantity();
                                product.setStock(stockRestante);

                                AuditDTO auditDTO = new AuditDTO(product.getUuid(), "Reduced stock product ", userEmail, userRole,
                                        "Stock reduced by: " + deductStockRequestDTO.quantity() + ", by: " + userName);

                                ReportDTO reportDTO = new ReportDTO(product.getUuid(), "Reduced Stock", product.getPrice(), Instant.now(),
                                        List.of(new ProductQuantityReportDTO(product.getUuid(), product.getStock().intValue())));

                                kafkaTemplate.send(reportTopicName, product.getUuid().toString(), reportDTO);
                                kafkaTemplate.send(auditEventTopicName, product.getUuid().toString(), auditDTO);

                                return r2Template.update(product)
                                        .map(productMapper::toDTO);
                            }
                            return Mono.error(new IllegalArgumentException("Insuficient stock"));
                        })
                );

    }
}
