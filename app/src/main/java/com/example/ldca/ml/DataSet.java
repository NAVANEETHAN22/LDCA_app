package com.example.ldca.ml;

import java.util.List;

public class DataSet {

    public double[][] X;
    public int[] y;

    public int rows;
    public int cols;

    public DataSet(double[][] X, int[] y) {
        this.X = X;
        this.y = y;
        this.rows = X.length;
        this.cols = X[0].length;
    }

}
