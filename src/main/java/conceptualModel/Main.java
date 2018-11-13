package conceptualModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;
import net.sourceforge.plantuml.GeneratedImage;
import net.sourceforge.plantuml.SourceFileReader;

public class Main {

	static final String wikidataStr = "Wikidata";
	static final String dbpediaStr = "DBpedia";

	private static final Logger log = LoggerFactory.getLogger(Main.class);

	static HashMap<String, Double> propertyMinsup = new HashMap<String, Double>();

	public static HashMap<Integer, String> HashmapItem = new HashMap<Integer, String>();
	public static HashMap<List<String>, String> MFPElementSup = new HashMap<List<String>, String>();

	public static void main(String[] args) throws IOException, NotFoundException, URISyntaxException {

		// encoding/spmf_library=UTF-8
		log.info("starting...");
		checkArguments(args);
		String dirWhereJarIs = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI())
				.getPath();
		// FIXME: automatize recuperation of jar name, do not hard code it in following
		// string.
		String fullConfFileName = Paths.get(dirWhereJarIs, "conf.json").toString().replace("target\\classes\\", "")
				.replace("target/classes/", "").replace("lod-cmOK.jar/", "").replace("lod-cmOK.jar\\", "");
		log.info("loading configuration file: " + fullConfFileName);
		Configuration conf = Configuration.fromJson(fullConfFileName);
		String classname = args[0];
		String threshold = args[1];
		String datasetName = args.length > 2 ? args[2] : "dbpedia";

		String rootPath = conf.rootPath; // FIXME: document this in Configuration class
		String set = conf.set; // FIXME: document this in Configuration class
		String folderPath = rootPath + set + classname;

		log.debug("classname: " + classname);
		log.debug("threshold: " + threshold);
		log.debug("datasetName: " + datasetName);
		log.debug("rootPath: " + rootPath);
		log.debug("set: " + set);
		log.debug("datasets size: " + conf.datasets.size());
		for (Dataset ds : conf.datasets) {
			log.debug("--------");
			log.debug("datasetName: " + ds.datasetName);
			log.debug("hdtFilePath: " + ds.hdtFilePath);
			log.debug("classNamespace: " + ds.classNamespace);
			log.debug("--------");
		}
		Optional<Dataset> optionalDataset = conf.datasets.stream()
				.filter(x -> x.datasetName.equalsIgnoreCase(datasetName)).findFirst();
		if (!optionalDataset.isPresent()) {
			log.error("No corresponding dataset found: " + datasetName);
			System.exit(1);
		}
		Dataset ds = optionalDataset.get();

		String hdtPath = ds.hdtFilePath;

		// String dbpediaHDTPath = "/srv/www/htdocs/demo_conception/dataset.hdt";
		String instanceType = "http://dbpedia.org/ontology/" + classname; // classname is from DBpedia
		log.info("instanceType: " + instanceType);
		log.info("Selected datasetName: " + ds.datasetName);
		// String wikidataHDTPath =
		// "/data2/hamdif/doctorants/ph/linkeddatasets/hdt/wikidata/wikidata2018_09_11.hdt";
		// String hdtPath = dbpediaHDTPath;
		if (ds.datasetName.equalsIgnoreCase(wikidataStr)) {
			log.debug("using wikidata... getting corresponding class of: " + classname);
			// We have to search for Wikidata equivalent class since
			// the interface present only DBpedia classes.
			log.debug("equivalent_classes exists ? " + Helpers.isFileExists("./equivalent_classes"));
			List<String> lines = Files.readAllLines(Paths.get("./", "equivalent_classes"));
			log.debug("# of equivalent classes: " + lines.size());
			final String tmpString = instanceType;
			Optional<String> newInstanceType = lines.stream().filter(x -> x.startsWith(tmpString + "\t")).findFirst();
			if (!newInstanceType.isPresent()) {
				log.error("no wikidata equivelent class to " + instanceType + " has been found!");
				System.exit(0);
			}
			instanceType = newInstanceType.get();
			log.info("new instance type name: " + instanceType);
			// // TODO: we can make it quicker by creatling a file
			// // containing all pair of classes such as each line is:
			// // DBpedia_Class Wikiadata_Equivalent_Class
			// Dataset dbpedia = conf.datasets.stream().filter(x ->
			// x.datasetName.equalsIgnoreCase(dbpediaStr)).findFirst()
			// .get();
			// log.debug("dbpedia found ? " + dbpedia);
			// try (HDT hdt = HDTManager.loadHDT(dbpedia.hdtFilePath, null)) {
			// IteratorTripleString it = hdt.search(instanceType,
			// "http://www.w3.org/2002/07/owl#equivalentClass", "");
			// while (it.hasNext()) {
			// TripleString ts = it.next();
			// String wikidataInstanceType = ts.getObject().toString();
			// instanceType = wikidataInstanceType;
			// log.info("new instance type name: " + instanceType);
			// }
			// }
		}

