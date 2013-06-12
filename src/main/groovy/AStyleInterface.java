import java.io.File;

import com.github.abrarsyed.gmcp.Constants;
import com.github.abrarsyed.gmcp.Main;

/**
* The AStyleInterface class contains the variables and methods to call the
* Artistic Style shared library to format a source file.
*/

public class AStyleInterface
{
    /**
    * Static constructor to load the native Artistic Style library.
    * Does not need to terminate if the shared library fails to load.
    * But the exception must be handled when a function is called.
    */
    static
    { // load shared library from the classpath
        File os = new File(Constants.DIR_NATIVES, "2.02.1\\"+Main.os.name());
        File arch = new File(os, System.getProperty("os.arch").replace("\\W", ""));
        File file = new File(arch, System.mapLibraryName("AStylej"));
        System.load(file.getAbsolutePath());
    }
    
    // bouncer
    public String getVersion()
    {
    	return AStyleGetVersion();
    }
    
    // bouncer
    public String formatSources(String in, String options)
    {
    	return AStyleMain(in, options);
    }

    /**
    * Calls the GetVersion function in Artistic Style.
    *
    * @return    A String containing the version number of Artistic Style.
    */
    public native String AStyleGetVersion();

    /**
    * Calls the AStyleMain function in Artistic Style.
    *
    * @param    textIn  A string containing the source code to be formatted.
    * @param   options   A string of options to Artistic Style.
    * @return  A String containing the formatted source from Artistic Style.
    */
    private native String AStyleMain(String textIn, String options);
    
    /**
    * Error handler for messages from Artistic Style.
    * This method is called only if there are errors when AStyleMain is called.
    * This is for debugging and there should be no errors when the calling
    * parameters are correct.
    * Signature: (ILjava/lang/String;)V
    *
    * @param  errorNumber   The error number from Artistic Style.
    * @param  errorMessage  The error message from Artistic Style.
    */
    private void ErrorHandler(int errorNumber, String errorMessage)
    {
    	System.err.println("astyle error " + String.valueOf(errorNumber) + " - "  + errorMessage);
    }

}
