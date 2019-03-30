package cb.util;

import cb.ml.Observation;
import robocode.RobocodeFileWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DataStorage {
    public static void writeCSV(File file, ArrayList<Observation> data) throws IOException {
        RobocodeFileWriter rfw = new RobocodeFileWriter(file);
        System.out.println(file.getName());
        for (int i = 0; i < data.size(); i++) {
            Observation observation = data.get(i);
            if (i == 0) {
                rfw.write(observation.getFeatures().keysToString() + "type,guessFactor\n");
            }
            rfw.write(observation.toString() + "\n");
        }
        rfw.close();
    }
}
