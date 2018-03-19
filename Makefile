bundle:
	cd ..; tar -cvf demo-scala-labels.tar demo-scala-labels/{Makefile,src,build.sbt,README.md,project/{assembly.sbt,build.properties}}; gzip demo-scala-labels.tar 

console:
	spark-shell --packages edu.cmu.spf:slio_2.11:0.1.0 --jars bigdata-labels.jar

