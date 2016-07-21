package edu.sjtu.cs.action.main;
import edu.mit.jwi.item.POS;
import edu.sjtu.cs.action.knowledgebase.*;
import edu.sjtu.cs.action.util.Helper;
import edu.sjtu.cs.action.util.*;

import java.io.*;
import java.text.DateFormat;
import java.util.*;

import edu.sjtu.cs.action.util.Parser;
import org.apache.commons.io.FileUtils;

/*
 * Author: xinyu
 * Last Modified: 2016-06-20
 * 
 * Description: This class deals with:
 *  1). Build Noun list
 * 	2). Extract Action instance from news body
 *  3). Extract noun concept from news title
 *  4). Build Action-noun map
 *  5). Process news data
 */

public class Main {

	private static int DICT_INDEX = 5;
	final private static String ACTION_OCCUR = "dat/occurrence/action_occurrence/" ;
	final private static String ACTION_TMP_URL = "dat/tmp/action_occurrence_tmp/";
	final private static String ACTION_URL = "dat/action/action_138.txt";
	final private static String ACTION_POP_URL = "dat/action/action_popular_in_103.txt";
	final private static String VERB_URL = "dat/action/verb_138.txt";
	final static private String YU_PROCESSED_RESULT_URL = "dat/action/concepts_new/";
	private static String NOUN_DICT_URL = "dat/noun_concept/noun_" + DICT_INDEX +".dict";
	private static String NOUN_TMP_URL = "dat/tmp/noun_occurrence_" + DICT_INDEX + "_tmp/";
	private static String NOUN_RST_URL = "dat/occurrence/noun_occurrence/"  +DICT_INDEX +".txt";


	private static String VERB_DICT_URL = "dat/verb/verb.dict";
	private static String INFL_DICT_URL = "dat/verb/inflection.dict";
	private static String INSTANCE_TMP_URL = "dat/tmp/instance_occurrence_tmp/";
	private static String ARGUMENT_TMP_URL = "dat/tmp/argument_tmp/";
	final private static String VERB_TMP_URL = "dat/tmp/verb_occurrence_tmp/";
	final private static String ACTION_IN_NOUN_TMP_URL = "dat/tmp/action_in_noun/raw/";
	final private static String ACTION_IN_NOUN_SORTED_TMP_URL = "dat/tmp/action_in_noun/sorted/";
	final private static String ACTION_NOUN_MAP_FILTERED_URL = "dat/action_noun_map/1_filtered_100/";
	final private static String ACTION_CLASS_FOR_EVAL_URL = "dat/action/concept_100.txt";
	final private static String ACTION_TEST_URL = "dat/action/concept_test.txt";

	final private static String ACTION_NEWS_OCCUR = "dat/news_action/" ;
	final private static String SYN_URL = "dat/action/synset_138.txt";
	private static List<String> puncList;
	final private static String[] VERB_TAG = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};
	private static String[] DUMMY_VERBS = {"be","am","is","are","was","were","have","has","had","get","got","gets"};
	private static List<String[]> actionList;
	private static List<String> verbList;
	private static Lemmatizer lm ;
	private static Wordnet wordnet;
	private static List<Set<String>> inflectionList;
	private static HashSet<String> inflectionSet;
	private static List<HashSet<String>> synonymList;
	private static HashSet<String> synsetSet;
	private static List<String> dummyVerbs;
	private static HashMap<String, List<String>> verb2subj;
	private static HashMap<String, List<String>> verb2obj;
	private static int offset = 55914;

	public static void main(String args[]) throws Exception{

		Date rightNow;
		Locale currentLocale;
		DateFormat timeFormatter;
		DateFormat dateFormatter;
		String timeOutput;
		String dateOutput;

		long startTime = System.nanoTime();

		//buildNounList();
		//postagNewsTitle(70,100);
		//extractVerb();
		//buildVerbDict();
		//extractActionInstance(30,50);

		//buildGlobalArgumentDict();
		//filterArgumentDict();
		extractActionConcept(0, 10);
		//extractActionConceptGlobally(0, 1);
		//tokenizeActionInNews(1,10);
		//testExtraction();
		//extractNoun(11,12);
		//buildActionNounMap(0,10);
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


	public static void testExtraction() throws Exception{
		Extraction e = new Extraction(0,0,0);
		e.testExtractionInstance();
	}

	public static void buildNounList()throws Exception{
		NounList nounList = new NounList(4, true);
		nounList.getNounList();
	}

	public static void extractVerb() throws Exception{
		Verb verb = new Verb();
		verb.getVerbFromCorpus();
	}

	public static void buildVerbDict() throws Exception{
		Verb verb = new Verb();
		verb.filterVerbDict();
	}

	public static void filterArgumentDict()throws Exception{
		Verb verb = new Verb();
		verb.filterArgumentDict();
	}

	public static void buildGlobalArgumentDict()throws Exception{
		Verb verb = new Verb();
		verb.buildGlobalArgumentDict();
	}

	public static void extractNoun(int l, int u)throws Exception{
		Extraction e = new Extraction(l, u, offset);
		e.extractNounFromNewsTitle();
	}

	public static void extractActionInstance(int l, int u)throws Exception{
		Extraction e = new Extraction(l, u, offset);
		e.extractActionInstanceFromNewsBody();
	}

	public static void extractActionConcept(int l, int u)throws Exception{
		Extraction e = new Extraction(l, u, offset);
		e.extractActionConceptFromActionInstance();
	}

	public static void extractActionConceptGlobally(int l, int u)throws Exception{
		Extraction e = new Extraction(l, u, offset);
		e.extractActionConceptGlobally();
	}

	public static void buildActionNounMap(int l, int u)throws Exception{
		MapBuilder m = new MapBuilder(4, 10);
		m.build(l, u);
	}

	public static void parseAndPostagNewsBody(int l, int u)throws Exception{
		News n = new News(l, u, offset);
		n.parseAndPostagBody();
	}

	public static void lemmatizeNews(int l, int u)throws Exception{
		News n = new News(l, u, offset);
		n.lemmatizeNews();
	}

	public static void postagNewsTitle(int l, int u) throws Exception{
		News n = new News(l, u, offset);
		n.postagTitle();
	}

	public static void tokenizeActionInNews(int l, int u)throws Exception{
		News n = new News(l, u, offset);
		n.tokenizeAction();
	}

}

