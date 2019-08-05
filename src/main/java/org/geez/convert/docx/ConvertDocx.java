package org.geez.convert.docx;

import org.geez.convert.Converter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

import com.ibm.icu.text.Transliterator;

// StatusBar Imports:

import javafx.beans.property.ReadOnlyDoubleWrapper;



public abstract class ConvertDocx extends Converter {
	protected String fontOut = null;
	protected String fontIn = null;
	protected char huletNeteb = 0x0;
	double totalNodes = 0;
	private boolean setProgress = true;

    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper();
    
    public ConvertDocx( final File inputFile, final File outputFile ) {
    	super( inputFile, outputFile );
    }
    
	public void setFont(String fontOut) {
		this.fontOut = fontOut;
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
	
	
	protected void localCheck( Text text ) {
		return;
	}

	public String convertText( Text text ) {
		localCheck( text );
		return xlit.transliterate( text.getValue() );
	}

	public String convertText( String text ) {
		return xlit.transliterate( text );
	}
	
	public void setTargetTypefaces(List<String> targetTypefaces) {
		this.targetTypefaces = targetTypefaces;
	}
	
	public List<String> getTargetTypefaces() {
		return  targetTypefaces;
	}
	
	public void processStyledObjects( final JaxbXmlPart<?> part, StyledTextFinder stFinder ) throws Docx4JException {
		if(! stFinder.hasStyles() ) {
			return;
		}
		stFinder.clearResults();
		
		new TraversalUtil( part.getContents(), stFinder );

		HashMap<Text,String> textNodes = (HashMap<Text,String>)stFinder.results;
		
		if( setProgress ) {
			double i = progress.get() * totalNodes;
			for(Text text: textNodes.keySet() ) {
				fontIn = textNodes.get(text);
				xlit = fontToTransliteratorMap.get( fontIn );
				String out = convertText( text );
				text.setValue( out );
				progress.set( i / totalNodes );
				i++;
			}
		}
		else {
			for(Text text: textNodes.keySet() ) {
				fontIn = textNodes.get(text);
				xlit = fontToTransliteratorMap.get( fontIn );
				String out = convertText( text );
				text.setValue( out );
			}
		}
		if(! stFinder.symResults.isEmpty() ) {
			HashMap<R.Sym,String> symNodes = (HashMap<R.Sym,String>)stFinder.symResults; 
			for(R.Sym sym: symNodes.keySet() ) {
				fontIn = symNodes.get(sym);
				xlit = fontToTransliteratorMap.get( fontIn );
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
							String out = convertText( text );
							text.setValue( out );
							rObjects.set(i, text);
					}
					}
				}
			}
		}
	}
	

	public void processUnstyledObjects( final JaxbXmlPart<?> part, UnstyledTextFinder ustFinder ) throws Docx4JException {
			HashMap<Text,String> textNodes = (HashMap<Text,String>)ustFinder.results; 

			if( setProgress ) {
				double i = 0.0;
				for(Text text: textNodes.keySet() ) {
					fontIn = textNodes.get(text);
					xlit = fontToTransliteratorMap.get( fontIn );
					String out = convertText( text );
					text.setValue( out );
					progress.set( i / totalNodes );
					i++;
				}		
			}
			else {
				for(Text text: textNodes.keySet() ) {
					fontIn = textNodes.get(text);
					xlit = fontToTransliteratorMap.get( fontIn );
					String out = convertText( text );
					text.setValue( out );
				}
			}
			if(! ustFinder.symResults.isEmpty() ) {
				HashMap<R.Sym,String> symNodes = (HashMap<R.Sym,String>)ustFinder.symResults; 
				for(R.Sym sym: symNodes.keySet() ) {
					fontIn = symNodes.get(sym);
					xlit = fontToTransliteratorMap.get( fontIn );
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
								String out = convertText( text );
								text.setValue( out );
								rObjects.set(i, text);
							}
						}
					}
				}
			}
	}
	

	// make this an abstract method
	public void normalizeText( final JaxbXmlPart<?> part, StyledTextFinder stFinder, UnstyledTextFinder ustFinder ) throws Docx4JException {
		if( stFinder.hasStyles() ) {
			stFinder.clearResults();
		
			new TraversalUtil( part.getContents(), stFinder );

		}
		
		ustFinder.clearResults();
		new TraversalUtil( part.getContents(), ustFinder );
	}
	
	
	@Override
	public Void call() {
		process( inputFile, outputFile );
        return null;
	}
	public void process( final File inputFile, final File outputFile )
	{
		try {
			setProgress = true;
			totalNodes = 0.0;
			progress.set( 0.0 );
			Thread.sleep(100);
			WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load( inputFile );		
			MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
			
       		Map<String,String> styleIdToFont  = DocxUtils.readStyles(wordMLPackage, targetTypefaces, fontOut);
       		StyledTextFinder stf = new StyledTextFinder( styleIdToFont );
    		UnstyledTextFinder ustf = new UnstyledTextFinder(targetTypefaces, fontOut);
    		
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
	
    /*
	public static void main( String[] args ) {
		if( args.length != 3 ) {
			System.err.println( "Exactly 3 arguements are expected: <system> <input file> <output file>" );
			System.exit(0);
		}

		String systemIn = args[0];
		String inputFilepath  = System.getProperty("user.dir") + "/" + args[1];
		String outputFilepath = System.getProperty("user.dir") + "/" + args[2];
		File inputFile = new File ( inputFilepath );
		File outputFile = new File ( outputFilepath );


	    ConvertDocx converter = null;

		switch( systemIn ) {
			case "brana":
				converter = new ConvertDocxBrana( inputFile, outputFile );
				break;
					
			case "geezii":
				converter = new ConvertDocxFeedelGeezII( inputFile, outputFile );
				break;				
    			
		   	case "geezigna":
	    		converter = new ConvertDocxFeedelGeezigna( inputFile, outputFile );
	    		break;
	
		   	case "geezbasic":
	    		converter = new ConvertDocxGeezFont( inputFile, outputFile );
	    		break; 
		
			case "geeznewab":
				converter = new ConvertDocxFeedelGeezNewAB( inputFile, outputFile );
				break;

			case "geeztypenet":
				converter = new ConvertDocxGeezTypeNet( inputFile, outputFile );
				break;

			case "powergeez":
				converter = new ConvertDocxPowerGeez( inputFile, outputFile );
				break;
				
			case "samawerfa":
				converter = new ConvertDocxSamawerfa( inputFile, outputFile );
				break;

			case "visualgeez":
				converter = new ConvertDocxVisualGeez( inputFile, outputFile );
				break;
				
			case "visualgeez2000":
				converter = new ConvertDocxVisualGeez2000( inputFile, outputFile );
				break;
		
			default:
				System.err.println( "Unrecognized input system: " + systemIn );
				System.exit(1);
		}

		converter.process( inputFile, outputFile );
	}
	*/
}
