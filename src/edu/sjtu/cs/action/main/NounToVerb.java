package edu.sjtu.cs.action.main;

import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.util.Lemmatizer;

import java.io.*;
import java.util.*;

public class NounToVerb {
	
	private final static String WORD_LIST = "dat/top10000.txt";
	private final static String NV_MAP = "dat/noun_verb.txt";
	private static Wordnet wn;
	private static Lemmatizer lm;
	public static void main(String[] args)throws Exception
	{
		run();
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
		for(String st : getEquivalentVerb("acquisition"))
		{
			System.out.println(st);
		}
	}
	
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
