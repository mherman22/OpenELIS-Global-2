package org.openelisglobal.compliancereport.pdf;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto.CollectionConditionDto;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto.ParameterResultDto;
import org.openelisglobal.compliancereport.dto.OrderPreviewDto.SignatureDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class LaporanHasilPdfGenerator {

    @Autowired
    private MessageSource messageSource;

    private String msg(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale);
    }

    // -----------------------------------------------------------------------
    // Certificate data holder passed to generate()
    // -----------------------------------------------------------------------
    @Getter
    @Setter
    public static class CertificateData {
        private String certificateNumber;
        private String generatedAt;
        private String labName;
        private String labSubtitle;
        private String addressLine1;
        private String addressLine2;
        private String phone;
        private String labLogoBase64;
        private String accreditationNumber;
        private String accreditationBody;
        private String accreditationLogoBase64;
        private String footerText;
        private boolean showPageNumbers;
        private String pageNumberFormat;
        private String labNumber;
        private String siteName;
        private String standardName;
        private String collectionDate;
        private List<CollectionConditionDto> collectionConditions;
        private List<ParameterResultDto> parameters;
        private List<SignatureDto> signatures;
    }

    // -----------------------------------------------------------------------
    // Colours and fonts
    // -----------------------------------------------------------------------
    private static final BaseColor HEADER_BG = new BaseColor(0x0F, 0x62, 0xFE); // CDS blue
    private static final BaseColor PASS_BG = new BaseColor(0xDE, 0xF7, 0xE9);
    private static final BaseColor FAIL_BG = new BaseColor(0xFF, 0xD7, 0xD9);
    private static final BaseColor MARGINAL_BG = new BaseColor(0xFF, 0xF8, 0xE1);
    private static final BaseColor SECTION_BG = new BaseColor(0xF4, 0xF4, 0xF4);
    private static final BaseColor BORDER_COLOR = new BaseColor(0xE0, 0xE0, 0xE0);

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.WHITE);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.WHITE);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.DARK_GRAY);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.DARK_GRAY);
    private static final Font FONT_VALUE = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY);
    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
    private static final Font FONT_TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.DARK_GRAY);
    private static final Font FONT_PASS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8,
            new BaseColor(0x19, 0x8F, 0x3D));
    private static final Font FONT_FAIL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8,
            new BaseColor(0xDA, 0x1E, 0x28));
    private static final Font FONT_MARGINAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8,
            new BaseColor(0xF1, 0xC2, 0x1B));
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA, 7, BaseColor.GRAY);

    // -----------------------------------------------------------------------
    // Main entry point
    // -----------------------------------------------------------------------
    public byte[] generate(CertificateData data, Locale locale) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 50);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            if (data.isShowPageNumbers()) {
                writer.setPageEvent(new PageFooterEvent(data));
            }
            doc.open();
            addContent(doc, data, locale);
            doc.close();
        } catch (DocumentException e) {
            LogEvent.logError(e);
            throw new RuntimeException("PDF generation failed", e);
        }
        return baos.toByteArray();
    }

    private void addContent(Document doc, CertificateData data, Locale locale) throws DocumentException {
        addHeader(doc, data);
        addCertificateTitleBar(doc, data, locale);
        addSpacer(doc, 8);
        addSampleInfoSection(doc, data, locale);
        if (data.getCollectionConditions() != null && !data.getCollectionConditions().isEmpty()) {
            addSpacer(doc, 6);
            addCollectionConditionsSection(doc, data, locale);
        }
        addSpacer(doc, 6);
        addResultsSection(doc, data, locale);
        if (data.getSignatures() != null && !data.getSignatures().isEmpty()) {
            addSpacer(doc, 10);
            addSignatureSection(doc, data, locale);
        }
    }

    // -----------------------------------------------------------------------
    // Header: logo | lab info | accreditation
    // -----------------------------------------------------------------------
    private void addHeader(Document doc, CertificateData data) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[] { 1.5f, 5f, 2f });
        table.setWidthPercentage(100);
        table.setSpacingAfter(0);

        // Left: lab logo
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(4);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Image labLogo = tryDecodeImage(data.getLabLogoBase64());
        if (labLogo != null) {
            labLogo.scaleToFit(80, 60);
            logoCell.addElement(labLogo);
        } else {
            logoCell.addElement(new Paragraph(""));
        }
        table.addCell(logoCell);

        // Center: lab name and address
        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setPadding(4);
        infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Font labNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BaseColor.DARK_GRAY);
        infoCell.addElement(new Paragraph(coalesce(data.getLabName(), "Laboratory"), labNameFont));
        if (notBlank(data.getLabSubtitle())) {
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY);
            infoCell.addElement(new Paragraph(data.getLabSubtitle(), subFont));
        }
        if (notBlank(data.getAddressLine1())) {
            infoCell.addElement(new Paragraph(data.getAddressLine1(), FONT_VALUE));
        }
        if (notBlank(data.getAddressLine2())) {
            infoCell.addElement(new Paragraph(data.getAddressLine2(), FONT_VALUE));
        }
        if (notBlank(data.getPhone())) {
            infoCell.addElement(new Paragraph("Tel: " + data.getPhone(), FONT_VALUE));
        }
        table.addCell(infoCell);

        // Right: accreditation
        PdfPCell accCell = new PdfPCell();
        accCell.setBorder(Rectangle.NO_BORDER);
        accCell.setPadding(4);
        accCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        accCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Image accLogo = tryDecodeImage(data.getAccreditationLogoBase64());
        if (accLogo != null) {
            accLogo.scaleToFit(60, 50);
            accLogo.setAlignment(Image.RIGHT);
            accCell.addElement(accLogo);
        }
        if (notBlank(data.getAccreditationNumber())) {
            Font accFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.DARK_GRAY);
            Paragraph accPara = new Paragraph(data.getAccreditationNumber(), accFont);
            accPara.setAlignment(Element.ALIGN_RIGHT);
            accCell.addElement(accPara);
        }
        if (notBlank(data.getAccreditationBody())) {
            Paragraph bodyPara = new Paragraph(data.getAccreditationBody(), FONT_VALUE);
            bodyPara.setAlignment(Element.ALIGN_RIGHT);
            accCell.addElement(bodyPara);
        }
        table.addCell(accCell);

        doc.add(table);

        // Horizontal rule
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell ruleCell = new PdfPCell(new Phrase(""));
        ruleCell.setBackgroundColor(HEADER_BG);
        ruleCell.setFixedHeight(2);
        ruleCell.setBorder(Rectangle.NO_BORDER);
        rule.addCell(ruleCell);
        doc.add(rule);
    }

    // -----------------------------------------------------------------------
    // Blue title bar
    // -----------------------------------------------------------------------
    private void addCertificateTitleBar(Document doc, CertificateData data, Locale locale) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph title = new Paragraph(msg("pdf.laporanHasil.title", locale), FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(title);

        Paragraph subLine = new Paragraph("No: " + coalesce(data.getCertificateNumber(), "—"), FONT_SUBTITLE);
        subLine.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(subLine);

        table.addCell(cell);
        doc.add(table);
    }

    // -----------------------------------------------------------------------
    // Sample information section
    // -----------------------------------------------------------------------
    private void addSampleInfoSection(Document doc, CertificateData data, Locale locale) throws DocumentException {
        addSectionHeader(doc, msg("pdf.laporanHasil.section.sampleInfo", locale));

        PdfPTable table = new PdfPTable(new float[] { 2, 3, 2, 3 });
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        addInfoRow(table, msg("pdf.laporanHasil.label.labNumber", locale), coalesce(data.getLabNumber(), "—"),
                msg("pdf.laporanHasil.label.collectionDate", locale), coalesce(data.getCollectionDate(), "—"));
        addInfoRow(table, msg("pdf.laporanHasil.label.site", locale), coalesce(data.getSiteName(), "—"),
                msg("pdf.laporanHasil.label.referenceStandard", locale), coalesce(data.getStandardName(), "—"));
        addInfoRow(table, msg("pdf.laporanHasil.label.certificateNo", locale),
                coalesce(data.getCertificateNumber(), "—"), msg("pdf.laporanHasil.label.dateIssued", locale),
                coalesce(data.getGeneratedAt(), "—"));

        doc.add(table);
    }

    // -----------------------------------------------------------------------
    // Collection conditions section
    // -----------------------------------------------------------------------
    private void addCollectionConditionsSection(Document doc, CertificateData data, Locale locale)
            throws DocumentException {
        addSectionHeader(doc, msg("pdf.laporanHasil.section.collectionConditions", locale));

        List<CollectionConditionDto> conds = data.getCollectionConditions();
        int cols = Math.min(conds.size(), 4);
        if (cols == 0)
            return;

        PdfPTable table = new PdfPTable(cols * 2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        for (CollectionConditionDto c : conds) {
            PdfPCell labelCell = new PdfPCell(new Phrase(c.getLabel(), FONT_LABEL));
            styleInfoLabelCell(labelCell);
            table.addCell(labelCell);
            PdfPCell valCell = new PdfPCell(new Phrase(coalesce(c.getValue(), "—"), FONT_VALUE));
            styleInfoValueCell(valCell);
            table.addCell(valCell);
        }
        doc.add(table);
    }

    // -----------------------------------------------------------------------
    // Results table
    // -----------------------------------------------------------------------
    private void addResultsSection(Document doc, CertificateData data, Locale locale) throws DocumentException {
        addSectionHeader(doc, msg("pdf.laporanHasil.section.testResults", locale));

        if (data.getParameters() == null || data.getParameters().isEmpty()) {
            doc.add(new Paragraph("No results available.", FONT_VALUE));
            return;
        }

        PdfPTable table = new PdfPTable(new float[] { 3.5f, 1.5f, 1.5f, 2f, 1.5f });
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        String[] headers = { msg("pdf.laporanHasil.col.parameter", locale), msg("pdf.laporanHasil.col.result", locale),
                msg("pdf.laporanHasil.col.unit", locale), msg("pdf.laporanHasil.col.threshold", locale),
                msg("pdf.laporanHasil.col.status", locale) };
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, FONT_TABLE_HEADER));
            hCell.setBackgroundColor(new BaseColor(0x39, 0x39, 0x3E));
            hCell.setPadding(6);
            hCell.setBorderColor(BORDER_COLOR);
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hCell);
        }

        boolean alternate = false;
        for (ParameterResultDto p : data.getParameters()) {
            BaseColor rowBg = alternate ? new BaseColor(0xF4, 0xF4, 0xF4) : BaseColor.WHITE;

            PdfPCell nameCell = new PdfPCell(new Phrase(coalesce(p.getName(), "—"), FONT_TABLE_CELL));
            nameCell.setBackgroundColor(rowBg);
            nameCell.setPadding(5);
            nameCell.setBorderColor(BORDER_COLOR);
            table.addCell(nameCell);

            table.addCell(centeredCell(coalesce(p.getValue(), "—"), FONT_TABLE_CELL, rowBg));
            table.addCell(centeredCell(coalesce(p.getUnit(), "—"), FONT_TABLE_CELL, rowBg));
            table.addCell(centeredCell(coalesce(p.getThreshold(), "—"), FONT_TABLE_CELL, rowBg));
            table.addCell(buildStatusCell(p.getStatus(), locale));

            alternate = !alternate;
        }
        doc.add(table);
    }

    private PdfPCell buildStatusCell(String status, Locale locale) {
        BaseColor bg;
        Font font;
        String label;
        switch (coalesce(status, "UNKNOWN")) {
        case "PASS":
            bg = PASS_BG;
            font = FONT_PASS;
            label = msg("pdf.laporanHasil.status.pass", locale);
            break;
        case "FAIL":
            bg = FAIL_BG;
            font = FONT_FAIL;
            label = msg("pdf.laporanHasil.status.fail", locale);
            break;
        case "MARGINAL":
            bg = MARGINAL_BG;
            font = FONT_MARGINAL;
            label = msg("pdf.laporanHasil.status.marginal", locale);
            break;
        case "NOT_TESTED":
            bg = new BaseColor(0xE0, 0xE0, 0xE0);
            font = FONT_TABLE_CELL;
            label = msg("pdf.laporanHasil.status.notTested", locale);
            break;
        default:
            bg = new BaseColor(0xF4, 0xF4, 0xF4);
            font = FONT_TABLE_CELL;
            label = "—";
        }
        PdfPCell cell = new PdfPCell(new Phrase(label, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }

    // -----------------------------------------------------------------------
    // Signature section
    // -----------------------------------------------------------------------
    private void addSignatureSection(Document doc, CertificateData data, Locale locale) throws DocumentException {
        addSectionHeader(doc, msg("pdf.laporanHasil.section.signatures", locale));

        List<SignatureDto> sigs = data.getSignatures();
        int cols = Math.max(1, Math.min(sigs.size(), 3));
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        for (SignatureDto sig : sigs) {
            PdfPCell cell = new PdfPCell();
            cell.setBorderColor(BORDER_COLOR);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph rolePara = new Paragraph(humanizeMeaning(sig.getRole(), locale), FONT_LABEL);
            rolePara.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(rolePara);

            // Signature line placeholder
            PdfPTable lineTable = new PdfPTable(1);
            lineTable.setWidthPercentage(80);
            lineTable.setSpacingBefore(20);
            lineTable.setSpacingAfter(4);
            PdfPCell lineCell = new PdfPCell(new Phrase(""));
            lineCell.setBorder(Rectangle.BOTTOM);
            lineCell.setBorderColor(BaseColor.DARK_GRAY);
            lineCell.setFixedHeight(18);
            lineTable.addCell(lineCell);
            cell.addElement(lineTable);

            if (notBlank(sig.getName())) {
                Paragraph namePara = new Paragraph(sig.getName(), FONT_VALUE);
                namePara.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(namePara);
            }
            if (notBlank(sig.getTimestamp())) {
                Paragraph datePara = new Paragraph(sig.getTimestamp(), FONT_FOOTER);
                datePara.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(datePara);
            }
            table.addCell(cell);
        }
        doc.add(table);
    }

    private String humanizeMeaning(String meaning, Locale locale) {
        if (meaning == null)
            return "";
        switch (meaning.toUpperCase()) {
        case "AUTHORED":
            return msg("pdf.laporanHasil.sig.preparedBy", locale);
        case "VALIDATED_AND_RELEASED":
            return msg("pdf.laporanHasil.sig.validatedBy", locale);
        case "REJECTED":
            return msg("pdf.laporanHasil.sig.rejected", locale);
        default:
            return meaning;
        }
    }

    // -----------------------------------------------------------------------
    // Page footer event (page numbers)
    // -----------------------------------------------------------------------
    private static class PageFooterEvent extends PdfPageEventHelper {
        private final CertificateData data;

        PageFooterEvent(CertificateData data) {
            this.data = data;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(new float[] { 5, 2 });
            footer.setTotalWidth(document.right() - document.left());
            footer.setLockedWidth(true);

            String footerText = coalesce(data.getFooterText(), "");
            PdfPCell leftCell = new PdfPCell(new Phrase(footerText, FONT_FOOTER));
            leftCell.setBorder(Rectangle.TOP);
            leftCell.setBorderColor(BORDER_COLOR);
            leftCell.setPaddingTop(4);
            footer.addCell(leftCell);

            String pageNumFormat = coalesce(data.getPageNumberFormat(), "Page {page} of {total}");
            String pageText = pageNumFormat.replace("{page}", String.valueOf(writer.getPageNumber())).replace("{total}",
                    String.valueOf(writer.getPageNumber()));
            PdfPCell rightCell = new PdfPCell(new Phrase(pageText, FONT_FOOTER));
            rightCell.setBorder(Rectangle.TOP);
            rightCell.setBorderColor(BORDER_COLOR);
            rightCell.setPaddingTop(4);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            footer.addCell(rightCell);

            footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 5, writer.getDirectContent());
        }

        private static String coalesce(String v, String def) {
            return (v != null && !v.isBlank()) ? v : def;
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private void addSectionHeader(Document doc, String title) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(8);
        PdfPCell cell = new PdfPCell(new Phrase(title, FONT_SECTION));
        cell.setBackgroundColor(SECTION_BG);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(5);
        t.addCell(cell);
        doc.add(t);
    }

    private void addInfoRow(PdfPTable table, String label1, String value1, String label2, String value2) {
        PdfPCell l1 = new PdfPCell(new Phrase(label1, FONT_LABEL));
        styleInfoLabelCell(l1);
        table.addCell(l1);

        PdfPCell v1 = new PdfPCell(new Phrase(value1, FONT_VALUE));
        styleInfoValueCell(v1);
        table.addCell(v1);

        PdfPCell l2 = new PdfPCell(new Phrase(label2, FONT_LABEL));
        styleInfoLabelCell(l2);
        table.addCell(l2);

        PdfPCell v2 = new PdfPCell(new Phrase(value2, FONT_VALUE));
        styleInfoValueCell(v2);
        table.addCell(v2);
    }

    private void styleInfoLabelCell(PdfPCell cell) {
        cell.setBackgroundColor(SECTION_BG);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(5);
    }

    private void styleInfoValueCell(PdfPCell cell) {
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(5);
    }

    private PdfPCell centeredCell(String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }

    private void addSpacer(Document doc, float height) throws DocumentException {
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(height);
        doc.add(spacer);
    }

    private Image tryDecodeImage(String base64) {
        if (base64 == null || base64.isBlank())
            return null;
        try {
            String data = base64.contains(",") ? base64.split(",", 2)[1] : base64;
            byte[] bytes = Base64.getDecoder().decode(data);
            return Image.getInstance(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private static String coalesce(String v, String def) {
        return (v != null && !v.isBlank()) ? v : def;
    }

    private static boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }
}
