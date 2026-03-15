package com.examplatform.certificateservice.service;

import com.examplatform.certificateservice.domain.Certificate;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates a PDF certificate using iText 8.
 * Returns the PDF as a byte array for upload to MinIO.
 */
@Slf4j
@Service
public class PdfCertificateGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    public byte[] generate(Certificate cert) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, PageSize.A4.rotate()); // landscape
            doc.setMargins(60, 60, 60, 60);

            PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont bodyFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont italicFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            // Header
            doc.add(new Paragraph("CERTIFICATE OF ACHIEVEMENT")
                    .setFont(titleFont)
                    .setFontSize(28)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // Divider line (simulated with underline text)
            doc.add(new Paragraph("─────────────────────────────────────────────")
                    .setFont(bodyFont)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30));

            // Award statement
            doc.add(new Paragraph("This is to certify that")
                    .setFont(italicFont)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            // Student name
            doc.add(new Paragraph(cert.getStudentName())
                    .setFont(titleFont)
                    .setFontSize(24)
                    .setFontColor(ColorConstants.BLUE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            doc.add(new Paragraph("has successfully completed")
                    .setFont(italicFont)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            // Exam title
            doc.add(new Paragraph(cert.getExamTitle())
                    .setFont(titleFont)
                    .setFontSize(20)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            // Score
            doc.add(new Paragraph(String.format("with a score of %d%%", cert.getScorePercentage()))
                    .setFont(bodyFont)
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30));

            // Date
            String issueDate = LocalDate.now().format(DATE_FMT);
            doc.add(new Paragraph("Issued on: " + issueDate)
                    .setFont(bodyFont)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            // Verification code
            doc.add(new Paragraph("Verification Code: " + cert.getVerifyCode())
                    .setFont(bodyFont)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5));

            doc.add(new Paragraph("Verify at: http://localhost:8080/api/v1/certificates/verify/" + cert.getVerifyCode())
                    .setFont(italicFont)
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            // Footer divider
            doc.add(new Paragraph("─────────────────────────────────────────────")
                    .setFont(bodyFont)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20));

            doc.add(new Paragraph("EC7205 Online Exam & Certification Platform | University of Ruhuna")
                    .setFont(bodyFont)
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.close();
            log.info("Generated PDF certificate for enrollment {}", cert.getEnrollmentId());
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF certificate", e);
        }
    }
}
