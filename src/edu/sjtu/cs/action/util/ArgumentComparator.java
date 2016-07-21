package edu.sjtu.cs.action.util;

/**
 * Created by xinyu on 16-6-20.
 */

public class ArgumentComparator implements Comparable{
    String name;
    int instanceNum;

    public ArgumentComparator(String name, int num){
        this.name = name;
        this.instanceNum = num;
    }

    public String getName(){
        return name;
    }

    @Override
    public int compareTo(Object o) {
        int oNum = ((ArgumentComparator)o).instanceNum;
        return this.instanceNum - oNum;
    }
}