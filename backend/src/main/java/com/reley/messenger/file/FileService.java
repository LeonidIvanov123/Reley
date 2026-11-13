package com.reley.messenger.file;

import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface FileService {

    Mono<Void> save(UUID fileId, Flux<DataBuffer> content);

    Mono<Resource> load(UUID fileId);

    Mono<Void> delete(UUID fileId);

}
