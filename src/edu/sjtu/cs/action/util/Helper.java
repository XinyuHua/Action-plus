package edu.sjtu.cs.action.util;
import java.io.*;
import java.util.*;

public class Helper {

	final private static String BING_NEWS_CONV_URL = "dat/news/bing_news.tsv";
	final private static String BING_NEWS_URL = "dat/news/bing_news_1m.tsv";
	final private static String NOUN_OCC_URL = "dat/noun_occurrence/";
	final private static String ACTION_OCC_URL = "dat/action_occurrence/";
	final private static String REDUCED_DIR_URL = "dat/reduced/";
	private static void reduceNounOcc()throws Exception{
		BufferedWriter bw = new BufferedWriter(new FileWriter(REDUCED_DIR_URL + "noun_occurrence_all.txt"));
		for(int i = 0; i < 100; ++)
	}
	
	private static void convert()throws Exception
	{
		BufferedReader br = new BufferedReader( new FileReader( BING_NEWS_URL) );
		BufferedWriter bw = new BufferedWriter( new FileWriter( BING_NEWS_CONV_URL ));
		String line = null;
		int cnt = 0;
		while((line = br.readLine())!=null)
		{
			System.out.println(cnt++);
			bw.append(Utility.convertFromUTF8(line));
			bw.newLine();
		}
		br.close();
		bw.close();
	}

}
