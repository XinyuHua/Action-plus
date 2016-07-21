package edu.sjtu.cs.action.main;

import edu.sjtu.cs.action.knowledgebase.Probase;
import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.runner.extractActionConceptGlobalRunner;
import edu.sjtu.cs.action.runner.extractActionConceptRunner;
import edu.sjtu.cs.action.runner.extractActionInstanceRunner;
import edu.sjtu.cs.action.runner.extractNounRunner;
import edu.sjtu.cs.action.util.Lemmatizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by xinyu on 6/17/2016.
 *
 * This class extracts action instance or noun from corpus
 */
public class Extraction {

    private static final Integer NOUN_DICT_ID = 4;
    private static final Integer AC_DICT_ID = 1;
    private static final String NOUN_DICT_URL = "dat/noun/" + NOUN_DICT_ID + ".dict";
    private static final String AC_DICT_URL = "dat/action/action_" + AC_DICT_ID + ".dict";
    private static final String NEWS_URL = "dat/news/bing_news_sliced/";
    private static final String NEWS_PARSED_URL = "dat/news/bing_news_sliced_parsed/";
    private static final String NEWS_POSTAG_URL = "dat/news/bing_news_sliced_postag/";
    private static final String NEWS_DEBUG = "dat/news/debug.txt";
    private static final String VERB_URL = "dat/action/verb/verb.dict";
    private static final String INFLECTION_URL = "dat/action/verb/inflection.dict";

    private static final String NOUN_OUTPUT = "dat/noun/extracted_" + NOUN_DICT_ID + "/";
    private static final String ACI_T_OUTPUT = "dat/action/instance_extracted/with_targeted_verbs/";
    private static final String ACI_OUTPUT = "dat/action/instance_extracted/";
    private static final String ACC_OUTPUT = "dat/action/concept_extracted/yu_10_filtered/";
    private static final String ACC_GLB_OUTPUT = "dat/action/concept_extracted/yu_10_global/";

    private static final String ARG_URL = "dat/argument/yu_10_filtered/";
    private static final String ARG_GLB = "dat/argument/yu_10_global/";
    private int lowerBound;
    private int upperBound;
    private int offset;

    private String[] puncArray = {",", ".", "!", "-", "?"};
    private static List<String> puncList;

    List<String> verbList;
    List<Set<String>> inflectionList;
    HashSet<String> subjSet;
    HashSet<String> objSet;

    public Extraction(int l, int u, int o){
        this.lowerBound = l;
        this.upperBound = u;
        this.offset = o;
        puncList = Arrays.asList(puncArray);
        verbList = new ArrayList<>();
        inflectionList = new ArrayList<>();
    }


    public void testExtractionInstance()throws Exception{
        loadVerbAndInflection();
        extractActionInstanceRunner e = new extractActionInstanceRunner(verbList, inflectionList);
        BufferedReader testReader = new BufferedReader(new FileReader(NEWS_DEBUG));
        String doc = testReader.readLine();
        testReader.close();
        System.out.println(e.getAllInstancesFromText(doc));
    }

    public void extractNounFromNewsTitle()throws Exception{

        // Load noun dictionary
        BufferedReader dictReader = new BufferedReader(new FileReader( NOUN_DICT_URL ));
        HashSet<String> actionalSet = new HashSet<String>();
        String line = null;
        while((line = dictReader.readLine())!=null){
            if(!line.contains("_") && !line.contains("-")){
                actionalSet.add(line.toLowerCase());
            }
        }
        dictReader.close();
        File toCreate = new File(NOUN_OUTPUT);
        toCreate.mkdirs();

        // Start searching for nouns in news title
        int size = upperBound - lowerBound;
        Runnable[] eNA = new Runnable[size];
        for(int i = 0; i < size; ++i){
            eNA[ i ] = new extractNounRunner(i + lowerBound, actionalSet, offset, NOUN_OUTPUT, NEWS_URL);
        }

        Thread[] tdA = new Thread[size];
        for(int i = 0; i < size; ++i){
            tdA[ i ] = new Thread(eNA[ i ]);
            tdA[ i ].start();
        }

        for(int i = 0; i < size; ++i){
            tdA[ i ].join();
        }
        return;
    }

