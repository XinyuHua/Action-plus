package edu.sjtu.cs.action.action;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.util.NLP;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.*;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class Action {
	private String predicate;
	private String subject;
	private String object;
	
	public Action(String[] actionInArray)throws Exception{
		if(actionInArray.length != 3){
			throw new Exception("Invalid argument. Construction failure!");
		}			
		predicate = actionInArray[ 1 ];
		subject = actionInArray[ 0 ];
		object = actionInArray[ 2 ];
	}
	
	public String[] toStringArray(){
		String[] result = { subject, predicate, object};
		return result;
	}
	
	/*
	 * This method is more accurate but presumably slower
	 */
	public List<String> findInstance2(String sentence, Wordnet wn, ProbaseClient pc) throws Exception{
		List<String> result = new ArrayList<String>();
		String cleanSentence = sentence.replaceAll("[^\\p{L}\\p{Nd}\\s+\\.\\,]+", "");
		String[] sent = cleanSentence.split("((\\s+)|(?<=,)|(?=,)|(?=\\.))");
		List<String> verbPos = new ArrayList<String>();
		int idx = 1;
		for(String part : sent){
			if(part.length() != 0){
				if(wn.stemVerbFirst(part).equalsIgnoreCase(predicate)){
					verbPos.add(part + "-" + idx);
				}
			}
			idx++;
		}
		if(verbPos.isEmpty())
			return result;
		int[] actionCheckedTimeArray = new int[verbPos.size()];
		int[] actionValidArray = new int[verbPos.size()];
		String[] parsed =  NLP.dependencyParse(cleanSentence);
		int verbIdx = 0;
		int commaPos = -1;
		int preParenthesis = -1;
		int postParenthesis = -1;
		String[] currentAction = new String[3];
		for(int i = 0; i < parsed.length; ++i){
			String part = parsed[ i ];
			// System.out.println(i + "\t" + part);
			if(part.contains("'")){
				continue;
			}
			if(part.contains(verbPos.get(verbIdx))){
				if(part.startsWith("nsubj(")){
					commaPos = part.indexOf(",");
					preParenthesis = part.indexOf("(");
					postParenthesis = part.indexOf(")");
					String verb = part.substring(preParenthesis + 1, commaPos);
					String noun = part.substring(commaPos + 1, postParenthesis).split("-")[0].trim();
					if( pc.isPair(subject, noun)){
						actionValidArray[ verbIdx ]++;
						actionCheckedTimeArray[ verbIdx ]++;
						currentAction[ 0 ] = noun;
						currentAction[ 1 ] = verb.split("-")[0];
					}
				}else if(part.startsWith("dobj")){
					commaPos = part.indexOf(",");
					preParenthesis = part.indexOf("(");
					postParenthesis = part.indexOf(")");
					String verb = part.substring(preParenthesis + 1, commaPos);
					String noun = part.substring(commaPos + 1, postParenthesis).split("-")[0].trim();
					if( pc.isPair(object, noun)){
						actionValidArray[ verbIdx ]++;
						actionCheckedTimeArray[ verbIdx ]++;
						currentAction[ 2 ] = noun;
					}
				}else if(part.startsWith("nsubjpass")){
					commaPos = part.indexOf(",");
					preParenthesis = part.indexOf("(");
					postParenthesis = part.indexOf(")");
					String verb = part.substring(preParenthesis + 1, commaPos);
					String noun = part.substring(commaPos + 1, postParenthesis).split("-")[0].trim();
					if( pc.isPair(object, noun)){
						actionValidArray[ verbIdx ]++;
						actionCheckedTimeArray[ verbIdx ]++;
						currentAction[ 2 ] = noun;
						currentAction[ 1 ] = verb.split("-")[0];
					}
				}else if(part.startsWith("nmod:agent")){
					commaPos = part.indexOf(",");
					preParenthesis = part.indexOf("(");
					postParenthesis = part.indexOf(")");
					String verb = part.substring(preParenthesis + 1, commaPos);
					String noun = part.substring(commaPos + 1, postParenthesis).split("-")[0].trim();
					if( pc.isPair(object, noun)){
						actionValidArray[ verbIdx ]++;
						actionCheckedTimeArray[ verbIdx ]++;
						currentAction[ 0 ] = noun;
						currentAction[ 1 ] = verb.split("-")[0];					
					}
				}
				if(actionCheckedTimeArray[ verbIdx ] == 2){
					//System.out.println(currentAction[0] + "," + currentAction[1]
					//			   + "," + currentAction[2]);
					if(actionValidArray[verbIdx] == 2){
						result.add(currentAction[0] + "," + currentAction[1]
								   + "," + currentAction[2]);
					}
					verbIdx++;
					if(verbIdx == verbPos.size())
						break;
				}
				
			}
		}
		//System.out.println(actionValidArray[0]);
		return result;
		
	}
	
	/*
	 * This method might not be accurate when the same verb occurs more than once, but this method is faster
	 */
	public String findInstance(String sentence, Wordnet wn, ProbaseClient pc) throws Exception{
		String[] result = new String[3];
		String[] parsed =  NLP.dependencyParse(sentence);
		boolean findPredicateSubject = false;
		boolean findPredicateObject = false;
		int predicatePos = -1;
		int commaPos = -1;
		int preParenthesis = -1;
		int postParenthesis = -1;
		for(String part : parsed){
			//System.out.println(part);
			if(part.contains("'"))
				continue;
			if(part.startsWith("nsubjpass")){
				commaPos = part.indexOf(",");
				preParenthesis = part.indexOf("(");
				postParenthesis = part.indexOf(")");
				String verb = part.substring(preParenthesis + 1, commaPos);
				String noun = part.substring(commaPos + 1, postParenthesis).split("-")[0].trim();
				String stem = wn.stemVerbFirst(verb.split("-")[0]);
				//System.out.println(verb + "\t" + noun);
				if(stem.equals(predicate)){
					if( pc.isPair(object, noun)){
						predicatePos = Integer.parseInt(verb.split("-")[1]);
						findPredicateObject = true;
						result[ 2 ] = noun;
						result[ 1 ] = stem;//.split("-")[0];
					}
				}
			}else if(part.startsWith("nsubj(")){
				commaPos = part.indexOf(",");
				preParenthesis = part.indexOf("(");
				postParenthesis = part.indexOf(")");
				String verb = part.substring(preParenthesis + 1, commaPos);
				String noun = part.substring(commaPos + 1, postParenthesis).split("-")[0].trim();
				String stem = wn.stemVerbFirst(verb.split("-")[0]);
				if(stem.equals(predicate)){
					if( pc.isPair(subject, noun)){
						predicatePos = Integer.parseInt(verb.split("-")[1]);
						findPredicateSubject = true;
						result[ 0 ] = noun;
						result[ 1 ] = verb;//.split("-")[0];
					}
				}
			}else if(part.startsWith("dobj")){
				commaPos = part.indexOf(",");
				preParenthesis = part.indexOf("(");
				postParenthesis = part.indexOf(")");
				String verb = part.substring(preParenthesis + 1, commaPos);
				String noun = part.substring(commaPos + 1, postParenthesis).split("-")[0].trim();
				if(Integer.parseInt(verb.split("-")[1]) == predicatePos){
					//System.out.println(verb);
					if( pc.isPair(object, noun)){
						findPredicateObject = true;
						result[ 2 ] = noun;
					}
				}
			}else if(part.startsWith("nmod:agent")){
				commaPos = part.indexOf(",");
				preParenthesis = part.indexOf("(");
				postParenthesis = part.indexOf(")");
				String verb = part.substring(preParenthesis + 1, commaPos);
				String noun = part.substring(commaPos + 1, postParenthesis).split("-")[0].trim();
				String stem = wn.stemVerbFirst(verb.split("-")[0]);
				if(Integer.parseInt(verb.split("-")[1]) == predicatePos){
					//System.out.println(verb);
					if( pc.isPair(subject, noun)){
						findPredicateSubject = true;
						result[ 0 ] = noun;
					}
				}
			}
		}
		
		if(!findPredicateSubject || !findPredicateObject){
			return null;
		}
		
		return result[0] + "," + result[1] + "," + result[2];
	}
	
	public static void main(String[] args)throws Exception
	{
		ProbaseClient pc = new ProbaseClient();
		Wordnet wn = new Wordnet(false);
		BufferedReader br = new BufferedReader(new FileReader("dat/news/window.txt"));
		String line = br.readLine().split("\t")[7];
		String[] sentences = line.split("\\.");
		String[] t = {"country", "invade", "country"};
		Action ac = new Action( t );
		String test1 = "\"In 2003, America invaded Iraq and then China was invaded by Japan.\"--"
				+ " Then Germany invaded UK.";
		String test2 = "Japan invaded China before and during the war"
				+ ", and Chinese who were sent to work in Japan and "
				+ "their descendants are suing Mitsubishi for compensation in China.";
//		List<String> r = new ArrayList<String>();
//		for(String st : sentences){
//			r.addAll( ac.findInstance2(st, wn, pc));
//		}
		
		List<String> r = ac.findInstance2(test2, wn, pc);

		for(String t2 : r)
		{
			System.out.println( t2 );
		}
	}
}
