package org.geez.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import javafx.scene.Scene;

public class ICUEditor extends CodeArea {
    private static final String[] KEYWORDS = new String[] {
    		"NFC", "NFD",
    		"M", "N"
    };
    
    private static final Pattern XML_TAG_WITH_CDATA = Pattern.compile(
    		"(?<ELEMENT>(</?\\h*)(\\w+)([^<>]*)(\\h*/?>))"
    		+ "|(?<COMMENT><!--[^<>]+-->)"
    		+ "|(?<CDATA>(<!\\[CDATA\\[)(.+)(\\]\\]>))",  Pattern.DOTALL)
    ;
    private static final Pattern XML_TAG_WITHOUT_CDATA = Pattern.compile(
    		// "(?<TRULE>(<tRule>)(.*?)(</tRule>))"
    		"(?<ELEMENT>(</?\\h*)(\\w+)([^<>]*)(\\h*/?>))"
    		+ "|(?<COMMENT><!--[^<>]+-->)",  Pattern.DOTALL)
    ;
    
    private static final Pattern CDATA_PATTERN = Pattern.compile(
    		"(<!\\[CDATA\\[\\h*)(.*)(\\h*\\]\\]>)",  Pattern.DOTALL)
    ;
   
    private static final Pattern ICU = Pattern.compile(
    		"(?<VARIABLE>(\\$\\w+)\\h*(=)\\h*([^;]+);)"
    		+ "|(?<COMMENT>#.*\n)" 
    		+ "|(?<DIRECTIVE>(::)(.*)(;))"
    		+ "|(?<ID>(\\[:)(\\w+)(:\\]))"
    		+ "|(?<FORWARDARROW>(→))"
    		+ "|(?<REVERSEARROW>(←))"
    		+ "|(?<BOTHARROW>(↔))"
    		+ "|(?<THING>\\$\\w+)"
    		)
    ;

    private static final Pattern ATTRIBUTES = Pattern.compile("(\\w+\\h*)(=)(\\h*\"[^\"]+\")");
    
    private static final int GROUP_OPEN_BRACKET = 2;
    private static final int GROUP_ELEMENT_NAME = 3;
    private static final int GROUP_ATTRIBUTES_SECTION = 4;
    private static final int GROUP_CLOSE_BRACKET = 5;
    private static final int GROUP_ATTRIBUTE_NAME = 1;
    private static final int GROUP_EQUAL_SYMBOL = 2;
    private static final int GROUP_ATTRIBUTE_VALUE = 3;
    
    private static final int GROUP_CDATA_OPEN_BRACKET  = 1;
    private static final int GROUP_CDATA_CONTENT  = 2;
    private static final int GROUP_CDATA_CLOSE_BRACKET = 3;
    
    private static final int GROUP_ICU_VARIABLE = 1;
    private static final int GROUP_ICU_COMMENT  = 2;
    private static final int GROUP_ICU_DIRECTIVE_SYMBOL = 7;
    private static final int GROUP_ICU_DIRECTIVE_TERM = 8;
    private static final int GROUP_ICU_DIRECTIVE_END = 9;
    private static final int GROUP_ICU_ID_OPEN = 11;
    private static final int GROUP_ICU_ID_TERM = 12;
    private static final int GROUP_ICU_ID_CLOSE = 13;
    
    private static final int GROUP_TRULE_OPEN     = 2;
    private static final int GROUP_TRULE_CONTENT  = 3;
    private static final int GROUP_TRULE_CLOSE    = 4;

    private String originalText = null;

    public ICUEditor() {
    	String osName = System.getProperty("os.name");
        if( osName.equals("Mac OS X") ) {
        	this.setStyle( "-fx-font-family: Kefa; -fx-font-size: 12;" );
            this.getProperties().put("font-family", "Kefa");
        }
        this.getProperties().put("font-size", "12");
    
        setParagraphGraphicFactory( LineNumberFactory.get( this ) );

        textProperty().addListener( (obs, oldText, newText) -> {
            this.setStyleSpans( 0, computeHighlighting(newText) );
        });
        // replaceText(0, 0, sampleCode);
    }
    
