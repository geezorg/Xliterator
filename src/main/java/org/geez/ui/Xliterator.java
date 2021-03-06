package org.geez.ui;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.controlsfx.control.StatusBar;
import org.geez.convert.ProcessorManager;
import org.geez.ui.xliterator.ConvertFilesTab;
import org.geez.ui.xliterator.ConvertTextTab;
import org.geez.ui.xliterator.EditorTab;
import org.geez.ui.xliterator.ICUEditor;
import org.geez.ui.xliterator.JsonIndexGenerator;
import org.geez.ui.xliterator.SyntaxHighlighterTab;
import org.geez.ui.xliterator.XliteratorConfig;
import org.geez.ui.xliterator.XliteratorTab;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.endrullis.draggabletabs.DraggableTabPane;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
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
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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


public final class Xliterator extends Application {
 
	private static final String VERSION = "v0.6.0";
    private Desktop desktop = Desktop.getDesktop();

	private String scriptIn   = null; // alphabetic based default
	private String variantIn  = null;
	private String scriptOut  = null;
	private String variantOut = null;
	private JsonObject transliteration = null;
	protected StatusBar statusBar = new StatusBar();
	private Menu inScriptMenu = null;
	// private Menu outVariantMenu = null;
	private Menu outScriptMenu  = null;
	
	private Menu  inScriptMenuLast = null;
	private Menu outScriptMenuLast = null;
	
	final Menu tabsMenu = new Menu( "Tabs" );
	// an object could be introduced to hold the various transliteration attributes:
	private String selectedTransliteration  = null;
	private String transliterationDirection = null;
	private String transliterationAlias     = null;
	private ArrayList<String> transliterationDependencies = null;
	
    private String defaultFontFamily          = null;
    private DraggableTabPane tabpane          = new DraggableTabPane();
    private MenuItem loadInternalMenuItem     = new MenuItem( "Load Selected Transliteration" );

    private SyntaxHighlighterTab syntaxHighlighterTab = new SyntaxHighlighterTab( "Syntax Highlighter" );
    private ConvertTextTab textTab            = new ConvertTextTab( "Convert Text", this );
    private ConvertFilesTab filesTab          = new ConvertFilesTab( "Convert Files", this );
    private ProcessorManager processorManager = new ProcessorManager();
    
    private ArrayList<EditorTab> editorTabs   = new ArrayList<EditorTab>(); 

    private EditorTab currentEditorTab  = null;
    private EditorTab selectedEditorTab = null;
    
    private final int APP_WIDTH  = 800;
    private final int APP_HEIGHT = 800;
    
	private String xlitStylesheet = "styles/xliterator.css";
	
    public static final String scriptInPreference   = "org.geez.ui.xliterator.scriptIn";
    public static final String variantInPreference  = "org.geez.ui.xliterator.variantIn";
    public static final String scriptOutPreference  = "org.geez.ui.xliterator.scriptOut";
    public static final String variantOutPreference = "org.geez.ui.xliterator.variantOut";
    public static final String useSelectedEditor    = "org.geez.ui.xliterator.editor.selected";
    public static final String transliterationPreference          = "org.geez.ui.xliterator.transliteration";
    public static final String transliterationIdPreference        = "org.geez.ui.xliterator.transliterationId";
    public static final String transliterationDirectionPreference = "org.geez.ui.xliterator.transliterationDirection";
    
    private Stage primaryStage      = null;
	private XliteratorConfig config = null;
	
    private Image visibleIcon       = new Image( ClassLoader.getSystemResourceAsStream( "images/icons/Color/12/gimp-visible.png" ) );
    private Image arrowForwardIcon  = new Image( ClassLoader.getSystemResourceAsStream( "images/chevron_right_grey_16x16.png" ) ); 
    private Image arrowBothIcon     = new Image( ClassLoader.getSystemResourceAsStream( "images/chevron_double_grey_16x16.png" ) );
    private Image checkIcon         = new Image( ClassLoader.getSystemResourceAsStream( "images/check_black_16x16.png" ) );
    private ColorAdjust monochrome  = new ColorAdjust();
	

