package org.geez.convert.docx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Style;


public class DocxUtils {
	   private static Map<String,String> styleIdToFont = new HashMap<String,String>();
	    
	   /* Create a map of style IDs to font target names.
	    * If a target font is found in a style definition,
	    *   set the typeface to the requested output font
	    *   (now, while we're at it, rather than rescan later to do so)
	    */
	    public static Map<String,String> readStyles( WordprocessingMLPackage wordMLPackage, List<String> targetTypefaces, String fontOut ) {
	    	
	    	MainDocumentPart mdp = wordMLPackage.getMainDocumentPart();
	    	if( mdp == null ) {
	    		return styleIdToFont;
	    	}
	    	StyleDefinitionsPart sdp = mdp.getStyleDefinitionsPart();
	    	if( sdp == null ) {
	    		return styleIdToFont;
	    	}

	    	boolean isSet = false;
	    	List<Style> styleList = sdp.getJaxbElement().getStyle();
	    	for (Style style : styleList) {
	    		String name = style.getName().getVal();
	    		String id = sdp.getIDForStyleName( name );
	    		RPr rpr = style.getRPr();
	    		if( (rpr != null) && (rpr.getRFonts() != null) ) {
	    			RFonts rfonts = rpr.getRFonts();
	    			String ascii = rfonts.getAscii();
	    			if( (ascii != null) && targetTypefaces.contains( ascii ) ) {
	    				styleIdToFont.put( id, ascii );
	    				rfonts.setAscii( fontOut );
	    				isSet = true;
	    			}
	    			String hAnsi = rfonts.getHAnsi();
	    			if( (hAnsi != null) &&  targetTypefaces.contains( hAnsi ) ) {
	    				if(! isSet)  {
	    					styleIdToFont.put( id, hAnsi );
	    				}
	    				rfonts.setHAnsi( fontOut );
	    				isSet = true;
	    			}
	    			String cs = rfonts.getCs();
	    			if( (cs != null) &&  targetTypefaces.contains( cs ) ) {
	    				if(! isSet)  {
	    					styleIdToFont.put( id, cs );
	    				}
	    				rfonts.setCs( fontOut );
	    				isSet = true;
	    			}
	    			String eastAsia = rfonts.getEastAsia();
	    			if( (eastAsia != null) &&  targetTypefaces.contains( eastAsia ) ) {
	    				if(! isSet)  {
	    					styleIdToFont.put( id, eastAsia );
	    				}
	    				rfonts.setEastAsia( fontOut );
	    				isSet = true;
	    			}
	    			
	    		}
	    		if( style.getBasedOn() != null ) {
	    			String basedOn = style.getBasedOn().getVal();
	    			if( styleIdToFont.containsKey( basedOn ) ) {
	    				styleIdToFont.put( id, styleIdToFont.get( basedOn ) );
	    			}
	    		}
	    	}
	    	
	    	return styleIdToFont ;
	    	
	    }
	    
	    
	    public static Map<String,String> getStyleMap() {
	    	return styleIdToFont ;
	    }
}
