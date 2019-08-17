package org.geez.convert.text;

import org.geez.convert.Converter;


import java.util.UUID;
import com.ibm.icu.text.Transliterator;



public class ConvertTextFile extends Converter {

    public ConvertTextFile( final String rulesFile, String direction ) {
		this.initialize( rulesFile, direction );
    }


    public ConvertTextFile( String editorText ) {	
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


}
