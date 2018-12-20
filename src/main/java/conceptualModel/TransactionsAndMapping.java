package conceptualModel;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compute transactions
 */
public class TransactionsAndMapping {
    private static final Logger log = LoggerFactory.getLogger(DatasetInformations.class);

    private Map<String, Integer> mappingPredicateToInt;
    private List<List<Integer>> transactions;
    private Map<String, Set<String>> predicatesBySubject;

	public String transactionsFilePath;

    /**
     * Each key is a subject and each corresponding set is its predicates
     * @param predicatesBySubject
     */
    public TransactionsAndMapping(Map<String, Set<String>> predicatesBySubject, Configuration conf, Dataset ds, InstanceType instanceType) {
        this.predicatesBySubject = predicatesBySubject;
        this.mappingPredicateToInt = new HashMap<>();
        this.transactions = new ArrayList<>();
        this.transactionsFilePath = conf.transactionsFilePath.replace("<DATASETNAME>", ds.datasetName).replace("<CLASSLABEL>", instanceType.label);// initiliser ici !!!! utiliser conf, ds et className
        if (OSValidator.isWindows()) {
        	this.transactionsFilePath = this.transactionsFilePath.replace("/mnt/d", "D:");
        }
    }

    /**
     * Return the list of the transactions. E.g. each line correspond to the predicates
     * used by a subject.
     */
    public List<List<Integer>> getTransactions() {
        return transactions;
    }

    /**
     * Contains the objects corresponding to the encoding.
     * @return
     */
    public Map<String, Integer> getMappings() {
        return mappingPredicateToInt;
    }

    /**
     * Each line is a transaction composed by item separated by a space.
     * @return
     */
    public List<String> getTransactionsToStringList() {
        List<String> newTransactions = new ArrayList<>(transactions.size());
        for (List<Integer> transaction : transactions) {
            List<String> newList = new ArrayList<String>(transaction.size());
            for (Integer integer : transaction) {
                newList.add(String.valueOf(integer)); 
            }
            newTransactions.add(String.join(" ", newList));
        }
        return newTransactions;
    }

    /**
     * Compute all transactions from predicatesBySubject.
     * @throws IOException 
     */
    public void computeTransactions(boolean saveTofile) throws IOException {
        log.debug("Computing transactions...");
        int predicateCount = 1;
        for (Entry<String, Set<String>> predicateBySubject : predicatesBySubject.entrySet()) {
            Set<String> predicates = predicateBySubject.getValue();
            List<Integer> transaction = new ArrayList<>();
            for (String predicate : predicates) {
                if (mappingPredicateToInt.containsKey(predicate))
                    transaction.add(mappingPredicateToInt.get(predicate));
                else {
                    mappingPredicateToInt.put(predicate, predicateCount);
                    transaction.add(predicateCount);
                    predicateCount++;
                }
            }
            transactions.add(transaction);
        }
        if (saveTofile) {
        	//enregistrer ici les transations dans transactionsFilePath
        	Files.write(Paths.get(transactionsFilePath), getTransactionsToStringList(),  Charset.forName("UTF-8"));
        }
        log.debug("# of transactions: " + transactions.size());
    }

}