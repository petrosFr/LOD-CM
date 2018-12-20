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




public class MFP {
	
	
	private static final Logger log = LoggerFactory.getLogger(FP.class);
	public List<MFPElement> MFPElements;
	public Double threshold;
	public InstanceType instanceType;
	public Configuration conf;
	
	public MFP(double threshold, InstanceType instanceType, Configuration conf) {
		this.threshold = threshold;
		this.MFPElements = new ArrayList<>();
		this.instanceType = instanceType;
		this.conf = conf;
	}
	
	public void computeMFPs(TransactionsAndMapping tam) throws IOException {


		try {
			String scriptDirectory = conf.scriptDirectory; 
			String algoName = threshold > 80 ? "apriori-linux" : "fpgrowth-linux"; 
			String transactionsFilePath = tam.transactionsFilePath; 
			if (OSValidator.isWindows()) {
	        	transactionsFilePath = transactionsFilePath.replace("D:", "/mnt/d");
	        }
			String outputMFPFilePath =conf.outputMFPDirectoryPath + "/schema_minsup" + Double.valueOf(threshold).intValue() + ".txt";
			String cmd = scriptDirectory+"/"+algoName + " -tm -s" + threshold + " -m" +
					instanceType.label + " -g -v \\(%s\\) " + transactionsFilePath + " " + outputMFPFilePath;

			String bash;
			if (OSValidator.isWindows()) {
				bash = "bash";
				cmd = "-c \"" + cmd + "\"";
			} else {
				bash = "/bin/bash";
			}
			String[] cmdScript = new String[] {bash,cmd};
			for (int i=0; i<cmdScript.length;i++)
				System.out.println(cmdScript[i]);
			
			
			String cmdToExecute = String.join(" ", cmdScript);
			Process procScript = Runtime.getRuntime().exec(cmdToExecute);
		} catch (Exception e) {
			log.error("error during final step: ", e);
		}	
		System.out.println("MFP Done...");
	}
	
	

	public void saveFPSList(String mfpsDirectory) throws IOException {
		Path path = Paths.get(mfpsDirectory, this.threshold.toString());
		Files.write(path, this.MFPElements.stream().map(x -> x.toString()).collect(Collectors.toList()), Charset.forName("UTF-8"));	
	}
	
	
	public class MFPElement {
		// 1 5 9 (0.986689)
		public List<String> properties;
		public double frequency;
		
		@Override
		public String toString() {
			return String.join(" ", properties) + " (" + frequency + ")";
		}
	}

}
