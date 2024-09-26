package com.example.excelprocessor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@SpringBootApplication
public class ExcelProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExcelProcessorApplication.class, args);

        try {
            // Read data from the first two Excel files
            Map<String, String> roleGroupData = readRoleGroupData("RolesGroups.xlsx");
            Map<String, List<String>> userRolesData = readUserRolesData("UserRoles.xlsx");

            // Process the data and generate the third Excel file
            writeOutputExcel("Output.xlsx", roleGroupData, userRolesData);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to read role and group data from the first Excel file
    private static Map<String, String> readRoleGroupData(String filePath) throws IOException {
        Map<String, String> roleGroupMap = new HashMap<>();
        FileInputStream file = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;  // Skip header row
            String role = row.getCell(0).getStringCellValue();
            String groupNeeded = row.getCell(1).getStringCellValue();
            roleGroupMap.put(role, groupNeeded);
        }
        workbook.close();
        return roleGroupMap;
    }

    // Method to read user-role data from the second Excel file
    private static Map<String, List<String>> readUserRolesData(String filePath) throws IOException {
        Map<String, List<String>> userRolesMap = new HashMap<>();
        FileInputStream file = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;  // Skip header row
            String user = row.getCell(0).getStringCellValue();
            String role = row.getCell(1).getStringCellValue();

            userRolesMap.computeIfAbsent(user, k -> new ArrayList<>()).add(role);
        }
        workbook.close();
        return userRolesMap;
    }

    // Method to write the output Excel file based on the processed data
    private static void writeOutputExcel(String filePath, Map<String, String> roleGroupData,
                                         Map<String, List<String>> userRolesData) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("User Roles Analysis");

        // Create Header Row
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("User");
        header.createCell(1).setCellValue("All Roles User has");
        header.createCell(2).setCellValue("Total Roles for User");
        header.createCell(3).setCellValue("Names of roles where group needed is YES");
        header.createCell(4).setCellValue("Names of roles where group needed is NO");
        header.createCell(5).setCellValue("Names of roles where group needed is Not found");

        int rowNum = 1;
        for (String user : userRolesData.keySet()) {
            Row row = sheet.createRow(rowNum++);

            List<String> roles = userRolesData.get(user);
            int totalRoles = roles.size();

            List<String> rolesGroupYes = new ArrayList<>();
            List<String> rolesGroupNo = new ArrayList<>();
            List<String> rolesGroupNotFound = new ArrayList<>();

            for (String role : roles) {
                String groupNeeded = roleGroupData.getOrDefault(role, "Not found");

                if ("Yes".equals(groupNeeded)) {
                    rolesGroupYes.add(role);
                } else if ("No".equals(groupNeeded)) {
                    rolesGroupNo.add(role);
                } else {
                    rolesGroupNotFound.add(role);
                }
            }

            row.createCell(0).setCellValue(user);
            row.createCell(1).setCellValue(String.join(", ", roles));
            row.createCell(2).setCellValue(totalRoles);
            row.createCell(3).setCellValue(String.join(", ", rolesGroupYes));
            row.createCell(4).setCellValue(String.join(", ", rolesGroupNo));
            row.createCell(5).setCellValue(String.join(", ", rolesGroupNotFound));
        }

        // Write the output to the Excel file
        FileOutputStream fileOut = new FileOutputStream(filePath);
        workbook.write(fileOut);
        fileOut.close();
        workbook.close();
    }
}
