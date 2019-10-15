package org.geez.ui.xliterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

public class ICUEditor extends CodeArea {
    private File icuFile = null;
    private String internalFileName = null;
    
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

    private String originalText = "";
    
	public static final String defaultStylesheet = "styles/icu-highlighting.css";
	public static final String userStylesheet    = "icu-highlighting.css";

    public ICUEditor() {
    	String osName = System.getProperty("os.name").toLowerCase();
        if( osName.startsWith( "mac" ) ) {
        	this.setStyle( "-fx-font-family: Kefa; -fx-font-size: 12;" );
            this.getProperties().put("font-family", "Kefa");
        }
        
        this.getProperties().put("font-size", "12");
        this.setId( "icuEditor" );
        // this.setStyle( "-fx-text-fill: white;" );
    
        setParagraphGraphicFactory( LineNumberFactory.get( this ) );

        textProperty().addListener( (obs, oldText, newText) -> {
        	if( "".equals(newText) ) {
        		return;
        	}
            this.setStyleSpans( 0, computeHighlighting(newText) );
        });
        // replaceText(0, 0, sampleCode);
        
        
    	MenuItem arrow1 = new MenuItem( "↔" );
    	MenuItem arrow2 = new MenuItem( "→" );
    	MenuItem arrow3 = new MenuItem( "←" );

        
        arrow1.setOnAction( evt -> {
        	this.insertText( this.getCaretPosition(), "↔" );
        	
        });
        arrow2.setOnAction( evt -> {
        	this.insertText( this.getCaretPosition(), "→" );
        	
        });
        arrow3.setOnAction( evt -> {
        	this.insertText( this.getCaretPosition(), "←" );
        	
        });
        
        // better would be to reference these styles by name from the icu-highlighting.css
        arrow1.setStyle( "-fx-font-weight: bold; -fx-text-fill: mediumvioletred;" );
        arrow2.setStyle( "-fx-font-weight: bold; -fx-text-fill: #008e00;" );
        arrow3.setStyle( "-fx-font-weight: bold; -fx-text-fill: orangered;" );
        
        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll( arrow1, arrow2, arrow3 );

        
        // add an input mapping that shows that context menu when you 
        //  right click somewhere in the area
        Nodes.addInputMap(this, 
                InputMap.consume(
                    EventPattern.mouseClicked(MouseButton.SECONDARY),
                    evt -> {
                        // show the area using the mouse event's screen x & y values
                        menu.show( this, evt.getScreenX(), evt.getScreenY() );
                    }
                )
        );
        
        replaceDialog = new Dialog<>();
        findDialog = new Dialog<>();
        
    }
    
    
    public static void loadStylesheets(Scene scene) {
		String userHome = System.getProperty( "user.home" );
        String osName = System.getProperty( "os.name" ).toLowerCase();
        String userXlitPath = null;
        if( osName.startsWith( "win" ) ) {
        	userXlitPath   = userHome + "/AppData/Xliterator";
        }
        else { // assume OSX or Linux/Uhix
        	userXlitPath   = userHome + "/.config/xlit";      	
        }
        try {
			File userStylesFile = new File( userXlitPath + "/" + userStylesheet );
			
			InputStream inputStream = null;
			if( userStylesFile.exists() ) {
				inputStream = new FileInputStream( userStylesFile );
				scene.getStylesheets().add( userStylesFile.toURI().toURL().toString() );
			}
			else {
				scene.getStylesheets().add( ClassLoader.getSystemResource( defaultStylesheet ).toExternalForm() );
			}
        }
        catch(IOException ex) {
        	System.out.println( ex );
        }
		
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
		String rulesFilePath = (rulesFile.contains( "/" ) ) ? rulesFile : "common/transforms/" + rulesFile ; 
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

		internalFileName = rulesFile;
		icuFile = null;
    }
    
    public void loadFile(File icuFile) throws IOException {
    	this.icuFile = icuFile;
		internalFileName = null;
    	originalText = FileUtils.readFileToString(icuFile, StandardCharsets.UTF_8);
		replaceText( originalText );
    }
    
	public String saveContentToFile() throws IOException {
		return saveContentToFile( getText() );
	}
		
	public String saveContentToFile(String newContent) throws IOException {
		  if( this.getContent() == null )
			  return null;
		  
		  FileWriter fstream = new FileWriter( icuFile );
		  BufferedWriter out = new BufferedWriter( fstream );
		  
		  out.write( newContent );
		  
		  //Close the output stream
		  out.close();
		  
		  originalText = newContent; // save a new baseline for comparison
		  
		  return icuFile.getName();
	}
		
	public String saveContentToNewFile(Stage stage) throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( "Save ICU File" );
		
