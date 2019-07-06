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
import org.docx4j.wml.ParaRPr;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Text;


public class  StyledTextFinder extends CallbackImpl {
    
    public Map<Text,String> results = new HashMap<Text,String>();
    public Map<R.Sym,String> symResults = new HashMap<R.Sym,String>();
    
    public List<Text> resultsOrdered = new ArrayList<Text>();
    private Map<String,String> styleIdToFont = null;
    
    public StyledTextFinder( Map<String,String> styleIdToFont ) {
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

			if ( (ppr != null) && (ppr.getPStyle() != null)) {
				PStyle style = ppr.getPStyle();
				styleName = style.getVal();
			}
			
			if( styleIdToFont.containsKey( styleName ) ) {
				ParaRPr prpr = ppr.getRPr();
				// if the rpr has an rFonts setting then we have already visited
				// this text node and should not re-process.  Though it would be
				// to have both a w:style and w:rFonts set:
				if ( (prpr != null) && (prpr.getRFonts() != null) ) return null;  // UnstypedTextFinder has been here
				List<Object> pObjects = p.getContent();
				for(Object pobj: pObjects) {
					if( pobj instanceof org.docx4j.wml.R ) {
						R r = (org.docx4j.wml.R)pobj;
						RPr rpr = r.getRPr();
						if ( (rpr != null) && (rpr.getRFonts() != null) ) continue;  // UnstypedTextFinder has been here
						List<Object> rObjects = r.getContent();
						for(Object robj: rObjects) {
							Object tobj = XmlUtils.unwrap(robj);
							if ( tobj instanceof org.docx4j.wml.Text ) {
								// checkhere if styleIdToFont.get(styleName) might be null -?
								results.put( (org.docx4j.wml.Text)tobj, styleIdToFont.get(styleName) );
								resultsOrdered.add( (org.docx4j.wml.Text)tobj );
							}
							else if( tobj instanceof org.docx4j.wml.R.Sym ) {
								R.Sym sym = (org.docx4j.wml.R.Sym)tobj;
								if( styleIdToFont.containsValue( sym.getFont() ) ) {
									symResults.put( sym, sym.getFont() );
								}
							}
						}
					}
					else {
						// w:t node is a direct child of the w:p node:
						Object tobj = XmlUtils.unwrap(pobj);
						if( tobj instanceof org.docx4j.wml.Text ) {
							results.put( (org.docx4j.wml.Text)tobj, styleIdToFont.get(styleName) );
							resultsOrdered.add( (org.docx4j.wml.Text)tobj );
						}
					}
				}
			}
		}
        return null;
    }
    
}  
