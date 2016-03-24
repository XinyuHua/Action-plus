package edu.sjtu.cs.action.util;
import java.io.*;
import java.util.*;

public class Helper {

	final private static String BING_NEWS_URL = "dat/news/bing_news_ready.tsv";
	final private static String BING_NEWS_UNIQUE_URL = "dat/news/bing_news_unique.tsv";
	final private static String NOUN_OCC_URL = "dat/noun_occurrence/";
	final private static String ACTION_OCC_URL = "dat/action_occurrence/";
	final private static String REDUCED_DIR_URL = "dat/reduced/";
	final private static String[] VERB_TAG = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};
	private static String[] DUMMY_VERBS = {"be","am","is","are","was","were","have","has","had","get","got","gets"};
	public static void main(String[] args)throws Exception{
		extractActionDemo();
	}
	
	public static void extractActionDemo()throws Exception{
		String subjects[] = {"Russia", "United States", "United Kingdom and France", "He"};
		String objects[] = {"Iraq", "Saudi Arabia", "Iraq and Saudi Arabia", "big dog"};
		String verbs[] = {" invaded ", " has invaded ", " was invaded by ", " eats "};
		
		String sentence = subjects[3] + verbs[3] + objects[3] + ".";
		
		/*
		 * Initialize verbTagSet and dummyVerbSet.
		 * verbTagSet stores postag labels used to represent verbs.
		 * dummyVerbSet stores inflections of verbs that should be ignored.
		 */
				
		HashSet<String> verbTagSet = new HashSet<String>();
		HashSet<String> dummyVerbSet = new HashSet<String>();
		for(String s : VERB_TAG){
			verbTagSet.add(s);
		}
		for(String s : DUMMY_VERBS){
			dummyVerbSet.add(s);
		}
		System.out.println("Sentence: " + sentence);
		
		/*
		 * Parse and postag document. Results are stored in two lists.
		 * Each sentence in the document is represented by dependency tree as
		 * an element in the first list, and is represented by postag labels 
		 * as and element in the second list.
		 */
		Parser parser = new Parser();
		List<List<String>> result = parser.dependencyParseDocument(sentence);
		//System.out.println("result size:" + result.size());
		List<String> parsed_result = result.get(0);
		List<String> postag_result = result.get(1);
		//System.out.println("parsed size: " + parsed_result.size());
		String depStr = parsed_result.get(0);
		String[] posStr = postag_result.get(0).replaceAll("\\[|\\]","").split(",\\s+");
		
		/*
		 *  Initialize dependency tree. And fill in indeces accordingly.
		 *  Elements in dependency tree are in the format: Pair(dep, idx)
		 */
		List<Set<Pair>> dependencyTree = new ArrayList<Set<Pair>>();
		for(int i = 0; i < posStr.length + 1; ++i){
			dependencyTree.add(new HashSet<Pair>());
		}
		
		for(String parsedPart : depStr.split("\\) ")){
			String dep = parsedPart.substring(0, parsedPart.indexOf("("));
			String content = parsedPart.substring(parsedPart.indexOf("(") + 1);
			String[] sp = content.split(",\\s+");
			int left = Integer.parseInt(sp[0].substring(sp[0].indexOf("-") + 1));
			int right = Integer.parseInt(sp[1].substring(sp[1].indexOf("-") + 1));
			Set<Pair> set = dependencyTree.get(left);
			set.add(new Pair(dep,right));
		}
		
		/*
		 * Display dependency tree
		 */
		int cnt = 0;
		for(Set<Pair> set : dependencyTree){
			System.out.print(cnt + "\t");
			for(Pair p : set){
				System.out.print(p + " ");
			}
			System.out.println();
			cnt++;
		}
		
		/*
		 * Initialize verbIds list and tokens list.
		 * verbIds list stores indeces of verbs, tokens list stores tokens 
		 * in the original sentence, in the original order. 
		 */
		cnt = 1;
		List<Integer> verbIds = new ArrayList<Integer>();
		List<String> tokens = new ArrayList<String>();
		tokens.add("ROOT");
		for(String pos : posStr){
			pos = pos.trim();
			String sp[] = pos.split("/");
			String token = sp[0];
			String TAG = sp[1];
			if(verbTagSet.contains(TAG) && !dummyVerbSet.contains(token)){
				verbIds.add(cnt);
			}
			tokens.add(token);
			cnt++;
		}
		
		/*
		 * Search actions in the document.
		 */
		List<Action> actionList = searchAction(tokens, dependencyTree, verbIds);
		for(Action ac : actionList){
			System.out.println(ac);
		}
	}
	
	/*
	 * This method search actions in the document, it starts searching from 
	 * each verb stored in the verbIds list.
	 */
	private static List<Action> searchAction(List<String> tokens, List<Set<Pair>> dependencyTree, List<Integer> verbIds){
		List<Action> output = new ArrayList<Action>();
		for(Integer id : verbIds){
			output.addAll(searchActionForVerb(tokens,dependencyTree,id));
		}
		return output;
	}
	
	/*
	 * This method search action given a certain verbId, it might find multiple actions given a single verb.
	 */
	private static List<Action> searchActionForVerb(List<String> tokens, List<Set<Pair>> dependencyTree, int verbId){
		String verb= tokens.get(verbId);
		Set<Pair> verbChildrenSet = dependencyTree.get(verbId);
		List<Action> output = new ArrayList<Action>();

		/* 
		 * Traverse children of the verb node, search for subjects and objects.
		 */
		List<Integer> subjs = new ArrayList<Integer>();
		List<Integer> objs = new ArrayList<Integer>();
		
		for(Pair verbRelatedPair : verbChildrenSet){
			if(verbRelatedPair.getDep().equals("nsubj")){
				int id = verbRelatedPair.getPos();
				subjs.add(id);
			}else if(verbRelatedPair.getDep().equals("nmod")){
				int id = verbRelatedPair.getPos();
				subjs.add(id);
			}else if(verbRelatedPair.getDep().equals("nsubjpass")){
				int id = verbRelatedPair.getPos();
				objs.add(id);
			}else if(verbRelatedPair.getDep().equals("dobj")){
				int id = verbRelatedPair.getPos();
				objs.add(id);
			}
		}
		
		for(Integer subj : subjs){
			String subject = getSubTree(dependencyTree, tokens, subj);
			for(Integer obj : objs){
				String object= getSubTree(dependencyTree, tokens, obj);
				Action tmp = new Action(subject, verb, object);
				output.add(tmp);
			}
		}
		return output;
	}
	
	/*
	 * This method search subtree of a given id, and output the longest probase entity as a string
	 */
	private static String getSubTree(List<Set<Pair>> dependencyTree, List<String> tokens, int id){
		String head = tokens.get(id);
		String result = head;
		Set<Pair> subTree = dependencyTree.get(id);
		for(Pair subTreePair : subTree){
			String dep = subTreePair.getDep();
			if(dep.equals("compound") || dep.equals("amod")){
				String modifier = tokens.get(subTreePair.getPos());
				result = modifier + " " + head;
			}
		}
		return result;
	}
	
	public static void removeDuplicates()throws Exception{
		BufferedReader br = new BufferedReader( new FileReader( BING_NEWS_URL) );
		BufferedWriter bw = new BufferedWriter( new FileWriter( BING_NEWS_UNIQUE_URL ));
		String line = null;
		int cnter = 0;
		int lineCnter = 0;
		HashSet<String> titleSet = new HashSet<>();
		while((line = br.readLine())!=null){
			String title = line.split("\t")[6];
			lineCnter++;
			if(lineCnter % 100000 == 0)
				System.out.println("Line: " + lineCnter);
			if(!titleSet.contains(title)){
				bw.write(line + "\n");
				titleSet.add(title);
			}
			else{
				cnter++;
				if(cnter % 1000 == 0)
					System.out.println("Duplicates: " + cnter);
			}
		}
		bw.close();
		br.close();
	}
	
}

