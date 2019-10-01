package org.geez.convert;

import org.geez.convert.docx.DocxProcessor;
import org.geez.convert.fontsystem.ConvertDocxGenericUnicodeFont;
import org.geez.convert.fontsystem.ConvertFontSystem;
import org.geez.convert.text.ConvertTextString;
import org.geez.convert.text.TextFileProcessor;

public class ProcessorManager {

	private DocxProcessor processorDocx = new DocxProcessor();
	private TextFileProcessor processorTxt = new TextFileProcessor();
	
	public ProcessorManager() { };
    
	public DocumentProcessor getFileProcessor(
			String selectedTransliteration, String transliterationDirection, String extension
			) throws Exception {
		
		DocumentProcessor processor = null;
		Converter converter = null;
	
		if ( extension.equals( "txt") ) {
			converter = new ConvertTextString( selectedTransliteration, transliterationDirection );
			processorTxt.addConverter( (ConvertTextString)converter );
			processor = processorTxt;
		}
		else {		
			converter = new ConvertDocxGenericUnicodeFont( selectedTransliteration, transliterationDirection );
			processorDocx.stashConverter( (ConvertFontSystem)converter );
			processor = processorDocx;
		}
		return processor;
	}
	
	public DocumentProcessor getFileProcessor(String extension) {
		return ( extension.equals( "txt") ) ? processorTxt : processorDocx ;
	}
	
    
	public DocumentProcessor getEditorTextProcessor(
			String selectedTransliteration, String transliterationDirection, String extension, String editorText
			) throws Exception {
		
		DocumentProcessor processor = null;
		Converter converter = null;
	
		if ( extension.equals( "txt") ) {
			converter = new ConvertTextString( editorText, "forward", true );	
			processorTxt.addConverter( (ConvertTextString)converter );
			processor = processorTxt;
		}
		else {		
			converter = new ConvertDocxGenericUnicodeFont( editorText );	
			processorDocx.stashConverter( (ConvertFontSystem)converter );
			processor = processorDocx;
		}
		
		return processor;
	}
	
}
