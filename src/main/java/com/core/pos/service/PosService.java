package com.core.pos.service;

import com.core.erp.domain.*;
import com.core.erp.dto.sales.SalesDetailDTO;
import com.core.erp.repository.*;
import com.core.erp.service.StockFlowService;
import com.core.pos.dto.*;
import com.core.erp.repository.SalesDetailRepository;
import com.core.erp.repository.SalesTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PosService {

    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final SalesDetailRepository salesDetailRepository;
    private final DisposalRepository disposalRepository;
    private final StoreStockRepository storeStockRepository;
    private final StockFlowService stockFlowService;


    // 거래 저장
    @Transactional
    public void saveTransactionWithDetails(SaleRequestDTO dto, String loginId) {

        // 1. 로그인 ID로 직원 조회
        EmployeeEntity employee = employeeRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("직원 정보를 찾을 수 없습니다."));
        StoreEntity store = employee.getStore();

        // 2. 결제 트랜잭션 생성
        SalesTransactionEntity tx = new SalesTransactionEntity();
        tx.setEmployee(employee);
        tx.setStore(store);
        tx.setAgeGroup(Integer.valueOf(dto.getAgeGroup()));
        tx.setGender(Integer.valueOf(dto.getGender()));

        // 아르바이트 ID 주입 (교대 정산용)
        if (dto.getPartTimerId() != null) {
            tx.setPartTimer(new PartTimerEntity(dto.getPartTimerId()));
        }

        // null-safe 합계 계산
        tx.setTotalPrice(dto.getItemList().stream()
                .mapToInt(item -> Optional.ofNullable(item.getUnitPrice()).orElse(0))
                .sum());

        tx.setDiscountTotal(dto.getItemList().stream()
                .mapToInt(item -> Optional.ofNullable(item.getDiscountPrice()).orElse(0))
                .sum());

        tx.setFinalAmount(dto.getItemList().stream()
                .mapToInt(item -> Optional.ofNullable(item.getFinalAmount()).orElse(0))
                .sum());
        tx.setPaymentMethod(dto.getPaymentMethod());
        tx.setTransactionStatus(0); // 정상 거래 상태
        tx.setPaidAt(LocalDateTime.now());
        tx.setCreatedAt(LocalDateTime.now());

        tx.setReceiptType(dto.getReceiptType());
        tx.setReceiptIdentity(dto.getReceiptIdentity());

        // 3. 트랜잭션 저장
        salesTransactionRepository.save(tx);

        // 4. 장바구니 항목별 상세 저장
        for (SaleItemDTO item : dto.getItemList()) {
            // 상품 조회
            ProductEntity product = productRepository.findById(item.getProductId().longValue())
                    .orElseThrow(() -> new RuntimeException("상품 정보를 찾을 수 없습니다."));

            // 상세 항목 생성
            SalesDetailEntity detail = new SalesDetailEntity();
            detail.setTransaction(tx);
            detail.setProduct(product);
            detail.setSalesQuantity(Optional.ofNullable(item.getSalesQuantity()).orElse(0));
            detail.setUnitPrice(Optional.ofNullable(item.getUnitPrice()).orElse(0));
            detail.setDiscountPrice(Optional.ofNullable(item.getDiscountPrice()).orElse(0));
            detail.setFinalAmount(Optional.ofNullable(item.getFinalAmount()).orElse(0));

            int costPrice = product.getProCost(); // 원가
            detail.setCostPrice(costPrice);

            // 실이익 = 결제금액 - 원가
            int finalAmount = Optional.ofNullable(item.getFinalAmount()).orElse(0);
            int realIncome = finalAmount - costPrice;
            detail.setRealIncome(realIncome);

            detail.setIsPromo(Optional.ofNullable(item.getIsPromo()).orElse(0));

            // 상세 저장
            salesDetailRepository.save(detail);

            // 재고 흐름 로그 기록 (판매)
            StoreStockEntity stock = storeStockRepository
                    .findByStore_StoreIdAndProduct_ProductId(store.getStoreId(), product.getProductId())
                    .orElseThrow(() -> new RuntimeException("재고 정보를 찾을 수 없습니다."));

            int beforeQty = stock.getQuantity();
            int afterQty = beforeQty - item.getSalesQuantity();
            stock.setQuantity(afterQty);
            storeStockRepository.save(stock);

            stockFlowService.logStockFlow(
                    store,
                    product,
                    4,
                    -item.getSalesQuantity(),
                    beforeQty,
                    afterQty,
                    "매장",
                    loginId,
                    "POS 판매"
            );
        }
    }

    // 거래 내역 조회
    public List<SalesHistoryDTO> getTransactionHistoryByStore(Integer storeId) {
        List<SalesTransactionEntity> transactions =
                salesTransactionRepository.findByStore_StoreIdOrderByPaidAtDesc(storeId);

        return transactions.stream().map(transaction -> {
            List<SalesDetailEntity> details =
                    salesDetailRepository.findByTransaction_TransactionId(transaction.getTransactionId());

            List<SalesDetailDTO> items = details.stream()
                    .map(SalesDetailDTO::new)
                    .collect(Collectors.toList());

            return new SalesHistoryDTO(transaction, items);
        }).collect(Collectors.toList());
    }

    // 환불 처리 로직 추가
    @Transactional
    public void processRefund(int transactionId, String refundReason) {
        // 1. 트랜잭션 조회
        SalesTransactionEntity transaction = salesTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래입니다: ID " + transactionId));

        // 이미 환불된 거래는 중복 환불 방지
        if (transaction.getTransactionStatus() == 1) {
            throw new IllegalStateException("이미 환불된 거래입니다.");
        }

        // 2. 거래 상태를 환불로 업데이트 (transaction_status = 1)
        transaction.setTransactionStatus(1); // 환불 상태
        transaction.setRefundReason(refundReason);  // 환불 사유 추가
        transaction.setRefundAmount(transaction.getFinalAmount());  // 환불 금액은 final_amount로 설정
        transaction.setRefundedAt(LocalDateTime.now());  // 환불 시간 설정

        // 3. 해당 거래의 상품 상세 환불 처리 (sales_detail)
        List<SalesDetailEntity> detailList = transaction.getSalesDetails();
        for (SalesDetailEntity detail : detailList) {
            // 각 상품의 환불 금액을 처리
            detail.setFinalAmount(0);  // 환불된 상품의 결제 금액 0으로 처리
            detail.setRefundAmount(detail.getRealIncome());  // 실제 수익(원가를 제외한 금액)을 환불 금액으로 처리


            // 환불 흐름 로그 기록
            StoreStockEntity stock = storeStockRepository
                    .findByStore_StoreIdAndProduct_ProductId(
                            transaction.getStore().getStoreId(), detail.getProduct().getProductId())
                    .orElseThrow(() -> new RuntimeException("재고 없음"));

            int beforeQty = stock.getQuantity();
            int afterQty = beforeQty + detail.getSalesQuantity();
            stock.setQuantity(afterQty);
            storeStockRepository.save(stock);

            stockFlowService.logStockFlow(
                    stock.getStore(),
                    detail.getProduct(),
                    5,
                    detail.getSalesQuantity(),
                    beforeQty,
                    afterQty,
                    "매장",
                    transaction.getEmployee().getEmpName(),
                    refundReason
            );
        }

        // 4. 저장
        salesTransactionRepository.save(transaction);
        salesDetailRepository.saveAll(detailList); // saveAll로 일괄 저장
    }

    // 폐기 등록
    @Transactional
    public void saveDisposal(DisposalRequestDTO dto, String loginId) {
        // 1. 재고 ID로 storeStock 조회
        StoreStockEntity stock = storeStockRepository.findById(dto.getStockId())
                .orElseThrow(() -> new IllegalArgumentException("해당 재고 ID를 찾을 수 없습니다: " + dto.getStockId()));

        // 2. 폐기 엔티티 생성 및 값 설정
        DisposalEntity entity = new DisposalEntity();
        entity.setStoreStock(stock);
        entity.setProduct(stock.getProduct()); // 연관 상품 정보 설정
        entity.setProName(stock.getProduct().getProName());
        entity.setDisposalDate(LocalDateTime.now()); // 현재 시간으로 설정
        entity.setDisposalQuantity(dto.getDisposalQuantity());
        entity.setProcessedBy(loginId); // 로그인한 사용자 정보로 등록자 이름 설정
        entity.setDisposalReason(dto.getDisposalReason());

        // 총 손실 금액 = 단가(원가) × 수량
        int costPrice = stock.getProduct().getProCost();
        entity.setTotalLossAmount(costPrice * dto.getDisposalQuantity());

        // 3. 저장
        disposalRepository.save(entity);

        // 4. 재고 흐름 로그 기록
        int beforeQty = stock.getQuantity();
        int afterQty = beforeQty - dto.getDisposalQuantity();
        stock.setQuantity(afterQty);
        storeStockRepository.save(stock);

        stockFlowService.logStockFlow(
                stock.getStore(),
                stock.getProduct(),
                3, // 폐기
                -dto.getDisposalQuantity(), // 폐기는 감소
                stock.getQuantity(), // 반영 전 수량
                stock.getQuantity() - dto.getDisposalQuantity(), // 반영 후 수량 (DB 업데이트 전이라 직접 계산)
                "매장",
                loginId,
                dto.getDisposalReason()
        );
    }

    // 영수증
    public SalesHistoryDTO getReceiptByTransactionId(Integer txId) {
        SalesTransactionEntity tx = salesTransactionRepository.findById(txId)
                .orElseThrow(() -> new RuntimeException("거래를 찾을 수 없습니다."));

        List<SalesDetailEntity> details = salesDetailRepository.findByTransaction_TransactionId(txId);

        List<SalesDetailDTO> items = details.stream()
                .map(SalesDetailDTO::new)
                .collect(Collectors.toList());

        return new SalesHistoryDTO(tx, items);
    }

}