package cb.ml;

public interface Movement {
    void addPoint(Features features, double guessFactor, ObservationType observationType);

    double getProbability(Features features, double minGuessFactor, double maxGuessFactor);
}
