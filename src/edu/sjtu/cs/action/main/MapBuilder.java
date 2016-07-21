package edu.sjtu.cs.action.main;

import edu.sjtu.cs.action.util.ValueComparatorDouble;
import edu.sjtu.cs.action.util.ValueComparatorInt;

import java.io.*;
import java.util.*;

/**
 * Created by xinyu on 7/19/2016.
 */
public class MapBuilder {

    private String NOUN_DICT_IDX; // noun_[1,2,3,4]
    private String AC_IDX; // yu_5 or yu_10
    private String OUTPUT_PATH;

    private static String NOUN_DAT_PATH = "dat/noun/extracted_";
    private static String NOUN_DICT_PATH;
    private static String AC_DAT_PATH = "dat/action/concept_extracted/";

    private HashMap<Integer, List<String>> nounDat;
    private HashMap<String, List<String>> nounsInAction;
    private List<String> nounList;
    private static HashMap<String, List<Integer>> nounFreqInAction;
    private static HashMap<String, Integer> dfForNoun;
    private static Integer documentSize;

    public MapBuilder(int nounIdx, int acIdx){
        NOUN_DICT_IDX = "noun_" + nounIdx;
        AC_IDX = "yu_" + acIdx;
        NOUN_DAT_PATH  = NOUN_DAT_PATH + nounIdx + "/";
        NOUN_DICT_PATH = "dat/noun/" + nounIdx + ".dict";
        AC_DAT_PATH = AC_DAT_PATH + AC_IDX + "/";

        OUTPUT_PATH = "dat/map/" + AC_IDX + "_" + NOUN_DICT_IDX + "/";
    }

    public void build(int lower, int upper)throws Exception{
        loadNounInTitle(lower, upper);
        loadAcInBody(lower, upper);
        computeFreq();
        computeTfidfAndWrite();
    }

    private void loadNounInTitle(int lower, int upper)throws Exception {
        nounList = new ArrayList<>();
        BufferedReader nounDictReader = new BufferedReader(new FileReader(NOUN_DICT_PATH));
        String line;
        while((line = nounDictReader.readLine())!=null){
            nounList.add(line);
        }
        nounDictReader.close();

        nounDat = new HashMap<>();
        for(int i = lower; i < upper; ++i){
            BufferedReader nounDatReader = new BufferedReader(new FileReader(NOUN_DAT_PATH + "noun_in_title_" + i + ".txt"));
            while((line = nounDatReader.readLine())!=null){
                String[] lineSplit = line.split(":");
                if(lineSplit.length == 1)continue;

                String[] nounListSplit = lineSplit[1].split("\t");
                Integer newsIdx = Integer.parseInt(lineSplit[0]);
                List<String> tmpNounArry = new ArrayList<>();
                for(String s : nounListSplit){
                    if(nounList.contains(s)) {
                        tmpNounArry.add(s);
                    }
                }
                nounDat.put(newsIdx, tmpNounArry);
            }
            nounDatReader.close();
        }
    }

    private void loadAcInBody(int lower, int upper)throws Exception{
        documentSize = 0;
        nounsInAction = new HashMap<>();
        String line;
        for(int i = lower; i < upper; ++i){
            BufferedReader acDatReader = new BufferedReader(new FileReader(AC_DAT_PATH + i + ".txt"));
            while((line = acDatReader.readLine())!=null){
                String[] lineSplit = line.split("\t");
                Integer newsIdx = Integer.parseInt(lineSplit[0]);
                if(!nounDat.containsKey(newsIdx)) continue;
                documentSize ++;
                List<String> tmpNounArry = nounDat.get(newsIdx);
                HashSet<String> tmpAcSet = new HashSet<>();
                for(int j = 1; j < lineSplit.length; ++j){
                    String action = lineSplit[ j ];
                    if(!action.contains("("))continue;
                    action = action.substring(0, action.indexOf("("));
                    tmpAcSet.add(action);
                }

                for(String action : tmpAcSet){
                    if( !nounsInAction.containsKey(action) ) {
                        nounsInAction.put(action, new ArrayList<>());
                    }
                    nounsInAction.get(action).addAll(tmpNounArry);
                }
            }
            acDatReader.close();
        }
    }

    private void computeTfidfAndWrite()throws Exception{
        for(String action : nounsInAction.keySet()) {
            System.out.println("computing tfidf for " + action);
            BufferedWriter bw;
            new File(OUTPUT_PATH).mkdirs();
            bw = new BufferedWriter(new FileWriter(OUTPUT_PATH + action + ".txt"));

            int nounIdx = 0;
            List<Integer> freqList = nounFreqInAction.get(action);

            HashMap<String, Double> tfidfForNoun = new HashMap<>();

            for(String noun : nounsInAction.get(action)){
                Integer tf = freqList.get(nounIdx);
                Integer df = dfForNoun.get(noun);
                Double tfidf = (1 + Math.log(tf * 1.0)) * Math.log10(documentSize / (df + 1.0));
                tfidfForNoun.put(noun, tfidf);
                nounIdx++;
            }

            ValueComparatorDouble bvc = new ValueComparatorDouble(tfidfForNoun);
            TreeMap<String, Double> sortedMap = new TreeMap<>(bvc);
            sortedMap.putAll(tfidfForNoun);

            for(String s : sortedMap.keySet()) {
                bw.write(s + "\t" + tfidfForNoun.get(s));
                bw.newLine();
            }
            bw.close();
        }
    }


    private void computeFreq(){
        nounFreqInAction = new HashMap<>();
        dfForNoun = new HashMap<>();
        for(String action : nounsInAction.keySet()) {
            List<String> nounList = nounsInAction.get(action);
            HashMap<String, Integer> nounFreq = new HashMap<>();
            for(String s : nounList){
                if(!nounFreq.containsKey(s)){
                    nounFreq.put(s, 1);
                } else {
                    nounFreq.put(s, 1 + nounFreq.get(s));
                }

                if(dfForNoun.containsKey(s)){
                    dfForNoun.put(s, dfForNoun.get(s) + 1);
                }else{
                    dfForNoun.put(s, 1);
                }
            }

            ValueComparatorInt bvc = new ValueComparatorInt(nounFreq);
            TreeMap<String, Integer> sortedMap = new TreeMap<>(bvc);
            sortedMap.putAll(nounFreq);

            List<String> sortedList = new ArrayList<>();
            List<Integer> freqList = new ArrayList<>();
            for(String s : sortedMap.keySet()) {
                sortedList.add(s);
                freqList.add(nounFreq.get(s));
            }
            nounsInAction.put(action, sortedList);
            nounFreqInAction.put(action, freqList);
        }
    }

}
