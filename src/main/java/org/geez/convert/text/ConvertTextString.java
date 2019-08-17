package org.geez.convert.text;

import java.util.UUID;

import org.geez.convert.Converter;

import com.ibm.icu.text.Transliterator;



public class ConvertTextString extends Converter {
	private String textIn = null;
	private String textOut = null;

    public ConvertTextString( String rulesFile, String direction ) {
		this.initialize( rulesFile, direction );
    }
    


    public ConvertTextString( String editorText ) {	
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


    public void setText( String textIn ) {
    	this.textIn = textIn;
    }
    
    public String getText() {
    	return textIn;
    }
    
    
    public String getTextOut() {
    	return textOut;
    }
    
    
	void initialize( final String tableRulesFile, final String direction ) {
		try {
			//TODO:  Update readRules to read ICU XML file
			//
			
			String id = tableRulesFile; // remove the file extension
			icuDirection = ( direction.equals("both") || direction.equals("forward") ) ? Transliterator.FORWARD : Transliterator.REVERSE;
			
			
			String rulesText = this.readRulesResourceFile( tableRulesFile);

			xlit = Transliterator.createFromRules( id, rulesText.replace( '\ufeff', ' ' ), icuDirection );

		} catch ( Exception ex ) {
			System.err.println( ex );
		}
	}


	public String convert() {
		if( textIn == null )
			return null;
		
		textOut = xlit.transliterate( textIn );
		return textOut;
	}
	public String convertText( String text ) {
		setText( text );
		return convert();
	}


}
