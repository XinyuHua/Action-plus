package edu.sjtu.cs.action.main;

import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.runner.lemmatizeNewsRunner;
import edu.sjtu.cs.action.runner.parseNewsRunner;
import edu.sjtu.cs.action.runner.postagNewsRunner;
import edu.sjtu.cs.action.runner.tokenizeActionInNewsRunner;

/**
 * Created by xinyu on 6/17/2016.
 *
 * This class deals with preprocessing of news data, including
 * lemmatize, parsing(body), postagging(title, body)
 */
public class News {
    private static final String NEWS_URL = "dat/news/bing_news_sliced/";
    private static final String NEWS_BODY_PARSED_URL = "dat/news/bing_news_sliced_parsed/";
    private static final String NEWS_BODY_POSTAG_URL = "dat/news/bing_news_sliced_postag/";
    private static final String NEWS_LEMMA_URL = "dat/news/bing_news_sliced_lemma/";
    private static final String NEWS_TITLE_POSTAG_URL = "dat/news/bing_news_sliced_title_postag/";
    private static final String NEWS_AC_TOKENIZE_URL = "dat/news/bing_news_sliced_action_tokenized/";
    private static final String AC_URL = "dat/action/concept_extracted/yu_10/";
    private int lowerBound;
    private int upperBound;
    private int size;
    private int offset;

    public News(int l, int u, int o){
        this.lowerBound = l;
        this.upperBound = u;
        size = upperBound - lowerBound;
        this.offset = o;
    }

    public void lemmatizeNews() throws Exception {
        int fileBase = lowerBound;

        Runnable[] lNA = new Runnable[size];
        for(int i = 0; i < size; ++i) {
            lNA[ i ] = new lemmatizeNewsRunner(fileBase + i, offset, NEWS_URL, NEWS_LEMMA_URL);
        }
        Thread[] tdA = new Thread[size];
        for(int i = 0; i < size; ++i){
            tdA[ i ] = new Thread(lNA[ i ]);
            tdA[ i ].start();
        }

        for(int i = 0; i < size; ++i){
            tdA[ i ].join();
        }
        return;
    }

    public void parseAndPostagBody()throws Exception{

        Runnable[] pNA = new Runnable[size];
        for(int i = 0; i < size; ++i){
            pNA[ i ] = new parseNewsRunner(lowerBound + i, offset, NEWS_URL, NEWS_BODY_PARSED_URL, NEWS_BODY_POSTAG_URL);
        }

        Thread[] tdA = new Thread[size];
        for(int i = 0; i < size; ++i){
            tdA[ i ] = new Thread(pNA[ i ]);
            tdA[ i ].start();
        }

        for(int i = 0; i < size; ++i){
            tdA[ i ].join();
        }
        return;
    }

    public void postagTitle()throws Exception{

        Runnable[] pNA = new Runnable[size];
        for(int i = 0; i < size; ++i){
            pNA[ i ] = new postagNewsRunner(lowerBound + i, offset, NEWS_URL, NEWS_TITLE_POSTAG_URL);
        }

        Thread[] tdA = new Thread[size];
        for(int i = 0; i < size; ++i){
            tdA[ i ] = new Thread(pNA[ i ]);
            tdA[ i ].start();
        }

        for(int i = 0; i < size; ++i){
            tdA[ i ].join();
        }
        return;
    }

    public void tokenizeAction() throws Exception{
        Runnable[] pNA = new Runnable[size];
        ProbaseClient[] pcs = new ProbaseClient[size];
        Wordnet wn = new Wordnet(false);
        for(int i = 0; i < size; ++i){
            pcs[ i ] = new ProbaseClient(4500 + i);
        }
        for(int i = 0; i < size; ++i){
            pNA[ i ] = new tokenizeActionInNewsRunner(lowerBound + i, offset,
                    NEWS_LEMMA_URL, AC_URL, NEWS_AC_TOKENIZE_URL, pcs[ i ], wn);
        }

        Thread[] tdA = new Thread[size];
        for(int i = 0; i < size; ++i){
            tdA[ i ] = new Thread(pNA[ i ]);
            tdA[ i ].start();
        }

        for(int i = 0; i < size; ++i){
            tdA[ i ].join();
        }

        return;
    }
}
