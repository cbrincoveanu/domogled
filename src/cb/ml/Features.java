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
        StringBuilder s = new StringBuilder();
        for (String k : features.keySet()) {
            double value = features.get(k);
            if (Math.abs(value) > 100 || Math.abs(Math.round(value)- value) < 0.01) {
                s.append(Math.round(value));
            } else {
                s.append(String.format("%.2f", value));
            }
            s.append(",");
        }
        return s.toString();
    }

    public String keysToString() {
        StringBuilder s = new StringBuilder();
        for (String k : features.keySet()) {
            s.append(k);
            s.append(",");
        }
        return s.toString();
    }
}
