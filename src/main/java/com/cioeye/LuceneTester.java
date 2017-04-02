package com.cioeye;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import com.sun.xml.internal.fastinfoset.util.CharArray;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

public class LuceneTester {

    private static final String indexDirPath = "/home/ionut/workspace/cioeye/index";
    private static final String dataDir = "/home/ionut/workspace/cioeye/data";
    private Directory indexDir;
    private Indexer indexer;
    private Searcher searcher;
    private RoAnalyzer analyzer;

    public static void main(String[] args) {
        LuceneTester tester;
        try {
            tester = new LuceneTester();
            tester.buildAnalyzer();
            tester.createIndex();

            String querystr = args.length > 0 ? args[0] : "lucene";
            querystr = "camasa";
            tester.search(querystr);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

//    private static String removeDiacritics(String text) {
//        // from https://www.drillio.com/en/2011/java-remove-accent-diacritic/
//        return text == null ? null :
//                Normalizer.normalize(text, Form.NFD)
//                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
//    }

    private void buildAnalyzer() {
        analyzer = new RoAnalyzer();
//        Set<String> stopWords = new HashSet<>();
//        for (Object word : RomanianAnalyzer.getDefaultStopSet()) {
//            char[] stopWord = (char[]) word;
//            String stopword = new String(stopWord);
//            stopWords.add(stopword);
//            stopWords.add(removeDiacritics(stopword));
//        }
//        CharArraySet stopSet = CharArraySet.copy(stopWords);
//        analyzer = new RomanianAnalyzer(stopSet);
    }

    private void createIndex() throws IOException{
//        Path indexPath = FileSystems.getDefault().getPath(indexDirPath);
//        indexDir =
//        //        FSDirectory.open(indexPath);
//                  new RAMDirectory();
//
//        indexer = new Indexer(indexDir, analyzer);
//        int numIndexed;
//        long startTime = System.currentTimeMillis();
//        numIndexed = indexer.createIndex(dataDir, new TextFileFilter());
//        long endTime = System.currentTimeMillis();
//        indexer.close();
//        System.out.println(numIndexed+" File indexed, time taken: "
//                +(endTime-startTime)+" ms");
    }

    private void search(String searchQuery) throws IOException, ParseException {
//        IndexReader reader = DirectoryReader.open(indexDir);
//        searcher = new Searcher(reader, analyzer);
//
//        long startTime = System.currentTimeMillis();
//
//        TopDocs hits = searcher.search(searchQuery);
//        long endTime = System.currentTimeMillis();
//
//        System.out.println(hits.totalHits +
//                " documents found. Time :" + (endTime - startTime));
//        for(ScoreDoc scoreDoc : hits.scoreDocs) {
//            Document doc = searcher.getDocument(scoreDoc);
//            System.out.println("File: "
//                    + doc.get(LuceneConstants.FILE_PATH));
//        }
//        reader.close();
    }
}