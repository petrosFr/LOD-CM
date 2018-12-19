package conceptualModel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.jena.base.Sys;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.pfv.spmf.algorithms.frequentpatterns.apriori.AlgoApriori;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemset;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

public class DatasetInformations {
    private static final Logger log = LoggerFactory.getLogger(DatasetInformations.class);
    private Dataset ds;
    public DatasetInformations(Dataset ds) {
		this.ds = ds;
	}

	public DatasetInformations() {
		// TODO Auto-generated constructor stub
	}

	/**
     * Compute the number of properties above the given threshold for the given
     * class.
     * 
     * @param hdtPath      Full path to the HDT file to look in
     * @param instanceType Type (or class) of observed instances
     * @param threshold    Number between 0 and 1 corresponding to the minimum
     *                     completeness required
     * @param propertyType Usualy rdf:type but might be different (for Wikipadia for
     *                     example)
     * @return The number of predicates above the threshold for the given class.
     * @throws IOException
     * @throws NotFoundException
     */
    public int getNumberOfPropertiesAboveThreshold(String hdtPath, String instanceType, double threshold, String propertyType, String instanceTypeFragment)
            throws IOException, NotFoundException {
        if (threshold > 1d) {
            log.error("Threshold must be between 0 and 1! You provided: " + threshold);
            System.exit(1);
        }
        if (!Helpers.isFileExists(hdtPath)) {
            log.error("File not found: " + hdtPath);
            System.exit(1);
        }
        
        Path transactionsFilePath = Paths.get("transactions_" + instanceTypeFragment + ".txt");
        if (!Helpers.isFileExists("transactions_" + instanceTypeFragment + ".txt")) {
            Map<String, Set<String>> predicatesBySubject = getPredicatesBySubject(hdtPath, propertyType, instanceType);
            TransactionsAndMapping tam = new TransactionsAndMapping(predicatesBySubject);
            tam.computeTransactions();           
            log.debug("Saving transactions in " + transactionsFilePath.toString() + "...");
            Files.write(transactionsFilePath, tam.getTransactionsToStringList(), Charset.forName("UTF-8"));
        }

        List<List<Itemset>> levels = getItemsetsByLevel(transactionsFilePath.toString(), threshold);

        // get the list of all itemset containing only one element
        // (in this case only one property)
        List<Itemset> levelOne = levels.get(1);
        int numberOfPropertiesAboveThreshold = levelOne.size();
        // for (Itemset itemset : levelOne) {
        //     int[] predicates = itemset.getItems(); // there should be only one predicate in it
        // }
        return numberOfPropertiesAboveThreshold;
    }

    public List<List<Itemset>> getItemsetsByLevel(String transactionsFilePath, double threshold)
            throws FileNotFoundException, IOException {
        // log.debug("FPGrowth...");
        // AlgoFPGrowth algo = new AlgoFPGrowth();
        // Itemsets itemsets = algo.runAlgorithm(transactionsFilePath.toString(), null, threshold);
        log.debug("Apriori...");
        AlgoApriori algo = new AlgoApriori();
        Itemsets itemsets = algo.runAlgorithm(threshold, transactionsFilePath.toString(), null);
        log.debug("# of itemsets: " + itemsets.getItemsetsCount());
        return itemsets.getLevels();
    }

