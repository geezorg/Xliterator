package org.geez.convert.docx;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.docx4j.TraversalUtil;
import org.docx4j.XmlUtils;
import org.docx4j.model.structure.HeaderFooterPolicy;
import org.docx4j.model.structure.SectionWrapper;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.JaxbXmlPart;
import org.docx4j.openpackaging.parts.WordprocessingML.EndnotesPart;
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart;
import org.docx4j.openpackaging.parts.WordprocessingML.FootnotesPart;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;
import org.geez.convert.DocumentProcessor;
import org.geez.convert.fontsystem.ConvertFontSystem;
import org.geez.convert.fontsystem.ConvertFontSystemDiacriticalSystem;


public class DocxProcessor extends DocumentProcessor {
	
	protected double totalNodes = 0;
	
	protected List<String> targetTypefaces = new  ArrayList<String>();
	protected Map<String,ConvertFontSystem> fontToConverterMap = new HashMap<String,ConvertFontSystem>();
	protected String fontOut = null;
	
	public void addConverter(ConvertFontSystem converter) {
		targetTypefaces.addAll( converter.getTargetTypefaces() );
		for(String font: converter.getTargetTypefaces()) {
			fontToConverterMap.put(font, converter);
		}
	}
    
	private void processText(Text text, String fontIn) {
		ConvertFontSystem converter = fontToConverterMap.get( fontIn );
		if( converter.isSpacePreservableSymbol( text.getValue() ) ) {
			text.setSpace( "preserve" );
		}
		String out = converter.convertText( text.getValue(), fontIn );
		text.setValue( out );
	}
	
	public void processStyledObjects( final JaxbXmlPart<?> part, DocxStyledTextFinder stFinder ) throws Docx4JException {
		if(! stFinder.hasStyles() ) {
			return;
		}
		stFinder.clearResults();
		
		new TraversalUtil( part.getContents(), stFinder );
		HashMap<Text,String> textNodes = (HashMap<Text,String>)stFinder.results;
		
		if( setProgress ) {
			double i = progress.get() * totalNodes;
			for(Text text: textNodes.keySet() ) {
				processText( text, textNodes.get(text) );
				progress.set( i / totalNodes );
				i++;
			}
		}
		else {
			for(Text text: textNodes.keySet() ) {
				processText( text, textNodes.get(text) );
			}
		}
		if(! stFinder.symResults.isEmpty() ) {
			HashMap<R.Sym,String> symNodes = (HashMap<R.Sym,String>)stFinder.symResults; 
			for(R.Sym sym: symNodes.keySet() ) {
				// fontIn = symNodes.get(sym);
				String symChar = sym.getChar();
				int decimal = Integer.parseInt( symChar, 16 );
				char ch = (char)decimal;
				
				// create a new text node, set the value, and remove the sym
				R r = (R)sym.getParent();
				List<Object> rObjects = r.getContent();
				int size = rObjects.size();
				for( int  i=0; i<size; i++ ) {
					Object robj = rObjects.get(i);
					Object tobj = XmlUtils.unwrap(robj);
					if( tobj instanceof org.docx4j.wml.R.Sym ) {
						R.Sym iSym = (org.docx4j.wml.R.Sym)tobj;
						if( symChar.equals( iSym.getChar() ) ) {
							// an R  may have more than one R.Sym child,
							// assume we are working with the same child if they have the same value
							// since we replace the child, it shouldn't matter if the child is different
							// so long as the value is the same
							Text text = new Text();
							text.setValue( String.valueOf( ch ) );
							processText( text, symNodes.get(sym) );
							rObjects.set(i, text);
					}
					}
				}
			}
		}
	}
	

