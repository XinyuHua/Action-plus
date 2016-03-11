package edu.sjtu.cs.action.main;
import edu.mit.jwi.item.POS;
import edu.sjtu.cs.action.knowledgebase.*;
import edu.sjtu.cs.action.util.Lemmatizer;
import edu.sjtu.cs.action.util.NLP;
import edu.sjtu.cs.action.util.Utility;

import java.io.*;
import java.text.DateFormat;
import java.util.*;

/*
 * Author: xinyu
 * Last Modified: 2016-3-11
 * 
 * Description: This class deals with: 
 * 	1). Extract Action instance from news body
 *  2). Extract Actional noun from bews title
 *  3). Get news for each sentence in extracted action files
 *  
 * Details: 
 *  1.1. Two methods to extract action, numbered 1 and 2
 *  1.2. Actions are extracted from /dat/news/bing_sliced/bing_news_[0...99].tsv
 *  1.3  Action instances together with senteces are stored into /dat/action_occurrence_[1,2]/unclustered/[action].txt
 *  
 *  2.1  Two dictionaries, ps and wn. ps has 145 words, wn has more than 9,000 words
 *  2.2  Nouns are extracted from /dat/news/bing_sliced/bing_news_[0...99].tsv
 *  2.3  Nouns are stored into /dat/noun_occurrence_[ps,wn].txt
 *  
 *  3.1  News are extracted from /dat/news/bing_sliced/bing_news_[0...99].tsv
 *  3.2  News are stored into /dat/news_for_action_[1,2]/unclustered/[action].txt
 */

public class Main {
	
	final private static String[] DICT_DAT = {"ps", "wn"};
	final private static String[] CLUSTERITY = {"clustered", "unclustered"};
	final private static int DICT_INDEX = 0;
	final private static int CORPUS_INDEX = 1;
	final private static int CLUSTERITY_INDEX = 1;
	final private static String ACTION_OCCUR = "/dat/action_occurrence_" + CORPUS_INDEX + "/" + CLUSTERITY[CLUSTERITY_INDEX] + "/" ;
	final private static String ACTION_TMP_URL = "dat/action_occurrence_tmp/";
	final private static String ACTION_URL = "dat/action/action_60.txt";
	final private static String VERB_URL = "dat/action/verb_60.txt";
	final private static String NOUN_DICT_URL = "dat/noun_concept/noun_" + DICT_DAT[DICT_INDEX] +".dict";
	final private static String BING_NEWS_URL = "dat/news/bing_news_ready.tsv";
	final private static String NOUN_TMP_URL = "dat/noun_occurrence_tmp/";
	final private static String NOUN_RST_URL = "dat/noun_occurrence_"  + DICT_DAT[DICT_INDEX] +".txt";
	final private static String ACTION_NEWS_OCCUR = "/dat/news_action_" + CORPUS_INDEX + "/" + CLUSTERITY[CLUSTERITY_INDEX] + "/" ;
	private static List<String> puncList;
	private static String[] DUMMY_VERBS = {"be","have","get"};
 	private static List<String[]> actionList;
	private static Lemmatizer lm ;
	private static ProbaseClient probaseClient;
	private static NLP nlp;
	private static Wordnet wordnet;
	private static List<Set<String>> inflectionList; 
	private static HashSet<String> inflectionSet;
	private static List<String> dummyVerbs;
	private static int offset = 134163;
	
	public static void main(String args[]) throws Exception{
		
		Date rightNow;
		Locale currentLocale;
		DateFormat timeFormatter;
		DateFormat dateFormatter;
		String timeOutput;
		String dateOutput;
		
		long startTime = System.nanoTime();
	//	getActionRunner(80, 4400, 20);
		getNounRunner(30,10);
	//	getNewsForAction();
		long endTime = System.nanoTime();
		long duration = endTime - startTime;
		System.out.println("Time elapsed:" + duration/1e9 + " sec");
		
		rightNow = new Date();
		currentLocale = new Locale("en");
		
		timeFormatter = DateFormat.getTimeInstance(DateFormat.DEFAULT, currentLocale);
		dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, currentLocale);
		
		timeOutput = timeFormatter.format(rightNow);
		dateOutput = dateFormatter.format(rightNow);
		
