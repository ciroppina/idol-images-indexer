package it.perfexia.idol.imagesindexer.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
* REST Service for indexing & retrieve binary content to/from IDOL Server
*
* Tested with ff. binary formats: jpg, bmp, tif, gif, pps, png
*/
@CrossOrigin(origins = "*")
@Configuration
@PropertySource("classpath:application.properties")
//@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE, value = "/")
@RestController
//@org.springframework.stereotype.Controller
public class IdolController {
		
	private final org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/**
	* Indexes to IDOL Server a given binary content, after base64UrlEncoding it
	*
	* @param fileName:String
	* @param ext:String
	*
	* @return a Long indexId from IdolServer response
	*/
	@RequestMapping(value="/index", method = {RequestMethod.POST}, params= {"fileName", "ext"})
	public synchronized @ResponseBody String indexThisBinaryContent(
		@RequestParam String fileName, @RequestParam String ext) throws IOException {

		byte[] content = this.getContent(fileName);		
		Encoder enc = Base64.getUrlEncoder();
		String cosa = enc.encodeToString(content);
		
		//debug: System.out.println("\n\tBASE64 URLENCODED: \n" + cosa + "");
		File file = new File(fileName);
		if (! file.exists()) return "ERROR -1: IL FILE NON ESISTE";
		if (! file.isAbsolute()) return "ERROR -1: IL PERCORSO DEL FILE NON E' VALIDO";
		if (! file.isFile()) return "ERROR -1: NON E' UN FILE";
		if (file.length() < 1) return "ERROR -1: IL FILE HA LUNGHEZZA ZERO";
		
		BufferedOutputStream os = new BufferedOutputStream( new FileOutputStream(
			new File(workDir +file.getName() + ".base64"), false) ); //false=overwrite
		os.write(cosa.getBytes());
		os.flush(); os.close();
		
		Map<String,String> dredocument = extractPropertiesFrom(fileName, ext);
		dredocument.put("#DREFIELD CONTENT_TYPE=", "\"BINARY BASE64 URLENCODED\"");
		dredocument.put("#DRECONTENT\n", cosa.trim());
		
		/* console debug 
		log.info("\n");
		for (String key : fileMetadata.keySet()) {
			log.info("\t" + key + " " + fileMetadata.get(key));
		}
		log.info("\n");
		*/
		
		String indexId = sendBinayToIdolServer(dredocument);
		
		return indexId.trim();
	}

	@Value( "${idol.host}" )
	private String idolHost;
	@Value( "${idol.port}" )
	private String idolPort;
	@Value( "${idol.indexport}" )
	private String idolIndexPort;
	@Value( "${idol.bynarydb}" )
	private String idolBynaryDb;
	@Value( "${binary.workdir}" )
	private String workDir;
	
	/**
	 * Tries to index binary content to IDOL Server
	 *  
	 * @param dredocument
	 * @return
	 * @throws IOException 
	 */
	private String sendBinayToIdolServer(Map<String, String> dredocument) throws IOException {
		String idolIndexId="EMPTY";
		
		//debug: logger.info("\n\t IDOL BINARY DB: " + idolBynaryDb);
		
		URL indexlUrl = new URL("http://" + idolHost+":" +idolIndexPort+"/DREADDDATA?%20HTTP/1.1");
		HttpURLConnection conn = (HttpURLConnection) indexlUrl.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		OutputStream os = conn.getOutputStream();
		String payload ="";
		payload += "#DREREFERENCE " + dredocument.get("#DREREFERENCE")+"\n";
		dredocument.remove("#DREREFERENCE");
		payload += "#DREDBNAME " + dredocument.get("#DREDBNAME")+"\n";
		dredocument.remove("#DREDBNAME");
		for (String field : dredocument.keySet()) {
			//if (! field.contains("DREREF") && ! field.contains("DREDB") )
			payload += field+" "+dredocument.get(field)+"\n";
		}
		payload += "#DREENDDOC\n";
		payload += "#DREENDDATAREFERENCE\n";
		
		os.write(payload.getBytes());
		
		InputStream is = conn.getInputStream();
		byte[] bytes = new byte[is.available()];
		is.read(bytes); 
		String idolMessage = new String(bytes);
		idolIndexId ="STATUS: " + conn.getResponseMessage() 
					+"; IDOL RETURNED: "+ idolMessage;
		
		return idolIndexId;
	}

