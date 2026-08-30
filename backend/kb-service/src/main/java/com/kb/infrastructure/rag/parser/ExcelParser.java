package com.kb.infrastructure.rag.parser;

import com.kb.domain.rag.ParsedDocument;
import com.kb.infrastructure.common.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

/**
 * Excel 文档解析器（.xlsx / .xls）
 * <p>
 * 将表格数据转换为结构化 Markdown 表格文本，
 * 保留行列结构以便 LLM 理解和检索
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
public class ExcelParser implements DocumentParserSpi {

    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName) throws ParseException {
        try {
            Workbook workbook;
            if (fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                workbook = new HSSFWorkbook(inputStream);
            }

            StringBuilder content = new StringBuilder();
            content.append("# ").append(fileName).append("\n\n");

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sheetCount", workbook.getNumberOfSheets());

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                content.append("## Sheet: ").append(sheetName).append("\n\n");

                int rowCount = 0;
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        cells.add(getCellValue(cell));
                    }
                    content.append("| ").append(String.join(" | ", cells)).append(" |\n");

                    // 第一行后加分隔符（Markdown 表格语法）
                    if (rowCount == 0 && !cells.isEmpty()) {
                        content.append("| ")
                                .append(String.join(" | ",
                                        Collections.nCopies(cells.size(), "---")))
                                .append(" |\n");
                    }
                    rowCount++;
                }
                metadata.put("sheet_" + sheetName + "_rows", rowCount);
                content.append("\n");
            }

            workbook.close();
            log.info("Excel parsed: {}, sheets={}", fileName, workbook.getNumberOfSheets());

            return ParsedDocument.builder()
                    .title(fileName)
                    .content(content.toString())
                    .fileType("xlsx")
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            throw new ParseException("Failed to parse Excel file: " + fileName, e);
        }
    }

    @Override
    public Set<String> supportedExtensions() {
        return Set.of("xlsx", "xls");
    }

    @Override
    public int priority() {
        return 5; // 专用解析器，高优先级
    }

    @Override
    public String getName() {
        return "ExcelParser (Apache POI)";
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().replace("|", "\\|").replace("\n", " ");
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                double val = cell.getNumericCellValue();
                yield val == Math.floor(val) && !Double.isInfinite(val)
                        ? String.valueOf((long) val) : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }
}
