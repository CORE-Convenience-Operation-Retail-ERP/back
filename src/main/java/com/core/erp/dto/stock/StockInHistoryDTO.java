package com.core.erp.dto.stock;

import com.core.erp.domain.StockInHistoryEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StockInHistoryDTO {
    private int historyId;
    private Integer storeId;
    private Integer partTimerId;
    private String partTimerName;
    private Integer productId;
    private String productName;
    private Long barcode;
    private Long orderId;
    private int inQuantity;
    private int unitPrice;
    private LocalDateTime inDate;
    private LocalDateTime expireDate;
    private int historyStatus;

    public StockInHistoryDTO(StockInHistoryEntity e) {
        this.historyId = e.getHistoryId();
        this.storeId = e.getStore() != null ? e.getStore().getStoreId() : null;
        this.partTimerId = e.getPartTimer() != null ? e.getPartTimer().getPartTimerId() : null;
        this.partTimerName = e.getPartTimer() != null ? e.getPartTimer().getPartName() : null;
        this.productId = e.getProduct() != null ? e.getProduct().getProductId() : null;
        this.productName = e.getProduct() != null ? e.getProduct().getProName() : null;
        this.barcode = e.getProduct() != null ? e.getProduct().getProBarcode() : null;
        this.orderId = e.getOrder() != null ? e.getOrder().getOrderId() : null;
        this.inQuantity = e.getInQuantity();
        this.unitPrice = e.getProduct() != null ? e.getProduct().getProCost() : 0;
        this.inDate = e.getInDate();
        this.expireDate = e.getExpireDate();
        this.historyStatus = e.getHistoryStatus();
    }

}
