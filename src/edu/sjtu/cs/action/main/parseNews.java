package edu.sjtu.cs.action.main;

import edu.sjtu.cs.action.util.Parser;
import java.io.*;
import java.util.*;

public class parseNews implements Runnable{

	private int part;
	private static final String NEWS_PATH = "dat/news/bing_news_sliced/";
	private static final String PARSED_PATH = "dat/news/bing_news_sliced_parsed/";
	private static final String POSTAG_PATH = "dat/news/bing_news_sliced_postag/";
	private int offset;
	private Parser parser;
	
	public parseNews(int part, int offset) throws Exception{
		this.part = part;
		this.offset = offset;
		this.parser = new Parser();
	}
	
	@Override
	public void run() {
		try{
			BufferedReader br = new BufferedReader(new FileReader(NEWS_PATH + "bing_news_" + part + ".tsv"));
			BufferedWriter postagWriter = new BufferedWriter(new FileWriter(POSTAG_PATH + "bing_news_pos_" + part + ".txt"));
			BufferedWriter parsedWriter = new BufferedWriter(new FileWriter(PARSED_PATH + "bing_news_parsed_" + part + ".txt"));
			String line = null;
			int cnter = 0;
			while((line = br.readLine())!=null){
				if(cnter % 10 == 0){
					System.out.println(part + "\t" + cnter);
				}
				String newsBody = line.split("\t")[1].trim();
				if(newsBody.charAt(0) == '"'){
					newsBody = newsBody.substring(1, newsBody.length() - 1);
				}
				newsBody = newsBody.replace("\\\"", "\"");
				List<List<String>> result = parser.dependencyParseDocument(newsBody);
				List<String> parsed = result.get(0);
				List<String> postag = result.get(1);
				for(String sent : parsed){
					parsedWriter.append(Integer.toString(cnter + offset * part) + "\t" + sent);
					parsedWriter.newLine();
					parsedWriter.flush();
				}
				
				for(String sent : postag){
					postagWriter.append(Integer.toString(cnter + offset * part) + "\t" + sent);
					postagWriter.newLine();
					postagWriter.flush();
				}
				cnter++;
				
			}
			postagWriter.close();
			parsedWriter.close();
			br.close();
			System.out.println(part + " finished.");
		}catch(Exception e){
		}
	}

}
