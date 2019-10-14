package org.geez.ui.xliterator;

import java.util.HashMap;
import java.util.prefs.Preferences;

import org.fxmisc.richtext.StyleClassedTextArea;
import org.geez.convert.text.ConvertTextString;
import org.geez.ui.Xliterator;

import com.google.gson.JsonObject;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class ConvertTextTab extends XliteratorTab {
	
    private final StyleClassedTextArea textAreaIn  = new StyleClassedTextArea();
    private final StyleClassedTextArea textAreaOut = new StyleClassedTextArea();
    
	private final Button convertButtonDown = new Button(); // ( "⬇" );
    private final Button convertButtonUp   = new Button(); // ( "⬆" );
    private final CheckBox autoConversionCheckBox = new CheckBox( "Auto Convert" );
    private boolean autoConvertDown = true;
    private boolean autoConvertUp = true;
    
    private final String textAreaInFontFacePref  = "org.geez.ui.xliterator.convertTextTab.textAreaIn.font.face";
    private final String textAreaInFontSizePref  = "org.geez.ui.xliterator.convertTextTab.textAreaIn.font.size";
    private final String textAreaOutFontFacePref = "org.geez.ui.xliterator.convertTextTab.textAreaOut.font.face";
    private final String textAreaOutFontSizePref = "org.geez.ui.xliterator.convertTextTab.textAreaOut.font.size";

	private Xliterator xlit = null; // used to get a handle on rules text
	
	public ConvertTextTab(String title, Xliterator xlit) {
		super( title );
		setup( xlit );
	}    
    
	private HashMap<String,String> registeredDependencies = new HashMap<String,String>();
	// private String selectedTransliteration = null;
    HashMap<String,ConvertTextString> textStringConverts = new HashMap<String,ConvertTextString>();
    
    /*
    private void convertTextAreaOld(StyleClassedTextArea textAreaIn, StyleClassedTextArea textAreaOut, String direction) {
    	String textIn = textAreaIn.getText();
    	if( textIn == null ) {
    		return;
    	}
    	
    	try {
    		if( (dependencies != null) && ( "false".equals( registeredDependencies.get(selectedTransliteration) ) ) ) {
    			xlit.getConfig().registerDependencies( dependencies );
    			registeredDependencies.put( selectedTransliteration , "true" );
    		}
	    	ConvertTextString stringConverter = null;
	    	if( selectedTransliteration.equals( Xliterator.useSelectedEdtior ) ) {
	    		// do not save the converter because the text may change:
	    		// The ConvertTextString constructor needs to be reworked here, see notes within its source file:
	    		// stringConverter = new ConvertTextString( editor.getText(), direction, true );
	    		
	    		EditorTab editorTab = xlit.getSelectedEditorTab();
	    		if( editorTab == null ) {
	    			// error alert
	    			return;
	    		}
	    		stringConverter = new ConvertTextString();
	    		stringConverter.setRules( editorTab.getEditor().getText(), direction );
	    	}
	    	else {
		    	String transliterationKey = ((alias == null ) ? selectedTransliteration : alias) + "-" + direction ;
		    	
		    	if(! textStringConverts.containsKey( transliterationKey ) ) {
		    		textStringConverts.put( transliterationKey, new ConvertTextString( selectedTransliteration, direction ) );
		    	}
		    	stringConverter = textStringConverts.get( transliterationKey );
	    	}
	    	
	    	stringConverter.setCaseOption( caseOption );
	    	stringConverter.setText( textIn );
	    	
	    	// textAreaOut.clear();
	        
	    	textAreaOut.replaceText( stringConverter.convertText( textIn ) );
		}
		catch(Exception ex) {
        	errorAlert(ex, "Translteration Definition Error. Correct to Proceed." );
			return;
		}
    }
     */

	ConvertTextString stringConverterDown = null;
	ConvertTextString stringConverterUp = null;
	private void initializeStringConverters() {
		
		try {	
	    	if( selectedTransliteration.equals( Xliterator.useSelectedEditor ) ) {
	    		// do not save the converter because the text may change:
	    		// The ConvertTextString constructor needs to be reworked here, see notes within its source file:
	    		// stringConverter = new ConvertTextString( editor.getText(), direction, true );
	    		
	    		EditorTab editorTab = xlit.getSelectedEditorTab();
	    		if( editorTab == null ) {
	    			// error alert
	    			return;
	    		}
	    		transliterationDirection = editorTab.getSelectedDirection();
	    		stringConverterDown = new ConvertTextString();
	    		stringConverterDown.setRules( editorTab.getEditor().getText(), transliterationDirection );
	    		
	    		if( "both".equals( transliterationDirection ) ) {
	        		stringConverterUp = new ConvertTextString();
	        		stringConverterUp.setRules( editorTab.getEditor().getText(), "reverse" );    	
	        		stringConverterUp.setCaseOption( caseOption );
	    		}
	    	}
	    	else {
		    	String transliterationKeyForward = ((alias == null ) ? (selectedTransliteration+ "-" + transliterationDirection) : alias) ;
		    	
		    	if(! textStringConverts.containsKey( transliterationKeyForward ) ) {
		    		textStringConverts.put( transliterationKeyForward, new ConvertTextString( selectedTransliteration, transliterationDirection ) );
		    	}
		    	stringConverterDown = textStringConverts.get( transliterationKeyForward );
		    	
	    		if( "both".equals( transliterationDirection ) ) {
	    			String reverseAlias = (backwardAlias == null ) ? null : backwardAlias ;
	    			if( reverseAlias == null ) {
	    				reverseAlias = (alias == null ) ? null : alias ;
	    			}
	    	    	String transliterationKeyReverse = (reverseAlias == null ) ? (selectedTransliteration + "-reverse") : reverseAlias ;
	    	    	
	    	    	if(! textStringConverts.containsKey( transliterationKeyReverse ) ) {
	    	    		textStringConverts.put( transliterationKeyReverse, new ConvertTextString( selectedTransliteration, "reverse" ) );
	    	    	}
	    	    	stringConverterUp = textStringConverts.get( transliterationKeyReverse );   
	    	    	stringConverterUp.setCaseOption( caseOption );
	    		}
	    		
	    		if( (dependencies != null) && ( "false".equals( registeredDependencies.get(selectedTransliteration) ) ) ) {
	    			// TBD: update registerDependencies to handle reverses:
	    			xlit.getConfig().registerDependencies( dependencies );
	    			registeredDependencies.put( selectedTransliteration , "true" );
	    		}
	    	}
    	
		}
		catch(Exception ex) {
	    	errorAlert(ex, "Translteration Definition Error. Correct to Proceed." );
			return;
		}
    	
    	stringConverterDown.setCaseOption( caseOption );
	}
	
	public void setCaseOption( String caseOption ) {
		super.setCaseOption( caseOption );
		if( stringConverterUp != null ) {
			stringConverterUp.setCaseOption( caseOption );
		}
		if(  stringConverterDown != null ) {
			stringConverterDown.setCaseOption( caseOption );
		}
	}
	
    private void convertTextArea(StyleClassedTextArea textAreaIn, StyleClassedTextArea textAreaOut, ConvertTextString stringConverter) {
    	String textIn = textAreaIn.getText();
    	if( textIn == null ) {
    		return;
    	}
        
    	textAreaOut.replaceText( stringConverter.convertText( textIn ) );

    }
    public void setup(Xliterator xlit) {
    	this.xlit = xlit;
        textAreaIn.setPrefHeight( 313 );
        textAreaOut.setPrefHeight( 313 );
        textAreaIn.setId( "convertText" );
        textAreaOut.setId( "convertText" );
        /*
    	textAreaIn.setStyle( "-fx-fill: white;" );
    	textAreaOut.setStyle( "-fx-fill: white;" );
    	*/
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
        textAreaInIncreaseFontSizeButton.setOnAction( evt -> {
        	incrementFontSize( textAreaIn );
        });
        textAreaInDecreaseFontSizeButton.setOnAction( evt -> {
        	decrementFontSize( textAreaIn );
        });
        textAreaInIncreaseFontSizeButton.setTooltip( new Tooltip( "Increase Font Size" ) );
        textAreaInDecreaseFontSizeButton.setTooltip( new Tooltip( "Descrease Font Size" ) );
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
        	if( stringConverterDown == null ) {
        		initializeStringConverters();
        	}
        	convertTextArea( textAreaIn, textAreaOut, stringConverterDown ); 
        });
        
        Image imageUp = new Image( classLoader.getResourceAsStream( "images/arrow-circle-up.png" ) );
        ImageView imageViewUp = new ImageView( imageUp );
        imageViewUp.setFitHeight( 18 );
        imageViewUp.setFitWidth( 18 );
        convertButtonUp.setGraphic( imageViewUp );
        // convertButtonUp.setStyle( "-fx-font-size: 24;");
        convertButtonUp.setDisable( true );
        convertButtonUp.setOnAction( event -> {
        	if( stringConverterUp == null ) {
        		initializeStringConverters();
        	}
        	convertTextArea( textAreaOut, textAreaIn, stringConverterUp ); 
        });
        
        Button textAreaOutIncreaseFontSizeButton = new Button( "+" ); 
        Button textAreaOutDecreaseFontSizeButton = new Button( "-" );
        textAreaOutIncreaseFontSizeButton.setOnAction( event -> {
        	incrementFontSize( textAreaOut );
        });
        textAreaOutDecreaseFontSizeButton.setOnAction( event -> {
        	decrementFontSize( textAreaOut );
        });
        autoConversionCheckBox.setOnAction( evt -> {
        	autoConvertDown = autoConvertUp = autoConversionCheckBox.isSelected();
        });
        autoConversionCheckBox.setSelected( true );
        autoConversionCheckBox.setDisable( true );
        
        Region hspacer = new Region();
        hspacer.prefWidth( 200 );
        HBox.setHgrow(hspacer, Priority.SOMETIMES);
        HBox textAreaOutMenuBox = new HBox( createFontChoiceBox( textAreaOut ), textAreaOutIncreaseFontSizeButton, textAreaOutDecreaseFontSizeButton, hspacer, autoConversionCheckBox, convertButtonDown, convertButtonUp );
        textAreaOutMenuBox.setAlignment(Pos.CENTER_LEFT);
        textAreaOutMenuBox.setPadding(new Insets(2, 2, 2, 2));
        textAreaOutMenuBox.setSpacing( 4 );
        
        textAreaOutIncreaseFontSizeButton.setTooltip( new Tooltip( "Increase Font Size" ) );
        textAreaOutDecreaseFontSizeButton.setTooltip( new Tooltip( "Descrease Font Size" ) );
        
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

    private void enableConversionButtions( String direction ) {
    	transliterationDirection = direction;
    	if( "forward".equals( direction ) ) {
			convertButtonUp.setDisable( true );
			convertButtonDown.setDisable( false );    		
    	}
    	else {
			convertButtonUp.setDisable( false );
			convertButtonDown.setDisable( false );     		
    	}    	
    }
    
    public void setScriptInAndDirection(String scriptIn, String direction) {
    	setScriptIn( scriptIn, null );
    	enableConversionButtions( direction );
    }
    
    public void setScriptIn(String scriptIn, String variantIn) {
    	super.setScriptIn(scriptIn, variantIn);
    	if( scriptIn.equals( Xliterator.useSelectedEditor ) ) {
    		this.selectedTransliteration = scriptIn;
			convertButtonUp.setDisable( false );
			convertButtonDown.setDisable( false );
			convertButtonDown.setTooltip( new Tooltip( "Convert in forward direction" )  );
			convertButtonUp.setTooltip( new Tooltip( "Convert in reverse direction" ) );
			stringConverterDown = null;
			stringConverterUp = null;
    	}
    	else {
			convertButtonUp.setDisable( true );
			convertButtonDown.setDisable( true );
    	}
    }
    
    /*
    public void setScriptOut(String scriptOut ) {
    	super.setScriptOut(scriptOut);
		// convertButtonUp.setDisable( true );
		convertButtonUp.setTooltip( new Tooltip( "Convert from: " + scriptOut + " to " + scriptIn )  );
		// convertButtonDown.setDisable( true );
		convertButtonDown.setTooltip( new Tooltip( "Convert from: " + scriptIn + " to " + scriptOut ) );
    }
    */
    
    /*
    public void setVariantOut( String variantOut, String selectedTransliteration, String transliterationDirection, ArrayList<String> dependencies, String alias ) {
    	super.setVariantOut(variantOut, selectedTransliteration, transliterationDirection, dependencies, alias);
    	this.selectedTransliteration = selectedTransliteration;
		registeredDependencies.put( selectedTransliteration , "false" );
		convertButtonUp.setDisable( false );
		convertButtonDown.setDisable( false );
        if( "both".equals( transliterationDirection ) ) {
        	convertButtonUp.setDisable( false );
        }
        else {
        	convertButtonUp.setDisable( true );        	
        }
    }
    */
  
    public void setTransliteration( JsonObject transliteration ) {
    	if( this.transliteration == null ) {
            // this must be the first call to the method, only set the listener once
            textAreaIn.textProperty().addListener( (obs, oldText, newText) -> {
            	if( autoConvertDown ) {
                	if( stringConverterDown == null ) {
                		initializeStringConverters();
                	}
            		autoConvertUp = false;
            		convertTextArea( textAreaIn, textAreaOut, stringConverterDown );
            		autoConvertUp = true;
            	}
            }); 		
    	}
    	super.setTransliteration( transliteration );
    	
    	stringConverterDown = null;
    	stringConverterUp = null;
    	
		registeredDependencies.put( selectedTransliteration , "false" );
		convertButtonUp.setTooltip( new Tooltip( "Convert from: " + scriptOut + " to " + scriptIn )  );
		convertButtonDown.setTooltip( new Tooltip( "Convert from: " + scriptIn + " to " + scriptOut ) );
		convertButtonUp.setDisable( false );
		convertButtonDown.setDisable( false );
        autoConversionCheckBox.setDisable( false );
		
        if( "both".equals( transliterationDirection ) ) {	
        	convertButtonUp.setDisable( false );
            textAreaOut.textProperty().addListener( (obs, oldText, newText) -> {
            	// only do this if reverse is supported
            	if( autoConvertUp ) {
                	if( stringConverterUp == null ) {
                		initializeStringConverters();
                	}
            		autoConvertDown = false;
            		convertTextArea( textAreaOut, textAreaIn, stringConverterUp );
            		autoConvertDown = true;
            	}
            });
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
        
		Preferences editorPrefs = Preferences.userNodeForPackage( EditorTab.class );
		setBackgroundColor( editorPrefs.get( EditorTab.editorBackgroundColor, "white" ) );
        
        return true;
    }
    
    public void setBackgroundColor(String color) {
    	// create a background fill 
    	BackgroundFill background_fill = new BackgroundFill( Color.valueOf( color ), CornerRadii.EMPTY, Insets.EMPTY ); 

    	// create Background 
    	Background background = new Background(background_fill); 

    	// set background 
    	textAreaIn.setBackground(background); 
    	textAreaOut.setBackground(background); 
    }
    
    public void setEditorTransliterationDirection( String direction ) {
    	if( selectedTransliteration.equals( Xliterator.useSelectedEditor ) ) {
        	enableConversionButtions( direction );
    	}
    }
}