package org.geez.transliterate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class XliteratorConfig {
	
	private String userConfigFilePath = "transliterations.json";
	private String transformsIndex = "common/transforms/index.json";
	
	private JsonObject config;
    
	public XliteratorConfig()  throws URISyntaxException, IOException {
		load( userConfigFilePath );
	}
	
	public XliteratorConfig( String configFilePath )  throws URISyntaxException, IOException {
		load( configFilePath );
	}
	
	
    private String readLineByLineJava8(String filePath)
    {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }
    
    public List<String> getInScripts() {    
        return new ArrayList<String>( config.getAsJsonObject("Scripts").keySet() );
    }
    
    
    public List<String> getOutScripts(String inScript ) {
        return new ArrayList<String>( config.getAsJsonObject("Scripts").getAsJsonObject( inScript ).keySet() );
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
    
    
    public void load ( String configFilePath ) throws URISyntaxException, IOException {
    	String json = null;
    	File configFile = new File( configFilePath );
    	Path configPath = null;
    	if( configFile.exists() ) {
    		//configPath = Paths.get( configFilePath );
    		json = readLineByLineJava8( configFilePath );
    	} else {
    		ClassLoader classLoader = this.getClass().getClassLoader();
    	
    		InputStream inputStream = classLoader.getResourceAsStream( transformsIndex ); 
    		BufferedReader br = new BufferedReader( new InputStreamReader(inputStream, "UTF-8") );
    		StringBuffer sb = new StringBuffer();
    		String line = null;
    		while ( (line = br.readLine()) != null) {
    			sb.append( line + "\n" );
    		}
    		br.close();
    		json = sb.toString();
    		// configPath = Paths.get( classLoader.getResource( transformsIndex ).toURI() );
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