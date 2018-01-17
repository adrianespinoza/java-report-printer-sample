package com.dharbor.providerportal.framework.printing;

//import com.dharbor.providerportal.service.printing.TrackingPrintingService;
//import com.dharbor.providerportal.web.i18n.Messages;
//import com.dharbor.providerportal.web.tag.Functions;
import com.itextpdf.text.*;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.tool.xml.ElementList;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.tool.xml.css.CssFile;
import com.itextpdf.tool.xml.css.StyleAttrCSSResolver;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.ElementHandlerPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class HtmlPDFPrinter {
	private String root;
    private Resource styles;
    private Resource leftLogo;
    private Resource rightLogo;


    private String dateSubmit;
    private String dateReSubmit;
    private String legalName;
    private String packageName;

    private List<Map<String, Object>> templates;

    private List<Chapter> chapterList = new ArrayList<Chapter>();   

    @SuppressWarnings("unused")
    private static Logger LOG = Logger.getLogger(HtmlPDFPrinter.class);

    public void setRoot(String root) {
        this.root = root;
    }

    public String print(List<Map<String, Object>> templates, String packageName, String dateSubmit,
                        String dateReSubmit, String legalName, String node) {
        this.dateSubmit = dateSubmit;
        this.dateReSubmit = dateReSubmit;
        this.packageName = packageName;
        this.legalName = legalName;
        this.templates = templates;

        chapterList = new ArrayList<Chapter>();
        
        Date date = new Date();
        
        String actualDate = date.toString();
        try {

            String fileName = packageName + "-" + Calendar.getInstance().getTimeInMillis();
            String filePath = root + fileName + ".pdf";
            Paragraph paragraph = new Paragraph("Custom message for package: " + packageName);
            HeaderFooterPageEvent event = new HeaderFooterPageEvent(packageName, actualDate, true);

            Document content = new Document(PageSize.LETTER);
            PdfWriter pdfWriterDoc = PdfWriter.getInstance(content, new FileOutputStream(filePath));
            HeaderFooterPageEvent event2 = new HeaderFooterPageEvent(packageName, actualDate, false);
            pdfWriterDoc.setBoxSize("art", new Rectangle(content.leftMargin(), content.rightMargin(), 600, 765));
            pdfWriterDoc.setPageEvent(event2);

            content.open();
            content = createFirstPage(content, paragraph, pdfWriterDoc);
            if (!node.equals("Track")) {
                createDoc(paragraph, event, node);
                content = createDocComplete(content, pdfWriterDoc, event);
            }
            content.close();
            pdfWriterDoc.close();

            return filePath;

        } catch (DocumentException e) {

            LOG.error("Error printing document");
            LOG.error(e);
            return Boolean.FALSE.toString();

        } catch (FileNotFoundException e) {

            LOG.error("Error printing document");
            LOG.error(e);
            return Boolean.FALSE.toString();

        }
    }

    private Document createFirstPage(Document content, Paragraph paragraph, PdfWriter pdfWriterDoc) {
        try {
            content = setHeader(content);
            content = setData(content, paragraph, packageName, legalName);
            content.newPage();
            //List<Map<String, Object>> tracking = trackingPrintingService.getApplicationMilestone(packageName);
            //addTimeLine(pdfWriterDoc.getDirectContent(), tracking);
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private Document createDocComplete(Document content, PdfWriter pdfWriterDoc, HeaderFooterPageEvent event) {
        try {
            Chapter indexChapter = new Chapter("Index", -1);
            indexChapter.setNumberDepth(-1);
            PdfPTable table = new PdfPTable(2);
            Integer indexSize = event.index.entrySet().size();
            Integer pageIndex = 1;
            if (indexSize > 45) {
                pageIndex = 2;
            }
            for (Map.Entry<String, Integer> index : event.index.entrySet()) {

                Chunk title = new Chunk(index.getKey() + "");
                PdfAction action = PdfAction.gotoLocalPage(index.getValue() + pageIndex, new PdfDestination(0), pdfWriterDoc);
                title.setAction(action);
                PdfPCell left = new PdfPCell(new Phrase(title));
                left.setBorder(Rectangle.NO_BORDER);

                Chunk pageNo = new Chunk(index.getValue() + pageIndex + "");
                pageNo.setAction(action);
                PdfPCell right = new PdfPCell(new Phrase(pageNo));
                right.setHorizontalAlignment(Element.ALIGN_RIGHT);
                right.setBorder(Rectangle.NO_BORDER);

                table.addCell(left);
                table.addCell(right);
            }
            indexChapter.add(table);
            content.add(indexChapter);

            for (Chapter c : chapterList) {
                content.add(c);
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return content;
    }

    private void createDoc(Paragraph paragraph, HeaderFooterPageEvent event, String node) {
        Document document = new Document(PageSize.LETTER);
        PdfWriter pdfWriter;
        List<Chapter> expComm = new ArrayList<Chapter>();
        try {
            pdfWriter = PdfWriter.getInstance(document, new ByteArrayOutputStream());

            pdfWriter.setBoxSize("art", new Rectangle(document.leftMargin(), document.rightMargin(), 600, 765));
            pdfWriter.setPageEvent(event);

            document.open();
            document = setHeader(document);
            document = setData(document, paragraph, packageName, legalName);

            document.newPage();
            //List<Map<String, Object>> tracking = trackingPrintingService.getApplicationMilestone(packageName);
            //addTimeLine(pdfWriter.getDirectContent(), tracking);

            Integer numberChapter = 0;

            Chapter chapter = null;
            Section subForm = null;
            Section section;
            BaseColor baseColor = new BaseColor(0, 0, 0);
            for (Map<String, Object> contentMap : templates) {

                String content = (String) contentMap.get("content");
                String formName = (String) contentMap.get("formName");
                String subFormName = (String) contentMap.get("subFormName");
                String sectionName = (String) contentMap.get("sectionName");
                String tittle = (String) contentMap.get("tittle");

                Boolean isForm = (Boolean) contentMap.get("isForm");
                Boolean isSubForm = (Boolean) contentMap.get("isSubForm");
                Boolean isSection = (Boolean) contentMap.get("isSection");
                Boolean commExp = (Boolean) contentMap.get("commExp");

                if (isForm) {
                    numberChapter++;
                    if (numberChapter > 1) {
                        document.add(chapter);
                        chapterList.add(chapter);
                    }
                    Chunk chapTitle = new Chunk(formName);
                    Font font = new Font(FontFamily.HELVETICA, 15, Font.BOLD, baseColor);
                    chapTitle.setFont(font);
                    chapter = new Chapter(new Paragraph(chapTitle), numberChapter);
                    chapTitle.setLocalDestination(chapter.getTitle().getContent());
                } else if (isSubForm) {
                    Chunk secTitle = new Chunk(subFormName);
                    Font font = new Font(FontFamily.HELVETICA, 13, Font.BOLD, baseColor);
                    secTitle.setFont(font);
                    subForm = chapter.addSection(new Paragraph(secTitle));
                    secTitle.setLocalDestination(subFormName);
                    if (subFormName.equals("Checklist")) {
                        Element contentParagraph = createParagraph(content);
                        subForm.add(contentParagraph);
                        document.add(chapter);
                        chapterList.add(chapter);
                    }
                } else if (isSection) {
                    Chunk secTitle = new Chunk(sectionName);
                    Font font = new Font(FontFamily.HELVETICA, 11, Font.BOLD, baseColor);
                    secTitle.setFont(font);
                    section = subForm.addSection(new Paragraph(secTitle));
                    secTitle.setLocalDestination(section.getTitle().getContent());
                    Element contentParagraph = createParagraph(content);
                    section.add(contentParagraph);
                } else if (commExp) {
                    numberChapter++;

                    Chunk chapTitle = new Chunk(tittle);
                    Font font = new Font(FontFamily.HELVETICA, 15, Font.BOLD, baseColor);
                    chapTitle.setFont(font);
                    Chapter chapterExp = new Chapter(new Paragraph(chapTitle), numberChapter);
                    chapTitle.setLocalDestination(chapterExp.getTitle().getContent());

                    Chunk secTitle = new Chunk(tittle);
                    Font fontSec = new Font(FontFamily.HELVETICA, 11, Font.BOLD, baseColor);
                    secTitle.setFont(fontSec);
                    section = chapterExp.addSection(new Paragraph(secTitle));
                    secTitle.setLocalDestination(section.getTitle().getContent());
                    Element contentParagraph = createParagraph(content);
                    section.add(contentParagraph);

                    expComm.add(chapterExp);
                }
            }
            if (node.equals("form") || node.equals("sub-form") || node.equals("section")) {
                document.add(chapter);
                chapterList.add(chapter);

            }
            for (Chapter chap : expComm) {
                document.add(chap);
                chapterList.add(chap);
            }
            document.close();
            pdfWriter.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Paragraph createParagraph(String content) {

        InputStream is = new ByteArrayInputStream(content.getBytes());
        Paragraph paragraph = new Paragraph();

        try {
            // CSS
            InputStream cssPathTest = getStyles().getInputStream();
            CSSResolver cssResolver = new StyleAttrCSSResolver();
            CssFile cssFileTest = XMLWorkerHelper.getCSS(cssPathTest);
            cssResolver.addCss(cssFileTest);
            // HTML
            HtmlPipelineContext htmlContext = new HtmlPipelineContext(null);
            htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());
            htmlContext.autoBookmark(false);
            // Pipelines
            ElementList elements = new ElementList();
            ElementHandlerPipeline end = new ElementHandlerPipeline(elements, null);
            HtmlPipeline html = new HtmlPipeline(htmlContext, end);
            CssResolverPipeline css = new CssResolverPipeline(cssResolver, html);
            // XML Worker
            XMLWorker worker = new XMLWorker(css, true);
            XMLParser parser = new XMLParser(worker);

            parser.parse(is);
            for (Element element : elements) {
                paragraph.add(element);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return paragraph;
    }

    public static Map<String, Color> getColors() {
        Map<String, Color> colors = new HashMap<String, Color>();
        colors.put("red_bar", new Color(212, 31, 37));
        colors.put("black_bar", new Color(74, 91, 113));
        colors.put("In Progress", new Color(15, 134, 214));
        colors.put("Submitted", new Color(74, 89, 112));
        colors.put("Under Review", new Color(135, 209, 253));
        colors.put("Return to Provider", new Color(255, 5, 5));
        colors.put("Resubmitted", new Color(97, 164, 224));
        colors.put("Approved", new Color(248, 132, 53));
        colors.put("Active", new Color(100, 132, 53));
        colors.put("Denied", new Color(255, 5, 5));
        colors.put("Corrections", new Color(15, 134, 214));
        colors.put("red_number", new Color(186, 62, 59));
        colors.put("Received By Medi-Cal", new Color(135, 209, 253));
        return colors;
    }

    public static void addTimeLine(PdfContentByte directContent, List<Map<String, Object>> tracking)
            throws DocumentException, IOException {
        Map<String, Color> colors = getColors();
        Date dateStart = (Date) tracking.get(0).get("DateTracking");
        Date dateEnd = (Date) tracking.get(tracking.size() - 1).get("DateTracking");
        String yearStart = new SimpleDateFormat("yyyy").format(dateStart);
        String yearEnd = new SimpleDateFormat("yyyy").format(dateEnd);
        directContent.setLineWidth(1f);
        addTextAtXY(yearStart, directContent, 70f, 510f);

        Color color = colors.get("black_bar");
        directContent.setRGBColorStroke(color.getRed(), color.getGreen(), color.getBlue());
        directContent.setRGBColorFill(color.getRed(), color.getGreen(), color.getBlue());

        directContent.rectangle(100, 500, 400, 30);
        directContent.closePathFillStroke();

        Color colorRed = colors.get("red_bar");
        directContent.setRGBColorStroke(colorRed.getRed(), colorRed.getGreen(), colorRed.getBlue());
        directContent.setRGBColorFill(colorRed.getRed(), colorRed.getGreen(), colorRed.getBlue());
        directContent.moveTo(100, 500);
        directContent.rectangle(100, 500, 400, 5);
        directContent.closePathFillStroke();
        addTextAtXY(yearEnd, directContent, 505f, 510f);
        Integer interLine = 400 / tracking.size();
        Integer cursor = 0;
        Integer index = 0;
        Boolean today = false;

        for (Map<String, Object> track : tracking) {
            Date dateTracking = (Date) track.get("DateTracking");
            String date = new SimpleDateFormat("MM/dd/yyyy").format(dateTracking);
            String milestone = (String) track.get("ApplicationMilestone");

            if (index == tracking.size() - 1) {
                today = true;
            }

            if (milestone.equals("Submitted") || milestone.equals("Resubmitted")) {
                elementTimeLine(directContent, 100 + cursor, 600, 0, 90, colors.get(milestone), milestone, date, today);
            } else {
                elementTimeLine(directContent, 100 + cursor, 560, 0, 40, colors.get(milestone), milestone, date, today);
            }

            cursor += interLine;
            index++;
        }
    }

    private static void elementTimeLine(PdfContentByte directContent, Integer x1, Integer y1, Integer deltaX,
                                        Integer deltaY, Color color, String milestone, String date, Boolean today)
            throws IOException, DocumentException {
        Map<String, Color> colors = getColors();
        Color colorBlack = colors.get("black_bar");
        directContent.setRGBColorStroke(colorBlack.getRed(), colorBlack.getGreen(), colorBlack.getBlue());
        directContent.setRGBColorFill(colorBlack.getRed(), colorBlack.getGreen(), colorBlack.getBlue());

        directContent.moveTo(x1, y1);
        directContent.lineTo(x1 - deltaX, y1 - deltaY);
        directContent.closePathFillStroke();

        directContent.setRGBColorStroke(color.getRed(), color.getGreen(), color.getBlue());
        directContent.setRGBColorFill(color.getRed(), color.getGreen(), color.getBlue());
        directContent.moveTo(x1, y1);
        directContent.lineTo(x1 + 5, y1 - 5);
        directContent.lineTo(x1, y1 - 10);
        directContent.closePathFillStroke();

        addTextAtXY(milestone, date, directContent, x1 + 7, y1);
        if (today) {
            Date dateNow = new Date();
            String now = new SimpleDateFormat("MM/dd/yyyy").format(dateNow);
            if (!date.equals(now)) {
                x1 = x1 + 10;
            }
            BaseFont labelFont = BaseFont.createFont(BaseFont.TIMES_ROMAN, "Cp1252", true);
            directContent.beginText();
            directContent.setColorFill(new BaseColor(0, 0, 0));
            directContent.setFontAndSize(labelFont, 7);
            directContent.setTextMatrix(x1 - deltaX - 10, y1 - deltaY - 32);
            directContent.showText("Today");
            directContent.endText();
            color = colors.get("red_bar");
            directContent.setRGBColorStroke(color.getRed(), color.getGreen(), color.getBlue());
            directContent.setRGBColorFill(color.getRed(), color.getGreen(), color.getBlue());
            directContent.moveTo(x1 - deltaX - 5, y1 - deltaY - 25);
            directContent.lineTo(x1 - deltaX - 5 + 5, y1 - deltaY - 25 + 5);
            directContent.lineTo(x1 - deltaX - 5 + 10, y1 - deltaY - 25);
            directContent.closePathFillStroke();
            directContent.setColorFill(new BaseColor(0, 0, 0));
        }
    }

    private Document setData(Document document, Paragraph paragraph, String appId, String legalName) throws DocumentException {
        document.addAuthor("Provider Portal");
        document.addCreator("Provider Portal");
        document.addSubject("Application Complete");
        document.addCreationDate();
        document.addTitle("Print Application");

        PdfPTable table = new PdfPTable(1);
        PdfPCell cell = new PdfPCell(paragraph);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        table.setSpacingAfter(200f);
        table.setSpacingBefore(200f);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        float[] columnWidths = new float[]{30f};
        table.setWidths(columnWidths);
        document.add(table);

        PdfPTable tableOne = new PdfPTable(2);
        PdfPCell cellOne = new PdfPCell(new Paragraph("Printing.coverPAge.AppId"));
        cellOne.setBorder(Rectangle.NO_BORDER);
        PdfPCell cellAppId = new PdfPCell(new Paragraph(appId));
        cellAppId.setBorder(Rectangle.NO_BORDER);
        tableOne.addCell(cellOne);
        tableOne.addCell(cellAppId);
        tableOne.setSpacingAfter(20f);
        document.add(tableOne);

        PdfPTable tableTwo = new PdfPTable(2);
        PdfPCell cellTwo = new PdfPCell(new Paragraph("Printing.coverPAge.legalName"));
        cellTwo.setBorder(Rectangle.NO_BORDER);
        PdfPCell cellLegalName = new PdfPCell(new Paragraph(legalName));
        cellLegalName.setBorder(Rectangle.NO_BORDER);
        tableTwo.addCell(cellTwo);
        tableTwo.addCell(cellLegalName);
        tableTwo.setSpacingAfter(20f);
        document.add(tableTwo);

        PdfPTable tableThree = new PdfPTable(2);
        PdfPCell cellThree = new PdfPCell(new Paragraph("Printing.coverPAge.submitDate"));
        cellThree.setBorder(Rectangle.NO_BORDER);
        PdfPCell cellDate = new PdfPCell(new Paragraph(dateSubmit));
        cellDate.setBorder(Rectangle.NO_BORDER);
        tableThree.addCell(cellThree);
        tableThree.addCell(cellDate);
        tableThree.setSpacingAfter(20f);
        document.add(tableThree);

        PdfPTable tableFour = new PdfPTable(2);
        PdfPCell cellFour = new PdfPCell(new Paragraph("Printing.coverPAge.resubmitDate"));
        cellFour.setBorder(Rectangle.NO_BORDER);
        PdfPCell cellDateResubmit = new PdfPCell(new Paragraph(dateReSubmit));
        cellDateResubmit.setBorder(Rectangle.NO_BORDER);
        tableFour.addCell(cellFour);
        tableFour.addCell(cellDateResubmit);
        document.add(tableFour);

        return document;
    }

    public static void addTextAtXY(String text, String date, PdfContentByte cb, float x, float y)
            throws IOException, DocumentException {
        BaseFont labelFont = BaseFont.createFont(BaseFont.TIMES_ROMAN, "Cp1252", true);
        cb.beginText();
        cb.setColorFill(new BaseColor(25, 50, 75));
        cb.setFontAndSize(labelFont, 7);
        cb.setTextMatrix(x, y);
        cb.showText(text);
        cb.endText();
        cb.beginText();
        cb.setColorFill(new BaseColor(105, 133, 168));
        cb.setFontAndSize(labelFont, 5);
        cb.setTextMatrix(x, y - 10);
        cb.showText(date);
        cb.endText();
    }

    public static void addTextAtXY(String text, PdfContentByte cb, float x, float y)
            throws IOException, DocumentException {
        BaseFont labelFont = BaseFont.createFont(BaseFont.TIMES_ROMAN, "Cp1252", true);
        cb.beginText();
        cb.setColorFill(new BaseColor(186, 62, 59));
        cb.setFontAndSize(labelFont, 12);
        cb.setTextMatrix(x, y);
        cb.showText(text);
        cb.endText();
    }

    private Document setHeader(Document document) throws DocumentException, IOException {
        PdfPTable table = new PdfPTable(3);
        //Image imageLeft = createImage("LEFT", 40f, 40f);
        //Image imageRight = createImage("RIGHT", 40f, 40f);

        String text = ("Printing.coverPAge.headerText");

        //PdfPCell cellOne = new PdfPCell(imageLeft);
        //cellOne.setBorder(Rectangle.NO_BORDER);
        //cellOne.setHorizontalAlignment(10);
        //cellOne.setPaddingLeft(75f);
        PdfPCell cellTwo = new PdfPCell(new Phrase(text));
        cellTwo.setBorder(Rectangle.NO_BORDER);
        cellTwo.setHorizontalAlignment(Element.ALIGN_CENTER);

        //PdfPCell cellThree = new PdfPCell(imageRight);
        //cellThree.setBorder(Rectangle.NO_BORDER);

        float[] columnWidths = new float[]{30f, 80f, 30f};
        table.setWidths(columnWidths);
        table.setWidthPercentage(100);
        //table.addCell(cellOne);
        table.addCell(cellTwo);
        //table.addCell(cellThree);

        document.add(table);

        return document;
    }

    private Image createImage(String position, Float with, Float height) throws BadElementException, IOException {
        Image image;
        URL imageUrl = getLeftLogo().getURL();

        if (position.equals("RIGHT")) {
            imageUrl = getRightLogo().getURL();
        }

        image = Image.getInstance(imageUrl);
        image.scaleAbsolute(with, height);

        return image;
    }

    public void initialize() {
        File rootFolder = new File(root);

        if (!rootFolder.exists()) {
            boolean isCreated = rootFolder.mkdir();

            if (!isCreated) {
                throw new UnsupportedOperationException("Unable to create the Root directory " + root + " for ExportApplicationStore");
            }

            return;
        }

        if (!rootFolder.isDirectory()) {
            throw new UnsupportedOperationException("Unable to initialize the ExportApplicationStore");
        }
    }

    public Resource getStyles() {
        return styles;
    }

    public void setStyles(Resource styles) {
        this.styles = styles;
    }

    public Resource getLeftLogo() {
        return leftLogo;
    }

    public void setLeftLogo(Resource leftLogo) {
        this.leftLogo = leftLogo;
    }

    public Resource getRightLogo() {
        return rightLogo;
    }

    public void setRightLogo(Resource rightLogo) {
        this.rightLogo = rightLogo;
    }
}
