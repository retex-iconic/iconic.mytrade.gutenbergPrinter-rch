package iconic.mytrade.gutenbergPrinter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import iconic.mytrade.gutenberg.jpos.linedisplay.service.MessageBox;
import iconic.mytrade.gutenberg.jpos.linedisplay.service.OperatorDisplay;
import iconic.mytrade.gutenberg.jpos.printer.service.Beeping;
import iconic.mytrade.gutenberg.jpos.printer.service.CarteFidelity;
import iconic.mytrade.gutenberg.jpos.printer.service.FiscalPrinterDataInformation;
import iconic.mytrade.gutenberg.jpos.printer.service.PrinterInfo;
import iconic.mytrade.gutenberg.jpos.printer.service.R3define;
import iconic.mytrade.gutenberg.jpos.printer.service.SmartTicket;
import iconic.mytrade.gutenberg.jpos.printer.service.TakeYourTime;
import iconic.mytrade.gutenberg.jpos.printer.service.TicketErrorSupport;
import iconic.mytrade.gutenberg.jpos.printer.service.Asynchronous.AsynchronousEvent;
import iconic.mytrade.gutenberg.jpos.printer.service.Asynchronous.GutenbergAction;
import iconic.mytrade.gutenberg.jpos.printer.service.Asynchronous.GutenbergActions;
import iconic.mytrade.gutenberg.jpos.printer.service.properties.Lotteria;
import iconic.mytrade.gutenberg.jpos.printer.service.properties.PrinterType;
import iconic.mytrade.gutenberg.jpos.printer.service.properties.SRTPrinterExtension;
import iconic.mytrade.gutenberg.jpos.printer.utils.Sprint;
import iconic.mytrade.gutenberg.jpos.printer.utils.String13Fix;
import iconic.mytrade.gutenbergPrinter.ej.FiscalEJFile;
import iconic.mytrade.gutenbergPrinter.rtchecks.RTchecks;
import iconic.mytrade.gutenbergPrinter.tax.DicoTaxLoad;
import iconic.mytrade.gutenbergPrinter.tax.DicoTaxObject;
import jpos.FiscalPrinter;
import jpos.FiscalPrinterConst;
import jpos.JposException;
import jpos.events.DirectIOEvent;
import jpos.events.ErrorListener;
import jpos.events.OutputCompleteListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import rtsTrxBuilder.hardTotals.HardTotals;

public class FiscalPrinterDriver implements jpos.FiscalPrinterControl17, StatusUpdateListener {

	private static jpos.FiscalPrinterControl17 fiscalPrinter;
	
	static boolean epsonClass4 = false;			// mi pare che sia così su tutti i pdv
	
    static String myRchFiscalNumber = "";
	
	private static String Rt2Fw_Rch="8";
			
    private static int RT_KO = -1;
    private static int RT_NOTINSERVICE = -2;
    private static int RT_OLDFILESTOSEND = -3;
    private static int RT_OK = 0;
    private static int RT_NWP = 1;
    private static int RT_PRESERVICE = 2;
    private static String PRINTERRTOFF = "  STAMPANTE RT OFF  ";
    
	private static int FPRT_RT_POSTVOID_NUMBER = 28; 
	private static int FPRT_RT_REFUND_NUMBER = 29;
	
    private static double RT2fw = 999;
    private static double Lotteryfw = 999;
    private static double SMTKfw = 999;
    private static double ILotteryfw = 999;
    private static boolean fwRT2enabled = false;	// abilita/disabilita i comandi in modalità RT2
    private static boolean fwLotteryenabled = false;
    private static boolean fwSMTKenabled = false;	// abilita/disabilita i comandi per SmartTicket
    private static boolean fwILotteryenabled = false;
    
	private boolean noReactionStatus = false;
	
	public boolean getNoReactionStatus() {
		return noReactionStatus;
	}
			
	public void setNoReactionStatus(boolean value) 
	{
		noReactionStatus = value;
	}
			
	private static double getRT2fw() {
		System.out.println("RT2 - getRT2fw : "+RT2fw);
		return RT2fw;
	}
	
	private void setRT2fw(String rttype) {
		RT2fw = Double.parseDouble(Rt2Fw_Rch);
		System.out.println("RT2 - setRT2fw : "+RT2fw);
	}
	
	private static double getLotteryfw() {
		System.out.println("RT2 - getLotteryfw : "+Lotteryfw);
		return Lotteryfw;
	}
	
	private void setLotteryfw(String rttype) {
		Lotteryfw = 7.0;
		System.out.println("RT2 - setLotteryfw : "+Lotteryfw);
	}
	
	private static double getSMTKfw() {
		System.out.println("SMTK - getSMTKfw : "+SMTKfw);
		return SMTKfw;
	}
	
	private void setSMTKfw(String rttype) {
		SMTKfw = 9;
		System.out.println("SMTK - setSMTKfw : "+SMTKfw);
	}
	
	private static double getILotteryfw() {
		System.out.println("RT2 - getILotteryfw : "+ILotteryfw);
		return ILotteryfw;
	}
	
	private void setILotteryfw(String rttype) {
		ILotteryfw = 9.00;
		System.out.println("RT2 - setILotteryfw : "+ILotteryfw);
	}
	
	public static boolean isfwRT2enabled() {
		return fwRT2enabled;
	}
	
	public static boolean isfwRT2disabled() {
		return (!isfwRT2enabled());
	}
	
	protected static void setfwRT2enabled(boolean fwrT2enabled) {
		System.out.println("RT2 - setfwRT2enabled : "+fwrT2enabled);
		fwRT2enabled = fwrT2enabled;
		DicoTaxLoad.setRT2enabled(fwRT2enabled);
	}

	private static boolean isfwLotteryenabled() {
		return fwLotteryenabled;
	}
	
	private static boolean isfwLotterydisabled() {
		return (!isfwLotteryenabled());
	}
	
	protected static void setfwLotteryenabled(boolean fwlotteryenabled) {
		System.out.println("RT2 - setfwLotteryenabled : "+fwlotteryenabled);
		fwLotteryenabled = fwlotteryenabled;
	}
	
	public static boolean isfwSMTKenabled() {
		return fwSMTKenabled;
	}
	
	public static boolean isfwSMTKdisabled() {
		return (!isfwSMTKenabled());
	}
	
	private static void setfwSMTKenabled(boolean fwsMTKenabled) {
		System.out.println("SMTK - setfwSMTKenabled : "+fwsMTKenabled);
		fwSMTKenabled = fwsMTKenabled;
	}
	
	public static boolean isfwILotteryenabled() {
		return fwILotteryenabled;
	}
	
	private static boolean isfwILotterydisabled() {
		return (!isfwILotteryenabled());
	}
	
	protected static void setfwILotteryenabled(boolean fwilotteryenabled) {
		System.out.println("RT2 - setfwILotteryenabled : "+fwilotteryenabled);
		fwILotteryenabled = fwilotteryenabled;
	}
	
	private String fwBuildNumber = "";
    
	private static boolean RegistratoreTelematico = false;
	private boolean ServerRegistratoreTelematico = false;
	
	protected void setRTModel(boolean f)
	{
		RegistratoreTelematico = f;
	}
	
	private void setRTModel(Boolean f)
	{
		RegistratoreTelematico = f.booleanValue();
	}
	
	protected static boolean isRTModel()
	{
		return ( RegistratoreTelematico );
	}
	
	private boolean isNotRTModel()
	{
		return ( ! this.isRTModel() );
	}
	   
	protected void setSRTModel(boolean f)
	{
		ServerRegistratoreTelematico = f;
	}
	
	private void setSRTModel(Boolean f)
	{
		ServerRegistratoreTelematico = f.booleanValue();
	}
	
	protected boolean isSRTModel()
	{
		return ( ServerRegistratoreTelematico );
	}
	
	private boolean isNotSRTModel()
	{
		return ( ! this.isSRTModel() );
	}
	   
    private boolean ExpiredCertificate = false;
    
	private boolean isExpiredCertificate() {
		return ExpiredCertificate;
	}
	
	private void setExpiredCertificate(boolean expiredCertificate) {
		ExpiredCertificate = expiredCertificate;
	}
	
	int getOpenTimeout() {
		return TakeYourTime.getOpenTimeout();
	}
	
	double doLoad(int PrinterModel, String PrinterName) {
	    double fw = 0;
	    
		setRTModel(SRTPrinterExtension.isPRT());
		setSRTModel(SRTPrinterExtension.isSRT());
		
//		if (isSRTModel() || isRTModel())
//		{
			HardTotals.init();
//		}
		
		PrinterInfo.SavePrinterInfo("Model", ""+PrinterModel);
		
		System.out.println ( "OPEN CON PRINTER <"+PrinterModel+">");
		
		try
		{
			System.out.println ( "Prima di Open ["+PrinterType.getPrinterJavaPosModel()+"]" );
			if ( PrinterType.getPrinterJavaPosModel() == PrinterType.JAVAPOS113 )
				fiscalPrinter = (jpos.FiscalPrinterControl113)new FiscalPrinter();
			else
				fiscalPrinter = (jpos.FiscalPrinterControl17)new FiscalPrinter();
			fiscalPrinter.addStatusUpdateListener(this);
			
			fiscalPrinter.open(PrinterName);
			
			System.out.println ( "Prima di Claim" );
			fiscalPrinter.claim(1000);
		}
		catch ( jpos.JposException e )
		{
			System.out.println ( "Printer Exception <"+e.toString()+">");
			return fw;
		}
		OperatorDisplay.pleaseDisplay (R3define.PRINTERVERIFY);
		System.out.println ("FP-1");
	    while ( true ) 
	    {
	    	System.out.println ("FP-2");
	    	setNoReactionStatus ( pleaseCheck() );
	    	System.out.println ("FP-3");
	    	if ( getNoReactionStatus () == false ) 
	    	{
	    		System.out.println ("FP-4");
				System.out.println("Printer "+getPhysicalDeviceName()+"("+getPhysicalDeviceDescription()+") correctly open.");
	    		break;
	    	}
	    	System.out.println ("FP-5");
	    	pleaseBeep(100,10);
	    	System.out.println ("FP-6");
	    }
		System.out.println ("FP-7");
		OperatorDisplay.pleaseDisplay (R3define.PRINTERREADY);
		
	    SharedPrinterFields.fiscalEJ = new FiscalEJFile ();
	    SharedPrinterFields.fiscalEJ.open("uk.co.datafit.wincor.system.device.FiscalEJFile", null);
	    SharedPrinterFields.fiscalEJ.setDeviceEnabled(true);
	    
		if (RTchecks.checkSRT(fiscalPrinter)) {
			// isRT è cambiato
			
			setRTModel(SRTPrinterExtension.isPRT());
			if (isRTModel()){
				setSRTModel(false);
				SRTPrinterExtension.setLikeNonFiscalMode(false);
			}
			System.out.println ("RetailCube-R3printers SRT Model                <"+isSRTModel()+">");
			System.out.println ("RetailCube-R3printers RT Model                 <"+isRTModel()+">");
			System.out.println ("RetailCube-R3printers Like Non Fiscal Mode     <"+SRTPrinterExtension.isLikeNonFiscalMode()+">");
			
//			if (isSRTModel() || isRTModel())
//			{
				HardTotals.init();
//			}
		}
		
	    if (isRTModel())
	    {
	    	int rtstatus = RT_KO;
	    	
	    	while (rtstatus < RT_OK)
	    	{
	    		try {
					rtstatus = checkRTStatus();
				} catch (JposException e1) {
					System.out.println ( "Printer Exception <"+e1.toString()+">");
				}
	    		if (rtstatus  < RT_OK){
//	    			if (isEpsonModel() && (rtstatus == RT_OLDFILESTOSEND))	// avanti lo stesso
//	    				break;
	    			OperatorDisplay.pleaseDisplay ( PRINTERRTOFF );
	    		}
	    		else
	    			break;
	    		
	    		try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
	    	}
	    	
	    	if (rtstatus == RT_PRESERVICE)
	    		setRTModel(false);
	    	
	    	if (rtstatus == RT_NWP && isExpiredCertificate() == false) {
	    		System.out.println("checkRTStatus - ATTENZIONE --------------------------------------");
	    		System.out.println("checkRTStatus - FORZATO REPORT Z");
	    		System.out.println("checkRTStatus - ATTENZIONE --------------------------------------");
	    		try {
					fiscalPrinter.printZReport();
				} catch (JposException e) {
					System.out.println ( "Printer Exception <"+e.toString()+">");
				}
	    	}
	    	
			PrinterInfo.SavePrinterInfo("RT FW Build Number", fwBuildNumber);
	    }
	    
	    if (isRTModel())
	    {
	    	while (SharedPrinterFields.RTPrinterId == null)
	    	{
	    		try {
					SharedPrinterFields.RTPrinterId = getPrinterId();
				} catch (JposException e1) {
					SharedPrinterFields.RTPrinterId = null;
					System.out.println ( "Printer Exception <"+e1.toString()+">");
				}
	    		if (SharedPrinterFields.RTPrinterId == null){
	    			OperatorDisplay.pleaseDisplay ( PRINTERRTOFF );
	    		}
	    		else {
	    			PrinterInfo.SavePrinterInfo("PrinterId", SharedPrinterFields.RTPrinterId);
	    			PrinterInfo.SavePrinterInfo("Printer Description", getPrinterDesc(SharedPrinterFields.RTPrinterId));
	    			break;
	    		}
	    		
	    		try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
	    	}
	    }
	    
		int[] ai = new int[1];
		String[] as = new String[1];
		getData(jpos.FiscalPrinterConst.FPTR_GD_FIRMWARE, ai, as);
        System.out.println ("RetailCube-R3printers printer FW : "+as[0]);

        PrinterInfo.SavePrinterInfo("FW", as[0]);
        
        String newS = String13Fix.cleandoublestr(as[0]);
		as[0] = newS;
		
    	fw = Double.parseDouble((as[0].substring(0, as[0].lastIndexOf((int)'.'))).trim());
		System.out.println("RT2 - fw : "+fw);
    	setfwLotteryenabled(fw >= getLotteryfw());
    	setfwRT2enabled(fw >= getRT2fw());
    	setfwSMTKenabled(fw >= getSMTKfw());
    	setfwILotteryenabled(fw >= getILotteryfw());
	    
	    if (isRTModel())
	    {
    		setLocalAccessControl();
    		setFidelityOption(1);
    		setAppendixOption(1,0);
    		SharedPrinterFields.VAT_N4_Dept = "40";
    		DicoTaxObject.setBASE_SERVICES_DEPT(Integer.parseInt(SharedPrinterFields.VAT_N4_Dept)+1);
    		SharedPrinterFields.Printer_IPAddress = getPrinterIpAdd();
			PrinterInfo.SavePrinterInfo("IPAddress", SharedPrinterFields.Printer_IPAddress);
	    }
	    
    	if (RTchecks.checkLottery(isfwLotteryenabled())) {
    		SharedPrinterFields.Lotteria.setLotteryOn(SharedPrinterFields.Lotteria.getLottery_disabled() == 0);
	    	AsynchronousEvent.go(null, new GutenbergAction(GutenbergActions.PleaseReloadLottery, Lotteria.isEnable(), Lotteria.isPrintBarcode()));
    	}
    	
		LogPrinterLevel(SharedPrinterFields.RTPrinterId, fw, isfwLotteryenabled(), isfwRT2enabled(), isfwSMTKenabled(), isfwILotteryenabled());
		PrinterInfo.LogPrinterInfo();
		
		return fw;
	}

