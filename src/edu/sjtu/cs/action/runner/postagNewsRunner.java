package edu.sjtu.cs.action.runner;

import edu.sjtu.cs.action.util.Parser;
import edu.sjtu.cs.action.util.Postag;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

/**
 * Created by xinyu on 6/17/2016.
 *
 * This class postags news titles
 */
public class postagNewsRunner implements Runnable{
    private int part;
    private String NEWS_URL;
    private String POSTAG_URL;
    private int offset;
    private Postag tagger;

    public postagNewsRunner(int part, int offset, String news_url, String postag_url) throws Exception{
        this.part = part;
        this.offset = offset;
        this.tagger = new Postag();
        this.NEWS_URL = news_url;
        this.POSTAG_URL = postag_url;
    }

    @Override
    public void run() {
        try{
            BufferedReader br = new BufferedReader(new FileReader(NEWS_URL + "bing_news_" + part + ".tsv"));
            BufferedWriter postagWriter = new BufferedWriter(new FileWriter(POSTAG_URL + "bing_news_title_pos_" + part + ".txt"));
            String line = null;
            int cnter = 0;
            while((line = br.readLine())!=null){
                if(cnter % 100 == 0){
                    System.out.println(part + "\t" + cnter);
                }
                String newsTitle = line.split("\t")[ 0 ].trim();
                if(newsTitle.charAt(0) == '"'){
                    newsTitle = newsTitle.substring(1, newsTitle.length() - 1);
                }
                newsTitle = newsTitle.replace("\\\"", "\"");

                List<String> tagged = tagger.postagDocument(newsTitle);

                if(tagged.size() > 1){
                    postagWriter.append(Integer.toString(cnter + offset * part) + "\t");
                    for(String pos : tagged){
                        postagWriter.append(pos + "\t");
                    }
                    postagWriter.newLine();
                }
                cnter++;
            }
            postagWriter.close();
            br.close();
            System.out.println(part + " finished.");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