	public XliteratorConfig getConfig() {
		return config;
	}
	
	
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
    	fileChooser.setTitle( "View ICU Files" );
        fileChooser.setInitialDirectory(
        	new File( System.getProperty( "user.home" ) )
        );                 
        fileChooser.getExtensionFilters().add(
        	new FileChooser.ExtensionFilter( "*.xml", "*.xml" )
        );
    }
    
    
    private Menu createInScriptsMenu( final Stage stage ) {
    	Menu menu = new Menu( "Script _In" );
        ToggleGroup groupInMenu = new ToggleGroup();
        
        // Create menu from the scripts in the configuration file:
    	JsonObject scripts = config.getInScripts();
    	ArrayList<String> scriptInList = new ArrayList<String>( scripts.keySet() );
    	Collections.sort( scriptInList );
    	
    	for(String scriptInKey : scriptInList) {
			//JsonObject script = scripts.getAsJsonObject( scriptKey );
    		JsonArray variantsIn = scripts.getAsJsonArray( scriptInKey );
    		int size = variantsIn.size();
    		if ( size == 0 ) {
    			RadioMenuItem menuItem = new RadioMenuItem( scriptInKey );
    			menuItem.setToggleGroup( groupInMenu );
        		menuItem.setOnAction( evt -> {
        			setScriptIn( scriptInKey, "_base" );
        			if( inScriptMenuLast != null ) {
        				inScriptMenuLast.setGraphic( null );
        				inScriptMenuLast = null;
        			}
        		});
        		menu.getItems().add( menuItem );
    		}
    		else {
    			ArrayList<String> variantObjectList = new ArrayList<String>();
    			for(int i=1; i<size; i++) {
    				variantObjectList.add( variantsIn.get(i).getAsString() );
    			}
                
    		    Collections.sort( variantObjectList );
    		    variantObjectList.add(0, variantsIn.get(0).getAsString() ); // _base is always at index 0
    		    
    		    
    			Menu scriptMenu = new Menu( scriptInKey );
    			for(int i=0; i<size; i++) {
    				// JsonObject vObject = (JsonObject)variantObjectList.get(i);
    				String variantInKey = variantObjectList.get(i);
    				RadioMenuItem menuItem = new RadioMenuItem( variantInKey );
        			menuItem.setToggleGroup( groupInMenu );
        			scriptMenu.getItems().add( menuItem );
        			
    				if( "_base".equals( variantInKey ) ) {
    					menuItem.setText( scriptInKey );
            			scriptMenu.getItems().add( new SeparatorMenuItem() );
    				}

            		menuItem.setOnAction( evt -> {
            			if( inScriptMenuLast != null ) {
            				inScriptMenuLast.setGraphic( null );
            			}
            			setScriptIn( scriptInKey, variantInKey );    		
            			scriptMenu.setGraphic( new ImageView( checkIcon ) );
            			inScriptMenuLast = scriptMenu;
            		});

    			}
    			menu.getItems().add( scriptMenu );
    		}
    		
    	}
    		
    	return menu;
    }
    
    
    private void tabToggler( MenuItem menuItem, XliteratorTab tab, ImageView onView, ImageView offFile ){
    	boolean show = (boolean)menuItem.getProperties().get( "show" );
    	show =! show;
    	menuItem.getProperties().put( "show", show );
    	
    	if( show ) {
    		tabpane.getTabs().add( tab );
    		menuItem.setGraphic( onView );
        	tabpane.getSelectionModel().select( tab );
    	}
    	else {
    		tabpane.getTabs().remove( tab );
    		menuItem.setGraphic( offFile );
    	}
    }
    
    
    private Menu createOutScriptsMenu(String scriptIn, String variantIn) {
   	 	outScriptMenu.getItems().clear();
   	 	scriptOut = null;
		// outVariantMenu.getItems().clear();
		variantOut = null;
    	scriptOutText.setText( "[None]" );
    	resourceText.setText( "[None]" );
    	
        ToggleGroup groupOutMenu = new ToggleGroup();
        
    	JsonObject scriptsOut = config.getOutScriptsOfInScriptAndInVariant(scriptIn, variantIn);
    	ArrayList<String> scriptOutList = new ArrayList<String>( scriptsOut.keySet() );
    	Collections.sort( scriptOutList );
    	
    	for(String scriptOutKey : scriptOutList) {
    		JsonArray variantsOut = scriptsOut.getAsJsonArray( scriptOutKey );
    		int size = variantsOut.size();
    		String variantOutZero = variantsOut.get(0).getAsJsonObject().get( "name" ).getAsString();
    		if ( (size == 1) && ( "_base".equals(variantOutZero) || "IPA".equals(scriptOutKey) ) ) {
    			// if the one element is "_base" , then 
    			RadioMenuItem menuItem = new RadioMenuItem( scriptOutKey );
    			menuItem.setToggleGroup( groupOutMenu );
    			JsonObject variantOutObject = variantsOut.get(0).getAsJsonObject();
        		menuItem.setOnAction( evt -> {
        			setScriptOut( scriptOutKey, "_base", variantOutObject );
            		
        			if( outScriptMenuLast != null ) {
        				outScriptMenuLast.setGraphic( null );
        				outScriptMenuLast = null;
        			}
        		});

        		outScriptMenu.getItems().add( menuItem );
        		
        		ImageView imageView = null;
        		if( "both".equals( variantOutObject.get("direction").getAsString() ) ) {
        			imageView = new ImageView( arrowBothIcon );
        		}
        		else {
        			imageView = new ImageView( arrowForwardIcon );    	        			
        		}
                menuItem.setGraphic( imageView );
    		}
    		else {
    			Menu scriptMenu = new Menu( scriptOutKey );
    			
    			ArrayList<JsonObject> variantObjectList = new ArrayList<JsonObject>();
    			for(int i=1; i<size; i++) {
    				variantObjectList.add( variantsOut.get(i).getAsJsonObject() );
    			}
                
    		    Collections.sort( variantObjectList, getComparator() );
    		    variantObjectList.add(0, variantsOut.get(0).getAsJsonObject() ); // _base is always at index 0
    		    
    			for(int i=0; i<size; i++) {
    				JsonObject vObject = (JsonObject)variantObjectList.get(i);
    				String variantOutKey = vObject.getAsJsonObject().get("name").getAsString();
    				RadioMenuItem menuItem = new RadioMenuItem( variantOutKey );
            		scriptMenu.getItems().add( menuItem );
    				if( "_base".equals( variantOutKey ) ) {
    					menuItem.setText( scriptOutKey );
            			scriptMenu.getItems().add( new SeparatorMenuItem() );
    				}

        			menuItem.setToggleGroup( groupOutMenu );
        			JsonObject variantOutObject = variantsOut.get(i).getAsJsonObject();
            		menuItem.setOnAction( evt -> {
            			if( outScriptMenuLast != null ) {
            				outScriptMenuLast.setGraphic( null );
            			}
            			setScriptOut( scriptOutKey, variantOutKey, variantOutObject );
            			scriptMenu.setGraphic( new ImageView( checkIcon ) );
            			outScriptMenuLast = scriptMenu;
            		});
            		
	        		ImageView imageView = null;
	        		if( "both".equals( variantOutObject.get("direction").getAsString() ) ) {
	        			imageView = new ImageView( arrowBothIcon );
	        		}
	        		else {
	        			imageView = new ImageView( arrowForwardIcon );    	        			
	        		}
	                menuItem.setGraphic( imageView );
    			}
    			outScriptMenu.getItems().add( scriptMenu );
    		}
    	}
    	
    	return outScriptMenu;
    }
    
    private static Comparator<JsonObject> getComparator() {
        return new Comparator<JsonObject>() {
           @Override
           public int compare(JsonObject o1, JsonObject o2) {
               String name1 = o1.get( "name" ).getAsString();
               String name2 = o2.get( "name" ).getAsString();

               // ordering is the natural String ordering in your example
               return name1.compareTo(name2); 
           }
        };
    }

    /*
    private Menu createOutVaraintsMenu(String outVariant) {
    	outVariantMenu.getItems().clear();
        ToggleGroup groupVariantOutMenu = new ToggleGroup();
             
    	JsonArray variants = config.getVariants(scriptIn, scriptOut);
    	for (int i = 0; i < variants.size(); i++) {
    		JsonObject variant = variants.get(i).getAsJsonObject();
    		if( variant.has( "visibility" ) && ("internal".equals( variant.get("visibility").getAsString() ) ) ) {
    			continue;
    		}
    		if( variant.get("name") == null ) {
    			for(String subVariantKey: variant.keySet() ) {
    				Menu variantSubMenu = new Menu( subVariantKey );
    				JsonArray subvariants = variant.getAsJsonArray( subVariantKey );
    		    	for (int j = 0; j < subvariants.size(); j++) {
    		    		JsonObject subvariant = subvariants.get(j).getAsJsonObject();
        	    		if( subvariant.has( "visibility" ) && ("internal".equals( subvariant.get("visibility").getAsString() ) ) ) {
        	    			continue;
        	    		}
    	    			String name = subvariant.get("name").getAsString();
    	        		RadioMenuItem menuItem = new RadioMenuItem( name );
    	        		menuItem.setToggleGroup( groupVariantOutMenu );
    	        		String transliterationID = subvariant.get( "path" ).getAsString();
    	        		String direction = subvariant.get( "direction" ).getAsString();
    	        		ArrayList<String> dependencies = new ArrayList<String>();
    	        		if( subvariant.has( "dependencies" ) ) {
    	        			JsonArray dependenciesJSON = subvariant.getAsJsonArray( "dependencies" );
    	        			for (int k = 0; k < dependenciesJSON.size(); k++) {
    	        				dependencies.add( dependenciesJSON.get(k).getAsString() );
    	        			}
    	        		}
    	                
    	                String alias = (subvariant.has("alias")) ? subvariant.get("alias").getAsString() : null;
    	                if( alias != null ) {
    	                	menuItem.getProperties().put( "alias", alias );
    	                }
    	                
    	        		menuItem.setOnAction( evt -> { 
    	        			this.selectedTransliteration  = transliterationID; 
    	        			this.transliterationDirection = direction; 
    	        			this.transliterationDependencies = (dependencies.isEmpty()) ? null: dependencies;
    	        			this.transliterationAlias = alias;
    	        			setVariantOut( subVariantKey + " - " + name ); 
    	        		});
    	        		menuItem.setId( transliterationID );
    	        		
    	        		ImageView imageView = null;
    	        		if( direction.equals( "both" ) ) {
    	        			imageView= new ImageView( arrowBothIcon );
    	        		}
    	        		else {
    	        			imageView= new ImageView( arrowForwardIcon );    	        			
    	        		}
    	                menuItem.setGraphic( imageView );
    	                
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
        		ArrayList<String> dependencies = new ArrayList<String>();
        		if( variant.has( "dependencies" ) ) {
        			JsonArray dependenciesJSON = variant.getAsJsonArray( "dependencies" );
        			for (int j = 0; j < dependenciesJSON.size(); j++) {
        				dependencies.add( dependenciesJSON.get(j).getAsString() );
        			}
        		}
        		
                
                String alias = (variant.has( "alias") ) ? variant.get("alias").getAsString() : null;
                if( alias != null ) {
                	menuItem.getProperties().put( "alias", alias );
                }
                
        		menuItem.setOnAction( evt -> {
        			this.selectedTransliteration = transliterationID;
        			this.transliterationDirection = direction;
        			this.transliterationDependencies = (dependencies.isEmpty()) ? null: dependencies;
        			this.transliterationAlias = alias;
        			setVariantOut( name); 
        		});
        		menuItem.setId( transliterationID );
        		
        		ImageView imageView = null;
        		if( direction.equals( "both" ) ) {
        			imageView= new ImageView( arrowBothIcon );
        		}
        		else {
        			imageView= new ImageView( arrowForwardIcon );    	        			
        		}
                menuItem.setGraphic( imageView );
                
        		outVariantMenu.getItems().add( menuItem );
    		}
    	}
    	if( (variants.size() == 0) ) {
    		// a json parse error should have occurred, 
    	}
    	
    	return outVariantMenu;
    }
    */

    private static final Pattern sourcePattern  = Pattern.compile( "^(.*)source\\h*=\\h*\"([^\"]+)\"(.*)$" );
    private static final Pattern targetPattern  = Pattern.compile( "^(.*)target\\h*=\\h*\"([^\"]+)\"(.*)$" );
    private static final Pattern variantPattern = Pattern.compile(  "^(.*)variant\\h*=\\h*\"([^\"]+)\"(.*)$" );
    private static final Pattern aliasPattern   = Pattern.compile( "^(.*)alias\\h*=\\h*\"([^\"]+)\"(.*)$");
    private static final Pattern backwardAliasPattern = Pattern.compile( "^(.*)backwardAlias\\h*=\\h*\"([^\"]+)\"(.*)$" );
    private static final Pattern directionPattern = Pattern.compile( "^(.*)direction\\h*=\\h*\"([^\"]+)\"(.*)$" );
    
    private JsonObject createEmptyTransliteration( String path ) {	
    	JsonObject emptyTransliteration = new JsonObject();    	
    	
    	DateTimeFormatter FOMATTER = DateTimeFormatter.ofPattern("yyy_MM_dd_HH_mm_ss_z");
    	 
    	//Zoned datetime instance
    	ZonedDateTime zdt = ZonedDateTime.now();
    	 
    	//Get formatted String
    	String zdtString = FOMATTER.format(zdt);
    	
    	emptyTransliteration.addProperty( "path", path );
    	emptyTransliteration.addProperty( "name", "VARIANT_OUT_" + zdtString);
    	emptyTransliteration.addProperty( "source", "SCRIPT_IN_" + zdtString);
    	emptyTransliteration.addProperty( "target", "SCRIPT_OUT_" + zdtString);
    	emptyTransliteration.addProperty( "direction", "forward" );

    	return emptyTransliteration;
    }
    
    
    private JsonObject createPseudoTransliteration( File externalIcuFile ) {

    	String fileName = externalIcuFile.getName(); 	
    	String extension = FilenameUtils.getExtension( fileName );
    	
    	JsonObject pseudoTransliteration = new JsonObject();
    	
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines( Paths.get(externalIcuFile.getPath()), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        String fileData = contentBuilder.toString();
        
    	String exName = null, exDirection = null, exAlias = null, exBackwardAlias = null, exSource = null, exTarget = null;
    	
    	
    	if( "txt".equals( extension ) ) {
    		exName = FilenameUtils.getBaseName( fileName );	
    		exDirection = fileData.contains( "↔" ) ? "both" : "forward" ;
    	}
    	else { // must be XML
	
    		Matcher matcher = sourcePattern.matcher( fileData );
    		if( matcher.matches() ) {
    			exSource = matcher.group( 2 );
    		}
    		else {
    			exSource = "unknown";
    		}
			pseudoTransliteration.addProperty( "source", exSource );
        	
    		matcher = targetPattern.matcher( fileData );
    		if( matcher.matches() ) {
    			exTarget = matcher.group( 2 );        	
    		}
			pseudoTransliteration.addProperty( "target", exTarget );
			
    		matcher = directionPattern.matcher( fileData );
    		if( matcher.matches() ) { 
    			exDirection = matcher.group( 2 );
    		}
        	pseudoTransliteration.addProperty( "direction", exDirection );
        	
    		matcher = variantPattern.matcher( fileData );
    		if( matcher.matches() ) {
    			exName = matcher.group( 2 );       
    		}

    		matcher = aliasPattern.matcher( fileData );
    		if( matcher.matches() ) {
	    		exAlias = matcher.group( 2 ); 
	        	pseudoTransliteration.addProperty( "alias", exAlias );
    		}
    		
    		matcher = backwardAliasPattern.matcher( fileData );
    		if( matcher.matches() ) {
	    		exBackwardAlias = matcher.group( 2 ); 
    			pseudoTransliteration.addProperty( "backwardAlias", exBackwardAlias );
    		}
    	}
    	
    	pseudoTransliteration.addProperty( "path", externalIcuFile.getPath() );
    	pseudoTransliteration.addProperty( "name", exName );
    	pseudoTransliteration.addProperty( "direction", exDirection );
    	
    	return pseudoTransliteration;
    }
    
    private EditorTab checkIfLoaded( String title ) {
    	for( EditorTab editorTab: editorTabs ) {
    		if( title.equals( editorTab.getTitle() ) ) {
    			return editorTab;
    		}
    	}
    	
    	return null;
    }

    private final MenuItem fileMenuItem = new MenuItem( "Select Files..." ); 
    private EditorTab createNewEditor( String title, Menu tabsMenu, Image visibleIcon, ColorAdjust monochrome ) {
    	EditorTab editorTab = new EditorTab( title );
        editorTab.setDefaultFontFamily( defaultFontFamily );
        editorTab.setClosable( true );
    	
        editorTab.setTransliteration(transliteration);
        /*
    	editorTab.getProperties().put( "direction", transliterationDirection );
        if( transliterationAlias != null ) {
        	editorTab.getProperties().put( "alias", transliterationAlias );
        }
        */
        editorTab.setup(primaryStage, this, saveMenuItem, saveAsMenuItem);
        
        editorTab.getEditor().prefHeightProperty().bind( primaryStage.heightProperty().subtract( 160 ) );
        editorTab.setOnSelectionChanged( evt -> {
        	if( editorTab.isSelected() ) {
        		fileMenuItem.setDisable( true );
        		if ( variantOut == null )
        			loadInternalMenuItem.setDisable( true );
        		else
        			loadInternalMenuItem.setDisable( false );
        		if(! "".equals( editorTab.getEditor().getText() ) ) {
            		closeMenuItem.setDisable( false );
            		closeAllMenuItem.setDisable( false );
        			saveMenuItem.setDisable( false );
        			saveAsMenuItem.setDisable( false );
        		}
        		currentEditorTab = editorTab;
        		toggleEditMenu( false );
        	}
    		// System.out.println( "Selected: " + editorTab.getTitle() + " isSelected: " + editorTab.isSelected() );
        });
        
        MenuItem editorTabViewMenuItem = new MenuItem( title );
        ImageView editorOnView  = new ImageView( visibleIcon );
        ImageView editorOffView = new ImageView( visibleIcon );
        editorOffView.setEffect( monochrome );
        editorTabViewMenuItem.setGraphic( editorOffView );
        editorTabViewMenuItem.getProperties().put( "show", false );
        editorTabViewMenuItem.setMnemonicParsing( false );
        editorTabViewMenuItem.setOnAction( evt -> tabToggler(editorTabViewMenuItem, editorTab, editorOnView, editorOffView) );
        tabsMenu.getItems().add( editorTabViewMenuItem );
        
        editorTab.getProperties().put( "editorTabViewMenuItem", editorTabViewMenuItem );
        editorTab.getProperties().put( "editorOnView", editorOnView );
        editorTab.getProperties().put( "editorOffView", editorOffView );

        if( editorTabs.size() == 0 ) {
        	inScriptMenu.getItems().add( new SeparatorMenuItem() );
        }
        editorTabs.add( editorTab );
        
    	RadioMenuItem editorTabItem = new RadioMenuItem( title );
    	editorTabItem.setOnAction( evt -> {
    		setUseEditor( title );
    		if( selectedEditorTab != null ) {
    			Label label = (Label)selectedEditorTab.getGraphic();
    			selectedEditorTab.setGraphic( null );
    			selectedEditorTab.setText( label.getText() );
    		}
    		selectedEditorTab = editorTab;
    		Label label = new Label( title );
    		label.setGraphic(new ImageView( checkIcon ) );
    		selectedEditorTab.setGraphic( label );
	    	selectedTransliteration = useSelectedEditor; 
	    	filesTab.setScriptIn( useSelectedEditor );
	    	textTab.setScriptInAndDirection( useSelectedEditor, editorTab.getSelectedDirection() );
	   	 	outScriptMenu.getItems().clear();
	   	 	scriptOut = null;
			// outVariantMenu.getItems().clear();
			variantOut = null;
	    	scriptInText.setText( "[Editor] " +  selectedEditorTab.getInText() );
	    	scriptOutText.setText( "[Editor] " + selectedEditorTab.getOutText() );
	    	resourceText.setText( "[Editor] " + title );
	    	setTransliteration( editorTab.getTransliteration() );
    	});
    	editorTabItem.setMnemonicParsing( false );
    	// editorTabItem.getProperties().put( "selection", useSelectedEditor );
    	inScriptMenu.getItems().add( editorTabItem );


        editorTab.setOnClosed( evt -> {
        	// check to save file
        	editorTabs.remove( editorTab );
        	tabsMenu.getItems().remove( editorTabViewMenuItem );
        	inScriptMenu.getItems().remove( editorTabItem );
            if( editorTabs.size() == 0 ) {
            	int lastIndex = inScriptMenu.getItems().size() - 1;  // this should be the separator index
            	inScriptMenu.getItems().remove( lastIndex );
            }
        });
        
		closeMenuItem.setDisable( false );
		closeAllMenuItem.setDisable( false );
        saveMenuItem.setDisable(false); // because the new editor will appear in the foreground
        saveAsMenuItem.setDisable(false); 
    	
		tabToggler( 
			editorTabViewMenuItem,
			editorTab, 
			(ImageView)editorTab.getProperties().get( "editorOnView" ), 
			(ImageView)editorTab.getProperties().get( "editorOffView" )
		);
		
        return editorTab;
    }
    
    
    final Menu editMenu = new Menu( "Edit" );
    private void toggleEditMenu(boolean disable) {
    	for (MenuItem item: editMenu.getItems() ) {
    		item.setDisable( disable );
    	}
    }
    
    final MenuItem saveMenuItem     = new MenuItem( "_Save" );
    final MenuItem saveAsMenuItem   = new MenuItem( "Save As..." );
    final MenuItem closeMenuItem    = new MenuItem( "_Close" );
    final MenuItem closeAllMenuItem = new MenuItem( "Close All" );
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
        String osName = System.getProperty("os.name").toLowerCase();
        if( osName.startsWith( "mac" ) ) {
            com.apple.eawt.Application.getApplication().setDockIconImage( SwingFXUtils.fromFXImage(logoImage, null) );  
            defaultFontFamily = "Kefa";
        }
        textTab.setDefaultFontFamily( defaultFontFamily );
        
        
        // Create and configure menus:
        
        
        //
        //=========================== BEGIN FILE MENU =============================================
        //
        final Menu fileMenu = new Menu("_File");
        

		final Menu newMenu = new Menu( "New File" );
		final MenuItem xmlMenuItem = new MenuItem( "XML" );
		xmlMenuItem.setOnAction( evt -> createNewFile( "Untitled", "XML" ) );
		xmlMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN) );
		final MenuItem txtMenuItem = new MenuItem( "TXT" );
		txtMenuItem.setOnAction( evt -> createNewFile( "Untitled", "TXT" ) );
		newMenu.getItems().addAll( xmlMenuItem, txtMenuItem );


        fileMenuItem.setDisable( true );
        
        saveMenuItem.setOnAction( actionEvent -> currentEditorTab.saveContent( stage, false ) );
        saveMenuItem.setDisable( true );
        saveMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN) );
        
        saveAsMenuItem.setOnAction( actionEvent -> currentEditorTab.saveContent( stage, true ) );
        saveAsMenuItem.setDisable( true );
        saveAsMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN) );


    	// Add transliteration file selection option:
		final MenuItem openMenuItem = new MenuItem( "Open ICU File..." );
        openMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN) );
		
        loadInternalMenuItem.setDisable( true );
        loadInternalMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN) );
        
        MenuItem quitMenuItem = new MenuItem( "_Quit Xliterator" );
        quitMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN) ) ;
        quitMenuItem.setOnAction( evt -> {
    			Window window = primaryStage.getScene().getWindow();
    			window.fireEvent( new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST) );
        });
        
        
        closeMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN) );
        closeMenuItem.setOnAction( evt -> {
            EventHandler<Event> handler = currentEditorTab.getOnClosed();
            if (null != handler) {
                handler.handle(null);
            }
            tabpane.getTabs().remove( currentEditorTab );

        });
        closeMenuItem.setDisable( true );
        
        closeAllMenuItem.setAccelerator( KeyCodeCombination.keyCombination( "Shortcut+Shift+W" ) );
        closeAllMenuItem.setOnAction( evt -> {
        	int size = tabpane.getTabs().size();
        	for(int i=0; i<size; i++) {
        		Tab tab = tabpane.getTabs().get(i);
        		if( tab instanceof EditorTab ) {
                    EventHandler<Event> handler = tab.getOnClosed();
                    if (null != handler) {
                        handler.handle(null);
                    }
        			tabpane.getTabs().remove( tab );
        			size--;
        			i--;
        		}
        	}
        	
        });
        closeAllMenuItem.setDisable( true );
        
        fileMenu.getItems().addAll( newMenu, openMenuItem, loadInternalMenuItem, fileMenuItem, new SeparatorMenuItem(), closeMenuItem, closeAllMenuItem, new SeparatorMenuItem(), saveMenuItem, saveAsMenuItem, new SeparatorMenuItem(), quitMenuItem ); 
        
        //
        //=========================== END FILE MENU =============================================
        
        
        //
        // We have prerequisite objects to configure the tabs and tabpane, lets do so now:
        //
        //=========================== BEGIN FILES TAB ===========================================
        filesTab.setClosable( false );
        filesTab.setProcessor( processorManager );
        filesTab.setComponents(fileMenuItem, statusBar, stage);
        filesTab.setOnSelectionChanged( evt -> {
        	if( filesTab.isSelected() ) {
        		fileMenuItem.setDisable( false );
        		// loadInternalMenuItem.setDisable( true );
        		saveMenuItem.setDisable( true );
        		saveAsMenuItem.setDisable( true );
        		toggleEditMenu( true );
        	}
        });
        //=========================== END FILES TAB =============================================

        
        //=========================== BEGIN TEXT TAB ============================================
        // textTab.setup();  //
        textTab.setDefaultFontFamily( defaultFontFamily );
        textTab.setClosable( false );
        textTab.setOnSelectionChanged( evt -> {
        	if( textTab.isSelected() ) {
        		// fileMenuItem.setDisable( true );
        		// loadInternalMenuItem.setDisable( true );
        		saveMenuItem.setDisable( true );
        		closeMenuItem.setDisable( true );
        		closeAllMenuItem.setDisable( true );
        		saveAsMenuItem.setDisable( true );
        		currentEditorTab = null;
        		toggleEditMenu( true );
        	}
        });
        //=========================== END TEXT TAB =============================================
        
        // Setup the tabs:
        tabpane.getTabs().addAll( textTab );
        //
        //  End of Tab & TabPane setup
        //
        
        
        //
        //=========================== BEGIN SCRIPT MENUS =============================================
        // 
        inScriptMenu   = createInScriptsMenu( stage );
        outScriptMenu  = new Menu( "Script _Out" );
         //outVariantMenu = new Menu( "_Variant" );
        //
        //=========================== END SCRIPT MENUS =============================================
        //
        

        //
        //=========================== BEGIN HELP MENU =============================================
        //
        final Menu helpMenu = new Menu( "Help" );
        final MenuItem aboutMenuItem = new MenuItem( "About" );
        helpMenu.getItems().add( aboutMenuItem );

        
        aboutMenuItem.setOnAction( evt -> {
		        Alert alert = new Alert(AlertType.INFORMATION);
		        alert.setTitle( "About The Xliterator" );
		        alert.setHeaderText( "An ICU based transliteration utility " + VERSION );
		        
		        FlowPane fp = new FlowPane();
		        Label label = new Label( "Visit the project homepage on" );
		        Hyperlink link = new Hyperlink( "GitHub" );
		        fp.getChildren().addAll( label, link );
		
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
        });
        
        final MenuItem demoMenuItem = new MenuItem( "Load Demo" );
        helpMenu.getItems().add( demoMenuItem );
        
        final Menu developerMenu = new Menu( "Developer" );
        final MenuItem referenceSpreadsheetMenuItem = new MenuItem( "Get Reference Spreadshet" );
        final MenuItem makeJsonMenuItem = new MenuItem( "Create JSON Index" );
        developerMenu.getItems().addAll( referenceSpreadsheetMenuItem, makeJsonMenuItem );
        helpMenu.getItems().add( developerMenu );
        
        referenceSpreadsheetMenuItem.setOnAction( evt -> {
        	File spreadsheet = config.exportReferenceSpreadsheet( stage );
        	if( spreadsheet == null ) {
        		return;
        	}
        	try {
        		desktop.open( spreadsheet );
        	}
        	catch(IOException ex) {
        		errorAlert( ex, "Could not open: " +spreadsheet.getPath() );
        	}
        });
        
        makeJsonMenuItem.setOnAction( evt -> {
	        final FileChooser fileChooser = new FileChooser();
	    	fileChooser.setTitle( "Select an Excel File" );
	        fileChooser.setInitialDirectory(
	        		new File( System.getProperty( "user.home" ) )
	        );                 
	        fileChooser.getExtensionFilters().add(
	        		new FileChooser.ExtensionFilter( "*.xlsx", "*.xlsx" )
	        );
	    	File spreadsheetFile = fileChooser.showOpenDialog( stage );
	        if( spreadsheetFile == null ) {
	        	return;
	        }
	        JsonIndexGenerator generator = new JsonIndexGenerator( stage );
	        
	        String jsonIndex = generator.generateIndex( spreadsheetFile );
	        
	        exportConvertedSpreadsheet( stage, jsonIndex );
        });
        
        //
        //=========================== END HELP MENU =============================================
        //
        
        
        //
        //=========================== BEGIN TABS MENU ===========================================
        //
        
        MenuItem fileConverterTabViewMenuItem = new MenuItem( "File Converter" );
        MenuItem textConverterTabViewMenuItem = new MenuItem( "Text Converter" );
        MenuItem syntaxHighlighterTabViewMenuItem = new MenuItem( "Syntax Highlighter" );

        tabsMenu.getItems().addAll( fileConverterTabViewMenuItem, textConverterTabViewMenuItem );
        
        monochrome.setSaturation(-1);
        

        ImageView fileConverterOnView = new ImageView( visibleIcon );
        ImageView fileConverterOffView = new ImageView( visibleIcon );
        fileConverterOffView.setEffect( monochrome );
        fileConverterTabViewMenuItem.setGraphic( fileConverterOffView );
        fileConverterTabViewMenuItem.getProperties().put( "show", false );	
        fileConverterTabViewMenuItem.setOnAction( evt -> tabToggler(fileConverterTabViewMenuItem, filesTab, fileConverterOnView, fileConverterOffView) );
   
        ImageView textConverterOnView = new ImageView( visibleIcon );
        ImageView textConverterOffView = new ImageView( visibleIcon );
        textConverterOffView.setEffect( monochrome );
        textConverterTabViewMenuItem.setGraphic( textConverterOnView );
        textConverterTabViewMenuItem.getProperties().put( "show", true );	
        textConverterTabViewMenuItem.setOnAction( evt -> tabToggler(textConverterTabViewMenuItem, textTab, textConverterOnView, textConverterOffView) );
        
        ImageView syntaxHighlighterOnView = new ImageView( visibleIcon );
        ImageView syntaxHighlighterOffView = new ImageView( visibleIcon );
        syntaxHighlighterOffView.setEffect( monochrome );
        syntaxHighlighterTabViewMenuItem.setGraphic( syntaxHighlighterOffView );
        syntaxHighlighterTabViewMenuItem.getProperties().put( "show", false );
        syntaxHighlighterTabViewMenuItem.setOnAction( evt -> tabToggler(syntaxHighlighterTabViewMenuItem, syntaxHighlighterTab, syntaxHighlighterOnView, syntaxHighlighterOffView) );
        //
        //=========================== END TABS MENU =============================================
        //
        
        //
        //=========================== BEGIN UPDATE FILE MENU ENTRIES ============================
        //
        loadInternalMenuItem.setOnAction( evt -> {
        		// load into a new editor tab
	        	if( selectedTransliteration.equals( useSelectedEditor) ) {
	        		return;
	        	}
            	try {
            		if( transliterationDependencies != null) { // load first so they apepar in the background
            			for(String dependency: transliterationDependencies) {
            				JsonObject dependencyObject = config.getTransliterationByName( dependency );
            				String dependentTransliteration = dependencyObject.get( "path" ).getAsString();
            				if( checkIfLoaded( dependentTransliteration ) == null ) {
            					EditorTab dependentEditorTab = createNewEditor( dependentTransliteration, tabsMenu, visibleIcon, monochrome );
            					dependentEditorTab.getEditor().loadResourceFile( dependentTransliteration );
            				}
            			}
            		}
            		EditorTab editorToLoad = checkIfLoaded( selectedTransliteration ) ;
    				if( editorToLoad == null ) {
    					currentEditorTab = createNewEditor( selectedTransliteration, tabsMenu, visibleIcon, monochrome );
    					currentEditorTab.getEditor().loadResourceFile( selectedTransliteration );
    					currentEditorTab.setScriptIn( scriptIn, variantIn );
    					currentEditorTab.setScriptOut( scriptOut );
    					currentEditorTab.setVariantOut( variantOut );
    				}
    				else {
    					currentEditorTab = editorToLoad;
    					tabpane.getSelectionModel().select( currentEditorTab );
    				}
                }
                catch(IOException ex) {
                	errorAlert(ex, "Error opening: " + selectedTransliteration );
                }
        });
        openMenuItem.setOnAction( evt -> {
		        final FileChooser fileChooser = new FileChooser();
		    	configureFileChooserICU(fileChooser);
		    	File externalIcuFile = fileChooser.showOpenDialog( stage );
		        if( externalIcuFile == null ) {
		        	return;
		        }
		        try {
		        	JsonObject pseudoTransliteration = createPseudoTransliteration( externalIcuFile );
		        	setTransliteration( pseudoTransliteration );
		        	currentEditorTab = createNewEditor( externalIcuFile.getName(), tabsMenu, visibleIcon, monochrome );
		        	currentEditorTab.loadFile( externalIcuFile );
		        	currentEditorTab.setTransliteration( pseudoTransliteration );

		        }
		        catch(IOException ex) {
		        	errorAlert(ex, "Error opening: " + externalIcuFile.getName() );
		        }
        });
        
        demoMenuItem.setOnAction( evt -> loadDemo(tabsMenu, visibleIcon, monochrome) );
        //
        //=========================== END UPDATE FILE MENU ENTRIES ============================
        //
        
        //
        //=========================== BEGIN PREFERENCES MENU ====================================
        //
        final Menu preferencesMenu = new Menu( "Preferences" );
        final MenuItem makeDefaultMappingMenuItem = new MenuItem( "Save Transliteration Selection" );
        final MenuItem makeDefaultFontsMenuItem   = new MenuItem( "Save Font Selections" );

        makeDefaultMappingMenuItem.setOnAction( evt -> saveDefaultMapping() );
        makeDefaultFontsMenuItem.setOnAction( evt -> saveDefaultFontSelections() );
        
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
        
        final MenuItem syntaxHighlightEditorMenuItem = new MenuItem( "Edit Syntax Highlighting" );
        syntaxHighlightEditorMenuItem.setOnAction( evt -> {
        	syntaxHighlightEditorMenuItem.setDisable( true );
        	tabsMenu.getItems().add( syntaxHighlighterTabViewMenuItem );
        	tabToggler( syntaxHighlighterTabViewMenuItem, syntaxHighlighterTab, syntaxHighlighterOnView, syntaxHighlighterOffView );
        	launchSyntaxHightlightEditor( stage ); 
        });
        
        syntaxHighlighterTab.setOnCloseRequest( evt -> {
        	syntaxHighlightEditorMenuItem.setDisable( false );
        	tabsMenu.getItems().remove(syntaxHighlighterTabViewMenuItem);
        });
        
        syntaxHighlighterTab.setOnSelectionChanged( evt -> {
        	if( syntaxHighlighterTab.isSelected() ) {
        		toggleEditMenu( true );
        	}
        });
      

        preferencesMenu.getItems().addAll( makeDefaultMappingMenuItem, makeDefaultFontsMenuItem, caseConversionMenu, syntaxHighlightEditorMenuItem );
        //
        //=========================== END PREFERENCES MENU ======================================
        //
        
        //
        //=========================== BEGIN EDIT MENU ===========================================
        //
        
        // TODO:  This menu needs to work with which ever text area is in focus
        
        MenuItem findMenuItem = new MenuItem("Find…");
        findMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN) );
        findMenuItem.setOnAction( evt -> currentEditorTab.getEditor().findWord(stage) );
        
        MenuItem replaceMenuItem = new MenuItem("Replace…");
        replaceMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN) );
        replaceMenuItem.setOnAction( evt -> currentEditorTab.getEditor().replace(stage) );
        
        MenuItem undoMenuItem = new MenuItem( "Undo" );
        undoMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.U, KeyCombination.SHORTCUT_DOWN) );
        undoMenuItem.setOnAction( evt -> currentEditorTab.getEditor().undo() );

        MenuItem redoMenuItem = new MenuItem( "Redo" );
        redoMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN) );
        redoMenuItem.setOnAction( evt -> currentEditorTab.getEditor().redo() );

        MenuItem cutMenuItem = new MenuItem( "Cut" );
        cutMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN) );
        cutMenuItem.setOnAction( evt -> currentEditorTab.getEditor().cut() );

        MenuItem copyMenuItem = new MenuItem( "Copy" );
        copyMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN) );
        copyMenuItem.setOnAction( evt -> currentEditorTab.getEditor().copy() );

        MenuItem pasteMenuItem = new MenuItem( "Paste" );
        pasteMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN) );
        pasteMenuItem.setOnAction( evt -> currentEditorTab.getEditor().paste() );

        // MenuItem deleteMenuItem = new MenuItem( "Delete" );
        // deleteMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN) );
        // deleteMenuItem.setOnAction( evt -> editorTab.getEditor().replaceSelection() );

        MenuItem selectAllMenuItem = new MenuItem( "Select All" );
        selectAllMenuItem.setAccelerator( new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN) );
        selectAllMenuItem.setOnAction( evt -> currentEditorTab.getEditor().selectAll() );
        
        editMenu.getItems().addAll( undoMenuItem, redoMenuItem, cutMenuItem, copyMenuItem, pasteMenuItem, /* deleteMenuItem, */ new SeparatorMenuItem(), findMenuItem, replaceMenuItem, new SeparatorMenuItem(), selectAllMenuItem);
		toggleEditMenu( true );
        //
        //=========================== END EDIT MENU ==============================================
        //
                
        
        
        // create the left menubar 
        final MenuBar leftBar = new MenuBar();  
  
        // add menus to the left menubar 
        leftBar.getMenus().addAll( fileMenu, editMenu, tabsMenu, inScriptMenu, outScriptMenu /* , outVariantMenu */);

        
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
        stage.setScene( scene ); 
        
    	ClassLoader classLoader = this.getClass().getClassLoader();
        scene.getStylesheets().add( classLoader.getResource( xlitStylesheet ).toExternalForm() );
		ICUEditor.loadStylesheets( scene );

        scene.getWindow().addEventFilter( WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent );
        
        // Check for a saved user menu preference:
        checkPreferences();
        
        stage.setAlwaysOnTop(false);
        stage.show();
    }
    
    private void closeWindowEvent(WindowEvent event) {
    	for(EditorTab editorTab: editorTabs) {
	    	if( editorTab.hasUnsavedChanges() ) {
	    		Alert alert = saveAndExitAlert( "Confirm Exit", "Exit without saving?", "Unsaved changes will be lost." );
	            alert.initOwner( primaryStage.getOwner() );
	            Optional<ButtonType> response = alert.showAndWait();
	
	            if( response.isPresent() ) {
	            	ButtonType type = response.get();
	                if( type.getButtonData() == ButtonData.CANCEL_CLOSE ) {
	                    event.consume();
	                }
	                else if( type.getButtonData() == ButtonData.YES ) {
	                	editorTab.saveContent( primaryStage, false );
	                }
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
    	if( currentEditorTab.hasUnsavedChanges() ) {
    		Alert alert = saveOrContinue( "Confirm Replace", "Save changes before loading?", "Unsaved changes will be lost." );
            alert.initOwner( primaryStage.getOwner() );
            Optional<ButtonType> response = alert.showAndWait();

            if( response.isPresent() ) {
            	ButtonType type = response.get();
                if( type.getButtonData() == ButtonData.CANCEL_CLOSE ) {
                	return false;
                }
                if( type.getButtonData() == ButtonData.YES ) {
                	currentEditorTab.saveContent( primaryStage, false );
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
        Preferences prefs = Preferences.userNodeForPackage( Xliterator.class );

        prefs.put( scriptInPreference, scriptIn );
        prefs.put( variantInPreference, variantIn );
        prefs.put( scriptOutPreference, scriptOut );
        prefs.put( variantOutPreference, variantOut );
        prefs.put( transliterationIdPreference, selectedTransliteration );
        prefs.put( transliterationDirectionPreference, transliterationDirection ); // not relevant
        prefs.put( transliterationPreference, transliteration.toString() );
        
        textTab.savePreferences();
    }

    
    private void checkPreferences() {
        Preferences prefs = Preferences.userNodeForPackage( Xliterator.class );
        
        String json = prefs.get( transliterationPreference, null);
        
        if( json == null ) {
        	return;
        }
        transliteration = new JsonParser().parse(json).getAsJsonObject();
        
        scriptIn = prefs.get( scriptInPreference, null );
        variantIn = prefs.get( variantInPreference, null );

        setScriptIn( scriptIn, variantIn );
        setMenuItemSelection( inScriptMenu, scriptIn, variantIn, "in" );
        
        scriptOut  = prefs.get( scriptOutPreference, null );
        variantOut = transliteration.get( "name" ).getAsString();

        setScriptOut( scriptOut, variantOut, transliteration );
        setMenuItemSelection( outScriptMenu, scriptOut, variantOut, "out" );
        
        
        selectedTransliteration  = prefs.get( transliterationIdPreference, null );
        transliterationDirection = prefs.get( transliterationDirectionPreference, null );
        
    }
    
    Text scriptInText  = new Text( "[None]" );
    Text scriptOutText = new Text( "[None]" );
    Text resourceText  = new Text( "[None]" );
    // status bar reference:
    // https://jar-download.com/artifacts/org.controlsfx/controlsfx-samples/8.40.14/source-code/org/controlsfx/samples/HelloStatusBar.java
    private void updateStatusMessage() {
    	scriptInText.setStyle( "-fx-font-weight: bold;" );
    	scriptOutText.setStyle( "-fx-font-weight: bold;" );
    	resourceText.setStyle( "-fx-font-weight: bold;" );
    	scriptInText.setFill( Color.RED );
    	scriptOutText.setFill( Color.GREEN );
    	resourceText.setFill( Color.BLUE );
        
    	TextFlow flowIn = new TextFlow();
        TextFlow flowOut = new TextFlow();
        TextFlow flowVOut = new TextFlow();

        Text in  = new Text("In: ");
        in.setStyle("-fx-font-weight: bold;");
       
        Text out = new Text("Out: ");
        out.setStyle("-fx-font-weight: bold;");
        
        Text rout = new Text("Resource: ");
        rout.setStyle("-fx-font-weight: bold;");
        
        flowIn.getChildren().addAll(in, scriptInText );
        flowOut.getChildren().addAll(out, scriptOutText );
        flowVOut.getChildren().addAll(rout, resourceText );
       
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
    
    
    private void setScriptIn(String scriptIn, String variantIn) {
    	this.scriptIn = scriptIn;
    	this.variantIn = variantIn;
    	
		if( selectedEditorTab != null ) {
			Label label = (Label)selectedEditorTab.getGraphic();
			selectedEditorTab.setGraphic( null );
			selectedEditorTab.setText( label.getText() );
		}
    	
    	String scriptInMessage = scriptIn;
    	if(! "_base".equals( variantIn ) ) {
    		scriptInMessage +=  " / " + variantIn ;
    	}
    	scriptInText.setText( scriptInMessage );
    	
    	createOutScriptsMenu( scriptIn, variantIn );
        loadInternalMenuItem.setDisable( true );
    	filesTab.setScriptIn( scriptIn, variantIn );
    	textTab.setScriptIn( scriptIn, variantIn );
    }
    
    
    private void setScriptOut(String scriptOut, String variantOut, JsonObject transliteration) {
    	this.scriptOut = scriptOut;
    	this.variantOut = variantOut;
		
    	String scriptOutMessage = scriptOut;
    	if(! "_base".equals( variantOut ) ) {
    		scriptOutMessage +=  " / " + variantOut ;
    	}
    	scriptOutText.setText( scriptOutMessage );
    	
    	filesTab.setScriptOut( scriptOut );
    	textTab.setScriptOut( scriptOut );
    	setTransliteration( transliteration );
    	resourceText.setText( "Internal: " + selectedTransliteration );
        loadInternalMenuItem.setDisable( true );

    	// setMenuItemSelection( outScriptMenu, scriptOut );

    	
        loadInternalMenuItem.setDisable( false );
    }
    
    
    
    public void setTransliteration( JsonObject transliteration ) {
    	this.transliteration = transliteration;
    	this.selectedTransliteration  = transliteration.get( "path" ).getAsString();
    	this.transliterationDirection = transliteration.get( "direction" ).getAsString();
    	this.transliterationAlias = ( transliteration.has( "alias" ) ) ? transliteration.get( "alias" ).getAsString() : null ;
    	
		ArrayList<String> dependencies = new ArrayList<String>();
		if( transliteration.has( "dependencies" ) ) {
			JsonArray dependenciesJSON = transliteration.getAsJsonArray( "dependencies" );
			for (int k = 0; k < dependenciesJSON.size(); k++) {
				dependencies.add( dependenciesJSON.get(k).getAsString() );
			}
		}		
		this.transliterationDependencies = (dependencies.isEmpty()) ? null: dependencies;
		
    	filesTab.setTransliteration( transliteration );
    	textTab.setTransliteration( transliteration );
  	}
    /*
    private void setVariantOut(String variantOut) {
    	this.variantOut = variantOut;
    	variantOutText.setText( variantOut );
        loadInternalMenuItem.setDisable( false );
        
    	filesTab.setVariantOut( variantOut, selectedTransliteration, transliterationDirection, transliterationDependencies, transliterationAlias );
    	textTab.setVariantOut( variantOut, selectedTransliteration, transliterationDirection, transliterationDependencies, transliterationAlias );
    	setMenuItemSelection( outVariantMenu, variantOut );
    }
    */
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
    
    
    private void setUseEditor(String title) {
		if( inScriptMenuLast != null ) {
			inScriptMenuLast.setGraphic( null );
			inScriptMenuLast = null;
		}
		if( outScriptMenuLast != null ) {
			outScriptMenuLast.setGraphic( null );
			outScriptMenuLast = null;
		}
		outScriptMenu.getItems().clear();
    	
    	for(MenuItem item: inScriptMenu.getItems() ) {
    		if( item.getClass() == RadioMenuItem.class ) {
	    		RadioMenuItem rItem = (RadioMenuItem)item;
	    		if ( title.equals( rItem.getText() ) ) {
	    			rItem.setSelected( true );
	    	    	// TODO: before converting, check if the title matches an editor tab
	    		}
	    		else {
	    			rItem.setSelected( false );
	    		}
    		}
    		else if( item.getClass() == Menu.class ) {
    			Menu menu = (Menu)item;
    			menu.setGraphic( null );
    			for(MenuItem subItem: menu.getItems() ) {
    				if( subItem.getClass() == RadioMenuItem.class ) {
    					((RadioMenuItem)subItem).setSelected( false );
    				}
    			}
    		}
    	}
    }
    
    
    private void setMenuItemSelection( Menu menu, String script, String variant, String menuDirection ) {
    	variant = ( "_base".equals( variant) ) ? script : variant ;
    	
    	for(MenuItem item: menu.getItems() ) {
    		if( item.getClass() == RadioMenuItem.class ) {
	    		RadioMenuItem rItem = (RadioMenuItem)item;
	    		if ( rItem.getText().equals( script ) ) {
	    			rItem.setSelected( true );
	    	    	// TODO: before converting, check if the title matches an editor tab
	    		}
	    		else {
	    			rItem.setSelected( false );
	    		}
    		}
    		else if( item.getClass() == Menu.class ) {
    			Menu submenu = (Menu)item;
    			submenu.setGraphic( null );
    			if( submenu.getText().equals( script ) ) {
        			submenu.setGraphic( new ImageView( checkIcon ) );
        			if( "in".equals( menuDirection ) ) {
        				inScriptMenuLast = submenu;
        			}
        			else {
        				outScriptMenuLast = submenu;       				
        			}
	    			for( MenuItem subItem: submenu.getItems() ) {
	    				if( subItem.getClass() == RadioMenuItem.class ) {
	    					RadioMenuItem rsubItem = (RadioMenuItem)subItem;
	    					if( rsubItem.getText().equals( variant ) ) {
	    						rsubItem.setSelected( true );
	    					}
	    					else {
	    						rsubItem.setSelected( false );	    						
	    					}
	    				}
	    			}
    			}
    		}
    	}
    }
    
    
    private void saveDefaultFontSelections() {
    	filesTab.saveDefaultFontSelections();
    	textTab.saveDefaultFontSelections();
    	if( currentEditorTab != null ) {
    		currentEditorTab.saveDefaultFontSelections();
    	}
    }
    
    
    int newCounter = 1;
    private void createNewFile( String title, String type ) {  	
    	String template = ( "XML".equals(type) )
    			? "templates/icu-xml-template.xml"
    			: "templates/icu-text-template.txt" 
    	;
    	try {
    		// this should probably be changed to use createNewEditor 
    		transliteration = createEmptyTransliteration( template );
    		EditorTab newTab = createNewEditor( ("New " + newCounter), tabsMenu, visibleIcon, monochrome );
    		newTab.getEditor().loadResourceFile( template );
    	}
    	catch(Exception ex) {
        	errorAlert(ex, "Error opening: " + template );
    	}
    	
    	newCounter++;
    }
    
    
    private void launchSyntaxHightlightEditor( Stage stage ) {
    	if( syntaxHighlighterTab == null) {
    		// closed previously
    		syntaxHighlighterTab = new SyntaxHighlighterTab( "Syntax Highlighter" );
    	}
    	if( syntaxHighlighterTab.isLoaded() ) {
    		// this check may be unnecessary, it *should* not be able to enter this method if
    		// the syntax highlight editor is already loaded
    		return;
    	}
    	try {
    		syntaxHighlighterTab.load( stage, this );
    	}
    	catch( IOException ex ) {
    		errorAlert( ex, "An error occured reading stylesheet." );
    	}
    	tabpane.getSelectionModel().select(syntaxHighlighterTab);
    }
    
    
    public ArrayList<EditorTab> getEditorTabs() {
    	return editorTabs;
    }
    
    
    public EditorTab getActiveEditorTab() {
    	return currentEditorTab;
    }
    
    
    public EditorTab getSelectedEditorTab() {
    	return selectedEditorTab;
    }
    
    public ConvertTextTab getConvertTextTab() {
    	return textTab;
    }
    
    private void loadDemo(Menu tabsMenu, Image visibleIcon, ColorAdjust monochrome) {
    	textTab.clearAll();
    	textTab.setTextIn( "ሰላም ዓለም" );
    	
    	setScriptIn( "Ethiopic", "Amharic" );
    	// setMenuItemSelection( inScriptMenu, "Ethiopic", "Amharic" );
		JsonObject demoTransliteration = config.getTransliterationByName( "Ethi-Aethiopica_Latn" );
    	setScriptOut( "Latin", "Aethiopica", demoTransliteration );
    	// setMenuItemSelection( outScriptMenu, "Latin", "Aethiopica" );
    	
    	setTransliteration( demoTransliteration );
    	
    	try {
    		currentEditorTab = createNewEditor( selectedTransliteration, tabsMenu, visibleIcon, monochrome );
    		currentEditorTab.getEditor().loadResourceFile( selectedTransliteration );
    		currentEditorTab.setScriptIn( scriptIn, variantIn );
    		currentEditorTab.setScriptOut( scriptOut );
    		setUseEditor( selectedTransliteration );
        }
        catch(IOException ex) {
        	errorAlert(ex, "Error opening: " + selectedTransliteration );
        }
    }
    
    
    
    public File exportConvertedSpreadsheet( Stage stage, String jsonIndex ) {
    	File targetFile = null;
		try {			
			FileChooser fileChooser = new FileChooser();
			fileChooser.setInitialFileName( "transliterations.json" );
			fileChooser.setTitle( "Save Transliteratios Spreadsheet" );
	        fileChooser.setInitialDirectory(
	        		new File( System.getProperty( "user.home" ) )
	        );                 
	        fileChooser.getExtensionFilters().add(
	        		new FileChooser.ExtensionFilter( "*.json", "*.json" )
	        );
			

			targetFile = fileChooser.showSaveDialog( stage );
			if (targetFile == null) {  // the user cancelled the save
				return null;
			}
			// Create file 
			FileWriter fstream = new FileWriter( targetFile );
			BufferedWriter out = new BufferedWriter ( fstream );
			out.write( jsonIndex );
			
			//Close the output stream
			out.close();
		}
    	catch (Exception ex){
    		errorAlert( ex, "An error occured while saving the file \"" + targetFile.getPath() + "\":"  );
    	}
		
		return targetFile;
	}
    
    public void setEditorTransliterationDirection( String direction ) {
    	textTab.setEditorTransliterationDirection( direction );
    	filesTab.setEditorTransliterationDirection( direction );
    }

}
