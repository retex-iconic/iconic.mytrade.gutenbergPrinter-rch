package iconic.mytrade.gutenbergPrinter;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import iconic.mytrade.gutenberg.jpos.linedisplay.service.MessageBox;
import iconic.mytrade.gutenberg.jpos.linedisplay.service.OperatorDisplay;
import iconic.mytrade.gutenberg.jpos.printer.service.CanPost;
import iconic.mytrade.gutenberg.jpos.printer.service.Cancello;
import iconic.mytrade.gutenberg.jpos.printer.service.Company;
import iconic.mytrade.gutenberg.jpos.printer.service.EjTokens;
import iconic.mytrade.gutenberg.jpos.printer.service.Extra;
import iconic.mytrade.gutenberg.jpos.printer.service.LastTicket;
import iconic.mytrade.gutenberg.jpos.printer.service.MethodE;
import iconic.mytrade.gutenberg.jpos.printer.service.PosApp;
import iconic.mytrade.gutenberg.jpos.printer.service.R3define;
import iconic.mytrade.gutenberg.jpos.printer.service.RTLottery;
import iconic.mytrade.gutenberg.jpos.printer.service.RTRounding;
import iconic.mytrade.gutenberg.jpos.printer.service.RTTxnType;
import iconic.mytrade.gutenberg.jpos.printer.service.SetVoidTrx;
import iconic.mytrade.gutenberg.jpos.printer.service.SmartTicket;
import iconic.mytrade.gutenberg.jpos.printer.service.TakeYourTime;
import iconic.mytrade.gutenberg.jpos.printer.service.TicketErrorSupport;
import iconic.mytrade.gutenberg.jpos.printer.service.TransactionSale;
import iconic.mytrade.gutenberg.jpos.printer.service.TxnHeader;
import iconic.mytrade.gutenberg.jpos.printer.service.properties.Lotteria;
import iconic.mytrade.gutenberg.jpos.printer.service.properties.MyTradeProperties;
import iconic.mytrade.gutenberg.jpos.printer.service.properties.PaperSavingProperties;
import iconic.mytrade.gutenberg.jpos.printer.service.properties.PrinterType;
import iconic.mytrade.gutenberg.jpos.printer.service.properties.SRTPrinterExtension;
import iconic.mytrade.gutenberg.jpos.printer.service.properties.SmartTicketProperties;
import iconic.mytrade.gutenberg.jpos.printer.service.properties.UI;
import iconic.mytrade.gutenberg.jpos.printer.service.properties.XRData;
import iconic.mytrade.gutenberg.jpos.printer.service.tax.RoungickTax;
import iconic.mytrade.gutenberg.jpos.printer.service.tax.VatInOutHandling;
import iconic.mytrade.gutenberg.jpos.printer.srt.DummyServerRT;
import iconic.mytrade.gutenberg.jpos.printer.srt.RTConsts;
import iconic.mytrade.gutenberg.jpos.printer.srt.Xml4SRT;
import iconic.mytrade.gutenberg.jpos.printer.utils.BasicDicoData;
import iconic.mytrade.gutenberg.jpos.printer.utils.Checksum;
import iconic.mytrade.gutenberg.jpos.printer.utils.Files;
import iconic.mytrade.gutenberg.jpos.printer.utils.LocalTimestamp;
import iconic.mytrade.gutenberg.jpos.printer.utils.Rounding;
import iconic.mytrade.gutenberg.jpos.printer.utils.RunShellScriptPoli20;
import iconic.mytrade.gutenberg.jpos.printer.utils.SRTCheckInput;
import iconic.mytrade.gutenberg.jpos.printer.utils.Sprint;
import iconic.mytrade.gutenberg.jpos.printer.utils.String13Fix;
import iconic.mytrade.gutenbergPrinter.eftpos.EftPos;
import iconic.mytrade.gutenbergPrinter.ej.EjCommands;
import iconic.mytrade.gutenbergPrinter.ej.ForFiscalEJFile;
import iconic.mytrade.gutenbergPrinter.lottery.LotteryCommands;
import iconic.mytrade.gutenbergPrinter.monitorrt.MonitorRT;
import iconic.mytrade.gutenbergPrinter.mop.LoadMops;
import iconic.mytrade.gutenbergPrinter.refund.RefundCommands;
import iconic.mytrade.gutenbergPrinter.report.Report;
import iconic.mytrade.gutenbergPrinter.rtvoid.VoidCommands;
import iconic.mytrade.gutenbergPrinter.smtk.SMTKCommands;
import iconic.mytrade.gutenbergPrinter.tax.DicoTaxLoad;
import iconic.mytrade.gutenbergPrinter.tax.DicoTaxToPrinter;
import iconic.mytrade.gutenbergPrinter.tax.TaxData;
import jpos.FiscalPrinterConst;
import jpos.JposException;
import rtsTrxBuilder.hardTotals.HardTotals;
import rtsTrxBuilder.support.ivaSEMPLICE;
import rtsTrxBuilder.support.scontiSEMPLICI;

public class PrinterCommands extends iconic.mytrade.gutenbergInterface.PrinterCommands {

	private final static int MyPrinterType = PrinterType.RCHPRINTF_F;		// oppure leggerlo dalla versione nel pom ?
	
	private static boolean versioned = false;
	
	public PrinterCommands() {
		Versioning();
	}

