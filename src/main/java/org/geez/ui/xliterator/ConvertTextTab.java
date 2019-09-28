package org.geez.ui.xliterator;

import java.util.HashMap;
import java.util.prefs.Preferences;

import org.fxmisc.richtext.StyleClassedTextArea;
import org.geez.convert.text.ConvertTextString;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ConvertTextTab extends XliteratorTab {
	
    private final StyleClassedTextArea textAreaIn  = new StyleClassedTextArea();
    private final StyleClassedTextArea textAreaOut = new StyleClassedTextArea();
    
	private final Button convertButtonDown = new Button(); // ( "⬇" );
    private final Button convertButtonUp   = new Button(); // ( "⬆" );
    
    private final String textAreaInFontFacePref  = "org.geez.ui.xliterator.convertTextTab.textAreaIn.font.face";
    private final String textAreaInFontSizePref  = "org.geez.ui.xliterator.convertTextTab.textAreaIn.font.size";
    private final String textAreaOutFontFacePref = "org.geez.ui.xliterator.convertTextTab.textAreaOut.font.face";
    private final String textAreaOutFontSizePref = "org.geez.ui.xliterator.convertTextTab.textAreaOut.font.size";

	private ICUEditor editor = null; // used to get a handle on rules text
	
	public ConvertTextTab(String title) {
		super( title );
		setup();
	}    
    
    HashMap<String,ConvertTextString> textStringConverts = new HashMap<String,ConvertTextString>();    
    private void convertTextArea(StyleClassedTextArea textAreaIn, StyleClassedTextArea textAreaOut, String direction) {
    	String textIn = textAreaIn.getText();
    	if( textIn == null ) {
    		return;
    	}
    	
    	try {
	    	ConvertTextString stringConverter = null;
	    	if( "Use Editor".equals( selectedTransliteration ) ) {
	    		// do not save the converter because the text may change:
	    		// The ConvertTextString constructor needs to be reworked here, see notes within its source file:
	    		// stringConverter = new ConvertTextString( editor.getText(), direction, true );
	    		
	    		stringConverter = new ConvertTextString();
	    		stringConverter.setRules( editor.getText(), direction );
	    	}
	    	else {
		    	String transliterationKey = selectedTransliteration + "-" + direction ;
		    	
		    	if(! textStringConverts.containsKey( transliterationKey ) ) {
		    		textStringConverts.put( transliterationKey, new ConvertTextString( selectedTransliteration, direction ) );
		    	}
		    	stringConverter = textStringConverts.get( transliterationKey );
	    	}
	    	
	    	stringConverter.setCaseOption( caseOption );
	    	stringConverter.setText( textIn );
	    	
	    	textAreaOut.clear();
	        
	    	textAreaOut.replaceText( stringConverter.convertText( textIn ) );
	    	if( "both".equals( transliterationDirection ) ) {
	    		convertButtonUp.setDisable( false );
	    	}
		}
		catch(Exception ex) {
        	errorAlert(ex, "Translteration Defition Error. Correct to Proceed." );
			return;
		}
    }
    
    public void setEditor(ICUEditor editor) {
    	// need this when the current editor changes
    	this.editor = editor; 
    }
    
    
    public void setup() {
        textAreaIn.setPrefHeight(313);
        textAreaOut.setPrefHeight(313);
        // textAreaIn.setFont( Font.font( defaultFont, FontWeight.NORMAL, 12) );
		if(! checkPreferences() ) {
	        textAreaIn.setStyle("-fx-font-family: '" + defaultFontFamily + "'; -fx-font-size: " + defaultFontSize + ";"  );
	        textAreaIn.getProperties().put( "font-family", defaultFontFamily );
	        textAreaIn.getProperties().put( "font-size", defaultFontSize );
	        textAreaOut.setStyle("-fx-font-family: '" + defaultFontFamily + "'; -fx-font-size:" + defaultFontSize + ";"  );
	        textAreaOut.getProperties().put( "font-family", defaultFontFamily );
	        textAreaOut.getProperties().put( "font-size", defaultFontSize );
		}
        
       //  Menu textAreaInFontMenu = createFontMenu( "textAreaIn" );
        

        Button textAreaInIncreaseFontSizeButton = new Button( "+" ); 
        Button textAreaInDecreaseFontSizeButton = new Button( "-" );
        HBox textAreaInMenuBox = new HBox( createFontChoiceBox( textAreaIn ), textAreaInIncreaseFontSizeButton, textAreaInDecreaseFontSizeButton );
        textAreaInMenuBox.setPadding(new Insets(2, 2, 2, 2));
        textAreaInMenuBox.setSpacing(4);
        textAreaInIncreaseFontSizeButton.setOnAction( event -> {
        	incrementFontSize( textAreaIn );
        });
        textAreaInDecreaseFontSizeButton.setOnAction( event -> {
        	decrementFontSize( textAreaIn );
        });
        // textAreaInMenuBox.setPrefHeight( 32.0 );
        

        ClassLoader classLoader = this.getClass().getClassLoader();
        Image imageDown = new Image( classLoader.getResourceAsStream( "images/arrow-circle-down.png" ) );
        ImageView imageViewDown = new ImageView( imageDown );
        imageViewDown.setFitHeight( 18 );
        imageViewDown.setFitWidth( 18 );
        convertButtonDown.setGraphic( imageViewDown );

        // convertButtonDown.setStyle( "-fx-font-size: 24;");
        convertButtonDown.setDisable( true );
        convertButtonDown.setOnAction( event -> {
        	convertTextArea( textAreaIn, textAreaOut, "forward" ); 
        });
        
        Image imageUp = new Image( classLoader.getResourceAsStream( "images/arrow-circle-up.png" ) );
        ImageView imageViewUp = new ImageView( imageUp );
        imageViewUp.setFitHeight( 18 );
        imageViewUp.setFitWidth( 18 );
        convertButtonUp.setGraphic( imageViewUp );
        // convertButtonUp.setStyle( "-fx-font-size: 24;");
        convertButtonUp.setDisable( true );
        convertButtonUp.setOnAction( event -> {
        	convertTextArea( textAreaOut, textAreaIn, "reverse" ); 
        });
        
        Button textAreaOutIncreaseFontSizeButton = new Button( "+" ); 
        Button textAreaOutDecreaseFontSizeButton = new Button( "-" );
        textAreaOutIncreaseFontSizeButton.setOnAction( event -> {
        	incrementFontSize( textAreaOut );
        });
        textAreaOutDecreaseFontSizeButton.setOnAction( event -> {
        	decrementFontSize( textAreaOut );
        });
        Region hspacer = new Region();
        hspacer.prefWidth( 200 );
        HBox.setHgrow(hspacer, Priority.SOMETIMES);
        HBox textAreaOutMenuBox = new HBox( createFontChoiceBox( textAreaOut ), textAreaOutIncreaseFontSizeButton, textAreaOutDecreaseFontSizeButton, hspacer, convertButtonDown, convertButtonUp );
        textAreaOutMenuBox.setAlignment(Pos.CENTER_LEFT);
        textAreaOutMenuBox.setPadding(new Insets(2, 2, 2, 2));
        textAreaOutMenuBox.setSpacing( 4 );
        
        VBox textVbox = new VBox( textAreaInMenuBox, textAreaIn, textAreaOutMenuBox, textAreaOut );
        textVbox.autosize();
        this.setContent( textVbox );
        
        textAreaIn.autosize();
        textAreaOut.autosize();
        
       
        textVbox.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
            	int newHeight = Integer.parseInt(newSceneHeight.toString().split("\\.")[0] );
            	int taHeight = 312 + (newHeight-692)/2;
                textAreaIn.setPrefHeight( taHeight );
                textAreaOut.setPrefHeight( taHeight );
                // System.out.println( "Height: " + newHeight + " Area: " + taHeight + " MenuBox: " + textAreaInMenuBox.getHeight()
                // + " ButtonBox: " + hUpDownButtonBox.getHeight() );
                
            }
        });
    }
   
    public void setScriptIn(String scriptIn ) {
    	super.setScriptIn(scriptIn);
		convertButtonUp.setDisable( true );
		convertButtonDown.setDisable( true );
    }
    
    public void setScriptOut(String scriptOut ) {
    	super.setScriptOut(scriptOut);
		convertButtonUp.setDisable( true );
		convertButtonDown.setDisable( true );
    }
      
    public void setVariantOut(String variantOut, String selectedTransliteration, String transliterationDirection ) {
    	super.setVariantOut(variantOut, selectedTransliteration, transliterationDirection);
		convertButtonUp.setDisable( false );
		convertButtonDown.setDisable( false );
        if( "both".equals( transliterationDirection ) ) {
        	convertButtonUp.setDisable( false );
        }
        else {
        	convertButtonUp.setDisable( true );        	
        }
    }
    
    public void clearAll() {
    	textAreaIn.clear();
    	textAreaOut.clear();
    }
    
    public void setTextIn(String text) {
    	textAreaIn.replaceText( text );
    }
    
    public void setTextOut(String text) {
    	textAreaOut.replaceText( text );
    }

    public void enableConvertForward(boolean enable) {
    	convertButtonDown.setDisable( !enable );
    }

    public void enableConvertReverse(boolean enable) {
    	convertButtonUp.setDisable( !enable );
    }

    public void enableConvertBoth(boolean enable) {
    	convertButtonDown.setDisable( !enable );
    	convertButtonUp.setDisable( !enable );
    }

    public void saveDefaultFontSelections() {
        Preferences prefs = Preferences.userNodeForPackage( ConvertTextTab.class );
        
        String value = (String)textAreaIn.getProperties().get( "font-family" );
        if( value != null ) {
        	prefs.put( textAreaInFontFacePref,  value );
        	prefs.put( textAreaInFontSizePref,  (String)textAreaIn.getProperties().get( "font-size" ) );
        }
        
        value = (String)textAreaOut.getProperties().get( "font-family" );
        if( value != null ) {
        	prefs.put( textAreaOutFontFacePref, value );
        	prefs.put( textAreaOutFontSizePref, (String)textAreaOut.getProperties().get( "font-size" ) );
        }
    }
      
    private boolean checkPreferences() {
        Preferences prefs = Preferences.userNodeForPackage( ConvertTextTab.class );
        
        String value = prefs.get( textAreaInFontFacePref, null );
        if( value == null ) {
        	return false;
        }
        
        textAreaIn.getProperties().put( "font-family", value );
        value = prefs.get( textAreaInFontSizePref, null );
        textAreaIn.getProperties().put( "font-size", value );
        setFontSize( textAreaIn, value );
    	value = prefs.get( textAreaOutFontFacePref, null );
        textAreaOut.getProperties().put( "font-family", value );
        value = prefs.get( textAreaOutFontSizePref, null );
        textAreaOut.getProperties().put( "font-size", value );
        setFontSize( textAreaOut, value );
        return true;
    }
}
