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
import java.util.UUID;

import com.ibm.icu.text.Transliterator;

// StatusBar Imports:

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;


public class ConvertTextFile extends Converter {

    public ConvertTextFile( final File inputFile, final File outputFile,  String rulesFile, String direction ) {
    	super( inputFile, outputFile );
		this.initialize( rulesFile, direction );
    }


    public ConvertTextFile( final File inputFile, final File outputFile,  String editorText ) {
    	super( inputFile, outputFile );
		
		try {
			String rulesText = editorText;
			if( editorText.startsWith( "<?xml" ) ) {
				rulesText = readRulesStringXML( editorText );
			}
			xlit = Transliterator.createFromRules( "Xliterator-" + UUID.randomUUID(), rulesText, Transliterator.FORWARD );
		} catch ( Exception ex ) {
			// put into dialog
			System.err.println( ex );
		}
	}
    

	void initialize( final String tableRulesFile, final String direction ) {
		try {
			//TODO:  Update readRules to read ICU XML file
			//
			
			String id = tableRulesFile; // remove the file extension
			icuDirection = ( direction.equals("both") || direction.equals("forward") ) ? Transliterator.FORWARD : Transliterator.REVERSE;
			
			
			String rulesText = readRulesResourceFile( tableRulesFile );

			xlit = Transliterator.createFromRules( id, rulesText.replace( '\ufeff', ' ' ), icuDirection );

		} catch ( Exception ex ) {
			System.err.println( ex );
		}
	}


	public String convertText( String text ) {
		return xlit.transliterate( text );
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
        		out.append( converted + "\n" );
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

}