	public void processUnstyledObjects( final JaxbXmlPart<?> part, DocxUnstyledTextFinder ustFinder ) throws Docx4JException {
			HashMap<Text,String> textNodes = (HashMap<Text,String>)ustFinder.results;
			
			if( setProgress ) {
				double i = 0.0;
				for(Text text: textNodes.keySet() ) {
					processText( text, textNodes.get(text) );
					progress.set( i / totalNodes );
					i++;
				}		
			}
			else {
				for(Text text: textNodes.keySet() ) {
					processText( text, textNodes.get(text) );
				}
			}
			if(! ustFinder.symResults.isEmpty() ) {
				HashMap<R.Sym,String> symNodes = (HashMap<R.Sym,String>)ustFinder.symResults; 
				for(R.Sym sym: symNodes.keySet() ) {
					// fontIn = symNodes.get(sym);
					String symChar = sym.getChar();
					int decimal = Integer.parseInt( symChar, 16 );
					char ch = (char)decimal;
					
					// create a new text node, set the value, and remove the sym
					R r = (R)sym.getParent();
					List<Object> rObjects = r.getContent();
					int size = rObjects.size();
					for( int  i=0; i<size; i++ ) {
						Object robj = rObjects.get(i);
						Object tobj = XmlUtils.unwrap(robj);
						if( tobj instanceof org.docx4j.wml.R.Sym ) {
							R.Sym iSym = (org.docx4j.wml.R.Sym)tobj;
							if( symChar.equals( iSym.getChar() ) ) {
								// an R  may have more than one R.Sym child,
								// assume we are working with the same child if they have the same value
								// since we replace the child, it shouldn't matter if the child is different
								// so long as the value is the same
								Text text = new Text();
								text.setValue( String.valueOf( ch ) );
								processText( text, symNodes.get(sym) );
								rObjects.set(i, text);
							}
						}
					}
				}
			}
	}
	
