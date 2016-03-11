package edu.sjtu.cs.action.main;
import java.util.*;
import java.io.*;

import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.util.Lemmatizer;

public class getActionOccurrence implements Runnable {
	final private static String IDX_FOLDER_URL = "dat/action_occurrence_tmp/";
	final private static String BING_NEWS_SLICED_URL = "dat/news/bing_news_sliced/";
	private ProbaseClient pb;
	private static Wordnet wn;
	private static Lemmatizer lm;
	private static List<String> puncList;
	private static List<Set<String>> inflectionList; 
	private static HashSet<String> inflectionSet;
	private static List<String> dummyVerbs;
	private static int offset = 134163;
	private static List<String[]> actionList;
	private int part;
	
	public getActionOccurrence( int part, ProbaseClient pb, Wordnet wn, 
			Lemmatizer lm, List<String> dv, List<String[]> al,  List<Set<String>> il,
			HashSet<String> is,	List<String> pl){
		this.pb = pb;
		this.wn = wn;
		this.lm = lm;
		this.dummyVerbs = dv;
		this.actionList = al;
		this.inflectionList = il;
		this.inflectionSet = is;
		this.puncList = pl;
		this.part = part;
	}
	
	public void run(){
		File fileToWrite = new File(IDX_FOLDER_URL + part + "/");
		fileToWrite.mkdirs();
		BufferedWriter[] bwArray = new BufferedWriter[actionList.size()];
		try{
			for(int i = 0; i < actionList.size(); ++i){
				String[] tmp = actionList.get(i);
				File tmpFile = new File(IDX_FOLDER_URL + part + "/" + tmp[0] + "_" + tmp[1] + "_" + tmp[2] + ".idx");
				bwArray[ i ] = new BufferedWriter( new FileWriter(tmpFile));	
			}
			
			BufferedReader newsReader = new BufferedReader(new FileReader(BING_NEWS_SLICED_URL + "bing_news_" + part + ".tsv"));
			String line = null;
			int cnt = -1, foundNumber = 0, potential = 0; // cnt = -1, which means the first news is numbered as 0
			int lineNum = part * offset - 1;
			while((line = newsReader.readLine())!=null){
				cnt++;
				lineNum++;
				String body = line.split("\t")[7];
				if(cnt % 1000 == 0){
					System.out.println("part:" + part +" read:" + cnt + " found:" + foundNumber + " potential:" + potential);	
				}
				boolean[] whichToCheck = new boolean[actionList.size()];
				int i = 0;
				boolean toCheck = false;
				for(Set<String> verbs : inflectionList){
					for(String s : verbs){
						if(body.contains(s)){
							whichToCheck[ i ] = true;
							toCheck = true;
							break;
						}
					}
					i++;
				}
				if(toCheck){
					for(int k = 0; k < actionList.size(); ++k){	
						if(whichToCheck[ k ]){
							String[] sentences = body.split("(?<=[.!?])\\s* ");
							for(String sentence : sentences){
								sentence = sentence.trim();
								if(isGoodSentence(sentence)){
									potential++;
									if(actionDetected(sentence, actionList.get(k))){
										bwArray[k].append(Integer.toString(lineNum) + "\t"
												  + sentence  +"\n");
										foundNumber++;
										bwArray[k].flush();
									}
								}
							}
						}
					}
				}
			}
			// 3.4 close IO
			newsReader.close();
			for(BufferedWriter bw : bwArray)
				bw.close();
			pb.disconnect();
		}
		catch(Exception e){
			System.out.println(e);
		}
	}
	
	private boolean actionDetected(String sentence, String[] action)throws Exception{
		List<String> tagged = lm.lemmatizeAndPOSTag(sentence);
		int findValue = 0, findValuePassive = 0;
		String subject = action[ 0 ];
		String object = action[ 2 ];
		String predicate = action[ 1 ];
		int sentenceLength = tagged.size();
		for(int i = 0; i < sentenceLength; ++i){
			findValue = 0;
			String[] current = tagged.get(i).split("_");
			if(!current[1].contains("V"))
				continue;
			
			if(dummyVerbs.contains(current[0]))
				continue;
			
			String firstStem = wn.stemVerbFirst(current[0]);
			if(firstStem == null)
				continue;
				
			if(predicate.equalsIgnoreCase(firstStem)){
				// check if subj and obj match
				int lower = ( i - 5 < 0) ? 0 : i - 5;
				int upper = ( i + 5 > sentenceLength - 1) ? sentenceLength - 1 : i + 5;
				for( int j = lower ; j < i; ++j){
					String[] focus = tagged.get(j).split("_");
					//System.out.println(focus[0] + "\t" + focus[1]);
					if(focus[1].startsWith("N") && pb.isPair(subject, focus[0])){
						//System.out.println(focus[0]);
						findValue++;
					}
					
					if(focus[1].startsWith("N") && pb.isPair(object, focus[0])){
						findValuePassive++;
					}
				}
				for( int j = i + 1; j <= upper; ++j){
					String[] focus = tagged.get(j).split("_");
					if(focus[1].startsWith("N") && pb.isPair(object, focus[0])){
						//System.out.println(focus[0]);
						findValue++;
					}
					
					if(focus[1].startsWith("N") && pb.isPair(subject, focus[0])){
						findValuePassive++;
					}
				}
				// action found,record idx
				if(findValue >= 2 || findValuePassive >= 2){
					return true;
				}
			}
		}
		return false;
	}
	
