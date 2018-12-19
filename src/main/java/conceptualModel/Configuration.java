package conceptualModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.google.gson.Gson;

/**
 * Allow to configure the program from a text file in a json format.
 */
public class Configuration {
    
    public String rootPath;
    public String set;
    /**
     * List of available datasets.
     */
    public List<Dataset> datasets;
    public String fpsDirectory;

    /**
     * Fill the configuration class with values from specified json file.
     * @param configurationFilePath
     * @return
     * @throws IOException
     */
    public static Configuration fromJson(String configurationFilePath) throws IOException {
        Gson gson = new Gson();
        Configuration conf = new Configuration();
        List<String> lines = Files.readAllLines(Paths.get(configurationFilePath));
        String json = String.join("", lines);
        conf = gson.fromJson(json, conf.getClass());
        return conf;		
    }
}