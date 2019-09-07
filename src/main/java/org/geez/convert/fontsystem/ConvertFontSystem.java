package org.geez.convert.fontsystem;

import org.geez.convert.Converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.ibm.icu.text.Transliterator;


public class ConvertFontSystem extends Converter {
	// protected String fontOut = null;
	//protected String fontIn = null;
	protected char huletNeteb = 0x0;
	
	public Set<String> supportedFonts = null;
	
	public ConvertFontSystem() {
		super();
	}
	
	public ConvertFontSystem( String direction ) {
	 	super( direction );
	}
	 
	public ConvertFontSystem( int icuDirection ) {
	 	super( icuDirection );
	}

	
	protected String fontName = null;
	protected List<String> targetTypefaces = new  ArrayList<String>();
	protected Map<String,Transliterator> fontToTransliteratorMap = new HashMap<String,Transliterator>();
	
	public void initialize( final String rulesFile, final String fontName )
	{
		// reverse because DocxConvert maps are defined Ethiopic -> Latin
		initialize( rulesFile, fontName, "reverse" );
	}
	
	public void initialize( final String rulesFile, final String fontName, final String transliterationDirection )
	{
		
		try {
			String tableText = readRulesResourceFile( rulesFile  );
			int direction = ( "reverse".equals( transliterationDirection) ) ? Transliterator.REVERSE : Transliterator.FORWARD ;
	
			String id;
			if( IDs == null ) {
				id = "Xliterator-" + UUID.randomUUID();
			}
			else {
				id = IDs[0];
			}
			xlit = Transliterator.createFromRules( id, tableText.replace( '\ufeff', ' ' ), direction );
			
			this.fontName = fontName;
			
			targetTypefaces.add( fontName );
			fontToTransliteratorMap.put( fontName, xlit );
		} catch ( Exception ex ) {
			// put into dialog
			System.err.println( ex );
		}
	}

	
	protected void localCheck(String text) {
		// system specific extra conversions
		return;
	}
 	
	public String convertText( String text, String fontIn ) {
		xlit = fontToTransliteratorMap.get( fontIn );
		if ( xlit == null ) {
			return null;
		}
		// localCheck( text ); no systems are using this anymore
		return xlit.transliterate( text );
	}
	
	public String convertText( String text ) {
		// this would only work for mono-font systems
		return xlit.transliterate( text );
	}
	
	public Set<String> getSupportedFonts() {
		return supportedFonts;
	}
	
	public List<String> getTargetTypefaces() {
		return targetTypefaces;
	}
	
	public char getHuletNeteb() {
		return huletNeteb;
	}
}
