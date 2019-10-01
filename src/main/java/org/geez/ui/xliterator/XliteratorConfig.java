package org.geez.ui.xliterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geez.convert.helpers.ICUHelper;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


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
    
    
    public List<String> getInScripts( boolean skipInternal ) {
		ArrayList<String> scriptList = null;
		
    	if( skipInternal ) {
    		scriptList = new ArrayList<String>();
            JsonObject scripts = config.getAsJsonObject("Scripts");
    		
            for(String inScriptKey: scripts.keySet() ) {
            	JsonObject inScript = scripts.getAsJsonObject( inScriptKey );
            	int outScriptSize = inScript.keySet().size();
            	int outScriptSkipCount = 0;
            	for(String outScript: inScript.keySet()) {
            		int variantSkipCount = 0;
                    JsonArray variants = inScript.getAsJsonArray( outScript );
                    for (int i = 0; i < variants.size(); i++) {
                        JsonObject variant = variants.get(i).getAsJsonObject();
                        if( variant.has( "visibility" ) && "internal".equals( variant.get( "visibility" ).getAsString() ) ) {
                        	variantSkipCount++;
                        }
    				}
                    if( variantSkipCount == variants.size() ) {
                    	// all children are internal, so we hid the script in
                    	outScriptSkipCount++;
                    }
    			}
            	
            	if(outScriptSkipCount <  outScriptSize) {
            		scriptList.add( inScriptKey );
            	}
    		}
    	}
    	else {
    		scriptList = new ArrayList<String>( config.getAsJsonObject("Scripts").keySet() );
    	}
    	
        Collections.sort(scriptList);
        return scriptList;
    }
    
    
    public List<String> getOutScripts( String inScript, boolean skipInternal ) {
		ArrayList<String> scriptList = null;
    	if( skipInternal ) {
    		scriptList = new ArrayList<String>();

        	JsonObject inScriptObj = config.getAsJsonObject("Scripts").getAsJsonObject( inScript );
        	for(String outScript: inScriptObj.keySet()) {
        		int variantSkipCount = 0;
                JsonArray variants = inScriptObj.getAsJsonArray( outScript );
                for (int i = 0; i < variants.size(); i++) {
                    JsonObject variant = variants.get(i).getAsJsonObject();
                    if( variant.has( "visibility" ) && "internal".equals( variant.get( "visibility" ).getAsString() ) ) {
                    	variantSkipCount++;
                    }
				}
                if( variantSkipCount < variants.size() ) {
                	// all children are internal, so we hid the script in
                	scriptList.add( outScript );
                }
			}
    		
    	}
    	else {
    		scriptList = new ArrayList<String>( config.getAsJsonObject("Scripts").keySet() );
    	}
    	
        Collections.sort(scriptList);
        return scriptList;
    }
    
    
    public JsonArray getVariants(String inScript, String outScript) {
    	return config.getAsJsonObject("Scripts").getAsJsonObject( inScript ).getAsJsonArray( outScript );
    }
    
    
    private void addVariantReverseEntry( JsonObject object, String from, String to, JsonObject variant ) {
    	if(! object.has( from ) ) {
    		object.add(from, new JsonObject());
    	}
    	
    	JsonObject inScript = object.getAsJsonObject( from );
    	if(! inScript.has( to ) ) {
    		inScript.add( to, new JsonArray() );
    	}
    	
    	JsonArray variants = inScript.getAsJsonArray( to );
    	// check if an element with the same "name" property already exists at this level, if so issue an error message and exit.
        for (int i = 0; i < variants.size(); i++) {
        	if( variants.get(i).getAsJsonObject().get( "name" ) != null  ) {
        		System.err.println( "Duplicate entry found at this level: " + variants.get(i).getAsJsonObject().get( "name" ).getAsString() );
        	}
        }
        JsonObject reverseVariant = variant.deepCopy();
        reverseVariant.remove( "direction" );
        reverseVariant.addProperty( "direction", "reverse" );
    	variants.add( reverseVariant );
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
    	List<String> inScripts = getInScripts( false );
    	for(String inScript: inScripts) {
    		List<String> outScripts = getOutScripts( inScript, false );
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
		for( String alias: dependencies ) {
	  		if( registered.contains( alias ) ) {
	  			continue;
	  		}
	  		// get source file and direction from alias
	  		JsonObject object = getTransliterationByAlias( alias );
	  		
	  		String path = object.get( "path" ).getAsString();
	  		String rulesFilePath = (path.contains( "/" ) ) ? path : "common/transforms/" + path ; 
	  		String direction = object.get( "direction" ).getAsString();
	  		
	  		registerTransliteration( alias, direction, rulesFilePath );
	  		registered.add( alias );
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
        JsonObject targetObject = config.deepCopy();

        JsonObject scripts = config.getAsJsonObject("Scripts");
        JsonObject targetScripts = targetObject.getAsJsonObject("Scripts");
        
        for(String inScriptKey: scripts.keySet() ) {
        	// System.out.println( inScriptKey  );
        	JsonObject inScript = scripts.getAsJsonObject( inScriptKey );
        	for(String outScript: inScript.keySet()) {
            	// System.out.println( "\t" + outScript  );
                JsonArray variants = inScript.getAsJsonArray( outScript );
                for (int i = 0; i < variants.size(); i++) {
                	
                    JsonObject variant = variants.get(i).getAsJsonObject();

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
                }
        	}
        }
        
        // Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // System.out.println( gson.toJson( targetObject ) );
        
    }
    
}