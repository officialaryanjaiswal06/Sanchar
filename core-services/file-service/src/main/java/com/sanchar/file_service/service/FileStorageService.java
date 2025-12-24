package com.sanchar.file_service.service;

import com.sanchar.file_service.client.UserClient;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {
    private final MinioClient minioClient;


    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    public String uploadProfilePicture(MultipartFile file, String userId) {
        try {
            // 1. Ensure Bucket Exists
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                log.info("Bucket '{}' not found. Creating it...", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());

            }

            // 2. Generate Unique Filename (user-ID + random UUID + extension)
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg"; // default to jpg if unknown

            // Format: user-123-ab12cd34.jpg
            String newFilename = "user-" + userId + "-" + UUID.randomUUID().toString().substring(0, 8) + extension;

            // 3. Upload File
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newFilename)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType()) // "image/jpeg", "image/png"
                            .build()
            );

            log.info("File uploaded successfully: {}", newFilename);


            return minioUrl + "/" + bucketName + "/" + newFilename;

        } catch (Exception e) {
            log.error("MinIO Upload Error: ", e);
            throw new RuntimeException("Image upload failed: " + e.getMessage());
        }
    }
}
