package edu.sjtu.cs.action.runner;

import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by xinyu on 6/24/2016.
 */
public class tokenizeActionInNewsRunner implements Runnable {

    private int part;
    private String NEWS_URL;
    private String ACC_URL;
    private String ACT_URL;
    private ProbaseClient pc;
    private int offset;
    private List<HashMap<String, String>> acOccurrences;
    private Wordnet wn;
    private int nonChanged;
    public tokenizeActionInNewsRunner(int part, int offset,
                                      String news_url, String acc_url, String act_url,
                                      ProbaseClient pc, Wordnet wn) throws Exception{
        this.part = part;
        this.offset = offset;
        NEWS_URL = news_url;
        ACC_URL = acc_url;
        ACT_URL = act_url;
        this.pc = pc;
        this.wn = wn;
        acOccurrences = new ArrayList<>();
        nonChanged = 0;
    }

    private void loadActionExtracted()throws Exception{
        BufferedReader acReader = new BufferedReader(new FileReader(ACC_URL + part + ".txt"));
        String line;
        int prev = 0;
        int holes = 0;
        System.out.println(part + " start loading actions extracted ... ");
        while((line = acReader.readLine())!=null){
            String[] splitted = line.split("\t");
            Integer idx = Integer.parseInt(splitted[ 0 ]);
            while(idx > prev){
                acOccurrences.add(null);
                holes++;
                prev++;
            }

            //Pick one if multiple
            HashMap<String, List<String>> actions = new HashMap<>();
            for(int i = 1; i < splitted.length; ++i){
                String mixed = splitted[ i ];
                if(!mixed.contains("("))continue;
                String concept = mixed.substring(0, mixed.indexOf("("));
                String instance = mixed.substring(mixed.indexOf("(") + 1, mixed.indexOf(")"));
                if(!actions.containsKey(instance)){
                    actions.put(instance, new ArrayList<>());
                }
                actions.get(instance).add(concept);
            }

            String maxConcept = "";
            HashMap<String, String> aci2acMap = new HashMap<>();
            for(String instance : actions.keySet()){
                String[] instanceSplit = instance.split("_");
                String subj, obj;
                if(instanceSplit.length == 3){
                    obj = instanceSplit[ 2 ];
                }else{
                    obj = "";
                }
                subj = instanceSplit[ 0 ];
                List<String> concepts = actions.get(instance);
                int max = 0;
                for(String concept : concepts){
                    String[] conceptSplit = concept.split("_");
                    String subjc, objc;
                    if(conceptSplit.length == 3){
                        objc = conceptSplit[ 2 ];
                    }else{
                        objc = "";
                    }
                    subjc = conceptSplit[ 0 ];
                    int score = pc.getFreq(subjc, subj) + pc.getFreq(objc, obj);
                    if(score > max){
                        max = score;
                        maxConcept = concept;
                    }
                }
                aci2acMap.put(instance, maxConcept);
            }
            acOccurrences.add(aci2acMap);
            prev++;
        }

        while(acOccurrences.size() < offset) acOccurrences.add(null);
        acReader.close();
        pc.disconnect();
        System.out.println(part + " action loaded.(" + holes + " empty)");
    }

    @Override
    public void run(){
        try {
            loadActionExtracted();
            BufferedReader newsReader = new BufferedReader(new FileReader(NEWS_URL + "bing_news_lemma_" + part + ".txt"));
            BufferedWriter tokenizedWriter = new BufferedWriter(new FileWriter(ACT_URL + part + ".txt"));
            String line;
            int idx = 0;
            while((line = newsReader.readLine())!=null){

                String convertedBody = tokenizeAction(line, acOccurrences.get(idx));
                tokenizedWriter.append(convertedBody);
                tokenizedWriter.newLine();
                idx++;
                if(idx % 1000 == 0){
                    System.out.println(part + "\t" + idx + " lines processed...");
                }
            }
            newsReader.close();
            tokenizedWriter.close();
            System.out.println("nonchanged for " + part + "=" + nonChanged);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private String tokenizeAction(String original, HashMap<String, String> acs)throws Exception{
        if(acs == null){
            nonChanged ++;
           return original;
        }
        String[] splitted = original.split("\\s+");
        String output = "";
        List<String> verbList = new ArrayList<>();
        List<String> aciList = new ArrayList<>();
        for(String aci : acs.keySet()){
            verbList.add(aci.split("_")[1]);
            aciList.add(aci);
        }
        for(int i = 0; i < splitted.length; ++i){
            String target = splitted[ i ];
            if(verbList.contains(target)){
                String aci = aciList.get(verbList.indexOf(target));
                String[] components = aci.split("_");
                String subj = components[ 0 ];
                String obj = (components.length == 3) ? components[ 2 ] : "";
                int searchLower = Math.max(i - 5, 0);
                int searchUpper = Math.min(i + 5, splitted.length);
                int matched = 0;

                for(int j = searchLower; j < searchUpper; ++j){
                    if(splitted[ j ].equalsIgnoreCase(subj)){
                        matched ++;
                    }else if(splitted[ j ].equalsIgnoreCase(obj)){
                        matched ++;
                    }
                }

                if(obj.length() == 0) matched ++;
                if(matched >= 2){
                    splitted[ i ] = acs.get(aci);
                }

            }
            output += " " + splitted[ i ];
        }
        return output;
    }

}
