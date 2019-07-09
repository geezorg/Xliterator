package org.geez.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.commons.io.FilenameUtils;
import org.controlsfx.control.StatusBar;
import org.geez.convert.Converter;
import org.geez.convert.docx.ConvertDocxGenericUnicodeFont;
import org.geez.convert.text.ConvertText;
import org.geez.transliterate.XliteratorConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
 

public final class Xliterator extends Application {
 
	private static final String VERSION = "v0.1.0";
    private Desktop desktop = Desktop.getDesktop();

	private String scriptIn  = null; // alphabetic based default
	private String scriptOut = null;
	private String variantOut = null;
	private boolean openOutput = true;
	private List<File> inputList = null;
	protected StatusBar statusBar = new StatusBar();
	private boolean converted = false;
	private Menu outVariantMenu = null;
	private Menu outScriptMenu  = null;
	private final Button convertButton = new Button("Convert");
	private String selectedTransliteration = null;
	
	private XliteratorConfig config = new XliteratorConfig();
	
    private static void configureFileChooser( final FileChooser fileChooser ) {      
    	fileChooser.setTitle("View Word Files");
        fileChooser.setInitialDirectory(
        		new File( System.getProperty("user.home") )
        );                 
        fileChooser.getExtensionFilters().add(
        		new FileChooser.ExtensionFilter("*.docx", "*.docx")
        );
        fileChooser.getExtensionFilters().add(
        		new FileChooser.ExtensionFilter("*.txt", "*.txt")
        );
    }
    
