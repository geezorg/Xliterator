package org.geez.convert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;

public abstract class DocumentProcessor implements Callable<Void> {
	
    protected File inputFile = null, outputFile = null;
	protected List<File> inputFileList = null , outputFileList = null;
	protected boolean setProgress = true;
	protected int currentIndex = -1;

	
    protected final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper();
    
    public void setFiles( final File inputFile, final File outputFile ) {
    	this.inputFile  = inputFile;
    	this.outputFile = outputFile;
    } 
	
	public void setFileList( List<File> inputFileList ) {
		this.inputFileList = inputFileList;
	}
    
    public ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty() ;
    }   
    
    public final double getProgress() {
        return progressProperty().get();
    }
    
	@Override
	public Void call() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    public DocumentProcessor() {
    }  

    public DocumentProcessor( List<File> inputFileList ) {
    	setFileList( inputFileList );
    }

    public DocumentProcessor( final File inputFile, final File outputFile ) {
    	setFiles( inputFile, outputFile );
    }
    
    public File getOutputFile() {
    	return outputFile;
    }
    public File getOutputFile(int index) {
    	if( outputFileList == null )
    		return getOutputFile();
    	
    	return outputFileList.get(index);
    }
    
    public void generateOutputFiles( String stem, String extension ) {
    	outputFileList = new ArrayList<File> ( inputFileList.size());
    	outputFileList.clear();
    	for(File inputFile: inputFileList) {
        	String inputFilePath = inputFile.getPath();
        	String outputFilePath = inputFilePath.replaceAll( "\\.docx", "-" + stem.replace( " ", "-" ) + extension );
    		File outputFile = new File ( outputFilePath );
    		outputFileList.add( outputFile );
    	}
    }
    
    public void setCurrentIndex(int currentIndex) {
    	this.currentIndex = currentIndex;
    }
}
