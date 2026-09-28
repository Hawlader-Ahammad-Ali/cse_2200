package com.mindmap.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.mindmap.config.AppConfig;
import com.mindmap.config.ServiceRegistry;
import com.mindmap.model.KnowledgeItem;
import com.mindmap.model.Source;
import com.mindmap.model.Tag;
import com.mindmap.service.AnalyticsService;
import com.mindmap.service.KnowledgeService;
import com.mindmap.util.AppContext;
import com.mindmap.util.exceptions.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Generates PDF documents for sources, knowledge items, and learning reports.
 *
 * <p>All methods run synchronously. Callers should invoke them from a worker
 * thread and only touch the UI on the JavaFX thread.</p>
 */
public final class PdfExporter {

    private static final Logger log = LoggerFactory.getLogger(PdfExporter.class);
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final DateTimeFormatter HUMAN_TS = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private PdfExporter() { }

    // =============================================================
    // SOURCE EXPORT
    // =============================================================

    public static void exportSource(Source source, File dest) throws ServiceException {
        if (source == null) throw new ServiceException("No source to export.");
        log.info("Exporting source '{}' to {}", source.getTitle(), dest.getAbsolutePath());

        List<KnowledgeItem> linked = ServiceRegistry.knowledgeService()
                .getKnowledgeForSource(source.getId());

        Document doc = new Document(PageSize.A4, 56, 56, 60, 60);
        try (FileOutputStream out = new FileOutputStream(dest)) {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            addFooter(writer);
            doc.open();

            // Title
            doc.add(PdfStyles.title(source.getSourceType().getIcon() + "  " + source.getTitle()));
            doc.add(PdfStyles.subtitle(source.getSourceType().getLabel()
                    + (source.getAuthorCreator() != null ? "  ·  " + source.getAuthorCreator() : "")));
            doc.add(separator());

            // Info table
            doc.add(PdfStyles.h1("Source Information"));
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.setWidths(new float[]{1.2f, 3f});
            info.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            addKeyValue(info, "Status", source.getStatus().getLabel());
            if (source.getRating() != null) {
                addKeyValue(info, "Rating", "★".repeat(source.getRating()));
            }
            if (source.getDateAdded() != null) {
                addKeyValue(info, "Added", source.getDateAdded().toLocalDate().toString());
            }
            if (source.getDateConsumed() != null) {
                addKeyValue(info, "Consumed", source.getDateConsumed().toString());
            }
            if (source.getUrl() != null && !source.getUrl().isBlank()) {
                addKeyValue(info, "URL", source.getUrl());
            }
            if (!source.getTags().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < source.getTags().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(source.getTags().get(i).getName());
                }
                addKeyValue(info, "Tags", sb.toString());
            }
            doc.add(info);

            // Description
            if (source.getDescription() != null && !source.getDescription().isBlank()) {
                doc.add(PdfStyles.h1("Description"));
                doc.add(PdfStyles.body(source.getDescription()));
            }

            // Notes
            if (source.getNotes() != null && !source.getNotes().isBlank()) {
                doc.add(PdfStyles.h1("Notes"));
                doc.add(PdfStyles.body(source.getNotes()));
            }

            // Related Knowledge
            if (!linked.isEmpty()) {
                doc.add(PdfStyles.h1("Related Knowledge (" + linked.size() + ")"));
                for (KnowledgeItem k : linked) {
                    doc.add(PdfStyles.h2(k.getItemType().getIcon() + "  " + k.getTitle()));
                    if (k.getCategory() != null && !k.getCategory().isBlank()) {
                        doc.add(PdfStyles.muted("Category: " + k.getCategory()));
                    }
                    if (k.getDescription() != null && !k.getDescription().isBlank()) {
                        doc.add(PdfStyles.body(k.getDescription()));
                    }
                    if (k.getPersonalNote() != null && !k.getPersonalNote().isBlank()) {
                        Paragraph p = new Paragraph();
                        p.add(new com.lowagie.text.Chunk("📝  My Takeaway:  ", PdfStyles.label()));
                        p.add(new com.lowagie.text.Chunk(k.getPersonalNote(), PdfStyles.bodyItalic()));
                        p.setSpacingAfter(8);
                        p.setLeading(15);
                        doc.add(p);
                    }
                }
            }

            doc.close();
            log.info("Source PDF written: {}", dest.getAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to export source PDF", e);
            throw new ServiceException("Could not write PDF: " + e.getMessage(), e);
        }
    }

    // =============================================================
    // KNOWLEDGE EXPORT
    // =============================================================

