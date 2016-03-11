package edu.sjtu.cs.action.knowledgebase;

import java.net.URL;
import java.io.*;
import java.util.*;

import edu.mit.jwi.*;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISenseKey;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.mit.jwi.morph.WordnetStemmer;

public class Wordnet {
	
	final static private File WORDNET_FILE = new File("dat/wordnet/dict");
	static private IRAMDictionary dict;
	static private WordnetStemmer stemmer;
	
	
	
	public Wordnet(boolean reside)throws Exception
	{
		dict = new RAMDictionary(WORDNET_FILE, ILoadPolicy.NO_LOAD);
		dict.open();
		if(reside)
		{
			System.out.println("Loading wordnet dictionary into memory...");
			dict.load( true );
			System.out.println("Wordnet loaded.");
		}
		
		stemmer = new WordnetStemmer(dict);
	}
	
	public static HashSet<String> getActNoun()throws Exception{
		HashSet<String> nounSet = new HashSet<String>();
		int j =0 ;
		for(Iterator<IIndexWord> i = dict.getIndexWordIterator(POS.NOUN); i.hasNext();){
			for(IWordID wid : i.next().getWordIDs()){
				IWord word = dict.getWord(wid);
				ISynset synset = word.getSynset();
				String LexFileName = synset.getLexicalFile().getName();
				if(LexFileName.equals("noun.act")){
					nounSet.add( word.getLemma() );
				}
			}
		}
		return nounSet;
	}
	
	public static List<String> getRelatedNouns(String word, POS pos)throws Exception{
		List<String> relatedWordsIDList = new ArrayList<String>();
		IIndexWord idxWord = dict.getIndexWord(word, pos);
		for(IWordID wordID : idxWord.getWordIDs()){
			System.out.println("id:" + wordID);
			List<IWordID> l = dict.getWord(wordID).getRelatedWords(Pointer.DERIVATIONALLY_RELATED);
			if(l.size() > 0){
				for(IWordID id : l){
					IWord iword = dict.getWord(id);
					if( id.getPOS() == POS.NOUN){
						relatedWordsIDList.add(iword.getLemma());
					}
				}
			}
		}
		return relatedWordsIDList;
	}
	
	public static List<String> stemNoun(String surfaceForm) throws Exception
	{
		return stemmer.findStems(surfaceForm, POS.NOUN);
	}
	
	public static String stemNounFirst(String surfaceForm)throws Exception
	{
		return stemmer.findStems(surfaceForm, POS.NOUN).get(0);
	}
	
	public static List<String> stemVerb(String surfaceForm) throws Exception
	{
		return stemmer.findStems(surfaceForm, POS.VERB);
	}
	
	public static String stemVerbFirst(String surfaceForm) throws Exception
	{
		List<String> stems = stemmer.findStems(surfaceForm, POS.VERB);
		if(!stems.isEmpty())
			return stems.get(0);
		return "";
	}
	
	public static List<String> getSynonymsForAllRoots(String surfaceForm, POS pos)throws Exception
	{
		List<String> stems = stemmer.findStems(surfaceForm, pos);
		List<String> result = new ArrayList<String>();
		for(String stem : stems)
		{
			result.addAll( getSynonyms( stem, pos ) );
		}
		return result;
	}
	
	public static List<String> getNounGlossForAllRoots(String surfaceForm) throws Exception
	{
		List<String> stems = stemmer.findStems(surfaceForm, POS.NOUN);
		List<String> result = new ArrayList<String>();
		for(String stem : stems)
		{
			result.addAll( getNounGloss( stem ) );
		}
		return result;
	}
	
	public static List<String> getHypernymsForAllRoots(String surfaceForm) throws Exception
	{
		List<String> stems = stemmer.findStems(surfaceForm, POS.NOUN);
		List<String> result = new ArrayList<String>();
		for(String stem : stems)
		{
			result.addAll( getHypernyms( stem ) );
		}
		return result;
	}
	
	public static List<String> getHyponymsForAllRoots(String surfaceForm) throws Exception
	{
		List<String> stems = stemmer.findStems(surfaceForm, POS.NOUN);
		List<String> result = new ArrayList<String>();
		for(String stem : stems)
		{
			result.addAll( getHyponyms( stem ) );
		}
		return result;
	}
	
