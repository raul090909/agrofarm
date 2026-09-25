package ru.agrofarm.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.agrofarm.entity.Operation;
import ru.agrofarm.repository.OperationRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
public class ExportController {

    private static final String[] HEADERS = {"№", "Дата", "Фермер", "Участок", "Тип", "Статья",
            "Сумма, ₽", "Количество", "Ед.", "Описание"};

    private final OperationRepository operations;

    public ExportController(OperationRepository operations) {
        this.operations = operations;
    }

    @GetMapping("/export/csv")
    @Transactional(readOnly = true)
    public void csv(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                    HttpServletResponse response) throws IOException {
        List<Operation> list = load(from, to);
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"operations_" + from + "_" + to + ".csv\"");
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(String.join(";", HEADERS)).append("\r\n");
        for (Operation o : list) {
            sb.append(o.getId()).append(';')
              .append(o.getOperationDate()).append(';')
              .append(esc(o.getUnit().getUser().getFullName())).append(';')
              .append(esc(o.getUnit().getName())).append(';')
              .append(o.getTypeLabel()).append(';')
              .append(esc(o.getCategory().getName())).append(';')
              .append(o.getAmount().toPlainString().replace('.', ',')).append(';')
              .append(o.getQuantity() != null ? o.getQuantity().stripTrailingZeros().toPlainString().replace('.', ',') : "").append(';')
              .append(o.getQuantityUnit() != null ? o.getQuantityUnit() : "").append(';')
              .append(esc(o.getDescription())).append("\r\n");
        }
        try (OutputStream out = response.getOutputStream()) {
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/export/excel")
    @Transactional(readOnly = true)
    public void excel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                      HttpServletResponse response) throws IOException {
        List<Operation> list = load(from, to);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"operations_" + from + "_" + to + ".xlsx\"");

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Операции");
            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            head.setFont(bold);
            head.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            Row h = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = h.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(head);
            }
            int rowNum = 1;
            double income = 0, expense = 0;
            for (Operation o : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(o.getId());
                row.createCell(1).setCellValue(o.getOperationDate().toString());
                row.createCell(2).setCellValue(o.getUnit().getUser().getFullName());
                row.createCell(3).setCellValue(o.getUnit().getName());
                row.createCell(4).setCellValue(o.getTypeLabel());
                row.createCell(5).setCellValue(o.getCategory().getName());
                Cell sum = row.createCell(6);
                sum.setCellValue(o.getAmount().doubleValue());
                sum.setCellStyle(money);
                if (o.getQuantity() != null) row.createCell(7).setCellValue(o.getQuantity().doubleValue());
                row.createCell(8).setCellValue(o.getQuantityUnit() != null ? o.getQuantityUnit() : "");
                row.createCell(9).setCellValue(o.getDescription() != null ? o.getDescription() : "");
                if ("income".equals(o.getType())) income += o.getAmount().doubleValue();
                else expense += o.getAmount().doubleValue();
            }
            rowNum++;
            totalRow(sheet, rowNum++, "Итого доходы", income, head, money);
            totalRow(sheet, rowNum++, "Итого расходы", expense, head, money);
            totalRow(sheet, rowNum, "Финансовый результат", income - expense, head, money);
            for (int i = 0; i < HEADERS.length; i++) sheet.autoSizeColumn(i);
            wb.write(response.getOutputStream());
        }
    }

    private List<Operation> load(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new IllegalArgumentException("Дата начала позже даты окончания");
        return operations.findBetween(from, to);
    }

    private static void totalRow(Sheet sheet, int idx, String label, double value, CellStyle head, CellStyle money) {
        Row r = sheet.createRow(idx);
        Cell l = r.createCell(5);
        l.setCellValue(label);
        l.setCellStyle(head);
        Cell v = r.createCell(6);
        v.setCellValue(value);
        v.setCellStyle(money);
    }

    private static String esc(String v) {
        if (v == null) return "";
        return v.replace(';', ',').replace('\n', ' ').replace('\r', ' ');
    }
}