	private static void Versioning() {
		if (versioned)
			return;
		versioned = true;
		
        Properties properties = new Properties();
        InputStream input = null;
        try {
            input = PrinterCommands.class.getClassLoader().getResourceAsStream("gutenbergPrinter.properties");
            if (input == null) {
                System.out.println("Sorry, unable to find gutenbergPrinter.properties");
            }
            else {
	            properties.load(input);
	            System.out.println("\n---------------------------------------------------------------------");
	            System.out.println("GutenbergPrinter Version  : " + properties.getProperty("PrinterVersion"));
	            System.out.println("GutenbergPrinter Type     : " + properties.getProperty("PrinterType"));
	            System.out.println("GutenbergInterface Version: " + properties.getProperty("InterfaceVersion"));
	            System.out.println("GutenbergCommon Version   : " + properties.getProperty("CommonVersion"));
	            System.out.println("---------------------------------------------------------------------\n");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}
	
	private static double fw = 0;
	
	static int staticMsgLen = 0;
	
	private static final String	LOGO_FOLDER	=	"/image/header/";
	static final String	LOGO_FILE			=	"MyLogo.bmp";
	static final String	TRAILER_LOGO_FILE	=	"MyTrailerLogo.bmp";
	static final int	LOGO_NUMBER			=	1;
	static final int	TRAILER_LOGO_NUMBER	=	2;
	
	protected static final String ALINER = "                                         ";
	
	private static final String	R3_DELETELASTTICKET_R3 = "R3_NEWCMD01_R3";
	
	private String CF = "C.F. Cliente ";
	private String PI = "P.IVA. Cliente ";
	private int CFLEN = 16;
	private int PILEN = 11;
    
	private boolean flagVoidRefund = false;
	
	private boolean isFlagVoidRefund(long amt, long adj) 
	{
		return ((flagVoidRefund) && (amt == 0) && (adj > 0));
	}
	
	private void setFlagVoidRefund(boolean flagVoidRefund) 
	{
		this.flagVoidRefund = flagVoidRefund;
	}
	
	private static boolean flagsVoidTicket = false;
	
	private static boolean isFlagsVoidTicket() 
	{
		return flagsVoidTicket;
	}
	
	private static void setFlagsVoidTicket(boolean flagsvoidticket) 
	{
		flagsVoidTicket = flagsvoidticket;
	}
	
	private static String lastticketsaved = null;
    private static String lastwrittenstring = "";
	
	private boolean	inReset;
	
	static BigDecimal discountedtaxamount = new BigDecimal(0.);
	
    ArrayList SSCO = null;
	private static double dailyTotal = 0.0;
	private	static double currentTicket = 0.0;
    private static boolean provaErrori = false;		// attenzione versione di test per prova errori
	private static int countErrori = 1;
	private static long totaleNonRiscosso = 0;
	
	private static boolean progress_cash = true;
	private static long progress_amount = 0;
	private static int enabledLowerRoundedPay = -1;
	
	private boolean AtLeastOnePrintedItem = false;
	
	protected static double smtkamount = 0.;

	private ArrayList xml = null;
	
	public static MonitorRT monitorRT = null;
	
	protected static GuiFiscalPrinterDriver fiscalPrinterDriver = null;
	
	private static File inout = null;		
	private static FileOutputStream fos = null;
	private static PrintStream ps = null;
	
	public boolean GetFwRT2enabled() {
		return (fiscalPrinterDriver.isfwRT2enabled());
	}
	
	public static boolean isRT2On() {
		if (SRTPrinterExtension.isPRT()) {
			return (fiscalPrinterDriver.isfwRT2enabled() && DicoTaxLoad.isRT2enabled());
		}
		else if (SRTPrinterExtension.isSRT()){
			return (Extra.isServerRt20() && DicoTaxLoad.isRT2enabled());
		}
		return (false);
	}
	
	public static boolean isFwRT2On() {
		if (SRTPrinterExtension.isPRT()) {
			return (fiscalPrinterDriver.isfwRT2enabled());
		}
		else if (SRTPrinterExtension.isSRT()){
			return (Extra.isServerRt20() && DicoTaxLoad.isRT2enabled());
		}
		return (false);
	}
	
	private boolean isFiscalAndSRTModel()
	{
		return (SRTPrinterExtension.isSRT() && SRTPrinterExtension.isNotLikeNonFiscalMode());
	}

	private boolean isLNFMAndSRTModel()
	{
		return (SRTPrinterExtension.isSRT() && SRTPrinterExtension.isLikeNonFiscalMode());
	}

	private boolean isNotRTActivated()
	{
		return (!SRTPrinterExtension.isRTActivated());
	}

	public int getOpenTimeout() {
		int timeout = fiscalPrinterDriver.getOpenTimeout();
		System.out.println("getOpenTimeout: "+timeout+" msec.");
		return timeout;
	}
	
    /* printer commands - Start
     * 
	private void open(int model,String device);
	private void beginFiscalReceipt(boolean printHeader) throws JposException;
	private void beginNonFiscal() throws JposException;
	private void endFiscalReceipt(boolean printHeader) throws JposException;
	private void endNonFiscal() throws JposException;
	private void printNormal(int station, String data) throws JposException;
	private void printRecItem(String description, long price, int quantity, int vatInfo, long unitPrice, String unitName) throws JposException;
	private void printRecItemAdjustment(int adjustmentType, String description, long amount, int vatInfo) throws JposException;
	private void printRecMessage(String message) throws JposException;
	private void printRecRefund(String description, long amount, int vatInfo) throws JposException;
	private void printRecSubtotal(long amount) throws JposException;
	private void printRecSubtotalAdjustment(int adjustmentType, String description, long amount) throws JposException;
	private void printRecTotal(long total, long payment, String description) throws JposException;
	private void printRecVoid(String description) throws JposException;
	private void printRecVoidItem(String description, long amount, int quantity, int adjustmentType, long adjustment, int vatInfo) throws JposException;
	private void printReport(int reportType, String startNum, String endNum) throws JposException;
	private void printXReport() throws JposException;
	private void printZReport() throws JposException;
	private void resetPrinter() throws JposException;
	private void setDate(String date) throws JposException;
	private void setHeaderLine(int lineNumber, String text, boolean doubleWidth) throws JposException;
	private void setTrailerLine(int lineNumber, String text, boolean doubleWidth) throws JposException;
	private void close();
    public void printPeriodicTotalsReport(String date1, String date2) throws JposException;
    public void printPowerLossReport() throws JposException;
    public void printRecNotPaid(String description, long amount) throws JposException;
    public void setAdditionalTrailer(String text) throws JposException;
    public void setPOSID(String POSID, String cashierID) throws JposException;
    public void setStoreFiscalID(String ID) throws JposException;
    public boolean getCapAdditionalLines() throws JposException;
    public boolean getCapAdditionalTrailer() throws JposException;
    public boolean getCapNonFiscalMode() throws JposException;
    public boolean getCapPowerLossReport() throws JposException;
    public boolean getCapSetHeader() throws JposException;
    public boolean getCapSetPOSID() throws JposException;
    public boolean getCapSetStoreFiscalID() throws JposException;
    public boolean getCapSetTrailer() throws JposException;
    public boolean getCapSubtotal() throws JposException;
    public boolean getCapTrainingMode() throws JposException;
    public boolean getCapXReport() throws JposException;
	*
	*/
    
    public static void printPeriodicTotalsReport(String date1, String date2) throws JposException {
    	fiscalPrinterDriver.printPeriodicTotalsReport(date1, date2);
    }
    
    public void printPowerLossReport() throws JposException {
    	fiscalPrinterDriver.printPowerLossReport();
    }
    
    public void printRecNotPaid(String description, long amount) throws JposException {
    	fiscalPrinterDriver.printRecNotPaid(description, amount);
    }
    
	public void setAdditionalHeader(String value)
	{
		try
		{
			fiscalPrinterDriver.setAdditionalHeader(value);
		}
		catch (Exception e)
		{
		}
	}
	   
    public void setAdditionalTrailer(String text) throws JposException
    {
    	if (fiscalPrinterDriver.getDayOpened())
    		System.out.println("setAdditionalTrailer - Fiscal day opened: don't do it.");
    	else
    		SetLogo(TRAILER_LOGO_FILE, TRAILER_LOGO_NUMBER);
			   
    	try
    	{
    		fiscalPrinterDriver.setAdditionalTrailer(text);
    	}
    	catch ( JposException nsme )
    	{
    		if ( nsme.getErrorCode() == 104 && nsme.getErrorCodeExtended() == 0 )
    		{
    			System.out.println ( "Versione di JavaPOS non supporta il metodo");
    		}
    		else
    		{
    			throw nsme;
    		}
    	}
    }
	   
    public void setPOSID(String POSID, String cashierID) throws JposException {
    	fiscalPrinterDriver.setPOSID(POSID, cashierID);
    }
    
    public void setStoreFiscalID(String ID) throws JposException {
    	fiscalPrinterDriver.setStoreFiscalID(ID);
    }
    
    public boolean getCapAdditionalLines() throws jpos.JposException
    {
    	return (fiscalPrinterDriver.getCapAdditionalLines());
    }
    
    public boolean getCapAdditionalTrailer() throws JposException {
    	return (fiscalPrinterDriver.getCapAdditionalTrailer());
    }
    
    public boolean getCapNonFiscalMode() throws JposException {
    	return (fiscalPrinterDriver.getCapNonFiscalMode());
    }
    
    public boolean getCapPowerLossReport() throws JposException {
    	return (fiscalPrinterDriver.getCapPowerLossReport());
    }
    
    public boolean getCapSetHeader() throws JposException {
    	return (fiscalPrinterDriver.getCapSetHeader());
    }
    
    public boolean getCapSetPOSID() throws JposException {
    	return (fiscalPrinterDriver.getCapSetPOSID());
    }
    
    public boolean getCapSetStoreFiscalID() throws JposException {
    	return (fiscalPrinterDriver.getCapSetStoreFiscalID());
    }
    
    public boolean getCapSetTrailer() throws JposException {
    	return (fiscalPrinterDriver.getCapSetTrailer());
    }
    
    public boolean getCapSubtotal() throws JposException {
    	return (fiscalPrinterDriver.getCapSubtotal());
    }
    
    public boolean getCapTrainingMode() throws JposException {
    	return (fiscalPrinterDriver.getCapTrainingMode());
    }
    
    public boolean getCapXReport() throws JposException {
    	return (fiscalPrinterDriver.getCapXReport());
    }
    
	public void open(int arg0, String arg1) {
		if (arg0 != MyPrinterType) {
			System.out.println("Wrong PrinterType Request : "+arg0+" instead of "+MyPrinterType);
			System.exit(-1);
		}
		
		SharedPrinterFields.resetInTicket();
		SharedPrinterFields.resetprtDone();
		RTConsts.setMAXITEMDESCRLENGTH(false, SRTPrinterExtension.isSRT(), SRTPrinterExtension.isNotLikeNonFiscalMode(), false);
		
		try
		{
			SharedPrinterFields.Lotteria = new RTLottery();
		}
		catch ( Exception e )
		{
			System.out.println ( "Errore costruttore : "+e.getMessage() );
		}
			
		DummyServerRT.XMLfilePath = rtsTrxBuilder.storerecallticket.Default.getExchangeRtsName();
		LastTicket.setLastticket(rtsTrxBuilder.storerecallticket.Default.getExchangeName());
		lastticketsaved = rtsTrxBuilder.storerecallticket.Default.getsourcePath()+"LastTicket.sav";
		
		fiscalPrinterDriver = new GuiFiscalPrinterDriver();
		fw = fiscalPrinterDriver.doLoad(arg0, arg1);
	}
	
	public void close() {
		try
		{
			System.out.println ( "Prima di Close" );
			fiscalPrinterDriver.close();
		}
		catch ( jpos.JposException e )
		{
			System.out.println ( "Printer Exception <"+e.toString()+">");
		}
	}
	
	public void beginFiscalReceipt(boolean arg0) throws JposException
	{
		checkPrinterPaperLow();
		
		System.out.println("MAPOTO-EXEC BEGINFISCAL");
		
		if (SRTPrinterExtension.isPRT()){
			String[] doc = {""};
    		String s = intestazione1(RTTxnType.getTypeTrx());
    		if (s.length() > 0){
    			if (SharedPrinterFields.isfiscalEJenabled()) ForFiscalEJFile.writeToFile("\n\t\t"+s.trim());
    			doc = s.trim().split(" ");
    		}
    		s = intestazione2(RTTxnType.getTypeTrx());
    		if (s.length() > 0)
    			if (SharedPrinterFields.isfiscalEJenabled()) ForFiscalEJFile.writeToFile("\t\t"+s.trim());
    		s = intestazione3(RTTxnType.getTypeTrx());
    		if (s.length() > 0)
    			if (SharedPrinterFields.isfiscalEJenabled()) ForFiscalEJFile.writeToFile("\t\t"+s.trim());
    		s = intestazione4(RTTxnType.getTypeTrx());
    		if (s.length() > 0)
    			if (SharedPrinterFields.isfiscalEJenabled()) ForFiscalEJFile.writeToFile("\t\t"+s.trim());
    		
	        int[] ai = new int[1];
	        String[] as = new String[1];
	        fiscalPrinterDriver.getData(FiscalPrinterConst.FPTR_GD_FISCAL_REC, ai, as);
            String n = as[0];
            
            String z = "";
            if (DummyServerRT.CurrentFiscalClosure == 0) {
	            fiscalPrinterDriver.getData(FiscalPrinterConst.FPTR_GD_Z_REPORT, ai, as);
	            try {
	            	z = Sprint.f("%04d",Integer.parseInt(as[0])+1);
		        } catch (NumberFormatException e) {
				   System.out.println("beginFiscalReceipt - NumberFormatException : " + e.getMessage());
				   z = Sprint.f("%04d",0);
				   fiscalPrinterDriver.resetPrinter();
		        }
            }
            else {
            	z = Sprint.f("%04d",DummyServerRT.CurrentFiscalClosure);
            }
            
            s = doc[0]+" N. "+z+"-"+n;
            if (SharedPrinterFields.isfiscalEJenabled()) ForFiscalEJFile.writeToFile("\t\t"+s);
            LastTicket.setDocnum(ALINER.substring(0, (int)((RTConsts.setMAXLNGHOFLENGTH()-s.length())/2))+s);
            
            intestazione5(false);
            
            DummyServerRT.CurrentFiscalClosure = Integer.parseInt(z);
            DummyServerRT.CurrentReceiptNumber = n;
		}
		
		inReset = false;
		
		discountedtaxamount = new BigDecimal(0.);
		discountedtaxamount = discountedtaxamount.setScale(2, RoundingMode.HALF_DOWN);
		
		if (SRTPrinterExtension.isSRT())
		{
			SSCO = null;
			totaleNonRiscosso = 0;
		}
		
		if (isLNFMAndSRTModel())
		{
			setFlagVoidRefund(false);
			SharedPrinterFields.inRetryFiscal = true;				// per le volte successive alla prima è una retry
			cleanDailyTotal();
			String Total = currentFiscalTotal(jpos.FiscalPrinterConst.FPTR_GD_DAILY_TOTAL);
		    storeDailyTotal(Total);
		    
			cancellaFile();
			
			HardTotals.doBeginFiscal();
			
			beginNonFiscal();
			
			testata();

			return;
		}
		
	    if (SmartTicket.isSmart_Ticket() && (RTTxnType.isSaleTrx() || SRTPrinterExtension.isSRT()))
	    {
	    	// in uno scontrino di reso questi comandi andrebbero in errore
	    	// se mandati a questo punto cioè dopo il comando di refund
    		fiscalPrinterDriver.SMTKsetReceiptParameters();
	    }
	    
		setFlagVoidRefund(false);
		AtLeastOnePrintedItem = false;
		SharedPrinterFields.inRetryFiscal = true;				// per le volte successive alla prima è una retry
		cleanDailyTotal();
		String Total = currentFiscalTotal(jpos.FiscalPrinterConst.FPTR_GD_DAILY_TOTAL);
	    storeDailyTotal(Total);
		System.out.println ( "MAPOTO-EXEC BEGINFISCAL AFTER STORE DAILY TOTAL" );
		
		System.out.println ( "MAPOTO-EXEC BEGINFISCAL BEFORE NATIVE" );
		fiscalPrinterDriver.beginFiscalReceipt(arg0);
		System.out.println ( "MAPOTO-EXEC BEGINFISCAL AFTER  NATIVE" );
		
		if (isFiscalAndSRTModel())
		{
		    cancellaFile();

		    HardTotals.doBeginFiscal();
			
			testata();
		}
		
		if (SRTPrinterExtension.isPRT())
		{
			totaleNonRiscosso = 0;
			
		    cancellaFile();
			initTicketOnFile();	// rimette l'header nel file LastTicket.out appena cancellato
		    
		    HardTotals.doBeginFiscal();
		    
			String s = LastTicket.getIntestazione1();
			if (s.length() > 0) scriviLastTicket(s);
			s = LastTicket.getIntestazione2();
			if (s.length() > 0) scriviLastTicket(s);
			s = LastTicket.getIntestazione3();
			if (s.length() > 0) scriviLastTicket(s);
			s = LastTicket.getIntestazione4();
			if (s.length() > 0) scriviLastTicket(s);
			s = LastTicket.getIntestazione5();
			if (s.length() > 0) scriviLastTicket(s);
		}
		
		if (isNotRTActivated())
		{
		    cancellaFile();
			initTicketOnFile();
		    HardTotals.doBeginFiscal();
		}
	}
	
	public void printNormal(int i, String s) throws JposException
	{
		EjCommands ej = SharedPrinterFields.getEjCommands();
		ej.printNormal(i, s+R3define.CrLf);
		
		printNormal_ejoff(i, s);
	}
	
	private void printNormal_ejoff(int i, String s) throws JposException
	{
		if (s.contains(Cancello.getTag()+R3define.CrLf)) {
			String s1 = s.replaceAll(Cancello.getTag()+R3define.CrLf, "");
			s = s1;
			if (s.length() == 0)
				return;
		}
		
		System.out.println("MAPOTO-EXEC PRINT NORMAL "+s);
		
		s = hideSomething(s);
		
		if (isLNFMAndSRTModel())
		{
			if (s.startsWith(R3_DELETELASTTICKET_R3))
			{
				// il file LastTicket.out viene cancellato e ricreato ad ogni BeginFiscal()
				// oppure se si riceve la stringa "R3_NEWCMD01_R3" in una printNormal()
				cancellaFile();
				//initTicketOnFile();	// rimette l'header nel file LastTicket.out appena cancellato
				return;
			}
			scriviLastTicket(s);
		}
		
		if (isFiscalAndSRTModel() || SRTPrinterExtension.isPRT() || isNotRTActivated())
			lastwrittenstring = s;
		
		setFlagVoidRefund(false);
		
		if (s.startsWith(R3_DELETELASTTICKET_R3))
		{
			return;
		}
		
		printNormal_I(i,s);
		
		lastwrittenstring = "";
		
		if (isFiscalAndSRTModel() || SRTPrinterExtension.isPRT() || isNotRTActivated())
		{
			scriviLastTicket(s);
		}
	}
	
	public void printRecItem(String s, long l, int i, int jIvaPolipos, long l1, String s1) throws JposException {
		System.out.println ( "MAPOTO-EXEC PRINT ITEM s="+s );
		System.out.println ( "MAPOTO-EXEC PRINT ITEM l="+l );
		System.out.println ( "MAPOTO-EXEC PRINT ITEM i="+i );
		System.out.println ( "MAPOTO-EXEC PRINT ITEM jIvaPolipos="+jIvaPolipos );
		System.out.println ( "MAPOTO-EXEC PRINT ITEM l1="+l1 );
		System.out.println ( "MAPOTO-EXEC PRINT ITEM s1="+s1 );
		
		if (SRTPrinterExtension.isSRT())
		{
			String ivadesc = DicoTaxLoad.getDTO(jIvaPolipos).getShortdescription();
			
			s = addIva(s, RTConsts.getMAXITEMDESCRLENGTH()-1, ivadesc);
		}
		
		if (SRTPrinterExtension.isPRT() && TaxData.isOmaggio(jIvaPolipos) && fiscalPrinterDriver.isfwRT2enabled()) {
			// lo vende automaticamente la printer
			return;
		}
		
		AtLeastOnePrintedItem = true;
		
		int j = DicoTaxToPrinter.getFromPoliposToPrinter(jIvaPolipos);
		if ((DicoTaxLoad.isIvaAllaPrinter()) && (j == SharedPrinterFields.VAT_N4_Index))
			j = Integer.parseInt(SharedPrinterFields.VAT_N4_Dept);
			
		System.out.println ( "MAPOTO-EXEC PRINT ITEM jIvaPolipos="+jIvaPolipos);
		System.out.println ( "MAPOTO-EXEC PRINT ITEM j="+j);
		
		final int MAXLNGHOFDESCR = 28;
		
		setFlagVoidRefund(false);
		
		System.out.println ( "MAPOTO-EXEC PRINT ITEM"+s );
		
		String newdes= null;
		newdes = s.replaceAll("(?i)TOTALE", "TOTA..");
		s = newdes;
		newdes = s.replaceAll("(?i)TOTAL", "TOTA.");
		
		String k2desc="";
		for(int k2idx=0; k2idx<newdes.length(); k2idx++)
		{
            if ((int)(newdes.charAt(k2idx)) >= 128)
                k2desc = k2desc + ".";
            else if ((int)(newdes.charAt(k2idx)) < 32)
                k2desc = k2desc + " ";
            else
                k2desc = k2desc + newdes.charAt(k2idx);
		}
		newdes=k2desc;
			
        String s2 = (s.length() > MAXLNGHOFDESCR) ? newdes.substring(0, MAXLNGHOFDESCR) :newdes;
        System.out.println("printRecItem in - s=<" + s2 + "> l=" + l + " i=" + i + " j=" + j
                + " l1=" + l1 + " s1=" + s1);
       	fiscalPrinterDriver.printRecItem(s2, l / 100, 0, j, l1, s1);
        System.out.println("printRecItem out");
		
		if (isFiscalAndSRTModel() || isNotRTActivated())
		{
			HardTotals.doPrintRecItem(l);
			
			scriviLastTicket(buildItem ( s, l ));
		}
		
		if (SRTPrinterExtension.isPRT())
		{
			HardTotals.doPrintRecItem(l);
			
			String ivadesc = String.format("%.2f", DicoTaxLoad.getDTO(jIvaPolipos).getTaxrate())+"%";
			s = addIva(s, RTConsts.getMAXITEMDESCRLENGTH()-1, ivadesc);
			
			scriviLastTicket(buildItem ( s, l ));
		}
		
		if ( iconic.mytrade.gutenberg.jpos.printer.service.RoungickInLinePromo.isRoungickInLinePromo() )
		{
			System.out.println ( "MAPOTO-EXEC PRINT ITEM PLUS "+s1 );
			ArrayList A = iconic.mytrade.gutenberg.jpos.printer.service.RoungickInLinePromo.getDiscountFromTable(s1);
				
			if ( A != null )
			{
				for ( int x = 0 ; x < A.size(); x++ )
				{
					String S = (String) A.get(x);
					printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, S);
				}
			}	
		}
	}

	public void printRecVoid(String arg0) throws JposException {
		System.out.println ( "MAPOTO-EXEC PRINT VOID" );

		if (SRTPrinterExtension.isPRT())
			setFlagsVoidTicket( true );
		
		setFlagVoidRefund(false);
		
		pleaseVoid ( arg0 );
	}
	
	private void pleaseVoid ( String arg0 ) throws JposException
	{
		fiscalPrinterDriver.printRecVoid(arg0);
	}
	
	public void endFiscalReceipt(boolean arg0) throws JposException {

        if (SRTPrinterExtension.isPRT())
        	SharedPrinterFields.Lotteria.resetLottery();
        
		System.out.println ( "MAPOTO-EXEC_ENDFISCAL CHECKING NONFISCAL TRAILER" );
		
		System.out.println ( "MAPOTO-EXEC ENDFISCAL - arg0="+arg0 );
		String dateTime = "";
		
		if (SRTPrinterExtension.isRTActivated() == true) {
			DummyServerRT.setSRTTillID();
		}
		
  	    if (SRTPrinterExtension.isSRT())
		{
  	    	if (RTTxnType.isVoidTrx())
  	    		DummyServerRT.saveXmlReceipt();
  	    	
  	    	DummyServerRT.pleaseDoServerInfo();
  	    	
  	    	//setSRTTillID();
  	    	
  	    	if (RTTxnType.isVoidTrx())
  	    		DummyServerRT.restoreXmlReceipt();
  	    	
  	    	String current_dateTime = "";
  	    	LocalTimestamp clts = TransactionSale.getEndData();
  	    	current_dateTime = Sprint.f("%d%02d%02dT%02d%02d%02d",new Integer(clts.getYear()+1900),new Integer(clts.getMonth()+1),new Integer(clts.getDay()),new Integer(clts.getHour()),new Integer(clts.getMinute()),new Integer(clts.getSecond()));
  	    	System.out.println("current_dateTime="+current_dateTime+" previous_dateTime="+DummyServerRT.previous_dateTime);
  	    	if (!isFlagsVoidTicket() && (!current_dateTime.equalsIgnoreCase(DummyServerRT.previous_dateTime)))
  	    	{
				xml = new ArrayList();
				
				if (RTTxnType.isSaleTrx() || RTTxnType.isRefundTrx())
				{
                   if (DummyServerRT.isSRTNcrType()) {
                        if (DummyServerRT.CurrentReceiptNumber != null && DummyServerRT.CurrentReceiptNumber.trim().equals("1")) {
                        	DummyServerRT.pleaseDoDailyOpen();
                        }
                    }
	                   
					xml = SharedPrinterFields.a;
					LocalTimestamp lts = TransactionSale.getEndData();
					dateTime = Sprint.f("%d%02d%02dT%02d%02d%02d",new Integer(lts.getYear()+1900),new Integer(lts.getMonth()+1),new Integer(lts.getDay()),new Integer(lts.getHour()),new Integer(lts.getMinute()),new Integer(lts.getSecond()));
		  	    	System.out.println("dateTime="+dateTime);

					LoadMops.loadMops();
					
					ArrayList ivasemplice = new ArrayList();
					ArrayList sco = null;
					if (SSCO != null){
						sco = new ArrayList();
			       		for ( int index = 0 ; index < SSCO.size(); index++  ){
			       			VatInOutHandling vatInOutH = (VatInOutHandling) SSCO.get(index);
			    			
			    			double value = ( vatInOutH.getLordo() - vatInOutH.getTotalAmount() );
			    			sco.add((scontiSEMPLICI ) new scontiSEMPLICI(Integer.parseInt(vatInOutH.getSwVatCode()), Integer.parseInt(vatInOutH.getRtVatCode()), value, vatInOutH.getRate(), vatInOutH.getFullDescription()));
			    			
			    			ivaSEMPLICE iva = new ivaSEMPLICE(""+vatInOutH.getSwVatCode(),
									  ""+vatInOutH.getRtVatCode(),
									  vatInOutH.getRate(),
									  vatInOutH.getLordo(),
									  vatInOutH.getTotalNet(),
									  vatInOutH.getTotalTax(),
									  vatInOutH.getFullDescription(),
									  vatInOutH.getShortDescription());
			    			ivasemplice.add(iva);
			    			
			    		}
			       	}
					
					if (RTTxnType.isRefundTrx()){
						DummyServerRT.pleaseDoSetReferenceDocument(RTConsts.INTESTAZIONE4, SRTPrinterExtension.isRoungick());
					}
					
					if (RTTxnType.isSaleTrx()){
						if (!DummyServerRT.isSRTToshibaType())
							DummyServerRT.CurrentDailyAmount = ""+(int)((Double.parseDouble(DummyServerRT.CurrentDailyAmount)+(HardTotals.Totale.getDouble()*100)));
					}
					
					DummyServerRT.pleaseDoFiscalReceipt(sco,
													   	xml,
													   	LoadMops.Mops,
													   	RTTxnType.getTypeTrx(),
													   	RoungickTax.getTotalTaxDouble(),
													   	SRTPrinterExtension.isIndentMode(),
													   	SRTPrinterExtension.isRoungick(),
													   	DummyServerRT.SRTTillID,
													   	dateTime,
													   	SRTPrinterExtension.getRefundMode(),
													   	DummyServerRT.CurrentReceiptNumber,
													   	DummyServerRT.CurrentFiscalClosure,
													   	ivasemplice,
													   	null,
													   	DummyServerRT.CurrentDailyAmount);
					
					if (RTTxnType.isSaleTrx()){
						if (DummyServerRT.isSRTToshibaType())
							DummyServerRT.CurrentDailyAmount = ""+(int)((Double.parseDouble(DummyServerRT.CurrentDailyAmount)+(HardTotals.Totale.getDouble()*100)));
					}
				}
				
				if (RTTxnType.isVoidTrx())
				{
					toDayDate();
					dateTime = DummyServerRT.getCurrent_dateTime();
					DummyServerRT.WriteVoided(dateTime,
											  DummyServerRT.CurrentReceiptNumber,
											  DummyServerRT.CurrentFiscalClosure,
											  DummyServerRT.CurrentDailyAmount);	// crea l'xml relativo allo scontrino annullato
				}
				
				//boolean secured = xml4srt.getSecuredMode();
				
				String xmlticket = "";
				if (RTTxnType.isVoidTrx())
				 xmlticket = DummyServerRT.StoreXmlTicket(DummyServerRT.CurrentReceiptNumber, SetVoidTrx.getTxnnumbertovoid());
				else
				 xmlticket = DummyServerRT.StoreXmlTicket(DummyServerRT.CurrentReceiptNumber, PosApp.getTransactionNumber()-1);
					
//				if (secured){
					if (DummyServerRT.sendToSRT(xmlticket, SRTPrinterExtension.getSecureModePort(), SRTPrinterExtension.getSecureModeTimeout()) == false)
					{
						if (RTTxnType.isSaleTrx() || RTTxnType.isRefundTrx())
							AutoVoidTrx();
						else {
							MessageBox.showMessage(RTConsts.OPERAZIONEANNULLATA, null, MessageBox.OK);
						}
						
	  	    			if (isFiscalAndSRTModel())
	  	    			{
	  	    				// ATTENZIONE: se si arriva qui lo scontrino successivo avrà lo stesso numero fiscale di quello annullato
	  	    				// per cui quasi lo stesso filename nella cartella "printerRts", potrebbe causare qualche problema in qualche caso,
	  	    				// ad esempio richiamando lo scontrino successivo per annullarlo la stampante va in errore.
	  	    				fiscalPrinterDriver.resetPrinter();
	  	    			}
	  	    			else
	  	    			{
	  	    				// potremmo passare da qui a seguito della stampa di uno scontrino di rtvoid quindi in modalità LNFM 
							printRecVoid("");
					  	    endFiscalReceipt_I(arg0);
					  	    SharedPrinterFields.resetInTicket();
					  	    setFlagsVoidTicket( false );
	  	    			}
						
	  	    			SharedPrinterFields.setMonitorState();
						SharedPrinterFields.a = new ArrayList();
				   		
				   		RTTxnType.setSaleTrx();
						CanPost.setCanPost(true);
	  	    			
//	  	    			while (true) {
	  						OperatorDisplay.pleaseDisplay(DummyServerRT.SERVEROFF);
	  						try {
								Thread.sleep(3000);
							} catch (InterruptedException e) {
							}
	  						OperatorDisplay.pleaseDisplay(DummyServerRT.TURNOFFON);
	  						try {
								Thread.sleep(3000);
							} catch (InterruptedException e) {
							}
//	  	    			}
	  	    			return;
					}
//				}
				
  	    		if (RTTxnType.isSaleTrx() || RTTxnType.isRefundTrx())
  	    			DummyServerRT.previous_dateTime = dateTime;
				if (RTTxnType.isVoidTrx())
				{
					Files.moveFile(SetVoidTrx.getSourcefiletovoid(), SetVoidTrx.getSourcefiletovoid()+".voided");
				}
				
				SSCO = null;
  	    	}
		}
  	    
  	    endFiscalReceipt_I(arg0);

		if (isFiscalAndSRTModel())
		{
			HardTotals.doEndFiscalSRT(RTTxnType.getTypeTrx());
			
			DummyServerRT.StoreTicket4Reprint(DummyServerRT.CurrentReceiptNumber);
			
			RTTxnType.setSaleTrx();
		}
		
		if (SRTPrinterExtension.isPRT())
		{
			HardTotals.doEndFiscalSRT(RTTxnType.getTypeTrx());
		}
		
		if (isNotRTActivated())
		{
			HardTotals.doEndFiscal();
		}
		
		SMTKCommands.Base64_Ticket(PosApp.getTransactionNumber()-1, false);
		SMTKCommands.Smart_Ticket(PosApp.getTransactionNumber()-1, false);
		
		if (XRData.MonitorRTisActive()) {
			if (SRTPrinterExtension.isPRT()) {
			
				if (monitorRT == null) monitorRT = new MonitorRT();
				
		    	String[] date = new String[1];
				try {
					fiscalPrinterDriver.getDate(date);
				} catch (Exception e) {
					monitorRT.logMRT("Exception : "+e.getMessage());
				}
	    	
				try {
					monitorRT.MonitorRT_Ticket(PosApp.getTransactionNumber()-1, DummyServerRT.CurrentFiscalClosure, DummyServerRT.CurrentReceiptNumber, SharedPrinterFields.RTPrinterId, date[0]);
				} catch (Exception e) {
					monitorRT.logMRT("Exception : "+e.getMessage());
				}
			
				String textfile = readFile(LastTicket.getLastticket(), true);
	        
				monitorRT.GetTicketDetails(SharedPrinterFields.RTPrinterId, date[0], DummyServerRT.CurrentFiscalClosure, DummyServerRT.CurrentReceiptNumber,
	        						   	   currentTicket/100, monitorRT.AliquoteTicket, textfile);
			}
		}
	}
	
	private void endFiscalReceipt_I(boolean arg0) throws JposException {
		if (isLNFMAndSRTModel())
		{
			setFlagVoidRefund(false);
			
	  	    endTicket(DummyServerRT.CurrentReceiptNumber);
	  	    
	  	    endTicketSRT(DummyServerRT.SRTServerID, DummyServerRT.SRTTillID, DummyServerRT.currentFingerPrint);
			HardTotals.doEndFiscalSRT(RTTxnType.getTypeTrx());
			SharedPrinterFields.resetInTicket();	    

			setFlagsVoidTicket( false );
	        
			endNonFiscal();
			
			DummyServerRT.StoreTicket4Reprint(DummyServerRT.CurrentReceiptNumber);
			
			SharedPrinterFields.setMonitorState();
			SharedPrinterFields.a = new ArrayList();

			RTTxnType.setSaleTrx();
			CanPost.setCanPost(true);
			return;
		}
		
		if (isFiscalAndSRTModel())
		{
	  	    endTicket(DummyServerRT.CurrentReceiptNumber);
	  	    
	  	    endTicketSRT(DummyServerRT.SRTServerID, DummyServerRT.SRTTillID, DummyServerRT.currentFingerPrint);

	  	    setFlagsVoidTicket( false );
		}
		
		setFlagVoidRefund(false);
		AtLeastOnePrintedItem = false;
		int state=0;
		SharedPrinterFields.resetprtDone();
		
		System.out.println ( "MAPOTO-EXEC_ENDFISCAL" );
		
		SharedPrinterFields.resetInTicket();	    
   		stampaBarcodePerResi();
    	
    	fiscalPrinterDriver.endFiscalReceipt(arg0);
    	
        SharedPrinterFields.resetInTicket();	    
        setFlagsVoidTicket( false );
        
		String Total = currentFiscalTotal(jpos.FiscalPrinterConst.FPTR_GD_DAILY_TOTAL);
	    if ( fiscalPrinterDriver.checkCurrentDailyTotal(Total) == false )
	    {
			if (SRTPrinterExtension.isPRT() && (RTConsts.getCURRENTROUNDING() == RTConsts.FULLROUNDING) && (SharedPrinterFields.RoundingRT != 0.0)) {
				if ( fiscalPrinterDriver.checkCurrentDailyTotalRounded(Total, SharedPrinterFields.RoundingRT.doubleValue()) == false ) {
					System.out.println ("FISCAL PRINTER RECEIPT NOT STORED");
			    	throw new JposException(9999);
				}
			}
			else {
				System.out.println ("FISCAL PRINTER RECEIPT NOT STORED");
		    	throw new JposException(9999);
			}
	    }
		
		if (SRTPrinterExtension.isPRT()) {
			if (RTTxnType.isRefundTrx()) {
				if (R3define.getRefundResult() != R3define.INVALID)
					R3define.setRefundResult(R3define.SUCCESS);
			}
			RTTxnType.setSaleTrx();
		}
		
		SharedPrinterFields.setMonitorState();
		SharedPrinterFields.a = new ArrayList();			// prova reset scontrino precedente
		
		CanPost.setCanPost(true);
	}
	
	public void resetPrinter() throws JposException {
		System.out.println ( "MAPOTO-EXEC RESET" );
		
		if ( inReset == true )
		{
			return;
		}
		inReset = true;
		setFlagVoidRefund(false);
		int a = getFiscalPrinterState();
		System.out.println("MAPOTO-EXEC_RESET_IN <"+a+">");
		if ( a == jpos.FiscalPrinterConst.FPTR_PS_MONITOR )
		{
			System.out.println("MAPOTO-EXEC_RESET_OUT IN STATE MONITOR NO ACTION ");
			return;
		}
		if (a == jpos.FiscalPrinterConst.FPTR_PS_FISCAL_RECEIPT)
		{
			pleaseVoid("");
			fiscalPrinterDriver.endFiscalReceipt(false);
		}
		else if (a == jpos.FiscalPrinterConst.FPTR_PS_NONFISCAL)
		{
			fiscalPrinterDriver.endNonFiscal();
		}
		else if (a == jpos.FiscalPrinterConst.FPTR_PS_FISCAL_RECEIPT_ENDING)
		{
			fiscalPrinterDriver.endFiscalReceipt(false);
		}
		resetAndClear();
		System.out.println("MAPOTO-EXEC_RESET_OUT <"+getFiscalPrinterState()+">");
	}
	
	public void beginNonFiscal() throws JposException {
   		System.out.println(  "MAPOTO-EXEC BEGINNONFISCALT <"+getFiscalPrinterState()+">");
		
		if (SRTPrinterExtension.isSRT())
		{
			HardTotals.doBeginNonFiscal();
		}
		
		if (SRTPrinterExtension.isPRT() || isNotRTActivated())
		{
		    cancellaFile();
			initTicketOnFile();	// rimette l'header nel file LastTicket.out appena cancellato
			HardTotals.doBeginNonFiscal();
		}
		
		inReset = false;
		setFlagVoidRefund(false);
		
    	fiscalPrinterDriver.beginNonFiscal( );
    	PrintLogo(LOGO_NUMBER);
		initTicket();
	}
	
	public void endNonFiscal() throws JposException {
		System.out.println  ( "MAPOTO-EXEC ENDNONFISCAL" );

        if (SRTPrinterExtension.isPRT())
        	SharedPrinterFields.Lotteria.resetLottery();
        
		if (SRTPrinterExtension.isSRT())
		{
			HardTotals.doEndNonFiscal();
		}
		
		if (SRTPrinterExtension.isPRT())
		{
			HardTotals.doEndNonFiscal();
		}
		
		if (isNotRTActivated())
		{
			HardTotals.doEndNonFiscal();
		}
		
		setFlagVoidRefund(false);
		SharedPrinterFields.resetInTicket();
		SharedPrinterFields.resetprtDone();
    	PrintLogo(TRAILER_LOGO_NUMBER);
    	fiscalPrinterDriver.endNonFiscal( );
    	SharedPrinterFields.setMonitorState();
	}
	
	public void printRecItemAdjustment(int arg0, String arg1, long arg2, int arg3) throws JposException {
		System.out.println ( "MAPOTO-EXEC PRINT ITEM ADJUSTMENT" );
		
		setFlagVoidRefund(false);
		fiscalPrinterDriver.printRecItemAdjustment(arg0, arg1, arg2, arg3);
		
		if (isFiscalAndSRTModel() || SRTPrinterExtension.isPRT() || isNotRTActivated())
		{
	    	HardTotals.doSubtotalAdjustment(arg2);

	    	scriviLastTicket(buildItemAdjustment ( "", arg2 ));
		}
	}

	public void printRecMessage(String arg0) throws JposException {
		if (arg0.equalsIgnoreCase(Cancello.getTag())) {
			return;
		}
		
		System.out.println ( "MAPOTO-EXEC PRINT MESSAGE "+arg0+" staticMsgLen=<"+staticMsgLen+">" );
		
		if (isLNFMAndSRTModel())
		{
			setFlagVoidRefund(false);
			
		    int i = RTConsts.setMAXLNGHOFLENGTH();
		    
		    String s1 = String13Fix.replaceAll(arg0,R3define.Lf, String13Fix.REPLACE);
		    s1 = String13Fix.replaceAll(s1,R3define.Cr, String13Fix.REPLACE);
		    s1 = String13Fix.replaceAll(s1,String13Fix.REPLACE+String13Fix.REPLACE, String13Fix.REPLACE);
		    String[] msg = String13Fix.split(s1,String13Fix.REPLACE);
		    for (int z = 0; z < msg.length; z++)
		    {
		    	printNormal_ejoff(jpos.FiscalPrinterConst.FPTR_S_RECEIPT,(msg[z].length() > i) ? msg[z].substring(0, i) : msg[z]);
		    }
			return;
		}
		
		String back30 = "";
		setFlagVoidRefund(false);
	    int i = 43;
	    
    	if ( staticMsgLen == 0 )
    		staticMsgLen = fiscalPrinterDriver.getMessageLength();
    	i = staticMsgLen;
    	
	    String s1 = String13Fix.replaceAll(arg0,R3define.Lf, String13Fix.REPLACE);
	    s1 = String13Fix.replaceAll(s1,R3define.Cr, String13Fix.REPLACE);
	    s1 = String13Fix.replaceAll(s1,String13Fix.REPLACE+String13Fix.REPLACE, String13Fix.REPLACE);

    	s1 = String13Fix.replaceAll(s1, "€", ""+(char)127);
		
		String newdes= null;
		newdes = s1.replaceAll("(?i)TOTALE", "TOTA..");
		s1 = newdes;
		newdes = s1.replaceAll("(?i)TOTAL", "TOTA.");
		s1 = newdes;
		
		if (SRTPrinterExtension.isPRT())
		{
		      String oldS = s1;
		      s1 = "";
		      for ( int j = 0 ; j < oldS.length(); j++ )
		      {
		    	  if ((oldS.charAt(j) < (char)32) || (oldS.charAt(j) > (char)127))
		    	  {
	        		  if ((oldS.charAt(j) == (char)10) || (oldS.charAt(j) == (char)13) || (oldS.charAt(j) == '�'))
	    	    		  s1 = s1 + oldS.charAt(j);
	        		  else
	        			  s1 = s1 + ' ';
		    	  }
		    	  else
		    		  s1 = s1 + oldS.charAt(j);
		      }
		}
		
	    String[] msg = String13Fix.split(s1,String13Fix.REPLACE);
	    for (int z = 0; z < msg.length; z++)
	    {
        	if (msg[z].startsWith(SharedPrinterFields.getCFPIvaTag())) {
        		String cfpi = msg[z].substring(SharedPrinterFields.getCFPIvaTag().length());
                cfpi = String13Fix.replaceAll(cfpi, "\r", "");
                cfpi = String13Fix.replaceAll(cfpi, "\n", "").trim();
        		if (SRTPrinterExtension.isPRT()) {
        			printRecCFPiva(cfpi);
        			continue;
        		}
        		else {
        			msg[z] = (cfpi.length() == PILEN ? PI+cfpi : CF+cfpi);
        		}
        	}

        	if (PaperSavingProperties.isMode()) {
	        	if (msg[z].length() == 0 || msg[z].replaceAll(" ", "").length() == 0)
	        		continue;
        	}
        	
			if (fw >= 6.0) {
				String Testo = (msg[z].length() > i) ? msg[z].substring(0, i) : msg[z];
				StringBuffer printrecmessage = new StringBuffer("=\"/("+Testo+")/&1");	/* 		="/(Testo)/&1 		*/
				if (AtLeastOnePrintedItem)
    				printrecmessage = new StringBuffer("=\"/("+Testo+")");
				fiscalPrinterDriver.executeRTDirectIo(0, 0, printrecmessage);
			}
    		else
    			fiscalPrinterDriver.printRecMessage((msg[z].length() > i) ? msg[z].substring(0, i) : msg[z]);
	    }
	    
		if (isFiscalAndSRTModel() || SRTPrinterExtension.isPRT() || isNotRTActivated())
		{
	    	if (!arg0.equalsIgnoreCase(lastwrittenstring)){
		    	scriviLastTicket(arg0);
	    	}
		}
		
	    System.out.println ( "MAPOTO-EXEC PRINT MESSAGE FINITA "+arg0 );
	}

	private void printRecNormalRefund(String s, long l, int i) throws JposException
	{
		System.out.println ( "MAPOTO-EXEC PRINT REFUND s="+s );
		System.out.println ( "MAPOTO-EXEC PRINT REFUND l="+l);
		System.out.println ( "MAPOTO-EXEC PRINT REFUND i="+i );
		
		printRecRefund(s, l, i);
		
		if (isFiscalAndSRTModel())
		{
		  	HardTotals.doPrintRecRefund(l);
		  	
	    	scriviLastTicket(buildItemRefund ( s, l ));
		}
		
		if (SRTPrinterExtension.isPRT())
		{
		  	HardTotals.doPrintRecRefund(l);
		  	
			String ivadesc = String.format("%.2f", DicoTaxLoad.getDTO(i).getTaxrate())+"%";
			s = addIva(s, RTConsts.getMAXITEMDESCRLENGTH()-1, ivadesc);
			
			scriviLastTicket(buildItem ( s, l ));
		}
	}

	public void printRecRefund(String s, long l, int iIvaPolipos) throws JposException {
		System.out.println ( "MAPOTO-EXEC PRINT REFUND s="+s );
		System.out.println ( "MAPOTO-EXEC PRINT REFUND l="+l);
		System.out.println ( "MAPOTO-EXEC PRINT REFUND i="+iIvaPolipos );
		
		if (SRTPrinterExtension.isSRT())
		{
			String ivadesc = DicoTaxLoad.getDTO(iIvaPolipos).getShortdescription();
			
			s = addIva(s, RTConsts.getMAXITEMDESCRLENGTH()-1, ivadesc);
		}
		
		if (SRTPrinterExtension.isPRT() && TaxData.isAcconti(iIvaPolipos) && fiscalPrinterDriver.isfwRT2enabled())
		{
			printRecAcconto(s, l, iIvaPolipos);
			return;
		}
		
		if (SRTPrinterExtension.isPRT() && TaxData.isBuonoMonouso(iIvaPolipos) && fiscalPrinterDriver.isfwRT2enabled())
		{
			printRecBMonoUso(s, l, iIvaPolipos);
			return;
		}
		
		int i = DicoTaxToPrinter.getFromPoliposToPrinter(iIvaPolipos);
		if ((DicoTaxLoad.isIvaAllaPrinter()) && (i == SharedPrinterFields.VAT_N4_Index))
			i = Integer.parseInt(SharedPrinterFields.VAT_N4_Dept);
		
		setFlagVoidRefund(true);
		System.out.println ( "MAPOTO-EXEC PRINT REFUND "+s );
		
	    final int MAXLNGHOFDESCR = 23;
		String newdes = null;
		newdes = s.replaceAll("(?i)TOTALE", "TOTA..");     
		s = newdes;
		newdes = s.replaceAll("(?i)TOTAL", "TOTA.");

		String k2desc="";
		for(int k2idx=0; k2idx<newdes.length(); k2idx++)
		{
	        if ((int)(newdes.charAt(k2idx)) >= 128)
	            k2desc = k2desc + ".";
            else if ((int)(newdes.charAt(k2idx)) < 32)
                k2desc = k2desc + " ";
	        else
	            k2desc = k2desc + newdes.charAt(k2idx);
		}
		newdes=k2desc;
	    String newMsg = (newdes.length()<= MAXLNGHOFDESCR)? newdes: newdes.substring (0, MAXLNGHOFDESCR);
    	if (l > 0)
    		fiscalPrinterDriver.printRecRefund (newMsg, l / 100, i);
    	else
    		printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, "Reso "+s);
	    
		if (isFiscalAndSRTModel() || isNotRTActivated())
		{
		  	HardTotals.doPrintRecRefund(l);
		  	
	    	scriviLastTicket(buildItemRefund ( s, l ));
		}
		
		if (SRTPrinterExtension.isPRT())
		{
		  	HardTotals.doPrintRecRefund(l);
		  	
			String ivadesc = String.format("%.2f", DicoTaxLoad.getDTO(iIvaPolipos).getTaxrate())+"%";
			s = addIva(s, RTConsts.getMAXITEMDESCRLENGTH()-1, ivadesc);
			
			scriviLastTicket(buildItemRefund ( s, l ));
		}
	}

