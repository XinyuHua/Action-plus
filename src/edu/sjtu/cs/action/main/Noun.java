package edu.sjtu.cs.action.main;

import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.util.ValueComparatorInt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.*;

/**
 * Created by xinyu on 4/28/2016.
 */
public class Noun {
    final private static String POSTAGGED = "dat/news/bing_news_sliced_title_postag/";
    final private static String NOUN_DIR = "dat/noun_concept/";
    final private static String[] NOUN_TAG = {"NN", "NNS", "NNP", "NNPS"};
    private static HashSet<String> nounTagSet;
    private static Wordnet wn;
    private static List<String> actionalHyperList;
    private static final String[] ACTIONAL_HYPER_ARRAY = {"action", "activity", "act"
            , "process", "practice", "event", "behavior"};
    private ProbaseClient pc;
    private HashMap<String, Integer> nounFreq;

    final static private int NOUN_IDX = 4;
    final static private int GoodEntityThreshold = 10;
    final static private String NOUN_DICT_URL = NOUN_DIR + "noun_" + NOUN_IDX + ".dict";
    final static private String NOUN_FREQ_URL = NOUN_DIR + "noun.freq";
    final static private String NOUN_HIER_URL = NOUN_DIR + "noun_" + NOUN_IDX + ".hierarchy";

    public static void main(String[] args) throws Exception {
        Date rightNow;
        Locale currentLocale;
        DateFormat timeFormatter;
        DateFormat dateFormatter;
        String timeOutput;
        String dateOutput;
        long startTime = System.nanoTime();

        Noun noun = new Noun();
        noun.getNounFromCorpus();

        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.println("Time elapsed:" + duration / 1e9 + " sec");

        rightNow = new Date();
        currentLocale = new Locale("en");

        timeFormatter = DateFormat.getTimeInstance(DateFormat.DEFAULT, currentLocale);
        dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, currentLocale);

        timeOutput = timeFormatter.format(rightNow);
        dateOutput = dateFormatter.format(rightNow);

        System.out.println(timeOutput);
        System.out.println(dateOutput);
    }


    public Noun()throws Exception{
        wn = new Wordnet(false);
        nounFreq = new HashMap<>();
        nounTagSet = new HashSet<String>();
        for(String s : NOUN_TAG){
            nounTagSet.add(s);
        }
    }

    public void hierarchicalizeDict() throws Exception {
        BufferedReader dictReader = new BufferedReader(new FileReader(NOUN_DICT_URL));
        BufferedWriter hierarchyWriter = new BufferedWriter(new FileWriter( NOUN_HIER_URL ));
        List<String> nounDict = new ArrayList<>();
        String line;
        System.out.println("Reading original dictionary into memory...");
        while((line = dictReader.readLine())!= null) {
            nounDict.add(line.trim());
        }
        dictReader.close();

        pc = new ProbaseClient(4400);
        int i = 0;
        for(String noun : nounDict) {
            System.out.println("Finding parent nodes for " + i + "th noun...");
            i++;
            hierarchyWriter.append(noun);
            for(String candidate : nounDict) {
                if(candidate.equals(noun)) continue;
                if(pc.getPop(candidate, noun) > 20) {
                    hierarchyWriter.append("\t" + candidate);
                }
            }
            hierarchyWriter.newLine();
        }
        hierarchyWriter.close();
        pc.disconnect();
    }

    /*
    This method filter Noun pool obtained from corpus. When Option = 0, it uses Wordnet to filter.
    When Option = 1, it uses Probase to filter. Both try to find nouns appropriate for an action
    concept.
     */
    public void filterNounDict( int Option )throws Exception {
        BufferedReader rawDictReader = new BufferedReader(new FileReader( NOUN_FREQ_URL ));
        BufferedWriter dictWriter = new BufferedWriter(new FileWriter( NOUN_DICT_URL ));
        String line =null;

        if(Option == 0) {
            wn = new Wordnet(true);
            while((line = rawDictReader.readLine())!=null) {
                String verb = line.split(":")[0].toLowerCase();
                Integer freq = Integer.parseInt(line.split(":")[1]);
                if(!verb.replaceAll("[^a-z-]","").equals(verb) || verb.length() < 3 || freq < 10)continue;
                if(isGoodNounWN(verb)) {
                    dictWriter.append(verb);
                    dictWriter.newLine();
                }
            }
        } else {
            actionalHyperList = Arrays.asList(ACTIONAL_HYPER_ARRAY);
            pc = new ProbaseClient(4400);
            while((line = rawDictReader.readLine())!=null) {
                String verb = line.split(":")[0];
                if(isGoodNounPB(verb)) {
                    dictWriter.append(verb);
                    dictWriter.newLine();
                }
            }
            pc.disconnect();
        }

        dictWriter.close();
        rawDictReader.close();
    }

    private boolean isGoodNounPB(String noun) {
        try{
            // 1. contains non-alphabetical, returns false
            if(noun.replaceAll("[^a-z]","").length() != noun.length()) {
                return false;
            }

            if(!pc.isProbaseEntity(noun)) {
                return false;
            }

            if(pc.getHypoNumber(noun) < 5) {
                return false;
            }

            boolean isOfAny = false;
            for(String hyper : actionalHyperList) {
              //  if(pc.isPair(hyper, noun)) {
                if(pc.getPop(hyper, noun) > GoodEntityThreshold){
                    isOfAny = true;
                    break;
                }
            }

            if(!isOfAny){
                return false;
            }


        }catch(Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private static boolean isGoodNounWN(String noun) {
        try{
            // 1. contains non-alphabetical, returns false
            if(noun.replaceAll("[^a-z]","").length() != noun.length()) {
                return false;
            }

            if(!wn.isNoun(noun)) {
                return false;
            }

        }catch(Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public void getNounFromCorpus()throws Exception {
        for(int i = 0; i < 100; ++i) {
            BufferedReader br = new BufferedReader( new FileReader( POSTAGGED + "bing_news_title_pos_" + i + ".txt"));
            String line;
            while((line = br.readLine()) != null) {
                String[] sp = line.split("\t");
                System.out.println(i + "\t" + sp[0]);
                List<String> tmp = getNounFromPostag(sp[1]);
                for(String s : tmp) {
                    if(nounFreq.containsKey(s)) {
                        nounFreq.put(s, nounFreq.get(s) + 1);
                    }else {
                        nounFreq.put(s, 1);
                    }
                }
                System.out.println("Size " + nounFreq.size());
            }
            br.close();
        }

        ValueComparatorInt bvc = new ValueComparatorInt( nounFreq );
        TreeMap<String, Integer> sortedMap = new TreeMap<>(bvc);
        sortedMap.putAll(nounFreq);

        BufferedWriter bw = new BufferedWriter(new FileWriter( NOUN_FREQ_URL ));
        for(String s : sortedMap.keySet()) {
            bw.append(s + ":" + nounFreq.get(s));
            bw.newLine();
        }
        bw.close();
    }


    private static List<String> getNounFromPostag(String postagged) throws Exception {
        List<String> result = new ArrayList<String>();
        String[] posStr = postagged.trim().replaceAll("\\[|\\]", "").split(",\\s+");
        for (String pos : posStr) {
            pos = pos.trim();
            String sp[] = pos.split("/");
            String token = sp[0];
            String TAG = sp[1];
            if (nounTagSet.contains(TAG) ) {
                result.add(wn.stemNounFirst(token));
            }
        }
        return result;

    }
}
