package conceptualModel;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class FilesGenerator {

	public static void main(String[] args) throws IOException {
		// java -server -Xmx300g -Xms8g -Dfile.encoding=UTF-8 -Dlog4j.configurationFile=/data2/hamdif/doctorants/ph/wardrobe_java/log4j2.xml -cp "/etudiants/deptinfo/p/pari_p1/dev/java/LOD-CM/*" conceptualModel.FilesGenerator
		System.out.println("salut les loulous !");
		System.out.println("comment ça va ?");		
		Path path = Paths.get("/etudiants/deptinfo/p/pari_p1/dev/java/LOD-CM/test.txt");
		Files.write(path, Arrays.asList("hello, les loulous !"), Charset.forName("UTF-8"));
		System.out.println("purée !");	
	}
	
//	public void orchestrator(Dataset ds) {
//		// set propertyType (P31 for wiki, rdf type pour dbp)
//		String propertyType = 
//		// get all classes
//		List<String> classes = getAllClasses(ds); //(Film, Actor...)
//		// transaction computation
//		for (String instanceType : classes) {
//			// e.g. instanceType = Film
//			Map<String, Set<String>> predicatesBySubject = getPredicatesBySubject(ds.hdtFilePath, propertyType, instanceType);
//	        TransactionsAndMapping tam = new TransactionsAndMapping(predicatesBySubject);
//	        tam.computeTransactions();  
//	        
//	        List<FP> fps = computeFPs(tam);
//	        // save fps
//	        List<MFP> mfps = computeMFPs(tam);
//	        // save mfps
//			List<CModeText> cmodelTexts = computeCModelTexts();
//	        // save cmodelTexts
//		}
//		 
//        
//		
//	}
//	
//	public List<String> getAllClasses(Dataset ds) {
//		throw new UnsupportedOperationException();
//	}
//	
//	public List<FP> computeFPs(TransactionsAndMapping tam) {
//		throw new UnsupportedOperationException();
//	}
//	
//	public List<MFP> computeMFPs(TransactionsAndMapping tam) {
//		throw new UnsupportedOperationException();
//	}
//	
//	public List<CModeText> computeCModelTexts() {
//		throw new UnsupportedOperationException();
//	}

}
