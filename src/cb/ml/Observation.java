package cb.ml;

public class Observation {
    private Features features;
    private ObservationType type;
    private double guessFactor;

    public Observation(Features features, ObservationType type, double guessFactor) {
        this.features = features;
        this.type = type;
        this.guessFactor = guessFactor;
    }

    public Features getFeatures() {
        return features;
    }

    @Override
    public String toString() {
        return features.toString() + type.toString() + "," + String.format("%.2f", guessFactor);
    }
}
