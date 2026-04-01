package com.example.ldca.ml;

import java.util.HashMap;
import java.util.Map;

public class NaiveBayes {

    Map<Integer, double[]> means = new HashMap<>();
    Map<Integer, double[]> variances = new HashMap<>();
    Map<Integer, Double> priors = new HashMap<>();

    public void train(DataSet data) {

        int n = data.rows;
        int features = data.cols;

        Map<Integer, Integer> classCounts = new HashMap<>();

        for (int i = 0; i < n; i++) {

            int label = data.y[i];
            classCounts.put(label, classCounts.getOrDefault(label,0)+1);

        }

        for (int c : classCounts.keySet()) {

            double[] mean = new double[features];
            double[] var = new double[features];

            int count = classCounts.get(c);

            for (int i = 0; i < n; i++) {

                if(data.y[i]==c){

                    for(int j=0;j<features;j++)
                        mean[j]+=data.X[i][j];

                }

            }

            for(int j=0;j<features;j++)
                mean[j]/=count;

            for (int i = 0; i < n; i++) {

                if(data.y[i]==c){

                    for(int j=0;j<features;j++){

                        double diff=data.X[i][j]-mean[j];
                        var[j]+=diff*diff;

                    }

                }

            }

            for(int j=0;j<features;j++)
                var[j]/=count;

            means.put(c,mean);
            variances.put(c,var);
            priors.put(c,(double)count/n);

        }

    }

    public int predict(double[] x){
        double bestScore = -Double.MAX_VALUE;
        int bestClass = -1;

        for(int c:means.keySet()){
            double score=Math.log(priors.get(c));
            double[] mean=means.get(c);
            double[] var=variances.get(c);

            for(int j=0;j<x.length;j++){
                double diff=x[j]-mean[j];
                score -= (diff*diff)/(2*var[j]+1e-9);
            }

            if(score>bestScore){
                bestScore=score;
                bestClass=c;
            }
        }
        return bestClass;
    }

    public double[] predictProba(double[] x) {
        double[] logProbs = new double[2]; // assuming binary classification 0 and 1
        
        for (int c : means.keySet()) {
            if (c > 1) continue; // safety for binary
            double score = Math.log(priors.get(c));
            double[] mean = means.get(c);
            double[] var = variances.get(c);

            for (int j = 0; j < x.length; j++) {
                double diff = x[j] - mean[j];
                score -= (diff * diff) / (2 * var[j] + 1e-9);
            }
            logProbs[c] = score;
        }

        // Softmax to convert log-probs to actual probabilities [0, 1]
        double maxLog = Math.max(logProbs[0], logProbs[1]);
        double exp0 = Math.exp(logProbs[0] - maxLog);
        double exp1 = Math.exp(logProbs[1] - maxLog);
        double sum = exp0 + exp1;
        
        return new double[]{ exp0 / sum, exp1 / sum };
    }
}