    public void setStyle(Scene scene) {
    	// review this issues:
    	// 1) if we add the stylesheet to the scene, does it apply for all tabs?  - seems OK
    	// 2) Can tabs have their own scene?
    	// 3) can we create a tab with a StackPane as shown in the scene?
    	//  Find out how to add a scrollbar to a TextArea / CodeArea / Tab
    	//
		// Scene scenex = new Scene(new StackPane( new VirtualizedScrollPane<>(this) ), 600, 400);
    	
		ClassLoader classLoader = this.getClass().getClassLoader();
        scene.getStylesheets().add( classLoader.getResource("styles/icu-highlighting.css").toExternalForm()  );
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
    	if( ! text.contains( "<?xml " ) ) {
    		return computeHighlightingPlainText( text ); 
    	}
    	else if( text.contains( "<![CDATA[" ) ) {
    		return computeHighlightingWithCDATA( text );
    	}
    	
    	//  Assume a <tRule> is present w/o CDATA
    		
    	return computeHighlightingWithoutCDATA( text );
    }
    
    private static StyleSpans<Collection<String>> computeHighlightingWithCDATA(String text) {
        Matcher matcher = XML_TAG_WITH_CDATA.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while(matcher.find()) {	
        	spansBuilder.add( Collections.emptyList(), matcher.start() - lastKwEnd );
        	if(matcher.group( "COMMENT" ) != null) {
        		spansBuilder.add( Collections.singleton("comment"), matcher.end() - matcher.start() );
        	}
        	else if(matcher.group("CDATA") != null) {
        		Matcher cMatcher = CDATA_PATTERN.matcher( matcher.group(0) );
        		cMatcher.find();

        		lastKwEnd = 0;
        		spansBuilder.add( Collections.emptyList(), cMatcher.start() - lastKwEnd );
        		spansBuilder.add( Collections.singleton("cdata"), cMatcher.end(GROUP_CDATA_OPEN_BRACKET) - cMatcher.start(GROUP_CDATA_OPEN_BRACKET) );   
        		
        		String cdataContent = cMatcher.group(GROUP_CDATA_CONTENT);
        		
        		if(! cdataContent.isEmpty() ) {
        			lastKwEnd = highlightIcuText( spansBuilder, cdataContent );
        		}
        		
    			lastKwEnd = cMatcher.end(GROUP_CDATA_CLOSE_BRACKET);
    			
        		spansBuilder.add( Collections.singleton("cdata"), lastKwEnd - cMatcher.start(GROUP_CDATA_CLOSE_BRACKET) );
    		}
        	else if( matcher.group("ELEMENT") != null ) {
        			
        			String attributesText = matcher.group(GROUP_ATTRIBUTES_SECTION);
        			
        			spansBuilder.add(Collections.singleton("tagmark"), matcher.end(GROUP_OPEN_BRACKET) - matcher.start(GROUP_OPEN_BRACKET));
        			spansBuilder.add(Collections.singleton("anytag"),  matcher.end(GROUP_ELEMENT_NAME) - matcher.end(GROUP_OPEN_BRACKET));

        			if(!attributesText.isEmpty()) {
        				
        				lastKwEnd = 0;
        				
        				Matcher amatcher = ATTRIBUTES.matcher(attributesText);
        				while(amatcher.find()) {
        					spansBuilder.add(Collections.emptyList(), amatcher.start() - lastKwEnd);
        					spansBuilder.add(Collections.singleton("attribute"), amatcher.end(GROUP_ATTRIBUTE_NAME) - amatcher.start(GROUP_ATTRIBUTE_NAME));
        					spansBuilder.add(Collections.singleton("tagmark"), amatcher.end(GROUP_EQUAL_SYMBOL) - amatcher.end(GROUP_ATTRIBUTE_NAME));
        					spansBuilder.add(Collections.singleton("avalue"), amatcher.end(GROUP_ATTRIBUTE_VALUE) - amatcher.end(GROUP_EQUAL_SYMBOL));
        					lastKwEnd = amatcher.end();
        				}
        				if(attributesText.length() > lastKwEnd) {
        					spansBuilder.add(Collections.emptyList(), attributesText.length() - lastKwEnd);
        				}
        			}

        			lastKwEnd = matcher.end(GROUP_ATTRIBUTES_SECTION);
        			
        			spansBuilder.add(Collections.singleton("tagmark"), matcher.end(GROUP_CLOSE_BRACKET) - lastKwEnd);
        		}
            lastKwEnd = matcher.end();
        }
       
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
    
    
    private static StyleSpans<Collection<String>> computeHighlightingWithoutCDATA(String text) {
        Matcher matcher = XML_TAG_WITHOUT_CDATA.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        
        boolean open = true;
        int tRuleDelta;
        while(matcher.find()) {	
        	spansBuilder.add( Collections.emptyList(), matcher.start() - lastKwEnd );
        	tRuleDelta = 0;
        	
        	if(matcher.group( "COMMENT" ) != null) {
        		spansBuilder.add( Collections.singleton("comment"), matcher.end() - matcher.start()) ;
        	}
        	else if( matcher.group("ELEMENT") != null ) {
        			
        			String attributesText = matcher.group(GROUP_ATTRIBUTES_SECTION);
        			String element = matcher.group(GROUP_ELEMENT_NAME);
        			
        			spansBuilder.add( Collections.singleton("tagmark"), matcher.end(GROUP_OPEN_BRACKET) - matcher.start(GROUP_OPEN_BRACKET) );
        			//
        			// if elementName = "tRule", extract the text between GROUP_OPEN_BRACKET and GROUP_ELEMENT_NAME and send to the ICU highlighter
        			// or does this text get highlighted in the attributesText.length() > lastKwEnd) section
        			// between lastKwEnd and attributesText.length ?

        			if( "tRule".equals( element ) && open ) { 
        				spansBuilder.add( Collections.singleton("anytag"), matcher.end(GROUP_ELEMENT_NAME) - matcher.end(GROUP_OPEN_BRACKET) );  				
            			spansBuilder.add(Collections.singleton("tagmark"), matcher.end(GROUP_CLOSE_BRACKET) - matcher.start(GROUP_CLOSE_BRACKET));
            			
        				String test = text.substring( matcher.end(GROUP_CLOSE_BRACKET), text.length() );
        				String icuText = test.substring( 0, test.indexOf( "</tRule>" ) );
        				highlightIcuText( spansBuilder, icuText );
        				tRuleDelta = icuText.length();
        				open = false;
        			}
        			else {
        				if( "tRule".equals( element ) ) { open = true; }
        				spansBuilder.add( Collections.singleton("anytag"), matcher.end(GROUP_ELEMENT_NAME) - matcher.end(GROUP_OPEN_BRACKET) );
        			

	        			if( (attributesText != null) && !attributesText.isEmpty() ) {
	        				
	        				lastKwEnd = 0;
	        				
	        				Matcher amatcher = ATTRIBUTES.matcher(attributesText);
	        				while(amatcher.find()) {
	        					spansBuilder.add(Collections.emptyList(), amatcher.start() - lastKwEnd);
	        					spansBuilder.add(Collections.singleton("attribute"), amatcher.end(GROUP_ATTRIBUTE_NAME) - amatcher.start(GROUP_ATTRIBUTE_NAME));
	        					spansBuilder.add(Collections.singleton("tagmark"), amatcher.end(GROUP_EQUAL_SYMBOL) - amatcher.end(GROUP_ATTRIBUTE_NAME));
	        					spansBuilder.add(Collections.singleton("avalue"), amatcher.end(GROUP_ATTRIBUTE_VALUE) - amatcher.end(GROUP_EQUAL_SYMBOL));
	        					lastKwEnd = amatcher.end();
	        				}
	        				if(attributesText.length() > lastKwEnd)
	        					spansBuilder.add(Collections.emptyList(), attributesText.length() - lastKwEnd);
	        				// probably highlight the ICU text here
	        			}
	
	        			lastKwEnd = matcher.end(GROUP_ATTRIBUTES_SECTION);
	        			
	        			spansBuilder.add(Collections.singleton("tagmark"), matcher.end(GROUP_CLOSE_BRACKET) - lastKwEnd);
        			}
        		}
              lastKwEnd = matcher.end() + tRuleDelta;
        }
       
        // spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
    
    
    private static StyleSpans<Collection<String>> computeHighlightingPlainText(String text) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		highlightIcuText( spansBuilder, text );
        return spansBuilder.create();
    }
    
    private static int highlightIcuText(StyleSpansBuilder<Collection<String>> spansBuilder, String text) {

        		Matcher iMatcher = ICU.matcher( text );

        		int lastKwEnd = 0;
        		while( iMatcher.find() ) {
   				 	spansBuilder.add( Collections.emptyList(), iMatcher.start() - lastKwEnd);
        			if( (iMatcher.group("VARIABLE") != null) /* || (iMatcher.group("XARIABLE") != null) */ ) {
        				 spansBuilder.add( Collections.singleton("variable"), iMatcher.end() - iMatcher.start() );
        			}
        			else if( (iMatcher.group("THING") != null) ) {
       				 	spansBuilder.add( Collections.singleton("variable"), iMatcher.end() - iMatcher.start() );
        			}
        			else if(iMatcher.group("COMMENT") != null) {
                		spansBuilder.add( Collections.singleton("comment"), iMatcher.end() - iMatcher.start() );
        			}
        			else if(iMatcher.group("DIRECTIVE") != null) {
                		spansBuilder.add( Collections.singleton("paren"),     iMatcher.end(GROUP_ICU_DIRECTIVE_SYMBOL) - iMatcher.start(GROUP_ICU_DIRECTIVE_SYMBOL) );
                		spansBuilder.add( Collections.singleton("directive"), iMatcher.end(GROUP_ICU_DIRECTIVE_TERM)   - iMatcher.start(GROUP_ICU_DIRECTIVE_TERM) );
                		spansBuilder.add( Collections.singleton("paren"),     iMatcher.end(GROUP_ICU_DIRECTIVE_END)    - iMatcher.start(GROUP_ICU_DIRECTIVE_END) );
        			}
        			else if(iMatcher.group("ID") != null) {
        				/* System.out.println( "ID: " + iMatcher.groupCount() );
        				System.out.println( "ID: "
        				+ iMatcher.group(9) + " / "
        				+ iMatcher.group(10) + " / "
        				+ iMatcher.group(11) + " / "
        				+ iMatcher.group(12) + " / "
        				); */
                		spansBuilder.add( Collections.singleton("paren"),      iMatcher.end(GROUP_ICU_ID_OPEN)  - iMatcher.start(GROUP_ICU_ID_OPEN) );
                		spansBuilder.add( Collections.singleton("identifier"), iMatcher.end(GROUP_ICU_ID_TERM)  - iMatcher.start(GROUP_ICU_ID_TERM) );
                		spansBuilder.add( Collections.singleton("paren"),      iMatcher.end(GROUP_ICU_ID_CLOSE) - iMatcher.start(GROUP_ICU_ID_CLOSE) );
        			}
        			else if(iMatcher.group("FORWARDARROW") != null) {
                		spansBuilder.add( Collections.singleton("forwardArrow"), iMatcher.end() - iMatcher.start() );
        			}
        			else if(iMatcher.group("REVERSEARROW") != null) {
                		spansBuilder.add( Collections.singleton("reverseArrow"), iMatcher.end() - iMatcher.start() );
        			}
        			else if(iMatcher.group("BOTHARROW") != null) {
                		spansBuilder.add( Collections.singleton("bothArrow"), iMatcher.end() - iMatcher.start() );
        			}
					lastKwEnd = iMatcher.end();
        		}
				if(text.length() > lastKwEnd) {
					spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
				}


    		return lastKwEnd;
    }
    
    public void loadResourceFile(String rulesFile) throws UnsupportedEncodingException, IOException {
		ClassLoader classLoader = this.getClass().getClassLoader();
		String rulesFilePath = "common/transforms/" + rulesFile ; 
		InputStream inputStream = classLoader.getResourceAsStream( rulesFilePath ); 
		
		BufferedReader br = new BufferedReader( new InputStreamReader(inputStream, "UTF-8") );
		StringBuffer sb = new StringBuffer();
		String line = null;

		while ( (line = br.readLine()) != null) {
			sb.append( line + "\n" );
		}
		br.close();
		originalText = sb.toString();
		replaceText( originalText  );

    }
    
    public void loadFile(File icuFile) throws IOException {
    	originalText = FileUtils.readFileToString(icuFile, StandardCharsets.UTF_8);
		replaceText( originalText  );
    }
    
    
    public boolean hasContentChanged(String newText) {
    	return (! originalText.equals(newText) );
    }
}
