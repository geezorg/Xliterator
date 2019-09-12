package org.geez.ui;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.StatusBar;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.geez.convert.Converter;
import org.geez.convert.DocumentProcessor;
import org.geez.convert.docx.DocxProcessor;
import org.geez.convert.fontsystem.ConvertDocxGenericUnicodeFont;
import org.geez.convert.fontsystem.ConvertFontSystem;
import org.geez.convert.text.ConvertTextString;
import org.geez.convert.text.TextFileProcessor;
import org.geez.transliterate.XliteratorConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

 

public final class Xliterator extends Application {
 
	private static final String VERSION = "v0.5.0";
    private Desktop desktop = Desktop.getDesktop();

	private String scriptIn  = null; // alphabetic based default
	private String scriptOut = null;
	private String variantOut = null;
	private boolean openOutput = true;
	private List<File> inputFileList = null;
	private File icuFile = null;
	protected StatusBar statusBar = new StatusBar();
	private boolean converted = false;
	private Menu inScriptMenu  = null;
	private Menu outVariantMenu = null;
	private Menu outScriptMenu  = null;
	private final Button convertButton = new Button("Convert");
	private final Button convertButtonDown = new Button(); // ( "⬇" );
    private final Button convertButtonUp = new Button(); // ( "⬆" );
	private String selectedTransliteration = null;
	private String transliterationDirection = null;
    private ICUEditor editor = new ICUEditor();
    private final TextArea textAreaIn = new TextArea();
    private final TextArea textAreaOut = new TextArea();
    private String defaultFont = null;
    private CheckComboBox<String> documentFontsMenu = new CheckComboBox<String>();
    MenuItem loadInternalMenuItem = new MenuItem( "Load Selected Transliteration" );
    
    private final int APP_WIDTH  = 800;
    private final int APP_HEIGHT = 800;
    
