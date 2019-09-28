package org.geez.ui.xliterator;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import org.fxmisc.flowless.VirtualizedScrollPane;

import javafx.geometry.Insets;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class EditorTab extends XliteratorTab {
	
    private ICUEditor editor = new ICUEditor();
	private File externalIcuFile = null;
	private boolean unsavedChanges = false;
        
    private final String editorFontFamilyPref = "org.geez.ui.xliterator.editor.font.family";
    private final String editorFontSizePref   = "org.geez.ui.xliterator.editor.font.size";
    private final String editorBackgroundColor = "org.geez.ui.xliterator.editor.background.color";
    

	public EditorTab( String title ) {
		super( title );
		checkPreferences();
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
        Menu editorFontMenu     = createFontMenu( editor );
        Menu editorFontSizeMenu = createFontSizeMenu( editor );
        
        MenuBar editorMenutBar  = new MenuBar();
        editorMenutBar.getMenus().addAll( editorFontMenu, editorFontSizeMenu );
        
        VBox editorVBox = new VBox( editorMenutBar, new StackPane( new VirtualizedScrollPane<>( editor ) ) );
        this.setContent( editorVBox );
        
        
        editor.textProperty().addListener( (obs, oldText, newText) -> {
    		String label = this.getTitle();
        	if( editor.hasContentChanged(newText) ) {
        		if( label.charAt(0) != '*' ) {
        			// this.setText( "*" + this.getText() );
        			this.setTitle( "*" + this.getTitle() );
        			saveMenuItem.setDisable( false );
        			unsavedChanges = true;
        		}
        	}
        	else {
        		if( label.charAt(0) == '*' ) {
        			// this.setText( this.getText().substring(1) );
        			this.setTitle( this.getTitle().substring(1) );
        			saveMenuItem.setDisable( true );
        			unsavedChanges = false;
        		}
        	}
			saveAsMenuItem.setDisable( false );
        });
	}
    
    
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
    	// setText( editorFile.getName() );
    	setTitle( editorFile.getName() );
    }
	
	
	public boolean hasUnsavedChanges() {
		// we need to make sure this is set right when a file is loaded.
		return unsavedChanges;
	}
	

    public void saveDefaultFontSelections() {
        Preferences prefs = Preferences.userNodeForPackage( EditorTab.class );

        if( fontFamily != null ) {
        	prefs.put( editorFontFamilyPref, fontFamily );
        }
        if( fontSize != null ) {
        	prefs.put( editorFontSizePref, (fontSize==null) ? defaultFontSize : fontSize );
        }
    }
    
    
    private boolean checkPreferences() {
		Preferences prefs = Preferences.userNodeForPackage( EditorTab.class );
		  
		fontFamily = prefs.get( editorFontFamilyPref, null );
		  
		if( fontFamily == null ) {
			return false;
		}
		  
		fontSize = prefs.get( editorFontSizePref, null );
		  
		editor.getProperties().put( "font-family", fontFamily );
		editor.getProperties().put( "font-size", fontSize );
		setFontSize( editor, fontSize );
		  
		  
		String bgcolor = prefs.get( editorBackgroundColor, "white" );
		//editor.setStyle( "-fx-background-color: " + bgcolor + ";" );
		setBackgroundColor( bgcolor );
		  
		return true;
    }
  
  
    public void setBackgroundColor(String color ) {
    	// create a background fill 
    	BackgroundFill background_fill = new BackgroundFill( Color.valueOf( color ),  CornerRadii.EMPTY, Insets.EMPTY); 

    	// create Background 
    	Background background = new Background(background_fill); 

    	// set background 
    	editor.setBackground(background); 
    }
      
}
