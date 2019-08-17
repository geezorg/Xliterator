package org.geez.convert.docx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.docx4j.TraversalUtil.CallbackImpl;
import org.docx4j.XmlUtils;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.ParaRPr;
import org.docx4j.wml.R;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Text;


public class DocxUnstyledTextFinder extends CallbackImpl {
    
    public Map<Text,String> results = new HashMap<Text,String>();
    public Map<R.Sym,String> symResults = new HashMap<R.Sym,String>();

    public List<Text> resultsOrdered = new ArrayList<Text>();
    private String fontOut = null;
    List<String> targetTypefaces= null;
    
    public DocxUnstyledTextFinder( List<String> targetTypefaces, String fontOut ) {
    	super();
    	this.targetTypefaces = targetTypefaces;
    	this.fontOut = fontOut;
    }
    
    
    public void clearResults() {
    	results.clear();
    	resultsOrdered.clear();
    	symResults.clear();
    }
    
    
	public String checkTargetFont( RFonts rfonts ) {
		
		if( rfonts == null ) {
			return null;
		}
	
		// We assume one of these fields will be set, and not more then one legacy typeface is set per RFonts element.
		boolean isSet = false;
		String fontIn = null;
		if( targetTypefaces.contains( rfonts.getAscii() ) ) {
			fontIn = rfonts.getAscii();
			rfonts.setAscii( fontOut );
			if( fontIn != null ) {
				isSet = true;
			}
		}
		if( targetTypefaces.contains( rfonts.getHAnsi() ) ) {
			if(! isSet ) {
				fontIn = rfonts.getHAnsi();
				if( fontIn != null ) {
					isSet = true;
				}
			}
			rfonts.setHAnsi( fontOut );
		}
		if( targetTypefaces.contains( rfonts.getCs() ) && isSet ) {
			/* After discovering the following, it seems best to not rely on w:cs
			 * because it can contradict with w:ascii and w:hAnsi
			 * 
			 * <w:rFonts w:ascii="VG2000 Main" w:hAnsi="VG2000 Main" w:cs="Ge'ez-1"/>
			 */
			/*
			if(! isSet ) {
				fontIn = rfonts.getCs();
				if( fontIn != null ) {
					isSet = true;
				}
			}
			*/
			rfonts.setCs( fontOut );
		}
		if( targetTypefaces.contains( rfonts.getEastAsia() ) ) {
			if(! isSet ) {
				fontIn = rfonts.getEastAsia();
				if( fontIn != null ) {
					isSet = true;
				}
			}
			rfonts.setEastAsia( fontOut );
		}

		return fontIn ;
	}
    
    /* update this class like StyledTextFinder where the constructor receives a list
     * of target fonts, and retrieves any w:text nodes for runs that apply the font
     */
    @Override
    public List<Object> apply(Object o) {
		
    	if (o instanceof org.docx4j.wml.R) {
    		R r = (org.docx4j.wml.R)o;
			RPr rpr = r.getRPr();
			if ( (rpr == null) || (rpr.getRFonts() == null) ) return null;

			String fontIn = checkTargetFont( rpr.getRFonts() );
			if ( fontIn == null ) {
				return null;
			}
			List<Object> rObjects = r.getContent();
			for(Object robj: rObjects) {
				Object tobj = XmlUtils.unwrap(robj);
				if ( tobj instanceof org.docx4j.wml.Text ) {
					results.put( (org.docx4j.wml.Text)tobj, fontIn );
					resultsOrdered.add( (org.docx4j.wml.Text)tobj );
				}
				else if( tobj instanceof org.docx4j.wml.R.Sym ) {
					R.Sym sym = (org.docx4j.wml.R.Sym)tobj;
					if( targetTypefaces.contains( sym.getFont() ) ) {
						symResults.put( sym, sym.getFont() );
					}
				}
			}
		}
		else if (o instanceof org.docx4j.wml.P) {
			// Why do we do this checking if we store no found nodes??
			// Remove this code, if nothing breaks after commenting it out
			/* We need this code block to map the font name (in checkTargetFont) to handle
			 * hidden control characters for paragraph breaks (¶) and the like,
			 * for example:
			 *          
			<w:p w:rsidR="00000000" w:rsidRDefault="002335F2">
                <w:pPr>
                    <w:jc w:val="center"/>
                    <w:rPr>
                        <w:rFonts w:ascii="GeezNewA" w:hAnsi="GeezNewA"/>
                        <w:i/>
                        <w:iCs/>
                        <w:sz w:val="28"/>
                    </w:rPr>
                </w:pPr>
			 */
			P p = (org.docx4j.wml.P)o;
			PPr ppr = p.getPPr();
			if (ppr == null ) return null;
			ParaRPr prpr = ppr.getRPr();
			if ( (prpr == null) || (prpr.getRFonts() == null) ) return null;

			String fontIn = checkTargetFont( prpr.getRFonts() );
			if ( fontIn == null ) {
				return null;
			}
		}
		
    	
        return null;
    }
}  