	private static boolean isGoodSentence(String query)throws Exception{
		try{
            //condition one:length
            if (query.length() <= 10 || query.length() > 300){
                return false;
            }
            //condition two: uniqueness 
            //guaranteed by scope reducer (here ignore it)
            
            //condition 3: starts with capital letter; ends with .
            if (query.charAt(0) < 'A' || query.charAt(0) > 'Z' || query.charAt(query.length() - 1) != '.'){
                return false;
            }
            
            // condition 4: number of words >=6 && <=20
            String[] words = query.split("\\s+");
            String cleanSentence = query.replaceAll("[^\\p{L}\\p{Nd}\\s+]+", "").toLowerCase();
			String[] tokens = cleanSentence.split("\\s+");
			
            if (words.length < 4 || words.length > 30){
                return false;
            }
            int badWords = 0, noisyCharacters = 0, numbers = 0;
           
            for (int i = 0; i < words.length; i++)
            {
                if (words[i].length() == 0) continue;
                boolean isBad = true;
                String temp = words[i];
                for(int j = 0; j < temp.length(); j++){
                    if(temp.charAt(j) >= 'A' && temp.charAt(j) <= 'Z' || temp.charAt(j) >= 'a' && temp.charAt(j) <= 'z')
                        isBad = false;
                    else
                        if (!(temp.charAt(j) >= '0' && temp.charAt(j) <= '9' 
                        	  	|| temp.charAt(j) == ',' || temp.charAt(j) == '.' 
                        	  	|| temp.charAt(j) == '-' || temp.charAt(j) == '\"')){
                        	noisyCharacters++;
                        }
                    if (temp.charAt(j) >= '0' && temp.charAt(j) <= '9') numbers++;
                }
                if (temp.length() == 1 
                		&& (temp.charAt(0) >= 'A' && temp.charAt(0) <= 'Z' 
                		|| temp.charAt(0) >= 'a' && temp.charAt(0) <= 'z') 
                		&& !(temp.charAt(0) == 'a' || temp.charAt(0) == 'A' || temp.charAt(0) == 'I'))
                    isBad = true;
                if(isBad) badWords++;
            }
            // condition 5: characters other than letters,numbers,',','.',' ','-','"' <=6 
            if (noisyCharacters > 6){
                return false;
            }
            //condition 6: pure non-letter words + wrong single-letter words <=4
            if (badWords > 4){
                return false;
            }
            //condition 7: number characters should not exceed 10
            if (numbers > 10){
                return false;
            }
            
            //condition 8: must contain at least one verb which appears in the verb dictionary
            //condition 9: at least contains one Probase instance
            boolean containsVerb = false;
            for(String token : tokens){
            	if(inflectionSet.contains(token)){
            		containsVerb = true;
            		break;
            	}
            }
            
            if(!containsVerb){
            	return false;
            }
            
            //condition 10: end with 1 punctuation.
            int puncCount = 0;
            for(int t = query.length() - 1; t >= 0; t--){
                if (puncList.contains(Character.toString( query.charAt(t) ))){
                    puncCount++;
                }
                else
                    break;
            }
            if (puncCount > 1){
                return false;
            }
            
            //condition 11: All - Upper Case
            int upperWin = 3;
            for (int t = 0; t < words.length - upperWin; t++)
            {
                int upperCount = 0;
                for (int i = 0; i < upperWin; i++){
                    String curWord = words[t + i];
                    if (curWord.toUpperCase().equals(curWord))
                        upperCount++;
                    else
                        break;
                }
                if (upperCount == 3){
                    return false;
                }
            }
            //condition 12: More than half words with upper captital
            int capCount = 0;
            for(String w : words){
                if(w.charAt(0) >= 'A' && w.charAt(0) <= 'Z' && w.charAt(1) >= 'a' && w.charAt(1) <= 'z')
                    capCount++;
            }
            
            if (1.0 * capCount / words.length > 0.5){
                return false;
            }
            
            //condition 13: Special char
            if (query.contains("/") || query.contains("\\")){
                return false;
            }
        }
        catch (Exception e1){
            return false;
        }
        return true;
	}
	
	
}
