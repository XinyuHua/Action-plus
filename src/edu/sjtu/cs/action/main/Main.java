package edu.sjtu.cs.action.main;
import edu.mit.jwi.item.POS;
import edu.sjtu.cs.action.knowledgebase.*;
import edu.sjtu.cs.action.util.Lemmatizer;
import edu.sjtu.cs.action.util.Utility;

import java.io.*;
import java.util.*;

public class Main {
	
	final private static String ACTION_URL = "dat/action/action_10.txt";
	final private static String IDX_FOLDER_URL = "dat/action_occurrence_1m/";
	final private static String BING_NEWS_URL = "dat/news/bing_news_1m.tsv";
	final private static String BING_NEWS_CONV_URL = "dat/news/bing_news_1m.tsv";
	final private static String NOUN_LIST_URL = "dat/noun_in_title_1m.txt";
	private static String[] DUMMY_VERBS = {"be","have","get"};
 	private static List<String[]> actionList = new ArrayList<String[]>();
	private static List<String> abstractList = new ArrayList<String>();
	private static Lemmatizer lm ;
	private static ProbaseClient probaseClient;
	private static Wordnet wordnet;
	
	public static void main(String args[]) throws Exception{
		//System.out.println( checkPresence(65));
		actionPlusRunner();
		//convert();
		//findNounInTitles();
	}
	
	
	// For each action, finds news items where the action occurs and record the index for those news
	private static void actionPlusRunner()throws Exception{	
		//1. Read Actions
		System.out.println("Reading actions...");
		loadActionList();
		System.out.println("Action Loaded.");
		//2. Create Action record file under demo folder
		BufferedWriter [] bwArray = new BufferedWriter[actionList.size()];
		
		for(int i = 0; i < actionList.size(); ++i)
		{
			String[] tmp = actionList.get(i);
			File tmpFile = new File(IDX_FOLDER_URL + tmp[0] + "_" + tmp[1] + "_" + tmp[2] + ".idx");
			bwArray[ i ] = new BufferedWriter( 
					new FileWriter(
							tmpFile));	
		}
		//3. Go through Bing_news, find existence of each action and record in their files
		// 3.1 Initialize tools
		probaseClient = new ProbaseClient();
		lm = new Lemmatizer();
		wordnet = new Wordnet(true);
		List<String> dummyVerbs = Arrays.asList(DUMMY_VERBS);
		
		// 3.2 Preparation synonym list for all verbs
		/*
		List<List<String>> synonymList = new ArrayList<List<String>>();
		for(int i = 0; i < actionList.size(); ++i)
		{
			String[] action = actionList.get(i);
			List<String> verbSynListTmp = wordnet.getSynonymsForAllRoots(action[1], POS.VERB);
			synonymList.add(verbSynListTmp);
		}
		*/
		// 3.3 Bang!
		BufferedReader newsReader = new BufferedReader(new FileReader(BING_NEWS_CONV_URL));
		String line = null;
		String r1 = "", r2 = "";
		int findValue = 0;
		int cnt = -1, foundNumber = 0; // cnt = -1, which means the first news is numbered as 0
		while((line = newsReader.readLine())!=null)
		{
			cnt++;
			System.out.println("read:" + Integer.toString(cnt) + " found:" + foundNumber);
			
			String title = line.split("\t")[6];
			String body = line.split("\t")[7];
			List<String> tagged = lm.lemmatizeAndPOSTag(body);
			
			int paragraphLength = tagged.size();
			search:
			//for(int k = 0; k < synonymList.size(); ++k)
			for(int k = 0; k < actionList.size(); ++k)
			{
				// Load synonym list for current verb
				//List<String> verbSynTmp = synonymList.get(k);
				String currentVerb = actionList.get(k)[1];
				// traverse verbs in the body
				for(int i = 0; i < paragraphLength; ++i)	
				{
					findValue = 0;
					String[] current = tagged.get(i).split("_");
					if(!current[1].contains("V"))
						continue;
					
					if(dummyVerbs.contains(current[0]))
						continue;
					
					String firstStem = wordnet.stemVerbFirst(current[0]);
					if(firstStem == null)
						continue;
						
					//if(verbSynTmp.contains(firstStem))
					if(currentVerb.equalsIgnoreCase(firstStem))
					{
						// check if subj and obj match
						int lower = ( i - 5 < 0) ? 0 : i - 5;
						int upper = ( i + 5 > paragraphLength - 1) ? paragraphLength - 1 : i + 5;
						for( int j = lower ; j < i; ++j)
						{
							String[] focus = tagged.get(j).split("_");
							//System.out.println(focus[0] + "\t" + focus[1]);
							if(focus[1].startsWith("N") && probaseClient.isPair(actionList.get(k)[0], focus[0]))
							{
								findValue++;
								r1 = focus[0];
								break;
							}
						}
						for( int j = i + 1; j <= upper; ++j)
						{
							String[] focus = tagged.get(j).split("_");
							if(focus[1].startsWith("N") && probaseClient.isPair(actionList.get(k)[2], focus[0]))
							{
								findValue++;
								
								r2 = focus[0];
								System.out.println(">>>"+
								actionList.get(k)[0] +"\t"+ actionList.get(k)[1] + "\t" +actionList.get(k)[2]  + "\t"+
								r2+"\t"+Integer.toString(findValue));
								break;
							}
						}
						// action found,record idx
						if(findValue == 2)
						{
							foundNumber ++;
							System.out.println("<<<<"+actionList.get(k)[1]);
							bwArray[k].append(Integer.toString(cnt) + "\t" +
							firstStem + "\t" + r1 + "\t" + r2 +"\t" +actionList.get(k)[0] 
									+ "\t"+ actionList.get(k)[1]+"\n");
							
							continue search;
						}
						
					}
				}
			}
			
		}
		
		// 3.4 close IO
		newsReader.close();
		for(BufferedWriter bw : bwArray)
			bw.close();
	}
		
	
	private static void getNounFromNewsTitle() throws Exception
	{
		probaseClient = new ProbaseClient();
		BufferedReader br = new BufferedReader(new FileReader(BING_NEWS_URL));
		BufferedWriter bw = new BufferedWriter(new FileWriter(NOUN_LIST_URL));
		
		String line = null;
		int cnt = -1;
		Lemmatizer lm = new Lemmatizer();
		while((line = br.readLine())!= null)
		{
			cnt++;
			String title = line.split("\t")[6].toLowerCase();
			List<String> result = lm.lemmatizeAndPOSTag(title);
			List<String> tmp = new ArrayList<String>();
			for(String sent:  result)
			{
				String[] splitted = sent.split("_");
				if(splitted.length < 2)
					continue;
				if(splitted[1].startsWith("N"))
				{
					if(probaseClient.isGoodConcept(splitted[0]))
						tmp.add(splitted[0]);
				}
			}
			if(!tmp.isEmpty())
			{
				System.out.println(Integer.toString(cnt) + "\t" + Integer.toString(tmp.size()));
				bw.append(Integer.toString(cnt) + ":");
				for(String st : tmp)
					bw.append(st + "\t");
				bw.newLine();
			}
		}
		br.close();
		bw.close();	
	}
	
	
	private static boolean matchAction(String paragraph)throws Exception
	{
		probaseClient = new ProbaseClient();
		lm = new Lemmatizer();
		wordnet = new Wordnet(false);
		String[] action = {"country","invade","country"};
		List<String> dummyVerbs = Arrays.asList(DUMMY_VERBS);
		List<String> verbSynList = wordnet.getSynonymsForAllRoots(action[1], POS.VERB);
		
		paragraph = "United States military is occupying Iraq in this Janurary. "
				+ "This move might has profound impact on this region.";
		List<String> tagged = lm.lemmatizeAndPOSTag(paragraph);
		int paragraphLength = tagged.size();
		int findValue = 0;
		for(int i = 0; i < tagged.size(); ++i)
		{
			String[] current = tagged.get(i).split("_");
			if(!current[1].contains("V"))
				continue;
			
			if(dummyVerbs.contains(current[0]))
				continue;
			
			if(verbSynList.contains(wordnet.stemVerbFirst(current[0])))
			{
				
				int lower = ( i - 5 < 0) ? 0 : i - 5;
				int upper = ( i + 5 > paragraphLength - 1) ? paragraphLength - 1 : i + 5;
				for( int j = lower ; j < i; ++j)
				{
					String[] focus = tagged.get(j).split("_");
					if(focus[1].startsWith("N") && probaseClient.isPair("country", focus[0]))
					{
						System.out.println(focus[0] + " sub");
						findValue++;
						break;
					}
				}
				
				for( int j = i + 1; j <= upper; ++j)
				{
					String[] focus = tagged.get(j).split("_");
					if(focus[1].startsWith("N") && probaseClient.isPair("country", focus[0]))
					{
						System.out.println(focus[0]);
						findValue++;
						break;
					}
				}
				
				if(findValue == 2)
				{
					return true;
				}
				
			}
			
		}
		return false;
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
	
	private static void convert()throws Exception
	{
		BufferedReader br = new BufferedReader( new FileReader( BING_NEWS_URL) );
		BufferedWriter bw = new BufferedWriter( new FileWriter( BING_NEWS_CONV_URL ));
		String line = null;
		int cnt = 0;
		while((line = br.readLine())!=null)
		{
			System.out.println(cnt++);
			bw.append(Utility.convertFromUTF8(line));
			bw.newLine();
		}
		br.close();
		bw.close();
	}
}
