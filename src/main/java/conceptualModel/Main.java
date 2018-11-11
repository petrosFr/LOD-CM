package conceptualModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import net.sourceforge.plantuml.GeneratedImage;
import net.sourceforge.plantuml.SourceFileReader;

public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);

	static HashMap<String, Double> propertyMinsup = new HashMap<String, Double>();

	
	/// link
	public static void main(String[] args) throws IOException, NotFoundException {
		
		checkArguments(args);
		Configuration conf = Configuration.fromJson("conf.json");
		String classname = args[0];
		String threshold = args[1];
		String datasetName = args.length > 2 ? args[2] : null;
		// String classname = "Film";
		// String threshold = "90";

		String rootPath = "/etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/itemsets/";
		String set = "dbpedia/2016-10/";
		
		String dbpediaHDTPath = "/srv/www/htdocs/demo_conception/dataset.hdt";
		String instanceType = "http://dbpedia.org/ontology/" + classname;
		String wikidataHDTPath = "/data2/hamdif/doctorants/ph/linkeddatasets/hdt/wikidata/wikidata2018_09_11.hdt";
		String hdtPath = dbpediaHDTPath;
		if (datasetName != null) {
			log.info("dataset name:" + datasetName);
			if (datasetName == "Wikidata") {
				// We have to search for Wikidata equivalent class since
				// the interface present only DBpedia classes.
				// TODO: we can make it quicker by creatling a file
				// containing all pair of classes such as each line is:
				// DBpedia_Class Wikiadata_Equivalent_Class
				try (HDT hdt = HDTManager.loadHDT(hdtPath, null)) {
					IteratorTripleString it = hdt.search(instanceType, "http://www.w3.org/2002/07/owl#equivalentClass", "");
					while (it.hasNext()) {
						TripleString ts = it.next();
						String wikidataInstanceType = ts.getObject().toString();
						instanceType = wikidataInstanceType;
						log.info("new instance type name:" + instanceType);
					}
				}	
				hdtPath = wikidataHDTPath;			 
			}
		}

		try (HDT hdt = HDTManager.loadHDT(hdtPath, null)) {

			String ItemHashmap = "";
			String TransactionSP = "";
			int numTrans = 0;

			List<String> subjects = new ArrayList<>();
			List<String> transactions = new ArrayList<>();
			List<String> itemHashMap = new ArrayList<>();
			HashMap<String, String> finalMap1 = new HashMap<String, String>();
			int item = 0;

			// String instanceType = classname;
			String folderPath = rootPath + set + classname;

			File tmpFolder = new File(folderPath);

			if (!tmpFolder.exists())
				tmpFolder.mkdirs();

			IteratorTripleString it = hdt.search("", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
					"");
			Set<String> subjectsTmp = new HashSet<>();
			while (it.hasNext()) {
				TripleString ts = it.next();
				String s = ts.getSubject().toString();
				subjectsTmp.add(s);
			}

			final Map<String, Set<String>> predicatesBySubject = new ConcurrentHashMap<>();
			subjectsTmp.parallelStream().forEach((subject) -> {
				IteratorTripleString iter = null;
				try {
					iter = hdt.search(subject, "", "");
				} catch (NotFoundException e) {
					e.printStackTrace();
				}
				Set<String> setTmp = new HashSet<>();
				while (iter.hasNext()) {
					TripleString ts = iter.next();
					String p = ts.getPredicate().toString();
					setTmp.add(p);
				}
				predicatesBySubject.put(subject, setTmp);
			});

			for (Entry<String, Set<String>> entry : predicatesBySubject.entrySet()) {
				String transaction = "";

				String subject = entry.getKey().toString();
				Set<String> predicates = entry.getValue();
				for (String predicate : predicates) {
					if (finalMap1.containsKey(predicate))
						transaction = transaction + finalMap1.get(predicate) + " ";
					else {
						item++;
						finalMap1.put(predicate, Integer.toString(item));
						transaction = transaction + finalMap1.get(predicate) + " ";
						itemHashMap.add(item + " => " + predicate);
					}
				}
				subjects.add(subject);
				transactions.add(transaction);
				numTrans++;
			}

			ItemHashmap = folderPath + "/itemHashmap.txt";
			Path fileItemHashmap = Paths.get(folderPath + "/itemHashmap.txt");
			Files.write(fileItemHashmap, itemHashMap, Charset.forName("UTF-8"));

			Path fileSubjects = Paths.get(folderPath + "/subjects.txt");
			Files.write(fileSubjects, subjects, Charset.forName("UTF-8"));

			TransactionSP = folderPath + "/transactions.txt";
			Path fileTransactionSP = Paths.get(folderPath + "/transactions.txt");
			Files.write(fileTransactionSP, transactions, Charset.forName("UTF-8"));

			// FPGrowth...
			// Load a sequence database
			String input = folderPath + "/transactions.txt";
			String output = folderPath + "/fpgrowth_" + threshold + ".txt";

			// Create an instance of the algorithm
			AlgoFPGrowth algo = new AlgoFPGrowth();

			// execute the algorithm
			double ms = Integer.parseInt(threshold) / 100.0;
			algo.runAlgorithm(input, output, ms);

			ItemHashmap = folderPath + "/itemHashmap.txt";
			TransactionSP = folderPath + "/transactions.txt";
			conceptualModel conceptual = new conceptualModel(hdt);

			conceptual.setPathFile(TransactionSP, output, ItemHashmap);
			conceptual.CreateTxtFile(classname, threshold, numTrans);

			File source = new File(
					"/srv/www/htdocs/demo_conception/pictures_uml/CModel_" + classname + "_" + threshold + ".txt");
			SourceFileReader readeruml = new SourceFileReader(source);
			List<GeneratedImage> list = readeruml.getGeneratedImages();
		}
		String[] cmdScript;
		if (Integer.parseInt(threshold) > 80)
			cmdScript = new String[] { "/bin/bash",
					"/etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/scriptApriori.sh", classname,
					threshold };
		else
			cmdScript = new String[] { "/bin/bash",
					"/etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub16/scriptFPgrowth.sh", classname,
					threshold };
		Process procScript = Runtime.getRuntime().exec(cmdScript);
	}

	/**
	 * Function checkings if arguments are correct. If not, provide a man page.
	 * @param args
	 */
	static void checkArguments(String[] args) {
		// TODO: provide a man page if arguments are not correct.
		if (args.length < 2) {
			log.error("There must be at least two arguments.");
			System.exit(0);
		}
	}

	/**
	 * Function that save a Jena model into a file.
	 * @param model The model to be saved
	 * @param file The full path of the file
	 * @param format The format of the serialization
	 */
	@Deprecated
	public static void saveModel(Model model, String file, RDFFormat format) {
		log.debug("enterring saveModel");
		try (OutputStream out = new FileOutputStream(file)) {
			RDFDataMgr.write(out, model, format);
		} catch (IOException e) {
			log.error("Error during saveModel:", e);
		}
	}
}
