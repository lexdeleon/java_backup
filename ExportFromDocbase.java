package com.repo.operations;

import com.repo.utilities.RollingLog;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.documentum.fc.client.DfClient;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfSysObject;
//import com.documentum.fc.client.DfQuery;
//import com.documentum.fc.client.IDfCollection;
//import com.documentum.fc.client.IDfQuery;

import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfId;
//import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfLoginInfo;

/*
 * This is a generic export program. Variables determining which object type,
 * the DQL query, and other variables are all stored in a separate 
 * plain text properties file to allow for on the fly customization.
 */

public class ExportFromDocbase 
{
	
	private static final int ERROR_SHUTDOWN = -1;
	private static final int NORMAL_SHUTDOWN = 0;
	private static final int MAX_LOG_SIZE = 2000000;
	
	final String DEFAULT_FILE_FORMAT = "crtext";
	
	public static String username = null;
	public static String password = null;
	public static String docbase = null;
	public static String localFolderPath = null;
	public static String logFilePath = null;
	public static String objType = null;
	public static boolean sendEmail = false;
	public static String emailAddresses = null;
	public static String fileExtension = null;
	public static int numObjsProcessed = 0;
	private static String logFile = null;
	private static String tempFile = null;
	private static IDfSession dfsession;
	private static int logLevel = RollingLog.LOG_DEBUG;
	private static int logSize = MAX_LOG_SIZE;
	private static IDfSessionManager manager = null;
	private RollingLog mainLog;
	private static ExportFromDocbase _instance;

	public static String objIDs = null;
	private static Calendar CALENDAR = Calendar.getInstance();
	
	public static void main(String[] args) 
	{
		if (args.length<1)
			System.out.println("Please enter the configuration file location");
		_instance = new ExportFromDocbase();
		_instance.go(args);
	}
	
	private void go(String[] args) 
	{
		
		// This is the default date format for logging used here
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy'-'MM'-'dd'_'HH.mm.ss");
		
		try 
		{
			// grab the attributes from the properties file function call 
			getInitParameters(args);
			// check the attributes function call
			validateParameters();
			// create log file 
			Date todaysDate = new Date(System.currentTimeMillis());
			logFile = logFilePath + "/export." + formatter.format(todaysDate) + ".log";
			openLog();
			
			// Establish Documentum session
			dfsession = manager.getSession(docbase);
			
			mainLog.write(RollingLog.LOG_ALL, "main(); Documentum session opened successfully - " + 
					formatter.format(new Date(System.currentTimeMillis())));
			
			// begin saving files
			processObjects();
			
			// when finished, write to log file 
			mainLog.write(RollingLog.LOG_ALL,  "main(); Successfully processed " + numObjsProcessed + " objects - " +
					formatter.format(new Date(System.currentTimeMillis())));
			
			// close program cleanly
			closeApplication(NORMAL_SHUTDOWN, "The processing was successful. [" + Integer.toString(numObjsProcessed) 
					+ "] files exported. Please review the log for details.");			
		} 
		catch (Exception e) 
		{
			
			// capture any errors that are thrown
			e.printStackTrace();
			if (mainLog != null) 
			{
				mainLog.write(RollingLog.LOG_ALL,  "main(); The processing failed with the following error at " +
						formatter.format(new Date(System.currentTimeMillis())));
				mainLog.write(RollingLog.LOG_ALL, "Exception: " + e.toString());
			}
			// shutdown cleanly and show error
			closeApplication(ERROR_SHUTDOWN, "The processing failed: " + e.getMessage());			
		}
	}
	
	private void getInitParameters(String[] args) throws Exception 
	{	
		// read properties file and write attributes to variables
		Properties p = new Properties();
		FileInputStream fis = new FileInputStream(args[0]);
		p.load(fis);
		username = p.getProperty("Username");
		password = p.getProperty("Password");
		docbase = p.getProperty("Docbase");
		localFolderPath = p.getProperty("LocalFolderPath");
		logFilePath = p.getProperty("LogFilePath");
		sendEmail = Boolean.valueOf(p.getProperty("SendEmail")).booleanValue();
		emailAddresses = p.getProperty("EmailAddresses");
		objType = p.getProperty("ObjType");
		objIDs = p.getProperty("ObjIDs");
	}
	
	private void validateParameters() throws Exception 
	{
		
		// make sure that required information for logging into Documentum exists
		IDfLoginInfo li = new DfLoginInfo();
		li.setUser(username);
		li.setPassword(password);
		IDfClient client = DfClient.getLocalClient();
		manager = client.newSessionManager();
		manager.setIdentity(docbase, li);
		
		try 
		{
			manager.authenticate(docbase);
		} 
		catch (DfException de) 
		{
			throw new Exception("Bad docbase credentials: " + de.toString());
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
			mainLog.write(RollingLog.LOG_ALL, "OpenLog(): Object Processing has begun.");
			
		} 
		catch (Exception e) 
		{
			throw new IOException("OpenLog(): " + e.toString());
		}
	}
	