	/**
	 * @param fileName
	 * @param ext
	 * @param file
	 * @throws IOException
	 * 
	 * @returns a Map of file properties
	 */
	private Map<String,String> extractPropertiesFrom(String fileName, String ext) throws IOException {
		File file = new File(fileName);

		Map<String,String> result = new HashMap<String,String>();
		result.put("#DREREFERENCE", fileName);
		result.put("#DREFIELD FILENAME_FULL=", "\""+fileName+"\"");
		result.put("#DREDBNAME", idolBynaryDb);
				
		String nameOfFile = file.getName();
		//System.out.println("\n\tATTRIBUTE OF FILE - nameOfFile: " + nameOfFile);
		result.put("#DREFIELD NAME_OF_FILE=", "\""+nameOfFile+"\"");
		result.put("#DRETITLE\n", nameOfFile);
		Encoder enc = Base64.getUrlEncoder();
		String base64name = enc.encodeToString( (nameOfFile+System.currentTimeMillis()) .getBytes() );
		result.put("#DREFIELD AUTN_IDENTIFIER=", "\""+base64name+"\"");

		//System.out.println("\n\tATTRIBUTE OF FILE - extension: " + ext);
		result.put("#DREFIELD EXTENSION_OF_FILE=", "\""+ext+"\"");
		Date lastModified = new Date( file.lastModified());
		//System.out.println("\n\tATTRIBUTE OF FILE - lastModifed: " + lastModified);
		result.put("#DREFIELD LAST_MODIFIED_DATE=", "\""+lastModified.toString()+"\"" );
		result.put("#DREDATE", lastModified.getTime()+"");
		BasicFileAttributes attrs = Files.readAttributes(Paths.get(fileName), BasicFileAttributes.class);
		Date created = new Date( attrs.creationTime().toMillis() );
		//System.out.println("\n\tATTRIBUTE OF FILE - created: " + created);
		result.put("#DREFIELD CREATED_DATE=", "\""+created.toString()+"\"");
		AclFileAttributeView acls = Files.getFileAttributeView(Paths.get(fileName), AclFileAttributeView.class);
		String currentOwner = acls.getOwner().getName();
		result.put("#DREFIELD CURRENT_OWNER=", "\""+currentOwner+"\"");
		//DosFileAttributeView dos = Files.getFileAttributeView(Paths.get(fileName), DosFileAttributeView.class);
		//BasicFileAttributeView basic = Files.getFileAttributeView(Paths.get(fileName), BasicFileAttributeView.class);
		UserDefinedFileAttributeView userdef = Files.getFileAttributeView(Paths.get(fileName), UserDefinedFileAttributeView.class);
		List<String> udProps = userdef.list();
		String accum = "";
		for (String udmeta : udProps) {
			accum += "|META " + udmeta;
			ByteBuffer buf = ByteBuffer.allocate(userdef.size(udmeta));
		    userdef.read(udmeta, buf);
		    buf.flip();
		    String metavalue = Charset.defaultCharset().decode(buf).toString()
		    	.replaceAll("\r", " ").replaceAll("\n", " ")
		    	.replaceAll("  ", " ").trim();
		    
		    result.put("#DREFIELD "+udmeta+"=\"", metavalue+"\"");
		    accum += "="+metavalue;
		}
		result.put("#DREFIELD USER_DEFINED_ALL_PROPERTIES=\"", accum+"\"");
		//System.out.println("\n\tATTRIBUTE OF FILE - userdefined props: " + accum +"\n");
		
		return result;
	}

	/**
	* Retrieves from FileSystem a binary content file
	*
	* @param fileName:String
	*
	* @return the read content, as a byte[] array
	*/
	private byte[] getContent(String fileName) throws IOException {
		BufferedInputStream is = 
				new BufferedInputStream(new FileInputStream(
					new File( fileName )));
		byte[] content = new byte[is.available()];
		is.read(content);
		logger.info("\n\tREAD " + content.length + " BYTES \n");
		is.close();
		
		return content;
	}
	
