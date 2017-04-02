package com.cioeye;

import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import spark.ModelAndView;
import spark.template.jade.JadeTemplateEngine;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.staticFiles;

public class SparkServer {
    private static final String indexDirPath = "/home/ionut/workspace/cioeye/index";
    private static final String dataDir = "/home/ionut/workspace/cioeye/data";

    public static void main(String[] args) {
        staticFiles.location("/public"); // Static files
        port(8080);

        Path indexPath = FileSystems.getDefault().getPath(indexDirPath);
        RoAnalyzer analyzer = new RoAnalyzer();

        Map<String, String> map = new HashMap<>();
        map.put("search_label", "Caută");
        map.put("title", "CioEye");

        // The index.jade template file is in the resources/templates directory
        get("/", (req, res) -> new ModelAndView(map, "index"), new JadeTemplateEngine());
        get("/search/:querystr", (req, res) -> {
            try {
                Directory indexDir = FSDirectory.open(indexPath);
                IndexReader reader = DirectoryReader.open(indexDir);
                Searcher searcher = new Searcher(reader, analyzer);

                search(req.params(":querystr"), searcher);
                reader.close();
            } catch (IOException | ParseException | InvalidTokenOffsetsException e) {
                e.printStackTrace();
            }
            return new ModelAndView(map, "index");
        }, new JadeTemplateEngine());

        // create index
        get("/createindex", (req, res) -> {
            try {
                Directory indexDir = FSDirectory.open(indexPath);
                Indexer indexer = new Indexer(indexDir, analyzer);
                createIndex(indexer);
            } catch (IOException err) {
                err.printStackTrace();
            }
            return new ModelAndView(map, "index");
        }, new JadeTemplateEngine());
    }

    private static void createIndex(Indexer indexer) throws IOException{
        int numIndexed;
        long startTime = System.currentTimeMillis();
        numIndexed = indexer.createIndex(dataDir, new TextFileFilter());
        long endTime = System.currentTimeMillis();
        indexer.close();
        System.out.println(numIndexed+" File indexed, time taken: "
                +(endTime-startTime)+" ms");
    }

    public static void search(String searchQuery, Searcher searcher)
            throws IOException, ParseException, InvalidTokenOffsetsException {
        long startTime = System.currentTimeMillis();

        TopDocs hits = searcher.search(searchQuery);

        long endTime = System.currentTimeMillis();

        System.out.println(hits.totalHits +
                " documents found. Time :" + (endTime - startTime));
        for(ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            String[] fragments = searcher.getHlFragments(searchQuery, new RoAnalyzer(), doc);
            System.out.println("File: "
                    + doc.get(LuceneConstants.FILE_PATH));
            for(String frag : fragments) {
                System.out.printf(frag + '\n');
            }
        }
    }
}