    /**
     * Get the predicates set of each subject of a given type
     * 
     * @param hdtPath      Full path to the HDT file to look in
     * @param instanceType Type (or class) of observed instances
     * @param propertyType Usualy rdf:type but might be different (for Wikipadia for
     *                     example)
     * @return The key is the subject and the set contains all predicates of this
     *         subject
     * @throws IOException
     * @throws NotFoundException
     */
    public Map<String, Set<String>> getPredicatesBySubject(String hdtPath, String propertyType, String instanceType)
            throws IOException, NotFoundException {
        final Map<String, Set<String>> predicatesBySubject = new ConcurrentHashMap<>();
        log.debug("HDT file loading...");
        try (HDT hdt = HDTManager.loadHDT(hdtPath, null)) {
            log.debug("HDT file loaded");
        // try (HDT hdt = HDTManager.mapIndexedHDT(hdtPath, null)) {
            log.debug("Subjects retrieving...");
            Set<String> subjects = new HashSet<>();
            IteratorTripleString it = hdt.search("", propertyType, instanceType);
            log.debug("query: " + "(? <" + propertyType + "> <" + instanceType + ">)");
            while (it.hasNext()) {
                TripleString ts = it.next();
                String s = ts.getSubject().toString();
                subjects.add(s);
            }
            log.debug("# of subjects: " + subjects.size());
            log.debug("Predicates by subject retrieving...");
            subjects.parallelStream().forEach((subject) -> {
                IteratorTripleString iter = null;
                try {
                    iter = hdt.search(subject, "", "");
                } catch (NotFoundException e) {
                    log.error("subject " + subject + " not found!");
                }
                Set<String> predicates = new HashSet<>();
                while (iter.hasNext()) {
                    TripleString ts = iter.next();
                    String p = ts.getPredicate().toString();
                    predicates.add(p);
                }
                predicatesBySubject.put(subject, predicates);
            });
            log.debug("# of predicate by subject: " + subjects.size());
        }
        return predicatesBySubject;
    }

    public static void main(String[] args) throws IOException, NotFoundException {
        log.debug("start!");
        // java -server -Xmx300g -Xms8g -Dfile.encoding=UTF-8 -cp "/etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/lod-cmOK.jar" conceptualModel.DatasetInformations "/srv/www/htdocs/demo_conception/dataset.hdt" "/data2/hamdif/doctorants/ph/linkeddatasets/hdt/wikidata/wikidata-20170313-all-BETA.hdt" 
        // java -server -Xmx300g -Xms8g -Dfile.encoding=UTF-8 -cp "/data2/hamdif/doctorants/ph/lod_cm/*" conceptualModel.DatasetInformations "/srv/www/htdocs/demo_conception/dataset.hdt" "/data2/hamdif/doctorants/ph/linkeddatasets/hdt/wikidata/wikidata-20170313-all-BETA.hdt"
        // test();
        if (args.length != 2) {
            System.out.println("You must provide the path to dbpedia then to wikidata HDT files.");
            log.info("You must provide the path to dbpedia then to wikidata HDT files.");
        }
        List<Double> thresholds = Arrays.asList(0.1d, 0.3d, 0.5d, 0.7d, 0.9d);
        // Map<String, String> dbpediaClasses = 
        //     Map.ofEntries(
        //         Map.entry("http://dbpedia.org/ontology/Scientist", "Scientist"),
        //         Map.entry("http://dbpedia.org/ontology/Film", "Film"),
        //         Map.entry("http://dbpedia.org/ontology/Settlement", "Settlement"),
        //         Map.entry("http://dbpedia.org/ontology/Organisation", "Organisation")
        //     );
        Map<String, String> wikidataClasses = 
            Map.ofEntries(
                Map.entry("http://www.wikidata.org/entity/Q901", "scientist"),
                Map.entry("http://www.wikidata.org/entity/Q11424", "film"),
                Map.entry("http://www.wikidata.org/entity/Q486972", "human settlement"),
                Map.entry("http://www.wikidata.org/entity/Q43229", "organization")
            );
        String propertyTypeWikidata = "http://www.wikidata.org/prop/direct/P31";
        // String propertyTypeDBpedia = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        
        String columns = "Class & " + String.join(" & ", thresholds.stream().map(x -> x.toString())
            .collect(Collectors.toList())).trim();
        String tabular = "|l|";
        for (double var : thresholds) {
            tabular += "l|";
        }

        try {
            // String dbpediaPath = args.length > 1 ? args[0] : "dataset.hdt";
            // log.debug("DBpedia path: " + dbpediaPath);
            // List<String> dbpediaLatexArray = generateLatexArray(dbpediaClasses, thresholds, dbpediaPath, propertyTypeDBpedia, "DBpedia number of predicates by classes and thresholds", "tab:dbpPredicatesThresholds", columns, tabular);
            // Path dbpediaResultPath = Paths.get("dbp_results.txt");
            // Files.write(dbpediaResultPath, dbpediaLatexArray, Charset.forName("UTF-8"));

            String wikidataPath = args[1];
            log.debug("Wikidata path: " + wikidataPath);
            List<String> wikidataLatexArray = generateLatexArray(wikidataClasses, thresholds, wikidataPath, propertyTypeWikidata, "Wikidata number of predicates by classes and thresholds", "tab:wikiPredicatesThresholds", columns, tabular);                
            Path wikidataResultPath = Paths.get("wikidata_results.txt");
            Files.write(wikidataResultPath, wikidataLatexArray, Charset.forName("UTF-8"));
        } catch (Exception e) {
            log.error("Error in main: ", e);
        }
        
        log.debug("end!");
    }

