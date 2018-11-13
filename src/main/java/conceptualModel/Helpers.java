package conceptualModel;

import java.io.File;

public class Helpers {
    /**
     * Helper to check if a string correspond to an existing file.
     * @param filePathString
     * @return
     */
    public static boolean isFileExists(String filePathString) {
        File f = new File(filePathString);
        return f.exists() && !f.isDirectory();
    }
}