		log.info("main computation...");
		try (HDT hdt = HDTManager.loadHDT(hdtPath, null)) {
			log.debug("HDT file loaded");
			String ItemHashmap = "";
			String TransactionSP = "";
			int numTrans = 0;

			List<String> subjects = new ArrayList<>();
			List<String> transactions = new ArrayList<>();
			List<String> itemHashMap = new ArrayList<>();
			HashMap<String, String> finalMap1 = new HashMap<String, String>();
			int item = 0;

			// String instanceType = classname;

			File tmpFolder = new File(folderPath);

			if (!tmpFolder.exists()) {
				log.info("creating folder: " + tmpFolder.toString());
				boolean creationResult = tmpFolder.mkdirs();
				log.info("folder created: " + creationResult);
			}
			String propertyType = ds.datasetName.equalsIgnoreCase(wikidataStr) ? "http://www.wikidata.org/prop/P31"
					: "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
			log.debug("propertyType: " + propertyType);
			IteratorTripleString it = hdt.search("", propertyType, instanceType);
			log.debug("query: " + "(? <" + propertyType + "> <" + instanceType + ">)");
			Set<String> subjectsTmp = new HashSet<>();
			while (it.hasNext()) {
				TripleString ts = it.next();
				String s = ts.getSubject().toString();
				subjectsTmp.add(s);
			}
			log.debug("subjectsTmp size: " + subjectsTmp.size());

			final Map<String, Set<String>> predicatesBySubject = new ConcurrentHashMap<>();
			log.debug("Parallel loop starting...");
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

			log.debug("predicatesBySubject size: " + predicatesBySubject.size());
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

			log.debug("Writing files...");
			ItemHashmap = folderPath + "/itemHashmap.txt";
			Path fileItemHashmap = Paths.get(folderPath + "/itemHashmap.txt");
			Files.write(fileItemHashmap, itemHashMap, Charset.forName("UTF-8"));

			Path fileSubjects = Paths.get(folderPath + "/subjects.txt");
			Files.write(fileSubjects, subjects, Charset.forName("UTF-8"));

			TransactionSP = folderPath + "/transactions.txt";
			Path fileTransactionSP = Paths.get(folderPath + "/transactions.txt");
			Files.write(fileTransactionSP, transactions, Charset.forName("UTF-8"));

			log.debug("FPGrowth starting...");
			// FPGrowth...
			// Load a sequence database
			String input = folderPath + "/transactions.txt";
			String output = folderPath + "/fpgrowth_" + threshold + ".txt";

			// Create an instance of the algorithm
			AlgoFPGrowth algo = new AlgoFPGrowth();

			// execute the algorithm
			double ms = Integer.parseInt(threshold) / 100.0;
			log.info("minsup: " + ms);
			// log.debug("input: " + input);
			Itemsets itemsets = null;
			try {		
				log.debug("input exist ? " + Helpers.isFileExists(input));		
				itemsets = algo.runAlgorithm(input, output, ms);
				log.debug("output exist ? " + Helpers.isFileExists(output));	
			} catch (Exception e) {
				log.error("Error in AlgoFPGrowth runAlgorithm", e);
			}
			log.debug("itemsets count: " + (itemsets != null ? itemsets.getItemsetsCount() : 0));

			ItemHashmap = folderPath + "/itemHashmap.txt";
			TransactionSP = folderPath + "/transactions.txt";
			conceptualModel conceptual = new conceptualModel(hdt);

			log.debug("Finishing main computation...");

			conceptual.setPathFile(TransactionSP, output, ItemHashmap);
			conceptual.CreateTxtFile(classname, threshold, numTrans);

			File source = new File(
					"/srv/www/htdocs/demo_conception/pictures_uml/CModel_" + classname + "_" + threshold + ".txt");			
			log.debug("source exist? " + Helpers.isFileExists("/srv/www/htdocs/demo_conception/pictures_uml/CModel_" + classname + "_" + threshold + ".txt"));
			SourceFileReader readeruml = new SourceFileReader(source);
			List<GeneratedImage> list = readeruml.getGeneratedImages();
			log.debug("# of generated images: " + list.size());
		} catch (Exception e) {
			log.error("error during main computation: ", e);
		}
		log.info("final step");
		try {
			String[] cmdScript;
			if (Integer.parseInt(threshold) > 80)
				cmdScript = new String[] { "/bin/bash",
						"/etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub26/scriptApriori.sh", classname,
						threshold };
			else
				cmdScript = new String[] { "/bin/bash",
						"/etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub26/scriptFPgrowth.sh", classname,
						threshold };
			Process procScript = Runtime.getRuntime().exec(cmdScript);
		} catch (Exception e) {
			log.error("error during final step: ", e);
		}

