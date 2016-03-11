package edu.sjtu.cs.action.knowledgebase;
import java.io.*;
import java.net.*;
import java.util.*;

public class ProbaseClient{

	private Socket connectionSocket;
	private PrintStream printerToServer;
	private BufferedReader readerFromServer;
    
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
    	printerToServer.println("isProbaseEntity");
    	printerToServer.println(concept.toLowerCase());
    	if(readerFromServer.readLine().equals("true"))
    		return true;
    	return false;
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
    	printerToServer.println("getFreq");
    	printerToServer.println(concept + "_" + instance);
    	String[] result = readerFromServer.readLine().split("_");
    	return Integer.parseInt(result[0]);
    }
    
    public Integer getPop(String concept, String instance) throws Exception
    {
    	printerToServer.println("getFreq");
    	printerToServer.println(concept + "_" + instance);
    	String[] result = readerFromServer.readLine().split("_");
    	return Integer.parseInt(result[1]);
    }
    
    
    
}

