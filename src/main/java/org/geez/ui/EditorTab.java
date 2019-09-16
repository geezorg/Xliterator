package org.geez.ui;

import org.fxmisc.flowless.VirtualizedScrollPane;

import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class EditorTab extends Tab {
	
    private ICUEditor editor = new ICUEditor();
    private String defaultFont = null;
    

	public EditorTab(String title) {
		super(title);
	}
	
	
	public ICUEditor getEditor() {
		return editor;
	}
	
    
    public void setDefaultFont(String defaultFont) {
    	this.defaultFont = defaultFont;
    }
    
    
    private void setFontSize(String fontSize) {        
		String fontFamily = (String) editor.getProperties().get("font-family");
		editor.setStyle( "-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + ";" );
		editor.getProperties().put( "font-size", fontSize );
    }
    
    
	public void setup( MenuItem saveMenuItem, MenuItem saveAsMenuItem ) {
        Menu editorFontMenu = createFontMenu( "editor" );
        Menu editorFontSizeMenu = createFontSizeMenu();
        MenuBar editorMenutBar = new MenuBar();
        editorMenutBar.getMenus().addAll( editorFontMenu, editorFontSizeMenu );
        VBox editorVBox = new VBox( editorMenutBar, new StackPane( new VirtualizedScrollPane<>( editor ) ) );
        this.setContent( editorVBox );
        
        
        editor.textProperty().addListener( (obs, oldText, newText) -> {
    		String label = this.getText();
        	if( editor.hasContentChanged(newText) ) {
        		if( label.charAt(0) != '*' ) {
        			this.setText( "*" + this.getText() );
        			saveMenuItem.setDisable( false );
        		}
        	}
        	else {
        		if( label.charAt(0) == '*' ) {
        			this.setText( this.getText().substring(1) );
        			saveMenuItem.setDisable( true );
        		}
        	}
			saveAsMenuItem.setDisable( false );
        });
	}
	
    private Menu createFontMenu( String component ) {
    	Menu menu = new Menu();
    	menu.setId( "transparent" );
    	menu.setGraphic( createFontChoiceBox( component ) );
        return menu;
    }
    

    private Menu createFontSizeMenu() {
    	Menu menu = new Menu();
    	menu.setId( "transparent" );
    	ChoiceBox<String> choiceBox = new ChoiceBox<>();
    	for(int i=10 ; i < 24; i++ ) {
    		String size = String.valueOf(i);
    		choiceBox.getItems().add( size );
    	}
    	choiceBox.getSelectionModel().select( "12" );
        choiceBox.setOnAction( evt -> setFontSize( choiceBox.getSelectionModel().getSelectedItem() ) );
    	menu.setGraphic( choiceBox );
        
        return menu;
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
    
    

    private void setFont(String font, String component) {
    		// set the font in all components, unless already set for the text areas
    		String fontSize = (String) editor.getProperties().get("font-size");
    		editor.setStyle( "-fx-font-family: '" + font + "'; -fx-font-size: " + fontSize + ";" );
    }

}
