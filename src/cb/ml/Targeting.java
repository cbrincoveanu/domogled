package cb.ml;

public interface Targeting {
    void addPoint(Features features, double guessFactor);

    double aim(Features features);
}