	public void printRecSubtotal(long arg0) throws JposException {
		System.out.println ( "MAPOTO-EXEC PRINT SUBTOTAL "+arg0 );
		
		setFlagVoidRefund(false);
		
		if (AtLeastOnePrintedItem == false)
			return;
		
		if (!SharedPrinterFields.isprtDone() && (fiscalPrinterDriver.getPrinterState() == jpos.FiscalPrinterConst.FPTR_PS_FISCAL_RECEIPT))
			fiscalPrinterDriver.printRecSubtotal(arg0);
	}

	public void printRecSubtotalAdjustment(int arg0, String arg1, long arg2) throws JposException {
		if (arg0 == 0) {
			// from GutenbergHub
			
			printScontiByTax(arg1, arg2);
			if (SRTPrinterExtension.isPRT() || SRTPrinterExtension.isSRT())
				HardTotals.doSubtotalAdjustment(arg2);
			return;
		}
		
		System.out.println ( "MAPOTO-EXEC PRINT SUBTOTAL ADJUSTMENT" );
		System.out.println ( "MAPOTO-EXEC PRINT SUBTOTAL ADJUSTMENT - arg0="+arg0 );
		System.out.println ( "MAPOTO-EXEC PRINT SUBTOTAL ADJUSTMENT - arg1="+arg1 );
		System.out.println ( "MAPOTO-EXEC PRINT SUBTOTAL ADJUSTMENT - arg2="+arg2 );
		
		if (SRTPrinterExtension.isPRT()){
			HardTotals.doSubtotalAdjustment(arg2);
			printScontiByTax();
			return;
		}
		
		if (isFiscalAndSRTModel())
		{
			if (RTTxnType.isVoidTrx())
				return;
			printScontiByTax(arg0, arg1, arg2);
			HardTotals.doSubtotalAdjustment(arg2);
		}
		else
			printRecSubtotalAdjustment_I(arg0, arg1, arg2);
		
		if (isNotRTActivated())
		{
			HardTotals.doSubtotalAdjustment(arg2);
	        String out = buildSubtotalAdjustment ( arg1, arg2 );
	        scriviLastTicket(out);
		}
	}
	
	private void printRecSubtotalAdjustment_I(int arg0, String arg1, long arg2) throws JposException {
		setFlagVoidRefund(false);
		
		System.out.println ( "MAPOTO-EXEC PRINT SUBTOTAL ADJ "+arg1+" "+arg2 );
		
   		fiscalPrinterDriver.printRecSubtotalAdjustment(arg0, RTConsts.SCONTO + arg1, arg2 / 100);
	}

	public void directIO(int i, int data[],  Object o) throws JposException
	{
		System.out.println ( "MAPOTO-EXEC DIRECTIO" );
		
		String s =  o.toString();
		StringBuffer bf = new StringBuffer(s);
		
		if ( ! SharedPrinterFields.isInTicket() ) 				// directIO estemporanei
		{
			fiscalPrinterDriver.directIO(i, data, o);
			return;
		}
		
//		if (SRTPrinterExtension.isSRT() || SRTPrinterExtension.isPRT())
//		{
			scriviLastTicket(bf.toString());
//		}
		
		setFlagVoidRefund(false);
		
		System.out.println ( "XDIRECTIO :"+ i +":"+data[0]+":"+s+":");
		
        String[] pString = {new String(s)};
    	if (i >= 1000)
    		fiscalPrinterDriver.directIO(i, data, pString);
    	else
    		fiscalPrinterDriver.directIO(i, data, s);
	}
	
    private void printScontiByTax(int arg0, String arg1, long arg2) throws JposException
	{
		SSCO = RoungickTax.getVatTable(true);
		if (SSCO != null)
		{
			discountedtaxamount = new BigDecimal(0.);
			discountedtaxamount = discountedtaxamount.setScale(2, RoundingMode.HALF_DOWN);
			for (int index=0; index < SSCO.size(); index++)
			{
				VatInOutHandling vatInOutH = (VatInOutHandling) SSCO.get(index);
				double value = Math.rint(( vatInOutH.getLordo() - vatInOutH.getTotalAmount() ) * 100) / 100;
				if (value > 0.00) {
					String out = buildSRTSubtotalAdjustment ( vatInOutH.getFullDescription(), (long)(value*10000) );
			        scriviLastTicket(out);
					
					EjCommands.Write(out, true);
					
					printRecSubtotalAdjustment_I(arg0, vatInOutH.getFullDescription(), (long)(value*10000));

					BigDecimal discamount = new BigDecimal(value);
					discamount = discamount.setScale(2, RoundingMode.HALF_DOWN);

					BigDecimal d = discamount.subtract(discamount.multiply(new BigDecimal(100.0)).divide(new BigDecimal(100.0+vatInOutH.getRate()),2,RoundingMode.HALF_DOWN));
					d = d.setScale(2, RoundingMode.HALF_DOWN);
					
					BigDecimal dd = d;
					dd = dd.setScale(2, RoundingMode.HALF_DOWN);
					
					discountedtaxamount = discountedtaxamount.add(dd);
				}
			}
		}
		else
			System.out.println("printScontiByTax - SSCO is null");
	}
    
    private void printScontiByTax() throws JposException
	{
		SSCO = RoungickTax.getVatTable(true);				
		if (SSCO != null)
		{
			double valueVI = 0;
			
			for (int index=0; index < SSCO.size(); index++)
			{
				VatInOutHandling vatInOutH = (VatInOutHandling) SSCO.get(index);
				double value = ( vatInOutH.getLordo() - vatInOutH.getTotalAmount() );
				if (value > 0.00) {
					String myDepartment = "";
					if (Integer.parseInt(vatInOutH.getRtVatCode()) == SharedPrinterFields.VAT_N4_Index)
						myDepartment = SharedPrinterFields.VAT_N4_Dept;
					else
						myDepartment = ""+DicoTaxToPrinter.getFromPoliposToPrinter(Integer.parseInt(vatInOutH.getSwVatCode()));
					String myDiscountAmount = ""+(int)(Math.rint(value*100));
					String myDiscountDescription = SharedPrinterFields.DESCRIZIONE_SCONTO;
					
					if (MyTradeProperties.isMergeVatDiscount() && MixedVat(myDepartment)) {
						valueVI+=value;
					}
					else {
						StringBuffer sbcmd = new StringBuffer("=U/*"+myDepartment+"/$"+myDiscountAmount+"/("+myDiscountDescription+")");
						System.out.println("printScontiByTax - sbcmd="+sbcmd);
						fiscalPrinterDriver.executeRTDirectIo(0, 0, sbcmd);
					}
					
			        String out = buildSubtotalAdjustment ( myDiscountDescription.substring(7)+" "+vatInOutH.getRate()+"%", (long)(Math.rint(value*100) * 100) );
			        scriviLastTicket(out);
				}
			}
			
			if (valueVI > 0.00) {
				// faccio un unico sconto sul totale contenente la somma degli sconti sulle varie aliquote VI
				String myDiscountAmount = ""+(int)(Math.rint(valueVI*100));
				
				StringBuffer sbcmd = new StringBuffer("=S");
				System.out.println("printScontiByTax - sbcmd="+sbcmd);
				fiscalPrinterDriver.executeRTDirectIo(0, 0, sbcmd);
				sbcmd = new StringBuffer("=V/*"+myDiscountAmount);
				System.out.println("printScontiByTax - sbcmd="+sbcmd);
				fiscalPrinterDriver.executeRTDirectIo(0, 0, sbcmd);
			}
			
		}
		else
			System.out.println("printScontiByTax - SSCO is null");
	}
    
    private void printScontiByTax(String vat, long value) throws JposException
	{
		// from GutenbergHub
		
		if (SRTPrinterExtension.isPRT()) {
			if (value > 0.00) {
				String myDepartment = "";
				if (Integer.parseInt(vat) == SharedPrinterFields.VAT_N4_Index)
					myDepartment = SharedPrinterFields.VAT_N4_Dept;
				else
					myDepartment = vat;
				String myDiscountAmount = ""+(int)(Math.rint(value));
				String myDiscountDescription = SharedPrinterFields.DESCRIZIONE_SCONTO;
				
				StringBuffer sbcmd = new StringBuffer("=U/*"+myDepartment+"/$"+myDiscountAmount+"/("+myDiscountDescription+")");
				System.out.println("printScontiByTax - sbcmd="+sbcmd);
				fiscalPrinterDriver.executeRTDirectIo(0, 0, sbcmd);
			}
		}
		else if (isFiscalAndSRTModel()) {
			if (value > 0) {
				printRecSubtotalAdjustment_I(1, vat, (long)(value*100));
			}
		}
		else {
			printRecSubtotalAdjustment_I(1, vat, value*100);
		}
	}
    
	public void printRecTotal(long arg0, long arg1, String arg2) throws JposException {
		System.out.println ( "MAPOTO-EXEC PRINT TOTAL "+arg0+" "+arg1+" "+arg2 );
		
		if (SRTPrinterExtension.isPRT() && (RTConsts.getCURRENTROUNDING() == RTConsts.FULLROUNDING)) {
	    	System.out.println("RoundingData.getRoundingRT = "+RTRounding.getRoundingRT());
			if (!(RTRounding.getRoundingRT() == 0.)) {
				SharedPrinterFields.RoundingRT = new Double(RTRounding.getRoundingRT());
				long val =  (long) (SharedPrinterFields.RoundingRT*10000);
				if (SharedPrinterFields.fiscalEJ != null)
					SharedPrinterFields.fiscalEJ.setRounding(val);
				System.out.println("RoundingRT = "+SharedPrinterFields.RoundingRT);
				RTRounding.setRoundingRT(0.);
			}
			else {
				SharedPrinterFields.RoundingRT = 0.0;
				if (SharedPrinterFields.fiscalEJ != null)
					SharedPrinterFields.fiscalEJ.setRounding(0);
			}
		}
		
	    if (SmartTicket.isSmart_Ticket())
			smtkamount = (double)((new Long(arg0)).doubleValue() / 10000);
		
		// ora dovrebbe arrivare sempre tipo 001Contanti
		String pd = arg2;
		String p = arg2.substring(0,3);
		String d = arg2.substring(3);
		if (SRTPrinterExtension.isPRT())
			arg2 = d;
		System.out.println ( "MAPOTO-EXEC PRINT TOTAL - arg2="+arg2 );
		
		if (SRTPrinterExtension.isSRT())
			DummyServerRT.pleaseDoServerInfo();
		
		if (SRTPrinterExtension.isPRT()){
			if ((arg0 == 0) && (arg1 == 0)) {
				if (AtLeastOnePrintedItem == false){
					int aliquota = fiscalPrinterDriver.getVATTableEntry();
					// brutto ma per ora la stampante si incazza se si passa la tessera 
					// e poi si chiude lo scontrino a zero senza vendere nulla
					// oppure se nello scontrino non c'è nessun item fiscale
					fiscalPrinterDriver.printRecItem("....................", 1, 0, aliquota, 0, "");
					fiscalPrinterDriver.printRecVoidItem("....................", 1, 1, 1, 0, aliquota);
//					fiscalPrinterDriver.printRecItem("....................", 0, 0, aliquota, 0, "");
				}
			}
		}
		
		if (SRTPrinterExtension.isPRT()){
			pd = arg2;
			arg2 = LoadMops.getRCHPrefixPayment(arg2);
			System.out.println ( "MAPOTO-EXEC PRINT TOTAL - arg2="+arg2 );
			if (fiscalPrinterDriver.isfwRT2enabled()) {
				int t = Integer.parseInt(p.substring(0,1));
				if (t == LoadMops.TICKETWN_TYPE)
					arg2 = arg2+","+p.substring(1,3);
				System.out.println ( "MAPOTO-EXEC PRINT TOTAL - arg2="+arg2 );
			}
			System.out.println ( "MAPOTO-EXEC PRINT TOTAL - arg2="+arg2 );
		}
		
		if (SRTPrinterExtension.isPRT()){
			if (RTTxnType.isRefundTrx()){
				if ((SharedPrinterFields.Lotteria.getLotteryCode() != null) && (SharedPrinterFields.Lotteria.getLotteryCode().length() > 0))
					LotteryCommands.SendLotteryCode(SharedPrinterFields.Lotteria.getLotteryCode());
				
				long t = arg0;
				
				SharedPrinterFields.getChangeDescription();
				arg0 = 0;
				arg1 = 0;
				arg2 = LoadMops.getRCHPrefixPayment(SharedPrinterFields.ChangeCurrency);
				System.out.println ( "MAPOTO before driver.printRecTotal arg2="+arg2);
	        	fiscalPrinterDriver.printRecTotal(arg0, arg1, arg2);
				System.out.println ( "MAPOTO after driver.printRecTotal");
	        	
	        	try {
					// scrittura file lastticket
					
					String out = buildTotal ( "", t );
					scriviLastTicket(out);
					
					out = buildTotalIva ( "", RoungickTax.getTotalTaxLong() );
					scriviLastTicket(out);
					
					LocalTimestamp lts = TransactionSale.getEndData();
					String date = Sprint.f("%02d-%02d-%d %02d:%02d",new Integer(lts.getDay()),new Integer(lts.getMonth()+1),new Integer(lts.getYear()+1900),new Integer(lts.getHour()),new Integer(lts.getMinute())); 
					out = ALINER.substring(0, (int)((RTConsts.setMAXLNGHOFLENGTH()-date.length())/2))+date;
					scriviLastTicket(out);
					
					out = LastTicket.getDocnum();
					scriviLastTicket(out);
					
					if ((SharedPrinterFields.Lotteria.getLotteryCode() != null) && (SharedPrinterFields.Lotteria.getLotteryCode().length() > 0)) {
						out = LastTicket.getLottery()+SharedPrinterFields.Lotteria.getLotteryCode();
						scriviLastTicket(out);
					}
					
					out = ALINER.substring(0, (int)((RTConsts.setMAXLNGHOFLENGTH()-SharedPrinterFields.RTPrinterId.length())/2))+SharedPrinterFields.RTPrinterId;
					scriviLastTicket(out);
				} catch (Exception e) {
					System.out.println("LastTicket error: "+e.getMessage());
				}
	        	return;
			}
		}

		if (SRTPrinterExtension.isSRT() || (SRTPrinterExtension.isPRT() && RTTxnType.isRefundTrx()==false)){
			String s = arg2;
			arg2 = LoadMops.getSrtDescription(pd);
			SharedPrinterFields.getChangeDescription();
			if (LoadMops.isNonRiscosso(arg2, isRT2On())) {
				totaleNonRiscosso+=arg1;
			}
			if (SRTPrinterExtension.isPRT())
				arg2=s;
		}
		
		if (isFiscalAndSRTModel() || SRTPrinterExtension.isPRT() || isNotRTActivated())
		{
			if (HardTotals.TotalePagato.getLongX100() == 0)
			{
				String out = buildTotal ( "", arg0 );
				scriviLastTicket(out);
				
				DummyServerRT.groups = new ArrayList();
			}
		}

		System.out.println ( "MAPOTO xprintRecTotal"+arg0+" "+arg1+" "+arg2);
		if ((arg0 < 0) || (arg1 < 0)){
			System.out.println ( "MAPOTO MINORE DI ZERO");
			return;
		}
		
		setFlagVoidRefund(false);
		SharedPrinterFields.setprtDone();
		
		if ( isCurrentTicketZero() )
		{
			System.out.println ( "MAPOTO before currentFiscalTotal");
			String printerTotal = currentFiscalTotal(jpos.FiscalPrinterConst.FPTR_GD_CURRENT_TOTAL);
			System.out.println ( "MAPOTO after currentFiscalTotal <"+printerTotal+">");
			if ( fiscalPrinterDriver.checkCurrentTicketTotal(printerTotal, arg0) == false )
			{
				System.out.println ("FISCAL PRINTER TOTAL TICKET UNMATCHED");
				throw new JposException(9998);
			}
			
	        if (SRTPrinterExtension.isPRT())
	        	LotteryCommands.SendLotteryCode(SharedPrinterFields.Lotteria.getLotteryCode());
	        
	        progress_cash = true;
	        progress_amount = 0;
		}
		
		if (SRTPrinterExtension.isSRT() && SRTPrinterExtension.isNotLikeNonFiscalMode())
		{
			if (SRTPrinterExtension.isTenderMerge()){
		        DummyServerRT.srtRecTotal tendermerge = new DummyServerRT.srtRecTotal(arg2, arg1);
			}
			else{
				System.out.println ( "MAPOTO before driver.printRecTotal");
	        	fiscalPrinterDriver.printRecTotal(arg0 / 100, arg1 / 100, arg2);
				System.out.println ( "MAPOTO after driver.printRecTotal");
			}
		}
		else
		{
			boolean isEftPayment = false;
			
			if (SRTPrinterExtension.isPRT()) {
		        DummyServerRT.srtRecTotal tendermerge = null;
		        String srtdescription = "";
	        	String prefix = arg2;
				if (arg2.indexOf(",") >= 0)
					prefix = arg2.substring(0, arg2.indexOf(","));
				if (prefix == "" || prefix.length() == 0) {
					// il mop che si sta usando non è caricato nei 30 mop sulla printer, in questo caso lo consideriamo Contanti come fa la printer 
					prefix = ""+SharedPrinterFields.INDICE_CONTANTI;
				}
				srtdescription = LoadMops.getSrtDescription(LoadMops.getRCHDescriptionPayment(Integer.parseInt(prefix)));
		        tendermerge = new DummyServerRT.srtRecTotal(srtdescription, arg1);
		        isEftPayment = LoadMops.isPagElettronico(srtdescription);
			}
			
			System.out.println ( "MAPOTO before driver.printRecTotal - arg0="+arg0+" arg1="+arg1+" arg2="+arg2);
        	fiscalPrinterDriver.printRecTotal(arg0 / 100, arg1 / 100, arg2);
        	if (isEftPayment) {
        		// da specifiche per questa printer può esserci solo un pagamento eft e deve essere l'ultimo 
        		EftPos.OfflineEftHandling(arg1, EftPos.getEFTAuthorizationCode(arg1));
        	}
			System.out.println ( "MAPOTO after driver.printRecTotal");
			
			if (enabledLowerRoundedPay == -1) {
				long rounding = (long)(Rounding.roundingSimulation(((double)arg0/10000))*10000);
				System.out.println ( "MAPOTO after driver.printRecTotal - rounding="+rounding);
				if (fiscalPrinterDriver.isfwRT2enabled() && rounding != 0.) {
					progress_amount+=arg1;
					if (arg2.indexOf(",") >= 0)
						arg2 = arg2.substring(0, arg2.indexOf(","));
					System.out.println ( "MAPOTO after driver.printRecTotal - arg2="+arg2);
					if (arg2.length() > 0) {
						if (Integer.parseInt(arg2) != SharedPrinterFields.INDICE_CONTANTI)
							progress_cash = false;
					}
					else
						progress_cash = false;
					System.out.println ( "MAPOTO after driver.printRecTotal - progress_amount="+progress_amount+" progress_cash="+progress_cash);
					long stilltopay = arg0-progress_amount;
					System.out.println ( "MAPOTO after driver.printRecTotal - stilltopay="+stilltopay+" getCURRENTROUNDING="+RTConsts.getCURRENTROUNDING()+" onlycash="+progress_cash+" rounding="+rounding);
					if ((stilltopay > 0) && (stilltopay < 300) && (Math.abs(stilltopay) == Math.abs(rounding))) {
						if ((RTConsts.getCURRENTROUNDING() == RTConsts.FULLROUNDING) || (RTConsts.getCURRENTROUNDING() == RTConsts.NEGATIVEROUNDING)) {
							if (progress_cash == true) {
								System.out.println ( "MAPOTO before driver.printRecTotal("+arg0/100+","+stilltopay/100+","+SharedPrinterFields.INDICE_SCONTOAPAGARE+")");
					        	fiscalPrinterDriver.printRecTotal(arg0/100, stilltopay/100, ""+SharedPrinterFields.INDICE_SCONTOAPAGARE);
								System.out.println ( "MAPOTO after driver.printRecTotal("+arg0/100+","+stilltopay/100+","+SharedPrinterFields.INDICE_SCONTOAPAGARE+")");
							}
						}
					}
				}
			}
		}
		
		if (isFiscalAndSRTModel())
		{
			HardTotals.doPrintRecTotal(arg1);
			
			if (!SRTPrinterExtension.isTenderMerge()){
				String out = buildItem ( arg2, arg1 );
				scriviLastTicket(out);
			}
			
			//if (HardTotals.TotalePagato.getLongX100() == 0)
			if (HardTotals.TotalePagato.getLongX100() >= HardTotals.Totale.getLongX100())
			{
				if (SRTPrinterExtension.isTenderMerge()){
					for (int i=0; i<DummyServerRT.groups.size(); i++){
						System.out.println ( "MAPOTO before driver.printRecTotal");
			        	fiscalPrinterDriver.printRecTotal(arg0 / 100, ((DummyServerRT.srtRecTotal)DummyServerRT.groups.get(i)).getAmount() / 100, ((DummyServerRT.srtRecTotal)DummyServerRT.groups.get(i)).getDescr());
						System.out.println ( "MAPOTO after driver.printRecTotal");
						
						String out = buildItem ( ((DummyServerRT.srtRecTotal)DummyServerRT.groups.get(i)).getDescr(), ((DummyServerRT.srtRecTotal)DummyServerRT.groups.get(i)).getAmount() );
						scriviLastTicket(out);
					}
				}
				
				String out = buildChange ( "", (HardTotals.TotalePagato.getLongX100()-HardTotals.Totale.getLongX100()) );
				scriviLastTicket(out);
				
				out = buildTotalIva ( "", RoungickTax.getTotalTaxLong() );
				printNormal_ejoff(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, out);
				printNormal_ejoff(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
				
				out = buildTotalAmount ( "", HardTotals.Totale.getLongX100() );
				printNormal_ejoff(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, out);
				out = buildAmountPaid ( "", HardTotals.Totale.getLongX100()-totaleNonRiscosso );
				printNormal_ejoff(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, out);
				printNormal_ejoff(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			}
		}
		
		if (SRTPrinterExtension.isPRT())
		{
			HardTotals.doPrintRecTotal(arg1);
			
			if (HardTotals.TotalePagato.getLongX100() >= HardTotals.Totale.getLongX100()+(long)(SharedPrinterFields.RoundingRT*10000))
			{
				try {
					// scrittura file lastticket
					
					String out = "";
					
					long resto = HardTotals.TotalePagato.getLongX100()-HardTotals.Totale.getLongX100();
					
					long rounding = 0;
					if (RTTxnType.isSaleTrx() && progress_cash && RTConsts.getCURRENTROUNDING() != RTConsts.ROUNDINGDISABLE)
						rounding = (long)(Rounding.roundingSimulation(((double)arg0/10000))*10000);
					
					if (rounding > 0) {		// a favore del pdv
						if (RTConsts.getCURRENTROUNDING() == RTConsts.NEGATIVEROUNDING)
							rounding = 0;
						resto-=rounding;
					}
					else if (rounding < 0) {	// a favore del cliente
						if (RTConsts.getCURRENTROUNDING() == RTConsts.POSITIVEROUNDING)
							rounding = 0;
						if (HardTotals.TotalePagato.getLongX100() > HardTotals.Totale.getLongX100())
							resto+=Math.abs(rounding);
					}
					
					if ((resto == 0) && (rounding < 0))
						resto+=Math.abs(rounding);
					
					if (RTTxnType.isSaleTrx())
					{
						if (resto > 0) {
							out = buildChange ( "", resto );
							scriviLastTicket(out);
						}
					}
					
					out = buildTotalIva ( "", RoungickTax.getTotalTaxLong() );
					scriviLastTicket(out);
					
					if (RTTxnType.isSaleTrx())
					{
						for (int i=0; i<DummyServerRT.groups.size(); i++) {
							if (((DummyServerRT.srtRecTotal)DummyServerRT.groups.get(i)).getAmount() == HardTotals.Totale.getLongX100()) {
								if (rounding < 0) {
									out = buildItem ( ((DummyServerRT.srtRecTotal)DummyServerRT.groups.get(i)).getDescr(), ((DummyServerRT.srtRecTotal)DummyServerRT.groups.get(i)).getAmount() );
								}
								else {
									out = buildItem ( ((DummyServerRT.srtRecTotal)DummyServerRT.groups.get(i)).getDescr(), ((DummyServerRT.srtRecTotal)DummyServerRT.groups.get(i)).getAmount()+rounding );
								}
							}
							else {
								out = buildItem ( ((DummyServerRT.srtRecTotal)DummyServerRT.groups.get(i)).getDescr(), ((DummyServerRT.srtRecTotal)DummyServerRT.groups.get(i)).getAmount() );
							}
							scriviLastTicket(out);
						}
						
						if (rounding < 0) {
							out = buildItem(LastTicket.getDisconpay(), Math.abs(rounding));
							scriviLastTicket(out);
						}
						
						//out = buildTotalAmount ( "", HardTotals.Totale.getLongX100() );
						//scriviLastTicket(out);
						
						long importopagato = HardTotals.Totale.getLongX100()-totaleNonRiscosso;
						if (rounding > 0)
							importopagato+=rounding;
						else if (rounding < 0)
							importopagato+=rounding;
						out = buildAmountPaid ( "", importopagato );
						scriviLastTicket(out);
					}
					
					LocalTimestamp lts = TransactionSale.getEndData();
					String date = Sprint.f("%02d-%02d-%d %02d:%02d",new Integer(lts.getDay()),new Integer(lts.getMonth()+1),new Integer(lts.getYear()+1900),new Integer(lts.getHour()),new Integer(lts.getMinute())); 
					out = ALINER.substring(0, (int)((RTConsts.setMAXLNGHOFLENGTH()-date.length())/2))+date;
					scriviLastTicket(out);
					
					out = LastTicket.getDocnum();
					scriviLastTicket(out);
					
					if ((SharedPrinterFields.Lotteria.getLotteryCode() != null) && (SharedPrinterFields.Lotteria.getLotteryCode().length() > 0)) {
						out = LastTicket.getLottery()+SharedPrinterFields.Lotteria.getLotteryCode();
						scriviLastTicket(out);
					}

					if (SharedPrinterFields.Lotteria.getILotteryCode().length() > 0) {
						out = LastTicket.getLottery()+SharedPrinterFields.Lotteria.getILotteryCode();
						printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, out);
						EjCommands ej = SharedPrinterFields.getEjCommands();
						ej.printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, out+R3define.CrLf);
					}
					
					out = ALINER.substring(0, (int)((RTConsts.setMAXLNGHOFLENGTH()-SharedPrinterFields.RTPrinterId.length())/2))+SharedPrinterFields.RTPrinterId;
					scriviLastTicket(out);
					
					if (RTTxnType.isSaleTrx())
					{
						out = ALINER.substring(0, (int)((RTConsts.setMAXLNGHOFLENGTH()-LastTicket.getPaymdetails().length())/2))+LastTicket.getPaymdetails();
						scriviLastTicket(out);
						
						for  ( int i = 0; i < SharedPrinterFields.a.size() ; i++ )
						{
							MethodE M = (MethodE)SharedPrinterFields.a.get(i);
							if (M.getM() == R3define.fprintRecTotal){
			                   if (SharedPrinterFields.lineePagamento != null) {
			                        for (Iterator iterator = SharedPrinterFields.lineePagamento.keySet().iterator(); iterator.hasNext();) {
			                           String k = (String) iterator.next();
			                           BigDecimal bd = new BigDecimal((Double)SharedPrinterFields.lineePagamento.get(k));
			                           bd = bd.setScale(2, RoundingMode.HALF_DOWN);
			                           Double valDouble = new Double(bd.doubleValue() * 10000);
			                           long val =  valDouble.longValue();                                        
			                           if (val == HardTotals.Totale.getLongX100())
			                        	   out = buildItem ( k, val+rounding);
			                           else
			                        	   out = buildItem ( k, val);
			                           scriviLastTicket(out);
			                        }
			                        SharedPrinterFields.lineePagamento = null;
			                    }else {                        
			                    	if (Long.parseLong(M.getV().get(1).toString()) == HardTotals.Totale.getLongX100()) {
										if (rounding < 0)
				                    		out = buildItem ( M.getV().get(2).toString().substring(3), Long.parseLong(M.getV().get(1).toString()) /*- resto*/ );
										else
											out = buildItem ( M.getV().get(2).toString().substring(3), Long.parseLong(M.getV().get(1).toString())+rounding /*- resto*/ );
			                    	}
			                    	else {
			                    		out = buildItem ( M.getV().get(2).toString().substring(3), Long.parseLong(M.getV().get(1).toString()) /*- resto*/ );
			                    	}
									scriviLastTicket(out);
			                    }
							}
						}
						
						if (rounding != 0.0) {
							long val =  rounding;
							if (val > 0) {
								out = buildItem (LastTicket.getRoundingup1(), val);
							}
							else {
								out = buildItem (LastTicket.getRoundingdown1(), Math.abs(val));
							}
							scriviLastTicket(out);
						}
					}
					
				} catch (Exception e) {
					System.out.println("LastTicket error: "+e.getMessage());
				}
			}
		}
		
		if (isNotRTActivated())
		{
			HardTotals.doPrintRecTotal(arg1);
			String out = buildItem ( arg2, arg1 );
			scriviLastTicket(out);
			
			if (HardTotals.TotalePagato.getLongX100() >= HardTotals.Totale.getLongX100())
			{
				out = buildChange ( "", (HardTotals.TotalePagato.getLongX100()-HardTotals.Totale.getLongX100()) );
				scriviLastTicket(out);
			}
		}
		
	}
	
