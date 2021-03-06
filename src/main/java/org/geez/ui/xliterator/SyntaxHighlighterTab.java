package org.geez.ui.xliterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geez.ui.Xliterator;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SyntaxHighlighterTab extends XliteratorTab {

	private boolean unsavedChanges = false;
	private boolean loaded = false;
	private String defaultStylesheet = "styles/icu-highlighting.css";
	private String userStylesheet    = "icu-highlighting.css";
	private String tempStylesheet    = "temp-highlighting.css";
	private String xlitStylesheet    = "styles/xliterator.css";
	private String exportStylesheet  = "xliterator-highlighting.css";
	
	private static final Pattern classPattern = Pattern.compile( "(#icuEditor|\\.paragraph-text) \\.(\\w+)(\\s+)?\\{" );
	/*
	private static final Pattern colorPattern = Pattern.compile( "-fx-fill: (#?\\w+);" );
	private static final Pattern boldPattern = Pattern.compile( "-fx-font-weight: bold;" );
	private static final Pattern italicattern = Pattern.compile( "-fx-font-style: italic;" );
	private static final Pattern underlinePattern = Pattern.compile( "-fx-underline: true;" );
	*/
	private static final Pattern stylePattern = Pattern.compile(
			"(?<COLOR>-fx-fill: ([#\\w]+);)"
			+ "|(?<BOLD>-fx-font-weight: bold;)"
			+ "|(?<ITALIC>-fx-font-style: italic;)"
			+ "|(?<UNDERLINE>-fx-underline: true;)"
	);
	
    private final String editorBackgroundColor = "org.geez.ui.xliterator.editor.background.color";
    private String bgcolor = null;
	
	private HashMap<String,ArrayList<Object>> styles = new HashMap<String,ArrayList<Object>>();
	private HashMap<String,ArrayList<Object>> updatedStyles = new HashMap<String,ArrayList<Object>>();
	
	private ArrayList<String> cssOrder = new ArrayList<String>(
			Arrays.asList(
				"forwardArrow", "reverseArrow", "bothArrow",
				"variable", "comment", "directive", "identifier",
				"paren", "tagmark", "anytag", "attribute",
				"avalue", "cdata"
			)
	);
    
	private Xliterator xlit = null;

	public SyntaxHighlighterTab( String title ) {
		super(title);
	}
	
	
	public void reset( String title ) {
		unsavedChanges = false;
	}
    
	
	public boolean isLoaded() {
		return loaded;
	}
    
    private Dialog<Color> createColorPickerDialog( String action, String question, Color _default ) {
        Dialog<Color> dialog = new Dialog<Color>();
        dialog.setTitle(action);
        dialog.setHeaderText(question);
        ColorPicker picker = new ColorPicker( _default );

        dialog.getDialogPane().setContent( picker );

        dialog.getDialogPane().getButtonTypes().addAll( ButtonType.OK, ButtonType.CANCEL );

        dialog.setResultConverter(buttonType -> {
            if (buttonType.equals( ButtonType.OK ) ) {
            	// setRubricationColor( silt, picker.getValue() );
                return picker.getValue();
            } else {
                return null;
            }
        });
        return dialog;
    }

	public void readStyleSheet(InputStream inputStream) {
		try {
			styles.clear();
			BufferedReader br = new BufferedReader( new InputStreamReader(inputStream, "UTF-8") );
			String line = null, cssClass = null, data = null;
			
			while ( (line = br.readLine()) != null ) {
				Matcher classMatcher = classPattern.matcher( line );
				if( classMatcher.matches() ) {
					cssClass = classMatcher.group(2);

					while (! "}".equals( (line = br.readLine()) ) ) {
						Matcher styleMatcher = stylePattern.matcher( line );
						while( styleMatcher.find() ) {
							if( styleMatcher.group("COLOR") != null) {
								data = styleMatcher.group(2);
								styles.put( cssClass, new ArrayList<Object>( Arrays.asList( data, false, false, false) ) );
							}
							else if( styleMatcher.group("BOLD") != null) {
								styles.get( cssClass ).set( 1, true );
							}
							else if( styleMatcher.group("ITALIC") != null) {
								styles.get( cssClass ).set( 2, true );
							}
							else if( styleMatcher.group("UNDERLINE") != null) {
								styles.get( cssClass ).set( 3, true );
							}
						}
					}
				}
			}
			br.close();
			
			loaded = true;
		}
		catch(Exception ex) {
			System.err.println( ex );
		}
	}
	
 
    
    private String getRGBString( Color color ) {
    	return String.format("#%02X%02X%02X",
    		    (int)(color.getRed()*255),
    		    (int)(color.getGreen()*255),
    		    (int)(color.getBlue()*255) );
    }
    
    private void setStyle(Label label, ArrayList<Object> list ) {
    	String style = "-fx-text-fill: " + list.get(0) + "; ";
    	
    	if( (boolean)list.get(1) ) {
    		style += "-fx-font-weight: bold; " ;
    	}

    	if( (boolean)list.get(2) ) {
    		style += "-fx-font-style: italic; " ;
    	}
    	
    	if( (boolean)list.get(3) ) {
    		style += "-fx-underline: true;" ;
    	}
    	
    	label.setStyle( style );
    }
    
    public void load( final Stage stage, final Xliterator xlit ) throws IOException {
    	this.xlit = xlit;
		if( styles.isEmpty() ) {
			InputStream inputStream = null;
			
			File userStylesFile = new File( userXlitPath + "/" + userStylesheet );
			// String base  = StringUtils.substringBefore(userStylesheet, ".css");
			// Path userStylesheetPath = Files.createTempFile( Paths.get( userXlitPath ), base, ".css" );
			if( userStylesFile.exists() ) {
				inputStream = new FileInputStream( userStylesFile );
			}
			
			if( inputStream == null ) {
				ClassLoader classLoader = this.getClass().getClassLoader();
				inputStream = classLoader.getResourceAsStream( defaultStylesheet );			
			}
			readStyleSheet( inputStream );
		}
		else {
			// this is a reload
			((GridPane)this.getContent()).getChildren().clear();
			this.setContent( null );
		}
		unsavedChanges = false;
		
		
		GridPane gridPane = createGridPane();
		VBox vbox = new VBox();

		Button resetButton   = new Button( "Reset"  );
		Button applyButton   = new Button( "Apply"  );
		Button saveButton    = new Button( "Save"   );
		Button exportButton  = new Button( "Export" );
		Button importButton  = new Button( "Import" );
		Button defaultButton = new Button( "Load Default" );
		
		HBox ioBox = new HBox();
		ioBox.getChildren().addAll( defaultButton, importButton, exportButton );
		ioBox.setAlignment( Pos.CENTER_LEFT );
		
		HBox controlBox = new HBox();
		controlBox.getChildren().addAll( resetButton, applyButton, saveButton );
		controlBox.setAlignment( Pos.CENTER_RIGHT );
		
		HBox toolbar = new HBox();
		toolbar.setMaxWidth( 600 );
		Region spacer = new Region();
		HBox.setHgrow( spacer, Priority.ALWAYS );
		
		toolbar.getChildren().addAll( ioBox, spacer, controlBox );

		
		vbox.getChildren().addAll( gridPane, toolbar );


		resetButton.setOnAction( evt -> {
				VBox vvbox = (VBox)this.getContent();
				vvbox.getChildren().set( 0, createGridPane() );
		});
		
		
		applyButton.setOnAction( evt -> {
				applyStylesheet( stage.getScene(), tempStylesheet, true );
				ArrayList<EditorTab> editorTabs = xlit.getEditorTabs();
				for( EditorTab editorTab: editorTabs ) {
					editorTab.setBackgroundColor( bgcolor );
					editorTab.setStyle( "" );
				}
				xlit.getConvertTextTab().setBackgroundColor( bgcolor );
		});
		
		saveButton.setOnAction( evt -> {
				saveStylesheet( stage.getScene(), userStylesheet, true );
		        Preferences prefs = Preferences.userNodeForPackage( EditorTab.class );
		        prefs.put( editorBackgroundColor, bgcolor );
		});
		
		exportButton.setOnAction(
				evt -> exportStylesheet( stage )
		);
		
		importButton.setOnAction( evt -> {
				importStylesheet( stage );
				VBox vvbox = (VBox)this.getContent();
				vvbox.getChildren().set( 0, createGridPane() );
		});
		
		
		defaultButton.setOnAction( evt -> {
				reloadDefault( stage.getScene() );
				VBox vvbox = (VBox)this.getContent();
				vvbox.getChildren().set( 0, createGridPane() );
		    	setBackgroundButtonColor( "white" );
		});
		

		this.setContent( vbox );
    }
	
    private void copyStyles() {
    	
    	updatedStyles.clear();
    	
		for(String style: styles.keySet()) {
			ArrayList<Object> newList = new ArrayList<Object>( styles.get( style ) );
			updatedStyles.put( style, newList );
			
		}
    	
    }
    
    private final String buttonStyleBase = "-fx-border-color: gray; -fx-padding: 10 20 10 20; -fx-background-radius: 5,4,3,5; -fx-background-insets: 0,1,2,0; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-color: " ;
    private final Button backgroundColorButton = new Button();
    private void setBackgroundButtonColor(String colorValue) {
    	backgroundColorButton.getProperties().put( "color" , Color.valueOf( colorValue ) );
    	backgroundColorButton.setStyle( buttonStyleBase + colorValue + ";" );
    }
    
	private GridPane createGridPane() {

		copyStyles();
		
		GridPane gridPane = new GridPane();

		gridPane.setPadding(new Insets(20,10,10,10));
		gridPane.setMaxWidth( 600 );
		gridPane.setMaxHeight( 400 );
		gridPane.setHgap( 10.0 );
		gridPane.setVgap( 20.0 );
		gridPane.setStyle( "-fx-font-family: \"Arial\"; -fx-border-color: black; -fx-border-width: 2px;");
	    
		
		int column = 0, row = 0;
		
		//=====================BACKGROUND COLOR========================================
		// Background color is a special case where we could not set the color in a stylesheet (check again...)
		// so we save it into a preference:
	    Preferences prefs = Preferences.userNodeForPackage( EditorTab.class );
		String colorValue = bgcolor = prefs.get( editorBackgroundColor, "white" );
		setBackgroundButtonColor( colorValue );
		backgroundColorButton.setOnAction( evt -> {
			Color color = (Color)backgroundColorButton.getProperties().get( "color" );
        	Dialog<Color> d = createColorPickerDialog( "Background Color", "Background Color", color );
        	Optional<Color> result = d.showAndWait();
        	if ( result.isPresent() ) {
         		String newColor = getRGBString( result.get() );
        		bgcolor = newColor;
				// colorButton1.setText( newColor );
        		backgroundColorButton.setStyle( buttonStyleBase + newColor + ";" );
        		backgroundColorButton.getProperties().put( "color" ,result.get() );
        	}
		});
	    
		Label hlabel = new Label( "Background: " );
		hlabel.setStyle( "-fx-font-weight: bold;" );
		HBox lbox = new HBox( hlabel );
		lbox.setAlignment( Pos.CENTER_RIGHT );
		
		HBox sbox = new HBox();
		sbox.getChildren().addAll( backgroundColorButton );
		sbox.setAlignment( Pos.CENTER_LEFT);
		
		gridPane.add( lbox, column,     row, 1, 1 );
		gridPane.add( sbox, (column+1), row, 1, 1 );
		column += 2;
		
		//=====================FOREGROUND COLOR========================================

		ArrayList<Object> list = styles.get( "text" );
		colorValue = (String)list.get(0);
		final Button foregroundColorButton = new Button(  );
		foregroundColorButton.getProperties().put( "color" , Color.valueOf( colorValue ) );
		foregroundColorButton.setStyle( buttonStyleBase + colorValue + ";" );
		foregroundColorButton.setOnAction( evt -> {
			Color color = (Color)foregroundColorButton.getProperties().get( "color" );
        	Dialog<Color> d = createColorPickerDialog( "Foreground Color", "Foreground Color", color );
        	Optional<Color> result = d.showAndWait();
        	if ( result.isPresent() ) {
        		String newColor = getRGBString( result.get() );
				ArrayList<Object> ulist = updatedStyles.get( "text" );
				ulist.set( 0, newColor );
				// colorButton2.setText( newColor );
				foregroundColorButton.setStyle( buttonStyleBase + newColor + ";" );
				foregroundColorButton.getProperties().put( "color" ,result.get() );
        	}
		});
		

		hlabel = new Label( "Foreground: " );
		hlabel.setStyle( "-fx-font-weight: bold;" );
		lbox = new HBox( hlabel );
		lbox.setAlignment( Pos.CENTER_RIGHT );
		
		sbox = new HBox();
		sbox.getChildren().addAll( foregroundColorButton );
		sbox.setAlignment( Pos.CENTER_LEFT);
		
		gridPane.add( lbox, column,     row, 1, 1 );
		gridPane.add( sbox, (column+1), row, 1, 1 );
		column++;
		
		//=====================SYNTAX HIGHLIGHT COLORS=================================
		
		column = 0;
		row = 1;
		
		for(String style : cssOrder) {
			if( "text".equals( style) ) {
				continue;
			}
			list = styles.get( style );
			Label label = new Label( style + ":  ");
			setStyle( label, list );

			ToggleButton bold = new ToggleButton( "B" );
			bold.setStyle( "-fx-font-weight: bold;" );
			bold.setOnAction( evt -> {
				ArrayList<Object> ulist = updatedStyles.get( style );
				ulist.set( 1, bold.isSelected() );
				setStyle( label,  ulist );
			});
			if( (boolean)list.get(1) ) {
				bold.setSelected( true );
			}
			ToggleButton italic = new ToggleButton( "I" );
			italic.setStyle( "-fx-font-style: italic;" );
			italic.setOnAction( evt -> {
				ArrayList<Object> ulist = updatedStyles.get( style );
				ulist.set( 2, italic.isSelected() );
				setStyle( label,  ulist );
			});
			if( (boolean)list.get(2) ) {
				italic.setSelected( true );
			}
			ToggleButton underline = new ToggleButton( "U" );
			underline.setStyle( "-fx-underline: true;" );
			underline.setOnAction( evt -> {
				ArrayList<Object> ulist = updatedStyles.get( style );
				ulist.set( 3, underline.isSelected() );
				setStyle( label,  ulist );
			});
			if( (boolean)list.get(3) ) {
				underline.setSelected( true );
			}
			colorValue = (String)list.get(0);
			Button colorButton = new Button ( colorValue );
			colorButton.getProperties().put( "color" , Color.valueOf( colorValue ) );
			colorButton.setStyle( "-fx-text-fill: " + colorValue + ";" );
			colorButton.setOnAction( evt -> {
				Color color = (Color)colorButton.getProperties().get( "color" );
	        	Dialog<Color> d = createColorPickerDialog( style + " Color", style + " Color", color );
	        	Optional<Color> result = d.showAndWait();
	        	if ( result.isPresent() ) {
	        		String newColor = getRGBString( result.get() );
					ArrayList<Object> ulist = updatedStyles.get( style );
					ulist.set( 0, newColor );
					setStyle( label,  ulist );
					colorButton.setText( newColor );
					colorButton.setStyle( "-fx-text-fill: " + newColor + ";" );
					colorButton.getProperties().put( "color", result.get() );
	        	}
			});
			
			
			lbox = new HBox( label );
			lbox.setAlignment( Pos.CENTER_RIGHT );
			
			sbox = new HBox();
			sbox.getChildren().addAll( colorButton, bold, italic, underline );
			sbox.setAlignment( Pos.CENTER_LEFT);
			
			gridPane.add( lbox, column,     row, 1, 1 );
			gridPane.add( sbox, (column+1), row, 1, 1 );
			column += 2;
			if( (column%4) == 0) {
				column = 0;
				row++;
			}
		}

		
		return gridPane;
	}
	
	
	public boolean hasUnsavedChanges() {
		// we need to make sure this is set right when a file is loaded.
		return unsavedChanges;
	}
	

    public void saveStylesheet( Scene scene, String stylesheet, boolean apply ) {
    	try {
            File xlitUserDir = new File( userXlitPath );
            if (! xlitUserDir.exists()){
            	xlitUserDir.mkdirs();
            }
            
	        OutputStream os = new FileOutputStream( userXlitPath + "/" + stylesheet );
	        final PrintStream printStream = new PrintStream(os);
	    	

	        StringBuffer sb = new StringBuffer( ".paragraph-text .text {\n" );
			ArrayList<Object> list = updatedStyles.get( "text" );
			sb.append( "\t-fx-fill: " + (String)list.get(0) + ";\n" );
	        sb.append(  "}\n\n" );
	        sb.append("#icuEditor .caret {\n\t-fx-stroke: " + (String)list.get(0)  + ";\n}\n\n" );
	        sb.append("#convertText .caret {\n\t-fx-stroke: " + (String)list.get(0)  + ";\n}\n" );
	        
	        printStream.println( sb );
	        
			for(String style : cssOrder) {
				if( "text".equals( style) ) {
					continue;
				}
				sb = new StringBuffer( "#icuEditor ." + style + " {\n" );
				
				list = updatedStyles.get( style );

				sb.append( "\t-fx-fill: " + (String)list.get(0) + ";\n" );
		    	if( (boolean)list.get(1) ) {
		    		sb.append( "\t-fx-font-weight: bold;\n" );
		    	}

		    	if( (boolean)list.get(2) ) {
		    		sb.append( "\t-fx-font-style: italic;\n" );
		    	}
		    	
		    	if( (boolean)list.get(3) ) {
		    		sb.append( "\t-fx-underline: true;\n" );
		    	}
		    	sb.append(  "}\n" );
		    	
		    	printStream.println( sb );
			}
			
			printStream.close();
			
			if( apply ) {
				applyStylesheet( scene, stylesheet, false );
				okAlert( "Updates Successfully Saved", "Updates Successfully Saved", "Syntax highlighting updates saved successfully." );
			}
    	}
    	catch(Exception ex) {
    		System.err.println( ex );
    	}

    }
    
    
    private void applyStylesheet( Scene scene, String stylesheet, boolean save ) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		scene.getStylesheets().clear();
        scene.setUserAgentStylesheet( null );
        
        if( save ) {
        	saveStylesheet( scene, stylesheet, false );
        }

		File ustylesheetFile = new File( userXlitPath + "/" + stylesheet );
		try {
			scene.getStylesheets().add( ustylesheetFile.toURI().toURL().toString() );
		}
		catch(MalformedURLException ex)  {
			System.err.println( ex );
		}
		
		scene.getStylesheets().add( classLoader.getResource( xlitStylesheet ).toExternalForm() );
    }
    
    
    private void reloadDefault( Scene scene ) {
    	applyStylesheet( scene, defaultStylesheet, false );
		ClassLoader classLoader = this.getClass().getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream( defaultStylesheet );			
		readStyleSheet( inputStream );
    }
    
    
    private void exportStylesheet( Stage stage ) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream( userStylesheet );
		if( inputStream == null ) {
			inputStream = classLoader.getResourceAsStream( defaultStylesheet );			
		}
		
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( "Save ICU File" );
		fileChooser.setInitialFileName( exportStylesheet );

		File file = fileChooser.showSaveDialog( stage );
		if (file == null) {  // the user cancelled the save
			return;
		}
		try {
			// Create file 
			FileWriter fstream = new FileWriter( file );
			BufferedWriter out = new BufferedWriter ( fstream );
			
			String content = readStylesheetToString( inputStream ); // a check for null content was made by the EditorTab
			out.write( content );
			
			//Close the output stream
			out.close();
		}
		catch( Exception ex ) {
			errorAlert( ex, "An Error Saving has Occured" );
		}
    }
    
    
    private void importStylesheet( Stage stage ) {
        final FileChooser fileChooser = new FileChooser();  
    	fileChooser.setTitle( "Import Stylesheet" );
        fileChooser.setInitialDirectory(
        		new File( System.getProperty( "user.home" ) )
        );                 
        fileChooser.getExtensionFilters().add(
        		new FileChooser.ExtensionFilter( "*.css", "*.css" )
        );
        
       	File importStylesheet = fileChooser.showOpenDialog( stage );
        if( importStylesheet == null ) {
        	// the user cancelled
        	return;
        }
		

		try {
			File userStylesFile = new File( userXlitPath + "/" + userStylesheet );
			Files.copy( importStylesheet.toPath(), userStylesFile.toPath(), StandardCopyOption.REPLACE_EXISTING );
			/*
	    	String styleSheetString = FileUtils.readFileToString(importStylesheet, StandardCharsets.UTF_8);
			
	    	String dir = this.getClass().getResource("/").getFile();
	        OutputStream os = new FileOutputStream( dir + userStylesheet );
	        final PrintStream printStream = new PrintStream(os);
	        
	        printStream.print(styleSheetString );
	        printStream.close();
			*/
			
	        applyStylesheet( stage.getScene(), userStylesheet, false );
			okAlert( "Import Successfully Saved", "Import Successfully Saved", "Syntax highlighting import saved successfully." );

		}
		catch( Exception ex ) {
			errorAlert( ex, "An Error Saving has Occured" );
		}
    }
    
    
    private String readStylesheetToString( InputStream inputStream ) throws IOException {
		BufferedReader br = new BufferedReader( new InputStreamReader(inputStream, "UTF-8") );
		StringBuffer sb = new StringBuffer();
		String line = null;
		
		while ( (line = br.readLine()) != null ) {
			sb.append( line );
			sb.append( "\n" );
		}
        return sb.toString();
    }
    
	
	protected void okAlert( String title, String header, String message ) {
        Alert alert = new Alert( AlertType.CONFIRMATION );
        alert.setTitle( title );
        alert.setHeaderText( header );
        alert.setContentText( message );
        alert.showAndWait();
	}
	

	protected void errorAlert( Exception ex, String header ) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle( "An Exception has occured" );
        alert.setHeaderText( header );
        alert.setContentText( ex.getMessage() );
        alert.showAndWait();
	}
}
