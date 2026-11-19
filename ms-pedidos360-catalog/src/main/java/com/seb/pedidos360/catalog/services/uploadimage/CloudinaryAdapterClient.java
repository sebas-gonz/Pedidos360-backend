package com.seb.pedidos360.catalog.services.uploadimage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CloudinaryAdapterClient implements UploadImage{
    private final Cloudinary cloudinary;
    @Override
    public Mono<String> uploadImage(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(bytes -> Mono.fromCallable( () -> {
                    Map result = cloudinary.uploader().upload(bytes, ObjectUtils.emptyMap());
                    return result.get("secure_url").toString();
                })).subscribeOn(Schedulers.boundedElastic());
    }
}