	public void printRecVoidItem(String s, long l, int i, int j, long l1, int kIvaPolipos) throws JposException {
		System.out.println ( "MAPOTO-EXEC PRINT VOID ITEM s="+s );
		System.out.println ( "MAPOTO-EXEC PRINT VOID ITEM l="+l);
		System.out.println ( "MAPOTO-EXEC PRINT VOID ITEM i="+i );
		System.out.println ( "MAPOTO-EXEC PRINT VOID ITEM j="+j );
		System.out.println ( "MAPOTO-EXEC PRINT VOID ITEM l1="+l1);
		System.out.println ( "MAPOTO-EXEC PRINT VOID ITEM k="+kIvaPolipos );
		
		if (SRTPrinterExtension.isSRT())
		{
			String ivadesc = DicoTaxLoad.getDTO(kIvaPolipos).getShortdescription();
			
			s = addIva(s, RTConsts.getMAXITEMDESCRLENGTH()-1, ivadesc);
		}
		
		if (SRTPrinterExtension.isPRT() && TaxData.isOmaggio(kIvaPolipos) && fiscalPrinterDriver.isfwRT2enabled())
		{
			printRecOmaggio(s, l, i, j, l1, kIvaPolipos);
			return;
		}
		
		int k = DicoTaxToPrinter.getFromPoliposToPrinter(kIvaPolipos);
		if ((DicoTaxLoad.isIvaAllaPrinter()) && (k == SharedPrinterFields.VAT_N4_Index))
			k = Integer.parseInt(SharedPrinterFields.VAT_N4_Dept);
		
		System.out.println ( "MAPOTO-EXEC PRINT VOID ITEM k="+k );
		
		String m = "CORREZIONE STORNO";
		
		System.out.println ( "MAPOTO-EXEC PRINT VOID ITEM "+s );
		
		if (isFlagVoidRefund(l, l1))
		{
			m = "CORREZIONE RESO";
			if (i > 0)
				i=i*(-1);
		}
		
	      final int MAXLNGHOFDESCR = 17;
	      String new_description = s.replaceAll("(?i)TOTALE", "TOTA..");
	      s = new_description;
	      new_description = s.replaceAll("(?i)TOTAL", "TOTA.");
	      
			String k2desc="";
			for(int k2idx=0; k2idx<new_description.length(); k2idx++)
			{
		        if ((int)(new_description.charAt(k2idx)) >= 128)
		            k2desc = k2desc + ".";
		        else
		            k2desc = k2desc + new_description.charAt(k2idx);
			}
			new_description=k2desc;
				
			String newMsg = (new_description.length()<= MAXLNGHOFDESCR)? new_description: 
	                                                               new_description.substring (0, MAXLNGHOFDESCR);			      
		    if (SRTPrinterExtension.isSRT()) newMsg = new_description;
		      
	      if ( i < 0)
	      {
	    	  long a;
    		  if (!isFlagVoidRefund(l, l1))
    			  a = l1 * i / -1000;
    		  else
    			  a = l1;
			  fiscalPrinterDriver.printRecItem(m, a / 100, 0, k, 0, "");
	    	  setFlagVoidRefund(false);
	    	  return;
	      }
	      if ( l ==  0 )
	    	  pleaseVoidItem (newMsg, l1, i, j, l, k);
	      else
	    	  pleaseVoidItem (newMsg, l, i, j, l1, k); 
		
		setFlagVoidRefund(false);
		
		if (isFiscalAndSRTModel() || SRTPrinterExtension.isPRT() || isNotRTActivated())
		{
			long chi = ( l ==  0 ) ? l1 : l;
		    int quanti = i / 1000;
  		  	if (!isFlagVoidRefund(l, l1))
  		  		chi = chi * quanti;
  		  	else
  		  		chi=chi*(-1);
			if (i < 0)
			{
				chi=chi*(-1);
				return;
			}
			HardTotals.doPrintRecVoid(chi);
			
	    	scriviLastTicket(buildItemVoid ( s, chi ));
		}
	}

