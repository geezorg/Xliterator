package org.geez.convert.docx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.docx4j.TraversalUtil.CallbackImpl;
import org.docx4j.XmlUtils;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase.PStyle;
import org.docx4j.wml.R;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Text;


public class DocxStyledTextFinder extends CallbackImpl {
    
    public Map<Text,String> results = new HashMap<Text,String>();
    public Map<R.Sym,String> symResults = new HashMap<R.Sym,String>();
    
    public List<Text> resultsOrdered = new ArrayList<Text>();
    private Map<String,Map<String,String>> styleIdToFont = null;
    
    public DocxStyledTextFinder( Map<String,Map<String,String>> styleIdToFont ) {
    	super();
    	this.styleIdToFont = styleIdToFont;
    }
    
    
    public boolean hasStyles() { 
    	return !( styleIdToFont.isEmpty() );
    }
    
    
    public void clearResults() {
    	results.clear();
    	resultsOrdered.clear();
    }
    
    
    @Override
    public List<Object> apply(Object o) {
    	if  (o instanceof org.docx4j.wml.P) {
			P p = (org.docx4j.wml.P)o;
			
			PPr ppr = p.getPPr();
			if (ppr == null) return null;
			String styleName = "Normal";

			// if ( (ppr != null) && (ppr.getPStyle() != null)) {
			if ( ppr.getPStyle() != null ) {
				PStyle style = ppr.getPStyle();
				styleName = style.getVal();
			}
			
			if( styleIdToFont.containsKey( styleName ) ) {
				Map<String,String> encodingToFont = styleIdToFont.get( styleName );
				String targetFont = encodingToFont.get( "ascii" );
				if( targetFont == null ) {
					targetFont = encodingToFont.get( "hAnsi" );
					if( targetFont == null ) {
						targetFont = encodingToFont.get( "cs" );
						if( targetFont == null ) {
							targetFont = encodingToFont.get( "eastAsia" );
						}
					}
				}
				List<Object> pObjects = p.getContent();
				for(Object pobj: pObjects) {
					if( pobj instanceof org.docx4j.wml.R ) {
						R r = (org.docx4j.wml.R)pobj;
						RPr rpr = r.getRPr();
						
						// Check if the rFont is overriding the the target font in one of the encoding options,
						// if so, do not capture this node:
						if ( (rpr != null) && (rpr.getRFonts() != null) ) {
							RFonts rfonts = rpr.getRFonts();
							if( encodingToFont.containsKey( "ascii") ) {
								String targetAsciiFont = encodingToFont.get( "ascii" );
								String encoding = rfonts.getAscii();
								if( (encoding != null) && !encoding.equals( targetAsciiFont ) ) {
									continue; // an override has occurred
								}	
							}
							if( encodingToFont.containsKey( "hAnsi") ) {
								String targethAnsiFont = encodingToFont.get( "hAnsi" );
								String encoding = rfonts.getAscii();
								if( (encoding != null) && !encoding.equals( targethAnsiFont ) ) {
									continue; // an override has occurred
								}	
							}
						}
						//
						// RPr rpr = r.getRPr();
						// if ( (rpr != null) && (rpr.getRFonts() != null) ) continue;  // UnstypedTextFinder has been here
						//
						// this check doesn't go far enough.  it may be the case that the rpr defines only the font
						// for one of the 4 encodings, like w:cs, and not w:hAnsi or w:ascii where an Ethopic font might be set,
						// thus this is not an override and UnstyledTextFinder also may not have found anything.  An example:
						/*
						 * document/styles.xml
						 * ...
						 * <w:style w:type="character" w:customStyle="1" w:styleId="BodyTextChar"><w:name w:val="Body Text Char"/>
						 *   <w:basedOn w:val="DefaultParagraphFont"/>
						 *   <w:link w:val="BodyText"/><w:semiHidden/>
						 *   <w:rsid w:val="009616E8"/>
						 *   <w:rPr>
						 *   <w:rFonts w:ascii="GeezNewA" w:eastAsia="Times New Roman" w:hAnsi="GeezNewA" w:cs="Times New Roman"/>
						 *   <w:szCs w:val="20"/>
						 *   </w:rPr>
						 * </w:style>
						 *
						 * then in the document:
						 *
						 *  <w:pPr>
						 *  <w:pStyle w:val="BodyText"/>
						 *  <w:rPr>
						 *  <w:rFonts w:cs="Arial"/>
						 *  </w:rPr>
						 *  </w:pPr>
						 *
						 *  therefore we should track the encoding attribute that the ethiopic typeface is defined under (w:hAnsi, etc)
						 *  within a style definition to then check of the newly encountered rpr overrides it or not.  thus styleIdToFont
						 *  should be a hash of these attributes.
						 *  
						 */
						List<Object> rObjects = r.getContent();
						for(Object robj: rObjects) {
							Object tobj = XmlUtils.unwrap(robj);
							if ( tobj instanceof org.docx4j.wml.Text ) {
								// check here if styleIdToFont.get(styleName) might be null -?
								results.put( (org.docx4j.wml.Text)tobj, targetFont );
								resultsOrdered.add( (org.docx4j.wml.Text)tobj );
							}
							else if( tobj instanceof org.docx4j.wml.R.Sym ) {
								R.Sym sym = (org.docx4j.wml.R.Sym)tobj;
								if( encodingToFont.containsValue( sym.getFont() ) ) {
									symResults.put( sym, sym.getFont() );
								}
							}
						}
					}
					else {
						// w:t node is a direct child of the w:p node:
						Object tobj = XmlUtils.unwrap(pobj);
						if( tobj instanceof org.docx4j.wml.Text ) {
							results.put( (org.docx4j.wml.Text)tobj, targetFont );
							resultsOrdered.add( (org.docx4j.wml.Text)tobj );
						}
					}
				}
			}
		}
        return null;
    }
    
}  
