package com.example.excelprocessor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@SpringBootApplication
public class ExcelProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExcelProcessorApplication.class, args);
    }
}

@RestController
class ExcelController {

    @GetMapping("/process-excel")
    public String processExcel() throws IOException {
        // Step 1: Load the two Excel sheets
        Map<String, List<String>> roleGroupMapping = loadRoleGroupData("roles_and_groups.xlsx");
        Map<String, List<String>> userRoleMapping = loadUserRoleData("users_and_roles.xlsx");

        // Step 2: Process the data to generate output
        List<Map<String, Object>> processedData = processUserRoleData(roleGroupMapping, userRoleMapping);

        // Step 3: Write the processed data to another Excel sheet
        writeOutputToExcel(processedData, "output.xlsx");

        return "Excel processed successfully. Check output.xlsx";
    }

    private Map<String, List<String>> loadRoleGroupData(String filePath) throws IOException {
        Map<String, List<String>> roleGroupMapping = new HashMap<>();
        FileInputStream file = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header row
            String role = row.getCell(0).getStringCellValue();
            String groupNeeded = row.getCell(1).getStringCellValue();
            String groupName = row.getCell(2).getStringCellValue();
            roleGroupMapping.put(role, Arrays.asList(groupNeeded, groupName));
        }

        workbook.close();
        return roleGroupMapping;
    }

    private Map<String, List<String>> loadUserRoleData(String filePath) throws IOException {
        Map<String, List<String>> userRoleMapping = new HashMap<>();
        FileInputStream file = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header row
            String user = row.getCell(0).getStringCellValue();
            String role = row.getCell(1).getStringCellValue();
            userRoleMapping.computeIfAbsent(user, k -> new ArrayList<>()).add(role);
        }

        workbook.close();
        return userRoleMapping;
    }

    private List<Map<String, Object>> processUserRoleData(Map<String, List<String>> roleGroupMapping,
                                                          Map<String, List<String>> userRoleMapping) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (String user : userRoleMapping.keySet()) {
            Map<String, Object> userData = new HashMap<>();
            List<String> roles = userRoleMapping.get(user);

            List<String> rolesYes = new ArrayList<>();
            List<String> groupsYes = new ArrayList<>();
            List<String> rolesNo = new ArrayList<>();
            List<String> rolesNotFound = new ArrayList<>();

            for (String role : roles) {
                List<String> groupData = roleGroupMapping.getOrDefault(role, Arrays.asList("Not found", ""));
                String groupNeeded = groupData.get(0);
                String groupName = groupData.get(1);

                if ("Yes".equalsIgnoreCase(groupNeeded)) {
                    rolesYes.add(role);
                    groupsYes.add(groupName);
                } else if ("No".equalsIgnoreCase(groupNeeded)) {
                    rolesNo.add(role);
                } else if ("Not found".equalsIgnoreCase(groupNeeded)) {
                    rolesNotFound.add(role);
                }
            }

            userData.put("User", user);
            userData.put("Total Roles", roles.size());
            userData.put("Roles (Yes)", String.join(", ", rolesYes));
            userData.put("Groups (Yes)", String.join(", ", groupsYes));
            userData.put("Roles (No)", String.join(", ", rolesNo));
            userData.put("Roles (Not Found)", String.join(", ", rolesNotFound));

            result.add(userData);
        }

        return result;
    }

    private void writeOutputToExcel(List<Map<String, Object>> processedData, String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Processed Data");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"User", "Total Roles", "Roles (Yes)", "Groups (Yes)", "Roles (No)", "Roles (Not Found)"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        int rowNum = 1;
        for (Map<String, Object> data : processedData) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue((String) data.get("User"));
            row.createCell(1).setCellValue((int) data.get("Total Roles"));
            row.createCell(2).setCellValue((String) data.get("Roles (Yes)"));
            row.createCell(3).setCellValue((String) data.get("Groups (Yes)"));
            row.createCell(4).setCellValue((String) data.get("Roles (No)"));
            row.createCell(5).setCellValue((String) data.get("Roles (Not Found)"));
        }

        FileOutputStream fileOut = new FileOutputStream(filePath);
        workbook.write(fileOut);
        fileOut.close();
        workbook.close();
    }
}
/* 
 <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.2</version>
    </dependency>
*/
