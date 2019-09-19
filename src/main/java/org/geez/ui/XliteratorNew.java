package org.geez.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.controlsfx.control.StatusBar;
import org.geez.convert.ProcessorManager;
import org.geez.transliterate.XliteratorConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

 

public final class XliteratorNew extends Application {
 
	private static final String VERSION = "v0.6.0";
    private Desktop desktop = Desktop.getDesktop();

	private String scriptIn  = null; // alphabetic based default
	private String scriptOut = null;
	private String variantOut = null;
	protected StatusBar statusBar = new StatusBar();
	private Menu inScriptMenu  = null;
	private Menu outVariantMenu = null;
	private Menu outScriptMenu  = null;
	private String selectedTransliteration = null;
	private String transliterationDirection = null;
    private String defaultFont    = null;
    MenuItem loadInternalMenuItem = new MenuItem( "Load Selected Transliteration" );
    private EditorTab editTab     = new EditorTab( "Mapping Editor" );
	ConvertTextTab textTab        = new ConvertTextTab( "Convert Text" );
	ConvertFilesTab filesTab      = new ConvertFilesTab( "Convert Files" );
	ProcessorManager processorManager = new ProcessorManager();
    
    private final int APP_WIDTH  = 800;
    private final int APP_HEIGHT = 800;
    
    private final String scriptInPreference   = "org.geez.preferences.scriptIn";
    private final String scriptOutPreference  = "org.geez.preferences.scriptOut";
    private final String variantOutPreference = "org.geez.preferences.variantOut";
    private final String transliterationIdPreference        = "org.geez.preferences.transliterationId";
    private final String transliterationDirectionPreference = "org.geez.preferences.transliterationDirection";
    
	private XliteratorConfig config = null;

	
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
	
	
    private static void configureFileChooserICU( final FileChooser fileChooser ) {      
    	fileChooser.setTitle( "View Word Files" );
        fileChooser.setInitialDirectory(
        		new File( System.getProperty( "user.home" ) )
        );                 
        fileChooser.getExtensionFilters().add(
        		new FileChooser.ExtensionFilter( "*.xml", "*.xml" )
        );
    }
    
    
    private Menu createInScriptsMenu(final Stage stage) {
    	Menu menu = new Menu( "Script _In" );
        ToggleGroup groupInMenu = new ToggleGroup();
        
        // Create menu from the scripts in the configuration file:
    	List<String> scripts = config.getInScripts();
    	for(String script: scripts) {
    		RadioMenuItem menuItem = new RadioMenuItem( script );
    		menuItem.setToggleGroup( groupInMenu );
    		menuItem.setOnAction( evt -> setScriptIn( script ) );
    		menu.getItems().add( menuItem );
    	}
    	
    	// Menu for the first edtior tab:
    	menu.getItems().add( new SeparatorMenuItem() );
    	RadioMenuItem editTabItem = new RadioMenuItem( "Use Editor" );
    	editTabItem.setOnAction( evt -> setUseEditor() );
    	menu.getItems().add( editTabItem );
        		
    	return menu;
    }
    
    
    private Menu createOutScriptsMenu(String outScript) {
   	 	outScriptMenu.getItems().clear();
   	 	scriptOut = null;
		outVariantMenu.getItems().clear();
		variantOut = null;
    	scriptOutText.setText( "[None]" );
    	variantOutText.setText( "[None]" );
    	
        ToggleGroup groupOutMenu = new ToggleGroup();
        
    	List<String> scripts = config.getOutScripts(outScript);
    	for(String script: scripts) {
    		RadioMenuItem menuItem = new RadioMenuItem( script );
    		menuItem.setToggleGroup( groupOutMenu );
    		menuItem.setOnAction( evt -> setScriptOut( script ) );
    		outScriptMenu.getItems().add( menuItem );
    	}
    	return outScriptMenu;
    }
    
    
    private Menu createOutVaraintsMenu(String outVariant) {
    	outVariantMenu.getItems().clear();
        ToggleGroup groupVariantOutMenu = new ToggleGroup();
             
    	JsonArray variants = config.getVariants(scriptIn, scriptOut);
    	for (int i = 0; i < variants.size(); i++) {
    		JsonObject variant = variants.get(i).getAsJsonObject();
    		if( variant.get("name") == null ) {
    			for(String subVariantKey: variant.keySet() ) {
    				Menu variantSubMenu = new Menu( subVariantKey );
    				JsonArray subvariants = variant.getAsJsonArray( subVariantKey );
    		    	for (int j = 0; j < subvariants.size(); j++) {
    		    		JsonObject subvariant = subvariants.get(j).getAsJsonObject();
    	    			String name = subvariant.get("name").getAsString();
    	        		RadioMenuItem menuItem = new RadioMenuItem( name );
    	        		menuItem.setToggleGroup( groupVariantOutMenu );
    	        		String transliterationID = subvariant.get( "path" ).getAsString();
    	        		String direction = subvariant.get( "direction" ).getAsString();
    	        		menuItem.setOnAction( evt -> { this.selectedTransliteration = transliterationID; this.transliterationDirection = direction; setVariantOut( subVariantKey + " - " + name ); });
    	        		menuItem.setId( transliterationID );
    	        		variantSubMenu.getItems().add( menuItem );
    		    	}
            		outVariantMenu.getItems().add( variantSubMenu );
    			}
    		} 
    		else if( variant.get("name") != null ) {
    			String name = variant.get("name").getAsString();
        		RadioMenuItem menuItem = new RadioMenuItem( name );
        		menuItem.setToggleGroup( groupVariantOutMenu );
        		String transliterationID = variant.get( "path" ).getAsString();
        		String direction = variant.get( "direction" ).getAsString();
        		menuItem.setOnAction( evt -> { this.selectedTransliteration = transliterationID; this.transliterationDirection = direction; setVariantOut( name ); } );
        		menuItem.setId( transliterationID );
        		outVariantMenu.getItems().add( menuItem );
    		}
    	}
    	if( (variants.size() == 0) ) {
    		// a json parse error should have occurred, 
    	}
    	
    	return outVariantMenu;
    }
    