	private boolean pleaseCheck()
	{
		  try
		  {
			  System.out.println ("FP-21");
			  fiscalPrinter.setDeviceEnabled(true);
			  System.out.println ("FP-22");
			  return ( false );
		  }
		  catch (jpos.JposException e)
		  {
			  System.out.println ("FP-23 <"+e.toString()+">");
			  System.out.println ("ERROR during enable of printer");
			  e.printStackTrace();
			  return ( true );
		  }
	}
	  
	   private int checkRTStatus() throws JposException
	   {
		   	int rtstatus = RT_OK;
		   	
		   	RTStatus status = null;

		   	int rtMainStatus = 0;
		   	int outOfService = 0;
		   	boolean noWorkingPeriod = false;
		   	int oldFiletoSend = 0;
		   
			int ALLisOK = 7;
			int maxFileStilltoSend = 5;
			
			StringBuffer op = new StringBuffer("0");
			int ret = this.executeRTDirectIo(5001, 0, op);
			System.out.println("checkRTStatus - status : "+ret+" ("+Sprint.f("%06d", Integer.toBinaryString(ret))+")");
			
			rtMainStatus = ret;
			
			if (((ret & 0x04) >> 2) == 1)
			{
				System.out.println("checkRTStatus - Printer is RT model");
				
				outOfService = ((ret & 0x08) >> 3);
				System.out.println("checkRTStatus - RT Out Of Service : " + outOfService);
				
				SharedPrinterFields.Lotteria.setLottery_disabled(outOfService);
				SharedPrinterFields.Lotteria.setLotteryOn(SharedPrinterFields.Lotteria.getLottery_disabled() == 0);
				
				if (ret == ALLisOK){
			    	ret = RTstilltosend();
			    	System.out.println("checkRTStatus - result : "+ret);
			    	if (ret < 0){
			    		oldFiletoSend = (ret * (-1)) - 100;
			    		noWorkingPeriod = true;
			    		rtstatus = RT_NWP;
			    	}
			    	else if (ret > 100){
			    		RTtrytosend();
				    	ret = RTstilltosend();
				    	System.out.println("checkRTStatus - result : "+ret);
			    		oldFiletoSend = ret - 100;
			    		//if (oldFiletoSend >= maxFileStilltoSend)
			    		//	reply = RT_OLDFILESTOSEND;
			    	}
			    	
					StringBuffer abilita = new StringBuffer(">U/$1");	// abilita sconti indiretti
					this.executeRTDirectIo(0, 0, abilita);
					
			    	int ltret = LTstilltosend();
			    	System.out.println("checkRTStatus - result : "+ltret);
			    	if (ltret > 100){
			    		LTtrytosend();
			    		ltret = LTstilltosend();
				    	System.out.println("checkRTStatus - result : "+ltret);
				    	if (ltret > 100){
							StringBuffer readpending = new StringBuffer("<</?i/*6");
							this.executeRTDirectIo(0, 0, readpending);
				    	}
			    	}
				}
				else if (ret < ALLisOK){
					rtstatus = RT_PRESERVICE;	// in realtà significa NON censito oppure NON attivato
				}
				else if (ret > ALLisOK){
					rtstatus = RT_NOTINSERVICE;
				}
				
				System.out.println("checkRTStatus - RT No Working Period : " + noWorkingPeriod);
				System.out.println("checkRTStatus - RT Old File to send n. : " + oldFiletoSend);
				
			   // questo lo faccio solo per avere i dati da scrivere nel file di log
			   // non fa nessuna interrogazione ma salva solo i dati appena letti
			   status = new RTStatus("", rtMainStatus, -1, fiscalPrinter.getDayOpened(),
					   				 noWorkingPeriod, -1, oldFiletoSend, -1, -1, -1, outOfService,
					   				"", "", -1, -1, -1, -1);
			   
			   setLotteryfw("");
			   setRT2fw("");
			   setSMTKfw("");
			   setILotteryfw("");
			   
			   getCertificateSetting("");
			   getVPSetting();
			}
			else
			{
				System.out.println("checkRTStatus - Printer is NOT RT model");
				rtstatus = RT_KO;
			}
			
			logRTStatus(rtstatus, status);
			   
			return rtstatus;
	   }
	   
		private String getPrinterIpAdd()
		{
			String IpAdd = "";
			
			String ethernetSetting[] = getEthernetSetting();
			
			int ipadd = 0;
//			int submask = 1;
//			int gw = 2;
//			int dns = 3;
//			int port = 4;
//			int mac = 5;
			
			StringTokenizer st = new StringTokenizer(ethernetSetting[ipadd], " ");
			String[] tmp = new String[st.countTokens()];
			for (int i = 0; i < tmp.length; i++) {
				tmp[i] = st.nextToken();
			}
			IpAdd = tmp[1];
			
			System.out.println("getPrinterIpAdd - returning : "+IpAdd);
			return IpAdd;
		}
		
	    private String[] getEthernetSetting()
	    {
	    	String[] ret = null;
	    	
	        int cmdInt = 0;
	        int[] mydata = {0};
	        String cmd = "<</?e";
	        
		   DirectIOListener p=new DirectIOListener();
		   fiscalPrinter.addDirectIOListener((jpos.events.DirectIOListener) p);
		   
	        try {
	        	p.started = true;
	        	p.buffer="";
				
				directIO(cmdInt, mydata, cmd);
				while (p.started) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
				}
				
				//System.out.println("getEthernetSetting - buffer = "+p.buffer);
				
				StringTokenizer st = new StringTokenizer(p.buffer, ",");
				ret = new String[st.countTokens()];
				for (int i = 0; i < ret.length; i++) {
					ret[i] = st.nextToken();
					//System.out.println("getEthernetSetting - ret["+i+"] = "+ret[i]+" - "+ret[i].length());
				}
				
			} catch (JposException e) {
				System.out.println("getEthernetSetting - Exception : " + e.getMessage());
			}
	        
	        fiscalPrinter.removeDirectIOListener(p);
	        
