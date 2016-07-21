package edu.sjtu.cs.action.runner;

import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.util.Action;
import edu.sjtu.cs.action.util.Pair;
import edu.sjtu.cs.action.util.Parser;

import java.io.*;
import java.util.*;

/**
 * Created by xinyu on 6/17/2016.
 */
public class extractActionInstanceRunner  implements Runnable {

    private static String OUT_PATH = "dat/action/instance_extracted/";
    final private static String BING_NEWS_PARSED_URL = "dat/news/bing_news_sliced_parsed/";
    final private static String BING_NEWS_POSTAG_URL = "dat/news/bing_news_sliced_postag/";

    final private static String[] VERB_TAG = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};
    private static String[] personPronArray = {"he","she","him","her","i","me","you",
            "they","them","his","mine","theirs","yours","myself","yourself","we","us",
            "himself", "herself","themselves","ourselves","yourselves","who","whom"};
    private static String[] thingPronArray = {"this", "that", "it", "those", "these", "its"};
    private static String[] specialSignArray = {"$", "%"};
    private static String[] specialSignExplained = {"money", "percent"};
    private static List<String> measureWords;
    private ProbaseClient pb;
    private static Wordnet wn;
    private static List<String> puncList;
    private static List<String> personPronList;
    private static List<String> thingPronList;
    private static List<Set<String>> inflectionList;
    private static List<String> verbList;
    private static HashSet<String> verbTagSet;
    private static List<String> specialList;
    private int part;
    private boolean debug;
    private int offset;
    public extractActionInstanceRunner( int part, ProbaseClient pb, Wordnet wn, List<String> vl,  List<Set<String>> il,
                                  List<String> pl,  String outPath, int offset){
        this.pb = pb;
        this.wn = wn;
        this.verbList = vl;
        this.inflectionList = il;
        this.puncList = pl;
        this.part = part;
        this.OUT_PATH = outPath;
        personPronList = Arrays.asList(personPronArray);
        thingPronList = Arrays.asList(thingPronArray);

        verbTagSet = new HashSet<String>();
        for(String s : VERB_TAG){
            verbTagSet.add(s);
        }

        specialList = new ArrayList<>();
        for(String s : specialSignArray) {
            specialList.add(s);
        }
        debug = false;
        this.offset = offset;

        measureWords = new ArrayList<>();
        measureWords.add("pound");
        measureWords.add("ton");
        measureWords.add("piece");
        measureWords.add("hundred");
        measureWords.add("thousand");
        measureWords.add("million");
    }

    // This constructor is only for debug use
    public extractActionInstanceRunner( List<String> vl,  List<Set<String>> il)throws Exception{
        this.verbList = vl;
        this.inflectionList = il;
        this.pb = new ProbaseClient(4500);
        this.wn = new Wordnet(false);
        personPronList = Arrays.asList(personPronArray);
        thingPronList = Arrays.asList(thingPronArray);
        verbTagSet = new HashSet<String>();
        for(String s : VERB_TAG){
            verbTagSet.add(s);
        }

        specialList = new ArrayList<>();
        for(String s : specialSignArray) {
            specialList.add(s);
        }
        measureWords = new ArrayList<>();
        measureWords.add("pound");
        measureWords.add("ton");
        measureWords.add("piece");
        measureWords.add("hundred");
        measureWords.add("thousand");
        measureWords.add("million");
        debug = true;
    }

    public extractActionInstanceRunner()throws Exception{
        this.pb = new ProbaseClient(4500);
        this.wn = new Wordnet(false);
        personPronList = Arrays.asList(personPronArray);
        thingPronList = Arrays.asList(thingPronArray);
        verbTagSet = new HashSet<String>();
        for(String s : VERB_TAG){
            verbTagSet.add(s);
        }
        specialList = new ArrayList<>();
        for(String s : specialSignArray) {
            specialList.add(s);
        }

        measureWords = new ArrayList<>();
        measureWords.add("pound");
        measureWords.add("ton");
        measureWords.add("piece");
        measureWords.add("hundred");
        measureWords.add("thousand");
        measureWords.add("million");
    }

    public String getAllInstancesFromText(String doc)throws Exception {
        String resultStr = "";
        Parser parser = new Parser();
        List<List<String>> result = parser.dependencyParseDocument(doc);
        List<String> depList = result.get(0);
        List<String> posList = result.get(1);
        for(int i = 0; i < depList.size(); ++i) {
            String depStr = depList.get(i);
            String posStr = posList.get(i);
            Object[] rst = extractActionInstanceFromSentence(depStr, posStr, 0);
            String toWrite = (String) rst[0];
            resultStr += toWrite;
        }
        return resultStr;
    }

    public String getAllInstancesFromParsedAndPostag(String parsed, String postag)throws Exception{
        Object[] rst = extractActionInstanceFromSentence(parsed, postag, 0);
        String resultStr = (String) rst[0];
        return resultStr;
    }

    public void test(String doc) throws Exception {
        String sentence = doc;
        Parser parser = new Parser();
        List<List<String>> result = parser.dependencyParseDocument(sentence);
        List<String> depList = result.get(0);
        List<String> posList = result.get(1);
        for(int i = 0; i < depList.size(); ++i) {
            String depStr = depList.get(i);
            String posStr = posList.get(i);
            Object[] rst = extractActionInstanceFromSentence(depStr, posStr, 0);
            String toWrite = (String) rst[0];
            System.out.println(i + ":" + toWrite);
        }
        pb.disconnect();
    }

    public void run(){
        File fileToWrite = new File(OUT_PATH + part + ".txt");
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileToWrite));
            BufferedReader parsedNewsReader = new BufferedReader(new FileReader(BING_NEWS_PARSED_URL + "bing_news_parsed_" + part + ".txt"));
            BufferedReader postagNewsReader = new BufferedReader(new FileReader(BING_NEWS_POSTAG_URL + "bing_news_pos_" + part + ".txt"));
            String line = null;
            int foundNumber = 0; // cnt = -1, which means the first news is numbered as 0
            int newsIdx = -1;
            int oldIdx = -1;
            while((line = parsedNewsReader.readLine())!=null){
                newsIdx = Integer.parseInt(line.split("\t")[0]);
                Integer lineNumber = newsIdx % offset;
                String parsed = line.split("\t")[1];
                line = postagNewsReader.readLine();
                String postag = line.split("\t")[1];
                if(newsIdx % 1000 == 0){
                    System.out.println("part:" + part +" read:" + lineNumber + "(total:" + offset + ")" + " found:" + foundNumber);
                }

                Object[] rst = extractActionInstanceFromSentence(parsed, postag, foundNumber);
                String toWrite = (String) rst[0];
                foundNumber = (int) rst[1];
                if(toWrite.length() != 0){
                    if(oldIdx == newsIdx){
                        bw.append(toWrite);
                    }else{
                        bw.newLine();
                        bw.append(newsIdx + toWrite);
                        oldIdx = newsIdx;
                    }
                    bw.flush();
                }
            }
            parsedNewsReader.close();
            postagNewsReader.close();
            bw.close();
            pb.disconnect();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println(e);
        }
    }

    private Object[] extractActionInstanceFromSentence(String parsed, String postag, int foundNumber)throws Exception{
        String[] posStr = postag.trim().replaceAll("\\[|\\]","").split(",\\s+");
        String[] parsedStr = parsed.trim().split("\\)");
        List<Set<Pair>> dependencyTree = new ArrayList<>();
        for(int i = 0; i < posStr.length + 1; ++i){
            dependencyTree.add(new HashSet<>());
        }

        for(String parsedPart : parsedStr ) {
            String dep = parsedPart.substring(0, parsedPart.indexOf("(")).trim();
            String content = parsedPart.substring(parsedPart.indexOf("(") + 1).trim();
            String[] sp = content.split(",\\s+");
            int left = Integer.parseInt(sp[0].substring(sp[0].lastIndexOf("-") + 1));
            int right = Integer.parseInt(sp[1].substring(sp[1].lastIndexOf("-") + 1));
            Set<Pair> set = dependencyTree.get(left);
            set.add(new Pair(dep,right));
        }


        /*
         * Initialize verbIds list and tokens list.
         * verbIds list stores indeces of verbs, tokens list stores tokens
         * in the original sentence, in the original order.
         */
        int cnt = 1;
        List<Integer> verbIds = new ArrayList<Integer>();
        List<String> tokens = new ArrayList<String>();
        tokens.add("ROOT");
        for(String pos : posStr){
            pos = pos.trim();
            String sp[] = pos.split("/");
            String token = sp[0];
            String TAG = sp[1];

            if(verbTagSet.contains(TAG)){
                verbIds.add(cnt);
            }
            tokens.add(token);
            cnt++;
        }

	    /*
	     * Search actions in the document.
	     */
        List<Action> resultActionList = searchAction(tokens, dependencyTree, verbIds);
        Object[] output;
        output = writeAction(resultActionList, foundNumber);
        return output;

    }


    private Object[] writeAction(List<Action> resultActionList, int foundNumber)throws Exception{
        String toWrite = "";
        for(Action ac : resultActionList){
            String subj = ac.getSubj();
            String verb = ac.getVerb();
            String obj = ac.getObj();
            if(specialList.contains(subj)) {
                subj = specialSignExplained[ specialList.indexOf(subj) ];
            }

            if(specialList.contains(obj)) {
                obj = specialSignExplained[ specialList.indexOf(obj) ];
            }

            if(thingPronList.contains(subj)){
                subj = "thing";
            }
            if(thingPronList.contains(obj)) {
                obj = "thing";
            }
            //System.out.println(subj + " " + verb + " " +obj);

            if(verbList == null) {
                toWrite += "\t" + subj + "_" + verb + "_" + obj;
            } else {
                for(int i = 0; i < verbList.size(); ++i){
                    String verbInList = verbList.get(i);
                    Set<String> inflect = inflectionList.get(i);
                    if(inflect.contains(verb)){
                        foundNumber ++;
                        toWrite += "\t" + subj + "_" + verbInList + "_" + obj;
                    }
                }
            }

        }

        Object[] result = new Object[2];
        result[ 0 ] = toWrite;
        result[ 1 ] = foundNumber;
        return result;
    }

    /*
     * This method search actions in the document, it starts searching from
     * each verb stored in the verbIds list.
     */
    private List<Action> searchAction(List<String> tokens, List<Set<Pair>> dependencyTree, List<Integer> verbIds) throws Exception{
        List<Action> output = new ArrayList<>();
        for(Integer id : verbIds){
            output.addAll(searchActionForVerb(tokens,dependencyTree,id));
        }
        return output;
    }


    /*
     * This method search action given a certain verbId, it might find multiple actions given a single verb.
     */
    private List<Action> searchActionForVerb(List<String> tokens, List<Set<Pair>> dependencyTree, int verbId) throws Exception{
        String verb= tokens.get(verbId);
        Set<Pair> verbChildrenSet = dependencyTree.get(verbId);
        List<Action> output = new ArrayList<>();

		/*
		 * Traverse children of the verb node, search for subjects and objects.
		 */
        List<Integer> subjs = new ArrayList<>();
        List<Integer> objs = new ArrayList<>();
        boolean isPassive = false;
        List<Integer> buffer = new ArrayList<>();

        for(Pair verbRelatedPair : verbChildrenSet){

            if(verbRelatedPair.getDep().equals("compound:prt")) {
                int id = verbRelatedPair.getPos();
                verb = verb + " " + tokens.get(id);
            }

            if(verbRelatedPair.getDep().equals("nsubj")){
                int id = verbRelatedPair.getPos();
                subjs.add(id);
            }else if(verbRelatedPair.getDep().equals("nmod")){
                int id = verbRelatedPair.getPos();
                buffer.add(id);
            }else if(verbRelatedPair.getDep().equals("nsubjpass")){
                int id = verbRelatedPair.getPos();
                isPassive = true;
                objs.add(id);
            }else if(verbRelatedPair.getDep().equals("dobj")){
                int id = verbRelatedPair.getPos();
                objs.add(id);
            }else if(verbRelatedPair.getDep().equals("xcomp")){
                int id = verbRelatedPair.getPos();
                objs.add(id);
            }
        }

        if(isPassive) {
            subjs.addAll(buffer);
        }

        for(Integer subj : subjs){
            String subject = getSubTree(dependencyTree, tokens, subj);
            if(subject.replaceAll("[^a-zA-Z'.]","").length() != subject.replaceAll("\\s+","").length()) continue;
            if(personPronList.contains(subject.toLowerCase())) {
                subject = "person";
            }


            if(objs.isEmpty()) {
                output.add(new Action(subject, verb, ""));
            }

            for(Integer obj : objs){
                String object= getSubTree(dependencyTree, tokens, obj);
                if(personPronList.contains(object.toLowerCase())) {
                    object = "person";
                }

                if(subject.equals("") || object.equals("")){
                    continue;
                }

                Action tmp = new Action(subject, verb, object);
                output.add(tmp);
            }
        }
        return output;

    }

    /*
     * This method search subtree of a given id, and output the longest probase entity as a string
     */
    private String getSubTree(List<Set<Pair>> dependencyTree, List<String> tokens, int id) throws Exception {
        String head = tokens.get(id);
        String stem = head;
        stem = wn.stemNounFirst(head);

        // Be careful when change head word to its stem, because wn might fail to find the right stem
        if(!head.toLowerCase().equals(stem) && !stem.equals("")){
            head = stem;
        }
        String result = head;
        Set<Pair> subTree = dependencyTree.get(id);
        for (Pair subTreePair : subTree) {
            String dep = subTreePair.getDep();
            if (dep.equals("nmod")) {
                Integer subHead = subTreePair.getPos();
                Set<Pair> subSubTree = dependencyTree.get(subHead);
                for (Pair subSubPair : subSubTree) {
                    if (subSubPair.getDep().equals("case")) {
                        if (tokens.get(subSubPair.getPos()).equals("of") && measureWords.contains(head)) {
                            result = getSubTree(dependencyTree, tokens,subTreePair.getPos() );
                            //result = tokens.get(subTreePair.getPos());
                            //System.out.println(result);
                            return result;
                        }
                    }
                }
            }
            if (dep.equals("compound") || dep.equals("amod")) {
                String modifier = tokens.get(subTreePair.getPos());
                if (pb.isProbaseEntity(modifier + " " + head)) {
                    result = modifier + " " + head;
                }
            }

            if(dep.equals("case")) {
                if(tokens.get(subTreePair.getPos()).equals("in")) {
                    return "";
                }
            }
        }

        return result;
    }
}