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


/*
	This class only has one task, to create a list containing possible inflections
	of the action list.

	Last Modified: 2016-04-02
 */

public class Inflector{

	private final static String VERB_URL = "dat/action/verb/verb.dict";
	private final static String INF_NEW_URL = "dat/action/verb/inflection.dict";

	public static void main(String[] args)throws Exception{
		createInflectionsForVerbs();
	}

	public static void createInflectionsForVerbs()throws Exception{
		BufferedReader br = new BufferedReader(new FileReader(VERB_URL));
		BufferedWriter bw = new BufferedWriter(new FileWriter(INF_NEW_URL));
		String line = null;
		int cnter = 0;
		while((line = br.readLine())!=null){
			System.out.println(cnter++);
			String[] inflections = getInflections(line);
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
