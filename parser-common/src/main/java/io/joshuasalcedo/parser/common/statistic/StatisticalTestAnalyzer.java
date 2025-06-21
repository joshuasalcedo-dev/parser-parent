package io.joshuasalcedo.parser.common.statistic;

import io.joshuasalcedo.parser.common.Analyzer;
import io.joshuasalcedo.parser.common.model.StatisticalTestResult;
import org.apache.commons.math3.stat.inference.TTest;

import java.util.List;

// ====== STATISTICAL TEST ANALYZER ======
public class StatisticalTestAnalyzer implements Analyzer<StatisticalTestResult, List<double[]>> {
    
    @Override
    public StatisticalTestResult analyze(List<double[]> samples) {
        if (samples == null || samples.size() < 2) {
            return new StatisticalTestResult("t-test", 0.0, 1.0, 0.0, false, "Insufficient data");
        }
        
        double[] sample1 = samples.get(0);
        double[] sample2 = samples.get(1);
        
        TTest tTest = new TTest();
        double testStatistic = tTest.t(sample1, sample2);
        double pValue = tTest.tTest(sample1, sample2);
        
        // Critical value for alpha = 0.05, two-tailed test
        int df = sample1.length + sample2.length - 2;
        double criticalValue = 1.96; // Simplified - should use t-distribution
        
        boolean rejectNull = pValue < 0.05;
        String conclusion = rejectNull ? 
            "Reject null hypothesis: Significant difference between groups" :
            "Fail to reject null hypothesis: No significant difference";
        
        return new StatisticalTestResult(
            "Independent t-test",
            testStatistic,
            pValue,
            criticalValue,
            rejectNull,
            conclusion
        );
    }
}
