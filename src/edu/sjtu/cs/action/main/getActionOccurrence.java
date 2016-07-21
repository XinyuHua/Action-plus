package edu.sjtu.cs.action.main;
import java.util.*;
import java.io.*;

import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.util.Action;
import edu.sjtu.cs.action.util.Lemmatizer;
import edu.sjtu.cs.action.util.Pair;

public class getActionOccurrence implements Runnable {
	private static String OUT_PATH = "dat/action_occurrence_tmp/";
	final private static String BING_NEWS_PARSED_URL = "dat/news/bing_news_sliced_parsed/";
	final private static String BING_NEWS_POSTAG_URL = "dat/news/bing_news_sliced_postag/";

	private static String[] pronArray = {"he","she","him","her","i","me","you","they","them","his","mine","its","theirs","yours"};
	private  ProbaseClient pb;
	private static Wordnet wn;
	private static List<String> puncList;
	private static List<String> pronList;
	private static List<Set<String>> inflectionList; 
	private static HashSet<String> inflectionSet;
	private static HashSet<String> dummyVerbs;
	private static List<String[]> actionList;
	private static HashSet<String> verbTagSet;
	private int part;
	private boolean searchAll = false;
	
	public getActionOccurrence( int part, ProbaseClient pb, Wordnet wn, List<String[]> al,  List<Set<String>> il,
			HashSet<String> is,	List<String> pl, HashSet<String> dummyVerbSet, HashSet<String> verbTagSet, String outPath){
		this.pb = pb;
		this.wn = wn;
		this.dummyVerbs = dummyVerbSet;
		this.verbTagSet = verbTagSet;
		this.actionList = al;
		this.inflectionList = il;
		this.inflectionSet = is;
		this.puncList = pl;
		this.part = part;
		this.OUT_PATH = outPath;
		pronList = Arrays.asList(pronArray);
	}

