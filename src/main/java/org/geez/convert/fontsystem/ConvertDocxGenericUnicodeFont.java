package org.geez.convert.fontsystem;

import java.util.List;
import java.util.UUID;

import com.ibm.icu.text.Transliterator;


public class ConvertDocxGenericUnicodeFont extends ConvertFontSystem {

	public ConvertDocxGenericUnicodeFont( String rulesFile, String transliterationDirection ) {
		
		try {
			// specify the transliteration file in the first argument.
			// read the input, transliterate, and write to output
			String tableText = readRulesResourceFile( rulesFile  );
			int direction = ( "reverse".equals( transliterationDirection) ) ? Transliterator.REVERSE : Transliterator.FORWARD ;

			xlit = Transliterator.createFromRules( "Xliterator-" + UUID.randomUUID(), tableText.replace( '\ufeff', ' ' ), direction );
		} catch ( Exception ex ) {
			// put into dialog
			System.err.println( ex );
		}
	}
	
	public ConvertDocxGenericUnicodeFont( String editorText ) {
		
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
	
	
	public void setTargetTypefaces( List<String> targetTypefaces ) {
		this.targetTypefaces = targetTypefaces;
		
		for(String fontName: targetTypefaces ) {
			fontToTransliteratorMap.put( fontName, xlit );
		}
	}

}
