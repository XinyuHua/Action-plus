package edu.sjtu.cs.action.main;

import edu.sjtu.cs.action.util.*;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Created by xinyu on 5/29/2016.
 * Last modified: 6/1/2016
 *
 * This class creates action-noun map.
 */

public class getActionNounMap {
    // Path for additional information
    private static final String NOUN_DICT_ID = "4";
    private static final Integer FILE_UPPERBOUN = 100;
    private static final boolean USE_HIERARCHY = true;
    private static final String CONCEPT_DICT = "dat/action/groundtruth_dict.txt";
    private static final String NOUN_HIER_URL = "dat/noun_concept/noun_" + NOUN_DICT_ID + ".hierarchy";
    private static final String NOUN_DICT = "dat/noun_concept/noun_" + NOUN_DICT_ID + ".dict";

    // Path for input
    private static String NOUN_OCC_URL = "dat/tmp/noun_occurrence_" + NOUN_DICT_ID + "_tmp/";
    private static String AC_OCC_URL = "dat/tmp/concept_occurrence_tmp/eval/";
    private static final boolean WITH_INSTANCE = true;

    // Path for output
    private static String NOUN_IN_AC = "dat/tmp/noun_" + NOUN_DICT_ID + "_in_action/raw/";
    private static String AC_NOUN_MAP = "dat/tmp/noun_" + NOUN_DICT_ID + "_in_action/tfidf/";
    private static String NOUN_IN_AC_H = "dat/tmp/noun_" + NOUN_DICT_ID + "_in_action/hierarchy_raw/";
    private static String AC_NOUN_MAP_H = "dat/tmp/noun_" + NOUN_DICT_ID + "_in_action/hierarchy_tfidf/";
    private static String GENERATED_H_RAW = "dat/tmp/noun_" + NOUN_DICT_ID + "_in_action/generated_hierarchy_raw/";

    // Data structure to use
    private static HashSet<String> nounToCheck;
    private static HashSet<String> actionToCheck;
    private static HashMap<Integer, List<String>> nounsInNews;
    private static HashMap<String, List<String>> nounsInAction;
    private static HashMap<String, List<Integer>> nounFreqInAction;
    private static HashMap<String, List<String>> conceptsForInstance;
    private static HashMap<String, Integer> dfForNoun;
    private static Integer documentSize;

    public static void main(String[] args) throws Exception{
         computeFreqAndWrite();
         computeTFIDFAndWrite();
    }


