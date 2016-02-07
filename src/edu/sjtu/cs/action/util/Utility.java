package edu.sjtu.cs.action.util;

public class Utility {
	
	public static String convertFromUTF8(String s) {
        String out = null;
        try {
            out = new String(s.getBytes("ISO-8859-1"), "UTF-8");
            		
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
        return out;
    }
}
