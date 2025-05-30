package com.core.pos.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SaleRequestDTO {

    private String paymentMethod;
    private List<SaleItemDTO> itemList;
    private Integer partTimerId;
    private String ageGroup;
    private String gender;

    // 현금영수증 관련 필드 추가
    private String receiptType;      // "소득공제", "지출증빙"
    private String receiptIdentity;

}

