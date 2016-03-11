package edu.sjtu.cs.action.util;

import java.io.*;

import simplenlg.features.Feature;
import simplenlg.features.Form;
import simplenlg.features.Tense;
import simplenlg.framework.InflectedWordElement;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.WordElement;
import simplenlg.lexicon.XMLLexicon;
import simplenlg.realiser.english.Realiser;

public class Inflector{
	
	private final static String VERB_URL = "dat/action/verb_10.txt";
	private final static String ACTION_URL = "dat/action/action_10.txt";
	
	public static void main(String[] args)throws Exception{
		createInflectionsForActions();
	}
	
	public static void createInflectionsForActions()throws Exception{
		BufferedReader br = new BufferedReader(new FileReader(ACTION_URL));
		BufferedWriter bw = new BufferedWriter(new FileWriter(VERB_URL));
		String line = null;
		while((line = br.readLine())!=null){
			String[] inflections = getInflections(line.split("\\s+")[1]);
			for(String i : inflections){
				bw.append(i + "\t");
			}
			bw.newLine();
		}
		br.close();
		bw.close();
	}
	
	public static String[] getInflections(String infinitive)throws Exception{
		String[] result = new String[5]; // do, does, doing, did, done
		XMLLexicon lexicon = new XMLLexicon("D:\\xinyu\\data\\jars\\simplenlg-v4.4.3\\res\\default-lexicon.xml");
		Realiser realiser = new Realiser(lexicon);
		WordElement word = lexicon.getWord(infinitive, LexicalCategory.VERB);
		InflectedWordElement infl1 = new InflectedWordElement(word);
		InflectedWordElement infl2 = new InflectedWordElement(word);
		InflectedWordElement infl3 = new InflectedWordElement(word);
		InflectedWordElement infl4 = new InflectedWordElement(word);
		infl1.setFeature(Feature.FORM, Form.PRESENT_PARTICIPLE);
		String ing = realiser.realise(infl1).getRealisation();
		infl2.setFeature(Feature.FORM, Form.PAST_PARTICIPLE);
		String pp = realiser.realise(infl2).getRealisation();
		infl3.setFeature(Feature.TENSE, Tense.PAST);
		String past = realiser.realise(infl3).getRealisation();
		infl4.setFeature(Feature.TENSE, Tense.PRESENT);
		String present = realiser.realise(infl4).getRealisation();
		result[ 0 ] = infinitive;
		result[ 1 ] = present;
		result[ 2 ] = ing;
		result[ 3 ] = past;
		result[ 4 ] = pp;
 		return result;
	}
	
}
