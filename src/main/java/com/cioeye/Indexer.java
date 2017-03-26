package com.cioeye;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

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

    private Document getDocument(File file) throws IOException {
        Document document = new Document();

        //index file contents
        TextField contentField = new TextField(
                LuceneConstants.CONTENTS,
                new FileReader(file));
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
}