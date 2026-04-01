package com.example.ldca.ml;

public class LogisticRegression {

    public double[] weights;
    double lr = 0.01;
    int epochs = 50;

    public void train(DataSet data){

        int features=data.cols;

        weights=new double[features];

        for(int epoch=0;epoch<epochs;epoch++){

            for(int i=0;i<data.rows;i++){

                double pred=sigmoid(dot(data.X[i],weights));
                double error=data.y[i]-pred;

                for(int j=0;j<features;j++){

                    weights[j]+=lr*error*data.X[i][j];

                }

            }

        }

    }

    public int predict(double[] x){

        double p=sigmoid(dot(x,weights));

        return p>0.5?1:0;

    }

    double dot(double[] a,double[] b){

        double s=0;

        for(int i=0;i<a.length;i++)
            s+=a[i]*b[i];

        return s;

    }

    double sigmoid(double z){

        return 1.0/(1.0+Math.exp(-z));

    }

}
