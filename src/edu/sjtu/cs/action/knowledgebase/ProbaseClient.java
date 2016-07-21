package edu.sjtu.cs.action.knowledgebase;
import java.net.*;
import java.io.*;
import java.util.*;

public class ProbaseClient{

	private Socket connectionSocket;
	private PrintStream printerToServer;
	private BufferedReader readerFromServer;

	public static void main(String[] args) throws Exception {
		//ProbaseClient pc = new ProbaseClient(4401);
		HashMap<String, Integer> s = new HashMap<>();

		String a = (s.containsKey("s"))? "y" : "n";
		System.out.println(a);
		//pc.disconnect();
	}
	public ProbaseClient(int port)throws Exception
	{
		connectionSocket = new Socket("localhost",port);
		printerToServer = new PrintStream( connectionSocket.getOutputStream() );
		readerFromServer = new BufferedReader(new InputStreamReader( connectionSocket.getInputStream() ) );
	}

	public void disconnect()throws Exception{
		printerToServer.println("bye");
	}

	public boolean isProbaseEntity(String concept) throws Exception
	{
		boolean result_lower = false;
		boolean result_original = false;
		printerToServer.println("isProbaseEntity");
		printerToServer.println(concept.toLowerCase());
		if(readerFromServer.readLine().equals("true"))
			result_lower = true;
		printerToServer.println("isProbaseEntity");
		printerToServer.println(concept);
		if(readerFromServer.readLine().equals("true"))
			result_original = true;
		return result_lower || result_original;
	}

	public boolean isPair(String concept, String instance)throws Exception
	{
		concept = concept.trim().toLowerCase();
		instance = instance.trim();
		if(instance.length() < 2)
			return false;
		printerToServer.println("isPair");
		printerToServer.println(concept + "_" + instance);
		if(readerFromServer.readLine().equals("true"))
			return true;
		return false;
	}

	public boolean isGoodConcept(String concept)throws Exception
	{
		printerToServer.println("isGoodConcept");
		printerToServer.println(concept.toLowerCase());
		if(readerFromServer.readLine().equals("true"))
			return true;
		return false;
	}

	public List<String> findHyper(String instance)throws Exception
	{
		List<String> resultList = new ArrayList<String>();
		printerToServer.println("findHyper");
		printerToServer.println(instance);
		String originResult = readerFromServer.readLine();
		String[] result = originResult.split("_");
		for(String st : result){
			if(!st.isEmpty())
				resultList.add(st);
		}
		return resultList;
	}

	public List<String> findHypo(String concept)throws Exception
	{
		List<String> resultList = new ArrayList<String>();
		printerToServer.println("findHypo");
		printerToServer.println(concept);
		String[] result = readerFromServer.readLine().split("_");
		for(String st : result)
		{
			if(!st.isEmpty())
				resultList.add(st);
		}
		return resultList;
	}

	public Integer getFreq(String concept, String instance) throws Exception
	{
		concept = concept.trim();
		instance = instance.trim();

		// Both are empty
		if(concept.length() + instance.length() == 0) return 100;

		// Only one empty
		if(concept.length() * instance.length() == 0) return 0;

		// Concept and instance are identical
		if( concept.equals(instance)) {
			if(isProbaseEntity(concept)){
				return 100;
			}else{
				return 0;
			}
		}

		// Others
		printerToServer.println("getFreq");
		printerToServer.println(concept + "_" + instance);
		String[] result = readerFromServer.readLine().split("_");
		return Integer.parseInt(result[0]);
	}

	public Integer getPop(String concept, String instance) throws Exception
	{
		concept = concept.trim();
		instance = instance.trim();

		// Both are empty
		if(concept.length() + instance.length() == 0) return 100;

		// Only one empty
		if(concept.length() * instance.length() == 0) return 0;

		// Concept and instance are identical
		if( concept.equals(instance)) {
			if(isProbaseEntity(concept)){
				return 100;
			}else{
				return 0;
			}
		}

		// Others
		printerToServer.println("getFreq");
		printerToServer.println(concept + "_" + instance);
		String[] result = readerFromServer.readLine().split("_");
		return Integer.parseInt(result[1]);
	}

	public Integer getHypoNumber(String concept) throws Exception {
		printerToServer.println("getHypoNumber");
		printerToServer.println(concept);
		return Integer.parseInt(readerFromServer.readLine());
	}

	public Integer getHyperNumber(String instance) throws Exception {
		printerToServer.println("getHyperNumber");
		printerToServer.println(instance);
		return Integer.parseInt(readerFromServer.readLine());
	}

	public Double getVagueness(String concept)throws Exception{
		printerToServer.println("getVagueness");
		printerToServer.println(concept);
		return Double.parseDouble(readerFromServer.readLine());
	}


}

