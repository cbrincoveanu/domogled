package cb.ml;

public interface Targeting {
    void addPoint(Features features, double guessFactor, ObservationType observationType);

    double aim(Features features);
}