    public static void exportKnowledge(KnowledgeItem item, File dest) throws ServiceException {
        if (item == null) throw new ServiceException("No knowledge item to export.");
        log.info("Exporting knowledge '{}' to {}", item.getTitle(), dest.getAbsolutePath());

        List<Source> sources = ServiceRegistry.knowledgeService()
                .getSourcesFor(item.getId());

        Document doc = new Document(PageSize.A4, 56, 56, 60, 60);
        try (FileOutputStream out = new FileOutputStream(dest)) {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            addFooter(writer);
            doc.open();

            doc.add(PdfStyles.title(item.getItemType().getIcon() + "  " + item.getTitle()));
            doc.add(PdfStyles.subtitle(item.getItemType().getLabel()
                    + (item.getCategory() != null ? "  ·  " + item.getCategory() : "")));
            doc.add(separator());

            doc.add(PdfStyles.h1("Details"));
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.setWidths(new float[]{1.2f, 3f});
            info.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            addKeyValue(info, "Type",       item.getItemType().getLabel());
            addKeyValue(info, "Category",   item.getCategory() == null ? "—" : item.getCategory());
            addKeyValue(info, "Difficulty", item.getDifficulty());
            addKeyValue(info, "Importance", item.getImportance());
            addKeyValue(info, "Confidence", "★".repeat(item.getConfidence()) +
                    "☆".repeat(5 - item.getConfidence()));
            addKeyValue(info, "Origin",     item.getOrigin().getLabel());
            if (item.getDateLearned() != null) {
                addKeyValue(info, "Date learned", item.getDateLearned().toString());
            }
            if (!item.getTags().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < item.getTags().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(item.getTags().get(i).getName());
                }
                addKeyValue(info, "Tags", sb.toString());
            }
            doc.add(info);

            if (item.getDescription() != null && !item.getDescription().isBlank()) {
                doc.add(PdfStyles.h1("Description"));
                doc.add(PdfStyles.body(item.getDescription()));
            }

            if (item.getPersonalNote() != null && !item.getPersonalNote().isBlank()) {
                doc.add(PdfStyles.h1("My Takeaway"));
                doc.add(PdfStyles.body(item.getPersonalNote()));
            }

            if (!sources.isEmpty()) {
                doc.add(PdfStyles.h1("Where Did I Learn This?"));
                doc.add(PdfStyles.muted("You have explored this concept through "
                        + sources.size() + " source" + (sources.size() == 1 ? "" : "s") + "."));
                for (Source s : sources) {
                    Paragraph p = new Paragraph();
                    p.add(new com.lowagie.text.Chunk(s.getSourceType().getIcon() + "  ",
                            PdfStyles.body()));
                    p.add(new com.lowagie.text.Chunk(s.getTitle(),
                            new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11,
                                    com.lowagie.text.Font.BOLD, PdfStyles.COLOR_TEXT)));
                    p.add(new com.lowagie.text.Chunk("   —   " + s.getSourceType().getLabel(),
                            PdfStyles.small()));
                    p.setSpacingAfter(4);
                    doc.add(p);
                }
            }