	/**
	 * Utility
	 * 
	 * @param url
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String urlEncodeThis(String url) throws UnsupportedEncodingException {
		return URLEncoder.encode(url, "UTF-8");
	}

	/**
	* Retrieves from IDOL Server a given binary content and, after base64UrlDecoding it,
	* returns the binary content
	*
	* @param text, a free text to query IDOL Server
	* @return the Base64 urlDecoded binary content, by copying it to the HttpResponse outputStream 
	* 
	*/
	@RequestMapping(value="/retrieve", method = {RequestMethod.GET, RequestMethod.POST} , params= {"text"})
	public synchronized void retrieveThisByText(
		@RequestParam String text, HttpServletResponse response) throws IOException {
		
		//debug	System.out.println("\n\tSONO IO?: retrieveThisByText \n");

		// LA QUERY IDOL NON FUNZIONA SOLO CON IL PARAM &text=:String
		String base64encoded = queryIdolforThis(text);
		
		Decoder dec = Base64.getUrlDecoder();
		byte[] base64decoded = dec.decode(base64encoded);
		
		String fileName = "retrieved_at_" +System.currentTimeMillis() +".decoded";
		FileOutputStream os = new FileOutputStream(new File(workDir +fileName), false); 
		os.write(base64decoded);
		logger.info("\n\tHO SCRITTO IL FILE BINARIO: " + workDir +fileName  +".decoded \n");
		os.flush(); os.close();
		
		//GRAAAAAAAAAAZIE: https://twilblog.github.io/java/spring/rest/file/stream/2015/08/14/return-a-file-stream-from-spring-rest.html
		// Set the content type and attachment header
		response.addHeader("Content-disposition", "attachment;filename="+fileName);
		response.setContentType("txt/plain");
		
		// SOSTITUIRE IN SEGUITO CON QUERY IDOL SERVER
		// Copy the stream to the response's output stream
		IOUtils.copy( new FileInputStream(new File(workDir + fileName)), response.getOutputStream());
		response.flushBuffer();
	}

	
	/**
	* Retrieves from IDOL Server a given binary content and, after base64UrlDecoding it,
	* returns the binary content
	*
	* @param fileName:String, the indexed DREREFERENCE or a IDOLServer UUID
	* <h3>CAREFUL !!!!!: urlEncode fileName BEFORE calling this method,
	*     otherwise U'll get an Illegal target Exception  </h3>
	*
	* @return the Base64 urlDecoded binary content, by copying it to the HttpResponse outputStream 
	*/
	@RequestMapping(value="/retrieve", method = {RequestMethod.GET, RequestMethod.POST} , params= {"fileName"})
	public synchronized void retrieveThisBinaryContent(
		@RequestParam String fileName, HttpServletResponse response) throws IOException {
		
		//debug	System.out.println("\n\tSONO IO?: retrieveThisBinaryContent \n");

		String nameOfFile = new File(fileName).getName();
		// LA QUERY IDOL
		String base64encoded = queryIdolforThis(nameOfFile);
		
		Decoder dec = Base64.getUrlDecoder();
		byte[] base64decoded = dec.decode(base64encoded);
		
		FileOutputStream os = new FileOutputStream(new File(workDir +nameOfFile +".decoded"), false); 
		os.write(base64decoded);
		logger.info("\n\tHO SCRITTO IL FILE BINARIO: " + workDir +nameOfFile  +".decoded \n");
		os.flush(); os.close();
		
		//GRAAAAAAAAAAZIE: https://twilblog.github.io/java/spring/rest/file/stream/2015/08/14/return-a-file-stream-from-spring-rest.html
		// Set the content type and attachment header
		response.addHeader("Content-disposition", "attachment;filename=" +fileName+ ".decoded");
		response.setContentType("txt/plain");
		
		// Copy the stream to the response's output stream
		IOUtils.copy( new FileInputStream(new File(fileName + ".decoded")), response.getOutputStream());
		response.flushBuffer();
	}

	/**
	 * Retrieves the Base64 UrlEncoded Binary DRECONTENT from Idol Server
	 * 
	 * @param someText
	 * @return the DRECONTENT String, base64 urlEncoded
	 * @throws IOException
	 */
	private String queryIdolforThis(String someText) throws IOException {
		String base64encoded = "";
		
		URL indexlUrl = new URL("http://" + idolHost+":" +idolPort 
			+"/a=query&databaseMatch=" +idolBynaryDb +"&anyLanguage=true"
			+ "&print=fields&printfields=DREREFERENCE,CONTENT_TYPE,DRECONTENT"
			+ "&responseFormat=xml&maxResults=1"
			+ "&text={" +someText +"}");
		HttpURLConnection conn = (HttpURLConnection) indexlUrl.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoInput(true);
		
		try {
		//debug
		@SuppressWarnings("unused")
		int responseCode = conn.getResponseCode();
		} catch(Exception e) {
			return "<h4>Problem occurred trying to connect a remote IDOLServer</h4>";
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(
			conn.getInputStream()));
		String inputLine = null;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		logger.info("\n\tIDOL RETURNED Chars: " + response.length() );
					
		base64encoded = new String(response.toString());
		
		if(base64encoded.contains("<CONTENT_TYPE>BINARY BASE64"))
			base64encoded = base64encoded.substring(
				base64encoded.indexOf("DRECONTENT")+11, 
				base64encoded.indexOf("</DRECONTENT")
			).trim();
		else base64encoded ="";
		
		return base64encoded;
	}

}