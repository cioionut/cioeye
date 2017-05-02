package com.cioeye;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.TermsQuery;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryTermExtractor;
import org.apache.lucene.search.highlight.WeightedTerm;
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
                Map<String, Object> queryStats = getQueryStats(search_text, searcher);
                ArrayList<Object> searchResults = search(search_text, searcher);
                map.put("searchResults", searchResults);
                map.put("queryStats", queryStats);
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
                indexer.close();
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
            // String[] terms_list = searchQuery.split("\\s+");
            Map<String, String> termToFreq = getTFIDF(searchQuery, scoreDoc.doc, searcher);
            String[] fragments = searcher.getHlFragments(searchQuery, new RoAnalyzer(), doc);
            System.out.println("File: "
                    + doc.get(LuceneConstants.FILE_PATH));
            StringBuilder hlfrag = new StringBuilder();
            for(String frag : fragments) {
                System.out.printf(frag + '\n');
                hlfrag.append(frag).append('\n');
            }
            Map<String, Object> doc_map = new HashMap<>();
            doc_map.put("title", doc.get(LuceneConstants.FILE_NAME));
            doc_map.put("hlfrag", hlfrag.toString());
            doc_map.put("score", String.format("%.3g%n", scoreDoc.score));
            doc_map.put("tf", termToFreq);

            doc_list.add(doc_map);
        }
        return doc_list;
    }

    private static Map<String, String> getTFIDF(String searchQuery, int nrdoc, Searcher searcher)
            throws IOException, ParseException, InvalidTokenOffsetsException {

        Map<String, Long> termToFreq = new HashMap<>();
        Terms termVector = searcher.indexSearcher.getIndexReader().getTermVector(nrdoc, LuceneConstants.CONTENTS);

        TermsEnum itr = termVector.iterator();
        BytesRef termref = null;

        while ((termref = itr.next()) != null) {
            String termText = termref.utf8ToString();
            long termFreq = itr.totalTermFreq();
            termToFreq.put(termText, termFreq);
            // long docCount = itr.docFreq();
            // System.out.println("term: "+termText+", termFreq = "+termFreq+", docCount = "+docCount);
        }
        String[] queryWords = searchQuery.split(" ");
        Map<String, String> qtermToTfIdf = new HashMap<>();
        for (String word : queryWords) {
            Query query = searcher.queryParser.parse(word);
            WeightedTerm[] wterms = QueryTermExtractor.getTerms(query);
            for (WeightedTerm wterm : wterms) {
                String sterm = wterm.getTerm();
                Term termInstance = new Term(LuceneConstants.CONTENTS, sterm);
                long df = (long) searcher.indexSearcher.getIndexReader().docFreq(termInstance);
                long N = searcher.indexSearcher.getIndexReader().numDocs();
                double idf = 0;
                if (df != 0) {
                    idf = Math.log(N / df);
                }
                if (termToFreq.containsKey(sterm)) {
                    double tf_idf = termToFreq.get(sterm) * idf;
                    qtermToTfIdf.put(word, String.format("%.3g%n", tf_idf));
                }
            }
        }
        return qtermToTfIdf;
    }

    private static Map<String, Object> getQueryStats(String searchQuery, Searcher searcher)
            throws IOException, ParseException, InvalidTokenOffsetsException {
        String[] queryWords = searchQuery.split(" ");
        Map<String, Object> qtermToStats = new HashMap<>();

        for (String word : queryWords) {
            Query query = searcher.queryParser.parse(word);
            WeightedTerm[] wterms = QueryTermExtractor.getTerms(query);
            for (WeightedTerm wterm : wterms) {
                Map<String, String> qtermFreq = new HashMap<>();
                String sterm = wterm.getTerm();
                Term termInstance = new Term(LuceneConstants.CONTENTS, sterm);
                // qtermFreq.put("TF",
                //        searcher.indexSearcher.getIndexReader().totalTermFreq(termInstance));
                long df = (long) searcher.indexSearcher.getIndexReader().docFreq(termInstance);
                long N = searcher.indexSearcher.getIndexReader().numDocs();
                double idf = 0;
                if (df != 0) {
                    idf = Math.log(N / df);
                }
                qtermFreq.put("IDF", String.format("%.3g%n", idf));
                qtermToStats.put(word, qtermFreq);
            }
        }

        return qtermToStats;
    }
}

