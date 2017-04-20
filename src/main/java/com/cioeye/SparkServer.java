package com.cioeye;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import spark.ModelAndView;
import spark.template.jade.JadeTemplateEngine;

import javax.print.Doc;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
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

        Map<String, Object> map = new HashMap<>();
        map.put("search_label", "Caută");
        map.put("title", "CioEye");

        // The index.jade template file is in the resources/templates directory
        get("/", (req, res) -> {
            map.remove("searchResult");
            return new ModelAndView(map, "index");
        }, new JadeTemplateEngine());

        get("/search", (req, res) -> {
            try {
                Directory indexDir = FSDirectory.open(indexPath);
                IndexReader reader = DirectoryReader.open(indexDir);
                Searcher searcher = new Searcher(reader, analyzer);

                String search_text = req.queryParams("querystr");
                ArrayList<Object> searchResults = search(search_text, searcher);
                map.put("searchResults", searchResults);
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

    public static ArrayList<Object> search(String searchQuery, Searcher searcher)
            throws IOException, ParseException, InvalidTokenOffsetsException {

        long startTime = System.currentTimeMillis();
        TopDocs hits = searcher.search(searchQuery);
        long endTime = System.currentTimeMillis();

        ArrayList<Object> doc_list = new ArrayList<>();

        System.out.println(hits.totalHits +
                " documents found. Time :" + (endTime - startTime));
        for(ScoreDoc scoreDoc : hits.scoreDocs) {
            System.out.println(scoreDoc.score);
            Document doc = searcher.getDocument(scoreDoc);
            String[] terms_list = searchQuery.split("\\s+");
//            for (String term : terms_list) {
//                long tf = getTF(term, scoreDoc.doc);
//            }
            String[] fragments = searcher.getHlFragments(searchQuery, new RoAnalyzer(), doc);
            System.out.println("File: "
                    + doc.get(LuceneConstants.FILE_PATH));
            StringBuilder hlfrag = new StringBuilder();
            for(String frag : fragments) {
                System.out.printf(frag + '\n');
                hlfrag.append(frag).append('\n');
            }
            Map<String, String> doc_map = new HashMap<>();
            doc_map.put("title", LuceneConstants.FILE_NAME);
            doc_map.put("hlfrag", hlfrag.toString());
            doc_map.put("score", String.format("%.3g%n", scoreDoc.score));

            doc_list.add(doc_map);
        }
        return doc_list;
    }

    public static long getTF(String term, int nrdoc) {

        Term termInstance = new Term(LuceneConstants.CONTENTS, term);
        BytesRef qterm = termInstance.bytes();
        long termfreq = 0;
        try {
            Path indexPath = FileSystems.getDefault().getPath(indexDirPath);
            Directory indexDir = FSDirectory.open(indexPath);
            IndexReader reader = DirectoryReader.open(indexDir);

            Terms termVector = reader.getTermVector(nrdoc, "contents");
            TermsEnum itr = termVector.iterator();
            BytesRef termref = null;

            while ((termref = itr.next()) != null &&
                    qterm.bytesEquals(termref)) {
                String termText = termref.utf8ToString();
                long termFreq = itr.totalTermFreq();
                long docCount = itr.docFreq();

                System.out.println("term: "+termText+", termFreq = "+termFreq+", docCount = "+docCount);
            }
            reader.close();
        } catch (IOException err) {
            err.printStackTrace();
        }

        return termfreq;
    }
}
