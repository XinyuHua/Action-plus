package edu.sjtu.cs.action.runner;

import edu.sjtu.cs.action.util.Lemmatizer;
import edu.sjtu.cs.action.util.Parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

/**
 * Created by xinyu on 4/21/2016.
 */
public class lemmatizeNewsRunner implements Runnable{
    private int part;
    private String NEWS_URL;
    private String LEMMA_URL;


    private int offset;
    private Lemmatizer lemmatizer;

    public lemmatizeNewsRunner(int part, int offset, String news_url, String lemma_url) throws Exception{
        this.part = part;
        this.offset = offset;
        this.lemmatizer = new Lemmatizer();
        this.NEWS_URL = news_url;
        this.LEMMA_URL = lemma_url;
    }

    @Override
    public void run() {
        try{
            BufferedReader br = new BufferedReader(new FileReader(NEWS_URL + "bing_news_" + part + ".tsv"));
            BufferedWriter lemWriter = new BufferedWriter(new FileWriter(LEMMA_URL + "bing_news_lemma_" + part + ".txt"));
            String line = null;
            int cnter = 0;
            while((line = br.readLine())!=null){
                if(cnter % 10 == 0){
                    System.out.println(part + "\t" + cnter);
                }
                List<String> lemmatized = lemmatizer.lemmatize(line);
                for(String lemma : lemmatized) {
                    if(lemma.contains("www") || lemma.contains(".com") || lemma.length() <= 2) {
                        continue;
                    }

                    if(lemma.replaceAll("[^a-zA-Z]","").length() != 0) {
                        if(lemma.contains("/")) {
                            String[] splitted = lemma.split("/");
                            for(String s : splitted) {
                                lemWriter.append(s + " ");
                            }
                        } else {
                            lemWriter.append(lemma + " ");
                        }
                    }
                }
                lemWriter.newLine();
                cnter++;
            }
            lemWriter.close();
            br.close();
            System.out.println(part + " finished.");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
