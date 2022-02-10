package com.repo.operations;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import com.repo.utils.RollingLog;

public class ExtractMetadataFromPDF extends Object
{
	//dctm value object_name, title
	public static String attrTitle = null;
	//dctm value author
	public static String attrAuthor = null;
	//dctm value subject
	public static String attrSubject = null;
	//dctm value keywords[], accepted as csv
	public static String attrKeywords = null;
	// parent folder to be used in DCTM
	public static String attrFolder = null;
	
	public final static int LOG_ALL = 4;
	// debugging messages
	public final static int LOG_DEBUG = 0;
	//informational messages
	public final static int LOG_INFO = 1;
	//non-fatal warning messages
	public final static int LOG_WARN = 2;
	// fatal errors
	public final static int LOG_ERROR = 3;
	
	private static final String LOGGER_NAME = "tracing";
	private static final String CODE_NAME = "ExtractMetadataFromPDF: ";
	private RollingLog mainLog;
	public static ExtractMetadataFromPDF _instance;
	
	private static Calendar CALENDAR = Calendar.getInstance();
	public static String dt_today = null;
	public static String dt_prevday = null;
	
	public static String logFilePath = null;
	private static String logFile = null;
	private static int logLevel = 0;
	private static int logSize = 0x1e8480;
	public static String filePath = null;
	

	public static void main(String[] args) 
	{
		{
			System.out.println("start");
			_instance = new ExtractMetadataFromPDF();
			_instance.go(args);
		}
		
	}	
	
	public static long addDays(long time, int amount)
	{
		Calendar calendar = CALENDAR;
		synchronized(calendar)
		{
			calendar.setTimeInMillis(time);
			calendar.add(Calendar.DAY_OF_MONTH, amount);
			return calendar.getTimeInMillis();
		}
	}
		
	private void go(String[] args)
	{
  //this was originally written in Java 8 and this function was utilized heavily across the codebase
  //better functions exist in newer versions of Java. This is left as an example.
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		dt_today = sdf.format(new Date(addDays(cal.getTimeInMillis(), 0)));
		
		System.out.println("Today's Date:::"+dt_today);
				
		System.out.println("start in method!");		
		
    //custom log used to have ready to go file for use in auditing files and what was done
		logFilePath = "./logs";
		logFile =  (new StringBuilder(String.valueOf(logFilePath))).append("/ExtractMetadataFromPDF.").append(dt_today).append(".log").toString();
		logLevel = 3;
		
		

		extractMetadata(args);
	}
	
