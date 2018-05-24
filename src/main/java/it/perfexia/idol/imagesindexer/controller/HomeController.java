package it.perfexia.idol.imagesindexer.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
//@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE, value = "/home")
@RestController
//@org.springframework.stereotype.Controller
public class HomeController {
	
	private final String WELCOME_MSG;
	public HomeController() {
		this.WELCOME_MSG = "<p>The service provides a REST Endpoint that accepts streamed image files to be indexed to IDOLServer"
			+ "<br/>also, it provides another REST Endpoint that returns streamed image files from IDOLServer, by fileName</p>";
	}
	
	@RequestMapping("/home")
    public @ResponseBody String home() {
		System.out.println("\n\t@@@ sono nel metodo /home \n");
        return "<h4>Welcome to this IDOLServer base64 Images Indexer & Retriever service </h4>"
			+ WELCOME_MSG;
    }
	
	public synchronized Long indexThisBinaryContent(byte[] content, String fileName, String ext) throws IOException {
		if (content == null || content.length < 10)
			content = this.getContent(fileName);
		
		Encoder enc = Base64.getUrlEncoder();
		String cosa = enc.encodeToString(content);
		
		//debug: System.out.println("\n\tBASE64 URLENCODED: \n" + cosa + "");
		
		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(new File(fileName + ".base64"), false) );
		os.write(cosa.getBytes());
		os.flush(); os.close();
		// SOSTITUIRE IN SEGUITO CON INDEX-ID DI IDOL SERVER
		return new Long (cosa.length());
	}

	private byte[] getContent(String fileName) throws IOException {
		BufferedInputStream is = 
				new BufferedInputStream(new FileInputStream(
					new File( fileName )));
		byte[] content = new byte[is.available()];
		is.read(content);
		System.out.println("\n\tREAD " + content.length + " BYTES \n");
		is.close();
			
		return content;
	}
	
	public synchronized InputStream retrieveThisBinaryContent(String fileName) throws IOException {
		
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(new File(fileName + ".base64")));
		byte[] base64encoded = new byte[is.available()];
		is.read(base64encoded);
		is.close();
		
		Decoder dec = Base64.getUrlDecoder();
		base64encoded = dec.decode(base64encoded);
		
		FileOutputStream os = new FileOutputStream(new File(fileName + ".decoded"), false); 
		os.write(base64encoded);
		System.out.println("\n\tHO SCRITTO IL FILE BINARIO: " + fileName + ".decoded");
		os.flush(); os.close();
		
		// SOSTITUIRE IN SEGUITO CON QUERY IDOL SERVER
		return new BufferedInputStream(new FileInputStream(new File(fileName + ".decoded")));
	}

}