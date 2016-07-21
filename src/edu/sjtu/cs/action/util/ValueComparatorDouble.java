package edu.sjtu.cs.action.util;

import java.util.Comparator;
import java.util.Map;

/**
 * Created by xinyu on 6/17/2016.
 */
public class ValueComparatorDouble implements Comparator<String>
{
    Map<String, Double> base;
    public ValueComparatorDouble( Map<String, Double> base)
    {
        this.base = base;
    }

    public int compare(String a, String b)
    {
        if(base.get(a) >= base.get(b))
            return -1;
        else
            return 1;
    }
}