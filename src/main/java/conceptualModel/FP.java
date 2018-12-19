package conceptualModel;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;

/**
 * Frequent Pattern for a given class (through its transactions) and a given threshold
 * @author ISSA
 *
 */
public class FP {
	
	/*
	 * 
	 * 	1 #SUP: 110652
		1 5 #SUP: 110448
		1 5 9 #SUP: 110448
		1 9 #SUP: 110652
		5 #SUP: 111695
		5 9 #SUP: 111695
		9 #SUP: 111938

	 */
	private static final Logger log = LoggerFactory.getLogger(FP.class);
	public List<FPElement> FPElements;
	public Integer threshold;
	
	public FP(int threshold) {
		this.threshold = threshold;
		this.FPElements = new ArrayList<>(); 
	}

	public void computeFP(TransactionsAndMapping tam) throws IOException {
		// TODO Auto-generated method stub
		AlgoFPGrowth algo = new AlgoFPGrowth();

		// execute the algorithm
		double ms = threshold / 100.0;
		log.info("minsup: " + ms);
		// log.debug("input: " + input);
		String folderPath = null;
		String input = folderPath + "/transactions.txt";
		Path inputPath = Paths.get(folderPath, folderPath);
		if (!Files.exists(inputPath)) {
			Files.write(inputPath, tam.getTransactionsToStringList(), Charset.forName("UTF-8"));
		}
		String output = folderPath + "/fpgrowth_" + threshold + ".txt";
		Itemsets itemsets = algo.runAlgorithm(input, output, ms);
		System.out.println("sdfsdf");
		
	}
	
	

	public void saveFPSList(String fpsDirectory) throws IOException {
		Path path = Paths.get(fpsDirectory, this.threshold.toString());
		Files.write(path, this.FPElements.stream().map(x -> x.toString()).collect(Collectors.toList()), Charset.forName("UTF-8"));	
	}
	
	/**
	 * A line in a Frequent Pattern file
	 * @author ISSA
	 *
	 */
	public class FPElement {
		// 1 5 9 #SUP: 110448
		public List<String> properties;
		public int frequency;
		
		@Override
		public String toString() {
			return String.join(" ", properties) + " #SUP: " + frequency;
		}
	}
}
