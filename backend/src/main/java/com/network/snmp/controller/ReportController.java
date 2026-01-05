package com.network.snmp.controller;

import com.lowagie.text.DocumentException;
import com.network.snmp.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{systemName}/export")
    public ResponseEntity<byte[]> exportDeviceReport(@PathVariable String systemName) {
        try {
            byte[] pdfContent = reportService.generateDevicePdf(systemName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Report_" + systemName + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfContent);
        } catch (DocumentException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}