	public static long addDays(long time, int amount) 
	{
		
		Calendar calendar = CALENDAR;
		synchronized(calendar) 
		{
			calendar.setTimeInMillis(time);
			calendar.add(Calendar.DAY_OF_MONTH,  amount);
			return calendar.getTimeInMillis();
		}
	}
	
	private void processObjects() throws Exception 
	{
				
		tempFile = logFilePath + "/" + objType + "exceptionsTOBEINVESTIGATED.log";
		
		IDfSysObject objectAtHand = null;
			
		String lineFromFile = null;
		
		//begin reading the list of object IDs from the plain text file listing out the files to export
		//and placing the list into working memory
		//suitable for working with smaller datasets below a million documents
		BufferedReader ids = new BufferedReader(new FileReader (objIDs));
		
		List<String> idList = new ArrayList<String>();
		
		FileWriter writer = new FileWriter(tempFile);
		String newLine = System.getProperty("line.separator");
		
		while ((lineFromFile = ids.readLine()) != null) 
		{
			idList.add(lineFromFile);
		}
		ids.close();
		System.out.println("Object IDs added to List");
		
		//iterate through list of object IDs
		for (String id : idList) 
		{
			try 
			{
			IDfId idObj = new DfId(id);
			objectAtHand = (IDfSysObject) dfsession.getObject(idObj);
			// these lines grab the stored file extension and append it so that the file is saved properly
			// this allows for proper import file format setting
			IDfFormat extObj = objectAtHand.getFormat();
			String fileExtension = extObj.getDOSExtension();
			// the file name of the extracted file is set to the object ID and the stored DOS extension
			// all reports include the object ID as a primary key to serve as cross object reference
			// utilizing it as the file name allows for matching against lines of metadata from metadata reports
			objectAtHand.getFile(localFolderPath + "\\" + objectAtHand.getObjectId() + "." + fileExtension);
			objectAtHand = null;
			numObjsProcessed++;
			System.out.println("Processing object: " + numObjsProcessed + " Object ID: " + idObj);
			mainLog.write(RollingLog.LOG_ALL, "Processing object: " + numObjsProcessed + " Object: " + objectAtHand + 
					" Object ID: " + idObj);
			}
			catch (DfException ex)
			{
				//in the event of any error, log the error, write the object ID to a 
				// TO BE INVESTIGATED File, and continue to next object ID
				System.out.println("Exception caught in ProcessObj: " + ex.getMessage());
				mainLog.write(RollingLog.LOG_ALL, ex.getMessage());
				writer.append(id);
				writer.append(newLine);
				
			}
		}
		writer.flush();
		writer.close();
		return;
	}
	
	private void closeApplication(int Severity, String Message) 
	{
		
		String outputMsg = null;
		String outputSubj = null;
		
		switch (Severity) 
		{
		case ERROR_SHUTDOWN:
			outputMsg = "The program has exited with the following error: " + Message;
			outputSubj = "Object Processing Aborted";
			break;
		case NORMAL_SHUTDOWN:
			outputMsg = "Number of files exported: [" + Integer.toString(numObjsProcessed) + "] \n" + Message;
			outputSubj = "Export Completed\n";
			break;
		default:
			outputMsg = "Unknown System Shutdown: " + Message;
			outputSubj = "Object Processing Ended Prematurely";
			break;
		}
		
		System.out.println(outputSubj + " : " + outputMsg);
		
		if (dfsession != null)
			manager.release(dfsession);
		
		if (sendEmail) 
		{
			Mailer(outputSubj, outputMsg);
		}
		System.exit(Severity);
	}
	
	private void Mailer(String emailSub, String emailMsg) 
	{
		try
		{
			String host = "SMTP.TEST.COM";
			String from = "do_not_reply@test_email.com";
			Properties props = System.getProperties();
			props.put("mail.smtp.host", host);
			Session session = Session.getDefaultInstance(props, null);
			String address[] = emailAddresses.split(",");
			
			InternetAddress addr[] = new InternetAddress[address.length];
			for(int i = 0; i < address.length; i++)
				addr[i] = new InternetAddress(address[i]);
			
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.addRecipients(javax.mail.Message.RecipientType.TO, addr);
			message.setSubject(emailSub);
			Multipart mp = new MimeMultipart();
			MimeBodyPart mbp1 = new MimeBodyPart();
			mbp1.setText(emailMsg);

			mp.addBodyPart(mbp1);

			message.setContent(mp);
			Transport.send(message);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
}
