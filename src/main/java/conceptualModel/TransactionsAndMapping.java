package conceptualModel;

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

    /**
     * Each key is a subject and each corresponding set is its predicates
     * @param predicatesBySubject
     */
    public TransactionsAndMapping(Map<String, Set<String>> predicatesBySubject) {
        this.predicatesBySubject = predicatesBySubject;
        this.mappingPredicateToInt = new HashMap<>();
        this.transactions = new ArrayList<>();
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
     * Reach line is a transaction composed by item separated by a space.
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
     */
    public void computeTransactions() {
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
        log.debug("# of transactions: " + transactions.size());
    }

}