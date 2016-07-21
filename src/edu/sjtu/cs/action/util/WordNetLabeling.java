package edu.sjtu.cs.action.util;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.FieldAccessor_Integer;
import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.stanford.nlp.ling.Word;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by xinyu on 6/10/2016.
 */
public class WordNetLabeling {
    private static final String AC_INVENTORY_URL = "dat/action/concept_for_eval.txt";
    private static final String OUTPUT_URL = "dat/groundTruth/output_xinyu.txt";
    private static final String GLOSS_URL = "dat/evaluation/gloss/";
    private static final String NOUN_URL = "dat/evaluation/noun_in_gloss/";
    private static Wordnet wn;
    private static HashSet<String> actionSet;
    private static Postag tagger;

    public static void main(String[] args)throws Exception{
        label();
        //generateLabelingDate();
    }

    private static void generateLabelingDate()throws Exception{
        loadAction();
        tagger = new Postag();
        wn = new Wordnet(false);
        for(String ac : actionSet){
            System.out.println(ac);
            BufferedWriter bw_g = new BufferedWriter(new FileWriter(GLOSS_URL + ac + ".txt"));
            BufferedWriter bw_n = new BufferedWriter(new FileWriter(NOUN_URL + ac + ".txt"));
            String verb = ac.split("_")[1];
            List<String> glosses = wn.getVerbGloss(verb);
            for(String g : glosses){
                bw_g.append(g);
                bw_g.newLine();
                List<String> nouns = getNounFromGloss(g);
                for(String noun : nouns){
                    bw_n.append(noun + "\t");
                }
                bw_n.newLine();
            }
            bw_g.close();
            bw_n.close();
        }
    }

    private static void label()throws Exception{
        BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_URL));
        tagger = new Postag();
        loadAction();
        wn = new Wordnet(false);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for(String ac : actionSet){
            System.out.println("Want to label an action for me?(enter to continue, Q to quit)");
            String response = br.readLine().trim();
            if(response.equals("Q")){
                break;
            }
            System.out.println(ac);
            String verb = ac.split("_")[1];

            List<String> glosses = wn.getVerbGloss(verb);
            int i = 0;
            for(String s : glosses){
                System.out.println(i + "\t" + s);
                i++;
            }
            System.out.println("Enter the index of the sense(s) you think is correct for " + verb + " in " + ac + "(split by ,):");
            String chosen = br.readLine().trim();
            String[] chosenSplit = chosen.split(",");
            List<String> nouns = getNounFromGlosses(glosses, chosenSplit);
            i = 0;
            for(String n : nouns){
                System.out.print(i + ":" + n + "\t");
                i++;
            }

            System.out.println();
            System.out.print("Enter the index of noun you regard as good abstraction for this action(" + ac + "):");
            chosen = br.readLine();
            if(!chosen.trim().equals("")){
                chosenSplit = chosen.split(",");
                bw.append(ac + ":");
                for(String c : chosenSplit){
                    bw.append(nouns.get(Integer.parseInt(c)) + "\t");
                }
            }

            bw.newLine();
            bw.flush();
            System.out.println("------------------");
        }
        bw.close();
    }

    private static List<String> getNounFromGloss(String gloss)throws Exception{
        List<String> tags = tagger.postagDocument(gloss);
        List<String> result = new ArrayList<>();
        for(String t : tags){
            String[] splited = t.split("_");
            if(splited[1].equals("NN") || splited[1].equals("NNS") || splited[1].equals("NNPS") || splited[1].equals("NNP")){
                result.add(splited[0]);
            }
        }
        return result;
    }

    private static List<String> getNounFromGlosses(List<String> gloss, String[] chosen)throws Exception{
        List<String> result = new ArrayList<>();
        for(String s : chosen){
            String doc = gloss.get(Integer.parseInt(s));
            List<String> tags = tagger.postagDocument(doc);
            for(String t : tags){
                String[] splited = t.split("_");
                if(splited[1].equals("NN") || splited[1].equals("NNS") || splited[1].equals("NNPS") || splited[1].equals("NNP")){
                    result.add(splited[0]);
                }
            }
        }
        return result;
    }

    private static void loadAction()throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(AC_INVENTORY_URL));
        String line;
        actionSet = new HashSet<>();
        while((line = br.readLine())!=null){
            actionSet.add(line);
        }
        br.close();
    }

}