            doc.close();
            log.info("Knowledge PDF written: {}", dest.getAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to export knowledge PDF", e);
            throw new ServiceException("Could not write PDF: " + e.getMessage(), e);
        }
    }

    // =============================================================
    // LEARNING REPORT
    // =============================================================

    public static void exportLearningReport(File dest) throws ServiceException {
        log.info("Exporting learning report to {}", dest.getAbsolutePath());

        AnalyticsService analytics = ServiceRegistry.analyticsService();
        AnalyticsService.DashboardStats stats = analytics.getDashboardStats();
        Map<String, Integer> categories = analytics.getCategoryDistribution();
        List<AnalyticsService.TopicAccuracy> weak = analytics.getWeakTopics(8);
        List<AnalyticsService.TopicAccuracy> strong = analytics.getStrongTopics(8);
        List<AnalyticsService.CrossSourceConcept> cross = analytics.getTopCrossSourceConcepts(8);
        List<AnalyticsService.MonthlyGrowth> growth = analytics.getMonthlyGrowth(6);
        List<String> interests = analytics.getInterestSignals(5);

        String username = AppContext.getInstance().isLoggedIn()
                ? AppContext.getInstance().getCurrentUser().getDisplayLabel()
                : "Learner";

        Document doc = new Document(PageSize.A4, 56, 56, 60, 60);
        try (FileOutputStream out = new FileOutputStream(dest)) {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            addFooter(writer);
            doc.open();

            doc.add(PdfStyles.title("🧠  MindMap — Learning Report"));
            doc.add(PdfStyles.subtitle("Prepared for " + username
                    + "  ·  " + LocalDateTime.now().format(HUMAN_TS)));
            doc.add(separator());

            // -------- Overview --------
            doc.add(PdfStyles.h1("Overview"));
            PdfPTable overview = new PdfPTable(4);
            overview.setWidthPercentage(100);
            overview.setSpacingAfter(8);

            addStatCell(overview, "Sources",         String.valueOf(stats.totalSources()));
            addStatCell(overview, "Knowledge",       String.valueOf(stats.totalKnowledge()));
            addStatCell(overview, "Connections",     String.valueOf(stats.totalConnections()));
            addStatCell(overview, "Flashcards",      String.valueOf(stats.totalFlashcards()));
            addStatCell(overview, "Reviews due",     String.valueOf(stats.reviewsDue()));
            addStatCell(overview, "Reviewed today",  String.valueOf(stats.reviewedToday()));
            addStatCell(overview, "Current streak",  stats.currentStreak() + " day"
                    + (stats.currentStreak() == 1 ? "" : "s"));
            addStatCell(overview, "Quiz questions",  String.valueOf(stats.totalQuizQuestions()));
            doc.add(overview);

            // -------- Interests --------
            if (!interests.isEmpty()) {
                doc.add(PdfStyles.h1("What I'm Learning"));
                com.lowagie.text.List list = new com.lowagie.text.List(
                        com.lowagie.text.List.ORDERED, 16);
                list.setListSymbol("");
                int rank = 1;
                for (String s : interests) {
                    list.add(new com.lowagie.text.ListItem(
                            rank + ".   " + s, PdfStyles.body()));
                    rank++;
                }
                doc.add(list);
            }

            // -------- Categories --------
            if (!categories.isEmpty()) {
                doc.add(PdfStyles.h1("Top Categories"));
                PdfPTable catTable = new PdfPTable(3);
                catTable.setWidthPercentage(100);
                catTable.setWidths(new float[]{3f, 1f, 3f});
                catTable.setSpacingAfter(8);

                headerCell(catTable, "Category");
                headerCell(catTable, "Items");
                headerCell(catTable, "Share");

                int totalItems = categories.values().stream().mapToInt(Integer::intValue).sum();
                for (Map.Entry<String, Integer> e : categories.entrySet()) {
                    bodyCell(catTable, e.getKey());
                    bodyCell(catTable, String.valueOf(e.getValue()));
                    bodyCell(catTable, (totalItems == 0 ? 0 : e.getValue() * 100 / totalItems) + "%");
                }
                doc.add(catTable);
            }

            // -------- Weak / Strong topics --------
            if (!weak.isEmpty() || !strong.isEmpty()) {
                doc.add(PdfStyles.h1("Quiz Performance"));

                if (!weak.isEmpty()) {
                    doc.add(PdfStyles.h2("Weak Topics"));
                    PdfPTable t = new PdfPTable(3);
                    t.setWidthPercentage(100);
                    t.setWidths(new float[]{4f, 1.4f, 1.4f});
                    headerCell(t, "Topic"); headerCell(t, "Attempts"); headerCell(t, "Accuracy");
                    for (AnalyticsService.TopicAccuracy ta : weak) {
                        bodyCell(t, ta.label());
                        bodyCell(t, String.valueOf(ta.attempts()));
                        bodyCell(t, Math.round(ta.getAccuracy() * 100) + "%",
                                PdfStyles.COLOR_DANGER);
                    }
                    t.setSpacingAfter(10);
                    doc.add(t);
                }

                if (!strong.isEmpty()) {
                    doc.add(PdfStyles.h2("Strong Topics"));
                    PdfPTable t = new PdfPTable(3);
                    t.setWidthPercentage(100);
                    t.setWidths(new float[]{4f, 1.4f, 1.4f});
                    headerCell(t, "Topic"); headerCell(t, "Attempts"); headerCell(t, "Accuracy");
                    for (AnalyticsService.TopicAccuracy ta : strong) {
                        bodyCell(t, ta.label());
                        bodyCell(t, String.valueOf(ta.attempts()));
                        bodyCell(t, Math.round(ta.getAccuracy() * 100) + "%",
                                PdfStyles.COLOR_SUCCESS);
                    }
                    t.setSpacingAfter(10);
                    doc.add(t);
                }
            }

            // -------- Cross-source --------
            if (!cross.isEmpty()) {
                doc.add(PdfStyles.h1("Cross-Source Concepts"));
                doc.add(PdfStyles.muted(
                        "Ideas that appear in more than one source — often the "
                                + "most valuable connections to strengthen."));
                com.lowagie.text.List list = new com.lowagie.text.List(
                        com.lowagie.text.List.UNORDERED, 16);
                for (AnalyticsService.CrossSourceConcept c : cross) {
                    list.add(new com.lowagie.text.ListItem(
                            c.title() + "   (" + c.sourceCount() + " sources)",
                            PdfStyles.body()));
                }
                doc.add(list);
            }

            // -------- Monthly growth --------
            if (!growth.isEmpty()) {
                doc.add(PdfStyles.h1("Monthly Growth (last 6 months)"));
                PdfPTable t = new PdfPTable(4);
                t.setWidthPercentage(100);
                t.setWidths(new float[]{2f, 1.4f, 1.6f, 1.4f});
                headerCell(t, "Month");
                headerCell(t, "Sources");
                headerCell(t, "Knowledge");
                headerCell(t, "Reviews");
                for (AnalyticsService.MonthlyGrowth g : growth) {
                    bodyCell(t, AnalyticsService.prettyMonth(g.month()));
                    bodyCell(t, String.valueOf(g.sources()));
                    bodyCell(t, String.valueOf(g.knowledge()));
                    bodyCell(t, String.valueOf(g.reviews()));
                }
                t.setSpacingAfter(8);
                doc.add(t);
            }

            doc.add(separator());
            doc.add(PdfStyles.muted(
                    "Generated by MindMap v" + AppConfig.APP_VERSION
                            + "  ·  All data reflects your local knowledge base as of "
                            + LocalDateTime.now().format(HUMAN_TS)));

            doc.close();
            log.info("Learning report PDF written: {}", dest.getAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to export learning report PDF", e);
            throw new ServiceException("Could not write PDF: " + e.getMessage(), e);
        }
    }

    // =============================================================
    // Helpers
    // =============================================================

    private static void addKeyValue(PdfPTable table, String key, String value) {
        PdfPCell k = new PdfPCell(new Phrase(key, PdfStyles.label()));
        k.setBorder(Rectangle.NO_BORDER);
        k.setPaddingBottom(3);
        table.addCell(k);

        PdfPCell v = new PdfPCell(new Phrase(value == null ? "—" : value, PdfStyles.body()));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPaddingBottom(3);
        table.addCell(v);
    }

    private static void addStatCell(PdfPTable t, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8);
        cell.setBackgroundColor(new java.awt.Color(245, 246, 250));

        Paragraph p = new Paragraph();
        p.add(new com.lowagie.text.Chunk(value + "\n",
                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18,
                        com.lowagie.text.Font.BOLD, PdfStyles.COLOR_TEXT)));
        p.add(new com.lowagie.text.Chunk(label, PdfStyles.small()));
        cell.addElement(p);
        t.addCell(cell);
    }

    private static void headerCell(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, PdfStyles.label()));
        c.setBackgroundColor(new java.awt.Color(238, 241, 248));
        c.setBorderColor(PdfStyles.COLOR_BORDER);
        c.setPadding(6);
        t.addCell(c);
    }

    private static void bodyCell(PdfPTable t, String text) {
        bodyCell(t, text, PdfStyles.COLOR_TEXT);
    }

    private static void bodyCell(PdfPTable t, String text, java.awt.Color color) {
        com.lowagie.text.Font f = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 10,
                com.lowagie.text.Font.NORMAL, color);
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, f));
        c.setBorderColor(PdfStyles.COLOR_BORDER);
        c.setPadding(6);
        t.addCell(c);
    }

    private static Paragraph separator() {
        Paragraph p = new Paragraph();
        LineSeparator ls = new LineSeparator();
        ls.setLineColor(PdfStyles.COLOR_BORDER);
        ls.setLineWidth(0.8f);
        p.add(new com.lowagie.text.Chunk(ls));
        p.setSpacingAfter(10);
        return p;
    }

    private static void addFooter(PdfWriter writer) {
        writer.setPageEvent(new com.lowagie.text.pdf.PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter w, Document doc) {
                com.lowagie.text.pdf.PdfContentByte cb = w.getDirectContent();
                com.lowagie.text.pdf.ColumnText.showTextAligned(
                        cb,
                        Element.ALIGN_RIGHT,
                        new Phrase("Page " + w.getPageNumber(), PdfStyles.small()),
                        doc.right(), doc.bottom() - 30, 0);
                com.lowagie.text.pdf.ColumnText.showTextAligned(
                        cb,
                        Element.ALIGN_LEFT,
                        new Phrase("MindMap  ·  " + LocalDateTime.now().format(HUMAN_TS),
                                PdfStyles.small()),
                        doc.left(), doc.bottom() - 30, 0);
            }
        });
    }

    // =============================================================
    // Filename helpers
    // =============================================================

    public static String defaultSourceFileName(Source s) {
        return "MindMap_Source_" + com.mindmap.util.FileDialogs.sanitizeFileName(s.getTitle())
                + "_" + LocalDateTime.now().format(FILE_TS) + ".pdf";
    }

    public static String defaultKnowledgeFileName(KnowledgeItem k) {
        return "MindMap_Knowledge_" + com.mindmap.util.FileDialogs.sanitizeFileName(k.getTitle())
                + "_" + LocalDateTime.now().format(FILE_TS) + ".pdf";
    }

    public static String defaultReportFileName() {
        return "MindMap_Learning_Report_" + LocalDateTime.now().format(FILE_TS) + ".pdf";
    }
}