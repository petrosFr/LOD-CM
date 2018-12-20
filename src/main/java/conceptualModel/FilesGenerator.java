package conceptualModel;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class FilesGenerator {
	
	private static final Logger log = LoggerFactory.getLogger(FilesGenerator.class);
	Configuration conf;
	private static InstanceType instanceType;

	public FilesGenerator(Configuration conf) {
		this.conf = conf;
	}

	public static void main(String[] args) throws Exception {
		// java -server -Xmx300g -Xms8g -Dfile.encoding=UTF-8 -Dlog4j.configurationFile=/data2/hamdif/doctorants/ph/wardrobe_java/log4j2.xml -cp "/etudiants/deptinfo/p/pari_p1/dev/java/LOD-CM/*" conceptualModel.FilesGenerator
		// -conf C:\Users\ISSA\git\LOD-CM\conf.local.cnam.json -class http://dbpedia.org/ontology/Film -threshold 64 -datasetname DBpedia
		String fullConfFileName;

		if (args != null && args.length > 0 && Arrays.asList(args).contains("-conf")) {			
			 int index = Arrays.asList(args).indexOf("-conf");
			 fullConfFileName = args[index + 1];
		} else {
			String dirWhereJarIs = new File(FilesGenerator.class.getProtectionDomain().getCodeSource().getLocation().toURI())
					.getPath();
			fullConfFileName = Paths.get(dirWhereJarIs, "conf.json").toString().replace("target\\classes\\", "")
					.replace("target/classes/", "").replace("lod-cmOK1.jar/", "").replace("lod-cmOK1.jar\\", "");
		}
		log.info("loading configuration file: " + fullConfFileName);
		Configuration conf = Configuration.fromJson(fullConfFileName);
		
		if (args != null && args.length > 0 && Arrays.asList(args).contains("-class")) {			
			 int index = Arrays.asList(args).indexOf("-class");
			 String classUri = args[index + 1];
			 instanceType = new InstanceType(classUri);
			 index = Arrays.asList(args).indexOf("-threshold");
			 String thresholdStr = args[index + 1];
			 index = Arrays.asList(args).indexOf("-datasetname");
			 String datasetname = args[index + 1];
			 int threshold = Integer.parseInt(thresholdStr);
			 Optional<Dataset> dsOpt = conf.datasets.stream().filter(x -> x.datasetName.equalsIgnoreCase(datasetname)).findFirst();
			 if (!dsOpt.isPresent()) throw new Exception("Houston, on a un problème !");
			 processOneDataset(dsOpt.get(), instanceType, threshold, conf);
		} else {
			mainFunc(args, conf);			
		}
	}
	
	public static void mainFunc(String[] args, Configuration conf) throws URISyntaxException, IOException, NotFoundException {		
		FilesGenerator fg = new FilesGenerator(conf);
		for (Dataset ds : conf.datasets) {
			fg.orchestrator(ds);
		}
	}
	
	public void orchestrator(Dataset ds) throws IOException, NotFoundException {
		DatasetInformations di = new DatasetInformations(ds, conf);
		// get all classes
		List<InstanceType> classes = di.getAllClasses(); //(Film, Actor...)
		// transaction computation
		for (InstanceType instanceType : classes) {
			// e.g. instanceType = Film
			Map<String, Set<String>> predicatesBySubject = di.getPredicatesBySubject(instanceType.Uri);
	        TransactionsAndMapping tam = new TransactionsAndMapping(predicatesBySubject, conf, ds, instanceType);
	        tam.computeTransactions(true);  
	        
	        List<FP> fps = computeFPs(tam);
	        // save fps here
	        String fpsDirectory = conf.fpsDirectory.replace("<DATASET>", ds.datasetName).replace("<CLASSNAME>", instanceType.label);
	        for (FP fp : fps) {
	        	fp.saveFPSList(fpsDirectory);
			}
	        
	        List<MFP> mfps = computeMFPs(tam,instanceType);
	        String mfpsDirectory = conf.fpsDirectory.replace("<DATASET>", ds.datasetName).replace("<CLASSNAME>", instanceType.label);
	        for (MFP mfp : mfps) {
	        	mfp.saveFPSList(mfpsDirectory);
			}

	        
//			List<CModeText> cmodelTexts = computeCModelTexts();
//	        // save cmodelTexts here
		}		
	}
	
	


	/**
	 * Process only one given dataset. For test purpose ONLY!
	 * @param ds
	 * @param instanceTypeUri
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
    public static void processOneDataset(Dataset ds, InstanceType instanceType, int threshold, Configuration conf) throws IOException, NotFoundException {
    	DatasetInformations di = new DatasetInformations(ds, conf);
    	Map<String, Set<String>> predicatesBySubject = di.getPredicatesBySubject(instanceType.Uri);
        TransactionsAndMapping tam = new TransactionsAndMapping(predicatesBySubject, conf, ds, instanceType);
        tam.computeTransactions(true);
        FP fp = new FP(threshold);
        fp.computeFP(tam);
        
        MFP mfp = new MFP(threshold, instanceType, conf);
        mfp.computeMFPs(tam);
    }
	
	/**
	 * Retrieve all classes available in the given dataset
	 * @param ds
	 * @return
	 */
	public List<String> getAllClasses(Dataset ds) {
		throw new UnsupportedOperationException();
	}
	
	public List<FP> computeFPs(TransactionsAndMapping tam) {
		List<FP> fps = new ArrayList<>();
		IntStream.range(1, 100).forEach(
		        n -> {
		            FP fp = new FP(n);
		            try {
						fp.computeFP(tam);
					} catch (IOException e) {
						log.error(e.toString());
						System.exit(1);
					} // n = threshold
		            fps.add(fp);
		        }
		    );
		return fps;
	}
	
	public List<MFP> computeMFPs(TransactionsAndMapping tam, InstanceType instanceType) {
		List<MFP> mfps = new ArrayList<>();
		IntStream.range(1, 100).forEach(
		        n -> {
		            MFP mfp = new MFP(n, instanceType, conf);
		            try {
						mfp.computeMFPs(tam);
					} catch (IOException e) {
						log.error(e.toString());
						System.exit(1);
					} // n = threshold
		            mfps.add(mfp);
		        }
		    );
		return mfps;
	}
	
//	public List<CModeText> computeCModelTexts() {
//		throw new UnsupportedOperationException();
//	}

}
