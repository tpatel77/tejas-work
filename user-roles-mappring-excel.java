package com.example.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@RestController
public class ExcelController {

    @GetMapping("/process-excel")
    public String processExcel() throws IOException {
        // Load data from first Excel sheet (Roles and Groups)
        String firstExcelFilePath = "path/to/roles_groups.xlsx";
        Map<String, RoleGroupInfo> roleGroupMap = readFirstExcelSheet(firstExcelFilePath);

        // Load data from second Excel sheet (Users and Roles)
        String secondExcelFilePath = "path/to/users_roles.xlsx";
        Map<String, List<String>> userRolesMap = readSecondExcelSheet(secondExcelFilePath);

        // Generate output based on the data
        List<UserGroupInfo> outputData = processUserRoles(roleGroupMap, userRolesMap);

        // Write the output to a new Excel sheet
        String outputFilePath = "path/to/output_excel.xlsx";
        writeOutputToExcel(outputData, outputFilePath);

        return "Excel file processed successfully! Check the output file: " + outputFilePath;
    }

    private Map<String, RoleGroupInfo> readFirstExcelSheet(String filePath) throws IOException {
        Map<String, RoleGroupInfo> roleGroupMap = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String role = row.getCell(0).getStringCellValue();
                String groupNeeded = row.getCell(1).getStringCellValue();
                String groupName = row.getCell(2).getStringCellValue();

                roleGroupMap.put(role, new RoleGroupInfo(role, groupNeeded, groupName));
            }
        }

        return roleGroupMap;
    }

    private Map<String, List<String>> readSecondExcelSheet(String filePath) throws IOException {
        Map<String, List<String>> userRolesMap = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String user = row.getCell(0).getStringCellValue();
                String role = row.getCell(1).getStringCellValue();

                userRolesMap.computeIfAbsent(user, k -> new ArrayList<>()).add(role);
            }
        }

        return userRolesMap;
    }

    private List<UserGroupInfo> processUserRoles(Map<String, RoleGroupInfo> roleGroupMap, Map<String, List<String>> userRolesMap) {
        List<UserGroupInfo> outputData = new ArrayList<>();

        for (String user : userRolesMap.keySet()) {
            List<String> roles = userRolesMap.get(user);

            UserGroupInfo userGroupInfo = new UserGroupInfo();
            userGroupInfo.setUser(user);
            userGroupInfo.setAllRoles(String.join(", ", roles));
            userGroupInfo.setTotalRoles(roles.size());

            List<String> rolesGroupYes = new ArrayList<>();
            List<String> groupsGroupYes = new ArrayList<>();
            List<String> rolesGroupNo = new ArrayList<>();
            List<String> rolesGroupNotFound = new ArrayList<>();

            for (String role : roles) {
                RoleGroupInfo roleGroupInfo = roleGroupMap.get(role);

                if (roleGroupInfo != null) {
                    switch (roleGroupInfo.getGroupNeeded()) {
                        case "Yes":
                            rolesGroupYes.add(role);
                            groupsGroupYes.add(roleGroupInfo.getGroupName());
                            break;
                        case "No":
                            rolesGroupNo.add(role);
                            break;
                        case "Not found":
                            rolesGroupNotFound.add(role);
                            break;
                    }
                }
            }

            userGroupInfo.setRolesGroupYes(String.join(", ", rolesGroupYes));
            userGroupInfo.setGroupsGroupYes(String.join(", ", groupsGroupYes));
            userGroupInfo.setRolesGroupNo(String.join(", ", rolesGroupNo));
            userGroupInfo.setRolesGroupNotFound(String.join(", ", rolesGroupNotFound));

            outputData.add(userGroupInfo);
        }

        return outputData;
    }

    private void writeOutputToExcel(List<UserGroupInfo> outputData, String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Output");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"User", "All Roles", "Total Roles", "Roles (Group Yes)", "Groups (Group Yes)", "Roles (Group No)", "Roles (Not Found)"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Write data
        int rowIndex = 1;
        for (UserGroupInfo userInfo : outputData) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(userInfo.getUser());
            row.createCell(1).setCellValue(userInfo.getAllRoles());
            row.createCell(2).setCellValue(userInfo.getTotalRoles());
            row.createCell(3).setCellValue(userInfo.getRolesGroupYes());
            row.createCell(4).setCellValue(userInfo.getGroupsGroupYes());
            row.createCell(5).setCellValue(userInfo.getRolesGroupNo());
            row.createCell(6).setCellValue(userInfo.getRolesGroupNotFound());
        }

        // Write the output to a file
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }

        workbook.close();
    }
}

class RoleGroupInfo {
    private String role;
    private String groupNeeded;
    private String groupName;

    public RoleGroupInfo(String role, String groupNeeded, String groupName) {
        this.role = role;
        this.groupNeeded = groupNeeded;
        this.groupName = groupName;
    }

    public String getGroupNeeded() {
        return groupNeeded;
    }

    public String getGroupName() {
        return groupName;
    }
}

class UserGroupInfo {
    private String user;
    private String allRoles;
    private int totalRoles;
    private String rolesGroupYes;
    private String groupsGroupYes;
    private String rolesGroupNo;
    private String rolesGroupNotFound;

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getAllRoles() { return allRoles; }
    public void setAllRoles(String allRoles) { this.allRoles = allRoles; }

    public int getTotalRoles() { return totalRoles; }
    public void setTotalRoles(int totalRoles) { this.totalRoles = totalRoles; }

    public String getRolesGroupYes() { return rolesGroupYes; }
    public void setRolesGroupYes(String rolesGroupYes) { this.rolesGroupYes = rolesGroupYes; }

    public String getGroupsGroupYes() { return groupsGroupYes; }
    public void setGroupsGroupYes(String groupsGroupYes) { this.groupsGroupYes = groupsGroupYes; }

    public String getRolesGroupNo() { return rolesGroupNo; }
    public void setRolesGroupNo(String rolesGroupNo) { this.rolesGroupNo = rolesGroupNo; }

    public String getRolesGroupNotFound() { return rolesGroupNotFound; }
    public void setRolesGroupNotFound(String rolesGroupNotFound) { this.rolesGroupNotFound = rolesGroupNotFound; }
}
