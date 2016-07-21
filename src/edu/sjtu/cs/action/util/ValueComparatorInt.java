package edu.sjtu.cs.action.util;

import java.util.Comparator;
import java.util.Map;

/**
 * Created by xinyu on 6/16/2016.
 */
public class ValueComparatorInt implements Comparator<String>
{
    Map<String, Integer> base;
    public ValueComparatorInt( Map<String, Integer> base)
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
