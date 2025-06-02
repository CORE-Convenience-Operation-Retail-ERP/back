package com.core.erp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verified_device", uniqueConstraints = {
        @UniqueConstraint(name = "uq_verified_device", columnNames = {"deviceId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifiedDeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String phone;  // 인증된 사용자 전화번호

    @Column(nullable = false, length = 100)
    private String deviceId;  // 클라이언트에서 생성된 UUID

    @Column(length = 255)
    private String deviceName;  // userAgent (브라우저, OS 정보 등)

    @Column(nullable = false)
    private LocalDateTime verifiedAt;  // 인증된 시각

    @Column(nullable = false)
    private int attemptCount = 1; // 재인증 횟수

    @Column(nullable = false)
    private LocalDateTime lastAttemptAt;  // 마지막 인증 요청 시각

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.verifiedAt = now;
        this.lastAttemptAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.lastAttemptAt = LocalDateTime.now();
    }

    public void updateVerificationTime() {
        this.verifiedAt = LocalDateTime.now();
        this.attemptCount += 1;
    }
}