package org.geez.ui.xliterator;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.SpreadsheetMLPackage;
import org.docx4j.openpackaging.parts.SpreadsheetML.WorkbookPart;
import org.docx4j.openpackaging.parts.SpreadsheetML.WorksheetPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlsx4j.org.apache.poi.ss.usermodel.DataFormatter;
import org.xlsx4j.sml.Cell;
import org.xlsx4j.sml.Row;
import org.xlsx4j.sml.SheetData;
import org.xlsx4j.sml.Worksheet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class JsonIndexGenerator {
	
	private static Logger log = LoggerFactory.getLogger(JsonIndexGenerator.class);						

	private Stage stage = null;
	
	public JsonIndexGenerator( Stage stage ) {
		this.stage = stage;
	}

	
	public String generateIndex( File spreadsheetFile ) {

		try {
			
			// Open a document from the file system
			SpreadsheetMLPackage xlsxPkg = SpreadsheetMLPackage.load( spreadsheetFile );		
				
			WorkbookPart workbookPart = xlsxPkg.getWorkbookPart();
			WorksheetPart sheet = workbookPart.getWorksheet(0);
		
			DataFormatter formatter = new DataFormatter();

			return convertContext(sheet, formatter);
		}
		catch( Exception ex ) {
			System.err.println( ex );
		}
		
		return null;
	}
	
	
	private static String convertContext(WorksheetPart sheet, DataFormatter formatter) throws Docx4JException {

		Worksheet ws = sheet.getContents();
		SheetData data = ws.getSheetData();
		
		JsonObject index = new JsonObject();
		JsonObject scripts = new JsonObject();
		index.add( "Scripts", scripts );
		
		int FILE           = 0; // A
		int SCRIPT_IN      = 1; // B
		int VARIANT_IN     = 2; // C
		int SCRIPT_OUT     = 3; // D
		int VARIANT_OUT    = 4; // E
		int DIRECTION      = 5; // F
		int ALIAS          = 6; // G
		int BACKWARD_ALIAS = 7; // H
		int VISIBILITY     = 8; // I
		int DEPENDENCIES   = 9; // J
		int TOOLTIP        =10; // K
		
		List<Row> rows = data.getRow();
		rows.remove(0); //shift
		
		for (Row r : rows ) {
			System.out.println("row " + r.getR() );		
			
			String path = null, scriptIn = null, variantIn = "_base", scriptOut = null,
					variantOut = "_base", direction = null, alias = null, backwardAlias = null,
					dependencies = null, visibility = null, tooltip = null;
			
			
			for( Cell c: r.getC() ) {

				switch( c.getR().charAt(0) ) {
					case 'A':
						path      = formatter.formatCellValue( c );
						break;
						
					case 'B':
						scriptIn  = formatter.formatCellValue( c );
						break;
						
					case 'C':
						variantIn = formatter.formatCellValue( c );
						if( variantIn == null ) {
							variantIn = "_base";
						}
						else {
							variantIn = variantIn.trim();
							if( "".equals( variantIn ) ) {
								variantIn = "_base";
							}
						}
						break;
						
					case 'D':
						scriptOut = formatter.formatCellValue( c );
						break;
						
					case 'E':
						variantOut = formatter.formatCellValue( c );
						if( variantOut == null ) {
							variantOut = "_base";
						}
						else {
							variantOut = variantOut.trim();
							if( "".equals( variantOut ) ) {
								variantOut = "_base";
							}
						}
						break;
						
					case 'F':
						direction = formatter.formatCellValue( c );
						break;
						
					case 'G':
						alias = formatter.formatCellValue( c );
						if( alias != null ) {
							alias = alias.trim();
							if( "".equals( alias ) ) {
								alias = null;
							}
						}
						break;
							
					case 'H':
						backwardAlias = formatter.formatCellValue( c );
						if( backwardAlias != null ) {
							backwardAlias = backwardAlias.trim();
							if( "".equals( backwardAlias) ) {
								backwardAlias = null;
							}
						}
						break;
						
					case 'I':
							visibility = formatter.formatCellValue( c );
							if( visibility != null ) {
								visibility = visibility.trim();
								if( "".equals( visibility) ) {
									visibility = null;
								}
							}
							break;

					case 'J':
						dependencies = formatter.formatCellValue( c );
						if( dependencies != null ) {
							dependencies = dependencies.trim();
							if( "".equals( dependencies ) ) {
								dependencies = null;
							}
						}
						break;
	
					case 'K':
						tooltip = formatter.formatCellValue( c );
						if( tooltip != null ) {
							tooltip = tooltip.trim();
							if( "".equals( tooltip) ) {
								tooltip = null;
							}
						}
						break;	
				}
			}

			if(! scripts.has( scriptIn ) ) {
				scripts.add( scriptIn, new JsonObject() );
			}
			JsonObject scriptInObject = scripts.get( scriptIn ).getAsJsonObject();
			if(! scriptInObject.has( variantIn ) ) {
				scriptInObject.add( variantIn,  new JsonObject() );
			}
			JsonObject variantInObject = scriptInObject.get( variantIn ).getAsJsonObject();
			if(! variantInObject.has( scriptOut ) ) {
				variantInObject.add( scriptOut,  new JsonArray() );
			}
			JsonArray scriptOutArray = variantInObject.get( scriptOut ).getAsJsonArray();
			JsonObject variantOutObject = new JsonObject();
			variantOutObject.addProperty( "path", path );
			variantOutObject.addProperty( "name", variantOut );
			variantOutObject.addProperty( "direction", direction );
			if( (alias != null) ) {
				variantOutObject.addProperty( "alias", alias );
			}
			if( (backwardAlias != null) ) {
				variantOutObject.addProperty( "backwardAlias", backwardAlias );
			}
			if( (visibility != null) ) {
				variantOutObject.addProperty( "visibility", visibility );
			}
			if( (tooltip != null) ) {
				variantOutObject.addProperty( "tooltip", tooltip );
			}
			if( (dependencies != null) ) {
				JsonArray dependencyArray = new JsonArray();
				String[] tempArray = dependencies.split( "," );
				for(String dependency: tempArray) {
					dependencyArray.add( dependency );
				}
				variantOutObject.add( "dependencies", dependencyArray );
			}
			
			scriptOutArray.add( variantOutObject );
		}
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		return gson.toJson( index );
		
	}
	
	public void saveContentToNewFile( String content ) throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle( "Save index.json" );
		fileChooser.setInitialFileName( "index.json" );

		File file = fileChooser.showSaveDialog( stage );
		if (file == null) {  // the user cancelled the save
			return;
		}
		// Create file 
		FileWriter fstream = new FileWriter( file );
		BufferedWriter out = new BufferedWriter ( fstream );
		

		out.write( content );
		
		//Close the output stream
		out.close();
    }
	
}