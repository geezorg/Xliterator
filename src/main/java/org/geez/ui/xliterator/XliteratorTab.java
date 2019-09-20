package org.geez.ui.xliterator;

import org.fxmisc.richtext.StyleClassedTextArea;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;

public abstract class XliteratorTab extends Tab {
	
	protected boolean converted = false;
	protected String scriptIn = null;
	protected String scriptOut = null;
	protected String variantOut = null;
	protected String selectedTransliteration = null;
	protected String transliterationDirection = null;
	protected String caseOption = null;
	protected String defaultFont = null;
	protected String fontFamily = null;
	protected String fontSize = null;
	
    
	public XliteratorTab( String title ) {
		super(title);
	}

    public void setDefaultFont(String defaultFont) {
    	this.defaultFont = defaultFont;
    }

    

    protected void incrementFontSize(StyleClassedTextArea component) {
    	// StyleClassedTextArea textArea = ( "textAreaIn".equals(component) ) ? textAreaIn : textAreaOut ;

    	String fontFamily = (String) component.getProperties().get("font-family");
    	int newSize = Integer.parseInt( (String)component.getProperties().get("font-size") ) + 1;
    	if( newSize <= 24 ) {
    		String fontSize = String.valueOf( newSize );
    		component.setStyle( "-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + ";" ); 
    		component.getProperties().put( "font-size", fontSize );
    	}
    }

    protected void decrementFontSize(StyleClassedTextArea component) {
    	// StyleClassedTextArea textArea = ( "textAreaIn".equals(component) ) ? textAreaIn : textAreaOut ;

    	String fontFamily = (String) component.getProperties().get("font-family");
    	int newSize = Integer.parseInt( (String)component.getProperties().get("font-size") ) - 1;
    	if( newSize >= 10 ) {
    		String fontSize = String.valueOf( newSize );
    		component.setStyle( "-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + ";" ); 
    		component.getProperties().put( "font-size", fontSize );
    	}
    }
    
    protected void setFontSize( StyleClassedTextArea component, String fontSize  ) {
		// if( component.getProperties().get( "font-size") == null ) {
			component.setStyle( "-fx-font-family: '" + component.getProperties().get("font-family") + "'; -fx-font-size: " + fontSize + ";" ); 
		// }
    		component.getProperties().put( "font-size", fontSize );
    }
    
    protected void setFontFamily( StyleClassedTextArea component, String font ) {
    		component.setStyle( "-fx-font-family: '" + font + "'; -fx-font-size: " + component.getProperties().get("font-size") + ";" ); 
    		component.getProperties().put( "font-family", font );
    }
    
    
    protected Menu createFontMenu( StyleClassedTextArea component ) {
    	Menu menu = new Menu();
    	menu.setId( "transparent" );
    	menu.setGraphic( createFontChoiceBox( component ) );
        return menu;
    }
    
    
    protected Menu createFontSizeMenu( StyleClassedTextArea component ) {
    	Menu menu = new Menu();
    	menu.setId( "transparent" );
    	ChoiceBox<String> choiceBox = new ChoiceBox<>();
    	for(int i=10 ; i <= 24; i++ ) {
    		String size = String.valueOf(i);
    		choiceBox.getItems().add( size );
    	}
    	choiceBox.getSelectionModel().select( "12" );
        choiceBox.setOnAction( evt -> setFontSize( component, choiceBox.getSelectionModel().getSelectedItem() ) );
    	menu.setGraphic( choiceBox );
        
        return menu;
    }
    
    
    protected ChoiceBox<String> createFontChoiceBox( StyleClassedTextArea component, String defaultSelection ) {  
    	ChoiceBox<String> choiceBox = new ChoiceBox<>();
    	for(String font: javafx.scene.text.Font.getFamilies() ) {
    		choiceBox.getItems().add( font );
    	}
    	choiceBox.getSelectionModel().select( defaultSelection );
        choiceBox.setOnAction( evt -> setFontFamily( component, choiceBox.getSelectionModel().getSelectedItem() ) );
        
        return choiceBox;    
    }
    
    
    protected ChoiceBox<String> createFontChoiceBox( StyleClassedTextArea component ) {
    	return createFontChoiceBox(component, defaultFont);
    }
    
    
    public void setScriptIn( String scriptIn ) {
    	this.scriptIn = scriptIn;
    }
    
    public void setScriptOut( String scriptOut ) {
    	this.scriptOut = scriptOut;
    }
      
    public void setVariantOut( String variantOut, String selectedTransliteration, String transliterationDirection ) {
    	this.variantOut = variantOut;
    	this.selectedTransliteration = selectedTransliteration;
    	this.transliterationDirection = transliterationDirection;
    }
    
    public void setCaseOption( String caseOption ) {
    	this.caseOption = caseOption;
    }
	
	protected void errorAlert( Exception ex, String header ) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle( "An Exception has occured" );
        alert.setHeaderText( header );
        alert.setContentText( ex.getMessage() );
        alert.showAndWait();
	}
	
	
	protected void errorAlert( String title, String message ) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle( title );
        alert.setHeaderText( message );
        alert.setContentText( message );
        alert.showAndWait();
	}
	
}
