package com.merkoffice.app;

import android.util.Xml;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Basic offline DOCX import/export. Supports paragraphs and common inline text formatting. */
public final class DocxHelper {
    private DocxHelper() {}

    public static String read(InputStream input) throws Exception {
        String documentXml = null;
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192]; int count;
                    while ((count = zip.read(buffer)) != -1) out.write(buffer, 0, count);
                    documentXml = out.toString("UTF-8");
                    break;
                }
            }
        }
        if (documentXml == null) throw new IOException("This is not a valid DOCX document.");
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(documentXml));
        StringBuilder html = new StringBuilder();
        StringBuilder paragraph = null;
        boolean inText = false, bold = false, italic = false, underline = false;
        int event;
        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if ("p".equals(name)) paragraph = new StringBuilder();
                if ("rPr".equals(name)) { bold = false; italic = false; underline = false; }
                if ("b".equals(name)) bold = true;
                if ("i".equals(name)) italic = true;
                if ("u".equals(name)) underline = true;
                if ("t".equals(name)) inText = true;
            } else if ((event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT) && inText && paragraph != null) {
                String value = escape(parser.getText());
                if (bold) value = "<strong>" + value + "</strong>";
                if (italic) value = "<em>" + value + "</em>";
                if (underline) value = "<u>" + value + "</u>";
                paragraph.append(value);
            } else if (event == XmlPullParser.END_TAG) {
                String name = parser.getName();
                if ("t".equals(name)) inText = false;
                if ("p".equals(name) && paragraph != null) {
                    html.append("<p>").append(paragraph.length() == 0 ? "<br>" : paragraph).append("</p>");
                    paragraph = null;
                }
            }
        }
        return html.length() == 0 ? "<p><br></p>" : html.toString();
    }

    public static void write(String html, OutputStream output) throws Exception {
        org.jsoup.nodes.Document parsed = Jsoup.parseBodyFragment(html == null ? "" : html);
        StringBuilder body = new StringBuilder();
        for (Node node : parsed.body().childNodes()) appendBlock(node, body);
        if (body.length() == 0) body.append("<w:p/>");
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>" +
            body + "<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/></w:sectPr></w:body></w:document>";
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            put(zip, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
            put(zip, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
            put(zip, "word/document.xml", xml);
        }
    }

    private static void appendBlock(Node node, StringBuilder out) {
        if (node instanceof TextNode) {
            String text = ((TextNode) node).text();
            if (!text.trim().isEmpty()) out.append("<w:p><w:r><w:t xml:space=\"preserve\">").append(escape(text)).append("</w:t></w:r></w:p>");
        } else if (node instanceof Element) {
            Element el = (Element) node;
            if (el.tagName().matches("p|div|h1|h2|h3|li|blockquote")) {
                out.append("<w:p>");
                if (el.tagName().startsWith("h")) out.append("<w:pPr><w:pStyle w:val=\"Heading").append(el.tagName().substring(1)).append("\"/></w:pPr>");
                appendRuns(el, out, false, false, false);
                out.append("</w:p>");
            } else {
                for (Node child : el.childNodes()) appendBlock(child, out);
            }
        }
    }

    private static void appendRuns(Node node, StringBuilder out, boolean bold, boolean italic, boolean underline) {
        if (node instanceof TextNode) {
            String text = ((TextNode) node).getWholeText();
            if (!text.isEmpty()) {
                out.append("<w:r>");
                if (bold || italic || underline) {
                    out.append("<w:rPr>");
                    if (bold) out.append("<w:b/>");
                    if (italic) out.append("<w:i/>");
                    if (underline) out.append("<w:u w:val=\"single\"/>");
                    out.append("</w:rPr>");
                }
                out.append("<w:t xml:space=\"preserve\">").append(escape(text)).append("</w:t></w:r>");
            }
        } else if (node instanceof Element) {
            Element el = (Element) node;
            boolean b = bold || el.tagName().equals("b") || el.tagName().equals("strong");
            boolean i = italic || el.tagName().equals("i") || el.tagName().equals("em");
            boolean u = underline || el.tagName().equals("u");
            for (Node child : el.childNodes()) appendRuns(child, out, b, i, u);
        }
    }

    private static void put(ZipOutputStream zip, String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&apos;");
    }
}