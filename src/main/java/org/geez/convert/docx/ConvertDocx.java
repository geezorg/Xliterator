package org.geez.convert.docx;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;

/*
 * The non-maven way to build the jar file:
 *
 * javac -Xlint:deprecation -cp docx4j-6.0.1.jar:dependencies/commons-io-2.5.jar:../icu4j-63_1.jar:dependencies/slf4j-api-1.7.25.jar:slf4j-1.7.25 *.java
 * jar -cvf convert.jar org/geez/convert/docx/*.class org/geez/convert/tables/
 * java -cp convert.jar:docx4j-6.0.1.jar:dependencies/*:../icu4j-63_1.jar:slf4j-1.7.25/slf4j-nop-1.7.25.jar org.geez.convert.docx.ConvertDocx brana myFile-In.docx myFile-Out.docx
 *
 */


abstract class ConvertDocx  implements Callable<Void> {
	protected Transliterator t = null;
	protected String fontOut = null;
	protected String fontIn = null;
	protected char huletNeteb = 0x0;
	double totalNodes = 0;
	private boolean setProgress = true;

    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper();

    private File inputFile = null, outputFile = null;
    
    public ConvertDocx( final File inputFile, final File outputFile ) {
    	this.inputFile  = inputFile;
    	this.outputFile = outputFile;
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty() ;
    }   
    
    public final double getProgress() {
        return progressProperty().get();
    }
    
	public void setFont(String fontOut) {
		this.fontOut = fontOut;
	}
	
	public String readRules( String fileName ) throws IOException {
		String line, segment, rules = "";

		ClassLoader classLoader = this.getClass().getClassLoader();
		InputStream in = classLoader.getResourceAsStream( "tables/" + fileName ); 
		BufferedReader ruleFile = new BufferedReader( new InputStreamReader(in, "UTF-8") );
		while ( (line = ruleFile.readLine()) != null) {
			if ( line.trim().equals("") || line.charAt(0) == '#' ) {
				continue;
			}
			segment = line.replaceFirst ( "^(.*?)#(.*)$", "$1" );
			rules += ( segment == null ) ? line : segment;
		}
		ruleFile.close();
		return rules;
	}


	protected Transliterator translit1 = null;
	protected Transliterator translit2 = null;
	protected String fontName1 = null;
	protected String fontName2 = null;
	protected List<String> targetTypefaces = new  ArrayList<String>();
	protected Map<String,Transliterator> fontToTransliteratorMap = new HashMap<String,Transliterator>();

	public void initialize(
		final String table1RulesFile,
		final String table2RulesFile,
		final String fontName1,
		final String fontName2)
	{
		try {
			// specify the transliteration file in the first argument.
			// read the input, transliterate, and write to output
			String table1Text = readRules( table1RulesFile  );
			String table2Text = readRules( table2RulesFile );

			translit1 = Transliterator.createFromRules( "Ethiopic-ExtendedLatin", table1Text.replace( '\ufeff', ' ' ), Transliterator.REVERSE );
			translit2 = Transliterator.createFromRules( "Ethiopic-ExtendedLatin", table2Text.replace( '\ufeff', ' ' ), Transliterator.REVERSE );
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
	
	protected void localCheck( Text text ) {
		return;
	}

	public String convertText( Text text ) {
		localCheck( text );
		return t.transliterate( text.getValue() );
	}

	public String convertText( String text ) {
		return t.transliterate( text );
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
				t = fontToTransliteratorMap.get( fontIn );
				String out = convertText( text );
				text.setValue( out );
				progress.set( i / totalNodes );
				i++;
			}
		}
		else {
			for(Text text: textNodes.keySet() ) {
				fontIn = textNodes.get(text);
				t = fontToTransliteratorMap.get( fontIn );
				String out = convertText( text );
				text.setValue( out );
			}
		}
		if(! stFinder.symResults.isEmpty() ) {
			HashMap<R.Sym,String> symNodes = (HashMap<R.Sym,String>)stFinder.symResults; 
			for(R.Sym sym: symNodes.keySet() ) {
				fontIn = symNodes.get(sym);
				t = fontToTransliteratorMap.get( fontIn );
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
					t = fontToTransliteratorMap.get( fontIn );
					String out = convertText( text );
					text.setValue( out );
					progress.set( i / totalNodes );
					i++;
				}		
			}
			else {
				for(Text text: textNodes.keySet() ) {
					fontIn = textNodes.get(text);
					t = fontToTransliteratorMap.get( fontIn );
					String out = convertText( text );
					text.setValue( out );
				}
			}
			if(! ustFinder.symResults.isEmpty() ) {
				HashMap<R.Sym,String> symNodes = (HashMap<R.Sym,String>)ustFinder.symResults; 
				for(R.Sym sym: symNodes.keySet() ) {
					fontIn = symNodes.get(sym);
					t = fontToTransliteratorMap.get( fontIn );
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
	    /*
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
	    		converter = new ConvertDocxGeezBasic( inputFile, outputFile );
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
		*/
		converter.process( inputFile, outputFile );
	}
}