	public void run(){
		File fileToWrite = new File(OUT_PATH + part + ".txt");
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileToWrite));
			BufferedReader parsedNewsReader = new BufferedReader(new FileReader(BING_NEWS_PARSED_URL + "bing_news_parsed_" + part + ".txt"));
			BufferedReader postagNewsReader = new BufferedReader(new FileReader(BING_NEWS_POSTAG_URL + "bing_news_pos_" + part + ".txt"));
			String line = null;
			int foundNumber = 0; // cnt = -1, which means the first news is numbered as 0
			int newsIdx = -1;
			int oldIdx = -1;
			while((line = parsedNewsReader.readLine())!=null){
				newsIdx = Integer.parseInt(line.split("\t")[0]);
				String parsed = line.split("\t")[1];
				line = postagNewsReader.readLine();
				String postag = line.split("\t")[1];
				if(newsIdx % 1000 == 0){
					System.out.println("part:" + part +" read:" + newsIdx + " found:" + foundNumber);
				}

				Object[] rst = extractActionFromSentence(parsed, postag, foundNumber);
				String toWrite = (String) rst[0];
				foundNumber = (int) rst[1];
				if(toWrite.length() != 0){
					if(oldIdx == newsIdx){
						bw.append(toWrite);
					}else{
						bw.newLine();
						bw.append(newsIdx + toWrite);
						oldIdx = newsIdx;
					}
					bw.flush();
				}
			}

			parsedNewsReader.close();
			postagNewsReader.close();
			bw.close();
			pb.disconnect();
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println(e);
		}
	}

	private Object[] extractActionFromSentence(String parsed, String postag, int foundNumber)throws Exception{
		String[] posStr = postag.trim().replaceAll("\\[|\\]","").split(",\\s+");
		String[] parsedStr = parsed.trim().split("\\)");
		List<Set<Pair>> dependencyTree = new ArrayList<>();
		for(int i = 0; i < posStr.length + 1; ++i){
			dependencyTree.add(new HashSet<>());
		}

		for(String parsedPart : parsedStr ) {
			String dep = parsedPart.substring(0, parsedPart.indexOf("(")).trim();
			String content = parsedPart.substring(parsedPart.indexOf("(") + 1).trim();
			String[] sp = content.split(",\\s+");
			int left = Integer.parseInt(sp[0].substring(sp[0].lastIndexOf("-") + 1));
			int right = Integer.parseInt(sp[1].substring(sp[1].lastIndexOf("-") + 1));
			Set<Pair> set = dependencyTree.get(left);
			set.add(new Pair(dep,right));
		}


	        /*
	         * Initialize verbIds list and tokens list.
	         * verbIds list stores indeces of verbs, tokens list stores tokens
	         * in the original sentence, in the original order.
	         */
		int cnt = 1;
		List<Integer> verbIds = new ArrayList<Integer>();
		List<String> tokens = new ArrayList<String>();
		tokens.add("ROOT");
		for(String pos : posStr){
			pos = pos.trim();
			String sp[] = pos.split("/");
			String token = sp[0];
			String TAG = sp[1];
			if(verbTagSet.contains(TAG) && !dummyVerbs.contains(token)){
				verbIds.add(cnt);
			}
			tokens.add(token);
			cnt++;
		}

	        /*
	         * Search actions in the document.
	         */
		List<Action> resultActionList = searchAction(tokens, dependencyTree, verbIds);
		Object[] output;
		if(searchAll) {
			output = writeActionAll(resultActionList, foundNumber);
		} else {
			output = writeActionForKnown(resultActionList, foundNumber);
		}

		return output;

	}

	private Object[] writeActionAll(List<Action> resultActionList, int foundNumber)throws Exception{
		String toWrite = "";
		for(Action ac : resultActionList){
			foundNumber++;
			toWrite += "\t" + ac.toString();
		}
		Object[] result = new Object[2];
		result[0] = toWrite;
		result[1] = foundNumber;
		return result;
	}

	private Object[] writeActionForKnown(List<Action> resultActionList, int foundNumber)throws Exception{
		String toWrite = "";
		for(Action ac : resultActionList){
			String subj = ac.getSubj();
			String verb = ac.getVerb();
			String obj = ac.getObj();

			if(!inflectionSet.contains(verb)){
				continue;
			}
			//System.out.println(subj + " " + verb + " " +obj);
			for(int i = 0; i < actionList.size(); ++i){
				String[] action = actionList.get(i);
				Set<String> inflect = inflectionList.get(i);
				if(inflect.contains(verb)){
					//System.out.println(action[0] + "-" + subj);
					if(pb.isPair(action[0], subj) && pb.isPair(action[2], obj)){
						foundNumber ++;
						toWrite += "\t" + action[0] + "_" + action[1] + "_" + action[2];
						toWrite += "(" + ac.toString() + ")";
					}
				}
			}
		}

		Object[] result = new Object[2];
		result[ 0 ] = toWrite;
		result[ 1 ] = foundNumber;
		return result;
	}

	/*
     * This method search actions in the document, it starts searching from
     * each verb stored in the verbIds list.
     */
	private List<Action> searchAction(List<String> tokens, List<Set<Pair>> dependencyTree, List<Integer> verbIds) throws Exception{
		List<Action> output = new ArrayList<>();
		for(Integer id : verbIds){
			output.addAll(searchActionForVerb(tokens,dependencyTree,id));
		}
		return output;
	}


	/*
     * This method search action given a certain verbId, it might find multiple actions given a single verb.
     */
	private List<Action> searchActionForVerb(List<String> tokens, List<Set<Pair>> dependencyTree, int verbId) throws Exception{
		String verb= tokens.get(verbId);
		//System.out.println(verb);
		Set<Pair> verbChildrenSet = dependencyTree.get(verbId);
		List<Action> output = new ArrayList<>();

		/*
		 * Traverse children of the verb node, search for subjects and objects.
		 */
		List<Integer> subjs = new ArrayList<>();
		List<Integer> objs = new ArrayList<>();
		boolean isPassive = false;
		List<Integer> buffer = new ArrayList<>();

		for(Pair verbRelatedPair : verbChildrenSet){
			if(verbRelatedPair.getDep().equals("nsubj")){
				int id = verbRelatedPair.getPos();
				subjs.add(id);
			}else if(verbRelatedPair.getDep().equals("nmod")){
				int id = verbRelatedPair.getPos();
				buffer.add(id);
			}else if(verbRelatedPair.getDep().equals("nsubjpass")){
				int id = verbRelatedPair.getPos();
				isPassive = true;
				objs.add(id);
			}else if(verbRelatedPair.getDep().equals("dobj")){
				int id = verbRelatedPair.getPos();
				objs.add(id);
			}
		}

		if(isPassive) {
			subjs.addAll(buffer);
		}
		for(Integer subj : subjs){
			String subject = getSubTree(dependencyTree, tokens, subj);
			for(Integer obj : objs){
				String object= getSubTree(dependencyTree, tokens, obj);
				if(subject.equals("") || object.equals("")){
					continue;
				}
				Action tmp = new Action(subject, verb, object);
				output.add(tmp);
			}
		}
		return output;
	}

	/*
     * This method search subtree of a given id, and output the longest probase entity as a string
     */
	private String getSubTree(List<Set<Pair>> dependencyTree, List<String> tokens, int id) throws Exception {
		String head = tokens.get(id);
		if(pronList.contains(head)) {
			return "person";
		}
		String stem = wn.stemNounFirst( head );
		// Be careful when change head word to its stem, because wn might fail to find the right stem
		if(!head.toLowerCase().equals(stem) && !stem.equals("")){
			head = stem;
		}
		String result = head;
		Set<Pair> subTree = dependencyTree.get(id);
		for (Pair subTreePair : subTree) {
			String dep = subTreePair.getDep();
			if (dep.equals("nmod")) {
				Integer subHead = subTreePair.getPos();
				Set<Pair> subSubTree = dependencyTree.get(subHead);
				for (Pair subSubPair : subSubTree) {
					if (subSubPair.getDep().equals("case")) {
						if (tokens.get(subSubPair.getPos()).equals("of")) {
							result = tokens.get(subTreePair.getPos());
							return result;
						}
					}
				}
			}
			if (dep.equals("compound") || dep.equals("amod")) {
				String modifier = tokens.get(subTreePair.getPos());
				if (pb.isProbaseEntity(modifier + " " + head)) {
					result = modifier + " " + head;
				}
			}

			if(dep.equals("case")) {
				if(tokens.get(subTreePair.getPos()).equals("in")) {
					return "";
				}
			}
		}
		return result;
	}
}
