package com.core.erp.service;

import com.core.erp.domain.PartTimerEntity;
import com.core.erp.domain.StoreEntity;
import com.core.erp.dto.partTimer.PartTimerDTO;
import com.core.erp.dto.partTimer.PartTimerSearchDTO;
import com.core.erp.repository.AttendanceRepository;
import com.core.erp.repository.PartTimerRepository;
import com.core.erp.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@ToString
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartTimeService {

    private final PartTimerRepository partTimerRepository;
    private final StoreRepository storeRepository;
    private final AttendanceRepository attendanceRepository;

    private final String uploadDir = System.getProperty("user.dir") + "/uploads/parttimer/";

    // 역할 헬퍼 메서드
    private boolean isStore(String role) {
        return "STORE".equals(role);
    }

    private boolean isHQ(String role) {
        return role != null && role.startsWith("HQ");
    }

    private boolean isMaster(String role) {
        return "MASTER".equals(role);
    }

    public List<PartTimerDTO> searchPartTimers(String role, Integer storeId, PartTimerSearchDTO searchDTO) {
        Pageable pageable = PageRequest.of(searchDTO.getPage(), searchDTO.getSize());
        Page<PartTimerEntity> result;

        if (isHQ(role)) {
            result = partTimerRepository.searchHeadquarterSide(
                    searchDTO.getPartName(),
                    searchDTO.getPosition(),
                    searchDTO.getPartStatus(),
                    storeId,
                    searchDTO.getPartTimerId(),
                    pageable
            );
        } else if (isStore(role) || isMaster(role)) {
            result = partTimerRepository.searchStoreSide(
                    storeId,
                    searchDTO.getPartName(),
                    searchDTO.getPosition(),
                    searchDTO.getPartStatus(),
                    searchDTO.getPartTimerId(),
                    pageable
            );
        } else {
            throw new RuntimeException("권한이 없습니다.");
        }

        return result.map(PartTimerDTO::new).getContent();
    }

    public Page<PartTimerDTO> findAllPartTimers(String role, Integer storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PartTimerEntity> result;
        log.info("role = {}, storeId = {}", role, storeId);

        if (isHQ(role)) {
            result = partTimerRepository.findAll(pageable);
        } else if (isStore(role) || isMaster(role)) {
            result = partTimerRepository.findByStoreStoreId(storeId, pageable);
        } else {
            throw new RuntimeException("권한이 없습니다.");
        }

        return result.map(pt -> {
            PartTimerDTO dto = new PartTimerDTO(pt);

            // 오늘 출근 여부 판단
            boolean isCheckedIn = attendanceRepository.existsByPartTimerIdAndStoreIdAndAttendDate(
                    (long) pt.getPartTimerId(),
                    pt.getStore().getStoreId(),
                    LocalDate.now()
            );

            dto.setCheckedInToday(isCheckedIn);
            return dto;
        });    }

    public PartTimerDTO findPartTimerById(String role, Integer storeId, Integer partTimerId) {
        PartTimerEntity entity = partTimerRepository.findById(partTimerId)
                .orElseThrow(() -> new RuntimeException("해당 아르바이트를 찾을 수 없습니다."));

        if ((isStore(role) || isMaster(role)) &&
                !Objects.equals(entity.getStore().getStoreId(), storeId)) {
            throw new RuntimeException("본인 지점의 아르바이트만 조회할 수 있습니다.");
        }

        return new PartTimerDTO(entity);
    }

    @Transactional
    public void registerPartTimer(Integer storeId, PartTimerDTO partTimerDTO) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("지점을 찾을 수 없습니다."));

        PartTimerEntity entity = new PartTimerEntity(partTimerDTO, store);

        entity.setDeviceId(partTimerDTO.getDeviceId());
        entity.setDeviceName(partTimerDTO.getDeviceName());

        String uploadedPath = uploadFile(partTimerDTO.getFile());
        if (uploadedPath != null) {
            entity.setPartImg(uploadedPath);
        }

        partTimerRepository.save(entity);
    }


    @Transactional
    public void updatePartTimer(String role, Integer storeId, Integer partTimerId, PartTimerDTO partTimerDTO) {
        PartTimerEntity entity = partTimerRepository.findById(partTimerId)
                .orElseThrow(() -> new RuntimeException("해당 아르바이트를 찾을 수 없습니다."));

        if ((isStore(role) || isMaster(role)) &&
                !Objects.equals(entity.getStore().getStoreId(), storeId)) {
            throw new RuntimeException("본인 지점의 아르바이트만 수정할 수 있습니다.");
        }

        entity.setPartName(partTimerDTO.getPartName());
        entity.setPartGender(partTimerDTO.getPartGender());
        entity.setPartPhone(partTimerDTO.getPartPhone());
        entity.setPartAddress(partTimerDTO.getPartAddress());
        entity.setResignDate(partTimerDTO.getResignDate());
        entity.setSalaryType(partTimerDTO.getSalaryType());
        entity.setHourlyWage(partTimerDTO.getHourlyWage());
        entity.setAccountBank(partTimerDTO.getAccountBank());
        entity.setAccountNumber(partTimerDTO.getAccountNumber());
        entity.setPartStatus(partTimerDTO.getPartStatus());
        entity.setPosition(partTimerDTO.getPosition());
        entity.setWorkType(partTimerDTO.getWorkType());
        entity.setDeviceId(partTimerDTO.getDeviceId());
        entity.setDeviceName(partTimerDTO.getDeviceName());

        if (partTimerDTO.getFile() != null && !partTimerDTO.getFile().isEmpty()) {
            String uploadedPath = uploadFile(partTimerDTO.getFile());
            entity.setPartImg(uploadedPath);
        }
    }

    @Transactional
    public void deletePartTimer(String role, Integer storeId, Integer partTimerId) {
        PartTimerEntity entity = partTimerRepository.findById(partTimerId)
                .orElseThrow(() -> new RuntimeException("해당 아르바이트를 찾을 수 없습니다."));

        if ((isStore(role) || isMaster(role)) &&
                !Objects.equals(entity.getStore().getStoreId(), storeId)) {
            throw new RuntimeException("본인 지점의 아르바이트만 삭제할 수 있습니다.");
        }

        partTimerRepository.delete(entity);
    }

    private String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            File uploadFolder = new File(uploadDir);
            if (!uploadFolder.exists()) {
                uploadFolder.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            String savedFilename = UUID.randomUUID().toString() + ext;
            file.transferTo(new File(uploadDir + savedFilename));

            return "/uploads/parttimer/" + savedFilename;
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }

    public List<PartTimerDTO> findAllByStore(Integer storeId, String role) {
        List<PartTimerEntity> entities;

        if ("ROLE_HQ".equals(role)) {
            // 본사는 전체 조회
            entities = partTimerRepository.findAll();
        } else {
            // 매장은 자기 storeId만 조회
            entities = partTimerRepository.findByStore_StoreId(storeId);
        }

        return entities.stream()
                .map(PartTimerEntity::toDTO)
                .collect(Collectors.toList());
    }

}
