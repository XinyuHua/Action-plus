package edu.sjtu.cs.action.util;

import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;

import java.io.*;
import java.util.Arrays;
import java.util.*;

/**
 * Created by xinyu on 4/2/2016.
 */
public class NounList {
    private static final String NOUN_CONCEPT_URL= "dat/noun/";
    private static final String NEWS_TITLE_URL = "dat/news/bing_news_sliced_title_postag/";

    private static List<String> actionalHyperList;
    private static final String[] ACTIONAL_HYPER_ARRAY = {"action","activity","act"
            ,"process","practice","event","behavior"};

    private int NOUN_DICT_INDEX;
    private int PB_FILTER_NUM = 10;
    private int CORPUS_FILTERED_NUM = 50;
    private String DICT_URL;
    private String FREQ_URL;
    private Wordnet wn;
    private ProbaseClient pb;

    public NounList(int index, boolean pbNeeded)throws Exception{
        NOUN_DICT_INDEX = index;
        DICT_URL = NOUN_CONCEPT_URL + NOUN_DICT_INDEX + ".dict";
        FREQ_URL = NOUN_CONCEPT_URL + NOUN_DICT_INDEX + ".freq";
        if(pbNeeded){
            pb = new ProbaseClient(4400);
        }
        wn = new Wordnet(false);
        actionalHyperList = Arrays.asList(ACTIONAL_HYPER_ARRAY);
    }


    private List<String> getWNNouns()throws Exception{
        System.out.println("Getting all nouns from Wordnet...");
        List<String> wnAllNoun= new ArrayList<>();
        wnAllNoun.addAll(wn.getAllNoun());
        return wnAllNoun;
    }

    private List<String> getNounInTitles()throws Exception{
        System.out.println("Getting nouns from News titles...");
        List<String> result = new ArrayList<>();
        HashMap<String, Integer> freqMap = new HashMap<>();
        File newsFolder = new File(NEWS_TITLE_URL);
        String line;
        for(File file : newsFolder.listFiles()){
            System.out.println("Reading " + file.getName() + "...");
            BufferedReader newsReader = new BufferedReader(new FileReader(file));
            while((line = newsReader.readLine())!=null){
                String[] taggedArray = line.split("\t");

                HashSet<String> nounSetForThisNews = new HashSet<>();

                for(String token :  taggedArray){
                    String[] splitted = token.split("_");
                    if(splitted.length < 2)
                        continue;
                    if(splitted[1].startsWith("N")){
                        String noun = splitted[ 0 ];
                        nounSetForThisNews.add(noun.toLowerCase());
                    }
                }

                for(String noun : nounSetForThisNews){
                    if(freqMap.containsKey(noun)){
                        freqMap.put(noun, freqMap.get(noun) + 1);
                    } else {
                        freqMap.put(noun, 1);
                    }
                }
            }
        }

        ValueComparatorInt comparatorInt = new ValueComparatorInt(freqMap);
        TreeMap<String, Integer> orderedMap = new TreeMap<>(comparatorInt);
        orderedMap.putAll(freqMap);

        for(String noun : orderedMap.keySet()){
            if(freqMap.get(noun) < CORPUS_FILTERED_NUM){
                break;
            }
            //System.out.println(noun + "\t" + freqMap.get(noun));
            result.add(noun);
        }
        return result;
    }


    public void getNounList()throws Exception{
        switch(NOUN_DICT_INDEX){
            case 1:
                writeResultToDisk( filterByPB( getWNNouns() ) );
                break;
            case 2:
                writeResultToDisk( filterByPB( getNounInTitles() ) );
                break;
            case 3:
                writeResultToDisk( filterByLexicalInfo( getWNNouns() ) );
                break;
            case 4:
                writeResultToDisk( filterByLexicalInfo( getNounInTitles() ));
                break;
        }
    }

    private List<String> filterByPB(List<String> toFilter)throws Exception{
        System.out.println("Filtering by Probase concepts...");
       List<String> filtrered = new ArrayList<>();
        for(String noun : toFilter){
            if(isPBActional(noun)){
                filtrered.add(noun);
            }
        }
        pb.disconnect();
        return filtrered;
    }

    private List<String> filterByLexicalInfo(List<String> toFilter)throws Exception{
        System.out.println("Filtering by lexical information...");
        List<String> filtered = new ArrayList<>();
        for(String noun : toFilter){
            if(wn.isActionalNoun(noun) && noun.length() > 2) {
                filtered.add(noun);
            }
        }
        return filtered;
    }

    private void writeResultToDisk(List<String> filtered)throws Exception{
        System.out.println("Writing to disk...");
        BufferedWriter resultWriter = new BufferedWriter(new FileWriter(DICT_URL));
        for(String noun : filtered){
            resultWriter.append(noun);
            resultWriter.newLine();
        }
        resultWriter.close();
        System.out.println("Writing finished.");
    }


    private boolean isPBActional(String word) throws Exception {

        String noun = wn.stemNounFirst(word);
        if(!pb.isProbaseEntity(noun)) {
            return false;
        }

        List<String> concepts = pb.findHyper(noun);
        if(concepts.isEmpty()) {
            return false;
        }

        int highest = pb.getFreq(concepts.get(0), noun);
        if(highest < 5)
            return false;

        int threshold = (highest + 1) / 2;
        boolean tmp = false;
        for(String concept : concepts){
            if( pb.getFreq(concept, noun) < threshold){
                break;
            }
            if( actionalHyperList.contains(concept)){
                tmp = true;
                break;
            }
        }
        if(tmp && pb.findHypo(noun).size() > PB_FILTER_NUM)
            return true;

        return false;
    }
}
