package edu.sjtu.cs.action.main;

import edu.mit.jwi.item.POS;
import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.util.*;

import java.util.*;
import java.io.*;

public class Test {
	
	final private static String ACTION_URL = "dat/action/action_10.txt";
	final private static String VERB_URL = "dat/action/verb_10.txt";
	final private static String DEMO_FOLDER_URL = "dat/demo_1m/";
	final private static String IDX_FOLDER_URL = "dat/index_1m/";
	final private static String FREQ_FOLDER_URL = "dat/freq_1m/";
	final private static String NOUN_LIST_ALL_URL = "dat/abstract_noun_all.txt";
	final private static String NOUN_LIST_URL = "dat/noun_in_title_1m.txt";
	final private static String NOUN_LIST_FREQ_URL = "dat/abstract_noun_freq.txt";
	final private static String BING_NEWS_URL = "dat/news/bing_news_1m.tsv";
	final private static String BING_NEWS_CONV_URL = "dat/news/bing_news_1m.tsv";
	final private static String DETAIL_URL = "dat/details.txt";
	private static String[] DUMMY_VERBS = {"be","have","get"};
 	private static List<String[]> actionList = new ArrayList<String[]>();
	private static List<String> abstractList = new ArrayList<String>();
	private static Lemmatizer lm ;
	private static ProbaseClient pb;
	private static Wordnet wordnet;
	private static List<String> dummyVerbs;
	private static HashSet<String> inflectionSet;
	private static List<Set<String>> inflectionList; 
	public static void main(String[] args)throws Exception
	{
		HashMap<Integer, HashSet<Integer>> a = new HashMap<>();
		HashSet<Integer> t = new HashSet<>();
		t.add(1);
		a.put(0,t);
		HashSet<Integer> t2 = a.get(0);
		t2.add(3);
		System.out.println(a.get(0).size());
//		String body = "Am I joking? No, I don't think so. Right! Nop...";
//		String[] sentences = body.split("((?<=\\?)|(?<=\\!)|(?<=\\.))");
//		for(String s : sentences){
//			System.out.println(s.trim());
//		}
		//getNounFromNewsTitle();
		//getFrequentNounForAction();
//		loadActionList();
//		actionPlusRunner();
	}
	
	
	
	
	private static List<String> getNoun(String paragraph) throws Exception
	{
		List<String> result = new ArrayList<String>();
		List<String> tagged = lm.lemmatizeAndPOSTag(paragraph.toLowerCase());
		for(String st : tagged)
		{
			String[] parts = st.split("_");
			if(!parts[1].startsWith("N"))
				continue;
			//System.out.println(st);
			if(pb.isGoodConcept(parts[0]))
			{
				result.add(parts[0]);
			}
		}
		return result;
	}
	
	
	
	private static void showLine(int dest)throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(BING_NEWS_CONV_URL));
		String line = null;
		int cnt = 0;
		while((line = br.readLine())!=null)
		{
			if(cnt == dest)
			{
				System.out.println(line.split("\t")[7]);
				break;
			}
			cnt++;
		}
		
		br.close();
	}
	
	private static boolean checkPresence(int dest)throws Exception
	{
		pb = new ProbaseClient(4400);
		lm = new Lemmatizer();
		wordnet = new Wordnet(false);
		List<String> dummyVerbs = Arrays.asList(DUMMY_VERBS);
		BufferedReader br = new BufferedReader(new FileReader(BING_NEWS_URL));
		String line = null;
		int cnt = 0;
		String[] action = {"person","play","sports"};
		while((line = br.readLine())!=null)
		{
			if(cnt == dest)
				break;
			cnt++;
		}
		
		String body = line.split("\t")[7];
		List<String> tagged = lm.lemmatizeAndPOSTag(body);
		int findValue = 0;
		int paragraphLength = tagged.size();
				
		// Load synonym list for current verb
		List<String> verbSynTmp = wordnet.getSynonymsForAllRoots(action[1], POS.VERB);
			
		// traverse verbs in the body
		for(int i = 0; i < paragraphLength; ++i)	
		{
			String[] current = tagged.get(i).split("_");
			if(!current[1].contains("V"))
				continue;
			
			if(dummyVerbs.contains(current[0]))
				continue;
			
			String firstStem = wordnet.stemVerbFirst(current[0]);
			if(firstStem == null)
				continue;
				
			if(verbSynTmp.contains(firstStem))
			{
				// check if subj and obj match
				int lower = ( i - 5 < 0) ? 0 : i - 5;
				int upper = ( i + 5 > paragraphLength - 1) ? paragraphLength - 1 : i + 5;
				for( int j = lower ; j < i; ++j)
				{
					String[] focus = tagged.get(j).split("_");
					//System.out.println(focus[0] + "\t" + focus[1]);
					if(focus[1].startsWith("N") && pb.isPair(action[0], focus[0]))
					{
						findValue++;
						break;
					}
				}
				for( int j = i + 1; j <= upper; ++j)
				{
					String[] focus = tagged.get(j).split("_");
					if(focus[1].startsWith("N") && pb.isPair(action[2], focus[0]))
					{
						findValue++;
						break;
					}
				}
				// action found,record idx
				if(findValue == 2)
					return true;
				}
			}
		br.close();
		return false;
	}
	
	private static void testWordNet()throws Exception
	{
		Wordnet wn = new Wordnet(false);
		/*
		for(String st : wn.getSynonymsForAllRoots("dogs", POS.NOUN))
		{
			System.out.println(st);
		}*/
		System.out.println(wn.getHypernymsForAllRoots("dog"));
	}
	

	
	private static void testLemmatizer()throws Exception
	{
		Lemmatizer lt = new Lemmatizer();
		BufferedReader br = new BufferedReader(new FileReader("dat/bing_news_99.tsv"));
		String line = null;
		int cnt = 0;
		while((line = br.readLine())!=null)
		{
			line = Utility.convertFromUTF8( line.split("\t")[6].toLowerCase());
			cnt++;
			
			List<String> rst = lt.lemmatizeAndPOSTag(line);
			for(String st : rst)
			{
				System.out.print(st + " ");
			}
			System.out.println("--");
		}
		br.close();
		
	}
	

}
