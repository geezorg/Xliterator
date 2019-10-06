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
		
		int FILE           = 0;
		int SCRIPT_IN      = 1;
		int VARIANT_IN     = 2;
		int SCRIPT_OUT     = 3;
		int VARIANT_OUT    = 4;
		int DIRECTION      = 5;
		int ALIAS          = 6;
		int BACKWARD_ALIAS = 7;
		int VISIBILITY     = 8;
		int DEPENDENCIES   = 9;
		int TOOLTIP        =10;
		
		List<Row> rows = data.getRow();
		rows.remove(0); //shift
		
		for (Row r : rows ) {
			System.out.println("row " + r.getR() );		
			
			List<Cell> columns = r.getC();
			
			String file      = formatter.formatCellValue( columns.get(FILE) );
			String scriptIn  = formatter.formatCellValue( columns.get(SCRIPT_IN) );

			if( columns.get(VARIANT_IN) != null ) {
				String variantIn = formatter.formatCellValue( columns.get(VARIANT_IN) );
			}			
			String scriptOut = formatter.formatCellValue( columns.get(SCRIPT_OUT) );
			if( columns.get(VARIANT_OUT) != null ) {
				String variantOut = formatter.formatCellValue( columns.get(VARIANT_OUT) );
			}

			
			String direction = formatter.formatCellValue( columns.get(DIRECTION) );
			if( columns.get(ALIAS) != null ) {
				String alias = formatter.formatCellValue( columns.get(ALIAS) );
			}
			if( columns.get(BACKWARD_ALIAS) != null ) {
				String backwardAlias = formatter.formatCellValue( columns.get(BACKWARD_ALIAS) );
			}
			if( columns.get(DEPENDENCIES) != null ) {
				String dependencies = formatter.formatCellValue( columns.get(DEPENDENCIES) );
			}
			if( columns.get(VISIBILITY) != null ) {
				String visibility = formatter.formatCellValue( columns.get(VISIBILITY) );
			}
			if( columns.get(TOOLTIP) != null ) {
				String tooltip = formatter.formatCellValue( columns.get(TOOLTIP) );
			}

		}
		
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