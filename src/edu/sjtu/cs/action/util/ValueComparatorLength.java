package edu.sjtu.cs.action.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by xinyu on 6/17/2016.
 */
public class ValueComparatorLength implements Comparator<String> {

    HashMap<String, HashSet<String>> map = new HashMap<>();

    public ValueComparatorLength(HashMap<String, HashSet<String>> map){
        this.map.putAll(map);
    }

    @Override
    public int compare(String s1, String s2) {
        if(map.get(s1).size() >= map.get(s2).size()){
            return -1;
        }else{
            return 1;
        }
    }
}