	private XliteratorConfig config = null;
	private DocxProcessor processorDocx = new DocxProcessor();
	private TextFileProcessor processorTxt = new TextFileProcessor();

	
	private void errorAlert(Exception ex, String header ) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle( "An Exception has occured" );
        alert.setHeaderText( header );
        alert.setContentText( ex.getMessage() );
        alert.showAndWait();
	}

	
	private void errorAlert(String title, String message ) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle( title );
        alert.setHeaderText( message );
        alert.setContentText( message );
        alert.showAndWait();
	}
	
	// TODO: Disable "Save", "Save As" menus until the editor has content
	// Do not change the Editor Tab title if a file has never been loaded
	// Do not use FileWriter( icuFile ); if icuFile is still null
	private void saveEditorToFile(String newContent) throws IOException {
		  // Create file 
		  if( editor.getContent() == null )
			  return;
		  FileWriter fstream = new FileWriter( icuFile );
		  BufferedWriter out = new BufferedWriter( fstream );
		  out.write( newContent );
		  //Close the output stream
		  out.close();
	}
	
	private String saveAsEditorToFile(Stage stage, String newContent) throws IOException {
		FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save ICU File");
        File file = fileChooser.showSaveDialog( stage );
        if (file != null) {
  			  // Create file 
  			  FileWriter fstream = new FileWriter( file );
  			  BufferedWriter out = new BufferedWriter (fstream );
  			  out.write( newContent );
  			  //Close the output stream
  			  out.close();
  			  icuFile = file;
        }
        return file.getName();
    }
	
    private static void configureFileChooser( final FileChooser fileChooser ) {      
    	fileChooser.setTitle("View Word Files");
        fileChooser.setInitialDirectory(
        		new File( System.getProperty("user.home") )
        );                 
        fileChooser.getExtensionFilters().add(
        		new FileChooser.ExtensionFilter("*.docx & *.txt", "*.docx", "*.txt")
        );
    }
    
	
    private static void configureFileChooserICU( final FileChooser fileChooser ) {      
    	fileChooser.setTitle("View Word Files");
        fileChooser.setInitialDirectory(
        		new File( System.getProperty("user.home") )
        );                 
        fileChooser.getExtensionFilters().add(
        		new FileChooser.ExtensionFilter("*.xml", "*.xml")
        );
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
    private Menu createFontMenu(String component) {
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

    	/*
    	Menu menu = new Menu( "Font Size" );
        ToggleGroup groupInMenu = new ToggleGroup();
        
    	for(int i=10 ; i < 24; i++ ) {
    		RadioMenuItem menuItem = new RadioMenuItem( String.valueOf(i) );
    		menuItem.setToggleGroup( groupInMenu );
    		String size = String.valueOf(i);
    		menuItem.setOnAction( evt -> setFontSize( size ) ); 
    		if( i == 12 ) {
    			menuItem.setSelected( true );
    		}
    		menu.getItems().add( menuItem );
    	}
    	*/
        
        return menu;
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
    	editTabItem.setOnAction( 
    			new EventHandler<ActionEvent>() {
    				@Override
    				public void handle(final ActionEvent e) {
    					// TBD toggle off all other menu
    					/*
    					final FileChooser fileChooser = new FileChooser();
    					configureFileChooserICU(fileChooser);    
    					icuFile = fileChooser.showOpenDialog( stage );
    					*/
    				}	
    	});
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
    

    private void populateDocumentFontsMenu() {
    	ObservableList<String> fonts = FXCollections.observableArrayList();  	
    	try {
	    	for( File file: inputFileList) {
	    		String extension = FilenameUtils.getExtension( file.getPath() );
	    		if( "docx".equals( extension) ) {
					WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load( file );		
					MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
					for( String font: documentPart.fontsInUse() ) {
						fonts.add( font );
		       		}
	    		}
	    	}
    	}
    	catch( Docx4JException ex ) {
    		errorAlert(ex, "An error occured while reading documents." );
    	}
    	
    	documentFontsMenu.getItems().addAll( fonts );
        documentFontsMenu.setDisable( false );
    }
    
    private Tab editTab  = new Tab( "Mapping Editor" );
    @Override
    public void start(final Stage stage) {
    	try {
    		config = new XliteratorConfig(); 
    	}
    	catch(Exception ex) {
    		System.err.println( ex );
    		System.exit(0);
    	}
        stage.setTitle("Xliterator - An ICU Based Transliterator");
        Image logoImage = new Image( ClassLoader.getSystemResourceAsStream("images/Xliterator.png") );
        stage.getIcons().add( logoImage );
        String osName = System.getProperty("os.name");
        if( osName.equals("Mac OS X") ) {
            com.apple.eawt.Application.getApplication().setDockIconImage( SwingFXUtils.fromFXImage(logoImage, null) );  
            defaultFont = "Kefa";
        }
        
        TabPane tabpane = new TabPane();
    	Tab textTab  = new Tab( "Convert Text" );
        Tab filesTab = new Tab( "Convert Files" );
        editTab.setClosable( false ); // future set to true when multiple editors are supported
        textTab.setClosable( false );
        filesTab.setClosable( false );
    	
        tabpane.getTabs().addAll( editTab, textTab, filesTab );
        
        editor.textProperty().addListener( (obs, oldText, newText) -> {
    		String label = editTab.getText();
        	if( editor.hasContentChanged(newText) ) {
        		if( label.charAt(0) != '*' ) {
        			editTab.setText( "*" + editTab.getText() );
        		}
        	}
        	else {
        		if( label.charAt(0) == '*' ) {
        			editTab.setText( editTab.getText().substring(1) );
        		}
        	}
        });

        inScriptMenu =  createInScriptsMenu( stage );
        outScriptMenu  = new Menu( "Script _Out" );
        outVariantMenu = new Menu( "_Variant" );

        final Menu fileMenu = new Menu("_File"); 
        final FileChooser fileChooser = new FileChooser();
        
        ListView<Label> listView = new ListView<Label>();
        listView.setEditable(false);
        listView.setPrefHeight( 125 ); // 205 for screenshots
        listView.setPrefWidth( 310 );
        ObservableList<Label> data = FXCollections.observableArrayList();
        VBox listVBox = new VBox( listView );
        listView.autosize();
        // create menu items 
        final MenuItem fileMenuItem = new MenuItem( "Select Files..." ); 
        fileMenuItem.setOnAction(
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(final ActionEvent e) {
                	listView.getItems().clear();
                	configureFileChooser(fileChooser);    
                    inputFileList = fileChooser.showOpenMultipleDialog( stage );
                    
                    if ( inputFileList != null ) {
                    	for( File file: inputFileList) {
                    		Label rowLabel = new Label( file.getName() );
                    		data.add( rowLabel );
                    		Tooltip tooltip = new Tooltip( file.getPath() );
                    		rowLabel.setTooltip( tooltip );
                    	} 

                    	listView.setItems( data );
                    	if( variantOut != null ) {
                    		convertButton.setDisable( false );
                    	}
                    	populateDocumentFontsMenu();
                    }
                }
            }
        );
        fileMenuItem.setDisable( true );
    	// This should be moved under the top level "File" menu and is 

    	// Add transliteration file selection option:
		MenuItem openMenuItem = new MenuItem( "Open ICU File..." );
        openMenuItem.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                        final FileChooser fileChooser = new FileChooser();
                    	configureFileChooserICU(fileChooser);    
                        icuFile = fileChooser.showOpenDialog( stage );
                        try {
                        	// editor.replaceText( FileUtils.readFileToString(icuFile, StandardCharsets.UTF_8) );
                        	editor.loadFile( icuFile );
                        	editTab.setText( icuFile.getName() );
                        	setUseEditor(); // TBD: set transliteration direction?
                            convertButtonDown.setDisable( false );
                            convertButtonUp.setDisable( true );
                        	if( editor.getText().contains( "↔" ) ) {
                        		transliterationDirection = "both";
                                convertButtonUp.setDisable( false );
                        	} else {
                        		transliterationDirection = "forward"; // TODO: confirm this, it might be reverse only
                        	}
                        }
                        catch(IOException ex) {
                        	errorAlert(ex, "Error opening: " + icuFile.getName() );
                        }
                    }
                }
        );
		
        loadInternalMenuItem.setDisable( true );
        loadInternalMenuItem.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                    	try {
                        	editor.loadResourceFile( selectedTransliteration );
                        	editTab.setText( selectedTransliteration );
                        }
                        catch(IOException ex) {
                        	errorAlert(ex, "Error opening: " + icuFile.getName() );
                        }
                    }
                }
        );
        MenuItem saveMenuItem = new MenuItem( "Save" );
        saveMenuItem.setOnAction( actionEvent ->
        	{
        		try {
	        		if( icuFile == null) {
	        			saveAsEditorToFile( stage, editor.getText() );
	        		} 
	        		else {
	        			saveEditorToFile( editor.getText() );
	        		}
	        		editTab.setText( editTab.getText().substring(1) );
        		}
	        	catch (Exception ex){
	        		errorAlert( "Error occured saving file",  "An error occured while saving the file \"" + icuFile.getPath() + "\":\n" + ex.getMessage() );
	        	}
        	});
        MenuItem saveAsMenuItem = new MenuItem( "Save As..." );
        saveAsMenuItem.setOnAction( actionEvent ->
	    	{
	    		try {
	        		String newFileName = saveAsEditorToFile( stage, editor.getText() );
	        		editTab.setText( newFileName );
	    		}
	        	catch (Exception ex){
	        		errorAlert( "Error occured saving file",  "An error occured while saving the file \"" + icuFile.getPath() + "\":\n" + ex.getMessage() );
	        	}
	    	});
        
        MenuItem exitMenuItem = new MenuItem("Exit");
        exitMenuItem.setOnAction(actionEvent -> Platform.exit());
        fileMenu.getItems().addAll( fileMenuItem, openMenuItem, loadInternalMenuItem, saveMenuItem, saveAsMenuItem, new SeparatorMenuItem(), exitMenuItem ); 
        
        
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
        demoMenuItem.setOnAction( evt -> loadDemo() );
        
        // create a menubar 
        MenuBar leftBar = new MenuBar(); 
  
        // add menu to menubar 
        leftBar.getMenus().addAll( fileMenu, inScriptMenu, outScriptMenu , outVariantMenu );
        
        
        //=========================== BEGIN FILES TAB =============================================
        documentFontsMenu.setDisable( true );
       // MenuBar fileConverterMenuBar = new MenuBar();
        //Menu fileConverterFontMenu = createFontMenu( "file-converter" );
        //fileConverterMenuBar.getMenus().addAll( fileConverterFontMenu );
        ChoiceBox<String> outputFontMenu = createFontChoiceBox( "fileConverter", "Arial" );
        HBox filesTabMenuBox = new HBox( new Text( "Output Font:" ), outputFontMenu, new Text( "Document Fonts:" ), documentFontsMenu );
        filesTabMenuBox.setPadding(new Insets(2, 2, 2, 2));
        filesTabMenuBox.setSpacing(4);
        filesTabMenuBox.setAlignment( Pos.CENTER_LEFT );
        convertButton.setDisable( true );
        convertButton.setOnAction( event -> {
        	convertButton.setDisable( true );
        	convertButton.getProperties().put( "fontOut", outputFontMenu.getSelectionModel().getSelectedItem() );
        	convertFiles( convertButton, listView ); 
        });
        
        CheckBox openFilesCheckbox = new CheckBox( "Open file(s) after conversion?");
        openFilesCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> ov,
                Boolean old_val, Boolean new_val) {
                    openOutput = new_val.booleanValue();
            }
        });
        openFilesCheckbox.setSelected(true);
        
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.SOMETIMES);
        HBox hbottomBox = new HBox( openFilesCheckbox, bottomSpacer, convertButton );
        hbottomBox.setPadding(new Insets(4, 0, 4, 0));
        hbottomBox.setAlignment( Pos.CENTER_LEFT );

        VBox filesVbox = new VBox( filesTabMenuBox, listVBox, hbottomBox );
        filesTab.setContent( filesVbox );
        //=========================== END FILES TAB =============================================
        
        //=========================== BEGIN TEXT TAB ============================================
        
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
        textTab.setContent( textVbox );
        //=========================== END TEXT TAB ==============================================

        //=========================== BEGIN EDITOR TAB ===========================================

        Menu editorFontMenu = createFontMenu( "editor" );
        Menu editorFontSizeMenu = createFontSizeMenu();
        MenuBar editorMenutBar = new MenuBar();
        editorMenutBar.getMenus().addAll( editorFontMenu, editorFontSizeMenu );
        VBox editorVBox = new VBox( editorMenutBar, new StackPane( new VirtualizedScrollPane<>( editor ) ) );
        editTab.setContent( editorVBox );
       
        //=========================== END EDITOR TAB ==============================================

        
        // VBox vbottomBox = new VBox( hbottomBox, statusBar,  textAreaIn, hUpDownButtonBox, textAreaOut );
        
        editTab.setOnSelectionChanged( evt -> {
        	if( editTab.isSelected() ) {
        		fileMenuItem.setDisable( true );
        		openMenuItem.setDisable( false );
        		if ( variantOut == null )
        			loadInternalMenuItem.setDisable( true );
        		else
        			loadInternalMenuItem.setDisable( false );
        		saveMenuItem.setDisable( false );
        		saveAsMenuItem.setDisable( false );
        	}
        } );
        textTab.setOnSelectionChanged( evt -> {
        	if( textTab.isSelected() ) {
        		fileMenuItem.setDisable( true );
        		openMenuItem.setDisable( true );
        		loadInternalMenuItem.setDisable( true );
        		saveMenuItem.setDisable( true );
        		saveAsMenuItem.setDisable( true );
        	}
        } );
        filesTab.setOnSelectionChanged( evt -> {
        	if( filesTab.isSelected() ) {
        		fileMenuItem.setDisable( false );
        		openMenuItem.setDisable( true );
        		loadInternalMenuItem.setDisable( true );
        		saveMenuItem.setDisable( true );
        		saveAsMenuItem.setDisable( true );
        	}
        } );
        
        
        statusBar.setText( "" );
        updateStatusMessage();
     
        MenuBar rightBar = new MenuBar();
        rightBar.getMenus().addAll( helpMenu );
        Region spacer = new Region();
        spacer.getStyleClass().add("menu-bar");
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        HBox menubars = new HBox(leftBar, spacer, rightBar);
        menubars.setAlignment( Pos.CENTER_LEFT);
       // menubars.setPadding( new Insets(4, 4, 4, 4) );
 
        final BorderPane rootGroup = new BorderPane();
        rootGroup.setTop( menubars );
        // rootGroup.setCenter( listVBox );
        // rootGroup.setBottom( vbottomBox );
        rootGroup.setCenter( tabpane );
        rootGroup.setBottom( statusBar );
        rootGroup.setPadding( new Insets(8, 8, 8, 8) );
 
        Scene scene = new Scene(rootGroup, APP_WIDTH, APP_HEIGHT);
        scene.getStylesheets().add( classLoader.getResource("styles/xliterator.css").toExternalForm() );
        stage.setScene( scene ); 
        editor.setStyle( scene );
        editor.prefHeightProperty().bind( stage.heightProperty().multiply(0.8) );
        stage.show();
    }
 
    public static void main(String[] args) {
        Application.launch(args);
    }
 
    private void convertFiles(Button convertButton, ListView<Label> listView) {
        if ( inputFileList != null ) {
        	if( converted ) {
        		// this is a re-run, reset file names;
        		for(Label label: listView.getItems()) {
        			label.setStyle( "" );
        			label.setText( label.getText().replaceFirst( "\u2713 ", "" ) );
        		}
        		listView.refresh();
        		converted = false;
        	}
            int i = 0;
            for (File file : inputFileList) {
                processFile( file, convertButton, listView, i );
                i++;
                
                // this sleep seems to help slower CPUs
                // when a list of files is processed, and
                // avoids an exception from wordMLPackage.save( outputFile );
                try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
             }
            converted = true;
         } 
    }
    
    HashMap<String,ConvertTextString> textStringConverts = new HashMap<String,ConvertTextString>();    
    private void convertTextArea(TextArea textAreaIn, TextArea textAreaOut, String direction) {
    	String textIn = textAreaIn.getText();
    	if( textIn == null )
    		return;
    	
    	ConvertTextString stringConverter = null;
    	if( "Use Editor".equals( selectedTransliteration ) ) {
    		// do not save the converter because the text may change:
    		try {
    			stringConverter = new ConvertTextString( editor.getText() );
    		}
    		catch(Exception ex) {
            	errorAlert(ex, "Translteration Defition Error. Correct to Proceed." );
    			return;
    		}
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
    
    
    Converter converter = null;
	DocumentProcessor processor = null;
    private void processFile(File inputFile, Button convertButton, ListView<Label> listView, int listIndex) {
		File outputFile;
		
        try {
        	String inputFilePath = inputFile.getPath();
    		
    		String extension = FilenameUtils.getExtension( inputFilePath );
    		// a new converter instance is created for each file in a list. since if we can cache and reuse a converter.
    		// it may be necessary to reset the converter so it is in a neutral state
    		if ( extension.equals( "txt") ) {
            	String outputFilePath = inputFilePath.replaceAll("\\.txt", "-" + scriptOut.replace( " ", "-" ) + ".txt");
            	outputFile =  new File ( outputFilePath );
            	
            	if( selectedTransliteration.equals( "Use Editor") ) {
            		converter = new ConvertTextString( editor.getText() );	
            	}
            	else {
            		converter = new ConvertTextString( selectedTransliteration, transliterationDirection );
            	}
        		processorTxt.addConverter( (ConvertTextString)converter );
    			processor = processorTxt;
    		}
    		else {
            	String outputFilePath = inputFilePath.replaceAll("\\.docx", "-" + scriptOut.replace( " ", "-" ) + ".docx");
            	outputFile =  new File ( outputFilePath );

            	if( selectedTransliteration.equals( "Use Editor") ) {
            		converter = new ConvertDocxGenericUnicodeFont( editor.getText() );	
            	}
            	else {
            		converter = new ConvertDocxGenericUnicodeFont( selectedTransliteration, transliterationDirection );
            	}


    			ArrayList<String> targetTypefaces = new ArrayList<String>( documentFontsMenu.getCheckModel().getCheckedItems() );
    			((ConvertDocxGenericUnicodeFont)converter).setTargetTypefaces( targetTypefaces );

    			processorDocx.setFontOut( (String)convertButton.getProperties().get("fontOut") );
        		processorDocx.addConverter( (ConvertFontSystem)converter );
    			processor = processorDocx;
    		}
    		
    		processor.setFiles( inputFile, outputFile );

    		
    		
    		// references:
    		// https://stackoverflow.com/questions/49222017/javafx-make-threads-wait-and-threadsave-gui-update
    		// https://stackoverflow.com/questions/47419949/propagate-progress-information-from-callable-to-task
            Task<Void> task = new Task<Void>() {
                @Override protected Void call() throws Exception {
                	
                	updateProgress(0.0,1.0);
                	processor.progressProperty().addListener( 
                		(obs, oldProgress, newProgress) -> updateProgress( newProgress.doubleValue(), 1.0 )
                	);
                	updateMessage("[" +  (listIndex+1) + "/" + listView.getItems().size() + "]" );
                	processor.call();
                	updateProgress(1.0, 1.0);

    				done();
            		return null;
                } 
            };
            
            statusBar.progressProperty().bind( task.progressProperty() );
            statusBar.textProperty().bind( task.messageProperty() );
            
           	// remove bindings again
            task.setOnSucceeded( event -> { 
            	statusBar.progressProperty().unbind();
            	if ( openOutput ) {
            		if ( outputFile.exists() ) {
            			try { 
            				desktop.open( outputFile ); 
            			}
            			catch(IOException ex) {
            				errorAlert( "File IO Exception", "An error has occured opening the file:\n" + ex  );
            			}
            		}
            		else {
            			errorAlert( "Output file not found", "The output file \"" + outputFile.getPath() + "\" could not be found.  Conversation has likely failed." );
            		}
            	}
            	Label label = listView.getItems().get( listIndex );
                label.setText( "\u2713 " + label.getText() );
                label.setStyle( "-fx-font-style: italic;" );
                listView.refresh();
                convertButton.setDisable( false );
            
            });
            Thread convertThread = new Thread(task);
            convertThread.start();
            
            //convertThread.join();

        }
        catch (Exception ex) {
        	Logger.getLogger( Xliterator.class.getName() ).log( Level.SEVERE, null, ex );
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
        
        // working with a single flow leads to bad visual effects when the app size changes when the
        // font name changes, so we use an hbox instead
        // flow.getChildren().addAll( in, systemInText, separator1, out, systemOutText, separator2 );
        
        statusBar.getLeftItems().add( hbox );
    }
    private void setScriptIn(String scriptIn) {
    	this.scriptIn = scriptIn;
    	scriptInText.setText( scriptIn );
    	createOutScriptsMenu( scriptIn );
		convertButton.setDisable( true );
        convertButtonDown.setDisable( true );
        loadInternalMenuItem.setDisable( true );
    }
    private void setScriptOut(String scriptOut) {
    	this.scriptOut = scriptOut;
    	this.variantOut = null;
    	scriptOutText.setText( scriptOut );
    	variantOutText.setText( "[None]" );
    	createOutVaraintsMenu( scriptOut );
		convertButton.setDisable( true );
        convertButtonDown.setDisable( true );
        loadInternalMenuItem.setDisable( true );
    }
    private void setVariantOut(String variantOut) {
    	this.variantOut = variantOut;
    	variantOutText.setText( variantOut );
    	if( inputFileList != null ) {
    		convertButton.setDisable( false );
    	}
        convertButtonDown.setDisable( false );
        loadInternalMenuItem.setDisable( false );
        if( "both".equals( transliterationDirection ) ) {
        	convertButtonUp.setDisable( false );
        }
        else {
        	convertButtonUp.setDisable( true );        	
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
    	else if( "fileConverter".equals( component) ) {
    		
    	}
    	else {
    		// set the font in all components, unless already set for the text areas
    		String fontSize = (String) editor.getProperties().get("font-size");
    		editor.setStyle( "-fx-font-family: '" + font + "'; -fx-font-size: " + fontSize + ";" );
    		
    		if( textAreaIn.getProperties().get( "font-family") == null ) {
    			textAreaIn.setStyle( "-fx-font-family: '" + font + "'; -fx-font-size: " + fontSize + ";" ); 
    		}
    		if( textAreaOut.getProperties().get( "font-family") == null ) {
    			textAreaOut.setStyle( "-fx-font-family: '" + font + "'; -fx-font-size: " + fontSize + ";" ); 
    		}
    	}
    }
    
    
    private void setFontSize(String fontSize) {        
		String fontFamily = (String) editor.getProperties().get("font-family");
		editor.setStyle( "-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + ";" );
		editor.getProperties().put( "font-size", fontSize );
		
		if( textAreaIn.getProperties().get( "font-size") == null ) {
			textAreaIn.setStyle( "-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + ";" ); 
		}
		if( textAreaOut.getProperties().get( "font-size") == null ) {
			textAreaOut.setStyle( "-fx-font-family: '" + fontFamily + "'; -fx-font-size: " + fontSize + ";" ); 
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
    
    private void loadDemo() {
    	textAreaIn.clear();
    	textAreaOut.clear();
    	textAreaIn.setText( "ሰላም ዓለም" );
    	
    	setScriptIn( "Ethiopic" );
    	setScriptOut( "IPA" );
    	setVariantOut( "Amharic" );
    	selectedTransliteration = "am-am_FONIPA.xml";
    	transliterationDirection = "both";

    	setUseEditor();

    	for(MenuItem item: outScriptMenu.getItems() ) {
    		((RadioMenuItem)item).setSelected( false );
    		/*
    		RadioMenuItem rItem = (RadioMenuItem)item;
    		if ( "IPA".equals( rItem.getText() ) ) {
    			rItem.setSelected( true );
    		}
    		*/
    	}
    	for(MenuItem item: outVariantMenu.getItems() ) {
    		((RadioMenuItem)item).setSelected( false );
    		/*
    		RadioMenuItem rItem = (RadioMenuItem)item;
    		if ( "Amharic".equals( rItem.getText() ) ) {
    			rItem.setSelected( true );
    		}
    		*/
    	}
    	
    	
    	try {
        	editor.loadResourceFile( selectedTransliteration );
        	editTab.setText( selectedTransliteration );
        }
        catch(IOException ex) {
        	errorAlert(ex, "Error opening: " + icuFile.getName() );
        }
    }

}
