package edu.sjtu.cs.action.knowledgebase;

import edu.sjtu.cs.action.util.ValueComparatorInt;

import java.io.*;
import java.util.*;

/**
 * Created by xinyu on 6/24/2016.
 */
public class Probase {

    private final static String DICT_URL = "lib/probase/entity_dict_iteration.txt";
    private final static String MAT_URL = "lib/probase/matrix_iteration.txt";
    private final static String HYPON_URL = "lib/probase/hypoNum_iteration.txt";
    private final static String HYPEN_URL = "lib/probase/hyperNum_iteration.txt";
    private static HashMap<String,Integer> entityIdMap;
    private static List<String> entityList;
    private static HashSet<String> entitySet;
    private static HashMap<Integer, List<Integer>> conceptInstanceMap;
    private static HashMap<Integer, List<Integer>> instanceConceptMap;
    private static HashMap<List<Integer>, Integer[]> pairFreqMap;
    private static HashMap<String, Integer> conceptHypoNumMap;
    private static HashMap<String, Integer> instanceHyperNumMap;
    private static HashMap<String, Double> conceptVaguenessMap;

    public Probase()throws Exception{

    }

    public static void initialization()throws Exception{
        entityIdMap = new HashMap<>();
        entityList = new ArrayList<>();
        entitySet = new HashSet<>();
        conceptInstanceMap = new HashMap<>();
        instanceConceptMap = new HashMap<>();
        pairFreqMap = new HashMap<>();
        conceptHypoNumMap = new HashMap<>();
        instanceHyperNumMap = new HashMap<>();
        conceptVaguenessMap = new HashMap<>();
        loadProbaseData();
    }

    private static void loadProbaseData()throws Exception{
        BufferedReader dictReader = new BufferedReader(new FileReader(DICT_URL));
        BufferedReader matReader = new BufferedReader(new FileReader(MAT_URL));
        String line;

        System.out.println("Loading probase data...");
        long startTime = System.currentTimeMillis();
        System.out.println("--Loading dictionary...");
        while((line = dictReader.readLine()) != null){
            String[] splitted = line.split("\t");
            String entity = splitted[ 1 ];
            Integer id = Integer.parseInt(splitted[ 0 ]);
            entityIdMap.put( entity, id );
            entityList.add(entity);
            entitySet.add(entity);
        }
        dictReader.close();
        System.gc();

        int cnt = 0;
        System.out.println("--Loading relation matrix...");
        while((line = matReader.readLine())!=null){
            String[] splitted = line.split("\t");
            Integer conceptId = Integer.parseInt(splitted[0]), instanceId = Integer.parseInt(splitted[1]);
            Integer freq = Integer.parseInt(splitted[2]), popularity = Integer.parseInt(splitted[3]);
            Integer conceptSize = Integer.parseInt(splitted[4]);
            Double conceptVagueness = -1.0;
            if(!splitted[5].equals("NULL")){
                conceptVagueness = Double.parseDouble(splitted[5]);
            }

            if(!conceptInstanceMap.containsKey(conceptId)){
                conceptInstanceMap.put( conceptId, new ArrayList<>() );
            }

            conceptInstanceMap.get( conceptId ).add( instanceId );

            if(!instanceConceptMap.containsKey( instanceId ) ){
                instanceConceptMap.put( instanceId, new ArrayList<>() );
            }

            instanceConceptMap.get( instanceId ).add( conceptId );

            String concept = entityList.get(conceptId);
            String instance = entityList.get(instanceId);
            if(!conceptHypoNumMap.containsKey(concept)){
                conceptHypoNumMap.put(concept, conceptSize );
            }

            if(!conceptVaguenessMap.containsKey( concept ) ){
                conceptVaguenessMap.put( concept, conceptVagueness );
            }

            if(instanceHyperNumMap.containsKey(instance)){
                instanceHyperNumMap.put(instance, instanceHyperNumMap.get(instance) + 1 );
            }else{
                instanceHyperNumMap.put(instance, 1 );
            }

            Integer [] pair = { conceptId, instanceId };
            Integer [] freqPair = { freq, popularity };
            pairFreqMap.put(Arrays.asList(pair), freqPair);

            ++cnt;
            if(cnt % 1000000 == 0)
            {
                System.out.println( Double.toString( cnt / 1e6 ) + " million Lines loaded(69536566(69 million) lines in total )");
                System.gc();
            }
        }
        matReader.close();
        System.gc();

        System.out.println("Probase iteration 52 loaded.");
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Time elapsed: " + Long.toString(elapsedTime/1000) + "sec");
    }

