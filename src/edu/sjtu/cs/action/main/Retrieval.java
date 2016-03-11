package edu.sjtu.cs.action.main;
import java.io.*;
import java.util.*;

import edu.sjtu.cs.action.util.Lemmatizer;

/*
 * Author: Xinyu Hua
 * Last Modified: 2016-03-07
 * 
 * This class deals with IR methods to get the final answer, for the AC+ Applicatio.
 * It assumes existence of the action occurrence file, noun occurrence file.
 * 	- Action occurrence file should give index of news where each action is detected.
 * 	- Noun occurrence file should give index of news and noun(s) detected in the title.
 * 
 */

public class Retrieval {
	
	// Directories to write results
	final private static String[] DICT_DAT = {"ps", "wn"};
	final private static String[] CLUSTERITY = {"clustered", "unclustered"};
	final private static int DICT_INDEX = 0;
	final private static int CORPUS_INDEX = 1;
	final private static int CLUSTERITY_INDEX = 1;
	private static String RESULT_URL = "dat/results_" + DICT_DAT[DICT_INDEX] + "/corpus_" + CORPUS_INDEX + "/" + CLUSTERITY[ CLUSTERITY_INDEX ] + "/";
	private static String TFIDF_URL = RESULT_URL + "tfidf/";
	private static String SORTED_URL = RESULT_URL + "sorted/";
	
	// Directories to read data
	final private static String NOUN_OCCURRENCE = "dat/noun_occurrence_" +  DICT_DAT[DICT_INDEX] + "/";
	final private static String ACTION_OCCURRENCE = "dat/action_occurrence_" + CORPUS_INDEX + "/"+ CLUSTERITY[ CLUSTERITY_INDEX ] + "/";
	final private static String NOUN_DICT = "dat/noun_concept/noun_" + DICT_DAT[DICT_INDEX] + ".dict";
	final private static String NOUN_DF = "dat/noun_concept/noun_" + DICT_DAT[DICT_INDEX] + ".df";
	/*
	final private static String IDX_FOLDER_URL = "dat/action_occurrence_2/";
	final private static String NOUN_DICT_URL = "dat/noun_concept/noun.act.dict";
	final private static String NOUN_TITLE_PART_URL = "dat/noun_occurrence/";
	final private static String NOUN_TITLE_URL = "dat/noun_concept/actional_in_title_all.txt";
	final private static String NOUN_DF_URL = "dat/noun_concept/actional_df_all.txt";
	
	final private static String TFIDF_URL = "dat/tfidf_all_2/";
	final private static String SORTED_URL = "dat/sorted_all_2/";
	final private static String SORTED_CLT_URL = "dat/sorted_all_clustered/";
	final private static String TFIDF_CLT_URL = "dat/tfidf_all_clustered/";
	final private static String CLUSTERED_URL = "dat/news_for_action_clustered/";
	*/
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
	private static final int newsNumber = 13416252;
	private static final String ACTION_URL = "dat/action/action_" + actionNumber + ".txt";
	public static void main(String[] args)throws Exception{
		getNounList();
		readActionNames();
		//computeDF();
		//computeTFIDF();
		sortNoun();
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
	/*
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
	*/
	
	/*
	 * 	This function computes TF-IDF value for each noun, given action.
	 *  Here the TF value is the times a noun co-occur with an action,
	 *  they are linked by the news, if a noun present in the news title,
	 *  action is detected in the news body, then the TF + 1.
	 *  
	 *  Dep: readActionNames();getNounList();computeIDF();
	 */
	public static void computeTFIDF() throws Exception{
		computeIDF();
		String line = null;
		// 1. Read noun occurrence in news title, e.g. news_idx:noun_i[\t]noun_j...
		System.out.println("Read news to noun relations...(cost around 2 minutes)");
		news2Nouns = new int[ newsNumber ][];
		tfMatrix = new int[ actionNumber ][];
		
		// 2. Read news2Nouns, which is a 2-d array contains nouns in news title.
		for(int part = 0; part < 100; ++part){
			System.out.println("reading noun occurrence..." + part );
			BufferedReader news2NounReader = new BufferedReader(new FileReader(NOUN_OCCURRENCE +"noun_in_title_"  + part + ".txt"));
			while((line = news2NounReader.readLine())!=null){
				String[] sp = line.split(":");
				int idx = Integer.parseInt(sp[0]);
				String[] nouns = sp[1].split("\\s+");
				news2Nouns[ idx ] = new int[nouns.length];
				for(int j = 0; j < nouns.length; ++j){
					news2Nouns[ idx ][ j ] = nounList.indexOf(nouns[ j ]);
				}
			}
			news2NounReader.close();
		}
		
		// 3. Initialize tfMatrix, which is used to store tf values
		for(int k = 0; k < actionNameArray.length; ++k){
			tfMatrix[ k ] = new int[nounList.size()];
		}
		
		// 4. Compute tf values
		BufferedReader[] brArray = new BufferedReader[ actionNumber ];
		for(int k = 0; k < actionNameArray.length; ++k){
			System.out.println("computing tf..." + k );
			brArray[ k ] = new BufferedReader(new FileReader(  ACTION_OCCURRENCE +  "/" +actionNameArray[ k ] + ".idx"));
			while((line = brArray[ k ].readLine())!=null){
				int newsIdx = Integer.parseInt( line.split("\\s+")[0] );
				int[] nounIdxArray = news2Nouns[newsIdx];	
				if(nounIdxArray != null){
					for(int nounIdx : nounIdxArray){
						tfMatrix[ k ][nounIdx]++;
					}
				}
			}
			brArray[ k ].close();
		}
		
		// 5. Write to disk
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
	public static void computeDF() throws Exception{
		dfArray = new int[nounList.size()];
		BufferedWriter bw = new BufferedWriter(new FileWriter(NOUN_DF));
		String line = null;
		int cnt = 0;
		for(int part = 0; part < 100; ++part){
			BufferedReader br = new BufferedReader(new FileReader(NOUN_OCCURRENCE + "noun_in_title_" + Integer.toString(part) + ".txt"));
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
		}
		
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
	private static void computeIDF()throws Exception{
		idfArray = new double[nounList.size()];
		BufferedReader br = new BufferedReader(new FileReader(NOUN_DF));
		String line = null;
		line = br.readLine();
		int cnt = 0;
		documentSize = Integer.parseInt(line);
		while((line = br.readLine())!=null){
			double df = Double.parseDouble(line.split(":")[1]);
			if(df == 0.0){
				idfArray[cnt++] = 0;
			}else{
				double idf = Math.log10(documentSize / df);
				idfArray[ cnt++ ] = idf;
			}
		}
		br.close();
	}
	
	/*
	 * 	This function load all actions into actionNameArray,
	 *  e.g. country_invade_country
	 */
	public static void readActionNames()throws Exception{
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
	public static void getNounList()throws Exception{
		System.out.println("Start loading noun dictionary into memory...");
		BufferedReader nounReader = new BufferedReader(new FileReader(NOUN_DICT));
		String line = null;
		while((line = nounReader.readLine())!=null){
			line = line.toLowerCase();
			nounList.add(line);
			nounSet.add(line);
		}
		nounReader.close();
		System.out.println("Noun dictionary loaded.");
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