package org.geez.ui.xliterator;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.geez.convert.text.ConvertTextString;
import org.geez.ui.Xliterator;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;

public class EditorTab extends XliteratorTab {
	
    private ICUEditor editor = new ICUEditor();
    private Xliterator xlit = null;
	private File externalIcuFile = null;
	private boolean unsavedChanges = false;
        
    public static final String editorFontFamilyPref  = "org.geez.ui.xliterator.editor.font.family";
    public static final String editorFontSizePref    = "org.geez.ui.xliterator.editor.font.size";
    public static final String editorBackgroundColor = "org.geez.ui.xliterator.editor.background.color";
    

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
    
    
	private String selectedDirection = null;
	public String getSelectedDirection() {
		return selectedDirection;
	}
	
	
    boolean registered = false;
	public void setup(Stage primaryStage, Xliterator xlit, MenuItem saveMenuItem, MenuItem saveAsMenuItem ) {
		this.xlit = xlit;
		XliteratorConfig config = xlit.getConfig();
        Menu editorFontMenu     = createFontMenu( editor );
        Menu editorFontSizeMenu = createFontSizeMenu( editor );
        
        // alias = (String)getProperties().get( "alias" );
        
        MenuBar editorMenutBar  = new MenuBar();
        editorMenutBar.getMenus().addAll( editorFontMenu, editorFontSizeMenu );
        
        
        ChoiceBox<String> directionBox = new ChoiceBox<String>();
        directionBox.getItems().addAll( "Forward", "Both" );
        
        // transliterationDirection = (String)getProperties().get( "direction" );
        if( "both".equals( transliterationDirection ) ) {
        	directionBox.getSelectionModel().select(1);
        	selectedDirection = "both";
        }
        else {
        	directionBox.getSelectionModel().select(0);
        	selectedDirection = "forward";
        }
        directionBox.setOnAction( evt -> {
        	selectedDirection = directionBox.getSelectionModel().getSelectedItem().toLowerCase();
        	xlit.setEditorTransliterationDirection( selectedDirection );
        });

        Button actionButton = new Button( "Action:" );
        ChoiceBox<String> actionBox = new ChoiceBox<String>();
        actionBox.getItems().addAll( "Validate", "Register", "Unregister" );
        actionBox.getSelectionModel().select(0);
        
        actionButton.setOnAction( evt -> {
        	String action =  actionBox.getSelectionModel().getSelectedItem();
        	if( "Validate".equals( action ) ) {
        		try {
        			ConvertTextString testStringConverter = new ConvertTextString();
        			testStringConverter.setRules( editor.getText(), selectedDirection );
    		        Alert alert = new Alert(AlertType.INFORMATION);
    		        alert.setTitle( "Validatition Success" );
    		        alert.setHeaderText( "The transliteration rules are valid." );
        		}
        		catch(Exception ex) {
        			errorAlert(ex, "Compilation Failed:" );
        		}
        	}
        	else if( "Register".equals( action ) ) {
            	TextInputDialog registerDialog = createRegisterDialog( primaryStage );
            	// Dialog<Pair<String,String>> registerDialog = createRegisterDialog();
            	// Optional<Pair<String,String>> result = registerDialog.showAndWait();
            	Optional<String> result =  registerDialog.showAndWait();
            	result.ifPresent( selections -> {
            		name = result.get();
            		// String alias = selections.getKey();
            		// String direction = selections.getKey();
            		try {
            			// String direction = directionBox.getSelectionModel().getSelectedItem().toLowerCase();
            			config.registerTransliteration( name, selectedDirection, editor.getText() );
            			// getProperties().put( "direction", direction );
            			// getProperties().put( "alias", alias );
            			registered = true;
            		}
            		catch(Exception ex) {
            			errorAlert(ex, "A registration problem has occured:" );
            		}
            	});
        	}
        	else {
            	Alert alert = new Alert(AlertType.CONFIRMATION);
            	alert.setTitle( "Confirm Unregistration" );
            	alert.setHeaderText( "Confirm Unregistration" );
            	alert.setContentText( "Are you sure that you want to unregister \"" + name + "\"?" );

            	Optional<ButtonType> result = alert.showAndWait();
            	if (result.get() == ButtonType.OK){
            	   config.unregisterTransliteration( name );
            	   registered = false;
            	}        		
        	}
        });
        
        /*
        Button unregister = new Button( "Unregister" );
        Button register = new Button( "Register" );
        register.setTooltip( new Tooltip( "Register a name for current session" ) );
        register.setOnAction( evt -> {
        	TextInputDialog registerDialog = createRegisterDialog( primaryStage );
        	// Dialog<Pair<String,String>> registerDialog = createRegisterDialog();
        	// Optional<Pair<String,String>> result = registerDialog.showAndWait();
        	Optional<String> result =  registerDialog.showAndWait();
        	result.ifPresent( selections -> {
        		name = result.get();
        		// String alias = selections.getKey();
        		// String direction = selections.getKey();
        		try {
        			// String direction = directionBox.getSelectionModel().getSelectedItem().toLowerCase();
        			config.registerTransliteration( name, selectedDirection, editor.getText() );
        			// getProperties().put( "direction", direction );
        			// getProperties().put( "alias", alias );
        			unregister.setDisable( false );
        		}
        		catch(Exception ex) {
        			errorAlert(ex, "A registration problem has occured:" );
        		}
        	});
        	
        });

        
        unregister.setDisable( true );
        unregister.setTooltip( new Tooltip( "Unegister with ICU Transliterator Library" ) );
        unregister.setOnAction( evt -> {
        	Alert alert = new Alert(AlertType.CONFIRMATION);
        	alert.setTitle( "Confirm Unregistration" );
        	alert.setHeaderText( "Confirm Unregistration" );
        	alert.setContentText( "Are you sure that you want to unregister \"" + name + "\"?" );

        	Optional<ButtonType> result = alert.showAndWait();
        	if (result.get() == ButtonType.OK){
        	   config.unregisterTransliteration( name );
        	   unregister.setDisable( true );
        	}
        }); 
        */
        
        HBox controls = new HBox( 5 );
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        controls.setAlignment( Pos.CENTER_LEFT);
        controls.getChildren().addAll( editorMenutBar, spacer, new Label( "Direction: " ), directionBox, actionButton, actionBox );
        
        
        VBox editorVBox = new VBox( controls, new StackPane( new VirtualizedScrollPane<>( editor ) ) );
        this.setContent( editorVBox );
        
        
        editor.textProperty().addListener( (obs, oldText, newText) -> {
    		String label = this.getTitle();
        	if( editor.hasContentChanged(newText) ) {
        		if( label.charAt(0) != '*' ) {
        			// this.setText( "*" + this.getText() );
        			this.setTitle( "*" + this.getTitle() );
        			this.setStyle( "-fx-background-color: #CD5C5C" );
        			saveMenuItem.setDisable( false );
        			unsavedChanges = true;
        		}
        	}
        	else {
        		if( label.charAt(0) == '*' ) {
        			// this.setText( this.getText().substring(1) );
        			this.setTitle( this.getTitle().substring(1) );
        			this.setStyle( null );
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
        			setTitle( newFileName );
        			setStyle( null );
        		}
			}
			else {
				// this may or may not be the initial save
				if( editor.isInitialSave() ) {
	        		newFileName = editor.saveContentToNewFile( stage );
	        		if( newFileName != null ) {
	        			setTitle( newFileName );
	        			setStyle( null );
	        			
	        		}
				}
				else {
					newFileName = editor.saveContentToFile();
				}
			}
			
    		if( newFileName != null ) {
    			setTitle( newFileName );
    			setStyle( null );
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
    
    
    public String getBackgroundColor( ) {
		Preferences prefs = Preferences.userNodeForPackage( EditorTab.class );
		return prefs.get( editorBackgroundColor, "white" );
    }
    
    
    private boolean checkPreferences() {
		Preferences prefs = Preferences.userNodeForPackage( EditorTab.class );		  
		  
		String bgcolor = prefs.get( editorBackgroundColor, "white" );
		setBackgroundColor( bgcolor );
		
		fontFamily = prefs.get( editorFontFamilyPref, null );
		  
		if( fontFamily == null ) {
			return false;
		}
		  
		fontSize = prefs.get( editorFontSizePref, null );
		  
		editor.getProperties().put( "font-family", fontFamily );
		editor.getProperties().put( "font-size", fontSize );
		setFontSize( editor, fontSize );

		  
		return true;
    }
  
  
    public void setBackgroundColor(String color ) {
    	// create a background fill 
    	BackgroundFill background_fill = new BackgroundFill( Color.valueOf( color ), CornerRadii.EMPTY, Insets.EMPTY ); 

    	// create Background 
    	Background background = new Background(background_fill); 

    	// set background 
    	editor.setBackground(background); 
    }
    
    
    private TextInputDialog createRegisterDialog( Stage primaryStage ) {
    	TextInputDialog dialog = new TextInputDialog( name );
    	dialog.initOwner( primaryStage );
    	dialog.initStyle(StageStyle.UTILITY);
    	dialog.setTitle(  "Enter an Transliteration Name" );
    	dialog.setContentText(  "Enter a name:" );
    	dialog.setHeaderText(null);
    	dialog.setGraphic(null);
    	dialog.setResizable(true);
    	
    	
    	Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
    	okButton.setDisable(false);

    	TextField nameField = dialog.getEditor();
    	nameField.textProperty().addListener((observable, oldValue, newValue) -> {
    		okButton.setDisable( newValue.trim().isEmpty() );
    	});
    	
    	return dialog;
    }
    
    public String getInText() {
    	if( scriptIn == null ) {
    		return "Unknown";
    	}
    	String scriptInMessage = scriptIn;
    	if(! "_base".equals( variantIn ) ) {
    		scriptInMessage +=  " / " + variantIn ;
    	}
    	return scriptInMessage;
    }
    
    public String getOutText() {
    	if( scriptOut == null ) {
    		return "Unknown";
    	}
    	String scriptOutMessage = scriptOut;
    	if(! "_base".equals( variantOut ) ) {
    		scriptOutMessage +=  " / " + variantOut ;
    	}
    	
    	return scriptOutMessage;
    }
    
    
    /*
    private Dialog<Pair<String,String>> createRegisterDialogOld() {   	
    	Dialog<Pair<String,String>> dialog = new Dialog<>();
    	dialog.initStyle(StageStyle.UTILITY);
    	dialog.setTitle( "Register Transliteration" );
    	dialog.setHeaderText("Register Transliteration in ICU");
    	dialog.setResizable(true);
    	 
    	Label label1 = new Label("Alias: ");
    	Label label2 = new Label("Direction: ");
    	TextField aliasField = new TextField();
    	String oldAlias = alias; // (String)getProperties().get( "alias" );
    	if( oldAlias != null ) {
    		aliasField.setText( oldAlias );
    	}
    	final ToggleGroup directionGroup = new ToggleGroup();
    	RadioButton forward = new RadioButton( "Forward" );
    	RadioButton bidirectional = new RadioButton( "Bidirectional" );
    	forward.setToggleGroup( directionGroup );
    	forward.setSelected( true );
    	bidirectional.setToggleGroup( directionGroup );
    	
    	if( "both".equals( (String)getProperties().get( "direction" ) ) ) {
        	bidirectional.setSelected( true );
    	}
    	else {
        	forward.setSelected( true );
    	}
    	         
    	GridPane grid = new GridPane();
    	grid.add(label1, 1, 1);  grid.add(aliasField, 2, 1, 2, 1);
    	
    	grid.add(label2, 1, 2);  grid.add(forward, 2, 2);  grid.add(bidirectional, 3, 2);
    	grid.setVgap(10);
    	
    	dialog.getDialogPane().setContent(grid);
    	         
    	ButtonType buttonTypeOk = new ButtonType( "Okay", ButtonData.OK_DONE );
    	ButtonType buttonTypeCancel = new ButtonType( "Cancel", ButtonData.CANCEL_CLOSE );
    	dialog.getDialogPane().getButtonTypes().addAll( buttonTypeOk, buttonTypeCancel );
    	
    	Node okButton = dialog.getDialogPane().lookupButton (buttonTypeOk );
    	okButton.setDisable(true);

    	aliasField.textProperty().addListener((observable, oldValue, newValue) -> {
    		okButton.setDisable( newValue.trim().isEmpty() );
    	});
    	 
    	dialog.setResultConverter( dialogButton -> {
    		if (dialogButton == buttonTypeOk) {
    			return new Pair<>( aliasField.getText(), ( forward.isSelected() ? "forward" : "both" ) );
    		}
    	 
    		return null;
    	});
    	
    	return dialog;
    }
    */ 
}
