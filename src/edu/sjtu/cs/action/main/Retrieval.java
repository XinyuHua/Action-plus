package edu.sjtu.cs.action.main;
import java.io.*;
import java.util.*;

import edu.sjtu.cs.action.util.Lemmatizer;

/*
 * Author: Xinyu Hua
 * Last Modified: 2016-03-01
 * 
 * This class deals with IR methods to get the final answer, for the AC+ Applicatio.
 * It assumes existence of the action occurrence file, noun occurrence file.
 * 	- Action occurrence file should give index of news where each action is detected.
 * 	- Noun occurrence file should give index of news and noun(s) detected in the title.
 * 
 */

public class Retrieval {
	final private static String IDX_FOLDER_URL = "dat/action_occurrence_1m_bak/";
	final private static String BING_NEWS_URL = "dat/news/bing_news_1m.tsv";
	final private static String NOUN_DISTRI_URL = "dat/noun_concept/noun_in_title_1m.txt"; // news idx : noun0[\t]noun1...
	final private static String NOUN_DICT_URL = "dat/noun_concept/noun_1m.dict";
	final private static String NOUN_BODY_FREQ_URL = "dat/noun_concept/noun_body_freq_1m.txt";  // noun0: news idx0-freq0[\t]news idx1-freq1...
	final private static String NOUN_DF_URL = "dat/noun_concept/noun_df_1m.txt";
	final private static String TFIDF_URL = "dat/tfidf_1m/";
	final private static String SORTED_URL = "dat/sorted_1m/";
	final private static String TFIDF_SCHEME2_URL = "dat/tfidf_1m_scheme2/";
	final private static String SORTED_SCHEME2_URL = "dat/sorted_1m_scheme2/";
	
	private static HashMap<Integer, String[]> idxToNounMap = new HashMap<Integer, String[]>();
	private static int[] dfArray;
	private static double[] idfArray;
	private static int[][] tfMatrix;
	private static int[][] news2Nouns;
	private static String[] actionNameArray;
	private static List<String> nounList = new ArrayList<String>();
	private static HashSet<String> nounSet = new HashSet<String>();
	private static int documentSize = 0;
	private static final int actionNumber = 60;
	private static final String ACTION_URL = "dat/action/action_" + actionNumber + ".txt";
	public static void main(String[] args)throws Exception
	{
		getNounList();
		readActionNames();
		sortNoun();
		//computeIDF();
		//computeTFIDF2();
		//computeIDF();
		//computeTFIDF();
	}
	
	/*
	 * 	This function sort noun for actions, according to TFIDF value
	 * 
	 * 	dep: readActionNames();getNounList();
	 */
	private static void sortNoun() throws Exception{
		BufferedReader[] brArray = new BufferedReader[ actionNumber ];
		BufferedWriter[] bwArray = new BufferedWriter[ actionNumber ];
		String line = null;
		for(int i = 0; i < actionNameArray.length; ++i){
			System.out.println("sorting for " + actionNameArray[ i ]);
			brArray[ i ] = new BufferedReader( new FileReader(TFIDF_URL + actionNameArray[ i ] + ".tfidf"));
			HashMap<String, Double> nounTFIDFMap = new HashMap<String, Double>();
			int k = 0;
			// 1. Read tfidf data
			while((line = brArray[ i ].readLine())!=null){
				nounTFIDFMap.put(nounList.get(k), Double.parseDouble(line));
				k++;
			}
			brArray[ i ].close();
			// 2. Rank according to tfidf
			ValueComparator bvc = new ValueComparator(nounTFIDFMap);
			TreeMap<String, Double> sortedMap = new TreeMap<String, Double>(bvc);
			sortedMap.putAll(nounTFIDFMap);
			
			// 3. Write to disk
			bwArray[ i ] = new BufferedWriter(new FileWriter( SORTED_URL + actionNameArray[ i ] + ".sorted"));
			for(String nounKey : sortedMap.keySet()){
				bwArray[ i ].append(nounKey + "\t" + nounTFIDFMap.get(nounKey) + "\n");
			}
			bwArray[ i ].close();
		}
	}
	
