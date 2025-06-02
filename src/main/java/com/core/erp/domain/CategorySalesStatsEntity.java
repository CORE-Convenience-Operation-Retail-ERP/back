package com.core.erp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "category_sales_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CategorySalesStatsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_sales_stats_id")
    private int categorySalesStatsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "total_sales", nullable = false)
    private int totalSales;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
