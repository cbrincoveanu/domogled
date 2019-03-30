package cb.ml;

import java.util.TreeMap;

public class Features {
    private TreeMap<String, Double> features = new TreeMap<>();

    public void setFeature(String name, double value) {
        features.put(name, value);
    }

    public double getFeature(String name) {
        return features.get(name);
    }

    @Override
    public String toString() {
        String s = "";
        for (String k : features.keySet()) {
            s += String.format("%.2f", features.get(k)) + ",";
        }
        return s;
    }

    public String keysToString() {
        String s = "";
        for (String k : features.keySet()) {
            s += k + ",";
        }
        return s;
    }
}