	/*
	 * 	This function computes TF-IDF value for each noun, given action.
	 *  Different from method computeTFIDF(), this method uses the time 
	 *  noun occurs in the news body as frequency, rather than in title.
	 */
	private static void computeTFIDF2() throws Exception{
		String line = null;
		// 1. Read news to noun relations
		System.out.println("Read news to noun relations...");
		BufferedReader news2NounReader = new BufferedReader(new FileReader(NOUN_DISTRI_URL));
		news2Nouns = new int[ 1000000 ][];
		while((line = news2NounReader.readLine())!=null){
			String[] sp = line.split(":");
			int idx = Integer.parseInt(sp[0]);
			String[] nouns = sp[1].split("\\s+");
			news2Nouns[ idx ] = new int[nouns.length];
			for(int i = 0; i < nouns.length; ++i){
				news2Nouns[ idx ][ i ] = nounList.indexOf(nouns[ i ]);
			}
		}
		news2NounReader.close();
		
		// 2. Read noun to news relations
		System.out.println("Read noun to news relations...");
		BufferedReader noun2NewsReader = new BufferedReader(new FileReader(NOUN_BODY_FREQ_URL));
		List<HashMap<Integer, Integer>> noun2NewsWithFreq = new ArrayList<HashMap<Integer, Integer>>();
		while((line = noun2NewsReader.readLine())!=null){
			String[] sp = line.split(":");
			HashMap<Integer, Integer> tmp = new HashMap<Integer, Integer>();
			System.out.println(sp[0] + ":" + sp.length);
			if(sp.length > 1){
				for(String item : sp[1].split("\\s+")){
					int newsIdx = Integer.parseInt(item.split("_")[0]);
					int freq = Integer.parseInt(item.split("_")[1]);
					tmp.put(newsIdx, freq);
				}
				noun2NewsWithFreq.add(tmp);
			}
		}
		noun2NewsReader.close();
		
		// 3. Read news idx for each action
		System.out.println("computing tf...");
		BufferedReader[] brArray = new BufferedReader[ actionNumber ];
		tfMatrix = new int[ actionNumber ][];
		for(int i = 0; i < actionNameArray.length; ++i){
			System.out.println(i);
			tfMatrix[ i ] = new int[nounList.size()];
			brArray[ i ] = new BufferedReader(new FileReader( IDX_FOLDER_URL + actionNameArray[ i ] + ".idx"));
			while((line = brArray[ i ].readLine())!=null){
				int newsIdx = Integer.parseInt( line.split("\\s+")[0] );
				int[] tmp = news2Nouns[newsIdx];
				if(tmp != null){
					for(int nounIdx : tmp){
						//System.out.println(nounIdx + "\t" + newsIdx);
						//System.out.println(noun2NewsWithFreq.get(nounIdx).get(newsIdx));
						int freq = 0;
						if(noun2NewsWithFreq.get(nounIdx).get(newsIdx) != null){
							freq = noun2NewsWithFreq.get(nounIdx).get(newsIdx);
						}
						tfMatrix[ i ][nounIdx]+= freq;
					}
				}
			}
			brArray[ i ].close();
		}
		
		// 4. Write to disk
		System.out.println("Write result to disk...");
		BufferedWriter[] bwArray = new BufferedWriter[ actionNumber ];
		for(int i = 0; i < actionNameArray.length; ++i){
			bwArray[ i ] = new BufferedWriter(new FileWriter( TFIDF_SCHEME2_URL + actionNameArray[ i ] + ".tfidf"));
			for(int j = 0; j < nounList.size(); ++j){
				int tf = tfMatrix[ i ][ j ];
				bwArray[ i ].append(tf * idfArray[ j ] + "\n");
			}
			bwArray[ i ].close();
		}
		System.out.println("Finished.");
		return;
	}
	
