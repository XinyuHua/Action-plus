package edu.sjtu.cs.action.runner;

import edu.sjtu.cs.action.knowledgebase.Probase;
import edu.sjtu.cs.action.knowledgebase.Wordnet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by xinyu on 6/30/2016.
 */
public class extractActionConceptGlobalRunner implements Runnable {
    private static String INPUT;
    private static String OUTPUT;
    private Probase pb;
    private int part;
    private int offset;
    private static HashSet<String> subjSet;
    private static HashSet<String> objSet;
    private static List<String> specialList;
    private static Wordnet wordnet;
    private static boolean PRINT_INSTANCE = true;
    public extractActionConceptGlobalRunner(int part, int offset, HashSet<String> subjS, HashSet<String> objS,
                                            Probase pb, Wordnet wn, String input,String output){
        this.part = part;
        this.offset = offset;
        this.subjSet = subjS;
        this.objSet = objS;
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
        try {
            BufferedReader br = new BufferedReader(new FileReader(INPUT + part + ".txt"));
            BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT + part + ".txt"));
            String line;
            int curLineNumber = 0;
            int acFound = 0;
            List<String> instanceDat = new ArrayList<>();
            br.readLine(); //First line is always blank, so read it out first
            while ((line = br.readLine()) != null) {
                instanceDat.add(line);
            }

            for (String instanceLine : instanceDat) {
                if (curLineNumber % 500 == 0) {
                    System.out.println("file id:" + part + "\tline num:" + curLineNumber + "\tac found:" + acFound);
                }

                curLineNumber++;
                String[] lineSplit = instanceLine.split("\t");
                String globalIdx = lineSplit[0];
                String outputBuffer = "";

                for (int k = 1; k < lineSplit.length; ++k) {
                    String instance = lineSplit[k];

                    String[] instanceSplit = instance.split("_");
                    String verb = "", obj = "", subj = "";
                    String subjC = "", objC = "";
                    if (instanceSplit.length == 3) {
                        obj = instanceSplit[2].trim();
                        // obj = wordnet.stemNounFirst(obj);
                    }
                    subj = instanceSplit[0].trim();
                    // if(!subj.equals("")) subj = wordnet.stemNounFirst(subj);
                    verb = instanceSplit[1];

                    if (!subjSet.contains(subj) && !objSet.contains(obj)) continue;

                    if (specialList.contains(subj)) {
                        subjC = subj;
                    } else if(subj.length() == 0){
                        subjC = "";
                    } else {
                        List<String> subjHyperList = pb.findHyper(subj);
                        for(String hyper : subjHyperList){
                            if(subjSet.contains(hyper)){
                                subjC = hyper;
                                break;
                            }
                        }
                    }

                    if (specialList.contains(obj)) {
                        objC = obj;
                    } else if(obj.length() == 0) {
                        objC = "";
                    } else {
                        List<String> objHyperList = pb.findHyper(obj);
                        for(String hyper : objHyperList){
                            if(objSet.contains(hyper)){
                                objC = hyper;
                                break;
                            }
                        }
                    }

                    if(subjC.length() == 0 && objC.length() == 0)continue;

                    if (PRINT_INSTANCE) {
                        outputBuffer += "\t" + subj + "_" + verb + "_" + obj + ":";
                    }
                    outputBuffer += subjC + "_" + verb + "_" + objC;
                }

                if (outputBuffer.length() > 0) {
                    bw.append(globalIdx + outputBuffer);
                    bw.newLine();
                    bw.flush();
                }
            }
            br.close();
            bw.close();
            // pb.disconnect();
        }catch (Exception e){
            e.printStackTrace();
            System.err.println("Error opening file!");
        }
    }
}