		if( icuFile != null ) {
			fileChooser.setInitialFileName( icuFile.getName() );
		} else if( internalFileName != null ) {
			fileChooser.setInitialFileName( internalFileName );
		}
		File file = fileChooser.showSaveDialog( stage );
		if (file == null) {  // the user cancelled the save
			return null;
		}
		// Create file 
		FileWriter fstream = new FileWriter( file );
		BufferedWriter out = new BufferedWriter ( fstream );
		
		String content = getText(); // a check for null content was made by the EditorTab
		out.write( content );
		
		//Close the output stream
		out.close();
		
		originalText = content; // save a new baseline for comparison
		icuFile = file;
		internalFileName = null;
		
		return file.getName();
    }
       
    public boolean hasContentChanged(String newText) {
    	return (! originalText.equals(newText) );
    }
       
    public boolean isInitialSave() {
    	return (icuFile == null);
    }
    

    int pos;
    Dialog<Pair<String, String>> replaceDialog;
    Dialog<String> findDialog;
    
    public void findWord(Stage stage) {
        if (findDialog.getDialogPane().getButtonTypes().isEmpty()) {
            findDialog.initOwner(stage);
            findDialog.setTitle("Find");
            findDialog.setHeaderText(null);

            ButtonType findNext = new ButtonType("Find Next", ButtonBar.ButtonData.OK_DONE);
            findDialog.getDialogPane().getButtonTypes().addAll(findNext, ButtonType.CANCEL);

            GridPane grid = new GridPane();

            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 10, 10, 10));

            TextField word = new TextField();
            word.setStyle("-fx-pref-width: 250");
            word.setPromptText("Find a word");
            word.requestFocus();
            word.setFocusTraversable(true);

            grid.add(word, 0, 0);

            Button findNextBTN = (Button) findDialog.getDialogPane().lookupButton(findNext);
            findNextBTN.setDisable(true);

            word.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    findNextBTN.setDisable(newValue.trim().isEmpty());
                }
            });

            findDialog.getDialogPane().setContent(grid);

            findDialog.initModality(Modality.WINDOW_MODAL);

            findNextBTN.addEventFilter(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    while ((pos = getText().toUpperCase().indexOf(word.getText().toUpperCase(), pos)) >= 0) {
                        selectRange(pos, (pos + word.getText().length()));
                        pos += word.getText().length();
                        break;
                    }
                    event.consume();
                }
            });

        }
        findDialog.showAndWait();
    }


    public void replace(Stage stage) {
        if (replaceDialog.getDialogPane().getButtonTypes().isEmpty()) {
            replaceDialog.initOwner(stage);
            replaceDialog.setTitle("Replace");
            replaceDialog.setHeaderText(null);

            ButtonType replaceNext = new ButtonType("Replace Next", ButtonBar.ButtonData.OK_DONE);
            ButtonType replaceAll = new ButtonType("Replace All", ButtonBar.ButtonData.OK_DONE);
            replaceDialog.getDialogPane().getButtonTypes().addAll(replaceNext, replaceAll, ButtonType.CANCEL);

            GridPane grid = new GridPane();

            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 10, 10, 10));

            TextField word = new TextField();
            word.setStyle("-fx-pref-width: 250");
            word.setPromptText("Enter a word");
            word.requestFocus();

            TextField replaceWord = new TextField();
            replaceWord.setStyle("-fx-pref-width: 250");
            replaceWord.setPromptText("Enter a replacement");

            grid.add(word, 0, 0);
            grid.add(replaceWord, 0, 1);


            Button replaceAllBTN = (Button) replaceDialog.getDialogPane().lookupButton(replaceAll);
            replaceAllBTN.setDisable(true);

            Button replaceNextBTN = (Button) replaceDialog.getDialogPane().lookupButton(replaceNext);
            replaceNextBTN.setDisable(true);

            word.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    replaceNextBTN.setDisable(newValue.trim().isEmpty());
                    replaceAllBTN.setDisable(newValue.trim().isEmpty());
                }
            });

            replaceDialog.getDialogPane().setContent(grid);

            replaceDialog.initModality(Modality.WINDOW_MODAL);

            replaceNextBTN.addEventFilter(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    while ((pos = getText().toUpperCase().indexOf(word.getText().toUpperCase(), pos)) >= 0) {
                        selectRange(pos, (pos + word.getText().length()));
                        replaceSelection(replaceWord.getText());
                        pos += word.getText().length();
                        break;
                    }
                    event.consume();
                }
            });

            replaceAllBTN.addEventFilter(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    while ((pos = getText().toUpperCase().indexOf(word.getText().toUpperCase(), pos)) >= 0) {
                        selectRange(pos, (pos + word.getText().length()));
                        replaceSelection(replaceWord.getText());
                        pos += word.getText().length();
                    }
                    event.consume();
                }
            });

        }
        replaceDialog.showAndWait();
    }
}
