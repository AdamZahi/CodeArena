package com.codearena.module4_shop.service;

import com.codearena.module4_shop.dto.PurchaseResponse;
import com.codearena.module4_shop.dto.ShopItemDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class ExcelService {

    // ── EXPORT PRODUCTS ───────────────────────────
    public byte[] exportProducts(List<ShopItemDto> products) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Headers
            Row header = sheet.createRow(0);
            String[] headers = {"ID", "Name", "Category", "Price", "Stock", "Created At"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (ShopItemDto p : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getId().toString().substring(0, 8).toUpperCase());
                row.createCell(1).setCellValue(p.getName());
                row.createCell(2).setCellValue(p.getCategory().name());
                row.createCell(3).setCellValue(p.getPrice());
                row.createCell(4).setCellValue(p.getStock());
                row.createCell(5).setCellValue(p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ── EXPORT ORDERS ─────────────────────────────
    public byte[] exportOrders(List<PurchaseResponse> orders) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Orders");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Headers
            Row header = sheet.createRow(0);
            String[] headers = {"Order ID", "Participant", "Total Price", "Status", "Items Count", "Created At"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (PurchaseResponse o : orders) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(o.getId().toString().substring(0, 8).toUpperCase());
                row.createCell(1).setCellValue(o.getParticipantId());
                row.createCell(2).setCellValue(o.getTotalPrice());
                row.createCell(3).setCellValue(o.getStatus().name());
                row.createCell(4).setCellValue(o.getItems() != null ? o.getItems().size() : 0);
                row.createCell(5).setCellValue(o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}