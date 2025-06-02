package com.core.erp.repository;

import com.core.erp.domain.VerifiedDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerifiedDeviceRepository extends JpaRepository<VerifiedDeviceEntity, Long> {
    Optional<VerifiedDeviceEntity> findByDeviceId(String deviceId);
    Optional<VerifiedDeviceEntity> findByPhoneAndDeviceId(String phone, String deviceId);
    Optional<VerifiedDeviceEntity> findByPhone(String phone);

}