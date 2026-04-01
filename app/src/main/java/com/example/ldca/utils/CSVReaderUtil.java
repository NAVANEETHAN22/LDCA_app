package com.example.ldca.utils;

import java.io.*;
import java.util.*;

public class CSVReaderUtil {

    public static List<String[]> readCSV(File file) {

        List<String[]> data = new ArrayList<>();

        try {

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while((line = br.readLine()) != null){

                String[] row = line.split(",");
                data.add(row);

            }

            br.close();

        } catch(Exception e){
            e.printStackTrace();
        }

        return data;

    }

}
