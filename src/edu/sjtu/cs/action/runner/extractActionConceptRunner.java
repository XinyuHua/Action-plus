package edu.sjtu.cs.action.runner;

import edu.sjtu.cs.action.knowledgebase.Probase;
import edu.sjtu.cs.action.knowledgebase.Wordnet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by xinyu on 5/18/2016.
 * Last modified on 5/31/2016
 */
public class extractActionConceptRunner implements Runnable {
    private static String INPUT;
    private static String OUTPUT;
    private Probase pb;
    private int part;
    private int offset;
    private static HashMap<String, List<String>> verb2subj;
    private static HashMap<String, List<String>> verb2obj;
    private static List<String> specialList;
    private static Wordnet wordnet;
    private static boolean MATCH = false;
    private static boolean PRINT_INSTANCE = true;
    public extractActionConceptRunner(int part, int offset, HashMap<String, List<String>> v2s,
                                  HashMap<String, List<String>> v2o, Probase pb, Wordnet wn,
                                  String input,String output){
        this.part = part;
        this.offset = offset;
        this.verb2obj = v2o;
        this.verb2subj = v2s;
        this.pb = pb;
        this.wordnet = wn;
        this.INPUT = input;
        this.OUTPUT = output;
        specialList = new ArrayList<>();
        specialList.add("person");
        specialList.add("thing");
        specialList.add("percent");
        specialList.add("money");

    }


    @Override
    public void run() {
        try{
            BufferedReader br = new BufferedReader(new FileReader(INPUT + part + ".txt"));
            BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT + part + ".txt"));
            String line2;
            int curLineNumber = 0;
            int acFound = 0;
            List<String> instanceDat = new ArrayList<>();
            br.readLine(); //First line is always blank, so read it out first
            while((line2 = br.readLine())!=null){
                instanceDat.add(line2);
            }
            br.close();

            for(String line : instanceDat) {
                if(curLineNumber % 500 == 0){
                    System.out.println("file id:" + part + "\tline num:" + curLineNumber + "\tac found:" + acFound);
                }

                curLineNumber++;
                String[] lineSplit = line.split("\t");
                String globalIdx = lineSplit[0];
                String outputBuffer = "";

                for(int k = 1; k < lineSplit.length; ++k) {
                    String instance = lineSplit[k];

                    String[] instanceSplit = instance.split("_");
                    String verb = "", obj = "", subj = "";
                    if (instanceSplit.length == 3) {
                        obj = instanceSplit[2].trim();
                        // obj = wordnet.stemNounFirst(obj);
                    }
                    subj = instanceSplit[0].trim();
                    // if(!subj.equals("")) subj = wordnet.stemNounFirst(subj);
                    verb = instanceSplit[1];

                    if (!verb2subj.containsKey(verb) && !verb2obj.containsKey(verb)) continue;

                    List<String> subjListFromVerb = verb2subj.get(verb);
                    List<String> objListFromVerb = verb2obj.get(verb);

                    List<String> subjConcept = new ArrayList<>();
                    List<String> objConcept = new ArrayList<>();

                    if (!MATCH) {
                        if (specialList.contains(subj)) {
                            subjConcept.add(subj);
                        } else {
                            for (String subjc : subjListFromVerb) {
                                if (pb.getFreq(subjc, subj) > 10) {
                                    subjConcept.add(subjc);
                                }
                            }
                        }

                        if (specialList.contains(obj)) {
                            objConcept.add(obj);
                        } else {
                            for (String objc : objListFromVerb) {
                                if (pb.getFreq(objc, obj) > 10) {
                                    objConcept.add(objc);
                                }
                            }
                        }

                        if (PRINT_INSTANCE) {
                            outputBuffer += "\t" + subj + "_" + verb + "_" + obj + "(";
                        }
                        if (subjConcept.size() > 0 && objConcept.size() > 0) {
                            for (String subjc : subjConcept) {
                                for (String objc : objConcept) {
                                    acFound++;
                                    if (PRINT_INSTANCE) {
                                        outputBuffer += subjc + "_" + verb + "_" + objc + ",";
                                    }
                                }
                            }
                        } else if (subjConcept.size() == 0) {
                            for (String objc : objConcept) {
                                acFound++;
                                outputBuffer += "\t_" + verb + "_" + objc + ",";
                            }
                        } else {
                            for (String subjc : subjConcept) {
                                acFound++;
                                outputBuffer += "\t" + subjc + "_" + verb + "_,";
                            }
                        }
                    } else {
                        int argumentIdx = 0;
                            if(subj.equals("")){
                            for(String objc : objListFromVerb) {
                                if(pb.getFreq(objc, obj) > 10){
                                    subjConcept.add("");
                                    objConcept.add(objc);
                                    break;
                                }
                            }
                        }else{
                            for(String subjc : subjListFromVerb) {
                                String objc = objListFromVerb.get(argumentIdx);
                                // subjc and objc are correspondent arguments for the current verb
                                argumentIdx++;
                                //System.out.println(subjc + "\t" + objc);
                                if(pb.getFreq(subjc, subj) > 10 && pb.getFreq(objc, obj) > 10){
                                    subjConcept.add(subjc);
                                    objConcept.add(objc);
                                    break;
                                }
                            }
                        }


                        if(subjConcept.size() > 0) {
                            int n = 0;
                            for (String subjc : subjConcept) {
                                String objc = objConcept.get(n);
                                n++;
                                acFound++;
                                if(PRINT_INSTANCE){
                                    outputBuffer += "\t" + subjc + "_" + verb + "_" + objc + "(" + subj + "_" + verb + "_" + obj + ")";
                                }else{
                                    outputBuffer += "\t" + subjc + "_" + verb + "_" + objc;
                                }
                            }
                        }
                    }
                }

                if(outputBuffer.length() > 0){
                    bw.append(globalIdx + outputBuffer);
                    bw.newLine();
                    bw.flush();
                }

            }

            bw.close();
           // pb.disconnect();
        }catch (Exception e){
            e.printStackTrace();
            System.err.println("Error opening file!");
        }
    }
}
