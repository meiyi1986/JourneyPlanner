package au.com.jakebarnes.JourneyPlanner.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class FileHandler {
	
	public static ArrayList<File> listFilesForFolder(final File folder,
	        final boolean recursivity,
	        final String patternFileFilter) {

	    // Inputs
	    boolean filteredFile = false;

	    // Ouput
	    final ArrayList<File> output = new ArrayList<File> ();

	    // Foreach elements
	    for (final File fileEntry : folder.listFiles()) {

	        // If this element is a directory, do it recursivly
	        if (fileEntry.isDirectory()) {
	            if (recursivity) {
	                output.addAll(listFilesForFolder(fileEntry, recursivity, patternFileFilter));
	            }
	        }
	        else {
	            // If there is no pattern, the file is correct
	            if (patternFileFilter.length() == 0) {
	                filteredFile = true;
	            }
	            // Otherwise we need to filter by pattern
	            else {
	                filteredFile = Pattern.matches(patternFileFilter, fileEntry.getName());
	            }

	            // If the file has a name which match with the pattern, then add it to the list
	            if (filteredFile) {
	                output.add(fileEntry);
	            }
	        }
	    }

	    return output;
	}
}