    Stage primaryStage = null;
    
    @Override
    public void start(final Stage stage) {
    	try {
    		config = new XliteratorConfig(); 
    	}
    	catch(Exception ex) {
    		System.err.println( ex );
    		System.exit(0);
    	}
    	primaryStage = stage;
    	
        stage.setTitle( "Xliterator - An ICU Based Transliterator" );
        Image logoImage = new Image( ClassLoader.getSystemResourceAsStream( "images/Xliterator.png" ) );
        stage.getIcons().add( logoImage );
        String osName = System.getProperty("os.name");
        if( osName.equals("Mac OS X") ) {
            com.apple.eawt.Application.getApplication().setDockIconImage( SwingFXUtils.fromFXImage(logoImage, null) );  
            defaultFont = "Kefa";
        }
        textTab.setDefaultFont( defaultFont );
        
        
        // Create and configure menus:
        
        
        //
        //=========================== BEGIN FILE MENU =============================================
        //
        final Menu fileMenu = new Menu("_File");
        

		final Menu newMenu = new Menu( "New File" );
		final MenuItem xmlMenuItem = new MenuItem( "XML" );
		xmlMenuItem.setOnAction( evt -> createNewFile( "Untitled", "XML" ) );
		xmlMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
		final MenuItem txtMenuItem = new MenuItem( "TXT" );
		txtMenuItem.setOnAction( evt -> createNewFile( "Untitled", "TXT" ) );
		newMenu.getItems().addAll( xmlMenuItem, txtMenuItem );


        final MenuItem fileMenuItem = new MenuItem( "Select Files..." ); 
        fileMenuItem.setDisable( true );
        
        final MenuItem saveMenuItem = new MenuItem( "_Save" );
        saveMenuItem.setOnAction( actionEvent -> editTab.saveContent( stage, false ) );
        saveMenuItem.setDisable(true);
        saveMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        
        final MenuItem saveAsMenuItem = new MenuItem( "Save As..." );
        saveAsMenuItem.setOnAction( actionEvent ->  editTab.saveContent( stage, true ) );
        saveAsMenuItem.setDisable(true);
        saveAsMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN) );


    	// Add transliteration file selection option:
		final MenuItem openMenuItem = new MenuItem( "Open ICU File..." );
        openMenuItem.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                    	if(! checkUnsavedChanges() ) {
                    		return;
                    	}
                        final FileChooser fileChooser = new FileChooser();
                    	configureFileChooserICU(fileChooser);    
                    	File externalIcuFile = fileChooser.showOpenDialog( stage );
                        if( externalIcuFile == null ) {
                        	return;
                        }
                        try {
                        	// editor.replaceText( FileUtils.readFileToString(icuFile, StandardCharsets.UTF_8) );
                        	editTab.loadFile( externalIcuFile );
                        	setUseEditor(); // TODO: set transliteration direction?
                        	textTab.enableConvertForward( true );
                        	textTab.enableConvertReverse( false );
                        	if( editTab.getEditor().getText().contains( "↔" ) ) {
                        		transliterationDirection = "both";
                                textTab.enableConvertBoth( true );
                        	} else {
                        		transliterationDirection = "forward"; // TODO: confirm this, it might be reverse only
                        	}
                        	saveMenuItem.setDisable(false);
                        	saveAsMenuItem.setDisable(false);
                        }
                        catch(IOException ex) {
                        	errorAlert(ex, "Error opening: " + externalIcuFile.getName() );
                        }
                    }
                }
        );
        openMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
		
        loadInternalMenuItem.setDisable( true );
        loadInternalMenuItem.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                    	if(! checkUnsavedChanges() ) {
                    		return;
                    	}
                    	try {
                    		editTab.getEditor().loadResourceFile( selectedTransliteration );
                        	editTab.setText( selectedTransliteration );
                        	saveMenuItem.setDisable(false);
                        	saveAsMenuItem.setDisable(false);
                        }
                        catch(IOException ex) {
                        	errorAlert(ex, "Error opening: " + selectedTransliteration );
                        }
                    }
                }
        );
        loadInternalMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN));
        
        MenuItem quitMenuItem = new MenuItem( "_Quit Xliterator" );
        quitMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        quitMenuItem.setOnAction( evt -> {
    			Window window = primaryStage.getScene().getWindow();
    			window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
        });
        
        
        fileMenu.getItems().addAll( newMenu, fileMenuItem, openMenuItem, loadInternalMenuItem, saveMenuItem, saveAsMenuItem, new SeparatorMenuItem(), quitMenuItem ); 
        
        //
        //=========================== END FILE MENU =============================================
        
        
        //
        // We have prerequisite objects to configure the tabs and tabpane, lets do so now:
        //
        //=========================== BEGIN FILES TAB ===========================================
        filesTab.setClosable( false );
        filesTab.setProcessor(processorManager);
        filesTab.setComponents(fileMenuItem, statusBar, stage);
        filesTab.setOnSelectionChanged( evt -> {
        	if( filesTab.isSelected() ) {
        		fileMenuItem.setDisable( false );
        		openMenuItem.setDisable( true );
        		loadInternalMenuItem.setDisable( true );
        		saveMenuItem.setDisable( true );
        		saveAsMenuItem.setDisable( true );
        	}
        } );
        //=========================== END FILES TAB =============================================

        //=========================== BEGIN EDITOR TAB ===========================================
        editTab.setDefaultFont( defaultFont );
        editTab.setClosable( false ); // future set to true when multiple editors are supported
        editTab.setup(saveMenuItem, saveAsMenuItem);
        editTab.setOnSelectionChanged( evt -> {
        	if( editTab.isSelected() ) {
        		fileMenuItem.setDisable( true );
        		openMenuItem.setDisable( false );
        		if ( variantOut == null )
        			loadInternalMenuItem.setDisable( true );
        		else
        			loadInternalMenuItem.setDisable( false );
        		if(! "".equals( editTab.getEditor().getText() ) ) {
        			saveMenuItem.setDisable( false );
        			saveAsMenuItem.setDisable( false );
        		}
        	}
        } );
        //=========================== END EDITOR TAB ==============================================
        
        //=========================== BEGIN TEXT TAB ============================================
        textTab.setup( editTab.getEditor() );
        textTab.setDefaultFont( defaultFont );
        textTab.setClosable( false );
        textTab.setOnSelectionChanged( evt -> {
        	if( textTab.isSelected() ) {
        		fileMenuItem.setDisable( true );
        		openMenuItem.setDisable( true );
        		loadInternalMenuItem.setDisable( true );
        		saveMenuItem.setDisable( true );
        		saveAsMenuItem.setDisable( true );
        	}
        } );
        //=========================== END TEXT TAB =============================================
        
        // Setup the tabs:
        TabPane tabpane = new TabPane();
        tabpane.getTabs().addAll( editTab, textTab, filesTab );
        //
        //  End of Tab & TabPane setup
        //
        
        
        //
        //=========================== BEGIN SCRIPT MENUS =============================================
        // 
        inScriptMenu =  createInScriptsMenu( stage );
        outScriptMenu  = new Menu( "Script _Out" );
        outVariantMenu = new Menu( "_Variant" );
        //
        //=========================== END SCRIPT MENUS =============================================
        //
        
        
        //
        //=========================== BEGIN HELP MENU =============================================
        //
        final Menu helpMenu = new Menu( "Help" );
        final MenuItem aboutMenuItem = new MenuItem( "About" );
        helpMenu.getItems().add( aboutMenuItem );

        
        aboutMenuItem.setOnAction(
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(final ActionEvent e) {
			        Alert alert = new Alert(AlertType.INFORMATION);
			        alert.setTitle( "About The Xliterator" );
			        alert.setHeaderText( "An ICU based transliteration utility " + VERSION );
			        
			        FlowPane fp = new FlowPane();
			        Label label = new Label( "Visit the project homepage on" );
			        Hyperlink link = new Hyperlink("GitHub");
			        fp.getChildren().addAll( label, link);

			        link.setOnAction( (event) -> {
	                    alert.close();
	                    try {
		                    URI uri = new URI( "https://github.com/geezorg/Xliterator/" );
		                    desktop.browse( uri );
	                    }
	                    catch(Exception ex) {
	                    	
	                    }
			        });

			        alert.getDialogPane().contentProperty().set( fp );
			        alert.showAndWait();
                }
            }
        );
        
        final MenuItem demoMenuItem = new MenuItem( "Load Demo" );
        helpMenu.getItems().add( demoMenuItem );
        demoMenuItem.setOnAction( evt -> { loadDemo(); saveMenuItem.setDisable(false); saveAsMenuItem.setDisable(false); } );
        //
        //=========================== END HELP MENU =============================================
        //
        
        
        final Menu preferencesMenu = new Menu( "Preferences" );
        final MenuItem makeDefaultMenuItem = new MenuItem( "Save Default Mapping" );

        makeDefaultMenuItem.setOnAction( evt -> saveDefaultMapping() );
        final Menu caseConversionMenu = new Menu( "Convert Case" );
        final RadioMenuItem lowercaseMenuItem = new RadioMenuItem( "lowercase" );
        final RadioMenuItem uppercaseMenuItem = new RadioMenuItem( "UPPERCASE" );
        final RadioMenuItem titlecaseMenuItem = new RadioMenuItem( "Title Case" );
        final ToggleGroup caseGroup = new ToggleGroup();
        lowercaseMenuItem.setToggleGroup( caseGroup );
        uppercaseMenuItem.setToggleGroup( caseGroup );
        titlecaseMenuItem.setToggleGroup( caseGroup );
        
        lowercaseMenuItem.setOnAction( evt -> setCaseOption( lowercaseMenuItem ) );
        uppercaseMenuItem.setOnAction( evt -> setCaseOption( uppercaseMenuItem ) );
        titlecaseMenuItem.setOnAction( evt -> setCaseOption( titlecaseMenuItem ) );
        caseConversionMenu.getItems().addAll( lowercaseMenuItem, uppercaseMenuItem, titlecaseMenuItem );
        
        preferencesMenu.getItems().addAll( makeDefaultMenuItem, caseConversionMenu ); 
        
        // create a menubar 
        final MenuBar leftBar = new MenuBar();  
  
        // add menu to menubar 
        leftBar.getMenus().addAll( fileMenu, inScriptMenu, outScriptMenu , outVariantMenu );

        
        statusBar.setText( "" );
        updateStatusMessage();
     
        final MenuBar rightBar = new MenuBar();
        rightBar.getMenus().addAll( preferencesMenu, helpMenu );
        Region spacer = new Region();
        spacer.getStyleClass().add("menu-bar");
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        HBox menubars = new HBox(leftBar, spacer, rightBar);
        menubars.setAlignment( Pos.CENTER_LEFT);
 
        final BorderPane rootGroup = new BorderPane();
        rootGroup.setTop( menubars );
        rootGroup.setCenter( tabpane );
        rootGroup.setBottom( statusBar );
        rootGroup.setPadding( new Insets(8, 8, 8, 8) );
 
        Scene scene = new Scene(rootGroup, APP_WIDTH, APP_HEIGHT);
        ClassLoader classLoader = this.getClass().getClassLoader();
        scene.getStylesheets().add( classLoader.getResource("styles/xliterator.css").toExternalForm() );
        stage.setScene( scene ); 
        editTab.getEditor().setStyle( scene );
        editTab.getEditor().prefHeightProperty().bind( stage.heightProperty().multiply(0.8) );

        scene.getWindow().addEventFilter( WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent );
        
        // Check for a saved user menu preference:
        checkPreferences();
        	
        stage.show();
    }
    
    private void closeWindowEvent(WindowEvent event) {
    	if( editTab.hasUnsavedChanges() ) {
    		Alert alert = saveAndExitAlert( "Confirm Exit", "Exit without saving?", "Unsaved changes will be lost." );
            alert.initOwner( primaryStage.getOwner() );
            Optional<ButtonType> response = alert.showAndWait();

            if( response.isPresent() ) {
            	ButtonType type = response.get();
                if( type.getButtonData() == ButtonData.CANCEL_CLOSE ) {
                    event.consume();
                }
                else if( type.getButtonData() == ButtonData.YES ) {
                	editTab.saveContent( primaryStage, false );
                }
            }
    	}
    }

    
    private Alert saveAndExitAlert(String title, String header, String message) {
        ButtonType buttonTypeSaveAndExit = new ButtonType( "Save and Exit", ButtonData.YES );
        ButtonType buttonTypeExit = new ButtonType( "Exit", ButtonData.NO );
        ButtonType buttonTypeCancel = new ButtonType( "Cancel", ButtonData.CANCEL_CLOSE );

    	Alert alert = new Alert(AlertType.CONFIRMATION);
    	alert.setTitle( title );
    	alert.setHeaderText( header );
    	alert.setContentText( message );
    	
        alert.getButtonTypes().setAll( buttonTypeSaveAndExit, buttonTypeExit, buttonTypeCancel );
    	
    	return alert;
    }
    
    private Alert saveOrContinue(String title, String header, String message) {
        ButtonType buttonTypeSave = new ButtonType( "Yes", ButtonData.YES );
        ButtonType buttonTypeContinue = new ButtonType( "No", ButtonData.NO );
        ButtonType buttonTypeCancel = new ButtonType( "Cancel", ButtonData.CANCEL_CLOSE );

    	Alert alert = new Alert(AlertType.CONFIRMATION);
    	alert.setTitle( title );
    	alert.setHeaderText( header );
    	alert.setContentText( message );
    	
        alert.getButtonTypes().setAll( buttonTypeSave, buttonTypeCancel, buttonTypeContinue );
    	
    	return alert;
    }

    
    private boolean checkUnsavedChanges() {
    	if( editTab.hasUnsavedChanges() ) {
    		Alert alert = saveOrContinue( "Confirm Replace", "Save changes before loading?", "Unsaved changes will be lost." );
            alert.initOwner( primaryStage.getOwner() );
            Optional<ButtonType> response = alert.showAndWait();

            if( response.isPresent() ) {
            	ButtonType type = response.get();
                if( type.getButtonData() == ButtonData.CANCEL_CLOSE ) {
                	return false;
                }
                if( type.getButtonData() == ButtonData.YES ) {
                	editTab.saveContent( primaryStage, false );
                }
            } 
    	}
    	
    	return true;
    }
 
    
    public static void main(String[] args) {
        Application.launch(args);
    }

    private void saveDefaultMapping() {
        // Retrieve the user preference node for the package com.mycompany
        Preferences prefs = Preferences.userNodeForPackage( org.geez.transliterate.XliteratorConfig.class );

        prefs.put( scriptInPreference, scriptIn );
        prefs.put( scriptOutPreference, scriptOut );
        prefs.put( variantOutPreference, variantOut );
        prefs.put( transliterationIdPreference, selectedTransliteration );
        prefs.put( transliterationDirectionPreference, transliterationDirection );
    }
    
    private void checkPreferences() {
        // Retrieve the user preference node for the package com.mycompany
        Preferences prefs = Preferences.userNodeForPackage( org.geez.transliterate.XliteratorConfig.class );

        scriptIn   = prefs.get( scriptInPreference, null );
        if( scriptIn != null) {
        	setScriptIn( scriptIn );
        }
        
        scriptOut  = prefs.get( scriptOutPreference, null );
        if( scriptOut != null) {
        	setScriptOut( scriptOut );
        }
        
        
        selectedTransliteration  = prefs.get( transliterationIdPreference, null );
        transliterationDirection = prefs.get( transliterationDirectionPreference, null );
        
        variantOut = prefs.get( variantOutPreference, null );
        if( variantOut != null) {
        	setVariantOut( variantOut );
        }


    }
    
    Text scriptInText = new Text( "[None]" );
    Text scriptOutText = new Text( "[None]" );
    Text variantOutText = new Text( "[None]" );
    // status bar reference:
    // https://jar-download.com/artifacts/org.controlsfx/controlsfx-samples/8.40.14/source-code/org/controlsfx/samples/HelloStatusBar.java
    private void updateStatusMessage() {
    	scriptInText.setStyle( "-fx-font-weight: bold;" );
    	scriptOutText.setStyle( "-fx-font-weight: bold;" );
    	variantOutText.setStyle( "-fx-font-weight: bold;" );
    	scriptInText.setFill( Color.RED );
    	scriptOutText.setFill( Color.GREEN );
    	variantOutText.setFill( Color.BLUE );
        
    	TextFlow flowIn = new TextFlow();
        TextFlow flowOut = new TextFlow();
        TextFlow flowVOut = new TextFlow();

        Text in  = new Text("In: ");
        in.setStyle("-fx-font-weight: bold;");
       
        Text out = new Text("Out: ");
        out.setStyle("-fx-font-weight: bold;");
        
        Text vout = new Text("Variant: ");
        vout.setStyle("-fx-font-weight: bold;");
        
        flowIn.getChildren().addAll(in, scriptInText );
        flowOut.getChildren().addAll(out, scriptOutText );
        flowVOut.getChildren().addAll(vout, variantOutText );
       
        Separator separator1 = new Separator();
        separator1.setOrientation(Orientation.VERTICAL);
        separator1.setPadding( new Insets(0,0,0,6) );
        
        Separator separator2 = new Separator();
        separator2.setOrientation(Orientation.VERTICAL);
        separator2.setPadding( new Insets(0,0,0,6) );
        
        Separator separator3 = new Separator();
        separator2.setOrientation(Orientation.VERTICAL);
        separator2.setPadding( new Insets(0,0,0,6) );
        
        HBox hbox = new HBox();
        hbox.getChildren().addAll( flowIn, separator1, flowOut, separator2, flowVOut, separator3 );
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding( new Insets(2,0,0,0) );
        hbox.setSpacing(0.0);
        
        // working with a single flow leads to bad visual effects when the app size changes or
        // when the font name changes, so we use an hbox instead
        // flow.getChildren().addAll( in, systemInText, separator1, out, systemOutText, separator2 );
        
        statusBar.getLeftItems().add( hbox );
    }
    
    private void setScriptIn(String scriptIn) {
    	this.scriptIn = scriptIn;
    	scriptInText.setText( scriptIn );
    	createOutScriptsMenu( scriptIn );
        loadInternalMenuItem.setDisable( true );
    	filesTab.setScriptIn( scriptIn );
    	textTab.setScriptIn( scriptIn );
    	setMenuItemSelection( inScriptMenu, scriptIn );
    }
    private void setScriptOut(String scriptOut) {
    	this.scriptOut = scriptOut;
    	this.variantOut = null;
    	scriptOutText.setText( scriptOut );
    	variantOutText.setText( "[None]" );
    	createOutVaraintsMenu( scriptOut );
        loadInternalMenuItem.setDisable( true );
    	filesTab.setScriptOut( scriptIn );
    	textTab.setScriptOut(scriptOut);
    	setMenuItemSelection( outScriptMenu, scriptOut );
    }
    private void setVariantOut(String variantOut) {
    	this.variantOut = variantOut;
    	variantOutText.setText( variantOut );
        loadInternalMenuItem.setDisable( false );
    	filesTab.setVariantOut( variantOut, selectedTransliteration, transliterationDirection );
    	textTab.setVariantOut( variantOut, selectedTransliteration, transliterationDirection );
    	setMenuItemSelection( outVariantMenu, variantOut );
    }
    private String caseOption = null;
    private void setCaseOption(RadioMenuItem menuItem) {
    	String label = menuItem.getText().toLowerCase();
    	if( label.equals( caseOption ) ) {
    		// already selected, so this is a toggle off:
    		caseOption = null;
    		menuItem.setSelected( false );
    	}
    	else {
    		caseOption = label;
    	}
    	textTab.setCaseOption( caseOption );
    	filesTab.setCaseOption( caseOption );
    }
    
    private void setUseEditor() {    	
    	for(MenuItem item: inScriptMenu.getItems() ) {
    		if( item.getClass() == RadioMenuItem.class ) {
	    		RadioMenuItem rItem = (RadioMenuItem)item;
	    		if ( "Use Editor".equals( rItem.getText() ) ) {
	    			rItem.setSelected( true );
	    	    	selectedTransliteration = rItem.getText();
	    		}
	    		else {
	    			rItem.setSelected( false );
	    		}
    		}
    	}
    }
    
    
    private void setMenuItemSelection(Menu menu, String selection) {    	
    	for(MenuItem item: menu.getItems() ) {
    		if( item.getClass() == RadioMenuItem.class ) {
	    		RadioMenuItem rItem = (RadioMenuItem)item;
	    		if ( selection.equals( rItem.getText() ) ) {
	    			rItem.setSelected( true );
	    		}
	    		else {
	    			rItem.setSelected( false );
	    		}
    		}
    	}
    }
    
    
    private void createNewFile(String title, String type) {
    	if(! checkUnsavedChanges() ) {
    		return;
    	}

    	
    	String template = ( "XML".equals(type) )
    			? "templates/icu-xml-template.xml"
    			: "templates/icu-text-template.txt" 
    	;
    	try {
	    	editTab.getEditor().loadResourceFile( template );

	    	editTab.reset( "Untitled" );
    	}
    	catch(Exception ex) {
        	errorAlert(ex, "Error opening: " + template );
    	}
    	
    }
    
    
    private void loadDemo() {
    	if(! checkUnsavedChanges() ) {
    		return;
    	}
    	textTab.clearAll();
    	textTab.setTextIn( "ሰላም ዓለም" );
    	
    	setScriptIn( "Ethiopic" );
    	setScriptOut( "IPA" );
    	setVariantOut( "Amharic" );
    	selectedTransliteration = "am-am_FONIPA.xml";
    	transliterationDirection = "both";

    	for(MenuItem item: outScriptMenu.getItems() ) {
    		((RadioMenuItem)item).setSelected( false );
    	}
    	for(MenuItem item: outVariantMenu.getItems() ) {
    		((RadioMenuItem)item).setSelected( false );
    	}
    	
    	try {
    		editTab.getEditor().loadResourceFile( selectedTransliteration );
        	editTab.setText( selectedTransliteration );

        	setUseEditor();
        }
        catch(IOException ex) {
        	errorAlert(ex, "Error opening: " + selectedTransliteration );
        }
    }

}