    public static void computeTFIDFAndWrite()throws Exception{
        loadOccurrenceData();
        loadNounFreqInAction();
        System.out.println(nounsInAction.size());
        for(String action : nounsInAction.keySet()) {
            System.out.println("computing tfidf for " + action);
            BufferedWriter bw;
            if(USE_HIERARCHY){
                new File(AC_NOUN_MAP_H).mkdirs();
                bw = new BufferedWriter(new FileWriter(AC_NOUN_MAP_H + action + ".txt"));
            }else{
                new File(AC_NOUN_MAP).mkdirs();
                bw = new BufferedWriter(new FileWriter(AC_NOUN_MAP + action + ".txt"));
            }
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

    private static void loadNounFreqInAction() throws Exception{
        File folder;
        if(USE_HIERARCHY){
            folder = new File(NOUN_IN_AC_H);
            loadHierarchy();
        }else{
            folder = new File(NOUN_IN_AC);
        }
        String line;
        nounsInAction = new HashMap<>();
        nounFreqInAction = new HashMap<>();
        dfForNoun = new HashMap<>();
        for(File file : folder.listFiles()) {
            String action = file.getName().replaceAll(".txt", "");
            BufferedReader br = new BufferedReader(new FileReader(file));
            List<String> nounList = new ArrayList<>();
            List<Integer> freqList = new ArrayList<>();
            while((line = br.readLine()) != null){
                String noun = line.split("\t")[0];
                String freq = line.split("\t")[1];
                nounList.add(noun);
                freqList.add(Integer.parseInt(freq));
                if(dfForNoun.containsKey(noun)){
                    dfForNoun.put(noun, dfForNoun.get(noun) + 1);
                }else{
                    dfForNoun.put(noun, 1);
                }
            }
            nounsInAction.put(action, nounList);
            nounFreqInAction.put(action, freqList);
            br.close();
        }

    }



    public static void computeFreqAndWrite()throws Exception{
        loadOccurrenceData();

        if(USE_HIERARCHY) loadHierarchy();

        nounFreqInAction = new HashMap<>();
        for(String action : nounsInAction.keySet()) {
            List<String> nounList = nounsInAction.get(action);
            HashMap<String, Integer> nounFreq = new HashMap<>();
            for(String s : nounList){
                if(!nounFreq.containsKey(s)){
                    nounFreq.put(s, 1);
                } else {
                    nounFreq.put(s, 1 + nounFreq.get(s));
                }
            }

            if(USE_HIERARCHY) {
                HashMap<String, Integer> additional = new HashMap<>();
                for(String s : nounFreq.keySet()){
                    if( nounFreq.get(s) > 10) {
                        Integer freq = nounFreq.get(s);
                        List<String> concepts = conceptsForInstance.get(s);

                        for(String concept : concepts) {
                            if(additional.containsKey(concept)) {
                                additional.put(concept, additional.get(concept) + freq);
                            } else {
                                additional.put(concept, freq);
                            }
                        }
                    }
                }
                for(String add : additional.keySet()){
                    if(nounFreq.containsKey(add)) {
                        nounFreq.put(add, nounFreq.get(add) + additional.get(add));
                    } else {
                        nounFreq.put(add, additional.get(add));
                    }
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
            BufferedWriter bw;
            if(USE_HIERARCHY) {
                new File(NOUN_IN_AC_H).mkdirs();
                bw = new BufferedWriter(new FileWriter(NOUN_IN_AC_H + action + ".txt"));
            }else{
                new File(NOUN_IN_AC).mkdirs();
                bw = new BufferedWriter(new FileWriter(NOUN_IN_AC + action + ".txt"));
            }
            for(String s : sortedList){
                bw.append(s + "\t" + nounFreq.get(s));
                bw.newLine();
            }
            bw.close();
        }

    }

    private static void loadOccurrenceData()throws Exception{
        loadChosenAction();
        loadChosenNoun();
        nounsInNews = new HashMap<>();
        nounsInAction = new HashMap<>();
        String line;
        for(int i = 0; i < FILE_UPPERBOUN; ++i){
            System.out.println("Matching ac and noun for " + i);
            BufferedReader br = new BufferedReader(new FileReader(NOUN_OCC_URL + "noun_in_title_" + i + ".txt"));
            while((line = br.readLine())!=null){
                String[] lineSplit = line.split(":");
                if(lineSplit.length == 1)continue;

                String[] nounListSplit = lineSplit[1].split("\t");
                Integer newsIdx = Integer.parseInt(lineSplit[0]);
                List<String> tmpNounArry = new ArrayList<>();
                for(String s : nounListSplit){
                    if(nounToCheck.contains(s)) {
                        tmpNounArry.add(s);
                    }
                }
                nounsInNews.put(newsIdx, tmpNounArry);
            }
            br.close();
        }

        for(int i = 0; i < FILE_UPPERBOUN; ++i){
            BufferedReader br = new BufferedReader(new FileReader(AC_OCC_URL  + i + ".txt"));
            while((line = br.readLine())!=null){
                String[] lineSplit = line.split("\t");
                Integer newsIdx = Integer.parseInt(lineSplit[0]);
                if(!nounsInNews.containsKey(newsIdx)) continue;
                List<String> tmpNounArry = nounsInNews.get(newsIdx);

                for(int j = 1; j < lineSplit.length; ++j){
                    String action = lineSplit[ j ];
                    if(WITH_INSTANCE){
                        action = action.substring(0, action.indexOf("("));
                    }

                    if(!actionToCheck.contains(action)) continue;
                    if( !nounsInAction.containsKey(action) ) {
                        nounsInAction.put(action, new ArrayList<>());
                    }
                    nounsInAction.get(action).addAll(tmpNounArry);
                }
            }
            br.close();
        }

        // document size set to number of actions
        documentSize = nounsInAction.size();

        // document size set to number of news with noun discovered
      //  documentSize = nounsInNews.size();
    }


    private static void loadChosenAction()throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(CONCEPT_DICT));
        String line;
        actionToCheck = new HashSet<>();
        while((line = br.readLine())!=null){
            actionToCheck.add(line.split(":")[0]);
        }
        br.close();
    }

    private static void loadChosenNoun()throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(NOUN_DICT));
        String line;
        nounToCheck = new HashSet<>();
        while((line = br.readLine())!=null){
            nounToCheck.add(line.trim());
        }
        br.close();
    }


    private static void loadHierarchy()throws Exception{
        conceptsForInstance = new HashMap<>();
        BufferedReader hierReader = new BufferedReader(new FileReader(NOUN_HIER_URL));
        String line;
        int j = 0;
        while((line = hierReader.readLine())!=null){
            System.out.println("Loading hierarchy for " + j);
            j++;
            String[] splitted = line.split("\t");
            List<String> tmp = new ArrayList<>();
            for(int i = 1; i < splitted.length; ++i) {
                tmp.add(splitted[ i ]);
            }
            conceptsForInstance.put(splitted[0], tmp);
        }
        hierReader.close();
    }
}
