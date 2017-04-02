package com.cioeye;
import java.io.IOException;

import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;

class Searcher {

    private IndexSearcher indexSearcher;
    public QueryParser queryParser;

    Searcher(IndexReader reader, RoAnalyzer analyzer) throws IOException {
        indexSearcher = new IndexSearcher(reader);
        queryParser = new QueryParser(LuceneConstants.CONTENTS, analyzer);
    }

    TopDocs search(String searchQuery) throws IOException, ParseException {
        Query query = queryParser.parse(searchQuery);
        return indexSearcher.search(query, LuceneConstants.MAX_SEARCH);
    }

    String[] getHlFragments(String searchQuery, RoAnalyzer analyzer, Document doc)
            throws IOException, ParseException, InvalidTokenOffsetsException {
        Query query = queryParser.parse(searchQuery);
        SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
        Highlighter highlighter = new Highlighter(htmlFormatter,
                new QueryScorer(query));
        return highlighter.getBestFragments(analyzer, LuceneConstants.CONTENTS,
                doc.get(LuceneConstants.CONTENTS), 4);
    }

    Document getDocument(ScoreDoc scoreDoc) throws IOException{
        return indexSearcher.doc(scoreDoc.doc);
    }
}