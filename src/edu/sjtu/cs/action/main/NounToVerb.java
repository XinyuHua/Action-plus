package edu.sjtu.cs.action.main;

import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.util.Lemmatizer;

import java.io.*;
import java.util.*;

public class NounToVerb {
	
	private final static String WORD_LIST = "dat/top10000.txt";
	private final static String NV_MAP = "dat/noun_verb.txt";
	private final static String NOUN_ACT_DICT = "dat/noun_concept/noun.act.dict";
	private final static String[] ACTIONAL = {"action","activity","act"
		,"process","practice","event","behavior"};
	private static List<String> actionalList;
	private static Wordnet wn;
	private static Lemmatizer lm;
	private static ProbaseClient pb;
	public static void main(String[] args)throws Exception{
		getActNounFromWordnet();
		//debug();
	}
	
	public static void getActNounFromWordnet()throws Exception{
		wn = new Wordnet(false);
		HashSet<String> result = wn.getActNoun();
		BufferedWriter bw = new BufferedWriter(new FileWriter(NOUN_ACT_DICT));
		for(String s : result){
			bw.append(s);
			bw.newLine();
		}
		bw.close();
	}
	
	public static void show(String noun)throws Exception{
		for(String s : ACTIONAL){
			System.out.println(s + "\t\t" + noun + ":" + pb.getFreq(s,noun));
		}
		System.out.println("------------------");
	}
	
	public static void debug()throws Exception{
		pb = new ProbaseClient(4401);
		actionalList = Arrays.asList(ACTIONAL);
		System.out.println(isActional("beginning"));
		
	}
	
	public static boolean isActional(String noun)throws Exception{
		List<String> concepts = pb.findHyper(noun);
		if(concepts.isEmpty())
			return false;
		int highest = pb.getFreq(concepts.get(0), noun);
		if(highest < 5)
			return false;
		
		int threshold = (highest + 1) / 2;
		boolean tmp = false;
		for(String concept : concepts){
			if( pb.getFreq(concept, noun) < threshold){
				break;
			}
			//System.out.println("\t"+concept);
			
			if( actionalList.contains(concept)){
				//System.out.println(concept + ":"+pb.getFreq(concept,noun));
				tmp = true;
			}
		}
		if(tmp && pb.findHypo(noun).size() > 50)
			return true;
		return false;
	}
	
	/*
	public static void run2()throws Exception{
		pb = new ProbaseClient(4401);
		actionalList = Arrays.asList(ACTIONAL);
		BufferedReader br = new BufferedReader( new FileReader( WORD_LIST ) );
		BufferedWriter bw = new BufferedWriter( new FileWriter( ACTIONAL_DICT ));
		String line = null;
		int cnt = 0;
		while((line = br.readLine())!=null){
			System.out.println(cnt++ + "\t" + line);
			if(!isActional(line))
				continue;
			bw.append(line);
			bw.newLine();
		}
		bw.close();
		br.close();
	}
	
	
	public static void run()throws Exception
	{
		wn = new Wordnet(true);
		lm = new Lemmatizer();
		BufferedReader br = new BufferedReader( new FileReader( WORD_LIST ) );
		BufferedWriter bw = new BufferedWriter( new FileWriter( NV_MAP ));
		String line = null;
		int cnt = 0;
		while((line = br.readLine())!=null)
		{
			System.out.println(cnt++);
			System.out.println(line);
			HashSet<String> tmp = getEquivalentVerb(line);
			if(tmp.isEmpty())
				continue;
			bw.append(line+":");
			for(String st : tmp)
			{
				bw.append(st+",");
			}
			bw.newLine();
		}
		bw.close();
		br.close();
	}
	
	public static void test()throws Exception
	{
		wn = new Wordnet(true);
		lm = new Lemmatizer();
		for(String st : getEquivalentVerb("acquisition")){
			System.out.println(st);
		}
	}
	*/
	
	
	private static HashSet<String> getEquivalentVerb(String noun)throws Exception
	{
		HashSet<String> result = new HashSet<String>();
		List<String> glossary = wn.getNounGlossForAllRoots(noun);
		for(String term : glossary)
		{
			if(term.contains("the act of"))
			{
				result.addAll( getVerbs(term) );
			}
		}
		return result;
	}
	
	private static HashSet<String> getVerbs(String paragraph )throws Exception
	{
		List<String> tagged = lm.lemmatizeAndPOSTag(paragraph);
		HashSet<String> result = new HashSet<String>();
		for(String st : tagged)
		{
			String[] current = st.split("_");
			if(current[1].startsWith("V"))
				result.add(current[0]);
		}
		return result;
	}
	
	
	
}
