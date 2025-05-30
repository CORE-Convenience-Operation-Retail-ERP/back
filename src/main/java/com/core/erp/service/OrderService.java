package com.core.erp.service;

import com.core.erp.domain.*;
import com.core.erp.dto.PartialItemDTO;
import com.core.erp.dto.order.*;
import com.core.erp.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.EntityManager;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final StockInHistoryRepository stockInHistoryRepository;
    private final PartTimerRepository partTimerRepository;
    private final HQStockService hqStockService;
    private final WarehouseStockRepository warehouseStockRepository;
    private final StockFlowService stockFlowService;
    private final EntityManager entityManager;

    // 상품 목록 + 재고 조회 (발주 등록 시)
    public Page<OrderProductResponseDTO> getOrderProductList(
            Integer storeId, String productName, Long barcode,
            Integer categoryId, Integer isPromo, int page, int size,
            String sortBy, String sortDir
    ) {
        // 정렬 컬럼 매핑
        String sortColumn = mapToColumnName(sortBy);
        String direction = "desc".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";

        // LIMIT & OFFSET
        int offset = page * size;

        // 💡 Native SQL 조립
        String sql = "SELECT p.product_id AS productId, " +
                "p.pro_name AS productName, " +
                "p.pro_cost AS unitPrice, " +
                "p.pro_barcode AS barcode, " +
                "c.category_name AS categoryName, " +
                "COALESCE(s.quantity, 0) AS stockQty, " +
                "p.pro_stock_limit AS proStockLimit, " +
                "p.is_promo AS isPromo " +
                "FROM product p " +
                "LEFT JOIN category c ON p.category_id = c.category_id " +
                "LEFT JOIN store_stock s ON p.product_id = s.product_id AND s.store_id = :storeId " +
                "WHERE p.is_promo IN (0, 2, 3) " +
                "AND (:productName IS NULL OR p.pro_name LIKE CONCAT('%', :productName, '%')) " +
                "AND (:barcode IS NULL OR p.pro_barcode = :barcode) " +
                "AND (:categoryId IS NULL OR p.category_id = :categoryId) " +
                "AND (:isPromo IS NULL OR p.is_promo = :isPromo) " +
                "ORDER BY " + sortColumn + " " + direction + " " +
                "LIMIT :limit OFFSET :offset";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("storeId", storeId)
                .setParameter("productName", productName)
                .setParameter("barcode", barcode)
                .setParameter("categoryId", categoryId)
                .setParameter("isPromo", isPromo)
                .setParameter("limit", size)
                .setParameter("offset", offset)
                .getResultList();

        // 수동 매핑 (필요 시 SqlResultSetMapping 써도 됨)
        List<OrderProductResponseDTO> dtoList = rows.stream()
                .map(row -> new OrderProductResponseDTO(
                        (int) ((Number) row[0]).longValue(),          // productId
                        (String) row[1],                        // productName
                        ((Number) row[3]).longValue(),          // barcode
                        (String) row[4],                        // categoryName
                        ((Number) row[2]).intValue(),           // unitPrice
                        ((Number) row[5]).intValue(),           // stockQty
                        ((Number) row[6]).intValue(),           // proStockLimit
                        ((Number) row[7]).intValue()            // isPromo
                ))
                .toList();

        int total = productRepository.countProductsWithStock(
                storeId, productName, barcode, categoryId, isPromo
        );

        return new PageImpl<>(dtoList, PageRequest.of(page, size), total);
    }

    private String mapToColumnName(String sortBy) {
        return switch (sortBy) {
            case "productName" -> "p.pro_name";
            case "productId"   -> "p.product_id";
            default            -> "p.pro_name"; // 기본값
        };
    }


    //  발주 등록
    @Transactional
    public void registerOrder(Integer storeId, OrderRequestDTO requestDTO) {
        LocalDateTime now = LocalDateTime.now();

        // 이번 회차에 발주가 존재하는지 확인
        if (isAlreadyOrdered(storeId, now)) {
            throw new IllegalStateException("이미 " + getPeriod(now) + " 발주가 등록되었습니다.");
        }

        List<OrderItemRequestDTO> items = requestDTO.getItems();

        validateOrderItems(items);

        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 매장이 존재하지 않습니다."));

        // 총합 계산 및 임계치 검증
        int totalQuantity = 0;
        int totalAmount = 0;

        for (OrderItemRequestDTO item : items) {
            ProductEntity product = productRepository.findById(Long.valueOf(item.getProductId()))
                    .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

            int quantity = item.getQuantity();
            int unitPrice = item.getUnitPrice();

            if (quantity > product.getProStockLimit()) {
                throw new IllegalArgumentException("[" + product.getProName() + "]의 발주 수량(" + quantity + ")이 임계치(" + product.getProStockLimit() + ")를 초과합니다.");
            }

            totalQuantity += quantity;
            totalAmount += quantity * unitPrice;
        }

        // 발주서 저장
        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setStore(store);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(0);
        order.setTotalQuantity(totalQuantity);
        order.setTotalAmount(totalAmount);
        purchaseOrderRepository.save(order);

        // 발주 상세 저장
        for (OrderItemRequestDTO item : items) {
            ProductEntity product = productRepository.findById(Long.valueOf(item.getProductId()))
                    .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

            PurchaseOrderItemEntity orderItem = new PurchaseOrderItemEntity();
            orderItem.setPurchaseOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getProName());
            orderItem.setOrderQuantity(item.getQuantity());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItem.setTotalPrice(item.getQuantity() * item.getUnitPrice());
            orderItem.setOrderState(0); // 대기
            orderItem.setIsAbnormal(0);
            orderItem.setIsFullyReceived(0);
            orderItem.setReceivedQuantity(0);

            purchaseOrderItemRepository.save(orderItem);
        }
    }

    // 항목 필수값 검사
    private void validateOrderItems(List<OrderItemRequestDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("발주 항목이 없습니다.");
        }

        for (OrderItemRequestDTO item : items) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getUnitPrice() == null) {
                throw new IllegalArgumentException("상품 ID, 수량, 단가는 모두 필수입니다.");
            }
        }
    }


    public List<PurchaseOrderItemDTO> getOrderDetail(Long orderId, Integer loginStoreId, String role) {
        PurchaseOrderEntity order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 발주서가 존재하지 않습니다."));

        if (!"ROLE_MASTER".equals(role) && order.getStore().getStoreId() != loginStoreId) {
            throw new SecurityException("해당 발주서에 접근할 수 없습니다.");
        }

        List<PurchaseOrderItemEntity> entities =
                purchaseOrderItemRepository.findByPurchaseOrder_OrderId(orderId);

        return entities.stream()
                .map(PurchaseOrderItemDTO::new)
                .toList();
    }

    @Transactional
    public void completeOrder(Long orderId, Integer loginStoreId, String role, Integer partTimerId) {
        // 0. 발주서 조회 및 검증
        PurchaseOrderEntity order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("발주서가 존재하지 않습니다."));

        // 1. 알바 엔티티 조회
        PartTimerEntity partTimer = partTimerRepository.findById(partTimerId)
                .orElseThrow(() -> new IllegalArgumentException("입고 처리 알바를 찾을 수 없습니다."));

        // 2. 접근 권한 검증 (본인 매장이 아니고 HQ도 아니면 거절)
        if (!"ROLE_MASTER".equals(role) && order.getStore().getStoreId() != loginStoreId) {
            throw new SecurityException("입고 완료 권한이 없습니다.");
        }

        // 3. 중복 처리 방지
        if (order.getOrderStatus() == 1) {
            throw new IllegalStateException("이미 입고 완료된 발주입니다.");
        }

        // 4. 발주 상태 변경
        order.setOrderStatus(1); // COMPLETED
        order.setOrderDate(LocalDateTime.now()); // 입고일자 최신화
        purchaseOrderRepository.save(order);

        // 5. 발주 상세 항목 조회
        List<PurchaseOrderItemEntity> items = purchaseOrderItemRepository.findByPurchaseOrder_OrderId(orderId);

        // 6. 입고 이력 기록 (stock_in_history 생성)

        for (PurchaseOrderItemEntity item : items) {
            item.setReceivedQuantity(item.getOrderQuantity());
            item.setIsFullyReceived(1);
            item.setOrderState(2);
            purchaseOrderItemRepository.save(item);

            StockInHistoryEntity history = new StockInHistoryEntity();
            history.setStore(order.getStore());
            history.setPartTimer(partTimer);
            history.setProduct(item.getProduct());
            history.setOrder(order);
            history.setInQuantity(item.getOrderQuantity());
            history.setInDate(LocalDateTime.now());
            history.setExpireDate(null);
            history.setHistoryStatus(2);

            stockInHistoryRepository.save(history);

            updateWarehouseStock(order.getStore(), item.getProduct(), item.getOrderQuantity(), partTimer.getPartName());
        }

    }

    @Transactional
    public void partialComplete(Long orderId, List<PartialItemDTO> items, Integer loginStoreId, String role, Integer partTimerId) {
        PurchaseOrderEntity order = getOrderWithPermissionCheck(orderId, loginStoreId, role);
        PartTimerEntity partTimer = getPartTimer(partTimerId);

        boolean allFullyReceived = true;

        for (PartialItemDTO dto : items) {
            PurchaseOrderItemEntity item = getOrderItem(Long.valueOf(dto.getItemId()));

            int remainingQty = validateAndCalculateRemaining(item, dto);
            accumulateReceivedQty(item, dto.getInQuantity(), remainingQty);
            purchaseOrderItemRepository.save(item);

            recordStockInHistory(order, item, partTimer, dto.getInQuantity());
            updateWarehouseStock(order.getStore(), item.getProduct(), dto.getInQuantity(), partTimer.getPartName());

            if (item.getIsFullyReceived() == 0) {
                allFullyReceived = false;
            }
        }

        updateOrderStatus(order, allFullyReceived);
    }

    private PurchaseOrderEntity getOrderWithPermissionCheck(Long orderId, Integer loginStoreId, String role) {
        PurchaseOrderEntity order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("발주서가 존재하지 않습니다."));

        if (!"ROLE_MASTER".equals(role) && order.getStore().getStoreId() != loginStoreId) {
            throw new SecurityException("입고 권한이 없습니다.");
        }

        return order;
    }

    private PartTimerEntity getPartTimer(Integer partTimerId) {
        return partTimerRepository.findById(partTimerId)
                .orElseThrow(() -> new IllegalArgumentException("입고 처리 알바를 찾을 수 없습니다."));
    }

    private PurchaseOrderItemEntity getOrderItem(Long itemId) {
        return purchaseOrderItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("발주 항목이 존재하지 않습니다."));
    }

    private int validateAndCalculateRemaining(PurchaseOrderItemEntity item, PartialItemDTO dto) {
        int orderedQty = item.getOrderQuantity();
        int receivedQty = Optional.ofNullable(item.getReceivedQuantity()).orElse(0);
        int remainingQty = orderedQty - receivedQty;

        if (dto.getInQuantity() > remainingQty) {
            throw new IllegalArgumentException("상품 ID " + item.getProduct().getProductId() + " 입고 수량이 초과되었습니다.");
        }

        return remainingQty;
    }

    private void accumulateReceivedQty(PurchaseOrderItemEntity item, int inQty, int remainingQty) {
        int newReceivedQty = Optional.ofNullable(item.getReceivedQuantity()).orElse(0) + inQty;
        item.setReceivedQuantity(newReceivedQty);

        if (newReceivedQty >= item.getOrderQuantity()) {
            item.setIsFullyReceived(1);
            item.setOrderState(2); // 전체 입고 완료
        } else {
            item.setIsFullyReceived(0);
            item.setOrderState(1); // 부분 입고
        }
    }

    private void recordStockInHistory(PurchaseOrderEntity order, PurchaseOrderItemEntity item, PartTimerEntity partTimer, int inQty) {
        StockInHistoryEntity history = new StockInHistoryEntity();
        history.setStore(order.getStore());
        history.setPartTimer(partTimer);
        history.setProduct(item.getProduct());
        history.setOrder(order);
        history.setInQuantity(inQty);
        history.setInDate(LocalDateTime.now());
        history.setExpireDate(null);
        history.setHistoryStatus(2);
        stockInHistoryRepository.save(history);
    }

    private void updateWarehouseStock(StoreEntity store, ProductEntity product, int inQty, String processedBy) {
        WarehouseStockEntity stock = warehouseStockRepository
                .findByStore_StoreIdAndProduct_ProductId(store.getStoreId(), product.getProductId())
                .orElseGet(() -> {
                    WarehouseStockEntity newStock = new WarehouseStockEntity();
                    newStock.setStore(store);
                    newStock.setProduct(product);
                    newStock.setWarehouseId(0); // 기본 창고 ID, 필요 시 분기
                    newStock.setQuantity(0);
                    newStock.setLastInDate(LocalDateTime.now());
                    newStock.setStockStatus(1);
                    return warehouseStockRepository.save(newStock);
                });

        int beforeQty = stock.getQuantity();
        stock.setQuantity(beforeQty + inQty);
        stock.setLastInDate(LocalDateTime.now());

        warehouseStockRepository.save(stock);

        //  StockFlow 로그 기록 (입고: flowType = 0)
        stockFlowService.logStockFlow(
                store,
                product,
                0,
                inQty,
                beforeQty,
                beforeQty + inQty,
                "창고",
                processedBy,
                "발주 입고"
        );

        // 데이터 일관성을 위해 본사 재고 재계산
        try {
            hqStockService.recalculateAllHQStocks();
        } catch (Exception e) {
            // 재계산이 실패해도 입고는 성공했으므로 로그만 남김
            System.err.println("입고 처리 후 본사 재고 재계산 실패: " + e.getMessage());
        }

    }



    private void updateOrderStatus(PurchaseOrderEntity order, boolean allFullyReceived) {
        order.setOrderStatus(allFullyReceived ? 1 : 2);
        order.setOrderDate(LocalDateTime.now());
        purchaseOrderRepository.save(order);
    }

    public boolean isAlreadyOrdered(Integer storeId, LocalDateTime now) {
        String period = getPeriod(now);
        LocalDate today = now.toLocalDate();

        LocalDateTime start;
        LocalDateTime end;

        // PM 오전 6시부터 오후 13시 59분까지
        if ("AM".equals(period)) {
            start = today.atTime(6, 0);
            end = today.atTime(13, 59, 59);
        } else {
            // PM 오후 3시부터 다음날 새벽 4시 59분까지
            start = today.atTime(15, 0);
            end = today.plusDays(1).atTime(4, 59, 59);
        }

        return purchaseOrderRepository.existsByStore_StoreIdAndOrderDateBetween(storeId, start, end);
    }

    // 오전/ 오후 판별
    private String getPeriod(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        return (!time.isBefore(LocalTime.of(6, 0)) && time.isBefore(LocalTime.of(16, 0))) ? "AM" : "PM";
    }

    //  수정 시간제한
    private boolean isSameOrderTimeSlot(LocalDateTime orderDate, LocalDateTime now) {
        return getPeriod(orderDate).equals(getPeriod(now));
    }

    @Transactional
    public void updateOrder(Long orderId, Integer loginStoreId, String role, OrderRequestDTO dto) {
        log.info("업데이트 요청 - orderId: {}, storeId: {}, role: {}", orderId, loginStoreId, role);
        log.info("요청 데이터 DTO 존재 여부: {}", dto != null);
        log.info("DTO 내부 items: {}", dto.getItems());

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("수정할 항목이 없습니다.");
        }

        PurchaseOrderEntity order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("발주서가 존재하지 않습니다."));

        // 본사 이외 매장에 대해 권한 및 시간대 체크
        if (!"ROLE_MASTER".equals(role)) {
            if (order.getStore().getStoreId() != (loginStoreId)) {
                throw new SecurityException("해당 발주에 대한 수정 권한이 없습니다.");
            }

            if (order.getOrderStatus() != 0) {
                throw new IllegalStateException("입고 중이거나 완료된 발주는 할 수 없습니다.");
            }

            if (!isSameOrderTimeSlot(order.getOrderDate(), LocalDateTime.now())) {
                throw new IllegalStateException("해당 시간대에는 발주를 수정할 수 없습니다.");
            }
        }

        List<OrderItemRequestDTO> items = dto.getItems();
        validateOrderItems(items); // 필수 필드 체크

        // 임계치 검사
        for (OrderItemRequestDTO itemDto : items) {
            ProductEntity product = productRepository.findById(Long.valueOf(itemDto.getProductId()))
                    .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

            if (itemDto.getQuantity() > product.getProStockLimit()) {
                throw new IllegalArgumentException("[" + product.getProName() + "]의 발주 수량(" + itemDto.getQuantity() + ")이 임계치(" + product.getProStockLimit() + ")를 초과합니다.");
            }
        }

        // 기존 항목 삭제
        purchaseOrderItemRepository.deleteByPurchaseOrder_OrderId(orderId);

        // 재계산 및 재삽입
        int totalQty = 0;
        int totalAmount = 0;

        for (OrderItemRequestDTO itemDto : items) {
            ProductEntity product = productRepository.findById(Long.valueOf(itemDto.getProductId()))
                    .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

            int qty = itemDto.getQuantity();
            int price = itemDto.getUnitPrice();

            PurchaseOrderItemEntity item = new PurchaseOrderItemEntity();
            item.setPurchaseOrder(order);
            item.setProduct(product);
            item.setProductName(product.getProName());
            item.setOrderQuantity(qty);
            item.setUnitPrice(price);
            item.setTotalPrice(qty * price);
            item.setOrderState(0);
            item.setIsAbnormal(0);
            item.setIsFullyReceived(0);
            item.setReceivedQuantity(0);

            purchaseOrderItemRepository.save(item);

            totalQty += qty;
            totalAmount += qty * price;
        }

        order.setTotalQuantity(totalQty);
        order.setTotalAmount(totalAmount);
        order.setOrderDate(LocalDateTime.now());
        purchaseOrderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, Integer loginStoreId, String role) {
        PurchaseOrderEntity order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("발주서가 존재하지 않습니다."));

        if (!"ROLE_MASTER".equals(role) && order.getStore().getStoreId() != (loginStoreId)) {
            throw new SecurityException("해당 발주서에 대한 취소 권한이 없습니다.");
        }

        order.setOrderStatus(9); // 9 = 취소
        purchaseOrderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long orderId, Integer loginStoreId, String role) {
        // HQ는 삭제 금지
        if ("ROLE_MASTER".equals(role)) {
            throw new SecurityException("본사는 발주 삭제 권한이 없습니다.");
        }

        PurchaseOrderEntity order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("발주서가 존재하지 않습니다."));

        // 자신의 매장인지 확인
        if (order.getStore().getStoreId() != (loginStoreId)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }

        // 상태 체크: 대기중(0)만 삭제 허용
        if (order.getOrderStatus() != 0) {
            throw new IllegalStateException("입고 중이거나 완료된 발주는 삭제할 수 없습니다.");
        }

        // 먼저 항목부터 삭제
        purchaseOrderItemRepository.deleteByPurchaseOrder_OrderId(orderId);
        // 그 다음 발주서 삭제
        purchaseOrderRepository.delete(order);
    }

    public Page<PurchaseOrderDTO> searchOrderHistory(
            Integer storeId, String orderId, Integer orderStatus, String startDate, String endDate, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<PurchaseOrderProjection> projectionPage =
                purchaseOrderRepository.searchOrderHistory(storeId, orderId, orderStatus, startDate, endDate, pageable);

        List<PurchaseOrderDTO> dtoList = projectionPage.stream()
                .map(p -> new PurchaseOrderDTO(
                        p.getOrderId(),
                        p.getTotalQuantity(),
                        p.getTotalAmount(),
                        p.getOrderDate(),
                        p.getOrderStatus()))
                .toList();

        return new PageImpl<>(dtoList, pageable, projectionPage.getTotalElements());
    }
}

