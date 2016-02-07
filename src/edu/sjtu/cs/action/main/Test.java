package edu.sjtu.cs.action.main;

import edu.mit.jwi.item.POS;
import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.util.*;

import java.util.*;
import java.io.*;

public class Test {
	
	final private static String ACTION_URL = "dat/action/action_10.txt";
	final private static String DEMO_FOLDER_URL = "dat/demo_1m/";
	final private static String FREQ_FOLDER_URL = "dat/freq_1m/";
	final private static String NOUN_LIST_ALL_URL = "dat/abstract_noun_all.txt";
	final private static String NOUN_LIST_URL = "dat/noun_in_title_1m.txt";
	final private static String NOUN_LIST_FREQ_URL = "dat/abstract_noun_freq.txt";
	final private static String BING_NEWS_URL = "dat/news/bing_news_1m.tsv";
	final private static String BING_NEWS_CONV_URL = "dat/news/bing_news_1m.tsv";
	private static String[] DUMMY_VERBS = {"be","have","get"};
 	private static List<String[]> actionList = new ArrayList<String[]>();
	private static List<String> abstractList = new ArrayList<String>();
	private static Lemmatizer lm ;
	private static ProbaseClient probaseClient;
	private static Wordnet wordnet;
	private static List<String> dummyVerbs;
	
	public static void main(String[] args)throws Exception
	{
		//getNounFromNewsTitle();
		getFrequentNounForAction();
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
			if(probaseClient.isGoodConcept(parts[0]))
			{
				result.add(parts[0]);
			}
		}
		return result;
	}
	
	private static void getFrequentNounForAction()throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(NOUN_LIST_URL));
		HashMap<Integer, String[]> idxToNounArray = new HashMap<Integer, String[]>();
		String line = null;
		while((line = br.readLine())!=null)
		{
			String[] splitted = line.split(":");
			String[] splitted2 = splitted[1].split("\t");
			idxToNounArray.put(Integer.parseInt(splitted[0]), splitted2);
		}
		br.close();
		
		loadActionList();
		BufferedReader[] brArray = new BufferedReader[actionList.size()];
		for(int i = 0; i < actionList.size(); ++i)
		{
			String[] action = actionList.get(i);
			brArray[ i ] = new BufferedReader(new FileReader(DEMO_FOLDER_URL +
					action[0] + "_" + action[1] + "_" + action[2] + ".idx"));
		}
		
		BufferedWriter [] bwArray = new BufferedWriter[actionList.size()];
		for(int i = 0; i < actionList.size(); ++i)
		{
			String[] tmp = actionList.get(i);
			bwArray[ i ] = new BufferedWriter( 
					new FileWriter(
							FREQ_FOLDER_URL + tmp[0] + "_" + tmp[1] + "_" + tmp[2] + ".freq"));	
		}
		
		for(int i = 0; i < actionList.size(); ++i)
		{
			System.out.println(i);
			while((line = brArray[i].readLine())!=null)
			{
				
				String[] nouns = idxToNounArray.get(Integer.parseInt(line.split("\\s+")[0]));
				//System.out.println(nouns == null);
				if( nouns == null ||nouns.length == 0)
					continue;
				for(String noun : nouns)
					bwArray[i].append(noun + "\n");
			}
			brArray[i].close();
			bwArray[i].close();
		}
		
	}
	
	
	private static void loadActionList()throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(ACTION_URL));
		String line = null;
		actionList = new ArrayList<String[]>();
		while((line = br.readLine())!=null)
		{
			String splitted[] = line.split("\\s+");
			actionList.add(splitted);
		}
		br.close();
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
		probaseClient = new ProbaseClient();
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
					if(focus[1].startsWith("N") && probaseClient.isPair(action[0], focus[0]))
					{
						findValue++;
						break;
					}
				}
				for( int j = i + 1; j <= upper; ++j)
				{
					String[] focus = tagged.get(j).split("_");
					if(focus[1].startsWith("N") && probaseClient.isPair(action[2], focus[0]))
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
		BufferedReader br = new BufferedReader(new FileReader("dat/bing_news_100k.tsv"));
		String line = null;
		int cnt = 0;
		while((line = br.readLine())!=null)
		{
			line = Utility.convertFromUTF8( line.split("\t")[6].toLowerCase());
			cnt++;
			if(cnt > 30)
				break;
			List<String> rst = lt.lemmatizeAndPOSTag(line);
			for(String st : rst)
			{
				System.out.print(st + " ");
			}
			System.out.println("--");
		}
		br.close();
		
	}
	
	private static void testProbase()throws Exception
	{
		ProbaseClient pb = new ProbaseClient();
		System.out.println(pb.isPair("time","Ferrari"));
	}

	

}
