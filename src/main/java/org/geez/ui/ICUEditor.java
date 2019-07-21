package org.geez.ui;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ICUEditor extends CodeArea {
    private static final String[] KEYWORDS = new String[] {
    		"NFC", "NFD",
    		"M", "N"
    };
    
    private static final Pattern XML_TAG = Pattern.compile(
    		"(?<ELEMENT>(</?\\h*)(\\w+)([^<>]*)(\\h*/?>))"
    		+ "|(?<COMMENT><!--[^<>]+-->)"
    		+ "|(?<CDATA>(<!\\[CDATA\\[)(.+)(\\]\\]>))",  Pattern.DOTALL)
    ;
    
    private static final Pattern CDATA_PATTERN = Pattern.compile(
    		"(<!\\[CDATA\\[\\h*)(.*)(\\h*\\]\\]>)",  Pattern.DOTALL)
    ;
   
    private static final Pattern ICU = Pattern.compile(
    		"(?<VARIABLE>(\\$\\w+)\\h*(=)\\h*([^;]+);)"
    		+ "|(?<COMMENT>#.*\n)" 
    		+ "|(?<DIRECTIVE>(::)(.*)(;))"
    		+ "|(?<ID>(\\[:)(\\w+)(:\\]))"
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
    
    private static final int GROUP_ICU_VARIABLE  = 1;
    private static final int GROUP_ICU_COMMENT  = 2;
    private static final int GROUP_ICU_DIRECTIVE_SYMBOL = 7;
    private static final int GROUP_ICU_DIRECTIVE_TERM = 8;
    private static final int GROUP_ICU_DIRECTIVE_END = 9;
    private static final int GROUP_ICU_ID_OPEN = 11;
    private static final int GROUP_ICU_ID_TERM = 12;
    private static final int GROUP_ICU_ID_CLOSE = 13;
    
    // TODO: When the editor opens, have an example ICU transliteration loaded that the user
    // can experiment with and replace with their own.  Read from a resource file
    // The tab label can use the filename.
    // The "Scripts" menu should have a section list the tab labels as input sources

    private static final String sampleCode = String.join("\n", new String[] {
    		"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
    		"<!-- Sample XML -->",
    		"< orders >",
    		"	<Order number=\"1\" table=\"center\">",
    		"		<items>",
    		"			<Item>",
    		"				<type>ESPRESSO</type>",
    		"				<shots>2</shots>",
    		"				<iced>false</iced>",
    		"				<orderNumber>1</orderNumber>",
    		// "               <hello><![CDATA[ Hello World ]]></hello>",
    		"               <![CDATA[",
    		":: [ሀ-᎙] ;",
    		":: NFD (NFC) ;",
"",
"",
    		"$ejective = ’;",
    		"$glottal  = ’;",
    		"$pharyngeal = ‘;",
"",
"",
    		"# Use this $wordBoundary until bug 2034 is fixed in ICU:", 
    		"# http://bugs.icu-project.org/trac/ticket/2034",
    		"$wordBoundary =  [^[:L:][:M:][:N:]] ;",
"",
"",
    		"########################################################################",
    		"# Start of Syllabic Transformations",
    		"########################################################################",
"",
    		"ሀ → hā ; # ETHIOPIC SYLLABLE HA",
    		"ሁ → hu ; # ETHIOPIC SYLLABLE HU",
    		"ሂ → hī ; # ETHIOPIC SYLLABLE HI",
    		"ሃ → ha ; # ETHIOPIC SYLLABLE HAA",
    		"ሄ → hē ; # ETHIOPIC SYLLABLE HEE",
    		"ህ → hi ; # ETHIOPIC SYLLABLE HE",
    		"ሆ → ho ; # ETHIOPIC SYLLABLE HO",
    		"               ]]>",
    		"			</Item>",
    		"			<Item>",
    		"				<type>CAPPUCCINO</type>",
    		"				<shots>1</shots>",
    		"				<iced>false</iced>",
    		"				<orderNumber>1</orderNumber>",
    		"			</Item>",
    		"			<Item>",
    		"			<type>LATTE</type>",
    		"				<shots>2</shots>",
    		"				<iced>false</iced>",
    		"				<orderNumber>1</orderNumber>",
    		"			</Item>",
    		"			<Item>",
    		"				<type>MOCHA</type>",
    		"				<shots>3</shots>",
    		"				<iced>true</iced>",
    		"				<orderNumber>1</orderNumber>",
    		"			</Item>",
    		"		</items>",
    		"	</Order>",
    		"</orders>"
    		});

    public ICUEditor() {
    	String osName = System.getProperty("os.name");
        if( osName.equals("Mac OS X") ) {
        	this.setStyle( "-fx-font-family: Kefa" );
        }
    
        setParagraphGraphicFactory( LineNumberFactory.get( this ) );

        textProperty().addListener((obs, oldText, newText) -> {
            this.setStyleSpans( 0, computeHighlighting(newText) );
        });
        replaceText(0, 0, sampleCode);

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
    	
        Matcher matcher = XML_TAG.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while(matcher.find()) {
        	
        	spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
        	if(matcher.group("COMMENT") != null) {
        		spansBuilder.add(Collections.singleton("comment"), matcher.end() - matcher.start());
        	}
        	else if(matcher.group("CDATA") != null) {
        		Matcher cMatcher = CDATA_PATTERN.matcher( matcher.group(0) );
        		cMatcher.find();

        		lastKwEnd = 0;
        		spansBuilder.add(Collections.emptyList(), cMatcher.start() - lastKwEnd);
        		spansBuilder.add(Collections.singleton("cdata"), cMatcher.end(GROUP_CDATA_OPEN_BRACKET) - cMatcher.start(GROUP_CDATA_OPEN_BRACKET));        		
        		
        		// String data = cMatcher.group(GROUP_CDATA_CONTENT);
        		Matcher iMatcher = ICU.matcher( cMatcher.group(GROUP_CDATA_CONTENT) );
        		lastKwEnd = 0;
        		while( iMatcher.find() ) {
        			// data = iMatcher.group(GROUP_ICU_COMMENT);
   				 	spansBuilder.add( Collections.emptyList(), iMatcher.start() - lastKwEnd);
        			if(iMatcher.group("VARIABLE") != null) {
        				 spansBuilder.add( Collections.singleton("variable"), iMatcher.end() - iMatcher.start() );
        			}
        			else if(iMatcher.group("COMMENT") != null) {
                		spansBuilder.add( Collections.singleton("comment"), iMatcher.end() - iMatcher.start() );
        			}
        			else if(iMatcher.group("DIRECTIVE") != null) {
                		spansBuilder.add( Collections.singleton("paren"), iMatcher.end(GROUP_ICU_DIRECTIVE_SYMBOL) - iMatcher.start(GROUP_ICU_DIRECTIVE_SYMBOL) );
                		spansBuilder.add( Collections.singleton("directive"), iMatcher.end(GROUP_ICU_DIRECTIVE_TERM) - iMatcher.start(GROUP_ICU_DIRECTIVE_TERM) );
                		spansBuilder.add( Collections.singleton("paren"), iMatcher.end(GROUP_ICU_DIRECTIVE_END) - iMatcher.start(GROUP_ICU_DIRECTIVE_END) );

        			}
        			else if(iMatcher.group("ID") != null) {
        				System.out.println( "ID: " + iMatcher.groupCount() );
        				System.out.println( "ID: "
        				+ iMatcher.group(9) + " / "
        				+ iMatcher.group(10) + " / "
        				+ iMatcher.group(11) + " / "
        				+ iMatcher.group(12) + " / "
        				);
                		spansBuilder.add( Collections.singleton("paren"), iMatcher.end(GROUP_ICU_ID_OPEN) - iMatcher.start(GROUP_ICU_ID_OPEN) );
                		spansBuilder.add( Collections.singleton("identifier"), iMatcher.end(GROUP_ICU_ID_TERM) - iMatcher.start(GROUP_ICU_ID_TERM) );
                		spansBuilder.add( Collections.singleton("paren"), iMatcher.end(GROUP_ICU_ID_CLOSE) - iMatcher.start(GROUP_ICU_ID_CLOSE) );
        			}
					lastKwEnd = iMatcher.end();
        		}		
        		
        		spansBuilder.add(Collections.singleton("cdata"), cMatcher.end(GROUP_CDATA_CLOSE_BRACKET) - cMatcher.start(GROUP_CDATA_CLOSE_BRACKET));
        		
    			lastKwEnd = cMatcher.end(GROUP_CDATA_CLOSE_BRACKET);
        		// lastKwEnd = cMatcher.end();
    		}
        	else if(matcher.group("ELEMENT") != null) {
        			
        			String attributesText = matcher.group(GROUP_ATTRIBUTES_SECTION);
        			
        			spansBuilder.add(Collections.singleton("tagmark"), matcher.end(GROUP_OPEN_BRACKET) - matcher.start(GROUP_OPEN_BRACKET));
        			spansBuilder.add(Collections.singleton("anytag"), matcher.end(GROUP_ELEMENT_NAME) - matcher.end(GROUP_OPEN_BRACKET));

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
        				if(attributesText.length() > lastKwEnd)
        					spansBuilder.add(Collections.emptyList(), attributesText.length() - lastKwEnd);
        			}

        			lastKwEnd = matcher.end(GROUP_ATTRIBUTES_SECTION);
        			
        			spansBuilder.add(Collections.singleton("tagmark"), matcher.end(GROUP_CLOSE_BRACKET) - lastKwEnd);
        		}
            lastKwEnd = matcher.end();
        }
       
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
