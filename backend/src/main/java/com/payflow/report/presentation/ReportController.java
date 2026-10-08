package com.payflow.report.presentation;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import com.payflow.report.application.StatementReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final StatementReportService reports;

    public ReportController(StatementReportService reports) {
        this.reports = reports;
    }

    @GetMapping(value = "/statement.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> statement(@AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        byte[] pdf = reports.generatePdf(UUID.fromString(jwt.getSubject()), from, to);
        String filename = "payflow-statement-" + from + "-to-" + to + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="" + filename + """)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
