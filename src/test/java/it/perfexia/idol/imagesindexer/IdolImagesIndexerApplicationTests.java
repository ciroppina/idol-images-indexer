package it.perfexia.idol.imagesindexer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import it.perfexia.idol.imagesindexer.controller.HomeController;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IdolImagesIndexerApplicationTests {

	@Autowired
	HomeController controller;
	
	private final String FILENAME = "C:\\Users\\Administrator\\Pictures\\chicche\\dania_arthe.jpg";
	
	@Test
	public synchronized void testIndexing() throws Exception {
		
		String ext = FILENAME.substring(FILENAME.lastIndexOf('.'));

		BufferedInputStream is = 
			new BufferedInputStream(new FileInputStream(
				new File( FILENAME )));
		byte[] content = new byte[is.available()];
		is.read(content);
		//debug: System.out.println("\n\tREAD " + content.length + " BYTES \n");
		is.close();
		
		long indexId = controller.indexThisBinaryContent(content, FILENAME, ext);

		System.out.println("\n\tURLENCODED SIZE:  " + indexId);
		System.out.println("\tBINARY ORIG SIZE: " + content.length + "\n");
		
		assertThat(indexId > content.length);
		
		this.testRetrieve();
	}
	
	private void testRetrieve() throws Exception {
		
		System.out.println("\t\nIS CONTROLLER NULL?: " + (controller == null) + "\n");
		
		InputStream binary = null;
		try {
			binary = controller.retrieveThisBinaryContent(FILENAME);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		long size = binary.available();
		System.out.println("\n\tURLDECODED SIZE:  " + size);

		BufferedInputStream is = 
				new BufferedInputStream(new FileInputStream(
					new File( FILENAME )));
		long expected = is.available();
		System.out.println("\tEXPECTED SIZE:  " + expected);
		
		assertThat(size == expected);
	}

}