    /*
    This method extracts action instances from parsed corpus, the
     */
    public void extractActionInstanceFromNewsBody()throws Exception{
        int size = upperBound - lowerBound;

        loadVerbAndInflection();
        Wordnet wn = new Wordnet(true);
        ProbaseClient[] pcs = new ProbaseClient[size];
        for(int i = 0; i < size; ++i){
            pcs[ i ] = new ProbaseClient(4400 + i);
        }

        Runnable[] eNA = new Runnable[size];
        for(int i = 0; i < size; ++i){
            eNA[ i ] = new extractActionInstanceRunner(i + lowerBound, pcs[ i ], wn, verbList,
                                                        inflectionList, puncList, ACI_OUTPUT, offset);
        }

        Thread[] tdA = new Thread[size];
        for(int i = 0; i < size; ++i){
            tdA[ i ] = new Thread(eNA[ i ]);
            tdA[ i ].start();
        }

        for(int i = 0; i < size; ++i){
            tdA[ i ].join();
        }
        return;
    }


    public void extractActionConceptGlobally()throws Exception{
        loadArgumentGlobal();
        int size = upperBound - lowerBound;

        Wordnet wn = new Wordnet(true);

        Probase[] pcs = new Probase[size];
        for(int i = 0; i < size; ++i){
            pcs[ i ] = new Probase();
        }
        Probase.initialization();

        Runnable[] eNA = new Runnable[size];
        for(int i = 0; i < size; ++i){
            eNA[ i ] = new extractActionConceptGlobalRunner(i + lowerBound, offset, subjSet, objSet,
                    pcs[ i ], wn, ACI_OUTPUT, ACC_GLB_OUTPUT );
        }

        Thread[] tdA = new Thread[size];
        for(int i = 0; i < size; ++i){
            tdA[ i ] = new Thread(eNA[ i ]);
            tdA[ i ].start();
        }

        for(int i = 0; i < size; ++i){
            tdA[ i ].join();
        }

        return;
    }

    public void extractActionConceptFromActionInstance()throws Exception{
        int size = upperBound - lowerBound;

        HashMap<String, List<String>> verb2Subj = new HashMap<>();
        HashMap<String, List<String>> verb2Obj = new HashMap<>();
        String line;
        String[] splitted;
        File folder = new File(ARG_URL);
        for(File file : folder.listFiles()){
            String verb = file.getName().replace(".txt","");
            BufferedReader fileReader = new BufferedReader(new FileReader(file));
            List<String> subjList = new ArrayList<>();
            List<String> objList = new ArrayList<>();
            line = fileReader.readLine().trim();
            if(line.length() > 0){
                splitted = line.split("\t");
                for(String subj : splitted){
                    subjList.add(subj);
                }
            }
            verb2Subj.put(verb, subjList);

            line = fileReader.readLine();
            if(line != null){
                line = line.trim();
                if(line.length() > 0){
                    splitted = line.split("\t");
                    for(String obj : splitted){
                        objList.add(obj);
                    }
                }
            }
            verb2Obj.put(verb, objList);
            fileReader.close();
        }


        Wordnet wn = new Wordnet(true);
        /*
        ProbaseClient[] pcs = new ProbaseClient[size];
        for(int i = 0; i < size; ++i){
            pcs[ i ] = new ProbaseClient(4500 + i);
        }*/

        Probase[] pcs = new Probase[size];
        for(int i = 0; i < size; ++i){
            pcs[ i ] = new Probase();
        }
        Probase.initialization();

        Runnable[] eNA = new Runnable[size];
        for(int i = 0; i < size; ++i){
            eNA[ i ] = new extractActionConceptRunner(i + lowerBound, offset, verb2Subj, verb2Obj,
                    pcs[ i ], wn, ACI_OUTPUT, ACC_OUTPUT);
        }

        Thread[] tdA = new Thread[size];
        for(int i = 0; i < size; ++i){
            tdA[ i ] = new Thread(eNA[ i ]);
            tdA[ i ].start();
        }

        for(int i = 0; i < size; ++i){
            tdA[ i ].join();
        }

        return;
    }

    private void loadVerbAndInflection()throws Exception{
        BufferedReader verbReader = new BufferedReader(new FileReader( VERB_URL));
        BufferedReader inflReader = new BufferedReader(new FileReader( INFLECTION_URL));
        String line;
        while((line = verbReader.readLine())!=null){
            verbList.add(line);
        }
        verbReader.close();

        while((line = inflReader.readLine())!=null){
            inflectionList.add(new HashSet(Arrays.asList(line.split("\\s+"))));
        }
        inflReader.close();
    }

    private void loadArgumentGlobal()throws Exception{
        subjSet = new HashSet<>();
        objSet = new HashSet<>();
        BufferedReader subjReader = new BufferedReader(new FileReader(ARG_GLB + "subj.txt"));
        BufferedReader objReader = new BufferedReader(new FileReader(ARG_GLB + "obj.txt"));
        String line;
        while((line = subjReader.readLine())!=null){
            subjSet.add(line);
        }

        while((line = objReader.readLine()) != null){
            objSet.add(line);
        }

        subjReader.close();
        objReader.close();
    }
}