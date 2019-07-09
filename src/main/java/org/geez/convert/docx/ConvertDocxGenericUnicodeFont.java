package org.geez.convert.docx;

import java.io.File;

/*
 * The non-maven way to build the jar file:
 *
 * javac -Xlint:deprecation -cp docx4j-6.0.1.jar:dependencies/commons-io-2.5.jar:../icu4j-63_1.jar:dependencies/slf4j-api-1.7.25.jar:slf4j-1.7.25 *.java
 * jar -cvf convert.jar org/geez/convert/docx/*.class org/geez/convert/tables/
 * java -cp convert.jar:docx4j-6.0.1.jar:dependencies/*:../icu4j-63_1.jar:slf4j-1.7.25/slf4j-nop-1.7.25.jar org.geez.convert.docx.ConvertDocx geeznewab myFile-In.docx myFile-Out.docx
 *
 */


public class ConvertDocxGenericUnicodeFont extends ConvertDocx {

	public ConvertDocxGenericUnicodeFont( final File inputFile, final File outputFile, String rulesFile ) {
		super( inputFile, outputFile );
		this.initialize( rulesFile, "Brana II" );
	}

}
