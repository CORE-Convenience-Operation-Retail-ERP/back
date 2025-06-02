package com.core.erp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelService {

    /**
     * 상품 목록 엑셀 생성 (바이트 배열 반환)
     */
    public byte[] createProductExcel(List<Map<String, Object>> products) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("상품목록");

            // 헤더 스타일
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {"상품ID", "상품명", "바코드", "카테고리", "판매가", "원가", "프로모션", "등록일"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 데이터 행 생성
            int rowNum = 1;
            for (Map<String, Object> product : products) {
                Row row = sheet.createRow(rowNum++);
                
                createCell(row, 0, product.get("productId"), dataStyle);
                createCell(row, 1, product.get("productName"), dataStyle);
                createCell(row, 2, product.get("barcode"), dataStyle);
                createCell(row, 3, product.get("categoryName"), dataStyle);
                createCell(row, 4, product.get("sellPrice"), dataStyle);
                createCell(row, 5, product.get("costPrice"), dataStyle);
                createCell(row, 6, getPromoText((String) product.get("isPromo")), dataStyle);
                createCell(row, 7, product.get("regDate"), dataStyle);
            }

            // 열 너비 자동 조정
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 엑셀 파일을 바이트 배열로 변환
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            return outputStream.toByteArray();
        }
    }

    /**
     * 재고 현황 엑셀 생성 (바이트 배열 반환)
     */
    public byte[] createStockExcel(List<Map<String, Object>> stocks) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("재고현황");

            // 헤더 스타일
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {"상품ID", "상품명", "바코드", "매장명", "진열재고", "창고재고", "매장재고", "최근입고일", "프로모션상태"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 데이터 행 생성
            int rowNum = 1;
            for (Map<String, Object> stock : stocks) {
                Row row = sheet.createRow(rowNum++);
                
                createCell(row, 0, stock.get("productId"), dataStyle);
                createCell(row, 1, stock.get("productName"), dataStyle);
                createCell(row, 2, stock.get("barcode"), dataStyle);
                createCell(row, 3, stock.get("storeName"), dataStyle);
                createCell(row, 4, stock.get("storeQuantity"), dataStyle);
                createCell(row, 5, stock.get("warehouseQuantity"), dataStyle);
                createCell(row, 6, stock.get("totalQuantity"), dataStyle);
                createCell(row, 7, stock.get("latestInDate"), dataStyle);
                createCell(row, 8, stock.get("promoStatus"), dataStyle);
            }

            // 열 너비 자동 조정
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 엑셀 파일을 바이트 배열로 변환
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            return outputStream.toByteArray();
        }
    }

    /**
     * 헤더 스타일 생성
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }

    /**
     * 데이터 스타일 생성
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }

    /**
     * 셀 생성 및 값 설정
     */
    private void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
        
        cell.setCellStyle(style);
    }

    /**
     * 프로모션 상태 텍스트 변환
     */
    private String getPromoText(String isPromo) {
        if (isPromo == null) return "일반";
        
        return switch (isPromo) {
            case "0" -> "일반";
            case "1" -> "단종";
            case "2" -> "1+1";
            case "3" -> "2+1";
            default -> "일반";
        };
    }
} 