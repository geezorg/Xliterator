package org.geez.convert.text;

import org.geez.convert.Converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.ibm.icu.text.Transliterator;

// StatusBar Imports:

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;


public class ConvertText extends Converter {
	protected Transliterator t = null;

    
    public ConvertText( final File inputFile, final File outputFile ) {
    	super( inputFile, outputFile );
    }
	
	public String readRulesXML( String fileName ) throws IOException {
		String line, segment, rules = "";

		ClassLoader classLoader = this.getClass().getClassLoader();
		InputStream in = classLoader.getResourceAsStream( "tables/" + fileName ); 
		BufferedReader ruleFile = new BufferedReader( new InputStreamReader(in, "UTF-8") );
		while ( (line = ruleFile.readLine()) != null) {
			if ( line.trim().equals("") || line.charAt(0) == '#' ) {
				continue;
			}
			segment = line.replaceFirst ( "^(.*?)#(.*)$", "$1" );
			rules += ( segment == null ) ? line : segment;
		}
		ruleFile.close();
		return rules;
	}


	protected Transliterator translit = null;
	void initialize( final String tableRulesFile, final String direction ) {
		try {
			//TODO:  Update readRules to read ICU XML file
			//
			
			String id = tableRulesFile; // remove the file extension
			int icuDirection = ( direction.equals("both") ) ? Transliterator.FORWARD : Transliterator.REVERSE;
			
			
			String rulesText = readRulesXML( tableRulesFile  );

			translit = Transliterator.createFromRules( id, rulesText.replace( '\ufeff', ' ' ), icuDirection );

		} catch ( Exception ex ) {
			System.err.println( ex );
		}
	}


	public String convertText( String text ) {
		return t.transliterate( text );
	}
	
	
	@Override
	public Void call() {
		process( inputFile, outputFile );
        return null;
	}
	public void process( final File inputFile, final File outputFile )
	{
		try {
			setProgress = true;
			progress.set( 0.0 );
			Thread.sleep(100);

            
    		/*
    		 * Normalize text will extract the styled and unstyled text nodes and store them
    		 * in the stf and ustf arrays accordingly:
    		 */

			// read lines of input file, write to output file
			BufferedReader in = new BufferedReader(
					   new InputStreamReader(new FileInputStream(inputFile), "UTF8")
			);
			Writer out = new BufferedWriter(
					new OutputStreamWriter( new FileOutputStream(outputFile), "UTF8" )
			);

			String line, converted;
    		while ((line = in.readLine()) != null) {
        		converted = convertText( line );
        		out.append( converted );
    		}
    		        
            in.close();
            out.flush();
            out.close();
    		
       		setProgress = false;
            

		}
		catch ( Exception ex ) {
			System.err.println( ex );
		}
	}
	

	public static void main( String[] args ) {
		if( args.length != 3 ) {
			System.err.println( "Exactly 3 arguements are expected: <system> <input file> <output file>" );
			System.exit(0);
		}

		String systemIn = args[0];
		String inputFilepath  = System.getProperty("user.dir") + "/" + args[1];
		String outputFilepath = System.getProperty("user.dir") + "/" + args[2];
		File inputFile = new File ( inputFilepath );
		File outputFile = new File ( outputFilepath );


	    ConvertText converter = null;
	    /*
		switch( systemIn ) {
			case "brana":
				converter = new ConvertDocxBrana( inputFile, outputFile );
				break;
					
			case "geezii":
				converter = new ConvertDocxFeedelGeezII( inputFile, outputFile );
				break;				
    			
		   	case "geezigna":
	    		converter = new ConvertDocxFeedelGeezigna( inputFile, outputFile );
	    		break;
	
		   	case "geezbasic":
	    		converter = new ConvertDocxGeezBasic( inputFile, outputFile );
	    		break; 
		
			case "geeznewab":
				converter = new ConvertDocxFeedelGeezNewAB( inputFile, outputFile );
				break;

			case "geeztypenet":
				converter = new ConvertDocxGeezTypeNet( inputFile, outputFile );
				break;

			case "powergeez":
				converter = new ConvertDocxPowerGeez( inputFile, outputFile );
				break;
				
			case "samawerfa":
				converter = new ConvertDocxSamawerfa( inputFile, outputFile );
				break;

			case "visualgeez":
				converter = new ConvertDocxVisualGeez( inputFile, outputFile );
				break;
				
			case "visualgeez2000":
				converter = new ConvertDocxVisualGeez2000( inputFile, outputFile );
				break;
		
			default:
				System.err.println( "Unrecognized input system: " + systemIn );
				System.exit(1);
		}
		*/
		converter.process( inputFile, outputFile );
	}
}
