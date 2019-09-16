package org.geez.ui;

import java.util.HashMap;

import org.geez.convert.text.ConvertTextString;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ConvertTextTab extends Tab {
	
    private final TextArea textAreaIn = new TextArea();
    private final TextArea textAreaOut = new TextArea();
	private final Button convertButtonDown = new Button(); // ( "⬇" );
    private final Button convertButtonUp = new Button(); // ( "⬆" );
    
    private String defaultFont = null;
    
	private boolean converted = false;
	private String scriptIn = null;
	private String scriptOut = null;
	private String variantOut = null;
	private String selectedTransliteration = null;
	private String transliterationDirection = null;


	public ConvertTextTab(String title) {
		super( title );
	}
	
    
	
    
    public void setDefaultFont(String defaultFont) {
    	this.defaultFont = defaultFont;
    }
    
    private void setFontSize(String fontSize) {	
		if( textAreaIn.getProperties().get( "font-size") == null ) {
			textAreaIn.setStyle( "-fx-font-family: '" + textAreaIn.getProperties().get("font-family") + "'; -fx-font-size: " + fontSize + ";" ); 
		}
		if( textAreaOut.getProperties().get( "font-size") == null ) {
			textAreaOut.setStyle( "-fx-font-family: '" + textAreaOut.getProperties().get("font-family") + "'; -fx-font-size: " + fontSize + ";" ); 
		}
    }
    
    
    private void setFont(String font, String component) {
    	if( "textAreaIn".equals( component) ) {
    		textAreaIn.setStyle( "-fx-font-family: '" + font + "'; -fx-font-size: " + textAreaIn.getProperties().get("font-size") + ";" ); 
    		textAreaIn.getProperties().put( "font-family", font );
    	}
    	else if( "textAreaOut".equals( component) ) {
    		textAreaOut.setStyle( "-fx-font-family: '" + font + "'; -fx-font-size: " + textAreaOut.getProperties().get("font-size") + ";" );
    		textAreaOut.getProperties().put( "font-family", font );
    	}
    	else {
    		// how can we reach here?s
    		/*
    		if( textAreaIn.getProperties().get( "font-family") == null ) {
    			textAreaIn.setStyle( "-fx-font-family: '" + font + "'; -fx-font-size: " + fontSize + ";" ); 
    		}
    		if( textAreaOut.getProperties().get( "font-family") == null ) {
    			textAreaOut.setStyle( "-fx-font-family: '" + font + "'; -fx-font-size: " + fontSize + ";" ); 
    		}
    		*/
    	}
    }
    

    private void incrementFontSize(String component) {
    	TextArea textArea = ( "textAreaIn".equals(component) ) ? textAreaIn : textAreaOut ;

    	String fontFamily = (String) textArea.getProperties().get("font-family");
    	int newSize = Integer.parseInt( (String)textArea.getProperties().get("font-size") ) + 1;
    	if( newSize <= 24 ) {
    		String fontSize = String.valueOf( newSize );
    		textArea.setStyle( "-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + ";" ); 
    		textArea.getProperties().put( "font-size", fontSize );
    	}
    }

    private void decrementFontSize(String component) {
    	TextArea textArea = ( "textAreaIn".equals(component) ) ? textAreaIn : textAreaOut ;

    	String fontFamily = (String) textArea.getProperties().get("font-family");
    	int newSize = Integer.parseInt( (String)textArea.getProperties().get("font-size") ) - 1;
    	if( newSize >= 10 ) {
    		String fontSize = String.valueOf( newSize );
    		textArea.setStyle( "-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + ";" ); 
    		textArea.getProperties().put( "font-size", fontSize );
    	}
    }
    
    
    HashMap<String,ConvertTextString> textStringConverts = new HashMap<String,ConvertTextString>();    
    private void convertTextArea(TextArea textAreaIn, TextArea textAreaOut, String direction) {
    	String textIn = textAreaIn.getText();
    	if( textIn == null )
    		return;
    	
    	try {
	    	ConvertTextString stringConverter = null;
	    	if( "Use Editor".equals( selectedTransliteration ) ) {
	    		// do not save the converter because the text may change:
	    		stringConverter = new ConvertTextString( editTab.getEditor().getText(), direction, true );
	
	    	}
	    	else {
		    	String transliterationKey = selectedTransliteration + "-" + direction ;
		    	
		    	if(! textStringConverts.containsKey( transliterationKey ) ) {
		    		textStringConverts.put( transliterationKey, new ConvertTextString( selectedTransliteration, direction ) );
		    	}
		    	stringConverter = textStringConverts.get( transliterationKey );
	    	}
	    	
	
	    	stringConverter.setText( textIn );
	    	
	    	textAreaOut.clear();
	        
	    	textAreaOut.setText( stringConverter.convertText( textIn ) );
	    	if( "both".equals( transliterationDirection ) ) {
	    		convertButtonUp.setDisable( false );
	    	}
		}
		catch(Exception ex) {
        	errorAlert(ex, "Translteration Defition Error. Correct to Proceed." );
			return;
		}
    }
    
    public void setup() {
        
        textAreaIn.setPrefHeight(300);
        textAreaOut.setPrefHeight(300);
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
        HBox textAreaInMenuBox = new HBox( createFontChoiceBox( "textAreaIn" ), textAreaInIncreaseFontSizeButton, textAreaInDecreaseFontSizeButton);
        textAreaInMenuBox.setPadding(new Insets(2, 2, 2, 2));
        textAreaInMenuBox.setSpacing(4);
        textAreaInIncreaseFontSizeButton.setOnAction( event -> {
        	incrementFontSize( "textAreaIn" );
        });
        textAreaInDecreaseFontSizeButton.setOnAction( event -> {
        	decrementFontSize( "textAreaIn" );
        });
        

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
        	incrementFontSize( "textAreaOut" );
        });
        textAreaOutDecreaseFontSizeButton.setOnAction( event -> {
        	decrementFontSize( "textAreaOut" );
        });
        Region hspacer = new Region();
        hspacer.prefWidth( 200 );
        HBox.setHgrow(hspacer, Priority.SOMETIMES);
        HBox hUpDownButtonBox = new HBox( createFontChoiceBox( "textAreaOut" ), textAreaOutIncreaseFontSizeButton, textAreaOutDecreaseFontSizeButton, hspacer, convertButtonDown, convertButtonUp );
        hUpDownButtonBox.setAlignment(Pos.CENTER_LEFT);
        hUpDownButtonBox.setPadding(new Insets(2, 2, 2, 2));
        hUpDownButtonBox.setSpacing( 4 );
        
        VBox textVbox = new VBox( textAreaInMenuBox, textAreaIn, hUpDownButtonBox, textAreaOut );
        this.setContent( textVbox );
    }
    
    
    private ChoiceBox<String> createFontChoiceBox(String component, String defaultSelection) {  
    	ChoiceBox<String> choiceBox = new ChoiceBox<>();
    	for(String font: javafx.scene.text.Font.getFamilies() ) {
    		choiceBox.getItems().add( font );
    	}
    	choiceBox.getSelectionModel().select( defaultSelection );
        choiceBox.setOnAction( evt -> setFont( choiceBox.getSelectionModel().getSelectedItem(), component ) );
        
        return choiceBox;    
    }
    
    private ChoiceBox<String> createFontChoiceBox(String component) {
    	return createFontChoiceBox(component, defaultFont);
    }

    
    
    void setScriptIn(String scriptIn ) {
    	this.scriptIn = scriptIn;
		convertButton.setDisable( true );
    }
    
    void setScriptOut(String scriptOut ) {
    	this.scriptOut = scriptOut;
		convertButton.setDisable( true );
    }
      
    void setVariantOut(String variantOut, String selectedTransliteration, String transliterationDirection ) {
    	this.variantOut = variantOut;
    	this.selectedTransliteration = selectedTransliteration;
    	this.transliterationDirection = transliterationDirection;
    }
    

	
	private void errorAlert( Exception ex, String header ) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle( "An Exception has occured" );
        alert.setHeaderText( header );
        alert.setContentText( ex.getMessage() );
        alert.showAndWait();
	}
	
	
	private void errorAlert( String title, String message ) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle( title );
        alert.setHeaderText( message );
        alert.setContentText( message );
        alert.showAndWait();
	}
}
