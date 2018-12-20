package conceptualModel;

public class InstanceType {
	public String Uri; // e.g. http://dbpedia.org/ontology/Film
	public String label; // Film
	
	public InstanceType(String classUri) {
		this.Uri = classUri;
		int slashLastIndex = classUri.lastIndexOf("/");
		int hashtagLastIndex = classUri.lastIndexOf("#");
		int index = Math.max(slashLastIndex, hashtagLastIndex);
		this.label = classUri.substring(index + 1);
	}
}