    private Menu createInScriptsMenu() {
    	Menu menu = new Menu( "Script _In" );
        ToggleGroup groupInMenu = new ToggleGroup();
        
    	List<String> scripts = config.getInScripts();
    	for(String script: scripts) {
    		RadioMenuItem menuItem = new RadioMenuItem( script );
    		menuItem.setToggleGroup( groupInMenu );
    		menuItem.setOnAction( evt -> setScriptIn( script ) );
    		menu.getItems().add( menuItem );
    	}
    	return menu;
    }
    
    
    private Menu createOutScriptsMenu(String outScript) {
    	if( scriptOut != null ) {
    	 outScriptMenu.getItems().clear();
    	 scriptOut = null;
    	}
    	if( variantOut != null ) {
    		outVariantMenu.getItems().clear();
    		variantOut = null;
    	}
    	
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
    	if( variantOut != null )
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
    	        		menuItem.setOnAction( evt -> { this.selectedTransliteration = transliterationID; setVariantOut( subVariantKey + " - " + name ); });
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
        		menuItem.setOnAction( evt -> { this.selectedTransliteration = transliterationID; setVariantOut( name ); } );
        		menuItem.setId( variant.get( "path" ).getAsString() );
        		outVariantMenu.getItems().add( menuItem );
    		}
    	}
    	if( (variants.size() == 0) ) {
    		// a json parse error should have occurred, 
    	}
    	
    	return outVariantMenu;
    }
    
    
    @Override
    public void start(final Stage stage) {
        stage.setTitle("Xliterator - An ICU Based Transliterator");
        Image logoImage = new Image( ClassLoader.getSystemResourceAsStream("images/geez-org-avatar.png") );
        stage.getIcons().add( logoImage );
        String osName = System.getProperty("os.name");
        if( osName.equals("Mac OS X") ) {
            com.apple.eawt.Application.getApplication().setDockIconImage( SwingFXUtils.fromFXImage(logoImage, null) );      
        }

        Menu  inScriptMenu =  createInScriptsMenu();
        outScriptMenu = new Menu( "Script _Out" );
        outVariantMenu = new Menu( "_Variant" );



        ListView<Label> listView = new ListView<Label>();
        listView.setEditable(false);
        listView.setPrefHeight( 125 ); // 205 for screenshots
        listView.setPrefWidth( 310 );
        ObservableList<Label> data = FXCollections.observableArrayList();
        VBox listVBox = new VBox( listView );
        listView.autosize();
        
        
        convertButton.setDisable( true );
        convertButton.setOnAction( event -> {
        	convertButton.setDisable( true );
        	convertFiles( convertButton, listView ); 
        });

        final Menu fileMenu = new Menu("_File"); 
        final FileChooser fileChooser = new FileChooser();
        
        // create menu items 
        final MenuItem fileMenuItem1 = new MenuItem( "Select Files..." ); 
        fileMenuItem1.setOnAction(
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(final ActionEvent e) {
                	listView.getItems().clear();
                	configureFileChooser(fileChooser);    
                    inputList = fileChooser.showOpenMultipleDialog( stage );
                    
                    if ( inputList != null ) {
                    	for( File file: inputList) {
                    		Label rowLabel = new Label( file.getName() );
                    		data.add( rowLabel );
                    		Tooltip tooltip = new Tooltip( file.getPath() );
                    		rowLabel.setTooltip( tooltip );
                    	} 

                    	listView.setItems( data );
                    	if( variantOut != null ) {
                    		convertButton.setDisable( false );
                    	}
                    }
                }
            }
        );
        fileMenu.getItems().add( fileMenuItem1 ); 
        fileMenu.getItems().add( new SeparatorMenuItem() );
        
        MenuItem exitMenuItem = new MenuItem("Exit");
        exitMenuItem.setOnAction(actionEvent -> Platform.exit());
        fileMenu.getItems().add( exitMenuItem ); 
        
        
        final Menu helpMenu = new Menu( "Help" );
        final MenuItem aboutMenuItem = new MenuItem( "About" );
        helpMenu.getItems().add( aboutMenuItem );
        
        aboutMenuItem.setOnAction(
            new EventHandler<ActionEvent>() {
                @Override
                public void handle(final ActionEvent e) {
			        Alert alert = new Alert(AlertType.INFORMATION);
			        alert.setTitle( "About Legacy Ethiopic Docx Converter" );
			        alert.setHeaderText( "Legacy Ethiopic Font Converter for Docx " + VERSION );
			        
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
        
        
        // create a menubar 
        MenuBar leftBar = new MenuBar(); 
  
        // add menu to menubar 
        leftBar.getMenus().addAll( fileMenu, inScriptMenu, outScriptMenu , outVariantMenu);

        
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
        VBox vbottomBox = new VBox( hbottomBox, statusBar );

        statusBar.setText( "" );
        updateStatusMessage();

        
        MenuBar rightBar = new MenuBar();
        rightBar.getMenus().addAll( helpMenu );
        Region spacer = new Region();
        spacer.getStyleClass().add("menu-bar");
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        HBox menubars = new HBox(leftBar, spacer, rightBar);
        
 
        final BorderPane rootGroup =new BorderPane();
        rootGroup.setTop( menubars );
        rootGroup.setCenter( listVBox );
        rootGroup.setBottom( vbottomBox );
        rootGroup.setPadding( new Insets(8, 8, 8, 8) );
 
        stage.setScene(new Scene(rootGroup, 420, 220) ); // 305 for screenshots
        stage.show();
    }
 
    public static void main(String[] args) {
        Application.launch(args);
    }
 
    private void convertFiles(Button convertButton, ListView<Label> listView) {
    	
        if ( inputList != null ) {
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
            for (File file : inputList) {
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
    
    
    Converter converter = null;
    private void processFile(File inputFile, Button convertButton, ListView<Label> listView, int listIndex) {
        try {
        	String inputFilePath = inputFile.getPath();
        	String outputFilePath = inputFilePath.replaceAll("\\.docx", "-" + scriptOut.replace( " ", "-" ) + ".docx");
        	outputFilePath = inputFilePath.replaceAll("\\.txt", "-" + scriptOut.replace( " ", "-" ) + ".txt");
    		File outputFile = new File ( outputFilePath );
    		
    		String extension = FilenameUtils.getExtension( inputFilePath );
    		if ( extension.equals( "txt") ) {
    			converter = new ConvertText( inputFile, outputFile );
    		}
    		else {
    		/*
    		switch( systemIn ) {
		   		case brana:
		   			converter = new ConvertDocxBrana( inputFile, outputFile );
		   			break;
	    			
			   	case geezii:
		    		converter = new ConvertDocxFeedelGeezII( inputFile, outputFile );
		    		break;	
	    			
			   	case geezigna:
		    		converter = new ConvertDocxFeedelGeezigna( inputFile, outputFile );
		    		break;
		
			   	case geezbasic:
		    		converter = new ConvertDocxGeezBasic( inputFile, outputFile );
		    		break;    			
		    			
		   		case geeznewab:
	    			converter = new ConvertDocxFeedelGeezNewAB( inputFile, outputFile );
	    			break;

		    	case geeztypenet:
		    		converter = new ConvertDocxGeezTypeNet( inputFile, outputFile );
		   			break;

		    	case powergeez:
		    		converter = new ConvertDocxPowerGeez( inputFile, outputFile );
		   			break;

		    	case samawerfa:
		    		converter = new ConvertDocxSamawerfa( inputFile, outputFile );
		   			break;
		   			
		    	case visualgeez:
		    		converter = new ConvertDocxVisualGeez( inputFile, outputFile );
		    		break;
		   			
		    	case visualgeez2000:
		    		converter = new ConvertDocxVisualGeez2000( inputFile, outputFile );
		    		break;
    			
		    	default:
		    		System.err.println( "Unrecognized input system: " + systemIn );
		    		return;
    		}
    		*/
    			converter = new ConvertDocxGenericUnicodeFont(inputFile, outputFile, selectedTransliteration );
    			// ((ConvertDocxGenericUnicodeFont)converter).setFont( scriptOut );
    		}

    		
    		// references:
    		// https://stackoverflow.com/questions/49222017/javafx-make-threads-wait-and-threadsave-gui-update
    		// https://stackoverflow.com/questions/47419949/propagate-progress-information-from-callable-to-task
            Task<Void> task = new Task<Void>() {
                @Override protected Void call() throws Exception {
                	
                	updateProgress(0.0,1.0);
                	converter.progressProperty().addListener( 
                		(obs, oldProgress, newProgress) -> updateProgress( newProgress.doubleValue(), 1.0 )
                	);
                	updateMessage("[" +  (listIndex+1) + "/" + listView.getItems().size() + "]" );
                	converter.call();
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
                			Alert errorAlert = new Alert(AlertType.ERROR);
                			errorAlert.setHeaderText( "File IO Exception" );
                			errorAlert.setContentText( "An error has occured opening the file:\n" + ex );
                			errorAlert.showAndWait();
            			}
            		}
            		else {
            			Alert errorAlert = new Alert(AlertType.ERROR);
            			errorAlert.setHeaderText( "Output file not found" );
            			errorAlert.setContentText( "The output file \"" + outputFile.getPath() + "\" could not be found.  Conversation has likely failed." );
            			errorAlert.showAndWait();
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
    

    Text scriptInText = new Text( scriptIn );
    Text scriptOutText = new Text( scriptOut );
    Text variantOutText = new Text( variantOut );
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
    }
    private void setScriptOut(String scriptOut) {
    	this.scriptOut = scriptOut;
    	scriptOutText.setText( scriptOut );
    	variantOutText.setText( " " );
    	createOutVaraintsMenu( scriptOut );
		convertButton.setDisable( true );
    }
    private void setVariantOut(String variantOut) {
    	this.variantOut = variantOut;
    	variantOutText.setText( variantOut );
    	if( inputList != null ) {
    		convertButton.setDisable( false );
    	}
    }

}
