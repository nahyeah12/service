package com.ppi.utility.importer.service;

import com.ppi.utility.importer.model.CaseMaster;
import com.ppi.utility.importer.repository.CaseMasterRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Service class responsible for generating Excel reports from database records.
 */
@Service
public class GenerateReportService {

    private final CaseMasterRepository caseMasterRepository;

    @Autowired
    public GenerateReportService(CaseMasterRepository caseMasterRepository) {
        this.caseMasterRepository = caseMasterRepository;
    }

    /**
     * Generates an Excel report (.xlsx) for CaseMaster records matching the given file name.
     * The report includes specific columns and is formatted for readability.
     *
     * @param fileName The file name to query records by.
     * @return A byte array containing the generated Excel file.
     * @throws IOException If there's an error writing the Excel workbook.
     * @throws IllegalArgumentException If no records are found for the given file name.
     */
    @Transactional(readOnly = true) // Read-only transaction for report generation
    public byte[] generateReportExcel(String fileName) throws IOException {
        List<CaseMaster> records = caseMasterRepository.findByFileName(fileName);

        if (records.isEmpty()) {
            throw new IllegalArgumentException("No records found for file name: '" + fileName + "'");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("CaseMaster Report");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "CASE_ID",
                "IS_CURRENT_UK_RESIDENT",
                "FIRST_NAME",
                "LAST_NAME",
                "DATE_OF_BIRTH",
                "THIRD_PARTY_REFERENCE_1"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Populate data rows
            int rowNum = 1;
            for (CaseMaster record : records) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.createCell(0).setCellValue(record.getCaseId());
                dataRow.createCell(1).setCellValue(record.getIsCurrentUkResident() != null ? String.valueOf(record.getIsCurrentUkResident()) : "");
                dataRow.createCell(2).setCellValue(record.getFirstName());
                dataRow.createCell(3).setCellValue(record.getLastName());
                // Handle Date of Birth formatting
                if (record.getDateOfBirth() != null) {
                    CreationHelper createHelper = workbook.getCreationHelper();
                    CellStyle dateCellStyle = workbook.createCellStyle();
                    dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));
                    Cell dobCell = dataRow.createCell(4);
                    dobCell.setCellValue(java.sql.Date.valueOf(record.getDateOfBirth())); // Convert LocalDate to java.sql.Date for POI
                    dobCell.setCellStyle(dateCellStyle);
                } else {
                    dataRow.createCell(4).setCellValue("");
                }
                dataRow.createCell(5).setCellValue(record.getThirdPartyReference1());
            }

            // Auto-size columns for better readability
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(bos);
            return bos.toByteArray();

        } catch (IOException e) {
            System.err.println("Error generating Excel report: " + e.getMessage());
            throw new IOException("Failed to generate Excel report: " + e.getMessage(), e);
        }
    }
}
