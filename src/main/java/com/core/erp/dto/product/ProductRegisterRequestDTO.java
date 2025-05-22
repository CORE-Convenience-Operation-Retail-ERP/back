// src/main/java/com/core/erp/dto/ProductRegisterRequestDTO.java
package com.core.erp.dto.product;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ProductRegisterRequestDTO {
    private String proName;
    private String proBarcode;
    private Integer categoryId;
    private Integer proCost;
    private Integer proSellCost;
    private Integer proStockLimit;
    private MultipartFile proImage;
    private String manufacturer;
    private String manuNum;
    private String shelfLife;
    private String allergens;
    private String storageMethod;
    private Integer isPromo; // 0:판매중, 1:단종, 2:1+1, 3:2+1
}