	/*
	 * Previously this normalizeText was done by the diacrtical based converter, this works fine so long as
	 * a document is in a single font system. Since multi-font system documents have been found (diacritical mixed
	 * with non-diacrtical), we normalize the text outside of a converter.  This needs a re-think, the benefit of
	 * iterating outside of a converter is that only a single iteration is needed.  Alternatively, each converter
	 * in play could run the text node iteration individually.
	 * 
	 * We need to first "normalize" the text before processing it.  This avoids inserting lots of confusing
	 * complexity that would be needed to check for diacritical marks separated by xml elements from their
	 * bases.  This process will check if the first letter of run text is a diacritical mark, if so, then 
	 * move it to the last character of the previous run.  Thus <w:t>....b</w:t> ... <w:t>u...</w:t>
	 * becomes <w:t>...bu</w:t> ... <w:t>...</w:t> and "bu" will be converted properly to "ቡ".
	 * 
	 * There are two scenarios to check for and correct. The first is when in the Ethiopic font is specified
	 * in adjacent w:rFonts properties, and not in a named style. For example:
	 * 
	 * <w:r>
	 *  <w:rPr>
	 *    <w:rFonts w:ascii="..." w:ansi="..."/>
	 *  <w:rPr>
	 *  <w:t>b</w:t>
	 * </w:r>
	 * <w:r>
	 *  <w:rPr>
	 *    <w:rFonts w:ascii="..." w:ansi="..."/>
	 *  <w:rPr>
	 *  <w:t>u</w:t>
	 * </w:r>
	 * 
	 * The 2nd scenario is when an Ethiopic font is defined in a style, and no rFonts are present.  For example:
	 * 
	 * <w:p>
	 *   <w:pPr><w:pStyle w:val="BodyText"/><w:rPr><w:sz w:val="20"/></w:rPr></w:pPr>
	 *   <w:r>
	 *     <w:rPr>
	 *      <w:sz w:val="20"/>
	 *     <w:rPr>
	 *     <w:t>b</w:t>
	 *   </w:r>
	 *   <w:r>
	 *    <w:rPr>
	 *      <w:sz w:val="20"/>
	 *    <w:rPr>
	 *    <w:t>u</w:t>
	 *   </w:r>
	 * </w:p>
	 * 
	 */
	public void normalizeText( final JaxbXmlPart<?> part, DocxStyledTextFinder stFinder, DocxUnstyledTextFinder ustFinder ) throws Docx4JException {

		if( stFinder.hasStyles() ) {
			stFinder.clearResults();
		
			new TraversalUtil( part.getContents(), stFinder );
			// fix styled text nodes:
		
			Map<Text,String> styledText = stFinder.results;
			List<Text> styledTextOrdered = stFinder.resultsOrdered;
			int size = styledTextOrdered.size();
			for ( int i=1; i<size; i++ ) {
				Text text1 = styledTextOrdered.get(i);
				String value1 = text1.getValue();
				if( value1.length() > 0 ) {
					char firstChar = value1.charAt(0);
					String fontIn = styledText.get( text1 );
					ConvertFontSystem converter = fontToConverterMap.get( fontIn );
					if( converter instanceof ConvertFontSystemDiacriticalSystem ) {
						ConvertFontSystemDiacriticalSystem diaConverter = (ConvertFontSystemDiacriticalSystem)converter;
						if( diaConverter.isDiacritic( fontIn, String.valueOf(firstChar) ) )  {
							Text text0 = styledTextOrdered.get( i-1 );
							// check here if text0 is under the same font
							if( fontIn.equals( styledText.get( text0 ) ) ) {
								String value0 = text0.getValue();
							
								text0.setValue( value0 + firstChar );   // append to previous node as last char
								text1.setValue( value1.substring(1) );  // remove from current node
							}
						}
						else if( diaConverter.combinesWithHuletNeteb( firstChar ) ) {
							Text text0 = styledTextOrdered.get( i-1 );
							// check here if text0 is under the same font
							if( fontIn.equals( styledText.get( text0 ) ) ) {
								String value0 = text0.getValue();
								if( ( value0.length() > 0 ) && ( ( value0.charAt( value0.length() - 1) ) == diaConverter.getHuletNeteb() ) ) {
									text0.setValue( value0 + firstChar );   // append to previous node as last char
									text1.setValue( value1.substring(1) );  // remove from current node	
								}
							}
						}
				}
				}
			}
		}
		
		ustFinder.clearResults();
		new TraversalUtil( part.getContents(), ustFinder );
		
		Map<Text,String> unstyledText = ustFinder.results;
		List<Text> unstyledTextOrdered = ustFinder.resultsOrdered;
		int size = unstyledTextOrdered.size();
		for ( int i=1; i<size; i++ ) {
			Text text1 = unstyledTextOrdered.get(i);
			String value1 = text1.getValue();
			if( value1.length() > 0 ) {
				char firstChar = value1.charAt(0);
				String fontIn = unstyledText.get( text1 );
				ConvertFontSystem converter = fontToConverterMap.get( fontIn );
				if( converter instanceof ConvertFontSystemDiacriticalSystem ) {
					ConvertFontSystemDiacriticalSystem diaConverter = (ConvertFontSystemDiacriticalSystem)converter;
					if( diaConverter.isDiacritic( fontIn, String.valueOf(firstChar) ) )  {
						Text text0 = unstyledTextOrdered.get( i-1 );
						// check here if text0 is under the same font
						if( fontIn.equals( unstyledText.get( text0 ) ) ) {
							String value0 = text0.getValue();
							
							text0.setValue( value0 + firstChar );   // append to previous node as last char
							text1.setValue( value1.substring(1) );  // remove from current node
						}
					}
					else if( diaConverter.combinesWithHuletNeteb( firstChar ) ) {
						Text text0 = unstyledTextOrdered.get( i-1 );
						// check here if text0 is under the same font
						if( fontIn.equals( unstyledText.get( text0 ) ) ) {
							String value0 = text0.getValue();
							if( ( value0.length() > 0 ) && ( ( value0.charAt( value0.length() - 1) ) == diaConverter.getHuletNeteb() ) ) {
								text0.setValue( value0 + firstChar );   // append to previous node as last char
								text1.setValue( value1.substring(1) );  // remove from current node	
							}
						}
					}
				}
			}
		}
	}
	

	/*
	 * 
	public void normalizeText( final JaxbXmlPart<?> part, DocxStyledTextFinder stFinder, DocxUnstyledTextFinder ustFinder ) throws Docx4JException {
		if( stFinder.hasStyles() ) {
			stFinder.clearResults();
		
			new TraversalUtil( part.getContents(), stFinder );

		}
		
		ustFinder.clearResults();
		new TraversalUtil( part.getContents(), ustFinder );
	}
	*/

