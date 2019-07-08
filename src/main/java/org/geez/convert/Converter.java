package org.geez.convert;

import java.io.File;
import java.util.concurrent.Callable;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;

public class Converter  implements Callable<Void> {
	protected char huletNeteb = 0x0;
	protected boolean setProgress = true;

    protected final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper();

    protected File inputFile = null, outputFile = null;
    
    public Converter( final File inputFile, final File outputFile ) {
    	this.inputFile  = inputFile;
    	this.outputFile = outputFile;
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

}
