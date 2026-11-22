package com.seb.pedidos360.bff.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class ProductBffController {
    private final WebClient webClient;

    @Value("${clients.catalog.url}")
    private String catalogUrl;

    @GetMapping
    public Flux<Object> getAllProducts() {
        return webClient.get()
                .uri(catalogUrl + "/api/catalog/products")
                .retrieve()
                .bodyToFlux(Object.class);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Object> createProduct(
            @RequestPart("product") Object productRequest,
            @RequestPart("images") Flux<FilePart> imageFiles) {

        return imageFiles.collectList()
                .flatMap(files -> {
                    MultipartBodyBuilder builder = new MultipartBodyBuilder();
                    builder.part("product", productRequest, MediaType.APPLICATION_JSON);

                    for (FilePart file : files) {
                        MediaType contentType = file.headers().getContentType() != null
                                ? file.headers().getContentType()
                                : MediaType.APPLICATION_OCTET_STREAM;

                        builder.asyncPart("images", file.content(), DataBuffer.class)
                                .filename(file.filename())
                                .contentType(contentType);
                    }

                    return webClient.post()
                            .uri(catalogUrl + "/api/catalog/create-product")
                            .body(BodyInserters.fromMultipartData(builder.build()))
                            .retrieve()
                            .bodyToMono(Object.class);
                });
    }
    @PutMapping("/products/{uuid}")
    public Mono<Object> updateProduct(@PathVariable UUID uuid, @RequestBody Mono<Object> productRequest) {
        return webClient.put()
                .uri(catalogUrl + "/api/catalog/products/" + uuid)
                .body(productRequest, Object.class)
                .retrieve()
                .bodyToMono(Object.class);
    }

    @GetMapping("/products/{uuid}")
    public Mono<Object> getProduct(@PathVariable UUID uuid) {
        return webClient.get()
                .uri(catalogUrl + "/api/catalog/products/" + uuid)
                .retrieve()
                .bodyToMono(Object.class);
    }
}
