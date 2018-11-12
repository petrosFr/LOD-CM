package conceptualModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDFS;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class conceptualModel {
	private static final Logger log = LoggerFactory.getLogger(Main.class);

	public static String myNS = "http://subhi.com#";
	public static String dbpOntPath = isFileExists("/srv/www/htdocs/demo_conception/dbpedia_2016-10.nt") ? 
		"/srv/www/htdocs/demo_conception/dbpedia_2016-10.nt" :
		"dbpedia_2016-10.nt";
	public static Property sub = ResourceFactory.createProperty(myNS, "subclass");
	static Model dbpOnt = RDFDataMgr.loadModel(dbpOntPath);// <- charge dbo
	private String mfpPathFile;
	private String itemHashmap;
	private HDT hdt;

	public conceptualModel(HDT hdt) {		
		log.debug("conceptualModel constructor");
		this.hdt = hdt;
	}

	/**
	 * Helper to check if a string correspond to an existing file.
	 * @param filePathString
	 * @return
	 */
	public static boolean isFileExists(String filePathString) {
		File f = new File(filePathString);
		return f.exists() && !f.isDirectory();
	}

	public conceptualModel(String transPathFile, String mfpPathFile, String itemHashmap) {
		this.mfpPathFile = mfpPathFile;
		this.itemHashmap = itemHashmap;
	}

	public void setPathFile(String transPathFile, String mfpPathFile, String itemHashmap) {
		log.debug("entering setPathFile...");
		log.debug("transPathFile (" + isFileExists(transPathFile) + "): " + transPathFile);
		log.debug("mfpPathFile (" + isFileExists(mfpPathFile) + "): " + mfpPathFile);
		log.debug("itemHashmap : " + itemHashmap);
		this.mfpPathFile = mfpPathFile;
		this.itemHashmap = itemHashmap;
	}

	public static HashMap<Integer, String> HashmapItem = new HashMap<Integer, String>();
	public static HashMap<Integer, String> propertyMinsup = new HashMap<Integer, String>();

	@SuppressWarnings("null")
	private int getKey(HashMap<Integer, String> db, String value) {
		for (int key : db.keySet()) {
			if (db.get(key).equals(value)) {
				return key; // return the first found
			}
		}
		return (Integer) null;
	}

	/**
	 * upload hashMap
	 */
	public void readHashmap() {
		File file = new File(this.itemHashmap);
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

	/**
	 * in case of absence of the domain
	 * 
	 * @param p3
	 * @return
	 * @throws NotFoundException
	 */
	public String FindDomain(String p3) throws NotFoundException {
		HashMap<String, Integer> typeMap = new HashMap<String, Integer>();
		IteratorTripleString it = hdt.search("", p3, "");

		// We get all subjects of the wanted type first.
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
				// TODO: adapt property if Wikidata is selected
				iter = hdt.search(subject, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "");
			} catch (NotFoundException e) {
				e.printStackTrace();
			}
			Set<String> setTmp = new HashSet<>();
			while (iter.hasNext()) {
				TripleString ts = iter.next();
				String p = ts.getObject().toString();
				setTmp.add(p);
			}
			predicatesBySubject.put(subject, setTmp);
		});
		int big = 0;
		int c = 0;
		String ttt = "";
		for (Entry<String, Set<String>> entry : predicatesBySubject.entrySet()) {
			Set<String> types = entry.getValue().stream().filter((t) -> t.contains("dbpedia") && !t.contains("Wiki"))
					.collect(Collectors.toSet());
			for (String type : types) {
				if (typeMap.containsKey(type.toString())) {
					c = typeMap.get(type);
					c++;
					typeMap.put(type, c);
					if (big < c)
						big = c;
				} else {
					typeMap.put(type, 1);
				}
			}
		}
		Set<Object> oo = new HashSet<>();
		oo = getKeyFromValue(typeMap, big);
		for (Object object : oo)
			ttt = object.toString();
		return ttt;
	}

	/**
	 * in case of absence the range
	 * 
	 * @param p4
	 * @return
	 * @throws NotFoundException
	 */
	public String FindRange(String p4) throws NotFoundException {
		HashMap<String, Integer> typeMap = new HashMap<String, Integer>();
		IteratorTripleString it = hdt.search("", p4, "");
		Set<String> objectsTmp = new HashSet<>();
		while (it.hasNext()) {
			TripleString ts = it.next();
			String s = ts.getObject().toString();
			objectsTmp.add(s);
		}
		final Map<String, Set<String>> predicatesByObject = new ConcurrentHashMap<>();
		objectsTmp.parallelStream().forEach((object) -> {
			IteratorTripleString iter = null;
			try {
				// TODO: adapt property if Wikidata is selected
				iter = hdt.search(object, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "");
			} catch (NotFoundException e) {
				e.printStackTrace();
			}
			Set<String> setTmp = new HashSet<>();
			while (iter.hasNext()) {
				TripleString ts = iter.next();
				String p = ts.getObject().toString();
				setTmp.add(p);
			}
			predicatesByObject.put(object, setTmp);
		});

		int big = 0;
		int c = 0;
		String ttt = "";
		for (Entry<String, Set<String>> entry : predicatesByObject.entrySet()) {
			Set<String> types = entry.getValue().stream().filter((t) -> t.contains("dbpedia") && !t.contains("Wiki"))
					.collect(Collectors.toSet());
			for (String type : types) {
				if (typeMap.containsKey(type.toString())) {
					c = typeMap.get(type);
					c++;
					typeMap.put(type, c);
					if (big < c)
						big = c;
				} else {
					typeMap.put(type, 1);
				}
			}
		}
		Set<Object> oo = new HashSet<>();
		oo = getKeyFromValue(typeMap, big);
		for (Object object : oo)
			ttt = object.toString();
		return ttt;
	}

	/**
	 * find superclasses
	 * 
	 * @param c
	 * @return
	 */
	public Set<String> findSubclass(String c) {
		HashSet<String> sub = new HashSet<String>();
		String superclass = "";
		String requete1 = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " + "SELECT * WHERE { " + "<" + c
				+ "> rdfs:subClassOf ?superClass . " + " } ";
		Query q1 = QueryFactory.create(requete1);
		QueryExecution qexe1 = QueryExecutionFactory.create(q1, dbpOnt);
		ResultSet results1 = qexe1.execSelect();
		while (results1.hasNext()) {
			QuerySolution soln1 = results1.nextSolution();
			superclass = soln1.get("?superClass").toString();
			sub.add(superclass);
		}
		return sub;
	}

	public Set<String> findSubclassAll(String c) {
		HashSet<String> sub = new HashSet<String>();
		String superclass = "";
		String requete1 = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " + "SELECT * WHERE { " + "<" + c
				+ "> rdfs:subClassOf* ?superClass . " + " } ";
		Query q1 = QueryFactory.create(requete1);
		QueryExecution qexe1 = QueryExecutionFactory.create(q1, dbpOnt);
		ResultSet results1 = qexe1.execSelect();
		while (results1.hasNext()) {
			QuerySolution soln1 = results1.nextSolution();
			superclass = soln1.get("?superClass").toString();
			sub.add(superclass);
		}
		return sub;
	}

	public static Set<Object> getKeyFromValue(Map<String, Integer> hm, Object value) {
		Set<Object> lo = new HashSet<>();
		for (Object o : hm.keySet()) {
			if (hm.get(o).equals(value)) {
				lo.add(o);
			}
		}
		return lo;
	}

	Model outputModelsupclasses = ModelFactory.createDefaultModel();
	Model outputModelrelations = ModelFactory.createDefaultModel();

	public void CreateTxtFile(String type, String threshold, int numberofTransactions)
			throws IOException, NotFoundException {
		log.debug("entering CreateTxtFile...");
		log.debug("type: " + type);
		log.debug("threshold: " + threshold);
		log.debug("numberofTransactions: " + numberofTransactions);
		String attributes = "";
		List<String> CModel = new ArrayList<>();
		HashSet<String> finalclass = new HashSet<String>();
		CModel.add("@startuml");
		CModel.add("skinparam linetype ortho");

		Set<String> classes = new HashSet<>();
		Set<String> classesWithSubclass = new HashSet<>();
		BufferedReader reader = null;

		String pattern;
		double support;

		ArrayList<String> ObjectProperties = new ArrayList<>();
		ArrayList<String> NotObjectProperties = new ArrayList<>();
		classes.add("http://dbpedia.org/ontology/" + type);

		String line;
		readHashmap();
		File file = new File(this.mfpPathFile);
		reader = new BufferedReader(new FileReader(file));

		while ((line = reader.readLine()) != null) {
			pattern = line.substring(0, line.indexOf(" #"));
			support = Double.parseDouble(line.substring(line.indexOf("#") + 5));
			String[] properties = pattern.split(" ");
			int supp = (int) ((support / numberofTransactions) * 100);
			int thre = Integer.parseInt(threshold);
			if (properties.length == 1 && thre <= supp)
				propertyMinsup.put(Integer.parseInt(pattern), String.valueOf(supp));
		}
		reader.close();
		for (int name : propertyMinsup.keySet()) {

			int key = name;

			String property = HashmapItem.get(key);
			boolean NOObjectProperty = true;
			Set<RDFNode> listTypes = dbpOnt.listStatements(ResourceFactory.createResource(property),
					ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), (RDFNode) null)
					.toList().stream().map(Statement::getObject).collect(Collectors.toSet());
			for (RDFNode typ1 : listTypes) {
				if (typ1.asNode().toString().contains("#ObjectProperty")) {
					ObjectProperties.add(property);
					NOObjectProperty = false;
					break;
				}
			}
			if (NOObjectProperty)
				NotObjectProperties.add(property);
		}

		for (String opp : ObjectProperties) {

			int cc = getKey(HashmapItem, opp);
			String vv = propertyMinsup.get(cc);

			String dd = "";
			String rr = "";
			boolean dash = false;
			RDFNode domain = dbpOnt.getProperty(ResourceFactory.createResource(opp), RDFS.domain) != null
					? dbpOnt.getProperty(ResourceFactory.createResource(opp), RDFS.domain).getObject()
					: null;
			RDFNode range = dbpOnt.getProperty(ResourceFactory.createResource(opp), RDFS.range) != null
					? dbpOnt.getProperty(ResourceFactory.createResource(opp), RDFS.range).getObject()
					: null;

			if (domain == null) {
				dd = FindDomain(opp);
				classes.add(dd);
				dash = true;
			} else if (domain != null)
				dd = domain.toString();
			classes.add(dd.toString());

			if (range == null) {
				rr = FindRange(opp);
				classes.add(rr);
				dash = true;
			} else if (range != null)
				rr = range.toString();
			classes.add(rr.toString());

			String d = ResourceFactory.createResource(dd).toString()
					.substring(ResourceFactory.createResource(dd).toString().lastIndexOf("/") + 1);
			String r = ResourceFactory.createResource(rr).toString()
					.substring(ResourceFactory.createResource(rr).toString().lastIndexOf("/") + 1);
			String p = ResourceFactory.createProperty(opp).toString()
					.substring(ResourceFactory.createProperty(opp).toString().lastIndexOf("/") + 1);
			if (d.contains("#"))
				d = d.substring(d.lastIndexOf("#") + 1);
			if (r.contains("#"))
				r = r.substring(r.lastIndexOf("#") + 1);
			if (r.equals(d))
				continue;
			if (dash)
				CModel.add(d + " .. " + r + " : " + p + " sup:" + vv);
			else
				CModel.add(d + " -- " + r + " : " + p + " sup:" + vv);
		}
		attributes = "class " + type + "{\n";
		for (String dtp : NotObjectProperties) {

			int cc = getKey(HashmapItem, dtp);
			String vv = propertyMinsup.get(cc);
			RDFNode val = dbpOnt.getProperty(ResourceFactory.createResource(dtp), RDFS.range) != null
					? dbpOnt.getProperty(ResourceFactory.createResource(dtp), RDFS.range).getObject()
					: null;
			String p = ResourceFactory.createProperty(dtp).toString()
					.substring(ResourceFactory.createProperty(dtp).toString().lastIndexOf("/") + 1);
			if (p.contains("#"))
				p = p.substring(p.lastIndexOf("#") + 1);
			if (val != null) {
				String r = ResourceFactory.createResource(val.toString()).toString()
						.substring(ResourceFactory.createResource(val.toString()).toString().lastIndexOf("/") + 1);
				attributes = attributes + p + ":" + r + " sup=" + vv + "\n";
			} else {

				attributes = attributes + p + " sup=" + vv + "\n";
			}
		}
		attributes = attributes + "}";
		CModel.add(attributes);

		Set<String> subclasses = new HashSet<String>();
		for (String c : classes) {
			classesWithSubclass.add(c);
			subclasses = findSubclassAll(c);
			for (String s : subclasses)
				classesWithSubclass.add(s);
		}
		for (String c : classesWithSubclass) {
			subclasses = findSubclass(c);

			if (subclasses.size() == 0)
				continue;
			for (String sc : subclasses) {
				outputModelsupclasses.add(ResourceFactory.createStatement(ResourceFactory.createResource(c.toString()),
						sub, ResourceFactory.createResource(sc.toString())));

				String c1 = ResourceFactory.createResource(c).toString()
						.substring(ResourceFactory.createResource(c).toString().lastIndexOf("/") + 1);
				String c2 = ResourceFactory.createResource(sc).toString()
						.substring(ResourceFactory.createResource(sc).toString().lastIndexOf("/") + 1);
				if (c1.equals(c2))
					continue;
				if (c1.contains("#"))
					c1 = c1.substring(c1.lastIndexOf("#") + 1);
				if (c2.contains("#"))
					c2 = c2.substring(c2.lastIndexOf("#") + 1);
				CModel.add(c2 + " <|-- " + c1);
				if (c1.contains("Thing"))
					continue;
				else
					finalclass.add("\"" + c1 + "\"");
				if (c2.contains("Thing"))
					continue;
				else
					finalclass.add("\"" + c2 + "\"");
			}
		}

		CModel.add("@enduml");
		Path fileCModel = Paths
				.get("/srv/www/htdocs/demo_conception/pictures_uml/CModel_" + type + "_" + threshold + ".txt");
		Files.write(fileCModel, CModel, Charset.forName("UTF-8"));

		try (FileWriter fileJSON = new FileWriter(
				"/srv/www/htdocs/demo_conception/pictures_uml/JSONclasses_" + type + "_" + threshold + ".json")) {
			fileJSON.write(finalclass.toString());
			fileJSON.flush();
			fileJSON.close();
			System.out.println("Successfully Copied JSON Object to File...");
			System.out.println("\nJSON Object C BON");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
			// allow the web interface to handle files
			Set<PosixFilePermission> perms = Files.readAttributes(fileCModel, PosixFileAttributes.class).permissions();
			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_EXECUTE);
			perms.add(PosixFilePermission.GROUP_WRITE);
			perms.add(PosixFilePermission.GROUP_READ);
			perms.add(PosixFilePermission.GROUP_EXECUTE);
			perms.add(PosixFilePermission.OTHERS_WRITE);
			perms.add(PosixFilePermission.OTHERS_READ);
			perms.add(PosixFilePermission.OTHERS_EXECUTE);
			Files.setPosixFilePermissions(fileCModel, perms);
		}

		Main.saveModel(outputModelsupclasses, "/srv/www/htdocs/demo_conception/pictures_uml/subclasses.ttl",
				RDFFormat.TTL);
		Main.saveModel(outputModelrelations, "/srv/www/htdocs/demo_conception/pictures_uml/relation.ttl",
				RDFFormat.TTL);
		
		log.debug("outputModelsupclasses exists: " + isFileExists("/srv/www/htdocs/demo_conception/pictures_uml/subclasses.ttl"));
		log.debug("outputModelrelations exists: " + isFileExists("/srv/www/htdocs/demo_conception/pictures_uml/relation.ttl"));
		log.debug("leaving CreateTxtFile.");
	}
}
