###################################
### idol-images-indexer project ###

	this spring-boot 2.1.0.BUILD-SNAPSHOT
	project, provides REST endpoints for:
	
	- indexing BINARY (Base64Enc) files
	  to a remote IDOL Server database
	  
	- retrieve from a remote IDOL Server
	  database BINARY (Base64Dec) files
	  
	CAREFUL !!! bandwidth usage could be
	very huge
	
	
###	Endpoints:

	- /index?fileName=[fullPath]&ext=[format extension]
	  returns: @ResponseBody String [message from IDOL Server]
	  
	- /retrieve?fileName=[fullPath]
	  param fileName: the indexed DREREFERENCE or a IDOLServer UID
	  CAREFUL!: urlEncode fileName BEFORE calling this method
	  returns: the Base64 urlDecoded binary content, 
			   by copying it to the HttpResponse outputStream
	
	- /retrieve?text=[a free text]
	  returns: the Base64 urlDecoded binary content, 
			   by copying it to the HttpResponse outputStream

### Build:

	- mvn clean install [-DskipTests]
	
### Tests:

	to run tests:
	- mvn clean install
	
	CAREFUL! test class "IdolImagesIndexerApplicationTests"
	contains a hardcoded full file path: FILENAME, 
	for testing purposes. Change it with your own
	

### Run

	to run without testing:
	- mvn spring-boot:run -DskipTests=true
	
### Config

	config properties file:
	- src/main/resources/application.properties
	contains setting for remote IDOL Server
	and declares a working directory:
	- "binary.workdir"
	
###      THAT'S ALL FOLKS !     ###
###################################
