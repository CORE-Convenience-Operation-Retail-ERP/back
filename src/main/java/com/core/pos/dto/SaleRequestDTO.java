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

}