	/*
	 * 	This function computes TF-IDF value for each noun, given action.
	 *  Here the TF value is the times a noun co-occur with an action,
	 *  they are linked by the news, if a noun present in the news title,
	 *  action is detected in the news body, then the TF + 1.
	 *  
	 *  Dep: readActionNames();getNounList();computeIDF();
	 */
	private static void computeTFIDF() throws Exception{
		String line = null;
		// 1. Read noun occurrence in news title, e.g. news_idx:noun_i[\t]noun_j...
		System.out.println("Read news to noun relations...(cost around 2 minutes)");
		BufferedReader news2NounReader = new BufferedReader(new FileReader(NOUN_DISTRI_URL));
		news2Nouns = new int[ 1000000 ][];
		while((line = news2NounReader.readLine())!=null){
			String[] sp = line.split(":");
			int idx = Integer.parseInt(sp[0]);
			String[] nouns = sp[1].split("\\s+");
			news2Nouns[ idx ] = new int[nouns.length];
			for(int i = 0; i < nouns.length; ++i){
				news2Nouns[ idx ][ i ] = nounList.indexOf(nouns[ i ]);
			}
		}
		news2NounReader.close();
		
		// 2. Read action occurrence in news body, e.g. news_idx(where the action was detected)
		System.out.println("computing tf...");
		BufferedReader[] brArray = new BufferedReader[ actionNumber ];
		tfMatrix = new int[ actionNumber ][];
		for(int i = 0; i < actionNameArray.length; ++i){
			tfMatrix[ i ] = new int[nounList.size()];
			brArray[ i ] = new BufferedReader(new FileReader( IDX_FOLDER_URL + actionNameArray[ i ] + ".idx"));
			while((line = brArray[ i ].readLine())!=null){
				int newsIdx = Integer.parseInt( line.split("\\s+")[0] );
				int[] nounIdxArray = news2Nouns[newsIdx];	
				if(nounIdxArray != null){
					for(int nounIdx : nounIdxArray){
						tfMatrix[ i ][nounIdx]++;
					}
				}
			}
			brArray[ i ].close();
		}
		
		// 3. Write to disk
		System.out.println("Write result to disk...");
		BufferedWriter[] bwArray = new BufferedWriter[ actionNumber ];
		for(int i = 0; i < actionNameArray.length; ++i){
			bwArray[ i ] = new BufferedWriter(new FileWriter( TFIDF_URL + actionNameArray[ i ] + ".tfidf"));
			for(int j = 0; j < nounList.size(); ++j){
				int tf = tfMatrix[ i ][ j ];
				bwArray[ i ].append(tf * idfArray[ j ] + "\n");
			}
			bwArray[ i ].close();
		}
		System.out.println("Finished.");
		return;
	}
	
	
	/*
	 * 	This function computes the document frequency of each noun, 
	 * 	a.k.a in how many news the noun has appeared in the title.
	 */
	private static void computeDF() throws Exception{
		dfArray = new int[nounList.size()];
		BufferedReader br = new BufferedReader(new FileReader(NOUN_DISTRI_URL));
		BufferedWriter bw = new BufferedWriter(new FileWriter(NOUN_DF_URL));
		String line = null;
		int cnt = 0;
		while((line = br.readLine())!=null){
			String[] sp1 = line.split(":");
			String[] sp2 = sp1[1].split("\\s+");
			idxToNounMap.put(Integer.parseInt(sp1[0]), sp2);
			for(String noun : sp2){
				int idx = nounList.indexOf(noun);
				dfArray[ idx ]++;
			}
			cnt++;
		}
		br.close();
		bw.append(cnt + "\n");
		for(int i = 0; i < dfArray.length; ++i){
			bw.append(nounList.get(i) + ":" + dfArray[i] + "\n");
		}
		bw.close();
	}
	
	/*
	 * 	This function computes Inverse Document Frequency for each noun,
	 *  it assumes the existence of document frequency file.
	 */
	private static void computeIDF()throws Exception
	{
		idfArray = new double[nounList.size()];
		BufferedReader br = new BufferedReader(new FileReader(NOUN_DF_URL));
		String line = null;
		line = br.readLine();
		int cnt = 0;
		documentSize = Integer.parseInt(line);
		while((line = br.readLine())!=null)
		{
			double df = Double.parseDouble(line.split(":")[1]);
			double idf = Math.log10(documentSize / df);
			idfArray[ cnt++ ] = idf;
		}
		br.close();
	}
	
	/*
	 * 	This function load all actions into actionNameArray,
	 *  e.g. country_invade_country
	 */
	private static void readActionNames()throws Exception{
		System.out.println("Read action names...");
		BufferedReader actionReader = new BufferedReader(new FileReader(ACTION_URL));
		String line = null;
		actionNameArray = new String[ actionNumber ];
		int cnt = 0;
		while((line = actionReader.readLine())!=null){
			actionNameArray[ cnt++ ] = line.replace(" ", "_");
		}
		actionReader.close();
	}
	
