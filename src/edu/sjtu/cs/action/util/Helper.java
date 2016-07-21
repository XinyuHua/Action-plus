package edu.sjtu.cs.action.util;
import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.main.*;
import java.io.*;
import java.sql.SQLSyntaxErrorException;
import java.util.*;

public class Helper {

	final private static String BING_NEWS_URL = "dat/news/bing_news_ready.tsv";
	final private static String BING_NEWS_UNIQUE_URL = "dat/news/bing_news_unique.tsv";
	final private static String BING_NEWS_SLICED_URL = "dat/news/bing_news_sliced/";
	final private static String NOUN_OCC_URL = "dat/occurrence/noun_occurrence/";
	final private static String ACTION_OCC_URL = "dat/occurrence/action_occurrence/";
	final private static String[] VERB_TAG = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};
	final private static String NOUN_EXAM_URL = "dat/noun_concept/noun.exam";
	final private static String EXAM_IDX_URL = "dat/exam/index/";
	final private static String EXAM_OUT_URL = "dat/exam/news/";
	final private static String EVAL_URL = "dat/evaluation/";
	private static final String ABC_NEWS_PATH = "dat/evaluation/abc_news/eval_package/abc_news/";
	private static final String ABC_LEMMATIZE_PATH = "dat/evaluation/abc_news/eval_package/abc_news_lemma/";

	// Files necessary to find action instances for action classes
	private static final String AC_EVAL_LIST_URL = "dat/action/concept_100.txt";
	private static final String INSTANCE_OCC_URL = "dat/tmp/instance_occurrence_tmp/";
	private static final String INSTANCE_IN_AC_URL = "dat/tmp/instance_in_action_tmp/";
	private static final String CONCEPT_WITH_INSTANCE_URL = "dat/tmp/concept_occurrence_tmp/eval/";
	private static HashMap<String, List<String>> ac2instance;

	final private static int offset = 55914;
	static ProbaseClient pb;
	static Lemmatizer lemmatizer;
	private static String[] DUMMY_VERBS = {"be","am","is","are","was","were","have","has","had","get","got","gets"};
	private static List<String> conceptEvalList;

	public static void main(String[] args)throws Exception{
	//	findExampleNews();
		findInstanceForAction();
	}

	/*
	This method output the list of instances for a each of the action classes specified in AC_EVAL_LIST_URL
	 */
	public static void findInstanceForAction()throws Exception{

		ac2instance = new HashMap<>();
		// Load concept eval data
		BufferedReader evalReader = new BufferedReader(new FileReader(AC_EVAL_LIST_URL));
		String line;
		conceptEvalList = new ArrayList<>();
		while((line = evalReader.readLine())!=null){
			String ac = line.split(":")[0];
			conceptEvalList.add(ac);
		}
		evalReader.close();

		// Go through all the instance data
		for(int i = 0; i < 100; ++i) {
			BufferedReader instReader = new BufferedReader(new FileReader(CONCEPT_WITH_INSTANCE_URL + i + ".txt"));
			while ((line = instReader.readLine()) != null) {
				String[] lineSplit = line.split("\t");
				for (int pairIdx = 1; pairIdx < lineSplit.length; pairIdx++) {
					String aiPair = lineSplit[pairIdx];
					String action = aiPair.substring(0, aiPair.indexOf("("));
					String instance = aiPair.substring(aiPair.indexOf("(") + 1, aiPair.indexOf(")"));
					if (!ac2instance.containsKey(action)) {
						ac2instance.put(action, new ArrayList<>());
					}
					ac2instance.get(action).add(instance);
				}
			}
			instReader.close();
		}


		// Rank and write to file
		int acIdx = 0;
		int acTotal = ac2instance.size();
		for(String ac : ac2instance.keySet()){
			System.out.println("Writing " + acIdx + "th(" + acTotal + ") ac to disk...");
			acIdx ++;
			List<String> instanceList = ac2instance.get(ac);
			HashMap<String, Integer> freqMap = new HashMap<>();
			for(String s : instanceList){
				if(!freqMap.containsKey(s)){
					freqMap.put(s,1);
				}else{
					freqMap.put(s, freqMap.get(s) + 1);
				}
			}

			ValueComparatorInt bvc = new ValueComparatorInt(freqMap);
			TreeMap<String, Integer> result = new TreeMap<>(bvc);
			result.putAll(freqMap);
			BufferedWriter bw = new BufferedWriter(new FileWriter(INSTANCE_IN_AC_URL + ac + ".txt"));

			for(String s : result.keySet()){
				bw.append(s + "\t" + freqMap.get(s));
				bw.newLine();
			}
			bw.close();
		}

	}


	public static String getClassFromInstance(String instance) throws Exception{
		String result = "";
		String[] splitted = instance.split("_");
		String verb = splitted[1];
		for(String ac : conceptEvalList){
			String[] acSplit = ac.split("_");
			String acVerb = acSplit[1];

			if(!acVerb.equals(verb)) continue;

			if(splitted.length == acSplit.length){
				if(splitted.length == 3){
					if(pb.getPop(splitted[0], acSplit[0]) > 10 && pb.getPop(splitted[2], acSplit[2]) > 10){
						result = ac;
						break;
					}
				}else{
					if(pb.getPop(splitted[0], acSplit[0]) > 10){
						result = ac;
						break;
					}
				}
			}
		}
		return result;
	}

	public static void findExampleNews()throws Exception{
		BufferedReader br = new BufferedReader(new FileReader("dat/news/bing_news_sliced/bing_news_5.tsv"));
		String line = null;
		boolean foundInTitle = false;
		int lineNum = 0;
		while((line = br.readLine())!=null){
			lineNum += 1;
			foundInTitle = false;
			String[] lineSplit = line.split("\t");
			String title = lineSplit[ 0 ];
			String body = lineSplit[ 1 ];
			for(String s :title.split("\\s+")){
				if(s.toLowerCase().equals("disaster")){
					foundInTitle = true;
					System.out.println(title);
					break;
				}
			}

			if(foundInTitle){
				for(String s : body.split("\\s+")){
					if(s.toLowerCase().contains("destro")){
						System.out.println(lineNum);
						break;
					}
				}
			}
		}
		br.close();
	}


	/*
	 * This method parse bing news(body part), and also produce postag infomation
	 */



	public static void lemmatizeFile()throws Exception{

		BufferedReader listBr = new BufferedReader(new FileReader(EVAL_URL + "eval_10.txt"));
		String line;
		while((line = listBr.readLine())!=null){
			BufferedReader posBr = new BufferedReader(new FileReader(EVAL_URL + "groundtruth/" + line + ".pos"));
			BufferedWriter posLemWriter = new BufferedWriter(new FileWriter(EVAL_URL + "groundtruth_lemma/" + line + ".poslemma"));
			String innerLine = null;
			while((innerLine = posBr.readLine())!=null){
				List<String> lemmatized = lemmatizer.lemmatize(innerLine);
				for(String lemma : lemmatized) {
					if(lemma.contains("www") || lemma.contains(".com") || lemma.length() <= 2) {
						continue;
					}

					if(lemma.replaceAll("[^a-zA-Z]","").length() != 0) {
						if(lemma.contains("/")) {
							String[] splitted = lemma.split("/");
							for(String s : splitted) {
								posLemWriter.append(s + " ");
							}
						} else {
							posLemWriter.append(lemma + " ");
						}
					}
				}
				posLemWriter.newLine();
			}
			posLemWriter.close();
			posBr.close();
			System.out.println(line + ".pos finished.");

			BufferedReader negBr = new BufferedReader(new FileReader(EVAL_URL + "groundtruth/" + line + ".neg"));
			BufferedWriter negLemWriter = new BufferedWriter(new FileWriter(EVAL_URL + "groundtruth_lemma/" + line + ".neglemma"));

			while((innerLine = negBr.readLine())!=null){
				List<String> lemmatized = lemmatizer.lemmatize(innerLine);
				for(String lemma : lemmatized) {
					if(lemma.contains("www") || lemma.contains(".com") || lemma.length() <= 2) {
						continue;
					}

					if(lemma.replaceAll("[^a-zA-Z]","").length() != 0) {
						if(lemma.contains("/")) {
							String[] splitted = lemma.split("/");
							for(String s : splitted) {
								negLemWriter.append(s + " ");
							}
						} else {
							negLemWriter.append(lemma + " ");
						}
					}
				}
				negLemWriter.newLine();
			}
			negLemWriter.close();
			negBr.close();
			System.out.println(line + ".neg finished.");
		}
		listBr.close();
	}

	public static void searchIndex()throws Exception{
		String toSearch = "Amateur historian seeks Otter Lake";
		int globalIdx = 0;
		String line = null;
		for(int i = 0; i < 99; ++i) {
			System.out.println(i);
			BufferedReader br = new BufferedReader(new FileReader(BING_NEWS_SLICED_URL + "bing_news_" + i + ".tsv"));
			while((line = br.readLine())!=null) {
				String title = line.split("\t")[0];
				if(title.contains(toSearch)) {
					System.out.println("-----Found-----" + globalIdx + "---------------");
					return;
				}
				globalIdx++;
			}
			br.close();
		}
	}

	public static void getRandomIdxToExam()throws Exception{

		/*
		Load noun list to toShuffleMap
		 */
		System.out.println("Loading noun list...");
		HashMap<String, List<Integer>> toShuffleMap = new HashMap<>();
		BufferedReader examFileReader = new BufferedReader(new FileReader(NOUN_EXAM_URL));
		String line = null;
		while((line = examFileReader.readLine())!=null) {
			if(line.equals(""))
				continue;
			toShuffleMap.put(line, new ArrayList<>());
		}
		examFileReader.close();

		/*
		Load indeces from noun
		 */
		System.out.println("Loading original indeces...");
		for(String noun : toShuffleMap.keySet()) {
			String fileName = EXAM_IDX_URL + noun + ".idx";
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			while((line = br.readLine())!=null) {
				Integer idx = Integer.parseInt(line);
				toShuffleMap.get(noun).add(idx);
			}
			br.close();
		}

		HashSet<Integer> idxSet = new HashSet<>();
		HashMap<Integer, List<Integer>> newsIdxMap = new HashMap<>();
		Random randomGenerator = new Random();
		int nounIdx = 0;
		for(String noun : toShuffleMap.keySet()) {
			System.out.println("Get random for " + nounIdx);
			List<Integer> fullIdxList = toShuffleMap.get(noun);
			HashSet<Integer> localIdxSet = new HashSet<>();
			for(int i = 0; i < 10; ++i){
				int random = randomGenerator.nextInt(fullIdxList.size());
				localIdxSet.add(random);
			}

			BufferedReader br = new BufferedReader(new FileReader(EXAM_IDX_URL + noun + ".idx"));
			int cnter = 0;
			while((line = br.readLine()) != null) {
				if(localIdxSet.contains(cnter)) {
					int index = Integer.parseInt(line);
					idxSet.add(index);
					if(!newsIdxMap.containsKey(index)) {
						newsIdxMap.put(index, new ArrayList<>());
					}
					newsIdxMap.get(index).add(nounIdx);
				}
				cnter++;
			}
			br.close();
			nounIdx++;
		}
		System.out.println("Map size = " + newsIdxMap.size());

		List<Integer> orderdIndexSet = new ArrayList<>();
		orderdIndexSet.addAll(idxSet);
		Collections.sort(orderdIndexSet);
		System.out.println(orderdIndexSet.size() + " news to be loaded.");
		List<String> newsList = new ArrayList<>();
		int fileId = 0;
		int cnter = -1;
		System.out.println("Reading news...");
		BufferedReader br = new BufferedReader(new FileReader(BING_NEWS_SLICED_URL + "bing_news_" + fileId + ".tsv"));
		for(int idx : orderdIndexSet){
			int curFileId = idx / offset;
			int curLineId = idx % offset;
			if(curFileId != fileId) {
				br.close();
				System.out.println("traversing " + curFileId + "th news...");
				br = new BufferedReader(new FileReader(BING_NEWS_SLICED_URL + "bing_news_" + curFileId + ".tsv"));
				fileId = curFileId;
				cnter = -1;
			}

			while((line = br.readLine()) != null) {
				cnter++;
				if(cnter == curLineId) {
					newsList.add(line);
					break;
				}
			}

		}

		System.out.println(newsList.size() + " news loaded.");
		BufferedWriter[] bwArray = new BufferedWriter[toShuffleMap.size()];
		cnter = 0;
		for(String noun : toShuffleMap.keySet()){
			String fileName = EXAM_OUT_URL + noun + ".news";
			bwArray[ cnter ] = new BufferedWriter(new FileWriter(fileName));
			cnter++;
		}
		System.out.println("Writing to disk...");
		int i = 0;
		for(int idx : orderdIndexSet) {

			List<Integer> nounsToWrite = newsIdxMap.get(idx);
			System.out.println(idx +"th news has " + nounsToWrite.size() + " nous to write.");
			for(Integer j : nounsToWrite) {
				bwArray[ j ].append(newsList.get(i));
				System.out.println("Writing...");
				bwArray[ j ].newLine();
			}
			i++;
		}

		for(int j = 0; j < bwArray.length; ++j){
			bwArray[ j ].close();
		}
	}


}

