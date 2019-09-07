package org.geez.convert.fontsystem;

import com.ibm.icu.text.Transliterator;


abstract class ConvertFontSystemDuoFont extends ConvertFontSystem {
	protected Transliterator translit1 = null;
	protected Transliterator translit2 = null;
	protected String fontName1 = null;
	protected String fontName2 = null;

	
	public ConvertFontSystemDuoFont() {
		super();
	}
	
	public void initialize(
		final String table1RulesFile,
		final String table2RulesFile,
		final String fontName1,
		final String fontName2)
	{
		try {
			String table1Text = readRulesResourceFile( table1RulesFile  );
			String table2Text = readRulesResourceFile( table2RulesFile );

			translit1 = Transliterator.createFromRules( IDs[0], table1Text.replace( '\ufeff', ' ' ), Transliterator.REVERSE );
			translit2 = Transliterator.createFromRules( IDs[1], table2Text.replace( '\ufeff', ' ' ), Transliterator.REVERSE );
			this.fontName1 = fontName1;
			this.fontName2 = fontName2;
			
			targetTypefaces.add( fontName1 );
			targetTypefaces.add( fontName2 );
			fontToTransliteratorMap.put( fontName1, translit1 );
			fontToTransliteratorMap.put( fontName2, translit2 );

		} catch ( Exception ex ) {
			System.err.println( ex );
		}
	}
}
