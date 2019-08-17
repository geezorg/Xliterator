package org.geez.convert.text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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


public class TextFileProcessor extends DocumentProcessor {
	
	protected double totalNodes = 0;
	
	protected List<String> targetTypefaces = new  ArrayList<String>();
	protected Map<String,ConvertFontSystem> fontToConverterMap = new HashMap<String,ConvertFontSystem>();
	protected String fontOut = null;
	ConvertTextString converter = null;
	
	public void addConverter(ConvertTextString converter) {
    	this.converter = converter;
	}
    
	/*
	private void processText(Text text, String fontIn) {
		ConvertFontSystem converter = fontToConverterMap.get( fontIn );
		if( converter.isSpacePreservableSymbol( text.getValue() ) ) {
			text.setSpace( "preserve" );
		}
		String out = converter.convertText( text.getValue(), fontIn );
		text.setValue( out );
	}
	*/
	

    public TextFileProcessor() {
    	super();
    }	

    public TextFileProcessor( List<File> inputFileList ) {
    	super( inputFileList );
    }

    public TextFileProcessor( final File inputFile, final File outputFile ) {
    	super( inputFile, outputFile );
    }
    
    public TextFileProcessor( List<File> inputFileList, String fontOut ) {
    	super( inputFileList );
    	this.fontOut = fontOut;
    }

    public TextFileProcessor( final File inputFile, final File outputFile, ConvertTextString converter ) {
    	super( inputFile, outputFile );
    	this.converter = converter;
    }

    public void process() {
    	int size = inputFileList.size();
    	
		try {
			setProgress = true;
			totalNodes = 0.0;
			progress.set( 0.0 );
	    	for( int i=0; i<size; i++ ) {
	    			File inputFile = inputFileList.get(i);
	            	String inputFilePath = inputFile.getPath();
	            	String outputFilePath = inputFilePath.replaceAll("\\.docx", "-" + "Out.docx");
	            	outputFile =  new File ( outputFilePath );

	    	        Stream<String> lines = Files.lines(Paths.get( inputFile.getPath()) );

	                String content = lines.collect( Collectors.joining(System.lineSeparator()) );
	                lines.close();
	                
	                converter.convertText( content );
	                BufferedWriter writer = new BufferedWriter( new FileWriter(outputFile) );
	                writer.write( content );
	    			progress.set( (i+1)/size );
	                
	                writer.close();
	    			Thread.sleep(100);   
	    	}
		}
		catch(Exception ex) {
			System.err.println( ex );
		}
    }
    
    
	public void process( final File inputFile, final File outputFile )
	{
		try {
			setProgress = true;
			totalNodes = 0.0;
			progress.set( 0.0 );

	        Stream<String> lines = Files.lines(Paths.get( inputFile.getPath()) );

            // Formatting like \r\n will be lost
            // String content = lines.collect(Collectors.joining());

            // UNIX \n, WIndows \r\n
            String content = lines.collect( Collectors.joining(System.lineSeparator()) );
            lines.close();
            
            converter.convertText( content );
            BufferedWriter writer = new BufferedWriter( new FileWriter(outputFile) );
            writer.write( content );
			progress.set( 1.0 );
             
            writer.close();
            // Thread.sleep(100);
		       
		}
		catch(Exception ex) {
			System.err.println( ex );
		}
	}
    
	@Override
	public Void call() {
		process( inputFile, outputFile );
        return null;
	}
    
}
