package org.geez.convert;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;

public class Converter  implements Callable<Void> {
	protected char huletNeteb = 0x0;
	protected boolean setProgress = true;

    protected final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper();

    protected File inputFile = null, outputFile = null;
    
    DocumentBuilder builder = null; 
    public void init() {
    	try {
    		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    	
    		builder.setEntityResolver(new EntityResolver() {
    			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
    				if (systemId.contains("ldmlSupplemental.dtd")) {
    					ClassLoader classLoader = this.getClass().getClassLoader();
    					InputStream dtdStream = classLoader.getResourceAsStream("common/dtd/ldmlSupplemental.dtd");
    					return new InputSource(dtdStream);
    				} else {
    					return null;
    				}
    			}
    		});
    	}
    	catch( ParserConfigurationException ex) {
    		System.err.println( ex );
    	}
    }
    
    public Converter( final File inputFile, final File outputFile ) {
    	this.inputFile  = inputFile;
    	this.outputFile = outputFile;
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty() ;
    }   
    
    public final double getProgress() {
        return progressProperty().get();
    }
    
	@Override
	public Void call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	
	private String readRulesFromStream(InputStream is) throws IOException {
		String line, segment, rules = "";
		BufferedReader rulesFile = new BufferedReader( new InputStreamReader(is, "UTF-8") );
		while ( (line = rulesFile.readLine()) != null) {
			if ( line.trim().equals("") || line.charAt(0) == '#' ) {
				continue;
			}
			segment = line.replaceFirst ( "^(.*?)#(.*)$", "$1" );
			rules += ( segment == null ) ? line : segment;
		}
		rulesFile.close();
		return rules;	
	}
	
	public String readRules( String rulesFileTXT ) throws IOException {
		ClassLoader classLoader = this.getClass().getClassLoader();
		InputStream in = classLoader.getResourceAsStream( "tables/" + rulesFileTXT ); 
		return readRulesFromStream( in );
	}
	
	
	public String readRulesXML(String rulesFileXML ) throws Exception {		
		ClassLoader classLoader = this.getClass().getClassLoader();
		if(! rulesFileXML.contains( "/" ) ) {
			// this is a week test for a path hierarchy, we assume some path for a user defined file
			rulesFileXML = "common/transforms/" + rulesFileXML ; 
		}
		InputStream xmlStream = classLoader.getResourceAsStream( rulesFileXML );    
	    
	    Document doc = builder.parse( xmlStream );
	    NodeList nodes = doc.getElementsByTagName("tRule");
	    Element  element = (Element) nodes.item(0); // assume only one
	    
	    String rulesString = getCharacterDataFromElement( element );
	    InputStream is = new ByteArrayInputStream( rulesString.getBytes() );
	    
		return readRulesFromStream( is );
	}

	public static String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) {
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		}
		return "";
	}

}
