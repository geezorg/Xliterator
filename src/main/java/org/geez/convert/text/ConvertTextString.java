package org.geez.convert.text;

import org.geez.convert.Converter;

import com.ibm.icu.text.Transliterator;



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
			icuDirection = ( direction.equals("both") || direction.equals("forward") ) ? Transliterator.FORWARD : Transliterator.REVERSE;
			
			
			String rulesText = this.readRulesResourceFile( tableRulesFile);

			xlit = Transliterator.createFromRules( id, rulesText.replace( '\ufeff', ' ' ), icuDirection );

		} catch ( Exception ex ) {
			System.err.println( ex );
		}
	}


	public String convertText( String text ) {
		textOut = xlit.transliterate( text );
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
