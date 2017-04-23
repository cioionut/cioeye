package com.cioeye;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Files;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;

class Indexer {

    private IndexWriter writer;

    Indexer(Directory indexDirectory, RoAnalyzer analyzer) throws IOException {
        //this directory will contain the indexes
        //create the indexer
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(indexDirectory, config);
    }

    void close() throws IOException {
        writer.close();
    }

    private static String readFileString(File file) {
        // ref: http://makble.com/how-to-do-lucene-search-highlight-example
        Path filePath = file.toPath();
        try {
            String content_type = Files.probeContentType(filePath);
            if (content_type.contains("application/pdf")) {
                return getTextFromPdf(file);
            }
            if (content_type.contains("text/plain")) {
                return getTextFromTxt(file);
            }
            if (content_type.contains("text/html")) {
                return getTextFromHtml(file);
            }


        } catch (IOException e) {
            // e.printStackTrace();
        }
        return "";
    }

    private Document getDocument(File file) throws IOException {
        Document document = new Document();
        //index file contents
        FieldType type = new FieldType();
        type.setStored(true);
        type.setStoreTermVectors(true);
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        Field contentField = new Field(
                LuceneConstants.CONTENTS,
                readFileString(file),
                type);
//        TextField contentField = new TextField(
//                LuceneConstants.CONTENTS,
//                readFileString(file.getCanonicalPath()),
//                Field.Store.YES);
        //index file name
        StringField fileNameField = new StringField(
                LuceneConstants.FILE_NAME,
                file.getName(),
                Field.Store.YES);
        //index file path
        StringField filePathField = new StringField(
                LuceneConstants.FILE_PATH,
                file.getCanonicalPath(),
                Field.Store.YES);

        document.add(contentField);
        document.add(fileNameField);
        document.add(filePathField);

        return document;
    }

    private void indexFile(File file) throws IOException {
        System.out.println("Indexing "+file.getCanonicalPath());
        Document document = getDocument(file);
        writer.addDocument(document);
    }

    int createIndex(String dataDirPath, FileFilter filter)
            throws IOException, NullPointerException {
        //get all files in the data directory
        File[] files = new File(dataDirPath).listFiles();

        if (files != null) {
            for (File file : files) {
                if(!file.isDirectory()
                        && !file.isHidden()
                        && file.exists()
                        && file.canRead()
                        && filter.accept(file)
                        ){
                    indexFile(file);
                }
            }
        }
        return writer.numDocs();
    }

    static String getTextFromPdf(File pdfFile) throws IOException {
        PDDocument doc = PDDocument.load(pdfFile);
        return new PDFTextStripper().getText(doc);
    }
    static String getTextFromTxt(File txtFile) throws IOException {
        // ref: http://makble.com/how-to-do-lucene-search-highlight-example
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    new FileInputStream(txtFile), "UTF8"));
            String line;
            while ((line = in.readLine()) != null) {
                text.append(line).append("\r\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    static String getTextFromHtml(File file) throws IOException {
        String html = getTextFromTxt(file);
        return Jsoup.parse(html).text();
    }
}