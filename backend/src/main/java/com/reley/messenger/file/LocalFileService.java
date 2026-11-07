package com.reley.messenger.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        name = "reley.file-storage.type",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalFileService implements FileService {

    private final Path storagePath;

    public LocalFileService(@Value("${reley.file-storage.path}") String storagePath) {
        this.storagePath =
                Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(storagePath);
    }

    @Override
    public Mono<Void> save(UUID fileId, Flux<DataBuffer> content) {

        Path filePath =
                storagePath.resolve(fileId.toString());

        return DataBufferUtils.write(
                        content,
                        filePath,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING).then();
    }

    @Override
    public Mono<Resource> load(UUID fileId) {

        Path filePath =
                storagePath.resolve(fileId.toString());

        return Mono.fromCallable(() -> {

            Resource resource = new FileSystemResource(filePath);

            if (!resource.exists()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "File not found"
                );
            }

            return resource;
        });
    }

    @Override
    public Mono<Void> delete(UUID fileId) {

        Path filePath = storagePath.resolve(fileId.toString());

        return Mono.fromCallable(() -> Files.deleteIfExists(filePath)).then();
    }
}
