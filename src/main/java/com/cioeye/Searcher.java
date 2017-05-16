package com.cioeye;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.LongRangeField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;


class Searcher {

    public IndexSearcher indexSearcher;
    public QueryParser queryParser;

    Searcher(IndexReader reader, RoAnalyzer analyzer) throws IOException {
        indexSearcher = new IndexSearcher(reader);
        queryParser = new QueryParser(LuceneConstants.CONTENTS, analyzer);
    }

    TopDocs search(String searchQuery, long start_date, long end_date) throws IOException, ParseException {
        Query search_query = queryParser.parse(searchQuery);
        Query range_query = LongPoint.newRangeQuery(LuceneConstants.FILE_MODIFIED, start_date, end_date);
        BooleanQuery.Builder boolean_q_bilder = new BooleanQuery.Builder();
        boolean_q_bilder.add(search_query, BooleanClause.Occur.MUST);
        boolean_q_bilder.add(range_query, BooleanClause.Occur.MUST);
        BooleanQuery bool_q = boolean_q_bilder.build();
        return indexSearcher.search(bool_q, LuceneConstants.MAX_SEARCH);
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