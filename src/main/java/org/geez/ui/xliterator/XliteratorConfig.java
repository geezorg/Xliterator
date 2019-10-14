package org.geez.ui.xliterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.geez.convert.helpers.ICUHelper;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class XliteratorConfig extends ICUHelper {
	
	private String userConfigFilePath = "transliterations.json";
	private String transformsIndex    = "common/transforms/index.json";
	
	private JsonObject config;
    
	public XliteratorConfig()  throws URISyntaxException, IOException {
		load( userConfigFilePath );
	}
	
	public XliteratorConfig( String configFilePath ) throws URISyntaxException, IOException {
		load( configFilePath );
	}
    
    
    public JsonObject getInScripts() {
		JsonObject scriptsObject = new JsonObject();
        JsonObject scripts = config.getAsJsonObject("Scripts");
		
        for(String inScriptKey: scripts.keySet() ) {  // in-scripts
        	JsonObject inScript = scripts.getAsJsonObject( inScriptKey );
        	
        	int inVariantSize = inScript.keySet().size();
        	int inVariantSkipCount = 0;
			ArrayList<String> inVariantsList = new ArrayList<String>();
			
			boolean hasBase = false;
        	
        	for(String inVariantKey: inScript.keySet()) { // in-variants
        		int outScriptSkipCount = 0;
        		
        		JsonObject inVariant = inScript.getAsJsonObject( inVariantKey );
        		
        		int outScriptSize = inVariant.keySet().size();
        		
        		
        		for(String outScriptKey: inVariant.keySet()) { // out-scripts
        		
        			JsonArray outVariants = inVariant.getAsJsonArray( outScriptKey );
        			
        			int outVariantSkipCount = 0;
        			for (int i = 0; i < outVariants.size(); i++) { // out-variants
        				JsonObject outVariant = outVariants.get(i).getAsJsonObject();
        				if( outVariant.has( "visibility" ) && "internal".equals( outVariant.get( "visibility" ).getAsString() ) ) {
        					outVariantSkipCount++;
        				}
        			}
        			if( outVariantSkipCount == outVariants.size() ) {
        				// all children are internal, so we hid the script in
        				outScriptSkipCount++;
        			}
        			else {
        				if(! scriptsObject.has( inScriptKey ) ) {
        					scriptsObject.add( inScriptKey, new JsonArray() );
        				}
        				if( "_base".equals( inVariantKey ) ) {
        					hasBase = true;
        				}
        				else {
	        				// JsonArray inVariantsOfInScript = scriptsObject.getAsJsonArray( inScriptKey );
	        				if(! inVariantsList.contains( inVariantKey ) ) {
	        					// inVariantsOfInScript.add( inVariantKey );
	        					inVariantsList.add( inVariantKey );
	        				}
        				}
        			}
        		}
        	}
        	
        	if( inVariantsList.size() > 0 ) {
        		JsonArray inVariantsOfInScript = scriptsObject.getAsJsonArray( inScriptKey );
        		if( hasBase ) {
					inVariantsList.add( 0, "_base" );
        		}
        		for( String inVariantKey: inVariantsList ) {
        			inVariantsOfInScript.add( inVariantKey );
        		}
        	}
			hasBase = false;
		}

        return scriptsObject;
    }
    
    
    public List<String> getInScriptsList() {
    	ArrayList<String> scriptList = new ArrayList<String>( config.getAsJsonObject("Scripts").keySet() );
    	Collections.sort(scriptList);
    	return scriptList;
    }
    
    
    public List<String> getInVariantsOfInScriptList(String inScript) {
    	ArrayList<String> inVaraiantsList = new ArrayList<String>( config.getAsJsonObject("Scripts").getAsJsonObject(inScript).keySet() );
    	Collections.sort(inVaraiantsList);
    	return inVaraiantsList;
    }
    
    
    public List<String> getOutScriptOfInScriptAndInVariantList(String inScript, String inVariant) {
    	ArrayList<String> outScriptList = new ArrayList<String>( config.getAsJsonObject("Scripts").getAsJsonObject(inScript).getAsJsonObject( inVariant ).keySet() );
    	Collections.sort(outScriptList);
    	return outScriptList;
    }
    

    public JsonArray getOutVariantsOfInScriptInVariantAndOutScriptArray(String inScript, String inVariant, String outScript) {
    	return config.getAsJsonObject("Scripts").getAsJsonObject(inScript).getAsJsonObject( inVariant ).getAsJsonArray( outScript );
    }
    
    
    public JsonObject getOutScriptsOfInScriptAndInVariant( String inScript, String inVariant ) {
		JsonObject scriptsObject = new JsonObject();
    	JsonObject inVariantObject = config.getAsJsonObject("Scripts").getAsJsonObject( inScript ).getAsJsonObject( inVariant );
    	
    	JsonObject baseObject = null;
    	int outScriptSkipCount = 0;
    	for( String outScriptKey: inVariantObject.keySet() ) {
    		
			JsonArray outVariants = inVariantObject.getAsJsonArray( outScriptKey );
			ArrayList<JsonObject> outVariantsList = new ArrayList<JsonObject>();
			int outVariantSkipCount = 0;
			
			for (int i = 0; i < outVariants.size(); i++) { // out-variants
				JsonObject outVariant = outVariants.get(i).getAsJsonObject();
				String outVariantKey = outVariant.get("name" ).getAsString();
				if( outVariant.has( "visibility" ) && "internal".equals( outVariant.get( "visibility" ).getAsString() ) ) {
					outVariantSkipCount++;
				}
				else {
					if(! scriptsObject.has( outScriptKey ) ) {
						scriptsObject.add( outScriptKey, new JsonArray() );
					}
					if( "_base".equals( outVariantKey ) ) {
						baseObject = outVariant.deepCopy();
					}
					else {
	    				outVariantsList.add( outVariant.deepCopy() );
					}
				}
			}
				
				
        	//if( outVariantsList.size() > 0 ) {
        		JsonArray outVariantsOfInScript = scriptsObject.getAsJsonArray( outScriptKey );
        		if( baseObject != null ) {
        			outVariantsList.add( 0, baseObject);
        		}
        		for( JsonObject outVariant: outVariantsList ) {
        			outVariantsOfInScript.add( outVariant );
        		}
        	//}
        	baseObject = null;
			
    	}
        	
        return scriptsObject;
    }
    
    
    public List<String> getOutScriptsOfInScriptList( String inScript ) {

    	HashSet<String> outScriptSet = new HashSet<String>();
    	
    	for(String inVariant: config.getAsJsonObject("Scripts").getAsJsonObject( inScript ).keySet() ) {
    		outScriptSet.addAll( config.getAsJsonObject("Scripts").getAsJsonObject( inScript ).getAsJsonObject( inVariant ).keySet() );
    	}
    	
    	ArrayList<String> scriptList = new ArrayList<String>( outScriptSet );
    	Collections.sort(scriptList);
    	return scriptList;
    }
    
    
    public JsonArray getVariants(String inScript, String outScript) {
    	return config.getAsJsonObject("Scripts").getAsJsonObject( inScript ).getAsJsonArray( outScript );
    }

    
    private void addVariantReverseEntry( JsonObject bothDirectionScripts, String inScript /* to */, String inVariant, String outScript /* from */, JsonObject outVariant ) {
    	// Reverse the path:
    	//
    	// outScript
    	//   outVariant
    	//     inScript
    	//       inVariants
    	
    	if(! bothDirectionScripts.has( outScript ) ) {
    		bothDirectionScripts.add( outScript, new JsonObject() );
    	}
    	JsonObject outScriptObject = bothDirectionScripts.getAsJsonObject( outScript );
    	
    	String outVariantName = outVariant.get( "name" ).getAsString();
    	if(! outScriptObject.has( outVariantName ) ) {
    		outScriptObject.add( outVariantName, new JsonObject() );
    	}
    	JsonObject outVariantObject = outScriptObject.getAsJsonObject( outVariantName );
    	
    	if(! outVariantObject.has( inScript ) ) {
    		outVariantObject.add( inScript, new JsonArray() );
    	} 	
    	JsonArray inScripts = outVariantObject.getAsJsonArray( inScript );
    	
    	
    	// check if an element with the same "name" property already exists at this level, if so issue an error message and exit.
        for (int i = 0; i < inScripts.size(); i++) {
        	if( inScripts.get(i).getAsJsonObject().get( "name" ) != null  ) {
        		System.err.println( "Duplicate entry found at this level: " + inScripts.get(i).getAsJsonObject().get( "name" ).getAsString() );
        	}
        }
        JsonObject reverseVariant = outVariant.deepCopy();
        reverseVariant.remove( "direction" );
        reverseVariant.addProperty( "direction", "reverse" );
        inScripts.add( reverseVariant );
    }
    
    
    private void addSubVariantReverseEntry( JsonObject object, String from, String to, JsonObject subvariant, String subVariantKey ) {
    	if(! object.has( from ) ) {
    		object.add(from, new JsonObject());
    	}
    	
    	JsonObject inScript = object.getAsJsonObject( from );
    	if(! inScript.has( to ) ) {
    		inScript.add( to, new JsonArray() );
    	}
    	
        JsonObject reverseSubvariant = subvariant.deepCopy();
        reverseSubvariant.remove( "direction" );
        reverseSubvariant.addProperty( "direction", "reverse" );
    	
    	
    	JsonArray variants = inScript.getAsJsonArray( to );
    	boolean subvariantAdded = false;
    	for (int i = 0; i < variants.size(); i++) {
    		JsonObject variant = variants.get(i).getAsJsonObject();
    		if( variant.get("name") != null ) {
    			continue;
    		}
    		
    		if( variant.get( subVariantKey ) == null ) {
    			variant.add( subVariantKey, new JsonArray() );
    		}
    			
    		JsonArray subvariants = variant.getAsJsonArray( subVariantKey );
    		subvariants.add( reverseSubvariant );
    		subvariantAdded = true;
    	}
    	if( (variants.size() == 0) || (subvariantAdded == false) ) {
    		JsonArray subvariants = new JsonArray();
    		subvariants.add( reverseSubvariant );
    		JsonObject oSubvariant = new JsonObject();
    		oSubvariant.add( subVariantKey, subvariants );
    		variants.add( oSubvariant );
    	}
    }
    
    
    
    public JsonObject getTransliterationByAlias( String alias ) {
    	List<String> inScripts = getInScriptsList();
    	for(String inScript: inScripts) {
			List<String> inVariants = getInVariantsOfInScriptList(inScript);
	    	for(String inVariant: inVariants) {
	    		List<String> outScripts = getOutScriptOfInScriptAndInVariantList( inScript, inVariant );
	    		for(String outScript: outScripts) {
	    			JsonArray outVariants = getOutVariantsOfInScriptInVariantAndOutScriptArray(inScript, inVariant, outScript);
	    	    	for (int i = 0; i < outVariants.size(); i++) {
	    	    		JsonObject variant = outVariants.get(i).getAsJsonObject();
    	    			if( variant.has( "alias" ) && ( alias.equals( variant.get("alias").getAsString() ) ) ) {
    	    				return variant;
    	    			}
	    	    	}   		
	    		}


    	    }
    	}
    	
    	return null;
    }
    
    
    
    public JsonObject getTransliterationByName( String name ) {
    	List<String> inScripts = getInScriptsList();
    	for(String inScript: inScripts) {
			List<String> inVariants = getInVariantsOfInScriptList(inScript);
	    	for(String inVariant: inVariants) {
	    		List<String> outScripts = getOutScriptOfInScriptAndInVariantList( inScript, inVariant );
	    		for(String outScript: outScripts) {
	    			JsonArray outVariants = getOutVariantsOfInScriptInVariantAndOutScriptArray(inScript, inVariant, outScript);
	    	    	for (int i = 0; i < outVariants.size(); i++) {
	    	    		JsonObject variant = outVariants.get(i).getAsJsonObject();
	    	    		String variantName = variant.get( "source" ).getAsString() + "-" + variant.get( "target" ).getAsString();
    	    			if( variantName.equals( name ) ) {
    	    				return variant;
    	    			}
	    	    	}   		
	    		}


    	    }
    	}
    	
    	return null;
    }
    
    
    public JsonObject getTransliterationByAliasOld( String alias ) {
    	List<String> inScripts = getInScriptsList();
    	for(String inScript: inScripts) {
    		List<String> outScripts = getOutScriptsOfInScriptList( inScript );
    		for(String outScript: outScripts) {
    			JsonArray variants = getVariants(inScript, outScript);
    	    	for (int i = 0; i < variants.size(); i++) {
    	    		JsonObject variant = variants.get(i).getAsJsonObject();
    	    		if( variant.has( "name" ) ) {
    	    			if( variant.has( "alias" ) && ( alias.equals( variant.get("alias").getAsString() ) ) ) {
    	    				return variant;
    	    			}
    	    		}
    	    		else {
    	    			for(String subVariantKey: variant.keySet() ) {
    	    				JsonArray subvariants = variant.getAsJsonArray( subVariantKey );
    	    		    	for (int j = 0; j < subvariants.size(); j++) {
    	    		    		JsonObject subvariant = subvariants.get(j).getAsJsonObject();
    	    	    			if( subvariant.has( "alias" ) && ( alias.equals( subvariant.get("alias").getAsString() ) ) ) {
    	    	    				return subvariant;
    	    	    			}
    	    		    	}
    	    			} 

    	    		}
    	    	}
    		}
    	}
    	
    	return null;
    }
    
    
	protected ArrayList<String> registered = new ArrayList<String>();
	
	private String readResourceFile( String filePath ) throws IOException {
		ClassLoader classLoader = this.getClass().getClassLoader();
    	
		InputStream inputStream = classLoader.getResourceAsStream( filePath ); 
		BufferedReader br = new BufferedReader( new InputStreamReader(inputStream, "UTF-8") );
		StringBuffer sb = new StringBuffer();
		String line = null;
		while ( (line = br.readLine()) != null) {
			sb.append( line + "\n" );
		}
		br.close();
		
		return sb.toString();
	}
	
	// Registering a transliteration instance in the XliteratorConfig seems to be going
	// beyond its primary purpose (an interface to the index.json file , but not presently
	// clear where to place it
	protected void registerDependencies(ArrayList<String> dependencies) throws IOException, SAXException  {
    	// ConvertDocxGenericUnicodeFont converter = new ConvertDocxGenericUnicodeFont();
		for( String name: dependencies ) {
	  		if( registered.contains( name ) ) {
	  			continue;
	  		}
	  		// get source file and direction from alias
	  		JsonObject object = getTransliterationByName( name );
	  		
	  		String path = object.get( "path" ).getAsString();
	  		String rulesFilePath = (path.contains( "/" ) ) ? path : "common/transforms/" + path ; 
	  		String direction = object.get( "direction" ).getAsString();
	  		
	  		registerTransliterationFile( name, direction, rulesFilePath );
	  		registered.add( name );
		}
	}
    
    
    public void load ( String configFilePath ) throws IOException {
    	String json = null;
    	File configFile = new File( configFilePath );

    	if( configFile.exists() ) {
    		json = readLineByLineJava8( configFilePath );
    	}
    	else {
    		json = readResourceFile( transformsIndex );
    	}
		
        config = new JsonParser().parse(json).getAsJsonObject();
        JsonObject configClone = config.deepCopy();

        JsonObject scripts = config.getAsJsonObject( "Scripts" );
        JsonObject bothDirectionScripts = configClone.getAsJsonObject( "Scripts" );
        
        for(String inScriptKey: scripts.keySet() ) {
        	System.out.println( inScriptKey  );
        	JsonObject inScript = scripts.getAsJsonObject( inScriptKey );
        	for(String inVariantKey: inScript.keySet()) {
            	System.out.println( "\t" + inVariantKey  );
            	JsonObject inVariant = inScript.getAsJsonObject( inVariantKey );
            	for(String outScriptKey: inVariant.keySet()) {
                	System.out.println( "\t\t" + outScriptKey  );
                JsonArray outVariants = inVariant.getAsJsonArray( outScriptKey );
                for (int i = 0; i < outVariants.size(); i++) {
                	
                    JsonObject outVariant = outVariants.get(i).getAsJsonObject();
                    String outVariantKey = outVariant.get( "name" ).getAsString();
                    
                    System.out.println( "\t\t\tname: " + outVariantKey );
                    
            		if( outVariant.get("direction").getAsString().equals( "both" ) ) {
            			addVariantReverseEntry( bothDirectionScripts, inScriptKey, inVariantKey, outScriptKey, outVariant );
            		}

                    /*
                	if( variant.has( "name" ) ) {
                		// System.out.println( "\t\t" + variant.get("name").getAsString() );
                		if( variant.get("direction").getAsString().equals( "both" ) ) {
                			addVariantReverseEntry( targetScripts, outScript, inScriptKey, variant );
                		}
                	}
                	else {
                		for(String subVariantKey: variant.keySet()) {
                			// System.out.println( "\t\t\t" + subVariantKey );
                			if(! variant.get( subVariantKey ).isJsonArray() ) {
                				// unknown
                        		System.err.println( "Unrecognized structure." );
                        		System.exit(0);
                			}
                            JsonArray subvariants = variant.getAsJsonArray( subVariantKey );
                            for (int j = 0; j < subvariants.size(); j++) {
                                JsonObject subvariant = subvariants.get(j).getAsJsonObject();
                            	if( subvariant.has( "name" ) ) {
                            		// System.out.println( "\t\t\t\t" + subvariant.get("name").getAsString() );
                            		if( subvariant.get("direction").getAsString().equals( "both" ) ) {
                            			addSubVariantReverseEntry( targetScripts, outScript, inScriptKey, subvariant, subVariantKey );
                            		}
                            	}
                            	else {
                            		// unknown
                            		System.err.println( "Unrecognized structure." );
                            		System.exit(0);
                            	}
                            }
                		}
                	}
                	*/
                }
        	}
        }
        }
        // Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // System.out.println( gson.toJson( targetObject ) );
        
    } 
    
    
    public File exportReferenceSpreadsheet( Stage stage ) {
    	File targetFile = null;
		try {
			String spreadsheetName = "ICU-Transliterations.xlsx";
			
			FileChooser fileChooser = new FileChooser();
			fileChooser.setInitialFileName( spreadsheetName );
			fileChooser.setTitle( "Save Transliteratios Spreadsheet" );
			

			targetFile = fileChooser.showSaveDialog( stage );
			if (targetFile == null) {  // the user cancelled the save
				return null;
			}
			// Create file 
			// FileWriter fstream = new FileWriter( targetFile );
			// BufferedWriter out = new BufferedWriter ( fstream );
			
			InputStream in = ClassLoader.getSystemResourceAsStream( "data/" + spreadsheetName );
			OutputStream out = new FileOutputStream( targetFile );
			IOUtils.copy(in, out);
			
			//Close the output stream
			//out.close();
		}
    	catch (Exception ex){
    		errorAlert( ex, "An error occured while saving the file \"" + targetFile.getPath() + "\":"  );
    	}
		
		return targetFile;
	}
    
	
    
	protected void errorAlert( Exception ex, String header ) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle( "An Exception has occured" );
        alert.setHeaderText( header );
        alert.setContentText( ex.getMessage() );
        alert.showAndWait();
	}
    
}