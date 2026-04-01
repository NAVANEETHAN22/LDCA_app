package com.example.ldca.utils;

import java.util.*;

public class CategoricalEncoder {

    Map<String,Integer> map = new HashMap<>();

    public int encode(String value){

        if(!map.containsKey(value)){
            map.put(value,map.size());
        }

        return map.get(value);

    }

}
