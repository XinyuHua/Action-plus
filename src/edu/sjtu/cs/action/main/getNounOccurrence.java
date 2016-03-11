package edu.sjtu.cs.action.main;
import java.io.*;
import java.util.*;

import edu.sjtu.cs.action.util.Lemmatizer;

public class getNounOccurrence  implements Runnable {
	
	private int part;
	final private static String IDX_FOLDER_URL = "dat/noun_occurrence_tmp/";
	final private static String BING_NEWS_SLICED_URL = "dat/news/bing_news_sliced/";
	private static int offset = 134163;
	private static Lemmatizer lm;
	private static HashSet<String> actionalSet;
	public getNounOccurrence(int part, Lemmatizer lm, HashSet<String> ac){
		this.part = part;
		this.lm = lm;
		this.actionalSet = ac;
	}

	public void run(){
		try{
			BufferedReader newsReader = new BufferedReader(new FileReader(BING_NEWS_SLICED_URL + "bing_news_" + part + ".tsv"));
			BufferedWriter resultWriter = new BufferedWriter(new FileWriter(IDX_FOLDER_URL + "noun_in_title_" + part + ".txt"));
			String line = null;
			lm = new Lemmatizer();
			int cnt = -1;
			int lineNum = part * offset - 1;
			while((line = newsReader.readLine())!=null){
				cnt++;
				lineNum++;
				String title = line.split("\t")[6].toLowerCase();
				List<String> result = lm.lemmatizeAndPOSTag(title);
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
				if(!tmp.isEmpty()){
					System.out.println(part + "\t" + cnt + "\t" + tmp.size());
					resultWriter.append(Integer.toString(lineNum) + ":");
					for(String st : tmp)
						resultWriter.append(st + "\t");
					resultWriter.newLine();
					resultWriter.flush();
				}
			}
			resultWriter.close();
			newsReader.close();
		}catch(Exception e){
		}
		
	}
	/*
	public void run(){
		try{
			BufferedReader newsReader = new BufferedReader(new FileReader(BING_NEWS_SLICED_URL + "bing_news_" + part + ".tsv"));
			BufferedWriter resultWriter = new BufferedWriter(new FileWriter(IDX_FOLDER_URL + "noun_in_title_" + part + ".txt"));
			String line = null;
			lm = new Lemmatizer();
			int cnt = -1;
			int lineNum = part * offset - 1;
			while((line = newsReader.readLine())!=null){
				cnt++;
				lineNum++;
				String title = line.split("\t")[6].toLowerCase();
				List<String> result = lm.lemmatizeAndPOSTag(title);
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
				if(!tmp.isEmpty()){
					System.out.println(part + "\t" + cnt + "\t" + tmp.size());
					resultWriter.append(Integer.toString(lineNum) + ":");
					for(String st : tmp)
						resultWriter.append(st + "\t");
					resultWriter.newLine();
					resultWriter.flush();
				}
			}
			resultWriter.close();
			newsReader.close();
		}catch(Exception e){
		}
		
	}*/
}