	/*
	 *	This function load noun dictionary into nounList and nounSet.
	 *  If the dictionary does not exist, try to build it by calling
	 *  writeNounListToDisk();
	 */
	private static void getNounList()throws Exception{
		System.out.println("Start loading noun dictionary into memory...");
		BufferedReader nounReader = new BufferedReader(new FileReader(NOUN_DICT_URL));
		String line = null;
		while((line = nounReader.readLine())!=null){
			nounList.add(line);
			nounSet.add(line);
		}
		nounReader.close();
		System.out.println("Noun dictionary loaded.");
	}
	
	/*
	 * 	This function builds up a noun-news map. e.g. door:53_1[\t]55_1,
	 *  representing the noun "door" has appeared in news with index of 
	 *  53 and 55, with frequency of 1 for both.
	 *  
	 *  This function uses nounList and nounSet, so please call getNounList()
	 *  before call this function.
	 * 
	 */
	private static void getNounOccurrence() throws Exception{
		BufferedReader newsReader = new BufferedReader(new FileReader(BING_NEWS_URL));
		BufferedWriter bw = new BufferedWriter(new FileWriter(NOUN_BODY_FREQ_URL));
		String line = null;
		Lemmatizer lm = new Lemmatizer();
		List<HashMap<Integer, Integer>> resultList = new ArrayList<HashMap<Integer, Integer>>();
		for(int i = 0; i < nounList.size(); ++i)
			resultList.add(null);
		int newsIdx = 0;
		while((line = newsReader.readLine())!=null){
			System.out.println(newsIdx);
			String body = line.split("\t")[7].toLowerCase();
			List<String> result = lm.lemmatizeAndPOSTag(body);
			List<String> tmp = new ArrayList<String>();
			for(String sent:  result){
				String[] splitted = sent.split("_");
				if(splitted.length < 2)
					continue;
				String noun = splitted[0];
				if(splitted[1].startsWith("N")){
					if(nounSet.contains(noun)){
					//	System.out.println(noun + newsIdx);
						int idx = nounList.indexOf(noun);
						HashMap<Integer, Integer> tmpMap = resultList.get(idx);
						if(tmpMap == null){
							tmpMap = new HashMap<Integer, Integer>();
							tmpMap.put(newsIdx, 1);
						}else{
							if(tmpMap.containsKey(newsIdx)){
								tmpMap.put(newsIdx, tmpMap.get(newsIdx) + 1);
							}else{
								tmpMap.put(newsIdx, 1);
							}
						}
						resultList.set(idx, tmpMap);
					}
				}
			}
			newsIdx++;
		}
		for(int i = 0; i < nounList.size(); ++i){
			String noun = nounList.get(i);
			bw.append(noun + ":");
			HashMap<Integer, Integer> tmpMap = resultList.get(i);
			if(tmpMap == null){
				bw.newLine();
				continue;
			}	
			for(Integer news : tmpMap.keySet()){
				bw.append(news + "_" + tmpMap.get(news) + "\t");
			}
			bw.newLine();
		}
		bw.close();
		newsReader.close();
	}
	
	/*
	 * This function reads noun occurrence file and output a noun dictionary,
	 * where each noun are differenct.
	 */
	private static void writeNounListToDisk()throws Exception{
		BufferedReader br = new BufferedReader(new FileReader(NOUN_DISTRI_URL));
		BufferedWriter bw = new BufferedWriter(new FileWriter(NOUN_DICT_URL));
		System.out.println("Writing noun list to disk...");
		String line = null;
		while((line = br.readLine())!=null){
			String[] sp1 = line.split(":");
			for(String noun : sp1[1].split("\t")){
				if(!nounList.contains(noun)){
					nounList.add(noun);
					bw.append(noun + "\n");
				}
			}
		}
		System.out.println("Writing finished.");
		br.close();
		bw.close();
	}
}

class ValueComparator implements Comparator<String>
{
	Map<String, Double> base;
	public ValueComparator( Map<String, Double> base)
	{
		this.base = base;
	}
	
	public int compare(String a, String b)
	{
		if(base.get(a) >= base.get(b))
			return -1;
		else 
			return 1;
	}
}