		System.out.println(timeOutput);
		System.out.println(dateOutput);
		
	}
	
	/*
	 * This method finds news for sentences in action occurrence
	 */
	public static void getNewsForAction()throws Exception{
		loadActionList();
		BufferedWriter[] bwArray = new BufferedWriter[actionList.size()];
		HashMap<Integer, HashSet<Integer>> idx2ac = new HashMap<>();
		for(int i = 0; i < actionList.size(); ++i){
			System.out.println("collecting " + i);
			String[] action = actionList.get(i);
			String actionName =  action[0] + "_" + action[1] + "_" + action[2];
			bwArray[i] = new BufferedWriter(new FileWriter(
					ACTION_NEWS_OCCUR +actionName + ".news"));
			BufferedReader br = new BufferedReader(new FileReader(ACTION_OCCUR  + actionName + ".idx"));
			String line = null;
			while((line = br.readLine())!=null){
				String[] splitted = line.split("\t");
				int newsIdx = Integer.parseInt(splitted[0]);
				
				if(idx2ac.containsKey(newsIdx)){
					HashSet<Integer> tmp = idx2ac.get(newsIdx);
					tmp.add(i);
					
				}else{
					idx2ac.put(newsIdx, new HashSet<Integer>());
					HashSet<Integer> tmp = idx2ac.get(newsIdx);
					tmp.add(i);
				}
			}
			br.close();
		}
		BufferedReader newsReader = new BufferedReader(new FileReader(BING_NEWS_URL ));
		String line = null;
		int lineNum = 0;
		while((line = newsReader.readLine())!=null){
			System.out.println(lineNum);
			if(idx2ac.containsKey(lineNum)){
				String newsBody = line.split("\t")[7];
				for(Integer ac : idx2ac.get(lineNum)){
					bwArray[ ac ].append(lineNum + "\t" + newsBody + "\n");
				}
			}
			lineNum++;
		}
		for(int i = 0; i < actionList.size(); ++i){
			bwArray[ i ].close();
		}
		newsReader.close();
	}
	
	/*
	 * This method finds noun in news titles
	 */
	public static void getNounRunner(int fileBase, int size)throws Exception{
		lm = new Lemmatizer();
		
		// Load noun dictionary
		BufferedReader dictReader = new BufferedReader(new FileReader( NOUN_DICT_URL ));
		HashSet<String> actionalSet = new HashSet<String>();
		String line = null;
		while((line = dictReader.readLine())!=null){
			if(!line.contains("_") && !line.contains("-")){
				actionalSet.add(line.toLowerCase());
			}
		}
		dictReader.close();
		
		// Start searching for nouns in news title
		Runnable[] gNA = new Runnable[size];
		for(int i = 0; i < size; ++i){
			gNA[ i ] = new getNounOccurrence(i + fileBase,lm,actionalSet);
		}
		
		Thread[] tdA = new Thread[size];
		for(int i = 0; i < size; ++i){
			tdA[ i ] = new Thread(gNA[ i ]);
			tdA[ i ].start();
		}
		
		for(int i = 0; i < size; ++i){
			tdA[ i ].join();
		}
		
		
		return;
	}
	
	/*
	 * This method finds action instances in news bodies
	 */
	public static void getActionRunner(int fileBase, int portBase, int size)throws Exception{
		loadActionList();
		wordnet = new Wordnet(true);
		lm = new Lemmatizer();
		dummyVerbs = Arrays.asList(DUMMY_VERBS);
		String[] punc = { ",", ".", "!", "-", "?" };
		puncList = Arrays.asList(punc);
		ProbaseClient[] pA = new ProbaseClient[size];
		for(int i = 0; i < size; ++i){
			pA[ i ] = new ProbaseClient(portBase + i);
		}
		
		Runnable[] gA = new Runnable[size];
		for(int i = 0; i < size; ++i){
			gA[ i ] = new getActionOccurrence(i + fileBase,pA[ i ],wordnet, lm,
					dummyVerbs, actionList, inflectionList, inflectionSet, puncList); 
		}
		
		Thread[] tdA = new Thread[size];
		for(int i = 0; i < size; ++i){
			tdA[ i ] = new Thread(gA[ i ]);
			tdA[ i ].start();
		}
		for(int i = 0; i < size; ++i){
			tdA[ i ].join();
		}
		
		return;
	}
	
	public static void nounOccurrenceReduction()throws Exception{
		String line = null;
		// Reduce to one file
		BufferedWriter bw = new BufferedWriter(new FileWriter(NOUN_RST_URL));
		for(int part = 0; part < 100; ++part){
			BufferedReader br = new BufferedReader(new FileReader(NOUN_TMP_URL + "noun_in_title_" + part + ".txt"));
			while((line = br.readLine())!=null){
				bw.append(line);
				bw.newLine();
			}
			br.close();
		}
		bw.close();
	}
	
	public static void actionOccurrenceReduction()throws Exception{
		String line = null;
		for(String[] action : actionList){
			// Reduce to one file
			BufferedWriter bw = new BufferedWriter(new FileWriter(ACTION_OCCUR + action[0] + "_" + action[1] + "_" + action[2] + ".txt"));
			for(int part = 0; part < 100; ++part){
				BufferedReader br = new BufferedReader(new FileReader(ACTION_TMP_URL + "noun_in_title_" + part + ".txt"));
				while((line = br.readLine())!=null){
					bw.append(line);
					bw.newLine();
				}
				br.close();
			}
			bw.close();
		}
	}
	
	private static void loadActionList()throws Exception{
		BufferedReader br = new BufferedReader(new FileReader(ACTION_URL));
		BufferedReader br2 = new BufferedReader(new FileReader(VERB_URL));
		String line = null;
		actionList = new ArrayList<String[]>();
		inflectionList = new ArrayList<Set<String>>();
		inflectionSet = new HashSet<String>();
		while((line = br.readLine())!=null){
			String splitted[] = line.split("\\s+");
			actionList.add(splitted);
		}
		br.close();
		while((line = br2.readLine())!=null){
			List<String> lines = Arrays.asList(line.split("\\s+"));
			HashSet<String> tmp = new HashSet<String>();
			tmp.addAll(lines);
			inflectionList.add(tmp);
			inflectionSet.addAll(lines);
		}
		br2.close();
	}
}
