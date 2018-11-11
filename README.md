To install spmf (http://www.philippe-fournier-viger.com/spmf/):
mvn install:install-file -Dfile=c:/dev/java/lib/spmf.jar -DgroupId=ca.pfv -DartifactId=spmf -Dversion=2.34 -Dpackaging=jar
mvn install:install-file -Dfile=/etudiants/deptinfo/p/pari_p1/workspace/linked_itemset_sub26/lib/spmf.jar -DgroupId=ca.pfv -DartifactId=spmf -Dversion=2.34 -Dpackaging=jar

mvn clean compile assembly:single