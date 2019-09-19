package org.geez.ui;

import java.io.File;
import java.io.IOException;

import org.fxmisc.flowless.VirtualizedScrollPane;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EditorTab extends XliteratorTab {
	
    private ICUEditor editor = new ICUEditor();
	private File externalIcuFile = null;
	private boolean unsavedChanges = false;
    

	public EditorTab( String title ) {
		super(title);
	}
	
	public void reset( String title ) {
		setText( title );
		externalIcuFile = null;
		unsavedChanges = false;
		editor = null;
		editor = new ICUEditor();
	}
	
	
	public ICUEditor getEditor() {
		return editor;
	}
    
    
	public void setup( MenuItem saveMenuItem, MenuItem saveAsMenuItem ) {
        Menu editorFontMenu = createFontMenu( editor );
        Menu editorFontSizeMenu = createFontSizeMenu( editor );
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
        			unsavedChanges = true;
        		}
        	}
        	else {
        		if( label.charAt(0) == '*' ) {
        			this.setText( this.getText().substring(1) );
        			saveMenuItem.setDisable( true );
        			unsavedChanges = false;
        		}
        	}
			saveAsMenuItem.setDisable( false );
        });
	}
    
    
	/*
    private void setFontSize( String fontSize ) {
		String fontFamily = (String) editor.getProperties().get("font-family");
		editor.setStyle( "-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + ";" );
		editor.getProperties().put( "font-size", fontSize );
    }

    
    private void setFont( String font, String component ) {
    		// the editor is a single component, so we can drop this.
    		String fontSize = (String) editor.getProperties().get("font-size");
    		editor.setStyle( "-fx-font-family: '" + font + "'; -fx-font-size: " + fontSize + ";" );
    }
    */
    
    
    public void saveContent( Stage stage, boolean saveToNewFile ) {
    	if( editor.getText() == null ) {
    		// this shouldn't happen, the Save and Save As menu items should be disabled:
    		errorAlert( "No Data Error", "The editor contains no content." );
    		return;
    	}
		try {
			String newFileName = null;
			if( saveToNewFile ) {
        		newFileName = editor.saveContentToNewFile( stage );
        		if( newFileName != null ) {
        			setText( newFileName );
        		}
			}
			else {
				// this may or may not be the initial save
				if( editor.isInitialSave() ) {
	        		newFileName = editor.saveContentToNewFile( stage );
	        		if( newFileName != null ) {
	        			setText( newFileName );
	        		}
				}
				else {
					newFileName = editor.saveContentToFile();
				}
			}
			
    		if( newFileName != null ) {
    			setText( newFileName );
    		}
    		unsavedChanges = false;
		}
    	catch (Exception ex){
    		errorAlert( "Error occured saving file",  "An error occured while saving the file \"" + externalIcuFile.getPath() + "\":\n" + ex.getMessage() );
    	}
	}
    
    
    public void loadFile( File editorFile ) throws IOException {
    	editor.loadFile( editorFile );
    	setText( editorFile.getName() );
    }
	
	
	public boolean hasUnsavedChanges() {
		// we need to make sure this is set right when a file is loaded.
		return unsavedChanges;
	}
}
