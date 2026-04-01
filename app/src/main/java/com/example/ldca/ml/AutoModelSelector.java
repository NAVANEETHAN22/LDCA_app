package com.example.ldca.ml;

public class AutoModelSelector {

    public static String chooseModel(DataSet data) {
        if (data.rows < 200)       return "Naive Bayes";
        if (data.cols > 10)        return "Decision Tree";
        if (data.rows >= 500)      return "Random Forest";
        return "Logistic Regression";
    }

    /**
     * Returns a human-readable explanation of why a model was chosen.
     * Shown to the user when "Auto Select" is picked.
     */
    public static String getReason(DataSet data) {
        if (data.rows < 200)
            return "Naive Bayes — dataset has fewer than 200 rows (small dataset → Naive Bayes works well with limited samples)";
        if (data.cols > 10)
            return "Decision Tree — dataset has more than 10 features (high-dimensional → Decision Tree handles feature splits naturally)";
        if (data.rows >= 500)
            return "Random Forest — dataset has 500+ rows with few features (sufficient data for an ensemble → Random Forest gives better accuracy)";
        return "Logistic Regression — medium dataset with few features (clean linear boundary expected → Logistic Regression is reliable)";
    }

}
