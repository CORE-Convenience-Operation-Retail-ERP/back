package com.core.erp.service;

import com.core.erp.domain.VerifiedDeviceEntity;
import com.core.erp.repository.VerifiedDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerifiedDeviceService {

    private final VerifiedDeviceRepository repository;

    public void verifyDevice(String phone, String deviceId, String deviceName) {
        repository.findByDeviceId(deviceId).ifPresentOrElse(device -> {
            if (device.getPhone().equals(phone)) {
                device.updateVerificationTime();
                repository.save(device);
            } else {
                throw new IllegalStateException("이 기기는 이미 다른 사용자에게 인증되었습니다.");
            }
        }, () -> {
            VerifiedDeviceEntity newDevice = VerifiedDeviceEntity.builder()
                    .phone(phone)
                    .deviceId(deviceId)
                    .deviceName(deviceName)
                    .verifiedAt(LocalDateTime.now())
                    .lastAttemptAt(LocalDateTime.now())
                    .attemptCount(1)
                    .build();
            repository.save(newDevice);
        });
    }

    public boolean isDeviceVerified(String phone, String deviceId) {
        return repository.findByPhoneAndDeviceId(phone, deviceId).isPresent();
    }
}
