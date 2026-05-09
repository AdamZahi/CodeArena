package com.codearena.module8_terminalquest.export;

import com.codearena.module8_terminalquest.dto.OverviewDto;
import com.codearena.module8_terminalquest.entity.LevelProgress;
import com.codearena.module8_terminalquest.repository.LevelProgressRepository;
import com.codearena.module8_terminalquest.service.AdvancedStatsService;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final LevelProgressRepository levelProgressRepository;
    private final AdvancedStatsService advancedStatsService;

    public byte[] generatePlayerReport(String userId) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf  = new PdfDocument(writer);
            Document doc     = new Document(pdf);

            doc.add(new Paragraph("Terminal Quest — Player Report")
                    .setFontSize(18).setBold().setFontColor(ColorConstants.DARK_GRAY));
            doc.add(new Paragraph("Player: " + userId).setFontSize(12));
            doc.add(new Paragraph("Generated: " + Instant.now()).setFontSize(10)
                    .setFontColor(ColorConstants.GRAY));
            doc.add(new Paragraph(" "));

            List<LevelProgress> progresses = levelProgressRepository.findByUserId(userId);
            long completed = progresses.stream().filter(LevelProgress::isCompleted).count();
            int  stars     = levelProgressRepository.sumStarsEarnedByUserId(userId);

            doc.add(new Paragraph("Summary").setFontSize(14).setBold());
            doc.add(new Paragraph("Total attempts: " + progresses.size()));
            doc.add(new Paragraph("Completed: " + completed));
            doc.add(new Paragraph("Total stars: " + stars));
            doc.add(new Paragraph(" "));

            if (!progresses.isEmpty()) {
                doc.add(new Paragraph("Progress Details").setFontSize(14).setBold());
                Table table = new Table(UnitValue.createPercentArray(new float[]{40, 15, 15, 15, 15}))
                        .setWidth(UnitValue.createPercentValue(100));
                table.addHeaderCell(headerCell("Mission/Level"));
                table.addHeaderCell(headerCell("Completed"));
                table.addHeaderCell(headerCell("Attempts"));
                table.addHeaderCell(headerCell("Stars"));
                table.addHeaderCell(headerCell("Date"));

                for (LevelProgress lp : progresses) {
                    String ref = lp.getMission() != null
                            ? "Mission " + lp.getMission().getId().toString().substring(0, 8)
                            : lp.getLevel() != null
                                    ? "Level " + lp.getLevel().getId().toString().substring(0, 8)
                                    : "—";
                    table.addCell(ref);
                    table.addCell(lp.isCompleted() ? "Yes" : "No");
                    table.addCell(String.valueOf(lp.getAttempts()));
                    table.addCell(String.valueOf(lp.getStarsEarned()));
                    table.addCell(lp.getCompletedAt() != null ? lp.getCompletedAt().substring(0, 10) : "—");
                }
                doc.add(table);
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("[export] player report error: {}", e.getMessage());
            return new byte[0];
        }
    }

    public byte[] generateGlobalReport() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf  = new PdfDocument(writer);
            Document doc     = new Document(pdf);

            doc.add(new Paragraph("Terminal Quest — Global Report")
                    .setFontSize(18).setBold().setFontColor(ColorConstants.DARK_GRAY));
            doc.add(new Paragraph("Generated: " + Instant.now()).setFontSize(10)
                    .setFontColor(ColorConstants.GRAY));
            doc.add(new Paragraph(" "));

            OverviewDto overview = advancedStatsService.getOverview();
            doc.add(new Paragraph("Platform Overview").setFontSize(14).setBold());
            doc.add(new Paragraph("Total players: " + overview.getTotalPlayers()));
            doc.add(new Paragraph("Total mission attempts: " + overview.getTotalMissionAttempts()));
            doc.add(new Paragraph("Total completions: " + overview.getTotalMissionCompletions()));
            doc.add(new Paragraph("Overall completion rate: " + overview.getOverallCompletionRate() + "%"));
            doc.add(new Paragraph("Total survival sessions: " + overview.getTotalSurvivalSessions()));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Difficulty Breakdown").setFontSize(14).setBold());
            Table table = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                    .setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell(headerCell("Difficulty"));
            table.addHeaderCell(headerCell("Attempts"));
            table.addHeaderCell(headerCell("Completions"));
            table.addHeaderCell(headerCell("Rate"));
            overview.getDifficultyBreakdown().forEach(d -> {
                table.addCell(d.getDifficulty());
                table.addCell(String.valueOf(d.getTotalAttempts()));
                table.addCell(String.valueOf(d.getCompletions()));
                table.addCell(d.getCompletionRate() + "%");
            });
            doc.add(table);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("[export] global report error: {}", e.getMessage());
            return new byte[0];
        }
    }

    private Cell headerCell(String text) {
        return new Cell().add(new Paragraph(text).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY);
    }
}
