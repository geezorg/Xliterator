package org.geez.convert.docx;

import java.io.File;
import java.util.List;
import java.util.UUID;

import com.ibm.icu.text.Transliterator;

/*
 * The non-maven way to build the jar file:
 *
 * javac -Xlint:deprecation -cp docx4j-6.0.1.jar:dependencies/commons-io-2.5.jar:../icu4j-63_1.jar:dependencies/slf4j-api-1.7.25.jar:slf4j-1.7.25 *.java
 * jar -cvf convert.jar org/geez/convert/docx/*.class org/geez/convert/tables/
 * java -cp convert.jar:docx4j-6.0.1.jar:dependencies/*:../icu4j-63_1.jar:slf4j-1.7.25/slf4j-nop-1.7.25.jar org.geez.convert.docx.ConvertDocx geeznewab myFile-In.docx myFile-Out.docx
 *
 */


public class ConvertDocxGenericUnicodeFont extends ConvertDocx {

	public ConvertDocxGenericUnicodeFont( final File inputFile, final File outputFile, String rulesFile, String transliterationDirection ) {
		super( inputFile, outputFile );
		
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
	
	public ConvertDocxGenericUnicodeFont( final File inputFile, final File outputFile, String editorText ) {
		super( inputFile, outputFile );
		
		String rulesText = editorText;
		if( editorText.startsWith( "<?xml" ) ) {
			rulesText = readRulesStringXML( editorText );
		}
		
		try {
			xlit = Transliterator.createFromRules( "Xliterator-" + UUID.randomUUID(), rulesText, Transliterator.FORWARD );
		} catch ( Exception ex ) {
			// put into dialog
			System.err.println( ex );
		}
	}
	
	
	public void setTargetTypefaces(List<String> targetTypefaces) {
		this.targetTypefaces = targetTypefaces;
		
		for(String fontName: targetTypefaces ) {
			fontToTransliteratorMap.put( fontName, xlit );
		}
	}

}