	public void extractMetadata(String[] args)
	{
		
		try 
		{
			openLog();
				
			String dir = "./files";
						
			System.out.println("attempting stream line reader");
			mainLog.write(LOG_ERROR,  (new StringBuilder("attempting stream line reader").toString()));
		
			try 
			{
				System.out.println("Start listing files");
				mainLog.write(LOG_ERROR,  (new StringBuilder("Start listing files").toString()));
					
				// set the directory that needs to have files listed
				File source = new File(dir);
				//run through and list all files in the directory
				@SuppressWarnings("unchecked")
				List<File> files = (List<File>) FileUtils.listFiles(source, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
				
				System.out.println("Finish listing files!");
				mainLog.write(LOG_ERROR,  (new StringBuilder("Finished listing files!").toString()));

				FileWriter writer = new FileWriter("metadata.csv");
				String newLine=System.getProperty("line.separator");
				
			
				for (File file : files)
				{
					if (file.getName().toLowerCase().endsWith(".pdf"))
						{
						System.out.println("inside extraction of metadata");
						
						System.out.println("filepath is: " + file.getPath());
						mainLog.write(LOG_ERROR,  (new StringBuilder("filepath is ").append(file).toString()));
						//extract metadata from pdf
						PDDocument pd = PDDocument.load(file);
						System.out.println("processing document: " + file.getName());
						PDDocumentInformation pdi = pd.getDocumentInformation();
						
						Path src = Paths.get(file.getAbsolutePath()).normalize();
						Path dest = null;
						
						String parent = file.getParentFile().getName();
						
						//list of known subfolder names to use as comparison for sorting
							ArrayList<String> list = new ArrayList<String>();
							list.add("1985");
							list.add("1986a");
							list.add("1986b");
							list.add("1986c");
							list.add("1987A");
							list.add("1987B");
							list.add("1988");
							list.add("1988B");
							list.add("1989");
							list.add("1989B");
							list.add("1990");
							list.add("1991");
							list.add("1991B");
							list.add("1992A");
							list.add("1992B");
							list.add("1992C");
							list.add("1993A");
							list.add("1993B");
							list.add("1993C");
							list.add("1993D");
							list.add("1994A");
							list.add("1994B");
							list.add("1994C");
							list.add("1995A");
							list.add("1995B");
							list.add("1995C");
							list.add("1996A");
							list.add("1996B");
							list.add("1997");
							list.add("1998");
							list.add("1999");
							list.add("2000");
							list.add("2000thru2001");
							list.add("2001");
							list.add("2002");
							list.add("2003toPresent");
							list.add("PriorTo1985");
							list.add("ABCDEF");
							list.add("fooBar");
							list.add("fNord");
							list.add("Excuses");
								
						if (list.contains(parent))
						{
							if (parent == "ABCDEF")
							{
								dest = Paths.get("exceptions\\Working Directory\\ExampleSort\\" + parent.toString() + "\\" + file.getName());
							}
							else if (parent == "XYZ123")
							{
								dest = Paths.get("exceptions\\Working Directory\\Random Folder\\" + parent.toString() + "\\" + file.getName());
							}
							else
							{
								dest = Paths.get("exceptions\\Working Directory\\Folder\\" + parent.toString() + "\\" + file.getName());
							}
							
						}
						else if (!list.contains(parent))
						{
							dest = Paths.get("exceptions\\Working Directory\\" + parent.toString() + "\\" + file.getName());
						}
											
						
						System.out.println("gathering and writing metadata");
						if (pdi.getTitle() != null && !pdi.getTitle().trim().isEmpty())
						{
						attrTitle = (pdi.getTitle()).toString();
	//					System.out.println("Title of PDF is: " + attrTitle);;
//						mainLog.write(LOG_ERROR, (new StringBuilder("Title is: ").append(attrTitle).toString()));
						writer.append("\"" + attrTitle.toString() + "\",");

							if (pdi.getSubject() != null && !pdi.getSubject().trim().isEmpty())
							{
							attrSubject = (pdi.getSubject()).toString();
							writer.append("\"" + attrSubject.toString() + "\",");
		//					System.out.println("Subject of PDF is: " + attrSubject);;
//							mainLog.write(LOG_ERROR, (new StringBuilder("Subject is: ").append(attrSubject).toString()));
							}
							else
							{
								attrSubject = "";
								writer.append("\" \",");
							}
							if (pdi.getAuthor() != null && !pdi.getAuthor().trim().isEmpty())
							{
							attrAuthor = (pdi.getAuthor()).toString();
							writer.append("\"" + attrAuthor.toString() + "\",");
		//					System.out.println("Author of PDF is: " + attrAuthor);;
//							mainLog.write(LOG_ERROR, (new StringBuilder("Author is:").append(attrAuthor).toString()));
							}
							else
							{
								attrAuthor = "";
								writer.append("\" \",");
							}
							if (pdi.getKeywords() != null && !pdi.getKeywords().trim().isEmpty())
							{
							attrKeywords = (pdi.getKeywords()).toString();
							writer.append("\"" + attrKeywords.toString() + "\",");
		//					System.out.println("Keywords of PDF are: " + attrKeywords);;
//							mainLog.write(LOG_ERROR, (new StringBuilder("Keywords are: ").append(attrKeywords).toString()));
							}
							else
							{
								attrKeywords = "\" \",";
								writer.append("\" \",");
							}
							if (!parent.toString().isEmpty())
							{
								attrFolder = parent.toString();
								writer.append("\"" + attrFolder.toString() + "\",");
			//					System.out.println("Keywords of PDF are: " + attrKeywords);;
	//							mainLog.write(LOG_ERROR, (new StringBuilder("Keywords are: ").append(attrKeywords).toString()));
							}
							else
							{
								attrFolder = "\" \",";
								writer.append("\" \",");
							}
							}
						else 
						{
							mainLog.write(LOG_ERROR,  (new StringBuilder("FILENAME MISSING ").append(file).toString()));
							mainLog.write(LOG_ERROR,  (new StringBuilder("SOURCE DIRECTORY AND FILE ").append(src).toString()));
							mainLog.write(LOG_ERROR,  (new StringBuilder("DESTINATION DIRECTORY ").append(dest).toString()));
							System.out.println("Exception file: " + file.toString());
							//close out of the file
							pd.close();
							//create directories
							File destFile = new File(dest.toString());
							destFile.getParentFile().mkdirs();
							//move the file to the new location
							Files.move(src, dest);
							//move onto next file
							continue;
						}
						
						//write the original filename to the csv
						writer.append("\"" + file.getName() + "\"");
						System.out.println("filename is: " + file.getName());
						writer.append(newLine);
						
						System.out.println("File processed successfully: " + file.toString());
//						System.out.println("attrTitle is " + attrTitle.toString() + " attrSubject is " + attrSubject.toString() + " attrAuthor is " + attrAuthor.toString() + " attrKeywords is " + attrKeywords.toString());
						mainLog.write(LOG_ERROR,  (new StringBuilder("Title, Subject, Author, Keywords: ").append(attrTitle).append("-").append(attrSubject).append("-").append(attrAuthor).append("-").append(attrKeywords).toString()));
						
						pd.close();
													
					}
				}
				writer.flush();
				writer.close();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}
	
	private void openLog() throws IOException
	{
		try
		{
			mainLog = RollingLog.getInstance();
			mainLog.setStamp(true);
			mainLog.setFlush(true);
			mainLog.setLogLevel(logLevel);
			mainLog.startLog(logFile, logSize);
			mainLog.write(LOG_ALL,  "openLog(): The ExtractMetadataFromPDF utility started.");
		}
		catch (Exception e)
		{
			throw new IOException((new StringBuilder("openLog():")).append(e.toString()).toString());
		}
	}

public static String grabJustName (String inValue)
{
	StringTokenizer st = new StringTokenizer(inValue, ".");
	String token = null;
	if (st.hasMoreTokens()) 
	{
		token = (String)st.nextToken();
	}
	else
	{
		token = "";
	}
	return(token);
}

public static String[] splitFolderPath(String folderPath, String rootName) 
{
	
	int  indexRoot = folderPath.indexOf(rootName);
	if (-1 == indexRoot) return null;
	int index = folderPath.indexOf('/', indexRoot);
	if (-1 == index) return new String[0];
	return folderPath.substring(1 + index).split("/");
}

}

