package org.geez.ui.xliterator;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.apache.commons.io.FilenameUtils;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.StatusBar;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.geez.convert.DocumentProcessor;
import org.geez.convert.ProcessorManager;
import org.geez.convert.docx.DocxProcessor;
import org.geez.convert.fontsystem.ConvertDocxGenericUnicodeFont;
import org.geez.convert.fontsystem.ConvertFontSystem;
import org.geez.ui.Xliterator;

import com.google.gson.JsonObject;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ConvertFilesTab extends XliteratorTab {
	
	private final Button convertButton = new Button("Convert");
	private boolean openOutput = true;
	private String appendStyle = "Off";
    private CheckComboBox<String> documentFontsMenu = new CheckComboBox<String>();
	// private DocumentProcessor processor = null;
	private List<File> inputFileList = null;
    // private Converter converter = null;
    private Desktop desktop = Desktop.getDesktop();
	private StatusBar statusBar = null;
	
    private final String fileOutFontPref = "org.geez.ui.xliterator.convertFilesTab.fontOut";
	private Xliterator xlit = null; // used to get a handle on rules text


	public ConvertFilesTab(String title, Xliterator xlit) {
		super( title );
		this.xlit = xlit;
		this.defaultFontFamily = "Arial";
		checkPreferences();
	}
    
	
	/*
    // this is identical to the method in XliteratorTab, with the only difference being the
	// commented out .setOnAction line, see if this can be revised.
    protected ChoiceBox<String> createFontChoiceBox(String component, String defaultSelection) {  
    	ChoiceBox<String> choiceBox = new ChoiceBox<>();
    	for(String font: javafx.scene.text.Font.getFamilies() ) {
    		choiceBox.getItems().add( font );
    	}
    	choiceBox.getSelectionModel().select( defaultSelection );
        // choiceBox.setOnAction( evt -> setFont( choiceBox.getSelectionModel().getSelectedItem(), component ) );
        
        return choiceBox;    
    }
    */
    
    public void setFontFamily(StyleClassedTextArea component, String fontFamily) {
    	this.fontFamily = fontFamily;
    }
    
    
    ProcessorManager processorManager = null;
    public void setProcessor(ProcessorManager processorManager) {
    	this.processorManager = processorManager;
    }
    private void processFile(File inputFile, ListView<Label> listView, int listIndex) {

        try {
        	String inputFilePath   = inputFile.getPath();
        	String outputFilePath  = null;	
    		String extension       = FilenameUtils.getExtension( inputFilePath );
    		String editorRulesText = null;
    		String systemName      = null;
    		
    		if( selectedTransliteration.equals( Xliterator.useSelectedEdtior ) ) {
	    		EditorTab editorTab = xlit.getSelectedEditorTab();
	    		if( editorTab == null ) {
	    			// TODO: add error alert
	    			return;
	    		}
	    		editorRulesText = editorTab.getEditor().getText();
	    		// check if empty
	    		if( editorRulesText == null ) {
	    			// TODO: add error alert
	    			return;
	    		}
	    		
	    		systemName = editorTab.getTitle().replace( " ", "-" );
	    		systemName = systemName.replaceAll("\\.(\\w+)$", "" );
    		}
    		else {
    			systemName = scriptOut.replace( " ", "-" ) + variantOut.replace( " ", "-" ); // subvariant ?
    		}
    		
    		
    		DocumentProcessor processor = ( selectedTransliteration.equals( Xliterator.useSelectedEdtior )  )
    				? processorManager.getEditorTextProcessor( selectedTransliteration, transliterationDirection, extension, editorRulesText )
    				: processorManager.getFileProcessor( selectedTransliteration, transliterationDirection, extension )
    		;
    				
    		// a new converter instance is created for each file in a list. since if we can cache and reuse a converter.
    		// it may be necessary to reset the converter so it is in a neutral state
    		if ( extension.equals( "txt") ) {
            	outputFilePath = inputFilePath.replaceAll("\\.txt", "-" + systemName + ".txt");
    		}
    		else {
            	outputFilePath = inputFilePath.replaceAll("\\.docx", "-" + systemName + ".docx");
            	
    			ArrayList<String> targetTypefaces = new ArrayList<String>( documentFontsMenu.getCheckModel().getCheckedItems() );
    			
    			DocxProcessor docxProcessor = (DocxProcessor)processor;
    			ConvertDocxGenericUnicodeFont converter = (ConvertDocxGenericUnicodeFont)docxProcessor.getStashedConverter();
    			converter.setTargetTypefaces( targetTypefaces );
    			converter.setCaseOption( caseOption );
    			docxProcessor.setAppendOutput( appendStyle );
    			docxProcessor.setFontOut( (String)convertButton.getProperties().get("fontOut") );
    			docxProcessor.addConverter( (ConvertFontSystem)converter );
    		}
    		
        	File outputFile =  new File ( outputFilePath );
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
                processFile( file, listView, i );
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
    
	
	public void setComponents(MenuItem fileMenuItem, StatusBar statusBar, Stage stage) {
		this.statusBar = statusBar;
        ListView<Label> listView = new ListView<Label>();
        listView.setEditable(false);
        listView.setPrefHeight( 125 ); // 205 for screenshots
        listView.setPrefWidth( 310 );
        ObservableList<Label> data = FXCollections.observableArrayList();
        VBox listVBox = new VBox( listView );
        listView.autosize();
        
        ChoiceBox<String> outputFontMenu = createFontChoiceBox( null, (fontFamily==null)? defaultFontFamily : fontFamily );
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
        openFilesCheckbox.setOnAction( evt -> {
        	openOutput = openFilesCheckbox.isSelected();
        });
        /*
        openFilesCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> ov,
                Boolean old_val, Boolean new_val) {
                    openOutput = new_val.booleanValue();
            }
        });
        */
        openFilesCheckbox.setSelected(true);
        
        
        ChoiceBox<String> appendMenu = new ChoiceBox<String>();
        appendMenu.getItems().add( "Off" );
        appendMenu.getSelectionModel().select( "Off" );
        appendMenu.getItems().add( "On a New Line" );
        appendMenu.getItems().add( "  • On a New Line and with ()" );
        appendMenu.getItems().add( "With a space" );
        appendMenu.getItems().add( "  • With a space and with ()" );
        appendMenu.setOnAction( evt -> { appendStyle = appendMenu.getSelectionModel().getSelectedItem(); } );
        
        
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.SOMETIMES);
        Region optionSpacer = new Region();
        optionSpacer.setMinWidth( 10 );
        optionSpacer.setMaxWidth( 10 );
        HBox hbottomBox = new HBox( openFilesCheckbox, optionSpacer, new Label( "Append Output? "), appendMenu, bottomSpacer, convertButton );
        hbottomBox.setPadding(new Insets(4, 0, 4, 0));
        hbottomBox.setAlignment( Pos.CENTER_LEFT );

        VBox filesVbox = new VBox( filesTabMenuBox, listVBox, hbottomBox );
        this.setContent( filesVbox );
        final FileChooser fileChooser = new FileChooser();
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
        
        documentFontsMenu.setDisable( true );
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
    	
    	documentFontsMenu.getItems().clear();
    	documentFontsMenu.getItems().addAll( fonts );
        documentFontsMenu.setDisable( false );
    }

    
    public void setScriptIn(String scriptIn ) {
    	super.setScriptIn(scriptIn);
    	if( scriptIn.equals( Xliterator.useSelectedEdtior ) ) {
    		this.selectedTransliteration = scriptIn;
			convertButton.setDisable( false );
    	}
    	else {
			convertButton.setDisable( true );
    	}

    }
    
    
    public void setScriptOut(String scriptOut ) {
    	super.setScriptOut(scriptOut);
		convertButton.setDisable( true );
    }
    
    
    public void setVariantOut(String variantOut, String selectedTransliteration, String transliterationDirection, ArrayList<String> dependencies, String alias ) {
    	super.setVariantOut(variantOut, selectedTransliteration, transliterationDirection, dependencies, alias);

    	if( inputFileList != null ) {
    		convertButton.setDisable( false );
    	}
    }
    
    
    public void setTransliteration( JsonObject transliteration ) {
    	super.setTransliteration( transliteration);

    	if( inputFileList != null ) {
    		convertButton.setDisable( false );
    	}
    }
        

    public void saveDefaultFontSelections() {
        Preferences prefs = Preferences.userNodeForPackage( ConvertFilesTab.class );

        if( fontFamily != null ) {
        	prefs.put( fileOutFontPref, fontFamily );
        }
    }
    
    
    private boolean checkPreferences() {
        Preferences prefs = Preferences.userNodeForPackage( ConvertFilesTab.class );

        this.fontFamily = prefs.get( fileOutFontPref, null );
        
        return ( this.fontFamily == null );
    }
    
    
    public void setEditorTransliterationDirection( String direction ) {
    	if( selectedTransliteration.equals( Xliterator.useSelectedEdtior ) ) {
        	transliterationDirection = direction;
    	}
    }
}
