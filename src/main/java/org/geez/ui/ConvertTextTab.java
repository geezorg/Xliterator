package org.geez.ui;

import java.util.HashMap;

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
	
    private final StyleClassedTextArea textAreaIn = new StyleClassedTextArea();
    private final StyleClassedTextArea textAreaOut = new StyleClassedTextArea();
	private final Button convertButtonDown = new Button(); // ( "⬇" );
    private final Button convertButtonUp = new Button(); // ( "⬆" );


	private ICUEditor editor = null; // used to get a handle on rules text
	
	public ConvertTextTab(String title) {
		super( title );
	}    
    
    HashMap<String,ConvertTextString> textStringConverts = new HashMap<String,ConvertTextString>();    
    private void convertTextArea(StyleClassedTextArea textAreaIn, StyleClassedTextArea textAreaOut, String direction) {
    	String textIn = textAreaIn.getText();
    	if( textIn == null )
    		return;
    	
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
    
    public void setup(ICUEditor editor) {
        this.editor = editor;
        textAreaIn.setPrefHeight(313);
        textAreaOut.setPrefHeight(313);
        // textAreaIn.setFont( Font.font( defaultFont, FontWeight.NORMAL, 12) );
        textAreaIn.setStyle("-fx-font-family: '" + defaultFont + "'; -fx-font-size: 12;"  );
        textAreaIn.getProperties().put( "font-family", defaultFont );
        textAreaIn.getProperties().put( "font-size", "12" );
        textAreaOut.setStyle("-fx-font-family: '" + defaultFont + "'; -fx-font-size: 12;"  );
        textAreaOut.getProperties().put( "font-family", defaultFont );
        textAreaOut.getProperties().put( "font-size", "12" );
        
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
        HBox hUpDownButtonBox = new HBox( createFontChoiceBox( textAreaOut ), textAreaOutIncreaseFontSizeButton, textAreaOutDecreaseFontSizeButton, hspacer, convertButtonDown, convertButtonUp );
        hUpDownButtonBox.setAlignment(Pos.CENTER_LEFT);
        hUpDownButtonBox.setPadding(new Insets(2, 2, 2, 2));
        hUpDownButtonBox.setSpacing( 4 );
        
        VBox textVbox = new VBox( textAreaInMenuBox, textAreaIn, hUpDownButtonBox, textAreaOut );
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

}
