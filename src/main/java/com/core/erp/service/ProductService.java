package com.core.erp.service;

import com.core.erp.domain.*;
import com.core.erp.dto.CustomPrincipal;
import com.core.erp.dto.product.ProductDTO;
import com.core.erp.dto.product.ProductDetailResponseDTO;
import com.core.erp.dto.product.ProductRegisterRequestDTO;
import com.core.erp.dto.product.ProductUpdateRequestDTO;
import com.core.erp.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.Optional;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private StoreStockRepository storeStockRepository;
    @Autowired
    private StockInHistoryRepository stockInHistoryRepository;
    @Autowired
    private ProductDetailsRepository productDetailsRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private HQStockRepository hqStockRepository;
    @Autowired
    private WarehouseStockRepository warehouseStockRepository;

    @Autowired
    private StockInventoryCheckItemRepository stockInventoryCheckItemRepository;

    @Autowired
    private S3Service s3Service;

    // 전체 제품 목록 (기존 메서드)
    public List<ProductDTO> getAllProducts() {
        List<ProductEntity> products = productRepository.findAll();
        List<ProductDTO> result = new ArrayList<>();
        for (ProductEntity p : products) {
            Integer stock = storeStockRepository.sumStockByProductId(Long.valueOf(p.getProductId()));
            if (stock == null) stock = 0;

            // isPromo 값에 따라 상태 결정
            String status;
            if (p.getIsPromo() != null) {
                switch (p.getIsPromo()) {
                    case 1: status = "단종"; break;
                    case 2: status = "1+1 이벤트"; break;
                    case 3: status = "2+1 이벤트"; break;
                    default: status = "판매중";
                }
            } else {
                status = "판매중";
            }

            ProductDTO dto = new ProductDTO(p);
            dto.setProStock(stock);
            dto.setStatus(status);

            // 카테고리 이름 세팅
            if (p.getCategory() != null) {
                dto.setCategoryName(p.getCategory().getCategoryName());
            }

            // 본사 재고 정보 설정 및 lastUpdate 가져오기
            Optional<HQStockEntity> hqStockOpt = hqStockRepository.findByProductProductId(p.getProductId());
            if (hqStockOpt.isPresent()) {
                HQStockEntity hqStock = hqStockOpt.get();
                dto.setHqStock(hqStock.getQuantity());
                dto.setRecentStockInDate(hqStock.getLastUpdate()); // 본사 재고 업데이트 시간을 최근 입고일로 사용
            } else {
                dto.setHqStock(0);
                // 최근 입고일 조회 (본사 재고 정보가 없는 경우 기존 로직 사용)
                StockInHistoryEntity recentStockIn = stockInHistoryRepository.findTop1ByProduct_ProductIdOrderByInDateDesc(p.getProductId());
                if (recentStockIn != null) {
                    dto.setRecentStockInDate(recentStockIn.getInDate());
                } else {
                    dto.setRecentStockInDate(null);
                }
            }

            result.add(dto);
        }
        return result;
    }

    // 페이징된 제품 목록
    public Page<ProductDTO> getPagedProducts(Pageable pageable) {
        Page<ProductEntity> productPage = productRepository.findAll(pageable);

        return productPage.map(product -> {
            ProductDTO dto = new ProductDTO(product);

            // 매장 재고 정보 설정
            Integer storeStock = storeStockRepository.sumStockByProductId(Long.valueOf(product.getProductId()));
            if (storeStock == null) storeStock = 0;
            dto.setProStock(storeStock);

            // 본사 재고 정보 설정 및 lastUpdate 가져오기
            Optional<HQStockEntity> hqStockOpt = hqStockRepository.findByProductProductId(product.getProductId());
            if (hqStockOpt.isPresent()) {
                HQStockEntity hqStock = hqStockOpt.get();
                dto.setHqStock(hqStock.getQuantity());
                dto.setRecentStockInDate(hqStock.getLastUpdate()); // 본사 재고 업데이트 시간을 최근 입고일로 사용
            } else {
                dto.setHqStock(0);
                // 최근 입고일 조회 (본사 재고 정보가 없는 경우 기존 로직 사용)
                StockInHistoryEntity recentStockIn = stockInHistoryRepository
                    .findTop1ByProduct_ProductIdOrderByInDateDesc(product.getProductId());
                if (recentStockIn != null) {
                    dto.setRecentStockInDate(recentStockIn.getInDate());
                }
            }

            // 카테고리 이름 설정
            if (product.getCategory() != null) {
                dto.setCategoryName(product.getCategory().getCategoryName());
            }

            return dto;
        });
    }

    // 상세페이지용 상세 정보
    public ProductDetailResponseDTO getProductDetail(int productId, CustomPrincipal user) {
        Optional<ProductEntity> optProduct = productRepository.findById((long) productId);
        if (optProduct.isEmpty()) return null;

        ProductEntity product = optProduct.get();
        Integer storeId = user.getStoreId();

        // 총 매장 재고
        Integer totalStock = storeStockRepository.sumStockByProductId((long) productId);
        if (totalStock == null) totalStock = 0;

        // 본사 재고
        int hqStock = hqStockRepository.findByProductProductId(productId)
                .map(HQStockEntity::getQuantity)
                .orElse(0);

        // 판매 상태
        String status = switch (product.getIsPromo()) {
            case 1 -> "단종";
            case 2 -> "1+1 이벤트";
            case 3 -> "2+1 이벤트";
            default -> "판매중";
        };

        // 카테고리
        String categoryName = product.getCategory() != null ? product.getCategory().getCategoryName() : null;

        // 점포 재고 정보
        List<ProductDetailResponseDTO.StoreStockInfo> storeStocks;
        if (storeId != null) {
            storeStocks = storeStockRepository
                    .findByStore_StoreIdAndProduct_ProductId(storeId, productId)
                    .map(ss -> List.of(new ProductDetailResponseDTO.StoreStockInfo(
                            ss.getStore().getStoreName(), ss.getQuantity())))
                    .orElse(List.of());
        } else {
            storeStocks = storeStockRepository
                    .findByProduct_ProductId(productId, null) // 전체 매장 조회
                    .stream()
                    .map(ss -> new ProductDetailResponseDTO.StoreStockInfo(
                            ss.getStore().getStoreName(), ss.getQuantity()))
                    .toList();
        }

        // 최근 입고 내역 (이제 전체 내역)
        List<ProductDetailResponseDTO.StockInInfo> recentStockIns = stockInHistoryRepository
                .findByProduct_ProductIdOrderByInDateDesc(productId)
                .stream()
                .map(si -> new ProductDetailResponseDTO.StockInInfo(
                        si.getStore().getStoreName(),
                        si.getInDate().toLocalDate().toString(),
                        si.getInQuantity()))
                .toList();

        // 상세 정보
        ProductDetailResponseDTO.ProductDetailInfo detailInfo = null;
        ProductDetailsEntity detail = productDetailsRepository.findByProduct_ProductId(productId);
        if (detail != null) {
            detailInfo = new ProductDetailResponseDTO.ProductDetailInfo(
                    detail.getManufacturer(), detail.getManuNum(),
                    detail.getShelfLife(), detail.getAllergens(), detail.getStorageMethod());
        }

        // 가격 계산
        int cost = product.getProCost();
        int sell = product.getProSellCost();
        double profitRate = sell > 0 ? ((double) (sell - cost) / sell) * 100 : 0;
        double costRate = sell > 0 ? ((double) cost / sell) * 100 : 0;

        // 실사 관련: 기본 초기값
        int storeExpectedQty = 0;
        int warehouseExpectedQty = 0;
        Integer storeRealQty = null;
        Integer warehouseRealQty = null;

        if (storeId != null) {
            storeExpectedQty = storeStockRepository
                    .findQuantityByProductAndStore(productId, storeId)
                    .orElse(0);

            warehouseExpectedQty = warehouseStockRepository
                    .findQuantityByProductAndStore(productId, storeId)
                    .orElse(0);

            PageRequest pageRequest = PageRequest.of(0, 1);
            List<StockInventoryCheckItemEntity> result = stockInventoryCheckItemRepository
                    .findLatestAppliedCheckItemList(productId, storeId, pageRequest);

            if (!result.isEmpty()) {
                StockInventoryCheckItemEntity check = result.get(0);
                storeRealQty = check.getStoreRealQuantity();
                warehouseRealQty = check.getWarehouseRealQuantity();
            }
        }

        int totalExpectedQty = storeExpectedQty + warehouseExpectedQty;
        int totalRealQty = (storeRealQty != null ? storeRealQty : 0) + (warehouseRealQty != null ? warehouseRealQty : 0);

        // 카테고리 경로
        List<String> categoryPath = new ArrayList<>();
        CategoryEntity cat = product.getCategory();
        while (cat != null) {
            categoryPath.add(0, cat.getCategoryName());
            cat = cat.getParentCategory();
        }

        // 이벤트 기간
        String eventStart = product.getEventStart() != null ? product.getEventStart().toString() : null;
        String eventEnd = product.getEventEnd() != null ? product.getEventEnd().toString() : null;

        // 정기 입고 정보
        Integer regularInDay = null;
        Integer regularInQuantity = null;
        Boolean regularInActive = false;
        Optional<HQStockEntity> hqStockOpt = hqStockRepository.findByProductProductId(productId);
        if (hqStockOpt.isPresent()) {
            HQStockEntity hqStockEntity = hqStockOpt.get();
            regularInDay = hqStockEntity.getRegularInDay();
            regularInQuantity = hqStockEntity.getRegularInQuantity();
            regularInActive = hqStockEntity.getRegularInActive() != null ? hqStockEntity.getRegularInActive() : false;
        }

        return new ProductDetailResponseDTO(
                product.getProductId(),
                product.getProName(),
                categoryName,
                String.valueOf(product.getProBarcode()),
                product.getProImage(),
                status,
                totalStock,
                product.getProStockLimit(),
                cost,
                sell,
                profitRate,
                costRate,
                storeStocks,
                recentStockIns,
                detailInfo,
                product.getCategory().getCategoryId(),
                categoryPath,
                eventStart,
                eventEnd,
                hqStock,
                regularInDay,
                regularInQuantity,
                regularInActive,
                storeRealQty,
                warehouseRealQty,
                totalRealQty,
                storeExpectedQty,
                warehouseExpectedQty,
                totalExpectedQty
        );
    }

    // 상품 정보 수정
    public void updateProduct(int id, ProductUpdateRequestDTO dto) {
        ProductEntity product = productRepository.findById(Long.valueOf(id)).orElseThrow();
        product.setProName(dto.getProName());
        product.setProStockLimit(dto.getProStockLimit());
        product.setProCost(dto.getProCost());
        product.setProSellCost(dto.getProSellCost());
        product.setIsPromo(dto.getIsPromo());
        product.setProImage(dto.getProImage());

        // 이벤트 기간(LocalDateTime) 파싱 (날짜만 들어오면 "T00:00:00" 붙여서 변환)
        if (dto.getEventStart() != null && !dto.getEventStart().isEmpty()) {
            String start = dto.getEventStart();
            if (start.length() == 10) { // "yyyy-MM-dd"
                start += "T00:00:00";
            }
            product.setEventStart(LocalDateTime.parse(start));
        }
        if (dto.getEventEnd() != null && !dto.getEventEnd().isEmpty()) {
            String end = dto.getEventEnd();
            if (end.length() == 10) { // "yyyy-MM-dd"
                end += "T00:00:00";
            }
            product.setEventEnd(LocalDateTime.parse(end));
        }

        if (dto.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(dto.getCategoryId()).orElseThrow();
            product.setCategory(category);
        }

        productRepository.save(product);

        // 부가정보
        ProductDetailsEntity detail = productDetailsRepository.findByProduct_ProductId(id);
        if (detail == null) {
            detail = new ProductDetailsEntity();
            detail.setProduct(product);
        }
        detail.setManufacturer(dto.getManufacturer());
        detail.setManuNum(dto.getManuNum());
        detail.setShelfLife(dto.getShelfLife());
        detail.setAllergens(dto.getAllergens());
        detail.setStorageMethod(dto.getStorageMethod());
        productDetailsRepository.save(detail);
    }

    public int registerProduct(ProductRegisterRequestDTO dto) {
        // 필수값 체크
        if (dto.getProName() == null || dto.getProBarcode() == null || dto.getCategoryId() == null
                || dto.getProCost() == null || dto.getProSellCost() == null) {
            throw new IllegalArgumentException("필수값 누락");
        }
        if (dto.getProCost() < 0 || dto.getProSellCost() < 0) {
            throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
        }

        ProductEntity product = new ProductEntity();
        product.setProName(dto.getProName());
        product.setProBarcode(Long.valueOf(dto.getProBarcode()));
        product.setProCost(dto.getProCost());
        product.setProSellCost(dto.getProSellCost());
        product.setProStockLimit(dto.getProStockLimit() != null ? dto.getProStockLimit() : 0);
        product.setIsPromo(dto.getIsPromo() != null ? dto.getIsPromo() : 0); // 상태값 반영
        product.setProImage(null); // 일단 null, 아래에서 처리
        product.setCategory(categoryRepository.findById(dto.getCategoryId()).orElseThrow());
        product.setProCreatedAt(java.time.LocalDateTime.now());

        // S3에 이미지 파일 업로드
        if (dto.getProImage() != null && !dto.getProImage().isEmpty()) {
            try {
                String imageUrl = s3Service.uploadImage(dto.getProImage(), "products");
                product.setProImage(imageUrl);
            } catch (Exception e) {
                throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
            }
        }

        productRepository.save(product);

        // 부가정보 저장
        if (dto.getManufacturer() != null || dto.getManuNum() != null || dto.getShelfLife() != null
                || dto.getAllergens() != null || dto.getStorageMethod() != null) {
            ProductDetailsEntity detail = new ProductDetailsEntity();
            detail.setProduct(product);
            detail.setManufacturer(dto.getManufacturer());
            detail.setManuNum(dto.getManuNum());
            detail.setShelfLife(dto.getShelfLife());
            detail.setAllergens(dto.getAllergens());
            detail.setStorageMethod(dto.getStorageMethod());
            productDetailsRepository.save(detail);
        }

        return product.getProductId();
    }

    /**
     * 엑셀 다운로드용 상품 데이터 조회
     */
    public List<Map<String, Object>> getProductsForExcel(String categoryName, String productName, Integer isPromo) {
        List<ProductEntity> products = productRepository.findAll(); // 실제로는 조건에 따른 필터링 로직 추가
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (ProductEntity product : products) {
            // 필터 조건 적용
            if (categoryName != null && !categoryName.isEmpty() && 
                !product.getCategory().getCategoryName().contains(categoryName)) {
                continue;
            }
            
            if (productName != null && !productName.isEmpty() && 
                !product.getProName().contains(productName)) {
                continue;
            }
            
            if (isPromo != null && !product.getIsPromo().equals(isPromo)) {
                continue;
            }
            
            // 총 재고 계산 (StoreStock + WarehouseStock)
            Integer storeStock = storeStockRepository.sumStockByProductId((long) product.getProductId());
            if (storeStock == null) storeStock = 0;
            
            // WarehouseStock도 계산해야 하는데, 해당 메서드가 없으므로 일단 0으로 설정
            int totalStock = storeStock; // + warehouseStock (나중에 추가)
            
            Map<String, Object> productData = new HashMap<>();
            // ExcelService에서 기대하는 필드명으로 매핑
            productData.put("productId", product.getProductId());
            productData.put("productName", product.getProName()); // proName -> productName
            productData.put("barcode", product.getProBarcode()); // proBarcode -> barcode
            productData.put("categoryName", product.getCategory().getCategoryName());
            productData.put("sellPrice", product.getProSellCost()); // proSellCost -> sellPrice
            productData.put("costPrice", product.getProCost()); // proCost -> costPrice
            productData.put("isPromo", product.getIsPromo().toString()); // String으로 변환
            productData.put("regDate", product.getProCreatedAt()); // proCreatedAt -> regDate
            
            result.add(productData);
        }
        
        return result;
    }

}