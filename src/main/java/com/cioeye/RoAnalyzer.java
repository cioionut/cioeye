package com.cioeye;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;


public class RoAnalyzer extends Analyzer {
    private String language;
    private Set<String> stopWords;


    public RoAnalyzer(String language) {
        this.setStopWords();
        this.language = language;
    }

    RoAnalyzer() {
        this.setStopWords();
        this.language = "Romanian";
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new StandardTokenizer();
        TokenStream result = new StandardFilter(source);

        result = new LowerCaseFilter(result);
        result = new StopFilter(result, CharArraySet.copy(stopWords));
        result = new SnowballFilter(result, language);

        return new TokenStreamComponents(source, result);
    }

    private static String removeDiacritics(String text) {
        // from https://www.drillio.com/en/2011/java-remove-accent-diacritic/
        return text == null ? null :
                Normalizer.normalize(text, Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private void setStopWords() {
        stopWords = new HashSet<>();
        for (Object word : RomanianAnalyzer.getDefaultStopSet()) {
            char[] stopWord = (char[]) word;
            String stopword = new String(stopWord);
            stopWords.add(stopword);
            stopWords.add(removeDiacritics(stopword));
        }
    }
}
