package com.edc.spring.util;

import java.util.Collection;

/**
 * Created by edward.coo on 2018/8/10.
 */
public class StringUtils {
    private StringUtils(){}

    public static boolean isEmpty(String str) {
        return str == null || str.equals("");
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * copy the given collection to a string array
     * the collection must contain string elements only
     * @param collection to copy
     * @return the string array
     */
    public static String[] toStringArray(Collection<String> collection) {
        if(collection == null){
            return null;
        }
        return collection.toArray(new String[collection.size()]);
    }

    /**
     * convert the first char to lowerCase
     * @param str
     * @return
     */
    public static String toLowerCaseFirst(String str){
        if(isEmpty(str)){
            return "";
        }
        char[] chars = str.toCharArray();
        if(Character.isUpperCase(chars[0])){
            chars[0]+=32;
        }
        return String.valueOf(chars);
    }

}
