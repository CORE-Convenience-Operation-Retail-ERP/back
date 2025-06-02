package com.core.erp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * 이미지 파일을 S3에 업로드
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        String fileName = generateFileName(file.getOriginalFilename(), folder);
        
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
            
        } catch (Exception e) {
            log.error("S3 이미지 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }

    /**
     * 엑셀 파일을 S3에 업로드
     */
    public String uploadExcel(byte[] excelData, String fileName) {
        String key = "excel/" + fileName;
        
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .build();

            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(new ByteArrayInputStream(excelData), excelData.length));

            return generatePresignedUrl(key);
            
        } catch (Exception e) {
            log.error("S3 엑셀 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("S3 엑셀 업로드 실패", e);
        }
    }

    /**
     * S3 파일 삭제
     */
    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", e.getMessage());
        }
    }

    /**
     * 임시 다운로드 URL 생성 (1시간 유효)
     */
    public String generatePresignedUrl(String key) {
        try {
            // S3Presigner에도 동일한 자격 증명 사용
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            
            S3Presigner presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
            String url = presignedGetObjectRequest.url().toString();
            
            presigner.close();
            return url;
            
        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: {}", e.getMessage());
            throw new RuntimeException("다운로드 URL 생성 실패", e);
        }
    }

    /**
     * 파일명 생성
     */
    private String generateFileName(String originalFileName, String folder) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return folder + "/" + UUID.randomUUID().toString() + extension;
    }

    /**
     * URL에서 S3 키 추출
     */
    private String extractKeyFromUrl(String fileUrl) {
        if (fileUrl.contains("amazonaws.com/")) {
            return fileUrl.substring(fileUrl.indexOf("amazonaws.com/") + 14);
        }
        return fileUrl;
    }
} 