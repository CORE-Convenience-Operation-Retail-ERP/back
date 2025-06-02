package com.core.erp.dto.disposal;

import com.core.erp.domain.DisposalEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DisposalDTO {

    private int disposalId;
    private Integer stockId; // FK (id만 관리)
    private Integer productId;
    private String proName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime disposalDate;
    
    private int disposalQuantity;
    private String processedBy;
    private int totalLossAmount;
    private String disposalReason;

    // Entity → DTO 변환 생성자
    public DisposalDTO(DisposalEntity entity) {
        this.disposalId = entity.getDisposalId();
        this.stockId = entity.getStoreStock() != null ? entity.getStoreStock().getStockId() : null;
        this.productId = entity.getProduct() != null ? entity.getProduct().getProductId() : null;
        this.proName = entity.getProduct() != null ? entity.getProduct().getProName() : null;
        this.disposalDate = entity.getDisposalDate();
        this.disposalQuantity = entity.getDisposalQuantity();
        this.processedBy = entity.getProcessedBy();
        this.totalLossAmount = entity.getTotalLossAmount();
        this.disposalReason = entity.getDisposalReason();
    }
}