    public Integer getFreq(String concept, String instance)throws Exception{
        Integer result = 0;
        if(entityIdMap.containsKey(concept) && entityIdMap.containsKey(instance)){
            Integer conceptId = entityIdMap.get(concept);
            Integer instanceId = entityIdMap.get(instance);
            Integer[] key = { conceptId, instanceId};
            if(pairFreqMap.containsKey(Arrays.asList(key))){
                result = pairFreqMap.get(Arrays.asList(key))[0];
            }
        }
        return result;
    }

    public Integer getPopularity(String concept, String instance) throws Exception{
        Integer result = 0;
        if(entityIdMap.containsKey(concept) && entityIdMap.containsKey(instance)){
            Integer conceptId = entityIdMap.get(concept);
            Integer instanceId = entityIdMap.get(instance);
            Integer[] key = { conceptId, instanceId };
            if(pairFreqMap.containsKey(Arrays.asList(key))){
                result = pairFreqMap.get(Arrays.asList(key))[1];
            }
        }
        return result;
    }

    private boolean isProbaseEntity(String entity)throws Exception{
        return entitySet.contains(entity);
    }

    private boolean isPair(String concept, String instance)throws Exception{
        concept = concept.trim().toLowerCase();
        instance = instance.trim();
        if(entitySet.contains(concept) && entitySet.contains(instance)){
            if(concept.equals(instance))return true;
            Integer conceptId = entityIdMap.get(concept);
            Integer instanceId = entityIdMap.get(instance);
            if(!conceptInstanceMap.containsKey(conceptId)){
                return false;
            }

            List<Integer> instanceList = conceptInstanceMap.get(concept);
            if(instanceList.contains(instanceId))return true;
        }
        return false;
    }

    public List<String> findHypo(String concept){
        List<String> hypoList = new ArrayList<>();
        if(entityIdMap.containsKey(concept)){
            Integer conceptId = entityIdMap.get(concept);
            if(conceptInstanceMap.containsKey(conceptId)){
                List<Integer> tmpList = conceptInstanceMap.get(conceptId);
                for(Integer instanceId : tmpList){
                    hypoList.add(entityList.get(instanceId));
                }
            }
        }
        return hypoList;
    }

    public List<String> findHyper(String instance){
        List<String> hyperList = new ArrayList<>();
        if(entityIdMap.containsKey(instance)){
            HashMap<String, Integer> hyper2Freq = new HashMap<>();
            Integer instanceId = entityIdMap.get(instance);
            if( instanceConceptMap.containsKey(instanceId) ){
                List<Integer> tmpList = instanceConceptMap.get(instanceId);
                for(Integer conceptId : tmpList){
                    String concept = entityList.get(conceptId);
                    Integer[] pair = {conceptId, instanceId};
                    hyper2Freq.put(concept, pairFreqMap.get(Arrays.asList(pair))[ 1 ]);
                }
                ValueComparatorInt bvc = new ValueComparatorInt(hyper2Freq);
                TreeMap<String, Integer> sortedMap = new TreeMap<>(bvc);
                sortedMap.putAll(hyper2Freq);

                for(String concept : sortedMap.keySet()){
                    hyperList.add(concept);
                }
            }
        }
        return hyperList;
    }

    public int getHypoNumber(String concept)throws Exception{
        int result = 0;
        if(conceptHypoNumMap.containsKey(concept)){
            result = conceptHypoNumMap.get(concept);
        }
        return result;
    }

    public int getHyperNumber(String instance)throws Exception{
        int result = 0;
        if(instanceHyperNumMap.containsKey(instance)){
            result = instanceHyperNumMap.get(instance);
        }
        return result;
    }

    public Double getVagueness(String concept)throws Exception{
        Double result = -1.0;
        if(conceptVaguenessMap.containsKey(concept)){
            result = conceptVaguenessMap.get(concept);
        }
        return result;
    }
}
