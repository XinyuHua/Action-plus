package edu.sjtu.cs.action.runner;

import edu.sjtu.cs.action.util.Lemmatizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by xinyu on 6/17/2016.
 */
public class  extractNounRunner  implements Runnable {
    private int part;
    private static String OUT_PATH;
    private static String NEWS_URL;
    private static int offset;
    private static HashSet<String> actionalSet;
    private Lemmatizer lm;

    public extractNounRunner(int part, HashSet<String> ac, int offset, String outPath, String news_url){
        this.part = part;
        this.actionalSet = ac;
        this.offset = offset;
        OUT_PATH = outPath;
        NEWS_URL = news_url;
    }

    public void test(String input) throws Exception {
        BufferedReader newsReader = new BufferedReader(new FileReader(input));
        String line = null;
        lm = new Lemmatizer();
        int cnt = -1;
        int lineNum = part * offset - 1;
        while((line = newsReader.readLine())!=null){
            cnt++;
            lineNum++;
            String title = line.split("\t")[0].toLowerCase();
            String body = line.split("\t")[1].toLowerCase();
            if(body.split("\\s+").length > 200){
                String bodyFirstSentence = "";
                if(body.contains(".")){
                    bodyFirstSentence = body.substring(0, body.indexOf("."));
                }else if(body.contains(",")){
                    bodyFirstSentence = body.substring(0, body.indexOf(","));
                }
                title = title + bodyFirstSentence;
                title = title.replaceAll("\"","");
            }

            // Search for single words
            List<String> result = lm.lemmatizeAndPOSTag(title);
            //HashSet<String> tmp = new HashSet<String>();
            List<String> tmp = new ArrayList<String>();
            for(String sent:  result){
                String[] splitted = sent.split("_");
                if(splitted.length < 2)
                    continue;
                if(splitted[1].startsWith("N")){
                    if(actionalSet.contains(splitted[0])){
                        tmp.add(splitted[0]);
                    }
                }
            }

            // Search for non-single words
            for(String s : actionalSet) {
                if(s.contains(" ") && title.contains(s)){
                    tmp.add(s);
                }
            }

            if(!tmp.isEmpty()){
                System.out.println( cnt + "\t" + tmp.size());
                for(String st : tmp)
                    System.out.print(st + "\t");
                System.out.println();
            }
        }
        newsReader.close();
    }

    public void run(){
        try{
            BufferedReader newsReader = new BufferedReader(new FileReader(NEWS_URL + "bing_news_" + part + ".tsv"));
            BufferedWriter resultWriter = new BufferedWriter(new FileWriter(OUT_PATH + "noun_in_title_" + part + ".txt"));
            String line = null;
            lm = new Lemmatizer();
            int cnt = -1;
            int lineNum = part * offset - 1;
            while((line = newsReader.readLine())!=null){
                cnt++;
                lineNum++;
                String title = line.split("\t")[0].toLowerCase();
                String body = line.split("\t")[1].toLowerCase();
                //if(!title.equals("null"))
                //	System.out.println(cnt + "\t" + lineNum + "\t" + title.substring(0,3));
                if(body.split("\\s+").length > 200){
                    String bodyFirstSentence = "";
                    if(body.contains(".")){
                        bodyFirstSentence = body.substring(0, body.indexOf("."));
                    }else if(body.contains(",")){
                        bodyFirstSentence = body.substring(0, body.indexOf(","));
                    }
                    title = title + bodyFirstSentence;
                    title = title.replaceAll("\"","");
                }

                // Search for single words
                List<String> result = lm.lemmatizeAndPOSTag(title);
                List<String> result_all = lm.lemmatizeAndPOSTag(line);
                HashMap<String, Integer> tmp = new HashMap<>();
                //List<String> tmp = new ArrayList<String>();
                for(String sent:  result){
                    String[] splitted = sent.split("_");
                    if(splitted.length < 2)
                        continue;
                    if(splitted[1].startsWith("N")){
                        if(actionalSet.contains(splitted[0])){
                            tmp.put(splitted[0], 0);
                        }
                    }
                }

                // Search for non-single words
                for(String s : actionalSet) {
                    if(s.contains(" ") && title.contains(s)){
                        tmp.put(s, 0);
                    }
                }

                // Check existence in body
                List<String> filtered = new ArrayList<>();
                for(String sent : result_all) {
                    String[] splitted = sent.split("_");
                    if(splitted.length < 2)
                        continue;
                    if(splitted[1].startsWith("N")){
                        if(tmp.containsKey(splitted[0])){
                            tmp.put(splitted[0], tmp.get(splitted[0]) + 1);
                        }
                    }
                }

                if(!tmp.isEmpty()){
                    String toWrite = "";
                    System.out.println("File index:" + part + "\tLine number: " + cnt + "(total: 56814)\tNoun found in this title:" + tmp.size());
                    for(String st : tmp.keySet()) {
                        if(tmp.get(st) >= 3) {
                            toWrite += (st + "\t");
                        }
                    }
                    if(!toWrite.equals("")) {
                        resultWriter.append(Integer.toString(lineNum) + ":" + toWrite);
                        resultWriter.newLine();
                        resultWriter.flush();
                    }
                }
            }
            resultWriter.close();
            newsReader.close();
        }catch(Exception e){
            System.out.println(e);
            e.printStackTrace();
        }
    }
}
