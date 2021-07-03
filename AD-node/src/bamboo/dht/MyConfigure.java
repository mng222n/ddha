/**
 * 
 */
package bamboo.dht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;



/**
 * @author skminh
 *
 */
public class MyConfigure {

   /** String 'NONE' used as undefined value (i.e. null). */
   public static String NONE="NONE";

   /** The object that should be configured */
   MyConfigurable myConfigurable;

   
   /** Parses a single text line (read from the config file) */
   protected void parseLine(String line)
   {  // parse the text line..
   }

   /** Converts the entire object into lines (to be saved into the config file) */
   protected String toLines()
   {  // convert the object into to one or more text line..
      return "";
   }

   /** Costructs a Configure container */
   protected MyConfigure()
   {  this.myConfigurable=null;
   }

   /** Costructs a Configure container */
   public MyConfigure(MyConfigurable myConfigurable, String file)
   {  this.myConfigurable=myConfigurable;
      loadFile(file);
   }

       
   /** Loads Configure attributes from the specified <i>file</i> */
   protected void loadFile(String file)
   {  //System.out.println("DEBUG: loading Configuration");
      if (file==null)
      {  //System.out.println("DEBUG: no Configuration file");
         return;
      }
      //else
      BufferedReader in=null;
      try
      {  in=new BufferedReader(new FileReader(file));
                
         while (true)
         {  String line=null;
            try { line=in.readLine(); } catch (Exception e) { e.printStackTrace(); System.exit(0); }
            if (line==null) break;
         
            if (!line.startsWith("#"))
            {  if (myConfigurable==null) parseLine(line); else myConfigurable.parseLine(line);
            }
         } 
         in.close();
      }
      catch (Exception e)
      {  System.err.println("WARNING: error reading file \""+file+"\"");
         //System.exit(0);
         return;
      }
      //System.out.println("DEBUG: loading Configuration: done.");
   }


   /** Saves Configure attributes on the specified <i>file</i> */
   protected void saveFile(String file)
   {  if (file==null) return;
      //else
      try
      {  BufferedWriter out=new BufferedWriter(new FileWriter(file));
         out.write(toLines());
         out.close();
      }
      catch (IOException e)
      {  System.err.println("ERROR writing on file \""+file+"\"");
      }         
   }
   
}
