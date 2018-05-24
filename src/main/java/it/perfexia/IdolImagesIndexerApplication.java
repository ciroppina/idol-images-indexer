package it.perfexia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.SpringVersion;

@SpringBootApplication( scanBasePackages = {"it.perfexia"} )
@EnableConfigurationProperties
public class IdolImagesIndexerApplication {

	public static void main(String[] args) {
	
		String version = SpringVersion.getVersion();
		System.out.println("\n\t@@@ sono nel MAIN della app Spring v. "+version+"\n");

		//starting the Application
		SpringApplication.run(IdolImagesIndexerApplication.class); //args
	}

}