	private void pleaseVoidItem (String s, long l, int i, int j, long l1, int k) throws JposException
	{
		System.out.println ( "MAPOTO-EXEC PLEASEVOIDITEM"+s );
		  
		if ( PrinterType.getPrinterJavaPosModel() == PrinterType.JAVAPOS113 )
		{
			System.out.println ( "JavaPos 1.13 - Printer != TH230: unimplemented version" );
		}
		else
		{
	    	if (l > 0){
	    		//fiscalPrinterDriver.printRecVoidItem (s, l / 100, i / 1000, j, l1, k);
	    		long prezzo = (l / 100) * (i / 1000);
	    		int qta = 1;
	    		fiscalPrinterDriver.printRecVoidItem (s, prezzo, qta, j, l1, k);
	    	}
	    	else
	    		printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, "Storno "+s);
		}
	}
	
	public void printReport(int id, String ini,String end) throws JposException {
		printReportInHouse(id,ini,end);
	}

	private void printReportInHouse(int reportType, String startNum, String endNum)
	{
		System.out.println ( "MAPOTO-printReportInHouse in" );
		
		if (!(SRTPrinterExtension.isSRT() && reportType == Report.PRINTSOMERECEIPTBYDATE)) {
			Report.printReportInHouse(reportType, startNum, endNum);
			return;
		}
			
  		if (reportType == Report.PRINTFISCALMEMBYDATE)
  			reportType = jpos.FiscalPrinterConst.FPTR_RT_DATE;
  
		switch (reportType)
		{
			case Report.PRINTSOMERECEIPTBYDATE:
				r3PrintSomeReceiptByDate (startNum, endNum);
				break;

			case Report.PRINTALLRECEIPTBYDATE:
			case Report.DOWNLOADONFILE:
			case jpos.FiscalPrinterConst.FPTR_RT_ORDINAL:
			case jpos.FiscalPrinterConst.FPTR_RT_DATE:
			case Report.PRINTTOTFISCALMEMBYDATE:
			case Report.INITJOURNAL:
				MessageBox.showMessage("Funzione non valida", null, MessageBox.OK);
				break;
			
			default:
				break;
		}
		
		System.out.println ( "MAPOTO-printReportInHouse out" );
		return;
	}
	
	private void r3PrintSomeReceiptByDate (String startNum, String endNum)
	{
		//startNum = "ggmmaaaaNNNN";
		//endNum   = "ggmmaaaaNNNN";
		
		System.out.println ( "r3PrintSomeReceiptByDate in: "+startNum+"-"+endNum );
		
		Files.copyFile(LastTicket.getLastticket(),lastticketsaved);
		
		rtsTrxBuilder.storerecallticket.RecallTicket RT = new rtsTrxBuilder.storerecallticket.RecallTicket(startNum, endNum);
		
		leggiFile();
		
		Files.moveFile(lastticketsaved,LastTicket.getLastticket());
		
		System.out.println ( "r3PrintSomeReceiptByDate out" );
	}
	
	public void printXReport() throws JposException {
		fiscalPrinterDriver.printXReport();
	}

	public void printZReport() throws JposException {
		System.out.println ( "MAPOTO-EXEC PRINTREPORTZ" );
		
		if (SRTPrinterExtension.isRTActivated() == true) {
			DummyServerRT.setSRTTillID();
		}
		
  	    if (SRTPrinterExtension.isSRT())
  	    {
  	    	DummyServerRT.pleaseDoDailyClosure();
			
			System.out.println("printZReport - CurrentFiscalClosure = "+DummyServerRT.CurrentFiscalClosure);
			System.out.println("printZReport - CurrentReceiptNumber = "+DummyServerRT.CurrentReceiptNumber);
			System.out.println("printZReport - CurrentDailyAmount = "+DummyServerRT.CurrentDailyAmount);
			int OldCurrentFiscalClosure = DummyServerRT.CurrentFiscalClosure;
			int retry = 0;
			while (true)
			{
				retry++;
				
				DummyServerRT.pleaseDoServerInfo();
				
				if (DummyServerRT.CurrentFiscalClosure > OldCurrentFiscalClosure)
					break;
				
				System.out.println("printZReport - OldCurrentFiscalClosure = " + OldCurrentFiscalClosure + " - CurrentFiscalClosure = " + DummyServerRT.CurrentFiscalClosure + " - retry = " + retry);
				
				if (retry == 5)
					break;
				
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
			}
			if (DummyServerRT.CurrentFiscalClosure <= OldCurrentFiscalClosure){
				System.out.println("printZReport - OldCurrentFiscalClosure = " + OldCurrentFiscalClosure + " - CurrentFiscalClosure = " + DummyServerRT.CurrentFiscalClosure + " - retry = " + retry);
				DummyServerRT.CurrentFiscalClosure++;
				DummyServerRT.CurrentReceiptNumber = "1";
				DummyServerRT.CurrentDailyAmount = "000";
			}
			System.out.println("printZReport - CurrentFiscalClosure = "+DummyServerRT.CurrentFiscalClosure);
			System.out.println("printZReport - CurrentReceiptNumber = "+DummyServerRT.CurrentReceiptNumber);
			System.out.println("printZReport - CurrentDailyAmount = "+DummyServerRT.CurrentDailyAmount);
  	    }
  	    
  	    setFlagVoidRefund(false);
  	    
		if (XRData.MonitorRTisActive()) {
			if (SRTPrinterExtension.isPRT()) {
				
				if (monitorRT == null) monitorRT = new MonitorRT();
				
				try {
					monitorRT.preMonitorRT();
				} catch (Exception e) {
					monitorRT.logMRT("Exception : "+e.getMessage());
				}
			}
		}
		
  	    fiscalPrinterDriver.printZReport();
  	    
		if (isFiscalAndSRTModel() || SRTPrinterExtension.isPRT())
		{
			HardTotals.ProAzz.add(1);
			HardTotals.delReportZ();
		}
		
		if (XRData.MonitorRTisActive()) {
			if (SRTPrinterExtension.isPRT()) {
				
				if (monitorRT == null) monitorRT = new MonitorRT();
				
				try {
			    	if (DummyServerRT.CurrentFiscalClosure == 0) {
		    	        int[] ai = new int[1];
		    	        String[] as = new String[1];
		                getData(FiscalPrinterConst.FPTR_GD_Z_REPORT, ai, as);
		                DummyServerRT.CurrentFiscalClosure = Integer.parseInt(as[0])+1;
			    	}
			    	
			    	String[] date = new String[1];
			    	fiscalPrinterDriver.getDate(date);
			    	
					monitorRT.doMonitorRT(SharedPrinterFields.Printer_IPAddress, DummyServerRT.CurrentFiscalClosure, SharedPrinterFields.RTPrinterId, date[0], PosApp.getTillNumber());
				} catch (Exception e) {
					monitorRT.logMRT("Exception : "+e.getMessage());
				}
			}
		}
		
		if (SRTPrinterExtension.isPRT()) {
			if (DummyServerRT.CurrentFiscalClosure == 0) {
		        int[] ai = new int[1];
		        String[] as = new String[1];
	            fiscalPrinterDriver.getData(FiscalPrinterConst.FPTR_GD_Z_REPORT, ai, as);
	            DummyServerRT.CurrentFiscalClosure = Integer.parseInt(as[0])+1;
			}
			else
				DummyServerRT.CurrentFiscalClosure++;
		}
	}

	public void setDate(String arg0) throws JposException {
       System.out.println("setDate in - s=" + arg0);
       if (fiscalPrinterDriver.getDayOpened())
       {
    	   System.out.println("setDate - Fiscal day opened: don't do it.");
    	   return;
       }
	   String s1 = arg0.substring(8, 10) + arg0.substring(5, 7) + arg0.substring(0, 4)
	                + arg0.substring(11, 13) + arg0.substring(14, 16);
	   fiscalPrinterDriver.setDate(s1);
	   System.out.println("setDate out");
	}

	public void setHeaderLine(int arg0, String arg1, boolean arg2) throws JposException {
		if (fiscalPrinterDriver.getDayOpened())
		{
			System.out.println("setHeaderLine - Fiscal day opened: don't do it.");
			return;
		}
		if (arg0 == 1)
		{
			SetLogo(LOGO_FILE, LOGO_NUMBER);
		}
		int maxHeaderLine = fiscalPrinterDriver.getNumHeaderLines();
		if(arg0 > maxHeaderLine) 
			return;

		String s = arg1;
		arg1 = cleanLine(s);
		   
		fiscalPrinterDriver.setHeaderLine(arg0-1, (arg1.length()>36 ? arg1.substring(0, 36) : arg1), arg2);
	}

	public void setTrailerLine(int arg0, String arg1, boolean arg2) throws JposException {
		String s = arg1;
		arg1 = cleanLine(s);
		   
		fiscalPrinterDriver.setTrailerLine(arg0,arg1,arg2);
	}
	
	public static int getState() throws JposException
	{
       	 System.out.println("MAPOTO-FISICAL_PRINTER-STATE " + getFiscalPrinterState());
		 return getFiscalPrinterState();
	}
	
	public boolean getCoverOpen()
	{
		boolean retv = false;
		try
		{
			retv = fiscalPrinterDriver.getCoverOpen();
		}
		catch (jpos.JposException e)
		{
			System.out.println("getCoverOpen - exception:"+e.getMessage());
		}
		return retv;
	}
	
	public boolean getJrnEmpty()
	{
		boolean retv = false;
		try
		{
			retv = fiscalPrinterDriver.getJrnEmpty();
		}
		catch (jpos.JposException e)
		{
			System.out.println("getJrnEmpty - exception:"+e.getMessage());
		}
		return retv;
	}
	
	public boolean getJrnNearEnd()
	{
		boolean retv = false;
		try
		{
			retv = fiscalPrinterDriver.getJrnNearEnd();
		}
		catch (jpos.JposException e)
		{
			System.out.println("getJrnNearEnd - exception:"+e.getMessage());
		}
		return retv;
	}
	
	public boolean getRecEmpty()
	{
		boolean retv = false;
		try
		{
			retv = fiscalPrinterDriver.getRecEmpty();
		}
		catch (jpos.JposException e)
		{
			System.out.println("getRecEmpty - exception:"+e.getMessage());
		}
		return retv;
	}
	
	public boolean getRecNearEnd()
	{
		boolean retv = false;
		try
		{
			retv = fiscalPrinterDriver.getRecNearEnd();
		}
		catch (jpos.JposException e)
		{
			System.out.println("getRecNearEnd - exception:"+e.getMessage());
		}
		return retv;
	}
	
	public boolean getSlipNearEnd()
	{
		boolean retv = false;
		try
		{
			retv = fiscalPrinterDriver.getSlipNearEnd();
		}
		catch (jpos.JposException e)
		{
			System.out.println("getSlipNearEnd - exception:"+e.getMessage());
		}
		return retv;
	}
	
	protected static void getDate(String[] arg0) throws JposException {
		try
		{
			fiscalPrinterDriver.getDate(arg0);
		}
		catch (jpos.JposException e)
		{
			System.out.println("getDate - exception:"+e.getMessage());
		}
	}
	
	public void getData(int i, int ai[], String as[])
	{
		fiscalPrinterDriver.getData(i, ai, as);
	}
	
    /* printer commands - End
     * 
     */
	
	public static String checkEJStatus()
	{
		return fiscalPrinterDriver.checkEJStatus();
	}
	   
    private String buildItem ( String desc, long price )
    {
        int MAXLNGHOFLENGTH = RTConsts.setMAXLNGHOFLENGTH();
    	return ( buildbuild(MAXLNGHOFLENGTH,desc,price ));
    }
    
    private String buildItemVoid ( String de, long price )
    {
        int MAXLNGHOFLENGTH = RTConsts.setMAXLNGHOFLENGTH();
        String desc = de;
    	String lo =  ( buildbuild(MAXLNGHOFLENGTH,desc,price ));
    	return ( "Storno\n"+lo+"-" );
    }
    
    private String buildItemRefund ( String de, long price )
    {
        int MAXLNGHOFLENGTH = RTConsts.setMAXLNGHOFLENGTH();
        String desc = de;
    	String lo =  ( buildbuild(MAXLNGHOFLENGTH,desc,price ));
    	return ( "Reso\n"+lo+"-" );
    }
    
   	private String buildbuild ( int mx, String desc, long price )
    {
   		System.out.println("LONGPRICE:"+price);
        int MAXLNGHOFDESCR = 28;
        String newMsg = (desc.length()<= MAXLNGHOFDESCR)? desc: desc.substring (0, MAXLNGHOFDESCR);

        String p = (price == 0 ) ? "000":""+price;
        while ( p.length() < 3 )
        	p = p + "0";
        p = p.substring(0,p.length()-2);
        if ( p.length() < 2 )
        	p = "0"+p;
        if ( p.length() < 3 )
        	p = "0."+p;
        else
        	p = p.substring ( 0,p.length()-2 ) + "." + p.substring ( p.length()-2 );
    	return ( newMsg + ALINER.substring(0,mx-newMsg.length()-p.length())+ p);
    }
   	
    private String buildTotal ( String de, long price )
    {
        int MAXLNGHOFLENGTH = RTConsts.setMAXLNGHOFLENGTH();
        String desc = "TOTALE COMPLESSIVO "+de;
    	return ( buildbuild(MAXLNGHOFLENGTH,desc,price ));
    }
    
    private String buildChange ( String de, long price )
    {
        int MAXLNGHOFLENGTH = RTConsts.setMAXLNGHOFLENGTH();
        String desc = "Resto "+de;
    	return ( buildbuild(MAXLNGHOFLENGTH,desc,price ));
    }
    
    private String buildItemAdjustment ( String de, long price )
    {
        int MAXLNGHOFLENGTH = RTConsts.setMAXLNGHOFLENGTH();
        String desc = RTConsts.SCONTO+de;
    	String lo =  ( buildbuild(MAXLNGHOFLENGTH,desc,price ));
    	return ( lo+"-" );
    }
    
    private String buildSubtotalAdjustment ( String de, long price )
    {
        int MAXLNGHOFLENGTH = RTConsts.setMAXLNGHOFLENGTH();
        String desc = RTConsts.SCONTO+de;
    	String lo =  ( buildbuild(MAXLNGHOFLENGTH,desc,price ));
    	return ( lo+"-" );
    }
    
    private String buildTotalIva ( String de, long price )
    {
    	if (RTTxnType.isVoidTrx(RTTxnType.getTypeTrx())){
    		price = (long)Math.rint((Math.rint((DummyServerRT.getOld_receiptVAT())*100)/100)*10000);
    	}
        int MAXLNGHOFLENGTH = RTConsts.setMAXLNGHOFLENGTH();
        String desc = RTConsts.DICUIIVA+de;
        String out = buildbuild(MAXLNGHOFLENGTH,desc,price );
        EjCommands.Write(out, true);
    	return ( out );
    }
    
    private String buildTotalAmount ( String de, long price )
    {
        int MAXLNGHOFLENGTH = RTConsts.setMAXLNGHOFLENGTH();
        String desc = RTConsts.IMPORTOTOTALE+de;
        String out = buildbuild(MAXLNGHOFLENGTH,desc,price );
        EjCommands.Write(out, true);
    	return ( out );
    }
    
    private String buildAmountPaid ( String de, long price )
    {
        int MAXLNGHOFLENGTH = RTConsts.setMAXLNGHOFLENGTH();
        String desc = RTConsts.IMPORTOPAGATO+de;
        String out = buildbuild(MAXLNGHOFLENGTH,desc,price );
        EjCommands.Write(out, true);
    	return ( out );
    }
    
	private static String buildItemPlus ( String discount, String ivadesc )
	{
		int a = discount.lastIndexOf('-');
		String amt = discount.substring(a);
		
		String s = discount.substring(0, a);
		for (a=0; a<s.length(); a++){
			if (s.charAt(a) != ' ')
				break;
		}
		s = s.substring(a);
		for (a=s.length()-1; a>=0; a--){
			if (s.charAt(a) != ' ')
				break;
		}
		String des = s.substring(0, a+1);
		
		return(des + ALINER.substring(0,RTConsts.getMAXITEMDESCRLENGTH()-des.length()-ivadesc.length()-amt.length()-1)+ amt);
	}
    
    private String buildSRTSubtotalAdjustment ( String de, long price )
    {
        int MAXLNGHOFLENGTH = RTConsts.setMAXLNGHOFDISCOUNT();
        String desc = RTConsts.SCONTO+de;
    	String lo =  ( buildbuild(MAXLNGHOFLENGTH,desc,price ));
    	return ( lo+"-" );
    }
    
	public void resetAndClear() throws JposException {
		fiscalPrinterDriver.resetPrinter();
		fiscalPrinterDriver.clearError();
	}
	
	public void Reset_I() throws JposException {
	}
	
	public void Reset_II() throws JposException {
		fiscalPrinterDriver.resetPrinter();
	}
	
	public void Reset_III(boolean doit) {
		while ( true )
		{
			try
			{					 
				System.out.println ( "MAPOTO-RESET AFTERCLEAR INIT <"+fiscalPrinterDriver.getState()+">");
				resetAndClear();
				System.out.println ( "MAPOTO-RESET AFTERCLEAR END <"+fiscalPrinterDriver.getState()+">");
				break;
			}
			catch ( JposException e )
			{
				System.out.println ( "MAPOTO-RESETAFTERCLEAR <"+e.toString()+">");
				OperatorDisplay.pleaseDisplayWait(R3define.PRINTERVERIFY, 400);
			}
		}
	}
	
	public void AnnullaResoRT_Posponed()
	{
		if (!isFlagsVoidTicket())
			MessageBox.showMessage(RTConsts.RESONONCORRETTO, null, MessageBox.OK);
		
		try {
			resetAndClear();
			RTTxnType.setSaleTrx();
			SharedPrinterFields.setMonitorState();
			CanPost.setCanPost(true);
			if (SharedPrinterFields.isfiscalEJenabled()) ForFiscalEJFile.writeToFile(RTConsts.RESOANNULLATO);
		} catch (JposException e) {
			System.out.println("AnnullaResoRT_Posponed - Exception : " + e.getMessage());
		}
		
		SharedPrinterFields.setMyReply(R3define.PRINTER_REFUND_ERR);
		
		if (!isFlagsVoidTicket())
			MessageBox.showMessage(RTConsts.OPERAZIONEANNULLATA, null, MessageBox.OK);
		
		R3define.setRefundResult(R3define.FAILURE);
	}
	
	public static String currentFiscalTotal(int what) throws JposException
	{
			int [] arr_optArgs = new int[1];
			String [] arr_data = new String[1];
			arr_optArgs [0] = 1;
			arr_data    [0] = new String ();

			fiscalPrinterDriver.getData (what, arr_optArgs, arr_data);
	  	    if (what == jpos.FiscalPrinterConst.FPTR_GD_CURRENT_TOTAL)
	  	    	iconic.mytrade.gutenberg.jpos.printer.service.FiscalPrinterDataInformation.setNewDataAvailable(false);
	  	    System.out.println ( "RISPOSTA <"+what+"><"+arr_data[0]+">");
		    return ( arr_data[0] );
	}
	
	private static void cleanDailyTotal ( )
	{
		dailyTotal = (double)-1.0;	// distrugge il totale precedente
	}
	
	public static boolean niceDailyTotal ( )
	{
		return ((dailyTotal == (double)-1.0) ? false : true );
	}
	
	private static void storeDailyTotal ( String In )
	{
		dailyTotal = (Double.valueOf(In).doubleValue()/100);
		currentTicket = (double)0.0;
		if ( provaErrori )
			countErrori++;
	}
	
	public static boolean checkStoredDailyTotal(String Pr)
	{
		double	fromPrinter = Double.valueOf(Pr).doubleValue()/100;
		System.out.println("checkStoredDailyTotal - fromPrinter="+fromPrinter+" - dailyTotal="+dailyTotal);
		return ( fromPrinter <= dailyTotal );
	}
	
	public static boolean timeToRecoveryTotal()
	{
		boolean res = false;
		try
		{
			String Total = currentFiscalTotal(jpos.FiscalPrinterConst.FPTR_GD_DAILY_TOTAL);
		    if ( fiscalPrinterDriver.checkCurrentDailyTotal(Total) == true )
		    	res = true;
		}
		catch ( Exception e )
		{
			System.out.println("MAPOTO-PrinterRecovery: getDailyTotal <"+e.toString()+">");
			res = false;
		}
		return ( res );
	}
	
	  public static int resumeErrorI()
	  {
		  try
		  {
			  if ((SharedPrinterFields.isInTicket() == false) || 
				((SharedPrinterFields.isInTicket() == true) && (getState() == jpos.FiscalPrinterConst.FPTR_PS_FISCAL_RECEIPT_ENDING)))
			  {
				  String Total = currentFiscalTotal(jpos.FiscalPrinterConst.FPTR_GD_DAILY_TOTAL);
				  if ( fiscalPrinterDriver.checkCurrentDailyTotal(Total) == true )
				  {
					  SharedPrinterFields.a = new ArrayList();
					  return (0);
				  }
			  }
		  }
		  catch (JposException e)
		  {
			  e.printStackTrace();
			  return (-1);
		  }
			  
		  return (1);
	  }
	   
		private static String addIva(String desc, int max, String iva)
		{
			if (desc.length() > max)
				desc = desc.substring(0,max);
			
			for (int i=desc.length()+1; i<=max; i++)
				desc=desc+" ";
			
			desc=desc.substring(0,desc.length()-iva.length()-1)+" "+iva;
			
			return desc;
		}
		
		private void toDayDate() throws JposException 
		{
			String anno;
			String mese;
			String giorno;
			String ore;
			String min;
			String sec;
			
			GregorianCalendar gc = new GregorianCalendar();
			
			anno = gc.get(gc.YEAR) + "";
			mese = alligne100( gc.get(gc.MONTH) + 1 );
			giorno = alligne100( gc.get(gc.DATE) );
			
			ore = alligne100( (gc.get(gc.AM_PM) == gc.PM) ? gc.get(gc.HOUR)+12 : gc.get(gc.HOUR) );
			min = alligne100( gc.get(gc.MINUTE) );
			sec = alligne100( gc.get(gc.SECOND) );
			
			DummyServerRT.setCurrent_dateTime(giorno, mese, anno, ore, min, sec);
		}
		
		private void toDayDate(String num) throws JposException 
		{
			String anno;
			String mese;
			String giorno;
			String ore;
			String min;
			String sec;
			
			GregorianCalendar gc = new GregorianCalendar();
			
			anno = gc.get(gc.YEAR) + "";
			mese = alligne100( gc.get(gc.MONTH) + 1 );
			giorno = alligne100( gc.get(gc.DATE) );
			
			ore = alligne100( (gc.get(gc.AM_PM) == gc.PM) ? gc.get(gc.HOUR)+12 : gc.get(gc.HOUR) );
			min = alligne100( gc.get(gc.MINUTE) );
			sec = alligne100( gc.get(gc.SECOND) );
			
			String today = giorno + "/" + mese + "/" + anno + "      " + ore + ":" + min + ":" + sec;
			String fiscalnumber = "N."+Sprint.f("%04d", String.valueOf(Integer.parseInt(num)));
			String fiscalclosure = "RZ."+Sprint.f("%04d", String.valueOf(DummyServerRT.CurrentFiscalClosure));
			printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, today + "  " + fiscalclosure + " " + fiscalnumber);
			DummyServerRT.setCurrent_dateTime(giorno, mese, anno, ore, min, sec);
		}
		
		private String alligne100( int who )
		{
			String res = Integer.toString(who + 100 );
			return ( res.substring(1) );
		}
		
		private void endTicket(String FiscalNum) throws JposException
		{
			if (isFlagsVoidTicket())
				return;
			
			if (!RTTxnType.isVoidTrx())
			{
				printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
				printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, RTConsts.IVADETAILS);
				SSCO = RoungickTax.getVatTable(true);
				if (SSCO != null) {
					boolean isEsente = false;
					for ( int index = 0 ; index < SSCO.size(); index++  ){
						VatInOutHandling vatInOutH = (VatInOutHandling) SSCO.get(index);
						System.out.println("vatInOutH:"+vatInOutH);
						if (vatInOutH.getTotalAmount() > 0) {
						 String out = buildItem ( vatInOutH.getRtVatCode()+" - "+vatInOutH.getShortDescription(), (long)(vatInOutH.getTotalAmount()*10000));
		                 printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, out);
						}
						if (vatInOutH.getRate() == 0) {
							isEsente = true;
						}
					}							
					printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
					if (isEsente) {
						for ( int index = 0 ; index < SSCO.size(); index++  ){
							VatInOutHandling vatInOutH = (VatInOutHandling) SSCO.get(index);
							if (vatInOutH.getTotalAmount() > 0 && vatInOutH.getRate() == 0) {
							 String out = vatInOutH.getShortDescription() + " = " + vatInOutH.getFullDescription();
			                 printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, out);
							}
						}
					}
				}
			}
			
			printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			toDayDate(FiscalNum);
			printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			
			//CurrentReceiptNumber = ""+(Integer.parseInt(FiscalNum)+1);
		}
		
		private void endTicketSRT(String SRTServerID, String SRTTillID, String currentFingerPrint) throws JposException
		{
			if (isFlagsVoidTicket())
				return;
			
			if (!RTTxnType.isVoidTrx())
			{
				printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, RTConsts.PAYMDETAILS);
				for  ( int i = 0; i < SharedPrinterFields.a.size() ; i++ )
				{
					MethodE M = (MethodE)SharedPrinterFields.a.get(i);
					if (M.getM() == R3define.fprintRecTotal){
	                   if (SharedPrinterFields.lineePagamento != null) {
	                        for (Iterator iterator = SharedPrinterFields.lineePagamento.keySet().iterator(); iterator.hasNext();) {
	                           String k = (String) iterator.next();
	                           BigDecimal bd = new BigDecimal((Double)SharedPrinterFields.lineePagamento.get(k));
	                           bd = bd.setScale(2, RoundingMode.HALF_DOWN);
	                           Double valDouble = new Double(bd.doubleValue() * 10000);
	                           long val =  valDouble.longValue();                                        
	                           String out = buildItem ( k, val);
	                           printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, out);
	                        }
	                        SharedPrinterFields.lineePagamento = null;
	                    }else {                        
							long resto = 0;
							if (M.getV().get(2).toString().equalsIgnoreCase(SharedPrinterFields.ChangeCurrency))
								resto = HardTotals.TotalePagato.getLongX100()-HardTotals.Totale.getLongX100();
							String out = buildItem ( M.getV().get(2).toString().substring(3), Long.parseLong(M.getV().get(1).toString()) - resto );
							printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, out);
	                    }
					}
				}
				if (SharedPrinterFields.RoundingRT > 0.0) {
					long val =  SharedPrinterFields.RoundingRT.longValue();
	                String out = buildItem (RTConsts.ROUNDINGDETAILS, val);
					printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, out);
				}
			}
			
			if ((SharedPrinterFields.Lotteria.getLotteryCode() != null) && (SharedPrinterFields.Lotteria.getLotteryCode().length() > 0))
			{
				if (!RTTxnType.isVoidTrx())
				{
					printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, R3define.LotteryMessage + SharedPrinterFields.Lotteria.getLotteryCode());
					//for (int i = 0; i < R3define.LotteryMessages.length; i++) {
					//	printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, R3define.LotteryMessages[i]);
					//}
				}
				SharedPrinterFields.Lotteria.resetLottery();
			}

			printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, RTConsts.SERVERID+SRTServerID);
			printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, RTConsts.TILLID+SRTTillID);
			printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			
			if ((currentFingerPrint != null) && (currentFingerPrint.length() > 0))
			{
				printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, "--------"+RTConsts.FINGERPRINT+"-------");
				
				ArrayList cfp = new ArrayList();
				cfp = splitFingerPrint(currentFingerPrint);
				if (cfp.size() > 0){
					for (int i=0; i<cfp.size(); i++)
						printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, (String)cfp.get(i));
				}
				printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, "--------------------------------");
			}
			
			printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			
			if (!SharedPrinterFields.isCFcliente()){
				printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, RTConsts.INTERPELLO141);
				printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, RTConsts.INTERPELLO142);
				printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, RTConsts.INTERPELLO143);
				printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			}
			SharedPrinterFields.setCFcliente(false);
		}
		
		private ArrayList splitFingerPrint(String fingerprint)
		{
			int max = RTConsts.setMAXLNGHOFFINGERPRINT();
			
			ArrayList reply = new ArrayList();
			
			int lines = (fingerprint.length() / max) + 1;
			
			for (int i = 0; i < lines; i++){
				int begin = i * max;
				int end = begin + max;
	            if (end > fingerprint.length())
	                end = fingerprint.length();
				
				if (end > begin){
					String s = fingerprint.substring(begin, end);
					reply.add(s);
				}
			}
			
			return reply;
		}
		
		private void AutoVoidTrx()
		{
			SharedPrinterFields.setMyReply(R3define.PRINTER_SRT_ERR);
		}
		
		public static int  getPrinterState() throws JposException
		{
			while ( true )
			{
				if ( SharedPrinterFields.isInTicket() )
				{
					return ( SharedPrinterFields.getSimulateState() );
				}
				else
				{
					return ( getState() );
				}
			}
		}
		
		public static int  getSimulatedPrinterState() throws JposException
		{
			return ( SharedPrinterFields.getSimulateState() );
		}
		
		private static int getFiscalPrinterState() throws JposException
		{
			return (fiscalPrinterDriver.getPrinterState());
		}
		
		private void SetLogo(String name, int number)
		{
			System.out.println("SetLogo - setting logo: "+number+" - "+name);
					
			File filep = new File(SharedPrinterFields.WorkingFolder+LOGO_FOLDER+name);
			if (!filep.exists()){
				System.out.println("SetLogo: "+filep.getAbsolutePath()+" doesn't exist");
				return;
			}

			int cmdLoad = 1202;
			int cmdSelect = 1204;
    		int[] configuration = new int[] {3};
    		String[] path = new String[] {SharedPrinterFields.WorkingFolder+LOGO_FOLDER+name};
    		int[] logo = new int[] {3};
    		
    		if (number == TRAILER_LOGO_NUMBER) {
    			cmdSelect = 1205;
    			configuration[0] = 4;
    			logo[0] = 4;
    		}
    		
    		try {
				fiscalPrinterDriver.directIO(cmdLoad, configuration, path);
			} catch (Exception e) {
				System.out.println("SetLogo ("+cmdLoad+") - exception: "+e.getMessage());
			}
    		System.out.println("Waiting a little while...");
    		TakeYourTime.takeYourTime(5000);
    		
    		try {
    			fiscalPrinterDriver.directIO(cmdSelect, logo, (StringBuffer)null);
			} catch (Exception e) {
				System.out.println("SetLogo ("+cmdSelect+") - exception: "+e.getMessage());
			}
    		System.out.println("Waiting a little while...");
    		TakeYourTime.takeYourTime(5000);
		}
		   
		private void PrintLogo(int number)
		{
		}
		
		protected static void scriviLastTicket(String linea){
	    	apriFile();
			scriviFile(linea);
			chiudiFile();
		}
		
		private static void apriFile(){
		  	 try{
		  		 inout = new File(LastTicket.getLastticket());
		  		 fos = new FileOutputStream(inout,true);
		  		 ps = new PrintStream(fos);
		  	 }catch (Exception e) {
		  		 System.out.println("apriFile - errore:"+e);
		  	 }
		   }
		
		   private static void chiudiFile(){
		   	try{
		   		ps.close();
		   		ps = null;
		   		fos.close();
		   		fos = null;
		//	    inout = null;
		   	}catch (Exception e) {
		   		System.out.println("chiudiFile - errore:"+e);
		   	}
		   }
		   
		   private static void scriviFile(String linea)
		   {
			    if ((linea == null) || (linea.length() == 0))
			    	return;
			    
			    String s = linea;
			    char lastch;
			    
				lastch = s.charAt(s.length()-1);
				if (lastch >= ' ')
					s = s + R3define.LF;
			   
		    	 try{
					ps.print(s);
		    	 }catch (Exception e) {
		    		 System.out.println("scriviFile - errore:"+e);
		    	 }
		     }
		   
		   private void cancellaFile()
		   {
				if (inout == null)
			  		 inout = new File(LastTicket.getLastticket());
				
		     	 try{
		     		 inout.delete();
		     	 }catch (Exception e) {
		     		 System.out.println("cancellaFile - errore:"+e);
		     	 }
		      }
		   
			private boolean leggiFile()
			{
				char lastch;
				
				if (inout == null)
			  		 inout = new File(LastTicket.getLastticket());
				
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(inout);
				} catch (FileNotFoundException e) {
					 System.out.println("leggiFile - errore:"+e);
					 return(false);
				}
				
				try {
					beginNonFiscal();
				} catch (JposException e) {
			   		 System.out.println("leggiFile - errore:"+e);
					 return(false);
				}
				
				InputStreamReader isr = new InputStreamReader(fis);
				BufferedReader br = new BufferedReader(isr);
				String linea = null;
				try {
					linea = br.readLine();
				} catch (IOException e) {
				 System.out.println("leggiFile - errore:"+e);
				 return(false);
				}
				while (linea != null)
				{
					// può capitare xchè readLine() elimina i CrLf a fine linea
					if (linea.length() == 0)
						linea=" ";
					
					try {
						lastch = linea.charAt(linea.length()-1);
						if (lastch >= ' ')
							linea = linea + R3define.LF;
						printNormal(jpos.POSPrinterConst.PTR_S_RECEIPT, linea);
					} catch (JposException e1) {
				   		 System.out.println("leggiFile - errore:"+e1);
			    		 return(false);
					}
					
					linea = null;
					
					try {
						linea = br.readLine();
					} catch (IOException e) {
			    		 System.out.println("leggiFile - errore:"+e);
			    		 return(false);
					}
				}
				
				try {
					endNonFiscal();
				} catch (JposException e) {
			   		 System.out.println("leggiFile - errore:"+e);
					 return(false);
				}
				
				return(true);
			}
			
		protected static boolean initTicketOnFile() throws JposException
		{
			char	lastch;
			int		i;
			String	sd;
			
			ArrayList HH = TicketErrorSupport.getHD();
			if (HH == null)
				return ( false );
			
			for ( i = 0; i < 2 ; i++ )
			{
				sd = " ";
				lastch = sd.charAt(sd.length()-1);
				if (lastch >= ' ')
					sd = sd + R3define.LF;
				
				scriviLastTicket(sd);
			}
			for ( i = 0; i < HH.size() ; i++ )
			{
				sd = (String)HH.get( i );
				lastch = sd.charAt(sd.length()-1);
				if (lastch >= ' ')
					sd = sd + R3define.LF;
				
				scriviLastTicket(sd);
			}
			for ( i = 0; i < 2 ; i++ )
			{
				sd = " ";
				lastch = sd.charAt(sd.length()-1);
				if (lastch >= ' ')
					sd = sd + R3define.LF;
				
				scriviLastTicket(sd);
			}
			return ( true );
		}
		
		private void initTicket() throws JposException
		{
		}
		
	    private void space (int how) throws JposException
	    {
			for ( int i = 0 ; i < how; i++ )
			{
				fiscalPrinterDriver.printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT," ");
			}
	    }
	    
	    private String align_center(String s, int max)
	    {
	    	String t=s;
	    	String spaces = new String(new char[(max-(t.trim().length()))/2]).replace('\0', ' ');
	    	return(spaces+t);
	    }
		   
		private void printNormal_I(int station, String data) throws JposException
	    {

	      int state = getFiscalPrinterState();
	      
	      int msgLength = fiscalPrinterDriver.getMessageLength ();  
	      String oldS = data;
	      data = "";
	      int a = 0;
	      for ( int i = 0 ; i < oldS.length(); i++ )
	      {
	    	  if ( a == 40 )
	    	  {
	    		  if ( oldS.charAt(i) >= ' ' )
	    		  {
	    			  a = 0;
	    			  data += "\r\n";
	    		  }
	    	  }
	    	  if (SRTPrinterExtension.isPRT())
	    	  {
	        	  if ((oldS.charAt(i) == '\t') || (oldS.charAt(i) < (char)32) || (oldS.charAt(i) > (char)127))
	        	  {
	        		  if ((oldS.charAt(i) == (char)10) || (oldS.charAt(i) == (char)13))
	    	    		  data = data + oldS.charAt(i);
	        		  else
	        			  data = data + ' ';
	        	  }
		    	  else
		    		  data = data + oldS.charAt(i);
		    	  a += 1;
		    	  if ( oldS.charAt(i) < ' ' )
		    	  {
		    		  a = 0;
		    	  }
	    	  }
	    	  else
	    	  {
		    	  if (oldS.charAt(i) == '\t')
		    		  data = data + ' ';
		    	  else
		    		  data = data + oldS.charAt(i);
		    	  a += 1;
		    	  if ( oldS.charAt(i) < ' ' )
		    	  {
		    		  a = 0;
		    	  }
	    	  }
	      }
	      String msg = (data.length()  <= msgLength)? data: data.substring (0, msgLength);
	      
	      msg = String13Fix.replaceAll(msg, "€", ""+(char)127);
	     
	      switch (state)
	      {
	        case jpos.FiscalPrinterConst.FPTR_PS_NONFISCAL:
	        	String back36 = "";
	    	    String s1 = String13Fix.replaceAll(data,"\n", String13Fix.REPLACE);
	    	    s1 = String13Fix.replaceAll(s1,"\r", String13Fix.REPLACE);
	    	    s1 = String13Fix.replaceAll(s1,String13Fix.REPLACE+String13Fix.REPLACE, String13Fix.REPLACE);
	    	    s1 = String13Fix.replaceAll(s1, "€", ""+(char)127);
	    	    String[] data1 = String13Fix.split(s1,String13Fix.REPLACE);
	    	    for (int z = 0; z < data1.length; z++)
	    	    {
	    	    	if (SRTPrinterExtension.isSRT())
	    	    		back36 = please40 ( data1[z] );
	    	    	else
	    	    		back36 = please36 ( data1[z] );
	    	    	String s = back36.replaceAll("(?i)TOTALE", "TOTA..");
	   	        	fiscalPrinterDriver.printNormal(station, s);
	    	    }
	            break;
	        case jpos.FiscalPrinterConst.FPTR_PS_FISCAL_RECEIPT:
	        	if (SRTPrinterExtension.isPRT() || SRTPrinterExtension.isSRT()) {
	        		if (msg.startsWith(SharedPrinterFields.Lotteria.getLotteryTag())) {
	        			SharedPrinterFields.Lotteria.setLotteryCode(msg.substring(SharedPrinterFields.Lotteria.getLotteryTag().length()));
	        			break;
	        		}
	        	}
	        	if (msg.startsWith(SharedPrinterFields.getCFPIvaTag())) {
	        		String cfpi = msg.substring(SharedPrinterFields.getCFPIvaTag().length());
	                cfpi = String13Fix.replaceAll(cfpi, "\r", "");
	                cfpi = String13Fix.replaceAll(cfpi, "\n", "").trim();
	        		if (SRTPrinterExtension.isPRT()) {
	        			printRecCFPiva(cfpi);
	        			break;
	        		}
	        		else {
	        			msg = (cfpi.length() == PILEN ? PI+cfpi : CF+cfpi);
	        			break;	// non lo devo stampare
	        		}
	        	}
	        	printRecMessage(msg);
	        break;
	        case jpos.FiscalPrinterConst.FPTR_PS_FISCAL_RECEIPT_TOTAL:
	        case jpos.FiscalPrinterConst.FPTR_PS_FISCAL_RECEIPT_ENDING:
	        	printRecMessage(msg);
	        break;
	        
	        default:
	        	System.out.println("ATTENZIONE CHE CAZZO FAI: printNormal eseguita in stato <"+state+">");
	        	fiscalPrinterDriver.printNormal(station, msg);
	        break;
	      }
	      System.out.println ("*** Esco da PrintNormal ***");

	    }
	    
	    private String please25( String in)
	    {
	    	return ( pleaseAny (in,25) );
	    }
	    private String please30( String in)
	    {
	    	return ( pleaseAny (in,30) );
	    }
	    private String please36( String in)
	    {
	    	return ( pleaseAny (in,36) );
	    }
	    private String please40( String in)
	    {
	    	return ( pleaseAny (in,40) );
	    }
	    private String pleaseAny( String in,int qta)
	    {
	    	if (SRTPrinterExtension.isSRT())
	    	{
	    		char lastch = in.charAt(in.length()-1);
	    		if (!(lastch == '-'))
	    			qta--;
	    	}
			   
	    	String out = in;
	    	if ( in.length() <= qta )
	    		return ( out );
	    	int diff = in.length() - qta;
	    	while ( diff > 0 )
	    	{
	    		int i = out.indexOf("  ");
	    		if ( i == -1)
	    			break;
	    		diff--;
	    		String n = out.substring(0, i);
	    		n = n + out.substring(i+1);
	    		out = n;
	    	}
	    	return ( out );
	    }
		   
	    private static int[] dobcc(String s)
	    {
	    	int[] bcc = {0,0};
	    	int a=0;
	    	int bccl=0;
	    	int bcch=0;
	    	for (int i=1; i<(s.length()-3); i++){
	    		a = a ^ ((char) (s.charAt(i)));
	    	}
	    	bccl = a & 0x0F;
	        bccl = bccl + 0x20;
	        bcch = a >> 4;
	        bcch = bcch + 0x20;
	        bcc[0] = bcch;
	        bcc[1] = bccl;
	        return (bcc);
	    }
		   
	    private String cleanLine(String in)
	    {
	    	String out = "";
	    	for ( int i = 0 ; i < in.length(); i++ )
	    	{
	    		if ( in.charAt(i) >= ' ' )
	    			out = out + in.charAt(i);
	    		else
	    			out = out + ' ';
	    	}
			   
	    	return (out);
	    }
		   
		private void printDouble(int Mode, String data, int coloumn) throws JposException
		{
			String	buff = "";
			String	hdFsc = "014";
			String	hdPrt = R3define.ESC +"|3C";
			String	hd;
			int		i, j;
			int		coloumnLimit;
	        int[]	dt={0};
	        
	        dt[0] = R3define.DOUBLEPRINT;
			
			coloumnLimit = coloumn - 1;
			System.out.println ( "DOUBLE <"+coloumn+"><"+coloumnLimit+">");
			
			hd = hdFsc;

			String[] data1 = String13Fix.split(data,"\n");
			for (j = 0; j < data1.length; j++)
			{
				if ( Mode == R3define.DOUBLEPRINT_LEFT )
				{
					buff = data1[j] + R3define.DOUBLEPRINTBLANK;
					StringBuffer bjct=new StringBuffer(hd+buff.substring(0,coloumn));
					fiscalPrinterDriver.directIO(0, dt, bjct);
				}
				else if ( Mode == R3define.DOUBLEPRINT_RIGHT )
				{
					buff = R3define.DOUBLEPRINTBLANK + data1[j];
					StringBuffer bjct=new StringBuffer(hd+buff.substring(buff.length()-coloumn,buff.length()));
					fiscalPrinterDriver.directIO(0, dt, bjct);
				}
				else
				{
					i = data1[j].length();
					if ( i < coloumnLimit )
					{
						buff = R3define.DOUBLEPRINTBLANK.substring(0,(coloumn-i)/2);
						buff = buff + data1[j] + R3define.DOUBLEPRINTBLANK;
						StringBuffer bjct=new StringBuffer(hd+buff.substring(0,coloumn));
	    				fiscalPrinterDriver.directIO(0, dt, bjct);
					}
				}
			}
			return;
		}
		
		private void printRecCFPiva(String s) throws JposException
		{
			if (SRTPrinterExtension.isPRT())
			{
				int state = this.getFiscalPrinterState(); 
				System.out.println("printRecCFPiva - s = "+s+" - state = "+state);
				
				if ((s.length() != PILEN) && (s.length() != CFLEN))
					return;
				
				// lascio passare il comando solo in stato FPTR_PS_FISCAL_RECEIPT
				// anche se la Epson lo accetterebbe pure nello stato FPTR_PS_FISCAL_RECEIPT_ENDING,
				// ma la la RTOne segnalerebbe errore
				if (state != jpos.FiscalPrinterConst.FPTR_PS_FISCAL_RECEIPT)
					return;
				
	            String fiscalcode = s;
				StringBuffer sbcmd = new StringBuffer("=\"/?C/("+fiscalcode+")");		// stampa P.Iva / C.F.
	            
				System.out.println("printRecCFPiva - sbcmd = "+sbcmd.toString());
				fiscalPrinterDriver.executeRTDirectIo(0, 0, sbcmd);
				System.out.println("printRecCFPiva - sbcmd = "+sbcmd.toString());
				SharedPrinterFields.setCFPIvaFlag(true);
			}
		}
		
	    private void executeDirectIo(int Command, String Data){
	    	int[] dt={0};
	        StringBuffer bjct=new StringBuffer(Data);
	        try{
	        	dt[0]=Command;
	                  
	        	System.out.println("Appena Data: "+String.valueOf(dt[0])+" --- "+"Object: "+bjct.toString()); 

	        	this.directIO(0, dt, bjct); 
	                  
	        	System.out.println("Data: "+String.valueOf(dt[0])+" --- "+"Object: "+bjct.toString()); 
	        }catch(Exception e){
	        	System.out.println("Data error Exception: "+ e.getMessage());   
	        }
	    }
	    
		private String intestazione5(boolean fiscalreceipt)
		{
			if (RTTxnType.isVoidTrx())
				return "";
			
			String intestazione = RTConsts.DESCRIZIONE;
			if (fiscalreceipt){
				intestazione = intestazione + ALINER.substring(0,RTConsts.getMAXITEMDESCRLENGTH()-2-intestazione.length()-RTConsts.IVA.length())+ RTConsts.IVA;
			}
			else{
				intestazione = intestazione + ALINER.substring(0,RTConsts.getMAXITEMDESCRLENGTH()-1-intestazione.length()-RTConsts.IVA.length())+ RTConsts.IVA;
				intestazione = intestazione + ALINER.substring(0,RTConsts.setMAXLNGHOFLENGTH()-intestazione.length()-RTConsts.PREZZO.length())+ RTConsts.PREZZO;
			}
			LastTicket.setIntestazione5(intestazione);
			return intestazione;
		}
		
		protected String intestazione1(int type)
		{
			String intestazione = "";
			
			intestazione = RTConsts.INTESTAZIONE1;
			intestazione = ALINER.substring(0, (int)((RTConsts.setMAXLNGHOFLENGTH()-intestazione.length())/2))+intestazione;
			
			LastTicket.setIntestazione1(intestazione);
			return intestazione;
		}
		
		protected String intestazione2(int type)
		{
			String intestazione = "";
			
			if (RTTxnType.isVoidTrx(type)){
				intestazione = RTConsts.INTESTAZIONE23;
				intestazione = ALINER.substring(0, (int)((RTConsts.setMAXLNGHOFLENGTH()-intestazione.length())/2))+intestazione;
			}
			if (RTTxnType.isRefundTrx(type)){
				intestazione = RTConsts.INTESTAZIONE21;
				intestazione = ALINER.substring(0, (int)((RTConsts.setMAXLNGHOFLENGTH()-intestazione.length())/2))+intestazione;
			}
			if (RTTxnType.isSaleTrx(type)){
				intestazione = RTConsts.INTESTAZIONE20;
				intestazione = ALINER.substring(0, (int)((RTConsts.setMAXLNGHOFLENGTH()-intestazione.length())/2))+intestazione;
			}
			LastTicket.setIntestazione2(intestazione);
			return intestazione;
		}
		
		protected String intestazione3(int type)
		{
			String intestazione = "";
			
			if (RTTxnType.isVoidTrx(type) || RTTxnType.isRefundTrx(type)){
				intestazione = RTConsts.INTESTAZIONE3;
				intestazione = ALINER.substring(0, (int)((RTConsts.setMAXLNGHOFLENGTH()-intestazione.length())/2))+intestazione;
			}
			LastTicket.setIntestazione3(intestazione);
			return intestazione;
		}
		
		private String intestazione4(int type)
		{
			String intestazione = "";
			
			if (RTTxnType.isVoidTrx(type) || RTTxnType.isRefundTrx(type)){
				intestazione = RTConsts.INTESTAZIONE4;
				intestazione = ALINER.substring(0, (int)((RTConsts.setMAXLNGHOFLENGTH()-intestazione.length())/2))+intestazione;
			}
			LastTicket.setIntestazione4(intestazione);
			return intestazione;
		}
		
		private void testata() throws JposException
		{
			printNormal_ejoff(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			String s = intestazione1(RTTxnType.getTypeTrx());
			if (s.length() > 0) printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, s);
			s = intestazione2(RTTxnType.getTypeTrx());
			if (s.length() > 0) printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, s);
			s = intestazione3(RTTxnType.getTypeTrx());
			if (s.length() > 0) printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, s);
			s = intestazione4(RTTxnType.getTypeTrx());
			if (s.length() > 0) printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, s);
			printNormal_ejoff(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			s = intestazione5(this.isFiscalAndSRTModel());
			if (this.isFiscalAndSRTModel()){
				if (s.length() > 0) printNormal_ejoff(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, s);
			}
			else{
				if (s.length() > 0) printNormal_ejoff(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, rightAlign(s));
			}
		}
		
	    private String rightAlign ( String desc )
	    {
	        int MAXLNGHOFLENGTH = RTConsts.setMAXLNGHOFLENGTH();
	        
	        int mx = MAXLNGHOFLENGTH;
	        String newMsg = (desc.length() <= mx)? desc: desc.substring (0, mx);
	        
	    	return ( ALINER.substring(0,mx-newMsg.length())+ newMsg);
	    }
		
		private void stampaBarcodePerResi()
		{
			// usata solo negli scontrini fiscali
			
			if (!Lotteria.isPrintBarcode())
				return;
			
			if (RTTxnType.isRefundTrx() || RTTxnType.isVoidTrx())
				return;
			
			if (isFlagsVoidTicket())
				return;
			
			String anno;
			String mese;
			String giorno;
			GregorianCalendar gc = new GregorianCalendar();
			anno = Sprint.f("%02d",gc.get(gc.YEAR)-2000);	// fino al 2099 dovremmo essere a posto
			mese = Sprint.f("%02d",gc.get(gc.MONTH)+1);
			giorno = Sprint.f("%02d",gc.get(gc.DATE));
			
			String num = "";
			String repz = "";
			if (SRTPrinterExtension.isSRT()) {
				num = Sprint.f("%04d",DummyServerRT.CurrentReceiptNumber);
				repz = Sprint.f("%04d",DummyServerRT.CurrentFiscalClosure);
			}
			else {
				num = fiscalPrinterDriver.myRchFiscalNumber;
				
		        int[] ai = new int[1];
		        String[] as = new String[1];
				
	            fiscalPrinterDriver.getData(FiscalPrinterConst.FPTR_GD_Z_REPORT, ai, as);
	            try {
	            	repz = Sprint.f("%04d",Integer.parseInt(as[0])+1);
                } catch (NumberFormatException e) {
				   System.out.println("stampaBarcodePerResi - Exception : " + e.getMessage());
				   return;
                }
			}
            
            String bc = R3define.getBarcodePrefix();
			bc = bc + PosApp.getTillNumber() + giorno + mese + anno + repz + num;
			bc = bc + SharedPrinterFields.Lotteria.getLotteryCode();
			System.out.println ( "stampaBarcodePerResi printing barcode : <"+bc+">");
			
			StringBuffer objb = new StringBuffer("");
			int type = 8;
			
			if (UI.isUIActivated()) {
				bc = bc + SharedPrinterFields.RTPrinterId;
				System.out.println ( "stampaBarcodePerResi printing qrcode  : <"+bc+">");
       			type = 11;
			}
			
			objb = new StringBuffer(bc);
			fiscalPrinterDriver.executeRTDirectIo(5000, type, objb);
    		
			if (SharedPrinterFields.isfiscalEJenabled() && !SharedPrinterFields.PosponedInError) {
				try {
					if (type == 8)
						SharedPrinterFields.fiscalEJ.printBarcode(bc);
					if (type == 11)
						SharedPrinterFields.fiscalEJ.printQrcode(bc);
				} catch (JposException e) {
					   System.out.println("stampaBarcodePerResi - Exception : " + e.getMessage());
				}
			}
    		
    		SmartTicket.SMTKbarcodes_add(bc);
		}
		
		public void stampaBarcodePerResi(String bc, String lott)
		{
			// usata solo negli scontrini regalo non fiscali
			
			if (!Lotteria.isPrintBarcode())
				return;
			
			if (RTTxnType.isRefundTrx() || RTTxnType.isVoidTrx())
				return;
			
			System.out.println ( "stampaBarcodePerResi printing barcode : <"+bc+">");
			
			StringBuffer objb = new StringBuffer("");
			int type = 8;
			
			if (UI.isUIActivated()) {
				System.out.println ( "stampaBarcodePerResi printing qrcode  : <"+bc+">");
       			type = 11;
			}
			
			int[] dt={0};
			dt[0]=type;
			objb = new StringBuffer(bc);
			try {
				this.directIO(5000, dt, objb);
			} catch (JposException e) {
				System.out.println("stampaBarcodePerResi - Exception : " + e.getMessage());
			}
    		
			if (SharedPrinterFields.isfiscalEJenabled() && !SharedPrinterFields.PosponedInError) {
				try {
					if (type == 8)
						SharedPrinterFields.fiscalEJ.printBarcode(bc);
					if (type == 11)
						SharedPrinterFields.fiscalEJ.printQrcode(bc);
				} catch (JposException e) {
					   System.out.println("stampaBarcodePerResi - Exception : " + e.getMessage());
				}
			}
		}

		private static String convertiBarcodePerResi(String barcode)
		{
			if (!Extra.isNumericVAR())
				return barcode;
			
			String bc = "";
			
	    	for (int i=0; i<barcode.length(); i++) {
	    		if ((i < 3) || (i > 18)) {
	    			int c = (int)barcode.charAt(i);
	    			bc = bc + String.valueOf(c);
	    		}
	    		else
	    		{
	    			bc = bc + barcode.charAt(i);
	    		}
	    	}
	    	
			System.out.println("convertiBarcodePerResi returning barcode : <"+bc+">");
	    	return bc;
		}
		
		private String setwidth(String lott)
		{
			int width = 2;
			if (R3define.getBarcodePrefix().length() == 6)
				width = 3;
			else if (R3define.getBarcodePrefix().length() == 3)
				width = 2;
			if (lott.length() > 0)
				width--;
			return ""+width;
		}
		
		private String setsubset()
		{
			String subset = "{B";
			if (R3define.getBarcodePrefix().length() == 6)
				subset = "{C";
			else if (R3define.getBarcodePrefix().length() == 3)
				subset = "{B";
			return subset;
		}
		
		private void abilitaTaglioCarta(boolean flag)
		{
			if (!Lotteria.isPrintBarcode())
				return;
			
			return;
		}
		
		static boolean checkCurrentTicketTotal ( String Pr, long In )
		{
		    if (Company.getNegTill() == 1)
		    {
				System.out.println("MAPOTO-TicketErrorSupport:checkCurrentTicketTotal isNegativeTill="+Company.getNegTill());
				return ( true );
		    }

			System.out.println("TicketErrorSupport:Verifica Totale Gestionale con Totale Fiscale");
			if ( ( countErrori % 3 ) == 0 )		// debugs phase only
			{
				System.out.println("TicketErrorSupport:Simula Errore Totale Gestionale diverso da Totale Fiscale");
				return ( false );
			}
			double	fromPrinter = Double.valueOf(Pr).doubleValue()/100;
			currentTicket = (double)(In /100);
			if ( Math.abs(currentTicket) == Math.abs(fromPrinter) )
			{
				System.out.println("TicketErrorSupport:checkCurrentTicketTotal <true>");
				return ( true );
			}
			else
			{
				System.out.println("MAPOTO-TicketErrorSupport:checkCurrentTicketTotal <false> at PRINTER="+fromPrinter+" at TILL="+currentTicket);
				return ( false );
			}
		}
		
		private static boolean isCurrentTicketZero (  )
		{
			System.out.println("MAPOTO-TicketErrorSupport:isCurrentTicketZero <"+( currentTicket == (double)0.0 )+">");
			return ( currentTicket == (double)0.0 );
		}
		
		static boolean checkCurrentDailyTotal ( String In )
		{
		    if (Company.getNegTill() == 1)
		    {
				System.out.println("MAPOTO-TicketErrorSupport:checkCurrentDailyTotal isNegativeTill="+Company.getNegTill());
				return ( true );
		    }

			System.out.println("MAPOTO-TicketErrorSupport:Verifica Scontrino Fiscalizzato");
			if ( ( countErrori % 5 ) == 0 )	// debugs phase only
			{
				System.out.println("MAPOTO-TicketErrorSupport:Simula Errore Scontrino Non Fiscalizzato");
				return ( false );
			}
			boolean res = false;
			double	inD = Double.valueOf(In).doubleValue()/100;
			double	inT = dailyTotal + currentTicket;
			if ( Math.abs(inT) == Math.abs(inD) )
				res = true;
			System.out.println("MAPOTO-TicketErrorSupport:Check dailyTotal consistency="+res+"IN+SCO="+inT+ " PRT="+inD);
			return ( res );
		}

		static boolean checkCurrentDailyTotalRounded ( String In, double rounding )
		{
		    if (Company.getNegTill() == 1)
		    {
				System.out.println("MAPOTO-checkCurrentDailyTotalRounded isNegativeTill="+Company.getNegTill());
				return ( true );
		    }

			System.out.println("MAPOTO-checkCurrentDailyTotalRounded:Verifica Scontrino Fiscalizzato");
			if ( ( countErrori % 5 ) == 0 )	// debugs phase only
			{
				System.out.println("MAPOTO-checkCurrentDailyTotalRounded:Simula Errore Scontrino Non Fiscalizzato");
				return ( false );
			}
			boolean res = false;
			double	inD = Double.valueOf(In).doubleValue()/100;
			double	inT = dailyTotal + currentTicket + (rounding*100);
			if ( Math.abs(inT) == Math.abs(inD) )
				res = true;
			System.out.println("MAPOTO-checkCurrentDailyTotalRounded:Check dailyTotal consistency="+res+"IN+SCO="+inT+ " PRT="+inD);
			return ( res );
		}

		private void printRecOmaggio(String s, long l, int i, int j, long l1, int kIvaPolipos) throws JposException
		{
			System.out.println ( "MAPOTO-EXEC PRINT OMAGGIO s="+s );
			System.out.println ( "MAPOTO-EXEC PRINT OMAGGIO l="+l);
			System.out.println ( "MAPOTO-EXEC PRINT OMAGGIO kIvaPolipos="+kIvaPolipos );
			
			int Command = 1;
			
			int k = DicoTaxToPrinter.getFromPoliposToPrinter(kIvaPolipos);
			if ((DicoTaxLoad.isIvaAllaPrinter()) && (k == SharedPrinterFields.VAT_N4_Index))
				k = Integer.parseInt(SharedPrinterFields.VAT_N4_Dept);
			
			newModifierCommand(s, l, Command, k, kIvaPolipos);
		}
		
		private void printRecAcconto(String s, long l, int iIvaPolipos) throws JposException
		{
			System.out.println ( "MAPOTO-EXEC PRINT ACCONTO s="+s );
			System.out.println ( "MAPOTO-EXEC PRINT ACCONTO l="+l);
			System.out.println ( "MAPOTO-EXEC PRINT ACCONTO iIvaPolipos="+iIvaPolipos );
			
			int Command = 0;
			
			int i = DicoTaxToPrinter.getFromPoliposToPrinter(iIvaPolipos);
			if ((DicoTaxLoad.isIvaAllaPrinter()) && (i == SharedPrinterFields.VAT_N4_Index))
				i = Integer.parseInt(SharedPrinterFields.VAT_N4_Dept);
			
			newModifierCommand(s, l, Command, i, iIvaPolipos);
		}
		
		private void printRecBMonoUso(String s, long l, int iIvaPolipos) throws JposException
		{
			System.out.println ( "MAPOTO-EXEC PRINT BMONOUSO s="+s );
			System.out.println ( "MAPOTO-EXEC PRINT BMONOUSO l="+l);
			System.out.println ( "MAPOTO-EXEC PRINT BMONOUSO iIvaPolipos="+iIvaPolipos );
			
			int Command = 2;
			
			int i = DicoTaxToPrinter.getFromPoliposToPrinter(iIvaPolipos);
			if ((DicoTaxLoad.isIvaAllaPrinter()) && (i == SharedPrinterFields.VAT_N4_Index))
				i = Integer.parseInt(SharedPrinterFields.VAT_N4_Dept);
			
			newModifierCommand(s, l, Command, i, iIvaPolipos);
		}

		private void newModifierCommand(String descr, long amount, int cmd, int vat, int ivapolipos)
		{
			if (fiscalPrinterDriver.isfwRT2disabled())
				return;
			
			System.out.println("newModifierCommand - descr=<"+descr+">");
			System.out.println("newModifierCommand - amount="+amount);
			System.out.println("newModifierCommand - cmd="+cmd);
			System.out.println("newModifierCommand - vat="+vat);
			
			if (cmd == 2) {
				if (TaxData.isServizi(ivapolipos))
					System.out.println("RT2 - newModifierCommand - WARNING : articolo tipo Servizio");
				
				// =R2/$2000/&3/(BUONO MONOUSO)
				cmd = 3;
			}
			if (cmd == 1) {
				// =R2/$2000/&1/(OMAGGIO)
				cmd = 1;
			}
			if (cmd == 0) {
				if (TaxData.isServizi(ivapolipos))
					System.out.println("RT2 - newModifierCommand - WARNING : articolo tipo Servizio");
				
				// =R2/$2000/&2/(ACCONTO)
				cmd = 2;
			}
			StringBuffer sbcmd = new StringBuffer("=R"+vat+"/$"+(amount/100)+"/&"+cmd+"/("+descr+")");
			System.out.println("RT2 - newModifierCommand - sbcmd = "+sbcmd.toString());
			fiscalPrinterDriver.executeRTDirectIo(0, 0, sbcmd);
			System.out.println("RT2 - newModifierCommand - sbcmd = "+sbcmd.toString());
		}

		public boolean RTRefund(String data, String serialSRT, boolean freerefund) {
			
			if (!SRTCheckInput.checkInput(data, freerefund)) {
				MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
				RTTxnType.setSaleTrx();
				return false;
			}
		
			if (SRTPrinterExtension.isPRT()) {
				boolean isRefundable = false;
				
				String cassa = Sprint.f("%02d", data.substring(SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.CC), SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.CC)+SRTCheckInput.CC.length()));
				String repz = Sprint.f("%04d", data.substring(SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.ZZZZ), SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.ZZZZ)+SRTCheckInput.ZZZZ.length()));
				String num = Sprint.f("%04d", data.substring(SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.NNNN), SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.NNNN)+SRTCheckInput.NNNN.length()));
				BasicDicoData date = new BasicDicoData (data,
						SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.DD),
						SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.DD)+SRTCheckInput.DD.length()+SRTCheckInput.MM.length()+SRTCheckInput.YY.length(),
														false);
				String printerid = "";
				if (data.length() < SRTCheckInput.INPUTRT.length()) {
					printerid = SharedPrinterFields.RTPrinterId;
					String parte1 = data.substring(SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.CC), SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.NNNN)+SRTCheckInput.NNNN.length());
					String parte2 = data.substring(SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.NNNN)+SRTCheckInput.NNNN.length());
					data = parte1+printerid+parte2;
				}
				else {
					printerid = data.substring(SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.MMMMMMMMMMM), SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.MMMMMMMMMMM)+SRTCheckInput.MMMMMMMMMMM.length());
				}
				System.out.println("RTRefund - cassa = <"+cassa+"> repz = <"+repz+"> num = <"+num+"> data = <"+date.toString()+"> printerid = <"+printerid+">");

				RTConsts.INTESTAZIONE4 = repz+"-"+num+" del "+date.toString().substring(0, 2)+"-"+date.toString().substring(2, 4)+"-"+date.toString().substring(4);
				
				try {
					RefundCommands refcmd = new RefundCommands();
					isRefundable = refcmd.isRefundableDocument(repz, num, date.toString(), printerid, freerefund);
				} catch (JposException e) {
					System.out.println("RTRefund - errore:"+e.getMessage());
					isRefundable = false;
				}
				
				if (!isRefundable){
					System.out.println("RTRefund - isRefundable="+isRefundable);
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}
				
			    if (SmartTicket.isSmart_Ticket())
			    {
			    	// devo mandare questi comandi prima del comando di refund altrimenti vanno in errore
			    	fiscalPrinterDriver.SMTKsetReceiptParameters();
			    }
			    
				try {
					RefundCommands refcmd = new RefundCommands();
					if (!refcmd.RefundDocument(repz, num, date.toString(), printerid, freerefund)){
						MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
						RTTxnType.setSaleTrx();
						return false;
					}
				} catch (JposException e) {
					System.out.println("RTRefund - errore:"+e.getMessage());
					RTTxnType.setSaleTrx();
					return false;
				}
				
				SharedPrinterFields.Lotteria.setLotteryTill(Integer.parseInt(cassa));
				DummyServerRT.setOriginalFiscalClosure(repz);
				DummyServerRT.setOriginalReceiptNumber(num);
				if (data.length() > SRTCheckInput.INPUTRT.length()){
					SharedPrinterFields.Lotteria.setLotteryCode(data.substring(SRTCheckInput.INPUTRT.length()));
				}
				SharedPrinterFields.Lotteria.setLotteryMF(printerid);
				
				RTTxnType.setRefundTrx();
				
				OperatorDisplay.printSelectedDevices("EchoLineRefund", null, false, "OD");
				
				return true;
			}
			
			if (SRTPrinterExtension.isSRT()) {
				String repz = null;
				String num = null;
				String del = null;
				
				boolean isRefundable = false;
				
				String negozio = Sprint.f("%s", data.substring(SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.SSSS), SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.SSSS)+SRTCheckInput.SSSS.length()));
				String cassa = Sprint.f("%02d", data.substring(SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.CC), SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.CC)+SRTCheckInput.CC.length()));		
				del = Sprint.f("%s", data.substring(SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.YY), SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.YY)+SRTCheckInput.YY.length()*2));
				del = del + Sprint.f("%s", data.substring(SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.MM), SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.MM)+SRTCheckInput.MM.length()));
				del = del + Sprint.f("%s", data.substring(SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.DD), SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.DD)+SRTCheckInput.DD.length()));
				num = Sprint.f("%04d", data.substring(SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.NNNN), SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.NNNN)+SRTCheckInput.NNNN.length()));		
				repz = Sprint.f("%04d", data.substring(SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.ZZZZ), SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.ZZZZ)+SRTCheckInput.ZZZZ.length()));
				String serverid = "";
				if (data.length() < SRTCheckInput.INPUTSRT.length()) {
					serverid = DummyServerRT.SRTServerID;
					String parte1 = data.substring(SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.SSSS), SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.NNNN)+SRTCheckInput.NNNN.length());
					String parte2 = data.substring(SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.NNNN)+SRTCheckInput.NNNN.length());
					data = parte1+serverid+parte2;
				}
				else {
					serverid = data.substring(SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.MMMMMMMMMMM), SRTCheckInput.INPUTSRT.indexOf(SRTCheckInput.MMMMMMMMMMM)+SRTCheckInput.MMMMMMMMMMM.length());
				}
				
				System.out.println("RTRefund - negozio = <"+negozio+"> cassa = <"+cassa+"> repz = <"+repz+"> num = <"+num+"> data = <"+del+"> serverid = <"+serverid+">");
				
				OperatorDisplay.printSelectedDevices("PleaseWait",null,false,"OD");
				
				boolean noSearch = false;
				if (serialSRT == null || serialSRT.equalsIgnoreCase(DummyServerRT.SRTServerID) == false) {
					System.out.println("RTRefund - serialSRT diverso da quello del negozio:"+serialSRT+" "+DummyServerRT.SRTServerID);
				 	noSearch = true;
				}
				
				RTConsts.INTESTAZIONE4 = repz+"-"+num+" del "+del;
				
				isRefundable = DummyServerRT.pleaseDoReportReceiptRefund(repz, num, del, noSearch);
				
				System.out.println("RTRefund - isRefundable:"+isRefundable);				
				
				DummyServerRT.pleaseSetRefoundFound(isRefundable,serialSRT);		
				
				OperatorDisplay.printSelectedDevices("EchoLineRefund", null, false, "OD");	
				
				SharedPrinterFields.Lotteria.setLotteryTill(Integer.parseInt(cassa));
				DummyServerRT.setOriginalFiscalClosure(repz);
				DummyServerRT.setOriginalReceiptNumber(num);
				if (data.length() > SRTCheckInput.INPUTSRT.length()){
					SharedPrinterFields.Lotteria.setLotteryCode(data.substring(SRTCheckInput.INPUTSRT.length()));
				}
				else {
					RTTxnType.setRefundTrx();
				}
				
				return true;
			}
			
			return false;
		}
		
		public boolean RTVoid(String data) {
			
			if (!SRTCheckInput.checkInput(data, false)) {
				MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
				RTTxnType.setSaleTrx();
				return false;
			}
			
			if (SRTPrinterExtension.isPRT()) {
				if (Extra.isDeniedPostVoid()) {
					System.out.println("RTVoid - funzionalita' disabilitata");
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}
				
				boolean isVoidable = false;
				
				String cassa = Sprint.f("%02d", data.substring(SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.CC), SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.CC)+SRTCheckInput.CC.length()));
				String repz = Sprint.f("%04d", data.substring(SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.ZZZZ), SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.ZZZZ)+SRTCheckInput.ZZZZ.length()));
				String num = Sprint.f("%04d", data.substring(SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.NNNN), SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.NNNN)+SRTCheckInput.NNNN.length()));
				BasicDicoData date = new BasicDicoData (data,
						SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.DD),
						SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.DD)+SRTCheckInput.DD.length()+SRTCheckInput.MM.length()+SRTCheckInput.YY.length(),
														false);
				String printerid = "";
				if (data.length() < SRTCheckInput.INPUTRT.length()) {
					printerid = SharedPrinterFields.RTPrinterId;
					String parte1 = data.substring(SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.CC), SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.NNNN)+SRTCheckInput.NNNN.length());
					String parte2 = data.substring(SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.NNNN)+SRTCheckInput.NNNN.length());
					data = parte1+printerid+parte2;
				}
				else {
					printerid = data.substring(SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.MMMMMMMMMMM), SRTCheckInput.INPUTRT.indexOf(SRTCheckInput.MMMMMMMMMMM)+SRTCheckInput.MMMMMMMMMMM.length());
				}
				System.out.println("RTVoid - cassa = <"+cassa+"> repz = <"+repz+"> num = <"+num+"> data = <"+date.toString()+"> printerid = <"+printerid+">");
				
				if (!SRTCheckInput.chkValidDate(date.toString())) {
					System.out.println("RTVoid - funzionalita' fuori tempo massimo");
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}

				// TEMPORANEO fino a quando non si implementerà il postVoid su printer diversa 
				if (!printerid.equalsIgnoreCase(SharedPrinterFields.RTPrinterId)) {
					System.out.println("RTVoid - funzionalita' disabilitata");
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}
				// TEMPORANEO fino a quando non si implementerà il postVoid su printer diversa 
				
				TxnHeader txnHeader = SetVoidTrx.getTxnheadertovoid();
				if(txnHeader == null){
					System.out.println("RTVoid - txnHeader = null");
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}

				try {
					VoidCommands voidcmd = new VoidCommands();
					isVoidable = voidcmd.isVoidableDocument(repz, num, date.toString(), printerid);
				} catch (JposException e) {
					System.out.println("RTVoid - e:"+e.getMessage());
					isVoidable = false;
				}
				
				if (!isVoidable){
					System.out.println("RTVoid - isVoidable="+isVoidable);
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}
				
				SharedPrinterFields.Lotteria.setLotteryTill(Integer.parseInt(cassa));
				if (data.length() > SRTCheckInput.INPUTRT.length()){
					SharedPrinterFields.Lotteria.setLotteryCode(data.substring(SRTCheckInput.INPUTRT.length()));
				}
				SharedPrinterFields.Lotteria.setLotteryMF(printerid);
				
				try {
					SMTKCommands.SMTKsetVoidReceiptType();
					VoidCommands voidcmd = new VoidCommands();
					if (!voidcmd.VoidDocument(repz, num, date.toString(), printerid)) {
						MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
						RTTxnType.setSaleTrx();
						return false;
					}
					else {
						SMTKCommands.Base64_Ticket(txnHeader.getTransactionNumber(), true);
						SMTKCommands.Smart_Ticket(txnHeader.getTransactionNumber(), true);
					}
				} catch (JposException e) {
					System.out.println("RTVoid - e:"+e.getMessage());
				}
				
				RTTxnType.setSaleTrx();
				
				SetVoidTrx.resetVoidTrx();
				
				return true;
			}
			
			if (SRTPrinterExtension.isSRT()) {
				boolean isVoidable = false;
				int trxnum = 0;
				
				String cassa = Sprint.f("%02d", data.substring(SRTCheckInput.INPUT.indexOf(SRTCheckInput.CC), SRTCheckInput.INPUT.indexOf(SRTCheckInput.CC)+SRTCheckInput.CC.length()));
				String repz = Sprint.f("%05d", data.substring(SRTCheckInput.INPUT.indexOf(SRTCheckInput.ZZZZ), SRTCheckInput.INPUT.indexOf(SRTCheckInput.ZZZZ)+SRTCheckInput.ZZZZ.length()));
				String num = Sprint.f("%04d", data.substring(SRTCheckInput.INPUT.indexOf(SRTCheckInput.NNNN), SRTCheckInput.INPUT.indexOf(SRTCheckInput.NNNN)+SRTCheckInput.NNNN.length()));
				String serverid = "";
				if (data.length() < SRTCheckInput.INPUT.length()) {
					serverid = DummyServerRT.SRTServerID;
					String parte1 = data.substring(SRTCheckInput.INPUT.indexOf(SRTCheckInput.CC), SRTCheckInput.INPUT.indexOf(SRTCheckInput.NNNN)+SRTCheckInput.NNNN.length());
					String parte2 = data.substring(SRTCheckInput.INPUT.indexOf(SRTCheckInput.NNNN)+SRTCheckInput.NNNN.length());
					data = parte1+serverid+parte2;
				}
				else {
					serverid = data.substring(SRTCheckInput.INPUT.indexOf(SRTCheckInput.MMMMMMMMMMM), SRTCheckInput.INPUT.indexOf(SRTCheckInput.MMMMMMMMMMM)+SRTCheckInput.MMMMMMMMMMM.length());
				}
				System.out.println("RTVoid - cassa = <"+cassa+"> repz = <"+repz+"> num = <"+num+"> serverid = <"+serverid+">");
	
				String filename = rtsTrxBuilder.storerecallticket.Default.getRtsOutputPrefix() + "." + repz + "." + num + "." + "*";
				String[] rsh = RunShellScriptPoli20.runScript(true, "cd "+rtsTrxBuilder.storerecallticket.Default.getRtsStorePath()+"; find . -name "+filename);
				if ((rsh == null) || (rsh.length == 0)){
					System.out.println("RTVoid - <"+filename+"> non esistente");
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}
				String source = rtsTrxBuilder.storerecallticket.Default.getRtsStorePath()+rsh[0];
				String destin = rtsTrxBuilder.storerecallticket.Default.getExchangeRtsName();
				System.out.println("RTVoid - source = <"+source+"> destin = <"+destin+">");
				
				try {
					trxnum = Integer.parseInt(source.substring(source.lastIndexOf(".")+1));
				} catch (NumberFormatException e) {
					System.out.println("RTVoid - scontrino gia' annullato - trxnum="+source.substring(source.lastIndexOf(".")+1));
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}
				
				TxnHeader txnHeader = SetVoidTrx.getTxnheadertovoid();
				if(txnHeader == null){
					System.out.println("RTVoid - txnHeader = null");
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}
				
				String txndate = Sprint.f("%02d%02d%d", txnHeader.getStartDateTime().getDay(), txnHeader.getStartDateTime().getMonth()+1, txnHeader.getStartDateTime().getYear()+1900);
				if (!SRTCheckInput.chkValidDate(txndate)) {
					System.out.println("RTVoid - funzionalita' fuori tempo massimo");
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}
				
				File f = new File(source);
				if (f.exists() == false){
					System.out.println("RTVoid - <"+source+"> non esistente");
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}
				
				repz = Sprint.f("%04d", data.substring(SRTCheckInput.INPUT.indexOf(SRTCheckInput.ZZZZ), SRTCheckInput.INPUT.indexOf(SRTCheckInput.ZZZZ)+SRTCheckInput.ZZZZ.length()));
				try {
					isVoidable = DummyServerRT.pleaseDoReportReceipt(repz, num, f.getCanonicalPath(), trxnum);
				} catch (IOException e) {
					System.out.println("RTVoid - e:"+e.getMessage());
				}
				
				if (!isVoidable){
					System.out.println("RTVoid - isVoidable="+isVoidable);
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}
				
				if (!Files.copyFile(source, destin)){
					MessageBox.showMessage("WrongSequence", null, MessageBox.OK);
					RTTxnType.setSaleTrx();
					return false;
				}
				
				SharedPrinterFields.Lotteria.setLotteryTill(Integer.parseInt(cassa));
				if (data.length() > SRTCheckInput.INPUT.length()){
					SharedPrinterFields.Lotteria.setLotteryCode(data.substring(SRTCheckInput.INPUT.length()));
				}
				
				Date date = new Date(f.lastModified());
				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
				RTConsts.INTESTAZIONE4 = repz+"-"+num+" del "+sdf.format(date);
				
				RTTxnType.setVoidTrx();
				SetVoidTrx.setVoidTrx(source, txnHeader, trxnum);
				
				DummyServerRT.ReadVoided();		// legge i dati dello scontrino da annullare
				
				repz = Sprint.f("%05d", data.substring(SRTCheckInput.INPUT.indexOf(SRTCheckInput.ZZZZ), SRTCheckInput.INPUT.indexOf(SRTCheckInput.ZZZZ)+SRTCheckInput.ZZZZ.length()));
				String toprint = DummyServerRT.PrintVoided(cassa, repz, num);	// stampa lo scontrino annullato
				try {
					pleasePrintFiscalReceipt(toprint);
				} catch (Exception e1) {
					System.out.println("RTVoid - e:"+e1.getMessage());
				}
				
				try {
					SMTKCommands.SMTKsetVoidReceiptType();
					SMTKCommands.Base64_Ticket(txnHeader.getTransactionNumber(), true);
					SMTKCommands.Smart_Ticket(txnHeader.getTransactionNumber(), true);
				} catch (JposException e) {
					System.out.println("RTVoid - e:"+e.getMessage());
				}
				
				RTTxnType.setSaleTrx();
				
				SetVoidTrx.resetVoidTrx();
				
				return true;
			}
			
			return false;
		}
		
		private static void pleasePrintFiscalReceipt(String filename) throws Exception
		{
			boolean lnfm = false;

			lnfm = SRTPrinterExtension.isLikeNonFiscalMode();
			SRTPrinterExtension.setLikeNonFiscalMode(true);
			
			PrinterCommands prtcmd = SharedPrinterFields.getPrinterCommands();
			prtcmd.beginFiscalReceipt(false);
			
			prtcmd.printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			prtcmd.printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			
			File FILE = new File(filename);
			FileInputStream fstream = new FileInputStream(FILE);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			boolean doprint = false;
			while ((line = br.readLine()) != null)
			{
				if (line.startsWith(RTConsts.DESCRIZIONE))
					doprint = true;
				
				if ((!doprint) ||
					(line.startsWith(SharedPrinterFields.Lotteria.getLotteryTag())))
			    	continue;
			    
				prtcmd.printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, line.length() > RTConsts.setMAXLNGHOFLENGTH() ? line.substring(0, RTConsts.setMAXLNGHOFLENGTH()) : line);
			}	
			in.close();
			
			prtcmd.printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			prtcmd.printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			prtcmd.printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT, " ");
			
			prtcmd.endFiscalReceipt(false);
			
			SRTPrinterExtension.setLikeNonFiscalMode(lnfm);
		}
		
		private static void pleasePrintFiscalReceipt() throws Exception
		{
			//String flag;
			//int station;
			String message = null;
			String description = null;
			long price = 0;
			int quantity = 0;
			int vatInfo = 0;
			long unitPrice;
			String fullDescription = null;
			int adjustmentType = 0;
			long amount = 0;
			long total = 0;
			long payment = 0;
			long adjustment = 0;
			int id = 0;
			int data[] = new int[1];
			Object object;
			
			PrinterCommands prtcmd = SharedPrinterFields.getPrinterCommands();
			
			SAXBuilder builder = new SAXBuilder();
			FileInputStream fis = new FileInputStream(DummyServerRT.XMLfilePath);
			Document document = (Document) builder.build(fis);
//			Element rootNode = document.getRootElement().getChild(RECEIPT).getChild(PRINTERFISCALRECEIPT);
			Element rootNode = document.getRootElement();
			
			List list = rootNode.getChildren();
			for (int i = 0; i < list.size(); i++)
			{
				Element node = (Element) list.get(i);
				
				String cmd = node.getName();
				System.out.println("Xml4SRT - pleasePrint - cmd = "+cmd);
				if (cmd.equalsIgnoreCase(Xml4SRT.BEGINFISCALRECEIPT))
				{
					//flag = node.getAttributeValue("flag");
					//System.out.println(flag);
					try {
						prtcmd.beginFiscalReceipt(false);
					} catch (JposException e) {
						System.out.println(Xml4SRT.BEGINFISCALRECEIPT+": "+e.toString());
					}
				}
				//if (cmd.equalsIgnoreCase("beginNonFiscal"))
				//{
				//	try {
				//		beginNonFiscal();
				//	} catch (JposException e) {
				//		System.out.println("beginNonFiscal: "+e.toString());
				//	}
				//}
				if (cmd.equalsIgnoreCase(Xml4SRT.ENDFISCALRECEIPT))
				{
					//flag = node.getAttributeValue("flag");
					//System.out.println(flag);
					try {
						prtcmd.endFiscalReceipt(false);
					} catch (JposException e) {
						System.out.println(Xml4SRT.ENDFISCALRECEIPT+": "+e.toString());
					}
				}
				//if (cmd.equalsIgnoreCase("endNonFiscal"))
				//{
				//	try {
				//		endNonFiscal();
				//	} catch (JposException e) {
				//		System.out.println("endNonFiscal: "+e.toString());
				//	}
				//}
				//if (cmd.equalsIgnoreCase("printNormal"))
				//{
				//	station = Integer.parseInt(node.getAttributeValue("station"));
				//	message = node.getAttributeValue("message");
				//	System.out.println(""+station);
				//	System.out.println(message);
				//	try {
				//		printNormal(station, message);
				//	} catch (JposException e) {
				//		System.out.println("printNormal: "+e.toString());
				//	}
				//}
				if (cmd.equalsIgnoreCase(Xml4SRT.PRINTRECITEM))
				{
					description = node.getAttributeValue(Xml4SRT.DESCRIPTION);
					//price = Long.parseLong(node.getAttributeValue("price"));
					//quantity = Integer.parseInt(node.getAttributeValue(QUANTITY));
					quantity = (int)(Double.parseDouble(node.getAttributeValue(Xml4SRT.QUANTITY)));
					vatInfo = Integer.parseInt(node.getAttributeValue(Xml4SRT.VATID));
					unitPrice = Long.parseLong(""+(int)(Double.parseDouble(node.getAttributeValue(Xml4SRT.UNITPRICE).replace(',', '.'))*10000));
					//fullDescription = node.getAttributeValue("fullDescription");
					price = unitPrice;
					fullDescription = description;
					System.out.println(description);
					System.out.println("Xml4SRT - pleasePrint - "+price);
					System.out.println("Xml4SRT - pleasePrint - "+quantity);
					System.out.println("Xml4SRT - pleasePrint - "+vatInfo);
					System.out.println("Xml4SRT - pleasePrint - "+unitPrice);
					System.out.println("Xml4SRT - pleasePrint - "+fullDescription);
					try {
						prtcmd.printRecItem(description, price, quantity, vatInfo, unitPrice, fullDescription);
					} catch (JposException e) {
						System.out.println(Xml4SRT.PRINTRECITEM+": "+e.toString());
					}
				}
				if (cmd.equalsIgnoreCase(Xml4SRT.PRINTRECITEMADJUSTMENT))
				{
					adjustmentType = Integer.parseInt(node.getAttributeValue(Xml4SRT.ADJUSTMENTTYPE));
					description = node.getAttributeValue(Xml4SRT.DESCRIPTION);
					amount = Long.parseLong(node.getAttributeValue(Xml4SRT.AMOUNT));
					vatInfo = Integer.parseInt(node.getAttributeValue(Xml4SRT.VATID));
					System.out.println("Xml4SRT - pleasePrint - "+adjustmentType);
					System.out.println("Xml4SRT - pleasePrint - "+description);
					System.out.println("Xml4SRT - pleasePrint - "+amount);
					System.out.println("Xml4SRT - pleasePrint - "+vatInfo);
					try {
						prtcmd.printRecItemAdjustment(adjustmentType, description, amount, vatInfo);
					} catch (JposException e) {
						System.out.println(Xml4SRT.PRINTRECITEMADJUSTMENT+": "+e.toString());
					}
				}
				if (cmd.equalsIgnoreCase(Xml4SRT.PRINTRECSUBTOTAL))
				{
					amount = Long.parseLong(node.getAttributeValue(Xml4SRT.AMOUNT));
					System.out.println("Xml4SRT - pleasePrint - "+amount);
					try {
						prtcmd.printRecSubtotal(amount);
					} catch (JposException e) {
						System.out.println(Xml4SRT.PRINTRECSUBTOTAL+": "+e.toString());
					}
				}
				if (cmd.equalsIgnoreCase(Xml4SRT.PRINTRECMESSAGE))
				{
					message = node.getAttributeValue(Xml4SRT.MESSAGE);
					System.out.println("Xml4SRT - pleasePrint - "+message);
					try {
						prtcmd.printRecMessage(message);
					} catch (JposException e) {
						System.out.println(Xml4SRT.PRINTRECMESSAGE+": "+e.toString());
					}
				}
				if (cmd.equalsIgnoreCase(Xml4SRT.PRINTRECSUBTOTALADJUSTMENT))
				{
					adjustmentType = Integer.parseInt(node.getAttributeValue(Xml4SRT.ADJUSTMENTTYPE));
					description = node.getAttributeValue(Xml4SRT.DESCRIPTION);
					amount = Long.parseLong(""+(int)(Double.parseDouble(node.getAttributeValue(Xml4SRT.AMOUNT))*10000));
					vatInfo = Integer.parseInt(node.getAttributeValue(Xml4SRT.VATID));
					//System.out.println("Xml4SRT - pleasePrint - "+adjustmentType);
					//System.out.println("Xml4SRT - pleasePrint - "+description);
					//System.out.println("Xml4SRT - pleasePrint - "+amount);
					//System.out.println("Xml4SRT - pleasePrint - "+vatInfo);
					try {
						if (SRTPrinterExtension.isSRT())
							prtcmd.printRecSubtotalAdjustment(adjustmentType, "", amount);
						
				        String out = prtcmd.buildSubtotalAdjustment ( description, amount );
				        prtcmd.printNormal(jpos.FiscalPrinterConst.FPTR_S_RECEIPT,out);
					} catch (JposException e) {
						System.out.println(Xml4SRT.PRINTRECSUBTOTALADJUSTMENT+": "+e.toString());
					}
			    	HardTotals.doSubtotalAdjustment(amount);
				}
				if (cmd.equalsIgnoreCase(Xml4SRT.PRINTRECTOTAL))
				{
					total = Long.parseLong(""+(int)(Double.parseDouble(node.getAttributeValue(Xml4SRT.RECEIPTAMOUNT).replace(',', '.'))*10000));
					payment = Long.parseLong(""+(int)(Double.parseDouble(node.getAttributeValue(Xml4SRT.PAYMENT).replace(',', '.'))*10000));
					description = node.getAttributeValue(Xml4SRT.DESCRIPTION);
					System.out.println("Xml4SRT - pleasePrint - "+total);
					System.out.println("Xml4SRT - pleasePrint - "+payment);
					System.out.println("Xml4SRT - pleasePrint - "+description);
					try {
						prtcmd.printRecTotal(total, payment, description);
					} catch (JposException e) {
						System.out.println(Xml4SRT.PRINTRECTOTAL+": "+e.toString());
					}
				}
				if (cmd.equalsIgnoreCase(Xml4SRT.PRINTRECVOID))
				{
					description = node.getAttributeValue(Xml4SRT.DESCRIPTION);
					System.out.println("Xml4SRT - pleasePrint - "+description);
					try {
						prtcmd.printRecVoid(description);
					} catch (JposException e) {
						System.out.println(Xml4SRT.PRINTRECVOID+": "+e.toString());
					}
				}
				if (cmd.equalsIgnoreCase(Xml4SRT.PRINTRECITEMVOID))
				{
					description = node.getAttributeValue(Xml4SRT.DESCRIPTION);
					//amount = Long.parseLong(node.getAttributeValue("amount"));
					amount = Long.parseLong(""+(int)(Double.parseDouble(node.getAttributeValue(Xml4SRT.UNITPRICE))*10000));
					//quantity = Integer.parseInt(node.getAttributeValue(QUANTITY));
					quantity = Integer.parseInt(""+(int)(Double.parseDouble(node.getAttributeValue(Xml4SRT.QUANTITY))*1000));
					//adjustmentType = Integer.parseInt(node.getAttributeValue("adjustmentType"));
					//adjustment = Long.parseLong(node.getAttributeValue("adjustment"));
					adjustmentType = 1;
					adjustment = amount;
					vatInfo = Integer.parseInt(node.getAttributeValue(Xml4SRT.VATID));
					System.out.println("Xml4SRT - pleasePrint - "+description);
					System.out.println("Xml4SRT - pleasePrint - "+amount);
					System.out.println("Xml4SRT - pleasePrint - "+quantity);
					System.out.println("Xml4SRT - pleasePrint - "+adjustmentType);
					System.out.println("Xml4SRT - pleasePrint - "+adjustment);
					System.out.println("Xml4SRT - pleasePrint - "+vatInfo);
					try {
						prtcmd.printRecVoidItem(description, amount, quantity, adjustmentType, adjustment, vatInfo);
					} catch (JposException e) {
						System.out.println(Xml4SRT.PRINTRECITEMVOID+": "+e.toString());
					}
				}
				if (cmd.equalsIgnoreCase(Xml4SRT.PRINTRECREFUNDVOID))
				{
					description = Xml4SRT.CorrezioneReso;
					quantity = 1;
					unitPrice = amount;
					price = unitPrice;
					fullDescription = description;
					System.out.println("Xml4SRT - pleasePrint - "+description);
					System.out.println("Xml4SRT - pleasePrint - "+price);
					System.out.println("Xml4SRT - pleasePrint - "+quantity);
					System.out.println("Xml4SRT - pleasePrint - "+vatInfo);
					System.out.println("Xml4SRT - pleasePrint - "+unitPrice);
					System.out.println("Xml4SRT - pleasePrint - "+fullDescription);
					try {
						prtcmd.printRecItem(description, price, quantity, vatInfo, unitPrice, fullDescription);
					} catch (JposException e) {
						System.out.println(Xml4SRT.PRINTRECREFUNDVOID+": "+e.toString());
					}
				}
				//if (cmd.equalsIgnoreCase("resetPrinter"))
				//{
				//	try {
				//		resetPrinter();
				//	} catch (JposException e) {
				//		System.out.println("resetPrinter: "+e.toString());
				//	}
				//}
				if (cmd.equalsIgnoreCase("directIO"))
				{
					id = Integer.parseInt(node.getAttributeValue(Xml4SRT.ID));
					data[0] = Integer.parseInt(node.getAttributeValue(Xml4SRT.DATA));
					object = node.getAttributeValue(Xml4SRT.OBJECT);
					try {
						prtcmd.directIO(id, data, object);
					} catch (JposException e) {
						System.out.println("directIO: "+e.toString());
					}
				}
				if (cmd.equalsIgnoreCase(Xml4SRT.PRINTRECREFUND))
				{
					description = node.getAttributeValue(Xml4SRT.DESCRIPTION);
					amount = Long.parseLong(""+(int)(Double.parseDouble(node.getAttributeValue(Xml4SRT.UNITPRICE))*10000));
					vatInfo = Integer.parseInt(node.getAttributeValue(Xml4SRT.VATID));
					System.out.println("Xml4SRT - pleasePrint - "+description);
					System.out.println("Xml4SRT - pleasePrint - "+amount);
					System.out.println("Xml4SRT - pleasePrint - "+vatInfo);
					try {
						prtcmd.printRecRefund(description, amount, vatInfo);
					} catch (JposException e) {
						System.out.println(Xml4SRT.PRINTRECREFUND+": "+e.toString());
					}
				}
				//if (cmd.equalsIgnoreCase("printZReport"))
				//{
				//	try {
				//		printZReport();
				//	} catch (JposException e) {
				//		System.out.println("printZReport: "+e.toString());
				//	}
				//}
			}
		}
		
		public void setPaperSavingMode()
		{
			if (fiscalPrinterDriver.isfwRT2disabled())
				return;
			
			if (SRTPrinterExtension.isPRT())
			{
			}
		}		

		public void setRoundingMode(int mode)
		{
			if (fiscalPrinterDriver.isfwRT2disabled())
				return;
			
			System.out.println("RT2 - setRoundingMode - mode = "+mode);
			
			if (SRTPrinterExtension.isPRT())
			{
				StringBuffer key = new StringBuffer(SharedPrinterFields.KEY_SRV);
				fiscalPrinterDriver.executeRTDirectIo(0, 0, key);
	
				int Kind = mode;
				StringBuffer sbcmd = new StringBuffer(">C928/&8/$"+Kind);
				fiscalPrinterDriver.executeRTDirectIo(0, 0, sbcmd);
	
				key = new StringBuffer(SharedPrinterFields.KEY_REG);
				fiscalPrinterDriver.executeRTDirectIo(0, 0, key);
				
				if ((mode == RTConsts.FULLROUNDING) || (mode == RTConsts.NEGATIVEROUNDING)) {
					if (fw < 8.3) {
						enabledLowerRoundedPay = enableLowerRoundedPay(0);
						enabledLowerRoundedPay = -1;
					}
					else {
						enabledLowerRoundedPay = enableLowerRoundedPay(1);
					}
				}
				else
					enabledLowerRoundedPay = enableLowerRoundedPay(0);
				System.out.println("RT2 - setRoundingMode - enabledLowerRoundedPay = "+enabledLowerRoundedPay);
			}
			
			RTConsts.setCURRENTROUNDING(mode);
		}
		
		private String getRoundingMode()
		{
			String Kind = Sprint.f("%03d", RTConsts.ROUNDINGDISABLE);
			
			if (fiscalPrinterDriver.isfwRT2enabled())
			{
			}
	      	
			System.out.println("RT2 - getRoundingMode - Kind = "+Kind);
			return (Kind);
		}
		
		private int enableLowerRoundedPay(int mode)
		{
			int ret = -1;
			
			if (fiscalPrinterDriver.isfwRT2disabled())
				return ret;
			
			System.out.println("RT2 - enableLowerRoundedPay - mode = "+mode);
			
			StringBuffer sbcmd = new StringBuffer(">C928/&11/$"+mode);
			ret = fiscalPrinterDriver.executeRTDirectIo(0, 0, sbcmd);
			
			System.out.println("RT2 - enableLowerRoundedPay - ret = "+ret);
			return ret;
		}

		public void printBarcode(String bc) {
			int[] data = new int[25];

			System.out.println ( "MAPOTO-EXEC PRINT BARCODE "+bc );
				
			int l13 = bc.length()-R3define.getBARCODEHEADEREAN13().length();
			int l128 = bc.length()-R3define.getBARCODEHEADEREAN128().length();
			int lqrc = bc.length()-R3define.getQRCODEHEADER().length();
			int a = l13 -12;
			
			if (SRTPrinterExtension.isPRT())
			{
				String barcode = "";
                int[] mydata = {0};
                
				if (bc.startsWith(R3define.getBARCODEHEADEREAN13()))
				{
			    	barcode = bc.substring(bc.length()-l13);
					barcode = Checksum.makeEANCheck(barcode);
                    mydata[0] = 2;
				}
				else if (bc.startsWith(R3define.getBARCODEHEADEREAN128()))
				{
			    	barcode = bc.substring(bc.length()-l128);
                    mydata[0] = 8;
				}
				else if (bc.startsWith(R3define.getQRCODEHEADER()))
				{
			    	barcode = bc.substring(bc.length()-lqrc);
                    mydata[0] = 11;
				}
				try {
					this.directIO(5000, mydata, barcode);
				} catch (JposException e) {
					System.out.println("printBarcode - exception: "+e.getMessage());
				}
			}
			else
			{
/*				="/$2/(4006381333641)         -> stampa codice a barre EAN-13
				="/$1/(02345678901)           -> stampa codice a barre UPC-E 
				="/$3/(80369691)              -> stampa codice a barre EAN-8
				="/$4/(ABCDEFGH)              -> stampa codice a barre CODE-39
				="/$5/(042110000082)          -> stampa codice a barre UPC-A
				="/$6/(012345678902)          -> stampa codice a barre ITF
				="/$7/(0123456)               -> stampa codice a barre CODABAR 
				="/$8/(ABCDEFGHILMN)          -> stampa codice a barre CODE-128
				="/$9/(ABCDEFGH)              -> stampa codice a barre CODE-93
				="/$10/(12345678901)          -> conversione barcode UPC-E in UPC-A 
				="/$11/(abc123)               -> stampa codice QR (max. 45 caratteri)*/
                try {
                    int cmdInt = 1;
                    int[] mydata = {1};
                    String cmd = "";
					if (bc.startsWith(R3define.getBARCODEHEADEREAN13()))
					{
				    	String barcode = bc.substring(bc.length()-l13);
						barcode = Checksum.makeEANCheck(barcode);
						cmd = "=\"/$2/("+barcode+")";
					}
					else if (bc.startsWith(R3define.getBARCODEHEADEREAN128()))
					{
				    	String barcode = bc.substring(bc.length()-l128);
						cmd = "=\"/$8/("+barcode+")";
					}
					else if (bc.startsWith(R3define.getQRCODEHEADER()))
					{
				    	String barcode = bc.substring(bc.length()-lqrc);
						cmd = "=\"/$11/("+barcode+")";
					}
                    this.directIO(cmdInt, mydata, cmd);
				} catch (JposException e) {
					System.out.println("printBarcode - exception: "+e.getMessage());
				}
			}
		}

		protected boolean MixedVat()
		{
			// controlla se siamo in regime di Iva Ventilata
			
			//System.out.println("MixedVat - vat = " + vat);
			
			boolean ret = false;
			
			// non so in quale altra maniera capire se la stampante Rtone lavora in iva ventilata
			// attenzione che se nella giornata la stampante non ha lavorato mi resta ivaventilata=false anche quando
			// l'impostazione e' in iva ventilata, però non avendo lavorato i danni dovrebbero essere limitati (spero) 
			String[] dailyreport = VatReport(0, 40);
			for (int i=0; i < dailyreport.length; i++) {
				String vattype = "";
				if (dailyreport[i].length() == 32) {
					vattype = dailyreport[i].substring(3, 4);
					if (Integer.parseInt(vattype) == 2) {
						// iva ventilata
						ret = true;
						break;
					}
				}
			}
			
			//System.out.println("MixedVat - ret = " + ret);
			return ret;
		}
		
		private boolean MixedVat(String vat)
		{
			// controlla se siamo in regime di Iva Ventilata
			
			//System.out.println("MixedVat - vat = " + vat);
			
			boolean ret = false;
			
			int rtcode = Integer.parseInt(vat);
			if (rtcode >= 15 && rtcode <= 23)
				ret = true;
			
			//System.out.println("MixedVat - ret = " + ret);
			return ret;
		}
		
		public void beginFiscalDocument(int amount) throws JposException {
			fiscalPrinterDriver.beginFiscalDocument(amount);
		}
		
		public void beginTraining() throws JposException {
			fiscalPrinterDriver.beginTraining();
		}
		
		public void endFiscalDocument() throws JposException {
			fiscalPrinterDriver.endFiscalDocument();
		}
		
		public void endTraining() throws JposException {
			fiscalPrinterDriver.endTraining();
		}
		
		public void printFiscalDocumentLine(String documentLine) throws JposException {
			fiscalPrinterDriver.printFiscalDocumentLine(documentLine);
		}
		
		public boolean getTrainingModeActive() throws JposException {
			return fiscalPrinterDriver.getTrainingModeActive();
		}
		
		public boolean getNoReactionStatus() throws JposException {
			return fiscalPrinterDriver.getNoReactionStatus();
		}
		
		public static void clearError() throws JposException {
			fiscalPrinterDriver.clearError();
		}
		
		public void SMTKreadProperties()
		{
			SmartTicket.setSmart_Ticket(SmartTicketProperties.isSmartTicket());
			SmartTicket.setBase64_Ticket(SmartTicketProperties.isBase64Ticket());
			SmartTicket.Base64_Decode = SmartTicketProperties.getBase64Decode();
			SmartTicket._Smart_Ticket_ReceiptMode = SmartTicketProperties.getSmartTicketMode();
			
			System.out.println("Open - SmartTicket = "+SmartTicketProperties.isSmartTicket());
			System.out.println("Open - ServerUrl = "+SmartTicketProperties.getServerUrl());
			System.out.println("Open - SmartTicketMode = "+SmartTicketProperties.getSmartTicketMode());
			System.out.println("Open - Base64Ticket = "+SmartTicketProperties.isBase64Ticket());
			System.out.println("Open - Base64Decode = "+SmartTicketProperties.getBase64Decode());
			
			if (SRTPrinterExtension.isPRT()) {
		    	SmartTicket.setSmart_Ticket(SmartTicket.isSmart_Ticket() && fiscalPrinterDriver.isfwSMTKenabled());
				System.out.println("SMTK - SmartTicket : "+SmartTicket.isSmart_Ticket());
			}
			
		    if (SmartTicket.isSmart_Ticket())
		    {
		    	SmartTicket.Smart_Ticket_Mode = SmartTicketProperties.getServerUrl();
				System.out.println("SMTK - ServerUrl : "+SmartTicket.Smart_Ticket_Mode);
		    	if (SmartTicket.Smart_Ticket_Mode.equalsIgnoreCase("OFF"))
		    		SmartTicket.Smart_Ticket_Mode = SmartTicket.ERECEIPT_URL_SERVER_DISABLE;
		    	else if (SmartTicket.Smart_Ticket_Mode.equalsIgnoreCase("PULL"))
		    		SmartTicket.Smart_Ticket_Mode = SmartTicket.ERECEIPT_URL_SERVER_PULL;
		    	
		    	if (SmartTicket.Smart_Ticket_Mode.equalsIgnoreCase(SmartTicket.ERECEIPT_URL_SERVER_DISABLE)) {
		    		SmartTicket.setSmart_Ticket(false);
		    		SmartTicket.Smart_Ticket_ReceiptMode = SmartTicket.ERECEIPT_PAPER;
		    		SmartTicket.Smart_Ticket_Validity = SmartTicket.ERECEIPT_VALIDITY_ALL;
		    	}
		    	else {
		    		// Default setting
		    		SmartTicket.Smart_Ticket_ReceiptMode = SmartTicket._Smart_Ticket_ReceiptMode;
		    		SmartTicket.Smart_Ticket_Validity = SmartTicket.ERECEIPT_VALIDITY_ALL;
		    	}
	    		
		    	SmartTicket.SMTKsaveDefault();
	    		
				if (SRTPrinterExtension.isPRT()) {
		    		fiscalPrinterDriver.SMTKsetServerUrl(SmartTicket.Smart_Ticket_Mode);
		    		fiscalPrinterDriver.SMTKsetReceiptType(SmartTicket.Smart_Ticket_ReceiptMode, SmartTicket.Smart_Ticket_Validity);
		    		fiscalPrinterDriver.SMTKsetCustomerID(SmartTicket.Smart_Ticket_CustomerType, SmartTicket.Smart_Ticket_CustomerId);
		    		fiscalPrinterDriver.SMTKStatus();
		    	}
		    }
		    else
		    {
		    	// se ci  fosse già il fw per smart ticket disabilito tutto
	    		fiscalPrinterDriver.SMTKsetServerUrl(SmartTicket.ERECEIPT_URL_SERVER_DISABLE);
	    		fiscalPrinterDriver.SMTKsetReceiptType(SmartTicket.ERECEIPT_PAPER, SmartTicket.ERECEIPT_VALIDITY_ALL);
		    }
		}
		
		public void reprintLastTicket() {
			fiscalPrinterDriver.reprintLastTicket();
		}
		
		public void fwUpdate() {
			fiscalPrinterDriver.fwUpdate();
		}
		
		public static String getBarcodePrefix() {
			return R3define.getBarcodePrefix();
		}

		public static String myRchFiscalNumber() {
			return fiscalPrinterDriver.myRchFiscalNumber;
		}

		public void bitmap(String filename, int width, int height, int align) {
			fiscalPrinterDriver.bitmap(filename, width, height, align);
		}
		
	    public static String readFile(String filePath, boolean appendLf) {
	        StringBuilder sb = new StringBuilder();
	        BufferedReader in = null;
	        
	        try {
	            in = new BufferedReader(new FileReader(filePath));
	            String line;
	            while ((line = in.readLine()) != null) {
	                sb.append(line);
	                if (appendLf) {
	                    sb.append(R3define.Lf);
	                }
	            }
	        } catch (Exception e) {
	            System.out.println("readFile - Exception : " + e.getMessage());
	            return "";
	        } finally {
	            if (in != null) {
	                try {
	                    in.close();
	                } catch (IOException e) {
	                    System.out.println("readFile - Exception : " + e.getMessage());
	                    return "";
	                }
	            }
	        }
	        
	        return sb.toString();
	    }
		
	    private static String hideSomething(String in)
	    {
	    	String tokenstart = EjTokens.getTokenStart().trim();
	    	String tokenstop  = EjTokens.getTokenStop().trim();
	    	
	    	if (tokenstart == null || tokenstart.length() == 0)
	    		return in;
	    	if (tokenstop == null || tokenstop.length() == 0)
	    		return in;
	    	
	    	String out = "";
	    	if (in == null || in.length() == 0) {
	    		return out;
	    	}
	    	
	    	if (in.indexOf(tokenstart.trim()) < 0)
	    		return in;	// lascio tutto com'è
	    	
	    	//System.out.println("hideSomething - in = "+in);
	    	
	        String[] lines = in.split("\n");
	        
	        boolean betweenMarkers = false;

	        for (String line : lines) {
	            if (line.contains(tokenstart)) {
	                betweenMarkers = true;
	                continue;
	            }

	            if (line.contains(tokenstop)) {
	                betweenMarkers = false;
	                continue;
	            }

	            if (betweenMarkers && (line.trim().isEmpty())) {
	                continue;
	            }

	            out = out + line + "\n";
	        }
	        
	    	//System.out.println("hideSomething - out = "+out);
	    	
	    	return out;
	    }
				
	    protected int getSimulation()
	    {
	    	return fiscalPrinterDriver.getSimulation();
	    }
	    
		protected String getZRepIdAnswer(int zrep)
		{
			String ZRepId = "";
			
			// c'è qualche altro modo per capire quando la trasmissione/ricezione è completata ?
			ZRepId = "123456789012";
				
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
			}
			
			monitorRT.logMRT("getZRepIdAnswer - returning : "+ZRepId);
			return ZRepId;
		}
		
		protected String[] VatReport(int reporttype, int type)
		{
			String reply[] = new String[0];
			
			if (type == 40)
				type = 0;			// Total gross amount of sales (goods+services) by VAT rate
			else if (type == 41)
				type = 20;			// Total gross amount of refund documents by VAT rate
			else if (type == 42)
				type = 10;			// Total gross amount of void documents by VAT rate
			
			return (fiscalPrinterDriver.DailyPeriodicReport(reporttype, type));
		}
		
		protected String[] VatReport(int type, int zrepnum, String docnum, String date)
		{
			String reply[] = new String[0];
			
			return (fiscalPrinterDriver.SingleDocumentReport(type, zrepnum, docnum, date));
		}
		
		protected String[] PaymentReport(int reporttype, int type)
		{
			String reply[] = new String[0];
			
				
			type = 30;				// Amount collected for a single payment type
				
			return (fiscalPrinterDriver.DailyPeriodicReport(reporttype, type));
		}

	    private void checkPrinterPaperLow()
	    {
	    	while (true)
	    	{
	    		
		    	System.out.println("200726 - checkPrinterPaperLow");
		    	
		    	boolean errorFound = false;
		    	boolean ret = false;
		    	
				try {
					ret = fiscalPrinterDriver.getCoverOpen();
				} catch (JposException e) {
			    	System.out.println("200726 - getCoverOpen : "+e.getMessage());
			    	errorFound = true;
			    	ret = true;
				}
				System.out.println("200726 - Cover Open : " + ret);
				if(ret) {
			    	errorFound = true;
				    MessageBox.showMessage("CoverOpen",null,MessageBox.OK);
				}
				
		    	ret = false;
		    	
				try {
					ret = fiscalPrinterDriver.getRecEmpty();
				} catch (JposException e) {
			    	System.out.println("200726 - getRecEmpty : "+e.getMessage());
			    	errorFound = true;
			    	ret = true;
				}
				System.out.println("200726 - Receipt Empty : " + ret);
				if(ret) {
			    	errorFound = true;
				    MessageBox.showMessage("ReceiptEmpty",null,MessageBox.OK);
				}
		
				if (ret == false) {
					try {
						ret = fiscalPrinterDriver.getRecNearEnd();
					} catch (JposException e) {
				    	System.out.println("200726 - getReceiptNearEnd : "+e.getMessage());
				    	errorFound = true;
				    	ret = true;
					}
					System.out.println("200726 - Receipt Near End : " + ret);
					if(ret) {
				    	errorFound = true;
					    MessageBox.showMessage("ReceiptLow",null,MessageBox.OK);
					}
				}
				
		    	ret = false;
		    	
				try {
					ret = fiscalPrinterDriver.getJrnNearEnd();
				} catch (JposException e) {
			    	System.out.println("200726 - getJournalNearEnd : "+e.getMessage());
			    	errorFound = true;
			    	ret = true;
				}
				System.out.println("200726 - Journal Near End : " + ret);
				if(ret) {
//			    	errorFound = true;
				    MessageBox.showMessage("JournalLow",null,MessageBox.OK);
				}
				
				if (!errorFound)
					break;
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				
	    	}
	    }
		
}
