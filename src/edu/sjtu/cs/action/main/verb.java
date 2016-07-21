package edu.sjtu.cs.action.main;

import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.util.ValueComparatorInt;
import java.io.*;
import java.util.*;

/**
 * Created by xinyu on 4/5/2016.
 */
public class Verb {

    final private static String POSTAGGED = "dat/news/bing_news_sliced_postag/";
    final private static String VERB = "dat/action/verb/";
    final private static String YU_DAT = "dat/argument/yu_result/";
    final private static String YU_10_DAT = "dat/argument/yu_10/";
    final private static String YU_10_FILTERED = "dat/argument/yu_10_filtered/";
    final private static String YU_10_GLOBAL_DICT = "dat/argument/yu_10_global/";
    final private static String[] VERB_TAG = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};
    private static String[] DUMMY_VERBS = {"be","am","is","are","was","were","have","has","had","get","got","gets"};
    private HashSet<String> verbTagSet;
    private HashSet<String> dummyVerbSet;
    private Wordnet wn;
    private HashMap<String, Integer> verbFreq;
    private HashMap<String, Integer> verbDF;

    public Verb()throws Exception{
        wn = new Wordnet(false);
        verbFreq = new HashMap<>();
        verbTagSet = new HashSet<>();
        dummyVerbSet = new HashSet<>();
        for(String s : VERB_TAG){
            verbTagSet.add(s);
        }
        for(String s : DUMMY_VERBS){
            dummyVerbSet.add(s);
        }
    }

    // create a global subject dictionary and a global object dictionary
    // from filtered data
    public void buildGlobalArgumentDict() throws Exception{
        BufferedWriter subjWriter = new BufferedWriter(new FileWriter(YU_10_GLOBAL_DICT + "subj.txt"));
        BufferedWriter objWriter = new BufferedWriter(new FileWriter(YU_10_GLOBAL_DICT + "obj.txt"));
        File folder = new File(YU_10_FILTERED);
        HashSet<String> subjSet = new HashSet<>();
        HashSet<String> objSet = new HashSet<>();
        for(File file : folder.listFiles()){
           BufferedReader br = new BufferedReader(new FileReader(file));
            String line_1 = br.readLine();
            String line_2 = br.readLine();

            if(line_1 != null){
                String[] split1 = line_1.split("\\t");
                for(String s : split1){
                    subjSet.add(s);
                }
            }

            if(line_2 != null) {
                String[] split2 = line_2.split("\\t");
                for (String s : split2) {
                    objSet.add(s);
                }
            }
            br.close();
        }

        for(String s : subjSet){
            subjWriter.append(s);
            subjWriter.newLine();
        }

        for(String s : objSet){
            objWriter.append(s);
            objWriter.newLine();
        }

        subjWriter.close();
        objWriter.close();
    }

    public void filterArgumentDict()throws Exception{
        File folder = new File(YU_10_DAT);
        String[] badArg = {"factor", "name", "area", "advance feature", "datum",
                "word", "information", "parameter", "item", "feature", "term",
                "abstract term", "abstract concept", "aspect", "character",
                "essential information", "favorite", "matter","ordinary object",
                "part", "physical object", "semantic concept", "subject","type",
                "unit", "adjective", "variable"};
        HashSet<String> badArgSet = new HashSet<>(Arrays.asList(badArg));
        for(File file : folder.listFiles()){
            String fileName = file.getName();
            String verb = fileName.substring(3, fileName.indexOf("_",4));
            BufferedReader rawReader = new BufferedReader(new FileReader(file));
            BufferedWriter filteredWriter = new BufferedWriter(new FileWriter(YU_10_FILTERED + verb + ".txt"));
            String line;
            rawReader.readLine();
            rawReader.readLine();
            while((line = rawReader.readLine())!=null){
                if(line.trim().equals("Object"))break;
                line = line.trim();
                if(!badArgSet.contains(line)){
                    filteredWriter.append(line + "\t");
                }
            }
            filteredWriter.newLine();
            rawReader.readLine();
            while((line = rawReader.readLine())!=null){
                if(!badArgSet.contains(line)){
                    filteredWriter.append(line + "\t");
                }
            }
            rawReader.close();
            filteredWriter.close();
        }
    }

    public void filterVerbDict()throws Exception {
        HashSet<String> yuSet = new HashSet<>();
        File folder = new File(YU_DAT);
        for(File file : folder.listFiles()){
            String fileName = file.getName();
            String verb = fileName.substring(3, fileName.indexOf("_",3));
            yuSet.add(verb);
        }

        BufferedReader rawDictReader = new BufferedReader(new FileReader(VERB + "verb.freq"));
        BufferedWriter dictWriter = new BufferedWriter(new FileWriter(VERB + "verb.dict"));

        String line =null;

        while((line = rawDictReader.readLine())!=null) {
            String[] splitted = line.split(":");
            String verb = splitted[ 0 ];
            Integer freq = Integer.parseInt(splitted[1]);
            if(freq < 50)break;
            if(yuSet.contains(verb)){
                dictWriter.append(verb);
                dictWriter.newLine();
            }
        }
        dictWriter.close();
        rawDictReader.close();
    }

    public boolean isGoodVerb(String verb) {
        try{

            // 1. contains non-alphabetical, returns false
            if(verb.replaceAll("[^a-z]","").length() != verb.length()) {
                return false;
            }

            if(!wn.isVerb(verb)) {
                return false;
            }

        }catch(Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public void getVerbFromCorpus()throws Exception {
        for(int i = 0; i < 100; ++i) {
            BufferedReader br = new BufferedReader( new FileReader( POSTAGGED + "bing_news_pos_" + i + ".txt"));
            String line = null;
            int oldIdx = -1;
            int curIdx = 0;

            while((line = br.readLine()) != null) {
                String[] sp = line.split("\t");
                System.out.println(i + "\t" + sp[0]);
                List<String> tmp = getVerbFromPostag(sp[1]);
                for(String s : tmp) {
                    if(verbFreq.containsKey(s)) {
                        verbFreq.put(s, verbFreq.get(s) + 1);
                    }else {
                        verbFreq.put(s, 1);
                    }
                }
            }
            br.close();
        }

        ValueComparatorInt bvc = new ValueComparatorInt( verbFreq );
        TreeMap<String, Integer> sortedMap = new TreeMap<>(bvc);
        sortedMap.putAll(verbFreq);

        BufferedWriter bw = new BufferedWriter(new FileWriter(VERB + "verb.freq"));
        for(String s : sortedMap.keySet()) {
            bw.append(s + ":" + verbFreq.get(s));
            bw.newLine();
        }
        bw.close();
    }

    private List<String> getVerbFromPostag(String postagged) throws Exception {
        List<String> result = new ArrayList<String>();
        String[] posStr = postagged.trim().replaceAll("\\[|\\]", "").split(",\\s+");
        for (String pos : posStr) {
            pos = pos.trim();
            String sp[] = pos.split("/");
            String token = sp[0];
            String TAG = sp[1];
            if (verbTagSet.contains(TAG) && !dummyVerbSet.contains(token)) {
                result.add(wn.stemVerbFirst( token ));
            }
        }
        return result;
    }
}
