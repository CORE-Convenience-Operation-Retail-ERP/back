package com.core.erp.controller;

import com.core.erp.dto.CustomPrincipal;
import com.core.erp.dto.product.ProductDTO;
import com.core.erp.dto.product.ProductDetailResponseDTO;
import com.core.erp.dto.product.ProductUpdateRequestDTO;
import com.core.erp.dto.product.ProductRegisterRequestDTO;
import com.core.erp.service.ProductService;
import com.core.erp.service.S3Service;
import com.core.erp.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Autowired
    private ProductService productService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private ExcelService excelService;

    @GetMapping("/all")
    public List<ProductDTO> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/paged")
    public Page<ProductDTO> getPagedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.getPagedProducts(pageable);
    }

    @GetMapping("/detail/{id}")
    public ProductDetailResponseDTO getProductDetail(
            @PathVariable int id,
            @AuthenticationPrincipal CustomPrincipal user
    ) {
        return productService.getProductDetail(id, user);
    }

    @PutMapping("/edit/{id}")
    public void updateProduct(@PathVariable int id, @RequestBody ProductUpdateRequestDTO dto) {
        productService.updateProduct(id, dto);
    }

    // S3에 이미지 업로드
    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("파일이 비어있습니다.");
            }
            
            String imageUrl = s3Service.uploadImage(file, "products");
            
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("이미지 업로드에 실패했습니다: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> registerProduct(@ModelAttribute ProductRegisterRequestDTO dto) {
        try {
            int productId = productService.registerProduct(dto);
            Map<String, Object> result = new HashMap<>();
            result.put("productId", productId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("등록 실패");
        }
    }
    
    // 상품 목록 엑셀 다운로드
    @GetMapping("/download/excel")
    public ResponseEntity<byte[]> downloadProductsExcel(
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer isPromo) {
        try {
            List<Map<String, Object>> products = productService.getProductsForExcel(categoryName, productName, isPromo);
            byte[] excelData = excelService.createProductExcel(products);
            
            String fileName = "상품목록_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("엑셀 다운로드에 실패했습니다: " + e.getMessage()).getBytes());
        }
    }
}