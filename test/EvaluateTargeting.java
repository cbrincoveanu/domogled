import cb.ml.*;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EvaluateTargeting {
    private static final String targetingFolder = "data/targeting/";
    @Test
    public void evaluate() {
        try {
            List<Targeting> targetings = new ArrayList<>();
            targetings.add(new DummyTargeting());
            targetings.add(new BaselineTargeting());
            for (Targeting targeting : targetings) {
                System.out.println(targeting);
                File folder = new File(targetingFolder);
                File[] listOfFiles = folder.listFiles();
                for (File file : listOfFiles) {
                    if (file.isFile()) {
                        List<Item> data = getData(targetingFolder + file.getName());
                        int count = 0;
                        double score = 0;
                        for (Item item : data) {
                            double gf = targeting.aim(item.features);
                            double diff = (gf-item.guessFactor)*10; //TODO make dynamic, maybe even exact with x and y
                            score += 1.0 / (1 + diff*diff);
                            count++;
                            targeting.addPoint(item.features, item.guessFactor, item.observationType);
                        }
                        System.out.println("\t" + file.getName() + "\t" + score / count);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Item> getData(String fileStr) throws IOException {
        List<Item> items = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(fileStr));
        String[] headers = br.readLine().split(",");
        String line;
        while ((line = br.readLine()) != null) {
            String[] values = line.split(",");
            Item item = new Item();
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].equals("type")) {
                    item.observationType = ObservationType.valueOf(values[i]);
                } else if (headers[i].equals("guessFactor")) {
                    item.guessFactor = Double.parseDouble(values[i]);
                } else {
                    item.features.setFeature(headers[i], Double.parseDouble(values[i]));
                }
            }
            items.add(item);
        }
        return items;
    }

    private class Item {
        Features features = new Features();
        double guessFactor;
        ObservationType observationType;
    }
}
