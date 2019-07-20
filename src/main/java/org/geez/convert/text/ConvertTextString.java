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


public class ConvertTextString extends Converter {
	private String text = null;
	private String textOut = null;

    public ConvertTextString( String rulesFile, String direction ) {
		this.initialize( rulesFile, direction );
    }


    public void setText( String text ) {
    	this.text = text;
    }
    
    public String getText() {
    	return text;
    }
    
    
    public String getTextOut() {
    	return textOut;
    }
    
    
	void initialize( final String tableRulesFile, final String direction ) {
		try {
			//TODO:  Update readRules to read ICU XML file
			//
			
			String id = tableRulesFile; // remove the file extension
			int icuDirection = ( direction.equals("both") || direction.equals("forward") )? Transliterator.FORWARD : Transliterator.REVERSE;
			
			
			String rulesText = readRules( tableRulesFile );

			t = Transliterator.createFromRules( id, rulesText.replace( '\ufeff', ' ' ), icuDirection );

		} catch ( Exception ex ) {
			System.err.println( ex );
		}
	}


	public String convertText( String text ) {
		textOut = t.transliterate( text );
		return textOut;
	}
	
	
	@Override
	public Void call() {
		process( text );
        return null;
	}
	public void process( String text )
	{
		try {
			setProgress = true;
			progress.set( 0.0 );
			Thread.sleep(100);

			convertText( text );
				
    		
       		setProgress = false;
            

		}
		catch ( Exception ex ) {
			System.err.println( ex );
		}
	}

}
