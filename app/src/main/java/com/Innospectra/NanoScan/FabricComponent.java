package com.Innospectra.NanoScan;

import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FabricComponent {
    private LinkedHashMap<String, Double> fiberComposition = new LinkedHashMap<>();
    private String checkResult;
    private String SampleIdentity;
    public FabricComponent(String checkResult, String SampleIdentity) {
        this.checkResult = checkResult;
        this.SampleIdentity = SampleIdentity;
        try {
            Pattern pattern = Pattern.compile("([\\u4e00-\\u9fa5]+)\\s+([0-9.]+)");
//            Pattern pattern = Pattern.compile("([\\\\u4e00-\\\\u9fa5]+)(?:(?!\\\\s*[0-9.]+).)*\\\\s+([0-9.]+)");
            Matcher matcher = pattern.matcher(checkResult);

            while (matcher.find()) {
                System.out.println(matcher.group(1).trim() + " " + matcher.group(2).trim());
                fiberComposition.put(matcher.group(1).trim(), Double.parseDouble(matcher.group(2).trim()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LinkedHashMap<String, Double> getFiberComposition() {
        return fiberComposition;
    }

    public String getCheckResult() {
        return checkResult;
    }

    public String getSampleIdentity() {
        return SampleIdentity;
    }
}
