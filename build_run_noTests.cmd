@echo on

cd .
mvn clean install spring-boot:run -DskipTests

echo Finito!
pause