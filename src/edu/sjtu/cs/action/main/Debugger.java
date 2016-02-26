package edu.sjtu.cs.action.main;
import java.io.*;
import java.util.*;
public class Debugger {
	private static final String DEBUG_URL = "dat/debug/";
	private static final String NEWS_URL = "dat/news/bing_news_1m.tsv";
	
	public static void main(String[] args) throws Exception{
		//showUnmatchedNewsIdxForCIC();
		showUnmatchedNewsForCIC();
	}
	
	/*
	 * This method prints out news that were not captured by Action.findInstance2(),
	 * but were captured by the previous method that without dependency parsing.
	 * 
	 * Input: country_invade_country.idx; [...].idx.bak;
	 * Output: unmatched_news.txt
	 */
	private static void showUnmatchedNewsIdxForCIC()throws Exception{
		BufferedReader method1ResultReader = new BufferedReader(
												new FileReader(DEBUG_URL + "country_invade_country.idx" ));
		BufferedReader method2ResultReader = new BufferedReader(
												new FileReader(DEBUG_URL + "country_invade_country.idx.bak" ));
		
		BufferedWriter outputWriter = new BufferedWriter(new FileWriter(DEBUG_URL + "unmatched_news.idx"));
		HashSet<Integer> method1Set = new HashSet<Integer>();
		HashSet<Integer> method2Set = new HashSet<Integer>();
		String line = null;
		while((line = method1ResultReader.readLine())!=null){
			method1Set.add(Integer.parseInt( line.split("\\s+")[0] ));
		}
		method1ResultReader.close();
		while((line = method2ResultReader.readLine())!=null){
			Integer num = Integer.parseInt( line.split("\\s+")[0] );
			if(!method1Set.contains(num)){
				outputWriter.append(num + "\n");
			}
			method2Set.add(num);
		}
		outputWriter.append("-----\n");
		method2ResultReader.close();
		List<Integer> method1List = new ArrayList<Integer>();
		method1List.addAll(method1Set);
		Collections.sort(method1List);
		for(Integer st : method1List){
			if(!method2Set.contains(st)){
				outputWriter.append(st + "\n");
			}
		}
		outputWriter.close();
	}
	
	private static void showUnmatchedNewsForCIC()throws Exception{
		BufferedReader newsReader = new BufferedReader(new FileReader(NEWS_URL));
		BufferedReader idxReader = new BufferedReader(new FileReader(DEBUG_URL + "unmatched_news.idx"));
		BufferedWriter outputWriter = new BufferedWriter(new FileWriter(DEBUG_URL + "unmatched_news.txt"));
		List<Integer> idxMethod2MinusMethod1 = new ArrayList<Integer>();
		List<Integer> idxMethod1MinusMethod2 = new ArrayList<Integer>();
		String line = null;
		boolean flip = false;
		while((line = idxReader.readLine())!=null){
			if(line.startsWith("-")){
				flip = true;
			}else if(!flip){
				idxMethod2MinusMethod1.add(Integer.parseInt(line));
			}else{
				idxMethod1MinusMethod2.add(Integer.parseInt(line));
			}
		}
		idxReader.close();
		System.out.println(idxMethod2MinusMethod1.size());
		System.out.println(idxMethod1MinusMethod2.size());
		int cnt = 0;
		int pointer1minus2 = 0;
		int pointer2minus1 = 0;
		while((line = newsReader.readLine())!=null){
			if(pointer2minus1 < idxMethod2MinusMethod1.size() && cnt == idxMethod2MinusMethod1.get(pointer2minus1)){
				outputWriter.append("2-1:" + line.split("\t")[7] + "\n");
				pointer2minus1++;
			}else if(pointer1minus2 < idxMethod1MinusMethod2.size() && cnt == idxMethod1MinusMethod2.get(pointer1minus2)){
				outputWriter.append("1-2:" + line.split("\t")[7] + "\n");
				pointer1minus2++;
			}
			cnt++;
			System.out.println(cnt);
		}
		outputWriter.close();
		newsReader.close();
	}

}
