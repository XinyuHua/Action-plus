package edu.sjtu.cs.action.util;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xinyu on 6/10/2016.
 */
public class Postag {

    private static MaxentTagger tagger;

    public static void main(String[] args)throws Exception{
        Postag pos = new Postag();
        String doc = "This is a document. Try to postag it!";
        List<String> posResult = pos.postagDocument(doc);
        for(String s : posResult){
            System.out.println(s);
        }
    }

    public Postag()throws Exception{
        tagger = new MaxentTagger("lib/english-left3words-distsim.tagger");
    }

    public static List<String> postagDocument(String document) throws Exception{
        String tagged = tagger.tagString(document);
        List<String> result = new ArrayList<>();
        for(String s : tagged.split("\\s+")){
            result.add(s);
        }
        return result;
    }
}
