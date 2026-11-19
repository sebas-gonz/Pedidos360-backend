package com.seb.pedidos360.catalog.services.uploadimage;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface UploadImage {
    Mono<String> uploadImage(FilePart filePart);
}
