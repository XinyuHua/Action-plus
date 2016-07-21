package edu.sjtu.cs.action.main;

import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;

import java.io.*;
import java.util.*;

/**
 * Created by xinyu on 5/3/2016.
 */
public class getVerbOccurrence implements Runnable {
    private static String OUT_PATH = "dat/instance_occurrence_tmp/";
    final private static String BING_NEWS_PARSED_URL = "dat/news/bing_news_sliced_parsed/";
    final private static String BING_NEWS_POSTAG_URL = "dat/news/bing_news_sliced_postag/";

    final private static String[] VERB_TAG = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};

    private static Wordnet wn;
    private static List<String> puncList;
    private static List<String> personPronList;
    private static List<String> thingPronList;
    private static List<Set<String>> inflectionList;
    private static List<String> verbList;
    private static HashSet<String> verbTagSet;
    private static List<String> specialList;
    private int part;
    private boolean debug;
    private int offset;
    public getVerbOccurrence( int part, ProbaseClient pb, Wordnet wn, List<String> vl,  List<Set<String>> il,
                                  List<String> pl,  String outPath, int offset){

        this.wn = wn;
        this.verbList = vl;
        this.inflectionList = il;
        this.puncList = pl;
        this.part = part;
        this.OUT_PATH = outPath;

        verbTagSet = new HashSet<String>();
        for(String s : VERB_TAG){
            verbTagSet.add(s);
        }
        this.offset = offset;
    }

    public void run(){
        File fileToWrite = new File(OUT_PATH + part + ".txt");

    }

    private static List<String> getVerbFromPostag(String postagged) throws Exception {
        List<String> result = new ArrayList<String>();
        String[] posStr = postagged.trim().replaceAll("\\[|\\]", "").split(",\\s+");
        for (String pos : posStr) {
            pos = pos.trim();
            String sp[] = pos.split("/");
            String token = sp[0];
            String TAG = sp[1];

        }
        return result;

    }
}
