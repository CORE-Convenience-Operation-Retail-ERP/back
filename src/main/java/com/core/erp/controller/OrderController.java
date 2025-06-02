package com.core.erp.controller;

import com.core.erp.dto.*;
import com.core.erp.dto.order.OrderProductResponseDTO;
import com.core.erp.dto.order.OrderRequestDTO;
import com.core.erp.dto.order.PurchaseOrderDTO;
import com.core.erp.dto.order.PurchaseOrderItemDTO;
import com.core.erp.service.OrderService;
import com.core.erp.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    private final StockService stockService;

    @GetMapping("/products")
    public ResponseEntity<Page<OrderProductResponseDTO>> getOrderProductList(
            @AuthenticationPrincipal CustomPrincipal userDetails,
            @RequestParam(required = false) Integer storeId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Long barcode,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer isPromo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "productName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        log.info("📌 [Controller] storeId: {}, productName: {}, barcode: {}, categoryId: {}, isPromo: {}, page: {}, size: {}, sortBy: {}, sortDir: {}",
                storeId, productName, barcode, categoryId, isPromo, page, size, sortBy, sortDir);

        Integer effectiveStoreId = "ROLE_MASTER".equals(userDetails.getRole())
                ? storeId
                : userDetails.getStoreId();

        if (effectiveStoreId == null) {
            throw new IllegalArgumentException("storeId는 필수입니다.");
        }

        Page<OrderProductResponseDTO> result = orderService.getOrderProductList(
                effectiveStoreId, productName, barcode, categoryId, isPromo, page, size, sortBy, sortDir
        );

        return ResponseEntity.ok(result);
    }




    @PostMapping
    public ResponseEntity<String> registerOrder(
            @AuthenticationPrincipal CustomPrincipal userDetails,
            @RequestBody OrderRequestDTO requestDTO) {

        Integer storeId = userDetails.getStoreId();
        orderService.registerOrder(storeId, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body("발주 등록 완료");
    }

    @GetMapping("/history")
    public ResponseEntity<Page<PurchaseOrderDTO>> getOrderHistory(
            @AuthenticationPrincipal CustomPrincipal userDetails,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) Integer orderStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Integer storeId = userDetails.getStoreId();
        Page<PurchaseOrderDTO> result = orderService.searchOrderHistory(
                storeId, orderId, orderStatus, startDate, endDate, page, size
        );
        return ResponseEntity.ok(result);
    }


    @GetMapping("/history/{orderId}")
    public ResponseEntity<List<PurchaseOrderItemDTO>> getOrderDetail(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomPrincipal userDetails
    ) {
        Integer storeId = userDetails.getStoreId();
        String role = userDetails.getRole();

        List<PurchaseOrderItemDTO> items = orderService.getOrderDetail(orderId, storeId, role);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<String> completeOrder(
            @PathVariable Long orderId,
            @RequestParam Integer partTimerId,
            @AuthenticationPrincipal CustomPrincipal userDetails
    ) {
        Integer storeId = userDetails.getStoreId();
        String role = userDetails.getRole();

        orderService.completeOrder(orderId, storeId, role, partTimerId);

        return ResponseEntity.ok("전체 입고 완료");
    }


    @PostMapping("/{orderId}/partial-complete")
    public ResponseEntity<String> partialComplete(
            @PathVariable Long orderId,
            @RequestParam Integer partTimerId,
            @RequestBody List<PartialItemDTO> itemList,
            @AuthenticationPrincipal CustomPrincipal userDetails
    ) {
        Integer storeId = userDetails.getStoreId();
        String role = userDetails.getRole();

        orderService.partialComplete(orderId, itemList, storeId, role, partTimerId);

        return ResponseEntity.ok("부분 입고 완료");
    }




    @GetMapping("/alert/pending-items")
    public ResponseEntity<List<PurchaseOrderItemDTO>> getPendingStockAlerts(
            @AuthenticationPrincipal CustomPrincipal user
    ) {
        if (!"ROLE_STORE".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PurchaseOrderItemDTO> result = stockService.getPendingStockItems(user.getStoreId());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/update/{orderId}")
    public ResponseEntity<?> updateOrder(
            @PathVariable Long orderId,
            @RequestBody OrderRequestDTO dto,
            @AuthenticationPrincipal CustomPrincipal user
    ) {
        try {
            orderService.updateOrder(orderId, user.getStoreId(), user.getRole(), dto);
            return ResponseEntity.ok("발주 수정 완료");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 오류");
        }
    }

    @PatchMapping("/cancel/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId,
                                            @AuthenticationPrincipal CustomPrincipal user) {
        orderService.cancelOrder(orderId, user.getStoreId(), user.getRole());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomPrincipal user
    ) {
        orderService.deleteOrder(orderId, user.getStoreId(), user.getRole());
        return ResponseEntity.ok().build();
    }


}