		// write labels of MFP file
		String line = "";
		readHashmap(folderPath + "/itemHashmap.txt");
		String value = "";
		ArrayList<String> element = new ArrayList<>();
		BufferedReader MFPBuff = new BufferedReader(
				new FileReader(rootPath + set + classname + "/schemas/schema_minsup" + threshold + ".txt"));
		while ((line = MFPBuff.readLine()) != null) {
			String l1 = line.substring(0, line.indexOf("("));
			String[] properties = l1.split(" ");
			for (int i = 0; i < properties.length; i++) {
				int x = Integer.parseInt(properties[i]);
				if (HashmapItem.containsKey(x)) {
					value = HashmapItem.get(x);
					value = value.substring(value.lastIndexOf("/") + 1);
					if (value.contains("#"))
						value = value.substring(value.indexOf("#") + 1);
					element.add(value);
				}

			}
			String l2 = line.substring(line.indexOf("(") + 1, line.indexOf(")"));
			MFPElementSup.put(element, l2);
		}
		MFPBuff.close();
		BufferedWriter writer = new BufferedWriter(
				new FileWriter(rootPath + set + classname + "/schemas/schema_minsupElements" + threshold + ".txt"));

		// write MFP elements (labels)
		Set set1 = MFPElementSup.entrySet();
		String group = "";
		Iterator iterator = set1.iterator();
		while (iterator.hasNext()) {
			Map.Entry mentry = (Map.Entry) iterator.next();
			ArrayList<String> tt = new ArrayList<String>();
			ArrayList<String> elemnts = (ArrayList<String>) mentry.getKey();
			for (String e : elemnts) {
				group = group + " " + e;
			}
			writer.write(group + " (" + mentry.getValue() + ")");
		}
		writer.close();
		log.info("end of program.");
	}

	/**
	 * Function checkings if arguments are correct. If not, provide a man page.
	 * 
	 * @param args
	 */
	static void checkArguments(String[] args) {
		// TODO: provide a man page if arguments are not correct.
		if (args.length < 3) {
			log.error("There must be three arguments: class name, then threshold, then dataset name.");
			System.exit(1);
		}
	}

	/**
	 * Function that save a Jena model into a file.
	 * 
	 * @param model  The model to be saved
	 * @param file   The full path of the file
	 * @param format The format of the serialization
	 */
	public static void saveModel(Model model, String file, RDFFormat format) {
		log.debug("enterring saveModel");
		try (OutputStream out = new FileOutputStream(file)) {
			RDFDataMgr.write(out, model, format);
		} catch (IOException e) {
			log.error("Error during saveModel: ", e);
		}
	}

	/**
	 * Function that read a hashMap file.
	 * 
	 * @param path path of hashMap file
	 * 
	 */
	public static void readHashmap(String path) {
		File file = new File(path);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));

			String line = "";
			int p;
			String s;
			while ((line = reader.readLine()) != null) {
				p = Integer.parseInt(line.substring(0, line.indexOf(" =>")));
				s = line.substring(line.indexOf("=>") + 3);
				HashmapItem.put(p, s);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