	public static List<Integer> findSynonymsIdx(String target, List<String> candidates, POS pos) throws Exception
	{
		Integer[] result = new Integer[candidates.size()];
		int current = 0;
		List<String> stems1 = stemmer.findStems( target, pos);
		List<String> synsOf1 = new ArrayList<String>();
		for(String stem : stems1)
		{
			synsOf1.addAll( getSynonyms( stem, pos) );
		}
		search:
		for(String candidate : candidates)
		{
			List<String> stems2 = stemmer.findStems(candidate, pos);
			for(String stem2 : stems2)
			{
				if( synsOf1.contains(stem2) )
				{
					result[ current ] = 1;
					current++;
					continue search;
				}
			}
			result[ current ] = 0;
			current++;
		}
		return Arrays.asList(result);
	}
	
	public static boolean areSynonyms(String s1, String s2, POS pos)throws Exception
	{
		List<String> stems1 = stemmer.findStems(s1, pos);
		List<String> stems2 = stemmer.findStems(s2, pos);
		
		for(String stem1 : stems1)
		{
			for(String stem2 : stems2)
			{
				if(stem1.equals(stem2))
				{
					return true;
				}
			}
		}
		
		List<String> synsOf1 = new ArrayList<String>();
		for(String stem : stems1)
		{
			synsOf1.addAll( getSynonyms( stem, pos) );
		}
		
		for( String stem2 : stems2)
		{
			if( synsOf1.contains(stem2) )
				return true;
		}
		return false;
	}
	
	private static HashSet<String> getSynonyms(String stem, POS pos)throws Exception
	{
		IIndexWord idxWord = dict.getIndexWord(stem, pos);
		HashSet<String> result = new HashSet<String>();
		for(IWordID wordID : idxWord.getWordIDs())
		{
			ISynset synset = dict.getWord(wordID).getSynset();
			result.addAll( getWordsFromSynset(synset));
		}
		return result;
	}
	
	private static HashSet<String> getHypernyms(String stem)throws Exception
	{
		IIndexWord idxWord = dict.getIndexWord(stem, POS.NOUN);
		HashSet<String> result = new HashSet<String>();
		for(IWordID wordID : idxWord.getWordIDs())
		{
			ISynset synset = dict.getWord(wordID).getSynset();
			List<ISynsetID> hypernyms = synset.getRelatedSynsets(Pointer.HYPERNYM);
			for(ISynsetID sid : hypernyms)
			{
				ISynset synset_hyper = dict.getSynset(sid);
				result.addAll( getWordsFromSynset(synset_hyper) );
			}
		}
		return result;
	}
	private static HashSet<String> getNounGloss(String stem) throws Exception
	{
		IIndexWord idxWord = dict.getIndexWord(stem, POS.NOUN);
		HashSet<String> result = new HashSet<String>();
	
		if(idxWord != null)
		{
			for(IWordID wordID : idxWord.getWordIDs())
			{
				ISynset synset = dict.getWord(wordID).getSynset();
				result.add(synset.getGloss());
			}
		}
		
		return result;
	}
	
	private static HashSet<String> getHyponyms(String stem)throws Exception
	{
		IIndexWord idxWord = dict.getIndexWord(stem, POS.NOUN);
		HashSet<String> result = new HashSet<String>();
		for(IWordID wordID : idxWord.getWordIDs())
		{
			ISynset synset = dict.getWord(wordID).getSynset();
			List<ISynsetID> hyponyms = synset.getRelatedSynsets(Pointer.HYPONYM);
			for(ISynsetID sid : hyponyms)
			{
				ISynset synset_hypo = dict.getSynset(sid);
				result.addAll( getWordsFromSynset(synset_hypo) );
			}
		}
		return result;
	}
	
	private static HashSet<String> getWordsFromSynset(ISynset synset)throws Exception
	{
		HashSet<String> result = new HashSet<String>();
		for(IWord word : synset.getWords())
		{
			result.add( word.getLemma() );
		}
		return result;
	}
}