    private static List<String> generateLatexArray(Map<String, String> classes, List<Double> thresholds, String hdtPath, String propertyType, String caption, String label, String columns, String tabular) throws IOException, NotFoundException {
        /*
        \begin{table}
        \centering
        \caption{Transactions created from triples}\label{tab:filmtrans}
        \begin{tabular}{|L{2.2cm}|L{5cm}|}
          \hline
          \rowcolor{Gray} 
            Instance & Transaction\\
            \hline 
            The\_Godfather & director, musicComposer\\
            Goodfellas & director, editing\\
            True\_Lies & director, editing, musicComposer\\ 
           \hline
        \end{tabular}
        \end{table}
        */
        List<String> lines = new ArrayList<>(Arrays.asList(
            "\\begin{table}",
            "\\centering",
            "\\caption{" + caption + "}\\label{" + label + "}",
            "\\begin{tabular}{" + tabular + "}",
            "  \\hline",
            "  \\rowcolor{Gray}",
            "    " + columns + "\\\\",
            "    \\hline"
        ));
        for (Entry<String, String> classEntry : classes.entrySet()) {
            String line = classEntry.getValue() + " & ";
            log.debug("class: " + classEntry.getValue() + " // " + classEntry.getKey());
            for (Double threshold : thresholds) {
                log.debug("threshold: " + threshold);
                DatasetInformations di = new DatasetInformations();
                int numberOfPredicates = di.getNumberOfPropertiesAboveThreshold(hdtPath, classEntry.getKey(), threshold, propertyType, classEntry.getValue());
                line += numberOfPredicates + " & ";
            }
            line = line.trim();
            int index = line.lastIndexOf("&");
            line = line.substring(0, index);
            line += "\\\\";
            lines.add(line);
            lines.add("   \\hline");
        }
        lines.add("\\end{tabular}");
        lines.add("\\end{table}");
        return lines;
    }

    /**
     * Used only for testing purpose
     * @throws IOException
     * @throws NotFoundException
     */
    private static void test() throws IOException, NotFoundException {
        String propertyTypeDBpedia = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        List<Double> thresholds = Arrays.asList(0.8d, 0.9d);
        String dbpediaPath = "dataset.hdt";
        Map<String, String> dbpediaClasses = 
            Map.ofEntries(
                Map.entry("http://dbpedia.org/ontology/Actor", "Actor")
            );
        String columns = "Class & " + String.join(" & ", thresholds.stream().map(x -> x.toString())
            .collect(Collectors.toList())).trim();
        String tabular = "|l|";
        for (double var : thresholds) {
            tabular += "l|";
        }
        try {            
            List<String> result = generateLatexArray(dbpediaClasses, thresholds, dbpediaPath, propertyTypeDBpedia, "DBpedia number of predicates by classes and thresholds", "tab:dbpPredicatesThresholds", columns, tabular);
            // DatasetInformations di = new DatasetInformations();
            // int numberOfPredicates = di.getNumberOfPropertiesAboveThreshold(hdtPath, instanceType, threshold, propertyTypeDBpedia, "Actor");
            log.info("result: " + String.join("\n", result));
        } catch (Exception e) {
            log.error("error: ", e);
        }
    }

	public List<InstanceType> getAllClasses() {
		throw new UnsupportedOperationException();
	}

	public Map<String, Set<String>> getPredicatesBySubject(String instanceType) throws IOException, NotFoundException {
		return getPredicatesBySubject(ds.hdtFilePath, ds.propertyType, instanceType);
	}
}