	public void process( final File inputFile, final File outputFile )
	{
		try {
			setProgress = true;
			totalNodes = 0.0;
			progress.set( 0.0 );
			Thread.sleep(100);
			WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load( inputFile );		
			MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
			
       		Map<String,Map<String,String>> styleIdToFont = DocxUtils.readStyles(wordMLPackage, targetTypefaces, fontOut);
       		DocxStyledTextFinder    stf = new DocxStyledTextFinder( styleIdToFont );
    		DocxUnstyledTextFinder ustf = new DocxUnstyledTextFinder(targetTypefaces, fontOut);
    		
    		// see: https://stackoverflow.com/questions/34357005/javafx-task-update-progress-from-a-method
    		// selectFonts( documentPart );

			normalizeText( documentPart, stf, ustf );
    		totalNodes = stf.results.size() + ustf.results.size();
            
    		/*
    		 * Normalize text will extract the styled and unstyled text nodes and store them
    		 * in the stf and ustf arrays accordingly:
    		 */

       		processUnstyledObjects( documentPart, ustf );
       		processStyledObjects( documentPart, stf );
       		setProgress = false;
            
       		if( documentPart.hasFootnotesPart() ) {
	            FootnotesPart footnotesPart = documentPart.getFootnotesPart();
				normalizeText( footnotesPart, stf, ustf );
       			processUnstyledObjects( footnotesPart, ustf );
           		processStyledObjects( footnotesPart, stf );
       		}
       		if( documentPart.hasEndnotesPart() ) {
	            EndnotesPart endnotesPart = documentPart.getEndNotesPart();
				normalizeText( endnotesPart, stf, ustf );
       			processUnstyledObjects( endnotesPart, ustf );
           		processStyledObjects( endnotesPart, stf );		
       		}
       		
    		List<SectionWrapper> sectionWrappers = wordMLPackage.getDocumentModel().getSections();
    		
    		for (SectionWrapper sw : sectionWrappers) {
    			HeaderFooterPolicy hfp = sw.getHeaderFooterPolicy();
    			
    			if( hfp.getFirstHeader() != null ) {
    				HeaderPart headerPart = hfp.getFirstHeader();
    				normalizeText( headerPart, stf, ustf );
    	       		processUnstyledObjects( headerPart, ustf );
               		processStyledObjects( headerPart, stf );  
    			}
    			if( hfp.getDefaultHeader() != null ) {
    				HeaderPart headerPart = hfp.getDefaultHeader();
    				normalizeText( headerPart, stf, ustf );
    	       		processUnstyledObjects( headerPart, ustf );
               		processStyledObjects( headerPart, stf );  
    			}
    			if( hfp.getEvenHeader() != null ) {
    				HeaderPart headerPart = hfp.getEvenHeader();
    				normalizeText( headerPart, stf, ustf );
    	       		processUnstyledObjects( headerPart, ustf );
               		processStyledObjects( headerPart, stf );  
    			}
    			

    			if ( hfp.getFirstFooter() != null ) {
    				FooterPart footerPart = hfp.getFirstFooter();
    				normalizeText( footerPart, stf, ustf );
    	       		processUnstyledObjects( footerPart, ustf );
               		processStyledObjects( footerPart, stf ); 
    			}
    			if ( hfp.getDefaultFooter() != null ) {
    				FooterPart footerPart = hfp.getDefaultFooter();
    				normalizeText( footerPart, stf, ustf );
    	       		processUnstyledObjects( footerPart, ustf );
               		processStyledObjects( footerPart, stf );
    			}
    			if ( hfp.getEvenFooter() != null ) {
    				FooterPart footerPart = hfp.getEvenFooter();
    				normalizeText( footerPart, stf, ustf );
    	       		processUnstyledObjects( footerPart, ustf );
               		processStyledObjects( footerPart, stf );
    			}
    		}
       		
       		wordMLPackage.save( outputFile );

		}
		catch ( Exception ex ) {
			System.err.println( ex );
		}
	}
	
	
	public void setTargetTypefaces(List<String> targetTypefaces) {
		this.targetTypefaces = targetTypefaces;
	}
	
	public List<String> getTargetTypefaces() {
		return  targetTypefaces;
	}
    
    public void setFontOut(String fontOut) {
    	this.fontOut = fontOut;
    }

    public DocxProcessor() {
    	super();
    }	

    public DocxProcessor(String fontOut) {
    	super();
    	setFontOut( fontOut );
    }

    public DocxProcessor( List<File> inputFileList ) {
    	super( inputFileList );
    }

    public DocxProcessor( final File inputFile, final File outputFile ) {
    	super( inputFile, outputFile );
    }
    
    public DocxProcessor( List<File> inputFileList, String fontOut ) {
    	super( inputFileList );
    	this.fontOut = fontOut;
    }

    public DocxProcessor( final File inputFile, final File outputFile, String fontOut  ) {
    	super( inputFile, outputFile );
    	this.fontOut = fontOut;
    }
    
	@Override
	public Void call() {
		process( inputFile, outputFile );
        return null;
	}
    
}