	        return ret;
	    }
	    
	    private String getPrinterId() throws JposException {
	    	int[] opt = new int[1];
	    	String[] printerId = new String[1];
	    	
	    	if (isRTModel()) {
		    	String printerIdModel;
		    	String printerIdManufacturer;
		    	String printerIdNumber;
		    	
		    	fiscalPrinter.getData(FiscalPrinterConst.FPTR_GD_PRINTER_ID, opt, printerId);
				System.out.println("getPrinterId - printerId : <"+printerId[0]+">");
		    	printerIdManufacturer = printerId[0].trim().substring(3, 5);
		    	printerIdModel = "M" + printerId[0].trim().substring(0, 2);
		    	printerIdNumber = printerId[0].trim().substring(5);
				System.out.println("getPrinterId - returning : "+printerIdManufacturer + printerIdModel + printerIdNumber);
		      	return (printerIdManufacturer + printerIdModel + printerIdNumber);
	    	}
	    	else {
		    	fiscalPrinter.getData(FiscalPrinterConst.FPTR_GD_PRINTER_ID, opt, printerId);
				System.out.println("getPrinterId - returning : "+printerId[0].trim());
		    	return (printerId[0].trim());
	    	}
	    }
	    
	    private String getPrinterDesc(String printerid)
	    {
	    	String reply = "";
	    	String Manufacturer = "";
	    	String Type = "";
	    	String Model = "";
	    	
	    	if (!isRTModel())
	    		return reply;
	    	
	    	Manufacturer = printerid.substring(0, 2);
	    	
	    	if (Integer.parseInt(Manufacturer) == 99) {
	    		Manufacturer = "Epson";
	    		
	    		Type = printerid.substring(2, 3);
	    		if (Type.equalsIgnoreCase("I"))
	    			Type = "Native";
	    		else if (Type.equalsIgnoreCase("M"))
	    			Type = "Modified";
	    		
	    		Model = printerid.substring(3, 5);
	    		if (Model.equalsIgnoreCase("EX") || Model.equalsIgnoreCase("EB"))
	    			Model = "FP-81 II";
	    		else if (Model.equalsIgnoreCase("EY") || Model.equalsIgnoreCase("EC"))
	    			Model = "FP-90 III";
	    	}
	    	else if (Integer.parseInt(Manufacturer) == 88) {
	    		Manufacturer = "Diebold-Nixdorf";
	    		Model = "RT-One";
	    	}
	    	else if (Integer.parseInt(Manufacturer) == 72) {
	    		Manufacturer = "Rch";
	    		Model = "Print!F";
	    	}
	    	
	    	reply = Sprint.f("%s %s %s", Manufacturer, Model, Type);
	    	
			System.out.println("getPrinterDesc - returning : "+reply);
	    	return reply;
	    }
	    
	   private int RTstilltosend()
	   {
		   int ret = 0;
		   
		   StringBuffer op = new StringBuffer("0");
		   ret = this.executeRTDirectIo(6100, 0, op);
		   
		   return ret;
	   }
	   
	   private void RTtrytosend()
	   {
		   StringBuffer key = new StringBuffer(SharedPrinterFields.KEY_Z);
		   this.executeRTDirectIo(0, 0, key);

		   System.out.println("checkRTStatus - RTtrytosend start...");
		   StringBuffer send = new StringBuffer("=C422");
		   this.executeRTDirectIo(0, 0, send);
		   System.out.println("checkRTStatus - RTtrytosend ...end");

		   key = new StringBuffer(SharedPrinterFields.KEY_REG);
		   this.executeRTDirectIo(0, 0, key);
	   }
	   
	   private int LTstilltosend()
	   {
		   int ret = 0;
		   
		   if (SharedPrinterFields.Lotteria.isLotteryOn()){
			   StringBuffer op = new StringBuffer("0");
			   ret = this.executeRTDirectIo(3100, 0, op);
		   }
		   
		   return ret;
	   }
	   
	   private void LTtrytosend()
	   {
		   if (SharedPrinterFields.Lotteria.isLotteryOn()){
			   StringBuffer key = new StringBuffer(SharedPrinterFields.KEY_Z);
			   this.executeRTDirectIo(0, 0, key);

			   System.out.println("checkRTStatus - LTtrytosend start...");
			   StringBuffer send = new StringBuffer("=C482");
			   this.executeRTDirectIo(0, 0, send);
			   System.out.println("checkRTStatus - LTtrytosend ...end");

			   key = new StringBuffer(SharedPrinterFields.KEY_REG);
			   this.executeRTDirectIo(0, 0, key);
		   }
	   }
	   
	   private void logRTStatus(int errcode)
	   {
		   // RT_OLDFILESTOSEND = -3;
		   // RT_NOTINSERVICE = -2;
		   // RT_KO = -1;
		   // RT_OK = 0;
		   // RT_NWP = 1;
		   // RT_PRESERVICE = 2;
		   
		   String errmsg[] = {"Number of files in the queue for sending with a date older than the value expressed in days programmed",
				   			  "Not in service (MF mode)",
		   					  "Printer is NOT RT model",
		   					  "Printer is RT model",
		   					  "A Zreport must be performed before any operation can happen. A Zreport has not occurred for more than a day",
		   					  "Programmed RT Pre-Servizio"};
		   
		   File inout = null;		
		   FileOutputStream fos = null;
		   PrintStream ps = null;
		   
		   Calendar c = Calendar.getInstance();
           SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
           
           String s = sdf.format(c.getTime()); 
		   
		   s = s + " - Printer started";
		   
		   try {
			   String dayofmonth = Sprint.f("%02d", ""+c.getTime().getDate());
			   String filename = SharedPrinterFields.rtlog_folder+SharedPrinterFields.rtlog_name+"_"+dayofmonth+SharedPrinterFields.rtlog_ext;
			   inout = new File(filename);
			   fos = new FileOutputStream(inout,false);
			   ps = new PrintStream(fos);
			   
			   ps.println(s);
				
			   s = "errcode : " + errcode + " - " + errmsg[errcode + 3] + R3define.CrLf;
			   ps.print(s);
				
			   ps.close();
			   ps = null;
			   fos.close();
			   fos = null;
			   inout = null;
		   } catch(Exception e) {
			   System.out.println("logRTStatus - Exception : " + e.getMessage());
		   }
	   }
	   
	   private void logRTStatus(int errcode, RTStatus status)
	   {
		   // RT_OLDFILESTOSEND = -3;
		   // RT_NOTINSERVICE = -2;
		   // RT_KO = -1;
		   // RT_OK = 0;
		   // RT_NWP = 1;
		   // RT_PRESERVICE = 2;
		   
		   String errmsg[] = {"Number of files in the queue for sending with a date older than the value expressed in days programmed",
				   			  "Not in service (MF mode)",
		   					  "Printer is NOT RT model",
		   					  "Printer is RT model",
		   					  "A Zreport must be performed before any operation can happen. A Zreport has not occurred for more than a day",
		   					  "Programmed RT Pre-Servizio"};
		   
		   File inout = null;		
		   FileOutputStream fos = null;
		   PrintStream ps = null;
		   int NonDisp = -1;
		   
		   Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        
        String s = sdf.format(c.getTime()); 
		   
		   s = s + " - Printer started";
		   
		   try {
			   String dayofmonth = Sprint.f("%02d", c.getTime().getDate());
			   String filename = SharedPrinterFields.rtlog_folder+SharedPrinterFields.rtlog_name+"_"+dayofmonth+SharedPrinterFields.rtlog_ext;
			   inout = new File(filename);
			   fos = new FileOutputStream(inout,false);
			   ps = new PrintStream(fos);
			   
			   ps.println(s);
			   if (status != null) {
				   if (status.rtType.length() > 0) ps.println("RT type : " + status.rtType);
				   if (status.mainStatus != NonDisp) ps.println("RT Main status : " + status.mainStatus);
				   if (status.subStatus != NonDisp) ps.println("RT Sub status : " + status.subStatus);
				   ps.println("RT Daily open : " + status.dailyOpen);
				   ps.println("RT No Working Period : " + status.rtNoWorkingPeriod);
				   if (status.rtFileToSend != NonDisp) ps.println("RT File to send n. : " + status.rtFileToSend);
				   if (status.rtOldFileToSend != NonDisp) ps.println("RT Old File to send n. : " + status.rtOldFileToSend);
				   if (status.rtFileRejected != NonDisp) ps.println("RT File rejected n. : " + status.rtFileRejected);
				   if (status.fwBuildNumber != NonDisp) {
					   ps.println("RT FW Build Number : " + status.fwBuildNumber);
					   fwBuildNumber = ""+status.fwBuildNumber;
				   }
				   if (status.lastFwUpdateResult != NonDisp) ps.println("RT Last FW Update Result : " + status.lastFwUpdateResult);
				   if (status.outOfService != NonDisp) ps.println("RT Out Of Service : " + status.outOfService);
				   if (status.expiryDateCD.length() > 0) ps.println("RT CE Certificate Expiry date : " + status.expiryDateCD);
				   if (status.expiryDateCA.length() > 0) ps.println("RT CA Certificate Expiry date : " + status.expiryDateCA);
				   if (status.dgfeFileSystem != NonDisp) ps.println("RT DGFE File System : " + status.dgfeFileSystem);
				   if (status.trainingMode != NonDisp) ps.println("RT Trainig mode : " + status.trainingMode);
				   if (status.archivedFileRejected != NonDisp) ps.println("RT Archived File rejected n. : " + status.archivedFileRejected);
				   if (status.ripristinoCertif != NonDisp) ps.println("RT Recover Certificate : " + status.ripristinoCertif);
			   }
			   
			   s = "errcode : " + errcode + " - " + errmsg[errcode + 3] + R3define.CrLf;
			   ps.print(s);
				
			   ps.close();
			   ps = null;
			   fos.close();
			   fos = null;
			   inout = null;
		   } catch(Exception e) {
			   System.out.println("logRTStatus - Exception : " + e.getMessage());
		   }
	   }
	   
	   private RTStatus getRTStatus()
	   {
		   RTStatus status = null;
		   StringBuffer op = new StringBuffer("01");
		   executeRTDirectIo(1138, 0, op);
		   status = new RTStatus(op.toString());
		   return status;
	   }
	   
		private void LogPrinterLevel(String matricola, double firmware, boolean lotteria, boolean rt2, boolean smtk, boolean ilotteria)
		{
			if (isNotRTModel())
				return;
			
			iconic.mytrade.gutenberg.jpos.printer.utils.LogPrinterLevel.log("RchPrintF", matricola, firmware, lotteria, rt2, smtk, ilotteria);
		}
		
	    public static int executeRTDirectIo (int Command, int pData, StringBuffer bjct)
	    {
	    	int reply = 0;
	    	
            int[] dt={pData};
            String[] pString = {new String(bjct)};
            String str = bjct.toString();
            try
            {
            	System.out.println("executeRTDirectIo - Command : "+Command+" - dt : "+dt[0]);
            	if (Command >= 1000) {
            		fiscalPrinter.directIO(Command, dt, pString);
	            	System.out.println("executeRTDirectIo - pString : "+pString[0]);
	            	if (Command == 8003)
	            		bjct.append(pString[0]);
            	}
            	else {
	            	System.out.println("executeRTDirectIo - str input: "+str);
	            	fiscalPrinter.directIO(Command, dt, str);
	            	System.out.println("executeRTDirectIo - str output: "+str);
            	}
            }
            catch(Exception e)
            {
            	System.out.println("Data error\nException: "+ e.getMessage());
            	
            	if (((Command == 6000) || (Command == 6001)) && (pData == 1))
            		dt[0] = 0;	// document not voidable/refundable
            	
            	if ((Command == 0) && (pData == 0))
            		return -1;	// per non sporcare dt[0] assegnandogli -1 
            }
           reply = dt[0];
	    	
	    	return reply;
	    }
	  
	    public String getRTSettings(String specificSetting)
	    {
	    	String reply = "";
	    	
	        int cmdInt = 0;
	        int[] mydata = {0};
	        String cmd = SharedPrinterFields.KEY_SRV;
	        try {
	        	fiscalPrinter.directIO(cmdInt, mydata, cmd);
			} catch (JposException e) {
				System.out.println("getRTSettings - Exception : " + e.getMessage());
			}
	        
	 	   DirectIOListener p=new DirectIOListener();
	 	  fiscalPrinter.addDirectIOListener((jpos.events.DirectIOListener) p);

	       cmd = "<</?C/("+specificSetting+")";
	       try {
	    	   p.started = true;
	    	   p.buffer="";
	    	   directIO(cmdInt, mydata, cmd);
	    	   while (p.started) {
	    		   try {
	    			   Thread.sleep(500);
	    		   } catch (InterruptedException e) {
	    		   }
	    	   }
	    	   
	    	   reply = p.buffer;
	    	   
			} catch (JposException e) {
				System.out.println("getRTSettings - Exception : " + e.getMessage());
			}
	        
	       fiscalPrinter.removeDirectIOListener(p);
	   	   
	        cmd = SharedPrinterFields.KEY_REG;
	        try {
	        	fiscalPrinter.directIO(cmdInt, mydata, cmd);
			} catch (JposException e) {
				System.out.println("getRTSettings - Exception : " + e.getMessage());
			}
	        
	        System.out.println("getRTSettings - reply = " + reply);
	        return reply;
	    }
		    
	   private void getCertificateSetting(String expiryDateCD)
	   {
		   Date d1=null;
		   Date d2=null;
		   
		   String[] date = new String[1];
		   try {
			   fiscalPrinter.getDate(date);
		   } catch (JposException e1) {
			   System.out.println("checkRTStatus - getCertificateSetting - Exception : " + e1.getMessage());
			   return;
		   }
		   //System.out.println("checkRTStatus - getCertificateSetting - Printer date = "+date[0]);
        	
		   int cmdInt = 0;
		   int[] mydata = {0};
		   String cmd = "<</?D/*11";
	        
		   DirectIOListener p=new DirectIOListener();
		   fiscalPrinter.addDirectIOListener((jpos.events.DirectIOListener) p);
		   
		   try {
			   p.started = true;
			   p.buffer="";
				
			   directIO(cmdInt, mydata, cmd);
			   while (p.started) {
				   try {
					   Thread.sleep(500);
				   } catch (InterruptedException e) {
				   }
			   }
				
			   System.out.println("checkRTStatus - getCertificateSetting - Expiry date = "+p.buffer);
				
			   SimpleDateFormat sdformat = new SimpleDateFormat("yyyy/MM/dd");
			   d1 = sdformat.parse(p.buffer);
			   d2 = sdformat.parse(date[0].substring(4, 8)+"/"+date[0].substring(2, 4)+"/"+date[0].substring(0, 2));
			   
		   } catch (Exception e) {
			   System.out.println("checkRTStatus - getCertificateSetting - Exception : " + e.getMessage());
			   return;
		   } finally {
			   fiscalPrinter.removeDirectIOListener(p);
		   }
		   
		   boolean expired = (d1.compareTo(d2) < 0);
		   System.out.println("checkRTStatus - getCertificateSetting - Expired = "+expired);
		   setExpiredCertificate(expired);
		   
		   Date ceexpirydate = new Date(d1.getTime());
		   SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		   PrinterInfo.SavePrinterInfo("CE Expiry date", sdf.format(ceexpirydate).toString());
		   
		   return;
	    }
				
	private void getVPSetting()
	{
	   String vpexpirydate = "00/00/00";
	   String vplastdate = "00/00/00";
	   String vpexpiring = "";
	   String vpexpired = "";
	   String vpstateactive = "";
	   
	   Date d1=null;
	   Date d2=null;
	   
	   DirectIOListener p=new DirectIOListener();
	   fiscalPrinter.addDirectIOListener((jpos.events.DirectIOListener) p);
	   
	   try {
		   int cmdInt = 0;
		   int[] mydata = {0};
		   String cmd = "<</?D/*2";
	        
		   p.started = true;
		   p.buffer="";
			
		   directIO(cmdInt, mydata, cmd);
		   while (p.started) {
			   try {
				   Thread.sleep(500);
			   } catch (InterruptedException e) {
			   }
		   }
			
		   System.out.println("checkRTStatus - getVPSetting - Last date = "+p.buffer);

		   if (!p.buffer.equalsIgnoreCase("0")) {
			   SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			   d1 = sdf.parse(p.buffer);
			   d2 = new Date(d1.getTime());
			   sdf = new SimpleDateFormat("dd/MM/yyyy");
			   vplastdate = sdf.format(d2).toString();
			   System.out.println("checkRTStatus - getVPSetting - VP Last date = "+vplastdate);
		   }
		   
		   cmd = "<</?i/*2";
	        
		   p.started = true;
		   p.buffer="";
			
		   directIO(cmdInt, mydata, cmd);
		   while (p.started) {
			   try {
				   Thread.sleep(500);
			   } catch (InterruptedException e) {
			   }
		   }
			
		   System.out.println("checkRTStatus - getVPSetting - Time reading = "+p.buffer);
		   
		   int C = Integer.parseInt(p.buffer.substring(2, 3));
		   int D = Integer.parseInt(p.buffer.substring(3, 4));
		   vpexpiring = (C == 1 ? "true" : "false");
		   vpexpired = (D == 1 ? "true" : "false");
		   System.out.println("checkRTStatus - getVPSetting - VP Expiring = "+vpexpiring);
		   System.out.println("checkRTStatus - getVPSetting - VP Expired = "+vpexpired);
		   
		   cmd = "<</?i/*7";
	        
		   p.started = true;
		   p.buffer="";
			
		   directIO(cmdInt, mydata, cmd);
		   while (p.started) {
			   try {
				   Thread.sleep(500);
			   } catch (InterruptedException e) {
			   }
		   }
			
		   System.out.println("checkRTStatus - getVPSetting - State active = "+p.buffer);
		   
		   int A = Integer.parseInt(p.buffer.substring(0, 1));
		   vpstateactive = (A == 1 ? "true" : "false");
		   System.out.println("checkRTStatus - getVPSetting - VP State active = "+vpstateactive);
		   
	   } catch (Exception e) {
		   System.out.println("checkRTStatus - getVPSetting - Exception : " + e.getMessage());
		   return;
	   } finally {
		   fiscalPrinter.removeDirectIOListener(p);
	   }
	   	   
	   PrinterInfo.SavePrinterInfo("VP Expiry date", vpexpirydate);
	   PrinterInfo.SavePrinterInfo("VP Last date", vplastdate);
	   PrinterInfo.SavePrinterInfo("VP Expiring", vpexpiring);
	   PrinterInfo.SavePrinterInfo("VP Expired", vpexpired);
	   PrinterInfo.SavePrinterInfo("VP State Active", vpstateactive);
	   
	   return;
    }
			   
	private void setLocalAccessControl()
	{
		if (isfwRT2disabled())
			return;
		
		if (true)
			return;		// di default non dovrebbe essere necessario
		
		StringBuffer key = new StringBuffer(SharedPrinterFields.KEY_Z);
		this.executeRTDirectIo(0, 0, key);

		StringBuffer sbcmd = new StringBuffer("1703"+",0");
		this.executeRTDirectIo(1301, 0, sbcmd);

		key = new StringBuffer(SharedPrinterFields.KEY_REG);
		this.executeRTDirectIo(0, 0, key);
	}
		
	private void setFidelityOption(int mode)
	{
		System.out.println("RT2 - setFidelityOption - mode = "+mode);
		
		StringBuffer sbcmd = new StringBuffer(">C933/$"+mode);
		this.executeRTDirectIo(0, 0, sbcmd);
	}
		
	private void setAppendixOption(int mode, int cut)
	{
		System.out.println("RT2 - setAppendixOption - mode = "+mode+" - cut = "+cut);
		
		StringBuffer sbcmd = new StringBuffer(">C170/$501"+"/&"+mode+"/["+cut);
		this.executeRTDirectIo(0, 0, sbcmd);
	}
		
    private void pleaseBeep(int lunghezza, int interruzione)
    {
    	makeNoise();
    	Beeping sound = new Beeping();
    	sound.beep(lunghezza,interruzione);
    }
    
	private void makeNoise()
	{
			try
			{
				throw new Exception();
			}
			catch ( Exception e )
			{
				System.out.println ( makeNoise1 ( e ) );
			}
	  }
	
	  private String makeNoise1( Exception e)
	  {
		  try {
			    StringWriter sw = new StringWriter();
			    PrintWriter pw = new PrintWriter(sw);
			    e.printStackTrace(pw);
			    return (sw.toString());
			  }
			  catch(Exception e2) 
			  {
			    return ("bad stack2string");
			  }
	  }
	  
    /* printer commands - Start
     *
     */
	  
	public int getActualCurrency() throws JposException {
		return fiscalPrinter.getActualCurrency();
	}

	public String getAdditionalHeader() throws JposException {
		return fiscalPrinter.getAdditionalHeader();
	}

	public String getAdditionalTrailer() throws JposException {
		return fiscalPrinter.getAdditionalTrailer();
	}

	public boolean getCapAdditionalHeader() throws JposException {
		return fiscalPrinter.getCapAdditionalHeader();
	}

	public boolean getCapAdditionalTrailer() throws JposException {
		return fiscalPrinter.getCapAdditionalTrailer();
	}

	public boolean getCapChangeDue() throws JposException {
		return fiscalPrinter.getCapChangeDue();
	}

	public boolean getCapEmptyReceiptIsVoidable() throws JposException {
		return fiscalPrinter.getCapEmptyReceiptIsVoidable();
	}

	public boolean getCapFiscalReceiptStation() throws JposException {
		return fiscalPrinter.getCapFiscalReceiptStation();
	}

	public boolean getCapFiscalReceiptType() throws JposException {
		return fiscalPrinter.getCapFiscalReceiptType();
	}

	public boolean getCapMultiContractor() throws JposException {
		return fiscalPrinter.getCapMultiContractor();
	}

	public boolean getCapOnlyVoidLastItem() throws JposException {
		return fiscalPrinter.getCapOnlyVoidLastItem();
	}

	public boolean getCapPackageAdjustment() throws JposException {
		return fiscalPrinter.getCapPackageAdjustment();
	}

	public boolean getCapPostPreLine() throws JposException {
		return fiscalPrinter.getCapPostPreLine();
	}

	public boolean getCapSetCurrency() throws JposException {
		return fiscalPrinter.getCapSetCurrency();
	}

	public boolean getCapTotalizerType() throws JposException {
		return fiscalPrinter.getCapTotalizerType();
	}

	public String getChangeDue() throws JposException {
		return fiscalPrinter.getChangeDue();
	}

	public int getContractorId() throws JposException {
		return fiscalPrinter.getContractorId();
	}

	public int getDateType() throws JposException {
		return fiscalPrinter.getDateType();
	}

	public int getFiscalReceiptStation() throws JposException {
		return fiscalPrinter.getFiscalReceiptStation();
	}

	public int getFiscalReceiptType() throws JposException {
		return fiscalPrinter.getFiscalReceiptType();
	}

	public int getMessageType() throws JposException {
		return fiscalPrinter.getMessageType();
	}

	public String getPostLine() throws JposException {
		return fiscalPrinter.getPostLine();
	}

	public String getPreLine() throws JposException {
		return fiscalPrinter.getPreLine();
	}

	public int getTotalizerType() throws JposException {
		return fiscalPrinter.getTotalizerType();
	}

	public void printRecCash(long arg0) throws JposException {
		fiscalPrinter.printRecCash(arg0);
	}

	public void printRecItemFuel(String arg0, long arg1, int arg2, int arg3, long arg4, String arg5, long arg6,	String arg7) throws JposException {
		fiscalPrinter.printRecItemFuel(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
		
	}

	public void printRecItemFuelVoid(String arg0, long arg1, int arg2, long arg3) throws JposException {
		fiscalPrinter.printRecItemFuelVoid(arg0, arg1, arg2, arg3);
		
	}

	public void printRecPackageAdjustVoid(int arg0, String arg1) throws JposException {
		fiscalPrinter.printRecPackageAdjustVoid(arg0, arg1);
		
	}

	public void printRecPackageAdjustment(int arg0, String arg1, String arg2) throws JposException {
		fiscalPrinter.printRecPackageAdjustment(arg0, arg1, arg2);
		
	}

	public void printRecRefundVoid(String arg0, long arg1, int arg2) throws JposException {
		fiscalPrinter.printRecRefundVoid(arg0, arg1, arg2);
		
	}

	public void printRecSubtotalAdjustVoid(int arg0, long arg1) throws JposException {
		fiscalPrinter.printRecSubtotalAdjustVoid(arg0, arg1);
		
	}

	public void printRecTaxID(String arg0) throws JposException {
		fiscalPrinter.printRecTaxID(arg0);
		
	}

	public void setAdditionalHeader(String arg0) throws JposException {
		fiscalPrinter.setAdditionalHeader(arg0);
	}

	public void setAdditionalTrailer(String arg0) throws JposException {
		fiscalPrinter.setAdditionalTrailer(arg0);
	}

	public void setChangeDue(String arg0) throws JposException {
		fiscalPrinter.setChangeDue(arg0);
		
	}

	public void setContractorId(int arg0) throws JposException {
		fiscalPrinter.setContractorId(arg0);
		
	}

	public void setCurrency(int arg0) throws JposException {
		fiscalPrinter.setCurrency(arg0);
		
	}

	public void setDateType(int arg0) throws JposException {
		fiscalPrinter.setDateType(arg0);
		
	}

	public void setFiscalReceiptStation(int arg0) throws JposException {
		fiscalPrinter.setFiscalReceiptStation(arg0);
		
	}

	public void setFiscalReceiptType(int arg0) throws JposException {
		fiscalPrinter.setFiscalReceiptType(arg0);
		
	}

	public void setMessageType(int arg0) throws JposException {
		fiscalPrinter.setMessageType(arg0);
		
	}

	public void setPostLine(String arg0) throws JposException {
		fiscalPrinter.setPostLine(arg0);
		
	}

	public void setPreLine(String arg0) throws JposException {
		fiscalPrinter.setPreLine(arg0);
		
	}

	public void setTotalizerType(int arg0) throws JposException {
		fiscalPrinter.setTotalizerType(arg0);
		
	}

	private void addDirectIOListener(DirectIOListener arg0) {
		fiscalPrinter.addDirectIOListener(arg0);
		
	}

	public void addErrorListener(ErrorListener arg0) {
		fiscalPrinter.addErrorListener(arg0);
		
	}

	public void addOutputCompleteListener(OutputCompleteListener arg0) {
		fiscalPrinter.addOutputCompleteListener(arg0);
		
	}

	public void addStatusUpdateListener(StatusUpdateListener arg0) {
		fiscalPrinter.addStatusUpdateListener(arg0);
		
	}

	public void statusUpdateOccurred(StatusUpdateEvent arg0) {
		//((StatusUpdateListener) fiscalPrinter).statusUpdateOccurred(arg0);
		
	}

	public void addDirectIOListener(jpos.events.DirectIOListener arg0) {
		fiscalPrinter.addDirectIOListener(arg0);
		
	}

	public void removeDirectIOListener(jpos.events.DirectIOListener arg0) {
		fiscalPrinter.removeDirectIOListener(arg0);
		
	}

	public void beginFiscalDocument(int arg0) throws JposException {
		fiscalPrinter.beginFiscalDocument(arg0);
		
	}

	public void beginFiscalReceipt(boolean arg0) throws JposException {
		fiscalPrinter.beginFiscalReceipt(arg0);
	}

	public void beginFixedOutput(int arg0, int arg1) throws JposException {
		fiscalPrinter.beginFixedOutput(arg0, arg1);
		
	}

	public void beginInsertion(int arg0) throws JposException {
		fiscalPrinter.beginInsertion(arg0);
		
	}

	public void beginItemList(int arg0) throws JposException {
		fiscalPrinter.beginItemList(arg0);
		
	}

	public void beginNonFiscal() throws JposException {
		fiscalPrinter.beginNonFiscal();
	}

	public void beginRemoval(int arg0) throws JposException {
		fiscalPrinter.beginRemoval(arg0);
		
	}

	public void beginTraining() throws JposException {
		fiscalPrinter.beginTraining();
		
	}

	public void clearError() throws JposException {
		fiscalPrinter.clearError();
		
	}

	public void clearOutput() throws JposException {
		fiscalPrinter.clearOutput();
		
	}

	public void endFiscalDocument() throws JposException {
		fiscalPrinter.endFiscalDocument();
		
	}

	public void endFiscalReceipt(boolean arg0) throws JposException {
		fiscalPrinter.endFiscalReceipt(arg0);
	}

	public void endFixedOutput() throws JposException {
		fiscalPrinter.endFixedOutput();
		
	}

	public void endInsertion() throws JposException {
		fiscalPrinter.endInsertion();
		
	}

	public void endItemList() throws JposException {
		fiscalPrinter.endItemList();
		
	}

	public void endNonFiscal() throws JposException {
		fiscalPrinter.endNonFiscal();
	}

	public void endRemoval() throws JposException {
		fiscalPrinter.endRemoval();
		
	}

	public void endTraining() throws JposException {
		fiscalPrinter.endTraining();
		
	}

	public int getAmountDecimalPlace() throws JposException {
		return fiscalPrinter.getAmountDecimalPlace();
	}

	public boolean getAsyncMode() throws JposException {
		return fiscalPrinter.getAsyncMode();
	}

	public boolean getCapAdditionalLines() throws JposException {
		return fiscalPrinter.getCapAdditionalLines();
	}

	public boolean getCapAmountAdjustment() throws JposException {
		return fiscalPrinter.getCapAmountAdjustment();
	}

	public boolean getCapAmountNotPaid() throws JposException {
		return fiscalPrinter.getCapAmountNotPaid();
	}

	public boolean getCapCheckTotal() throws JposException {
		return fiscalPrinter.getCapCheckTotal();
	}

	public boolean getCapCoverSensor() throws JposException {
		return fiscalPrinter.getCapCoverSensor();
	}

	public boolean getCapDoubleWidth() throws JposException {
		return fiscalPrinter.getCapDoubleWidth();
	}

	public boolean getCapDuplicateReceipt() throws JposException {
		return fiscalPrinter.getCapDuplicateReceipt();
	}

	public boolean getCapFixedOutput() throws JposException {
		return fiscalPrinter.getCapFixedOutput();
	}

	public boolean getCapHasVatTable() throws JposException {
		return fiscalPrinter.getCapHasVatTable();
	}

	public boolean getCapIndependentHeader() throws JposException {
		return fiscalPrinter.getCapIndependentHeader();
	}

	public boolean getCapItemList() throws JposException {
		return fiscalPrinter.getCapItemList();
	}

	public boolean getCapJrnEmptySensor() throws JposException {
		return fiscalPrinter.getCapJrnEmptySensor();
	}

	public boolean getCapJrnNearEndSensor() throws JposException {
		return fiscalPrinter.getCapJrnNearEndSensor();
	}

	public boolean getCapJrnPresent() throws JposException {
		return fiscalPrinter.getCapJrnPresent();
	}

	public boolean getCapNonFiscalMode() throws JposException {
		return fiscalPrinter.getCapNonFiscalMode();
	}

	public boolean getCapOrderAdjustmentFirst() throws JposException {
		return fiscalPrinter.getCapOrderAdjustmentFirst();
	}

	public boolean getCapPercentAdjustment() throws JposException {
		return fiscalPrinter.getCapPercentAdjustment();
	}

	public boolean getCapPositiveAdjustment() throws JposException {
		return fiscalPrinter.getCapPositiveAdjustment();
	}

	public boolean getCapPowerLossReport() throws JposException {
		return fiscalPrinter.getCapPowerLossReport();
	}

	public int getCapPowerReporting() throws JposException {
		return fiscalPrinter.getCapPowerReporting();
	}

	public boolean getCapPredefinedPaymentLines() throws JposException {
		return fiscalPrinter.getCapPredefinedPaymentLines();
	}

	public boolean getCapRecEmptySensor() throws JposException {
		return fiscalPrinter.getCapRecEmptySensor();
	}

	public boolean getCapRecNearEndSensor() throws JposException {
		return fiscalPrinter.getCapRecNearEndSensor();
	}

	public boolean getCapRecPresent() throws JposException {
		return fiscalPrinter.getCapRecPresent();
	}

	public boolean getCapReceiptNotPaid() throws JposException {
		return fiscalPrinter.getCapReceiptNotPaid();
	}

	public boolean getCapRemainingFiscalMemory() throws JposException {
		return fiscalPrinter.getCapRemainingFiscalMemory();
	}

	public boolean getCapReservedWord() throws JposException {
		return fiscalPrinter.getCapReservedWord();
	}

	public boolean getCapSetHeader() throws JposException {
		return fiscalPrinter.getCapSetHeader();
	}

	public boolean getCapSetPOSID() throws JposException {
		return fiscalPrinter.getCapSetPOSID();
	}

	public boolean getCapSetStoreFiscalID() throws JposException {
		return fiscalPrinter.getCapSetStoreFiscalID();
	}

	public boolean getCapSetTrailer() throws JposException {
		return fiscalPrinter.getCapSetTrailer();
	}

	public boolean getCapSetVatTable() throws JposException {
		return fiscalPrinter.getCapSetVatTable();
	}

	public boolean getCapSlpEmptySensor() throws JposException {
		return fiscalPrinter.getCapSlpEmptySensor();
	}

	public boolean getCapSlpFiscalDocument() throws JposException {
		return fiscalPrinter.getCapSlpFiscalDocument();
	}

	public boolean getCapSlpFullSlip() throws JposException {
		return fiscalPrinter.getCapSlpFullSlip();
	}

	public boolean getCapSlpNearEndSensor() throws JposException {
		return fiscalPrinter.getCapSlpNearEndSensor();
	}

	public boolean getCapSlpPresent() throws JposException {
		return fiscalPrinter.getCapSlpPresent();
	}

	public boolean getCapSlpValidation() throws JposException {
		return fiscalPrinter.getCapSlpValidation();
	}

	public boolean getCapSubAmountAdjustment() throws JposException {
		return fiscalPrinter.getCapSubAmountAdjustment();
	}

	public boolean getCapSubPercentAdjustment() throws JposException {
		return fiscalPrinter.getCapSubPercentAdjustment();
	}

	public boolean getCapSubtotal() throws JposException {
		return fiscalPrinter.getCapSubtotal();
	}

	public boolean getCapTrainingMode() throws JposException {
		return fiscalPrinter.getCapTrainingMode();
	}

	public boolean getCapValidateJournal() throws JposException {
		return fiscalPrinter.getCapValidateJournal();
	}

	public boolean getCapXReport() throws JposException {
		return fiscalPrinter.getCapXReport();
	}

	public boolean getCheckTotal() throws JposException {
		return fiscalPrinter.getCheckTotal();
	}

	public int getCountryCode() throws JposException {
		return fiscalPrinter.getCountryCode();
	}

	public boolean getCoverOpen() throws JposException {
		return fiscalPrinter.getCoverOpen();
	}

	public void getDate(String[] arg0) throws JposException {
		fiscalPrinter.getDate(arg0);
	}

	public boolean getDayOpened() throws JposException {
		return fiscalPrinter.getDayOpened();
	}

	public int getDescriptionLength() throws JposException {
		return fiscalPrinter.getDescriptionLength();
	}

	public boolean getDuplicateReceipt() throws JposException {
		return fiscalPrinter.getDuplicateReceipt();
	}

	public int getErrorLevel() throws JposException {
		return fiscalPrinter.getErrorLevel();
	}

	public int getErrorOutID() throws JposException {
		return fiscalPrinter.getErrorOutID();
	}

	public int getErrorState() throws JposException {
		return fiscalPrinter.getErrorState();
	}

	public int getErrorStation() throws JposException {
		return fiscalPrinter.getErrorStation();
	}

	public String getErrorString() throws JposException {
		return fiscalPrinter.getErrorString();
	}

	public boolean getFlagWhenIdle() throws JposException {
		return fiscalPrinter.getFlagWhenIdle();
	}

	public boolean getJrnEmpty() throws JposException {
		return fiscalPrinter.getJrnEmpty();
	}

	public boolean getJrnNearEnd() throws JposException {
		return fiscalPrinter.getJrnNearEnd();
	}

	public int getMessageLength() throws JposException {
		return fiscalPrinter.getMessageLength();
	}

	public int getNumHeaderLines() throws JposException {
		return fiscalPrinter.getNumHeaderLines();
	}

	public int getNumTrailerLines() throws JposException {
		return fiscalPrinter.getNumTrailerLines();
	}

	public int getNumVatRates() throws JposException {
		return fiscalPrinter.getNumVatRates();
	}

	public int getOutputID() throws JposException {
		return fiscalPrinter.getOutputID();
	}

	public int getPowerNotify() throws JposException {
		return fiscalPrinter.getPowerNotify();
	}

	public int getPowerState() throws JposException {
		return fiscalPrinter.getPowerState();
	}

	public String getPredefinedPaymentLines() throws JposException {
		return fiscalPrinter.getPredefinedPaymentLines();
	}

	public int getPrinterState() throws JposException {
		return fiscalPrinter.getPrinterState();
	}

	public int getQuantityDecimalPlaces() throws JposException {
		return fiscalPrinter.getQuantityDecimalPlaces();
	}

	public int getQuantityLength() throws JposException {
		return fiscalPrinter.getQuantityLength();
	}

	public boolean getRecEmpty() throws JposException {
		return fiscalPrinter.getRecEmpty();
	}

	public boolean getRecNearEnd() throws JposException {
		if (!getCapRecNearEndSensor())
			return false;
		return fiscalPrinter.getRecNearEnd();
	}

	public boolean getSlipNearEnd() throws JposException {
		return false;
	}

	public int getRemainingFiscalMemory() throws JposException {
		return fiscalPrinter.getRemainingFiscalMemory();
	}

	public String getReservedWord() throws JposException {
		return fiscalPrinter.getReservedWord();
	}

	public int getSlipSelection() throws JposException {
		return fiscalPrinter.getSlipSelection();
	}

	public boolean getSlpEmpty() throws JposException {
		return fiscalPrinter.getSlpEmpty();
	}

	public boolean getSlpNearEnd() throws JposException {
		return fiscalPrinter.getSlpNearEnd();
	}

	public void getTotalizer(int arg0, int arg1, String[] arg2) throws JposException {
		fiscalPrinter.getTotalizer(arg0, arg1, arg2);
	}

	public boolean getTrainingModeActive() throws JposException {
		return fiscalPrinter.getTrainingModeActive();
	}

	public void getVatEntry(int arg0, int arg1, int[] arg2) throws JposException {
		fiscalPrinter.getVatEntry(arg0, arg1, arg2);
	}

	public void printDuplicateReceipt() throws JposException {
		fiscalPrinter.printDuplicateReceipt();
	}

	public void printFiscalDocumentLine(String arg0) throws JposException {
		fiscalPrinter.printFiscalDocumentLine(arg0);
	}

	public void printFixedOutput(int arg0, int arg1, String arg2) throws JposException {
		fiscalPrinter.printFixedOutput(arg0, arg1, arg2);
	}

	public void printNormal(int arg0, String arg1) throws JposException {
		fiscalPrinter.printNormal(arg0, arg1);
	}

	public void printPeriodicTotalsReport(String arg0, String arg1) throws JposException {
		fiscalPrinter.printPeriodicTotalsReport(arg0, arg1);
		
	}

	public void printPowerLossReport() throws JposException {
		fiscalPrinter.printPowerLossReport();
	}

	public void printRecItem(String arg0, long arg1, int arg2, int arg3, long arg4, String arg5) throws JposException {
		fiscalPrinter.printRecItem(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	public void printRecItemAdjustment(int arg0, String arg1, long arg2, int arg3) throws JposException {
		fiscalPrinter.printRecItemAdjustment(arg0, arg1, arg2, arg3);
	}

	public void printRecMessage(String arg0) throws JposException {
		fiscalPrinter.printRecMessage(arg0);
	}

	public void printRecNotPaid(String arg0, long arg1) throws JposException {
		fiscalPrinter.printRecNotPaid(arg0, arg1);
	}

	public void printRecRefund(String arg0, long arg1, int arg2) throws JposException {
		fiscalPrinter.printRecRefund(arg0, arg1, arg2);
	}

	public void printRecSubtotal(long arg0) throws JposException {
		fiscalPrinter.printRecSubtotal(arg0);
	}

	public void printRecSubtotalAdjustment(int arg0, String arg1, long arg2) throws JposException {
		fiscalPrinter.printRecSubtotalAdjustment(arg0, arg1, arg2);
	}

	public void printRecTotal(long arg0, long arg1, String arg2) throws JposException {
		fiscalPrinter.printRecTotal(arg0, arg1, arg2);
	}

	public void printRecVoid(String arg0) throws JposException {
		fiscalPrinter.printRecVoid(arg0);
	}

	public void printRecVoidItem(String arg0, long arg1, int arg2, int arg3, long arg4, int arg5) throws JposException {
		fiscalPrinter.printRecVoidItem(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	public void printReport(int arg0, String arg1, String arg2) throws JposException {
		fiscalPrinter.printReport(arg0, arg1, arg2);
	}

	public void printXReport() throws JposException {
		fiscalPrinter.printXReport();
	}

	public void printZReport() throws JposException {
		fiscalPrinter.printZReport();
	}

	public void removeDirectIOListener(DirectIOListener arg0) {
		fiscalPrinter.removeDirectIOListener(arg0);
	}

	public void removeErrorListener(ErrorListener arg0) {
		fiscalPrinter.removeErrorListener(arg0);
	}

	public void removeOutputCompleteListener(OutputCompleteListener arg0) {
		fiscalPrinter.removeOutputCompleteListener(arg0);
	}

	public void removeStatusUpdateListener(StatusUpdateListener arg0) {
		fiscalPrinter.removeStatusUpdateListener(arg0);
	}

	public void resetPrinter() throws JposException {
		fiscalPrinter.resetPrinter();
	}

	public void setAsyncMode(boolean arg0) throws JposException {
		fiscalPrinter.setAsyncMode(arg0);
	}

	public void setCheckTotal(boolean arg0) throws JposException {
		fiscalPrinter.setCheckTotal(arg0);
	}

	public void setDate(String arg0) throws JposException {
		fiscalPrinter.setDate(arg0);
	}

	public void setDuplicateReceipt(boolean arg0) throws JposException {
		fiscalPrinter.setDuplicateReceipt(arg0);
	}

	public void setFlagWhenIdle(boolean arg0) throws JposException {
		fiscalPrinter.setFlagWhenIdle(arg0);
	}

	public void setHeaderLine(int arg0, String arg1, boolean arg2) throws JposException {
		fiscalPrinter.setHeaderLine(arg0, arg1, arg2);
	}

	public void setPOSID(String arg0, String arg1) throws JposException {
		fiscalPrinter.setPOSID(arg0, arg1);
	}

	public void setPowerNotify(int arg0) throws JposException {
		fiscalPrinter.setPowerNotify(arg0);
	}

	public void setSlipSelection(int arg0) throws JposException {
		fiscalPrinter.setSlipSelection(arg0);
	}

	public void setStoreFiscalID(String arg0) throws JposException {
		fiscalPrinter.setStoreFiscalID(arg0);
	}

	public void setTrailerLine(int arg0, String arg1, boolean arg2) throws JposException {
		fiscalPrinter.setTrailerLine(arg0, arg1, arg2);
	}

	public void setVatTable() throws JposException {
		fiscalPrinter.setVatTable();
	}

	public void setVatValue(int arg0, String arg1) throws JposException {
		fiscalPrinter.setVatValue(arg0, arg1);
	}

	public void verifyItem(String arg0, int arg1) throws JposException {
		fiscalPrinter.verifyItem(arg0, arg1);
	}

	public void checkHealth(int arg0) throws JposException {
		fiscalPrinter.checkHealth(arg0);
	}

	public void claim(int arg0) throws JposException {
		fiscalPrinter.claim(arg0);
	}

	public void close() throws JposException {
		fiscalPrinter.close();
	}

	public void directIO(int arg0, int[] arg1, Object arg2) throws JposException {
		fiscalPrinter.directIO(arg0, arg1, arg2);
	}

	public String getCheckHealthText() throws JposException {
		return fiscalPrinter.getCheckHealthText();
	}

	public boolean getClaimed() throws JposException {
		return fiscalPrinter.getClaimed();
	}

	public String getDeviceControlDescription() {
		return fiscalPrinter.getDeviceControlDescription();
	}

	public int getDeviceControlVersion() {
		return fiscalPrinter.getDeviceControlVersion();
	}

	public boolean getDeviceEnabled() throws JposException {
		return fiscalPrinter.getDeviceEnabled();
	}

	public String getDeviceServiceDescription() throws JposException {
		return fiscalPrinter.getDeviceServiceDescription();
	}

	public int getDeviceServiceVersion() throws JposException {
		return fiscalPrinter.getDeviceServiceVersion();
	}

	public boolean getFreezeEvents() throws JposException {
		return fiscalPrinter.getFreezeEvents();
	}

	public String getPhysicalDeviceDescription() /*throws JposException*/ {
		try {
			return fiscalPrinter.getPhysicalDeviceDescription();
		} catch (JposException e) {
			return "";
		}
	}

	public String getPhysicalDeviceName() /*throws JposException*/ {
		try {
			return fiscalPrinter.getPhysicalDeviceName();
		} catch (JposException e) {
			return "";
		}
	}

	public int getState() {
		return fiscalPrinter.getState();
	}

	public void open(String arg0) throws JposException {
		fiscalPrinter.open(arg0);
	}

	public void release() throws JposException {
		fiscalPrinter.release();
	}

	public void setDeviceEnabled(boolean arg0) throws JposException {
		fiscalPrinter.setDeviceEnabled(arg0);
	}

	public void setFreezeEvents(boolean arg0) throws JposException {
		fiscalPrinter.setFreezeEvents(arg0);
	}

	public int getAmountDecimalPlaces() throws JposException {
		return fiscalPrinter.getAmountDecimalPlaces();
	}
	
    /* printer commands - End
    *
    */
	
	private void SetLotteryMessages() {
	}

	private void ReadLotteryMessages() {
	}

	/* SmartTicket commands - Start
	 * 
	 */

	protected void SMTKsetServerUrl(String url) {
		if (isfwSMTKdisabled())
			return;
		
	}
	
	private String SMTKgetServerUrl() {
		if (isfwSMTKdisabled())
			return "";
		
		int ret = -1;
		String Url = "";
		String Index = SmartTicket.ERECEIPT_URL_SERVER_TYPE;
		
      	return Url;
	}
	
	public void SMTKsetReceiptType(int doctype, int validity) {
		if (isfwSMTKdisabled())
			return;
		
	}
	
	private int SMTKgetReceiptType() {
		if (isfwSMTKdisabled())
			return 0;
		
		String Op = "01";
		String DocType = "0";
		String Validity = "1";
		
      	return Integer.parseInt(DocType);
	}
	
	private int SMTKgetReceiptValidity() {
		if (isfwSMTKdisabled())
			return 1;
		
		String Op = "01";
		String DocType = "0";
		String Validity = "1";
		
      	return Integer.parseInt(Validity);
	}
	
	void SMTKsetCustomerID(int type, String customerid) {
		if (isfwSMTKdisabled())
			return;
		
	}
	
	private int SMTKgetCustomerType() {
		if (isfwSMTKdisabled())
			return 0;
		
		String Op = "01";
		String Type = "0";
		String CustomerId = "";
		
      	return Integer.parseInt(Type);
	}
	
	private String SMTKgetCustomerId() {
		if (isfwSMTKdisabled())
			return "";
		
		String Op = "01";
		String Type = "";
		String CustomerId = "";
		
      	return CustomerId;
	}
	
	public boolean SMTKgetStatusReceipt(int zrep, String recId, String date) {
		if (isfwSMTKdisabled())
			return false;
		
		boolean ret = false;
		
		return ret;
	}
	
	private void SMTKgetStatus(int zrep, String date) {
		if (isfwSMTKdisabled())
			return;
		
	}
	
	protected void SMTKStatus()
	{
		return;
	}
	
	public void SMTKsetReceiptParameters()
	{
		if ((SmartTicket.getCustomerType() == SmartTicket._Smart_Ticket_CustomerType) && (SmartTicket.getCustomerId().equalsIgnoreCase(SmartTicket._Smart_Ticket_CustomerId))) {
			if (CarteFidelity.getCartaFidelity() != null && CarteFidelity.getCartaFidelity().length() > 0) {
				// se non è stato specificato nessun customerid ma è stata passata la tessera fidelity allora usiamo questa
				SmartTicket.setCustomerType(SmartTicket.ERECEIPT_DEFAULT_CUSTOMER);
				SmartTicket.setCustomerId(CarteFidelity.getCartaFidelity());
			}
		}
	
		if (SRTPrinterExtension.isPRT()) {
			// qui setto i parametri per lo scontrino che sta per andare in stampa, secondo le impostazioni decise dall'interfaccia grafica
			// oppure con le impostazioni di default se non sono state specificate tramite l'interfaccia grafica
			SMTKsetReceiptType(SmartTicket.Smart_Ticket_ReceiptMode, SmartTicket.Smart_Ticket_Validity);
			SMTKsetCustomerID(SmartTicket.Smart_Ticket_CustomerType, SmartTicket.Smart_Ticket_CustomerId);
		}
		
		SmartTicket.SMTKbarcodes_reset();
	}
	
	/* SmartTicket commands - End
	 * 
	 */
	
	public class DirectIOListener implements jpos.events.DirectIOListener
	{
	    private boolean started = true;
	    private String buffer = "";
	    
		public void directIOOccurred(DirectIOEvent arg0) {
			 if ((arg0.getSource().toString() != null) &&
				 (arg0.getSource().toString().startsWith("jpos.service.FiscalPrinter"))) {
				 buffer+=(String)arg0.getObject().toString();
				 started = false;
			 }
		}
	}
	
	public void getData(int i, int ai[], String as[])
	{
		if ((i == FPRT_RT_POSTVOID_NUMBER) || (i == FPRT_RT_REFUND_NUMBER))
		{
			double ret = GetDailyData(i);
			as[0] = Sprint.f("%010d", ret*100);
			return;
		}
		
		System.out.println( "MAPOTO-getData in" );
		String[] aString = new String[100];
		aString[0] = null;
		while ( true )
		{
			try
			{
				getDataF( i,  ai,  aString);
				break;
			}
			catch ( JposException e )
			{
				break;
			}
		}
		as[0] = aString[0];
		System.out.println ( "MAPOTO-getData out" );
	}
	
    private void getDataF(int i, int ai[], String as[]) throws JposException
    {
    	if ( TicketErrorSupport.getGetDataBuffered() == false )
    	{
    		TicketErrorSupport.setDataReply(  getDataUnbufferedF ( i,ai,as ) );
    		return;
    	}
    	System.out.println("getDataF in - tipo data=" + i);

        if (i == jpos.FiscalPrinterConst.FPTR_GD_RECEIPT_NUMBER || i == jpos.FiscalPrinterConst.FPTR_GD_FISCAL_REC)
        {
        	if ((i == jpos.FiscalPrinterConst.FPTR_GD_RECEIPT_NUMBER) &&
        		(myRchFiscalNumber != null) && (myRchFiscalNumber.length() > 0))
            {
	            FiscalPrinterDataInformation.setNewDataAvailable(false);
	         	as[0] = myRchFiscalNumber;
	           	FiscalPrinterDataInformation.setDataInformation(jpos.FiscalPrinterConst.FPTR_GD_RECEIPT_NUMBER, "  " + as[0]);
	         	FiscalPrinterDataInformation.setDataInformation(jpos.FiscalPrinterConst.FPTR_GD_FISCAL_REC, "  " + as[0]);  	
	         	System.out.println("getData out Fiscale - data=" + as[0]+"=");
	            FiscalPrinterDataInformation.setNewDataAvailable(true);
            }
            else
            {
	            FiscalPrinterDataInformation.setNewDataAvailable(false);
	            this.xgetData(jpos.FiscalPrinterConst.FPTR_GD_FISCAL_REC, ai, as); 
	            System.out.println("getDataF out Fiscale - data=" + as[0]+"=");
	            int  tck = 0;
                while ( as[0].startsWith(" ")) 
              	  as[0] = as[0].substring(1);
                tck = Integer.parseInt( as[0] );
            	tck = tck + 1;
                if (tck == 0)
                	tck = tck + 1;				// compatibilità con driver epson 
	            String s = Integer.toString(tck + 10000);
	            as[0] = s.substring(1);
	
	           	FiscalPrinterDataInformation.setDataInformation(jpos.FiscalPrinterConst.FPTR_GD_RECEIPT_NUMBER, "  " + as[0]);
	         	FiscalPrinterDataInformation.setDataInformation(jpos.FiscalPrinterConst.FPTR_GD_FISCAL_REC, "  " + as[0]);  	
	         	System.out.println("getDataF out Fiscale - data=" + as[0]+"=");
	            FiscalPrinterDataInformation.setNewDataAvailable(true);
	            myRchFiscalNumber = as[0];
	        }
        } 
        else if (i == jpos.FiscalPrinterConst.FPTR_GD_PRINTER_ID) 
        {
            // ERRORE REPORTZ
            FiscalPrinterDataInformation.setNewDataAvailable(false);
            this.xgetData(jpos.FiscalPrinterConst.FPTR_GD_PRINTER_ID, ai, as);
            System.out.println("getDataF out Fiscale - data=" + as[0]+"=");
            String mf = null;
            while ( as[0].startsWith(" ")) 
          	as[0] = as[0].substring(1);
           	mf = as[0].replaceAll(" ", "");
            
            if (isRTModel()) {
            	if (SharedPrinterFields.RTPrinterId != null && SharedPrinterFields.RTPrinterId.isEmpty() == false) {
                    System.out.println("getDataF out Fiscale - SharedPrinterFields.RTPrinterId="+SharedPrinterFields.RTPrinterId+"=");
            		mf = SharedPrinterFields.RTPrinterId;
            	}
            }
            
            FiscalPrinterDataInformation.setDataInformation(i, mf);
            FiscalPrinterDataInformation.setNewDataAvailable(true);
            System.out.println("getDataF out - data=" + mf+"=");
        } 
        else 
        {
            FiscalPrinterDataInformation.setNewDataAvailable(false);
            this.xgetData(i, ai, as);
            System.out.println("getDataF out Fiscale - data=" + as[0]+"=");
            while ( as[0].startsWith(" ")) 
            	  as[0] = as[0].substring(1);
    		if ( i == jpos.FiscalPrinterConst.FPTR_GD_CURRENT_TOTAL || i == jpos.FiscalPrinterConst.FPTR_GD_DAILY_TOTAL )
    			as [0] = as[0]+"00";
            
        	if (i == jpos.FiscalPrinterConst.FPTR_GD_FIRMWARE){
        		String fw = as[0].substring(5);
        		as[0] = fw;
        	}
        	if (i == jpos.FiscalPrinterConst.FPTR_GD_GRAND_TOTAL){
        		String gt = "";
        		for (int j=0; j < as[0].length(); j++){
        			if ((as[0].charAt(j) != '.') && (as[0].charAt(j) != ','))
        				gt = gt + as[0].charAt(j);
        		}
        		as[0] = gt;
        	}
            
    		System.out.println("getDataF out - data=" + as[0]+"=");
            FiscalPrinterDataInformation.setDataInformation(i, as[0]);
            FiscalPrinterDataInformation.setNewDataAvailable(true);
            System.out.println("getDataF out - data=" + as[0]+"=");
        } 
    }
    
	private String getDataUnbufferedF(int i, int ai[], String as[]) throws JposException 
    {
        System.out.println("getDataUnbuffered in - tipo data=" + i);

        if (i == jpos.FiscalPrinterConst.FPTR_GD_RECEIPT_NUMBER  || i == jpos.FiscalPrinterConst.FPTR_GD_FISCAL_REC )
        {
            this.xgetData(jpos.FiscalPrinterConst.FPTR_GD_FISCAL_REC, ai, as);  
            System.out.println("getDataUmbuffered out Fiscale - data=" + as[0]+"=");
            int  tck = 0;
            while ( as[0].startsWith(" ")) 
          	  as[0] = as[0].substring(1);
            tck = Integer.parseInt( as[0] );
            if (tck == 0)
            	tck = tck + 1;				// compatibilità con driver epson 
            String s = Integer.toString(tck + 10000);
            as[0] = s.substring(1);
            return ( as[0] );
        } 
        else if (i == jpos.FiscalPrinterConst.FPTR_GD_PRINTER_ID)
        {
            // ERRORE REPORTZ
            this.xgetData(jpos.FiscalPrinterConst.FPTR_GD_PRINTER_ID, ai, as);
            while ( as[0].startsWith(" ")) 
          	  as[0] = as[0].substring(1);
            String mf = as[0];
            return ( mf );
        } 
        else 
        {
            this.xgetData(i, ai, as);
            while ( as[0].startsWith(" ")) 
            	  as[0] = as[0].substring(1);
    		System.out.println("getDataUmbuffered out - data=" + as[0]+"=");
            return ( as[0] );
        } 
    }
	
	private double GetDailyData(int i) {
		if (isNotRTModel())
			return 0;
		
		double ret = 0;
		
		if (i == FPRT_RT_POSTVOID_NUMBER) {
			ret = getDailyVoid();
		}
		else if (i == FPRT_RT_REFUND_NUMBER) {
			ret = getDailyRefund();
		}
		
		return ret;
	}
	
	private double getDailyVoid() {
        FiscalPrinterDataInformation.setNewDataAvailable(false);
        
		double ret = 0;
		
		StringBuffer key = new StringBuffer(SharedPrinterFields.KEY_X);
		this.executeRTDirectIo(0, 0, key);
		
		int command = 8001;
        int[] dt = new int[10]; 
        dt[0] = 10;
        String[] pString = {""};
        try {
        	directIO(command, dt, pString);
		} catch (JposException e) {
			System.out.println("getDailyVoid - Exception : " + e.getMessage());
			return ret;
		}
		StringTokenizer st = new StringTokenizer(pString[0], "!");
		String[] reply = new String[st.countTokens()];
		for (int i = 0; i < reply.length; i++) {
			reply[i] = st.nextToken();
			System.out.println("getDailyVoid - reply["+i+"] = "+reply[i]+" - "+reply[i].length());
		}
		for (int i=0; i < reply.length; i++) {
			if (reply[i].length() == 32) {
				String s = reply[i].substring(14, 24);
				System.out.println("s = "+s);
				double d = Double.parseDouble(s) / 100;
				ret = Math.rint((ret+d)*100)/100;
			}
		}
			
		key = new StringBuffer(SharedPrinterFields.KEY_REG);
		this.executeRTDirectIo(0, 0, key);
		
       	FiscalPrinterDataInformation.setDataInformation(FPRT_RT_POSTVOID_NUMBER, ""+((int)(ret*10000)));
        FiscalPrinterDataInformation.setNewDataAvailable(true);
        
		return ret;
	}
	
	private double getDailyRefund() {
        FiscalPrinterDataInformation.setNewDataAvailable(false);
        
		double ret = 0;
		
		StringBuffer key = new StringBuffer(SharedPrinterFields.KEY_X);
		this.executeRTDirectIo(0, 0, key);
		
		int command = 8001;
        int[] dt = new int[10]; 
        dt[0] = 20;
        String[] pString = {""};
        try {
        	directIO(command, dt, pString);
		} catch (JposException e) {
			System.out.println("getDailyRefund - Exception : " + e.getMessage());
			return ret;
		}
		StringTokenizer st = new StringTokenizer(pString[0], "!");
		String[] reply = new String[st.countTokens()];
		for (int i = 0; i < reply.length; i++) {
			reply[i] = st.nextToken();
			System.out.println("getDailyRefund - reply["+i+"] = "+reply[i]+" - "+reply[i].length());
		}
		ret = 0;
		for (int i=0; i < reply.length; i++) {
			if (reply[i].length() == 32) {
				String s = reply[i].substring(14, 24);
				System.out.println("s = "+s);
				double d = Double.parseDouble(s) / 100;
				ret = Math.rint((ret+d)*100)/100;
			}
		}
			
		key = new StringBuffer(SharedPrinterFields.KEY_REG);
		this.executeRTDirectIo(0, 0, key);
		
       	FiscalPrinterDataInformation.setDataInformation(FPRT_RT_REFUND_NUMBER, ""+((int)(ret*10000)));
        FiscalPrinterDataInformation.setNewDataAvailable(true);
        
		return ret;
	}
	
	private void xgetData(int i, int ai[], String as[]) throws JposException
	{
		int retry = 5;
		
		while (true) {
			int errcode = 0;
			retry--;
			try {
				fiscalPrinter.getData(i, ai, as);
			}
			catch ( JposException e) {
				errcode = e.getErrorCode();
				System.out.println("200726 - as[0] = "+as[0]+" - retry = "+retry);
			}
			if (as[0] != null || retry == 0) {
				if (as[0] == null && retry == 0) {
					String code = Sprint.f("%02d", i);
					OperatorDisplay.setText_error("GETDATA ERROR     "+code);
					OperatorDisplay.pleaseDisplay (R3define.PRINTERVERIFY);
			    	throw new JposException(errcode);
				}
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
	}
	
    private static String[] split(String src, int len) {
        String[] result = new String[(int)Math.ceil((double)src.length()/(double)len)];
        for (int i=0; i<result.length; i++) {
            result[i] = src.substring(i*len, Math.min(src.length(), (i+1)*len))+R3define.Lf;
        }
        return result;
    }
    
    public ArrayList<String> getReceiptFromEj()
    {
    	ArrayList<String> ret = new ArrayList<String>();
		String[] ticket = null;
		
        int cmdInt = 0;
        int[] mydata = {0};
        String cmd = SharedPrinterFields.KEY_Z;
        try {
        	directIO(cmdInt, mydata, cmd);
		} catch (JposException e) {
			System.out.println("getReceiptFromEj - Exception : " + e.getMessage());
		}
        
	   DirectIOListener p=new DirectIOListener();
	   fiscalPrinter.addDirectIOListener((jpos.events.DirectIOListener) p);
		   
	   cmd = "=C453/$0";
	   try {
			p.started = true;
			p.buffer="";
			directIO(cmdInt, mydata, cmd);
			while (p.started) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
			
			ticket = split(p.buffer, 48);
			
		} catch (JposException e) {
			System.out.println("getReceiptFromEj - Exception : " + e.getMessage());
		}
        
        fiscalPrinter.removeDirectIOListener(p);
  	   
        cmd = SharedPrinterFields.KEY_REG;
        try {
        	directIO(cmdInt, mydata, cmd);
		} catch (JposException e) {
			System.out.println("getReceiptFromEj - Exception : " + e.getMessage());
		}
        
        String s = "";
		for (int i=0; i<ticket.length; i++) {
			if ((i<=9) || (i>=ticket.length-4))
				continue;
			s = String13Fix.replaceAll(ticket[i], "ﾀ", SharedPrinterFields.euro);
			ret.add(s);
		}
		return ret;
    }
    
	boolean checkCurrentTicketTotal ( String Pr, long In ) {
		return PrinterCommands.checkCurrentTicketTotal(Pr, In);
	}
	
	static boolean checkCurrentDailyTotal ( String In ) {
		return PrinterCommands.checkCurrentDailyTotal(In);
	}
	
	static boolean checkCurrentDailyTotalRounded ( String In, double rounding ) {
		return PrinterCommands.checkCurrentDailyTotalRounded(In, rounding);
	}
	
	   void reprintLastTicket()
	   {
		   OperatorDisplay.pleaseDisplay("Attendere Prego...");
		   
		   try {
			   fiscalPrinter.printDuplicateReceipt();
		   } catch (JposException e) {
			   System.out.println("Reprinting ticket: "+e.getMessage());
			   MessageBox.showMessage(e.getMessage(), null, MessageBox.OK);
		   }
	   }

	   void fwUpdate()
	   {
		   int[] ai = new int[1];
		   String[] as = new String[1];
	        
		   int[] opt = new int[1];
		   String[] printerId = new String[1];
		   try {
			   fiscalPrinter.getData(FiscalPrinterConst.FPTR_GD_PRINTER_ID, opt, printerId);
		   } catch (JposException e) {
			   System.out.println("fwUpdate - Exception: "+e.getMessage());
			   MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
			   return;
		   }
		   int matricola = Integer.parseInt(printerId[0].substring(printerId[0].length()-8));
		   if (matricola < 72018101) {
			   System.out.println("fwUpdate - matricola: "+matricola);
			   MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
			   return;
		   }
	    	
		   try {
			   if (fiscalPrinter.getDayOpened()) {
				   System.out.println("fwUpdate - Fiscal day opened: don't do it.");
				   MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
				   return;
			   }
		   } catch (JposException e) {
			   System.out.println("fwUpdate - Exception: "+e.getMessage());
			   MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
			   return;
		   }
			
		   int ret = RTstilltosend();
		   if (ret > 100) {
			   System.out.println("fwUpdate - some files still to send("+ret+")");
			   RTtrytosend();
			   ret = RTstilltosend();
			   if (ret > 100) {
				   System.out.println("fwUpdate - some files still to send("+ret+")");
				   MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
				   return;
			   }
		   }
	    	
		   try {
			   fiscalPrinter.getData(jpos.FiscalPrinterConst.FPTR_GD_FIRMWARE, ai, as);
		   } catch (JposException e) {
			   System.out.println("fwUpdate - Exception: "+e.getMessage());
			   MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
			   return;
		   }
		   System.out.println ("RT2 - fwUpdate - printer FW : "+as[0]);
            
		   StringBuffer key = new StringBuffer(SharedPrinterFields.KEY_SRV);
		   executeRTDirectIo(0, 0, key);
			
		   String cmd = "=C901";
		   StringBuffer sbcmd = new StringBuffer(cmd);
		   System.out.println("RT2 - fwUpdate - sbcmd = "+sbcmd.toString());
		   executeRTDirectIo(0, 0, sbcmd);
		   System.out.println("RT2 - fwUpdate - sbcmd = "+sbcmd.toString());
			
		   key = new StringBuffer(SharedPrinterFields.KEY_REG);
		   executeRTDirectIo(0, 0, key);
	   }

	   String checkEJStatus()
	   {
		   String reply = "";
			   
		   int cmdInt = 0;
		   int[] mydata = {0};
		   String cmd = "<</?g";
		        
		   DirectIOListener p=new DirectIOListener();
		   fiscalPrinter.addDirectIOListener((jpos.events.DirectIOListener) p);
			   
		   try {
			   p.started = true;
			   p.buffer="";
					
			   fiscalPrinter.directIO(cmdInt, mydata, cmd);
			   while (p.started) {
				   try {
					   Thread.sleep(500);
				   } catch (InterruptedException e) {
				   }
			   }
					
			   System.out.println("checkEJStatus - result : "+p.buffer);
			   reply = p.buffer;
					
		   } catch (JposException e) {
			   System.out.println("checkEJStatus - Exception : " + e.getMessage());
		   }
		        
		   fiscalPrinter.removeDirectIOListener(p);
			   
		   return reply;
	   }

	   public void bitmap(String filename,int width,int height,int align) //throws PrinterException
	   {
	   }
	   
	   int getVATTableEntry()
	   {
		   int ret = 1;
	    	
		   int cmdInt = 0;
		   int[] mydata = {0};
		   String cmd = SharedPrinterFields.KEY_X;
		   try {
			   fiscalPrinter.directIO(cmdInt, mydata, cmd);
		   } catch (JposException e) {
			   System.out.println("getVATTableEntry - getVATTableEntry_withEvents - Exception : " + e.getMessage());
		   }
	        
		   DirectIOListener p=new DirectIOListener();
		   fiscalPrinter.addDirectIOListener((jpos.events.DirectIOListener) p);
			   
		   cmd = "<</?C/&3";
		   try {
			   p.started = true;
			   p.buffer="";
			   fiscalPrinter.directIO(cmdInt, mydata, cmd);
			   while (p.started) {
				   try {
					   Thread.sleep(500);
				   } catch (InterruptedException e) {
				   }
			   }
			   System.out.println("getVATTableEntry - buffer = "+p.buffer);
			   //VNNNPPPP000000000000000000000000000000000000AB
			   while (p.buffer.length() >= 46)
			   {
				   String s = p.buffer.substring(0, 46);
				   System.out.println("getVATTableEntry - s = "+s);
				   String aliquota = s.substring(1, 4);
				   String rate = s.substring(4, 8);
				   int type = Integer.parseInt(s.substring(8, 9));
				   if (((type == 0) && (Integer.parseInt(rate) < 100) && (Integer.parseInt(rate) > 0)) ||
					   ((type == 2) && (Integer.parseInt(rate) == 0))) 
				   {
					   ret = Integer.parseInt(aliquota);
					   break;
				   }
				   p.buffer = p.buffer.substring(46);
			   }
		   } catch (JposException e) {
			   System.out.println("getVATTableEntry - getVATTableEntry_withEvents - Exception : " + e.getMessage());
		   }
	        
		   cmd = SharedPrinterFields.KEY_REG;
		   try {
			   fiscalPrinter.directIO(cmdInt, mydata, cmd);
		   } catch (JposException e) {
			   System.out.println("getVATTableEntry - getVATTableEntry_withEvents - Exception : " + e.getMessage());
		   }
	        
		   System.out.println("getVATTableEntry - ret = "+ret);
		   return ret;
	   }

	   public int getLotteryRec(String till, String date, int repz) {
		   int ret = 0;
			
		   String buffer = "";
				
		   int cmdInt = 0;
		   int[] mydata = {0};
		   String cmd = SharedPrinterFields.KEY_Z;
		   try {
			   fiscalPrinter.directIO(cmdInt, mydata, cmd);
		   } catch (JposException e) {
			   System.out.println("getLotteryRec - Exception : " + e.getMessage());
		   }
		        
		   DirectIOListener p=new DirectIOListener();
		   fiscalPrinter.addDirectIOListener((jpos.events.DirectIOListener) p);

		   cmd = "=C488/&"+date+"/*0";
		   try {
			   p.started = true;
			   p.buffer="";
			   fiscalPrinter.directIO(cmdInt, mydata, cmd);
			   while (p.started) {
				   try {
					   Thread.sleep(500);
				   } catch (InterruptedException e) {
				   }
			   }
		    	   
		   } catch (JposException e) {
			   System.out.println("getLotteryRec - Exception : " + e.getMessage());
		   }
		       
		   buffer = p.buffer;
		        
		   fiscalPrinter.removeDirectIOListener(p);
		   	   
		   cmd = SharedPrinterFields.KEY_REG;
		   try {
			   fiscalPrinter.directIO(cmdInt, mydata, cmd);
		   } catch (JposException e) {
			   System.out.println("getLotteryRec - Exception : " + e.getMessage());
		   }
		        
		   //0214-0003 00000 AC, 0214-0004 00000 AC, 0215-0001 00000 AC01010NON000000003E4
		   StringTokenizer st = new StringTokenizer(buffer, ",");
		   String[] tmp = new String[st.countTokens()];
		   for (int i = 0; i < tmp.length; i++) {
			   tmp[i] = st.nextToken().trim();
			   if (Integer.parseInt(tmp[i].substring(0, 4)) == repz)	// sommo accettati + rifiutati + archiviati
				   ret++;
		   }
			
		   return ret;
	   }

	   protected int getSimulation()
	   {
		   int ret = 0;
		   
		   DirectIOListener p=new DirectIOListener();
		   fiscalPrinter.addDirectIOListener((jpos.events.DirectIOListener) p);
		   
		   try {
			   int cmdInt = 0;
			   int[] mydata = {0};
			   String cmd = "<</?i/*7";
		        
			   p.started = true;
			   p.buffer="";
				
			   fiscalPrinter.directIO(cmdInt, mydata, cmd);
			   while (p.started) {
				   try {
					   Thread.sleep(500);
				   } catch (InterruptedException e) {
				   }
			   }
				
			   System.out.println("getSimulation - State active = "+p.buffer);
			   
			   int B = Integer.parseInt(p.buffer.substring(1, 2));
			   ret = B;
			   
		   } catch (Exception e) {
			   System.out.println("getSimulation - Exception : " + e.getMessage());
			   return ret;
		   } finally {
			   fiscalPrinter.removeDirectIOListener(p);
		   }
		   
		   return ret;
	    }

		String[] DailyPeriodicReport(int reporttype, int type)
		{
			String reply[] = new String[0];
			
			int cmdInt = 0;
	        int[] mydata = {0};
	        String cmd = SharedPrinterFields.KEY_X;
	        try {
	        	fiscalPrinter.directIO(cmdInt, mydata, cmd);
			} catch (JposException e) {
				System.out.println("DailyPeriodicReport - Exception : " + e.getMessage());
			}
	        
	        DirectIOListener p=new DirectIOListener();
	        fiscalPrinter.addDirectIOListener((jpos.events.DirectIOListener) p);
		   
	        cmd = "=C508/*"+type;		// daily report
	        if (reporttype == 1)		
	        	cmd = "=C518/*"+type;	// periodical report
		   
	        try {
	        	p.started = true;
				p.buffer="";
				fiscalPrinter.directIO(cmdInt, mydata, cmd);
				while (p.started) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
				}
		      	System.out.println("DailyPeriodicReport - buffer = "+p.buffer);
		      	
				StringTokenizer st = new StringTokenizer(p.buffer, " !");		// spero che sia la regola il blank oppure il ! separatore
				String[] ret = new String[st.countTokens()];
				for (int i = 0; i < ret.length; i++) {
					ret[i] = st.nextToken();
				}
				
				int i=0;
				for (i=0; i < ret.length; i++) {
					if (ret[i].length() < 32)
						break;
				}
				reply = new String[i];
				for (int j=0; j < i; j++) {
					reply[j] = ret[j];
				}
	        } catch (JposException e) {
	        	System.out.println("DailyPeriodicReport - Exception : " + e.getMessage());
	        }
		   
	        fiscalPrinter.removeDirectIOListener(p);
	 	   
	        cmd = SharedPrinterFields.KEY_REG;
	        try {
	        	fiscalPrinter.directIO(cmdInt, mydata, cmd);
	        } catch (JposException e) {
	        	System.out.println("DailyPeriodicReport - Exception : " + e.getMessage());
	        }
			
			return reply;
		}

		String[] SingleDocumentReport(int type, int zrepnum, String docnum, String date)
		{
			String reply[] = new String[0];
			
			int cmdInt = 0;
	        int[] mydata = {0};
	        String cmd = SharedPrinterFields.KEY_X;
	        try {
	        	fiscalPrinter.directIO(cmdInt, mydata, cmd);
			} catch (JposException e) {
				System.out.println("SingleDocumentReport - Exception : " + e.getMessage());
			}
	        
	        DirectIOListener p=new DirectIOListener();
	        fiscalPrinter.addDirectIOListener((jpos.events.DirectIOListener) p);
		   
	        cmd = "=C528/&"+date.substring(0, 4)+date.substring(6, 8)+"/["+zrepnum+"/]"+docnum+"/*"+type;
		   
	        try {
	        	p.started = true;
				p.buffer="";
				fiscalPrinter.directIO(cmdInt, mydata, cmd);
				while (p.started) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
				}
		      	System.out.println("SingleDocumentReport - buffer = "+p.buffer);
		      	
				StringTokenizer st = new StringTokenizer(p.buffer, " !");		// spero che sia la regola il blank oppure il ! separatore
				String[] ret = new String[st.countTokens()];
				for (int i = 0; i < ret.length; i++) {
					ret[i] = st.nextToken();
				}
				
				int i=0;
				for (i=0; i < ret.length; i++) {
					if (ret[i].length() < 32)
						break;
				}
				reply = new String[i];
				for (int j=0; j < i; j++) {
					reply[j] = ret[j];
				}
	        } catch (JposException e) {
	        	System.out.println("SingleDocumentReport - Exception : " + e.getMessage());
	        }
		   
	        fiscalPrinter.removeDirectIOListener(p);
	 	   
	        cmd = SharedPrinterFields.KEY_REG;
	        try {
	        	fiscalPrinter.directIO(cmdInt, mydata, cmd);
	        } catch (JposException e) {
	        	System.out.println("SingleDocumentReport - Exception : " + e.getMessage());
	        }
			
			return reply;
		}

}
