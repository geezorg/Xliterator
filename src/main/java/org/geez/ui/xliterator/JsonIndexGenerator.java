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

import com.google.gson.JsonObject;

import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class JsonIndexGenerator {
	
	private static Logger log = LoggerFactory.getLogger(JsonIndexGenerator.class);						

	private Stage stage = null;
	
	public JsonIndexGenerator( Stage stage ) {
		this.stage = stage;
	}

	
	public void generateIndex( File spreadsheetFile ) {

		try {
			
			// Open a document from the file system
			SpreadsheetMLPackage xlsxPkg = SpreadsheetMLPackage.load( spreadsheetFile );		
				
			WorkbookPart workbookPart = xlsxPkg.getWorkbookPart();
			WorksheetPart sheet = workbookPart.getWorksheet(0);
		
			DataFormatter formatter = new DataFormatter();

			// Now lets print the cell content
			displayContent(sheet, formatter);
		}
		catch( Exception ex ) {
			System.err.println( ex );
		}
	}
	
	
	private static void displayContent(WorksheetPart sheet, DataFormatter formatter) throws Docx4JException {

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
			
			String file = null, scriptIn = null, variantIn = null, scriptOut = null,
					variantOut = null, direction = null, alias = null, backwardAlias = null,
					dependencies = null, visibility = null, tooltip = null;
			
			
			for( Cell c: r.getC() ) {

				switch( c.getR().charAt(0) ) {
					case 'A':
						file      = formatter.formatCellValue( c );
						break;
						
					case 'B':
						scriptIn  = formatter.formatCellValue( c );
						break;
						
					case 'C':
						variantIn = formatter.formatCellValue( c );
						break;
						
					case 'D':
						scriptOut = formatter.formatCellValue( c );
						break;
						
					case 'E':
						variantOut = formatter.formatCellValue( c );
						break;
						
					case 'F':
						direction = formatter.formatCellValue( c );
						break;
						
					case 'G':
						alias = formatter.formatCellValue( c );
						break;
							
					case 'H':
						backwardAlias = formatter.formatCellValue( c );
						break;
						
					case 'I':
						dependencies = formatter.formatCellValue( c );
						break;
					
					case 'J':
						visibility = formatter.formatCellValue( c );
						break;
						
					case 'K':
						tooltip = formatter.formatCellValue( c );
						break;	
				}
			}

			if(! index.has( scriptIn ) ) {
				index.add( scriptIn, new JsonObject() );
			}

		}
		
		System.err.println( index.toString() );
		
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