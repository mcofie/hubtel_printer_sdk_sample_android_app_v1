package com.hubtel.hubtelprinters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;


import com.epson.epos2.ConnectionListener;
import com.epson.epos2.Epos2Exception;
import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;
import com.hubtel.hubtelprinters.Delegates.PrinterConnectionDelegate;
import com.hubtel.hubtelprinters.Delegates.PrinterSeachDelegate;
import com.hubtel.hubtelprinters.Delegates.PrintingTaskDelegate;
import com.hubtel.hubtelprinters.printerCore.Communication;
import com.hubtel.hubtelprinters.printerCore.PrinterConstants;
import com.hubtel.hubtelprinters.printerCore.PrinterModel;
import com.hubtel.hubtelprinters.printerCore.PrinterModelCapacity;
import com.hubtel.hubtelprinters.receiptbuilder.HubtelDeviceInfo;
import com.hubtel.hubtelprinters.receiptbuilder.ReceiptCreator;
import com.hubtel.hubtelprinters.receiptbuilder.ReceiptObject;
import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.StarIoExt;
import com.epson.epos2.discovery.Discovery;
import com.epson.epos2.discovery.DiscoveryListener;
import com.epson.epos2.discovery.FilterOption;
import com.epson.epos2.discovery.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

import static com.hubtel.hubtelprinters.printerCore.PrinterConstants.PREF_KEY_ACTIVE_HUBTEL_DEVICE;
import static com.hubtel.hubtelprinters.printerCore.PrinterConstants.PREF_KEY_ALLRECEIPTS_SETTINGS;
import static com.hubtel.hubtelprinters.printerCore.PrinterConstants.PREF_KEY_PRINTER_SETTINGS_JSON;


public class PrinterManager {



    private Context activity;
    private List<PrinterModel> printermodelList;
    private int mAllReceiptSettings;
    private FilterOption mFilterOption = null;

    private int       mModelIndex;
    private String    mPortName;
    private String    mPortSettings;
    private String    mMacAddress;
    private String    mModelName;
    private String mManufacturer;
    private Boolean   mDrawerOpenStatus;
    private int       mPaperSize;
   // private Activity activity;
    private List<PortInfo> portList;
    private  List<HubtelDeviceInfo> hubtelDeviceInfoList;
    private static HubtelDeviceInfo activeHubtelDevice;
    //public PrinterManagerDelegate delegate=null;
    public PrinterSeachDelegate seachDelegate=null;
    public PrinterConnectionDelegate connectionDelegate=null;
    public PrintingTaskDelegate printingTaskDelegate=null;
    private Printer mPrinter = null;
    private PrinterModel printerModel;
    SharedPreferences prefs;



    private int       mPrinterSettingIndex = 0;

    private boolean            mTryConnect = false;


    private static StarIOPort port = null;


    public  PrinterManager(){}
/*
    public PrinterManager(Activity _activity){



        portList = new ArrayList<PortInfo>();
        hubtelDeviceInfoList = new ArrayList<>();
        this.activity = _activity;
        mContext = _activity;



        try{

            prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            if (!prefs.contains(PREF_KEY_PRINTER_SETTINGS_JSON)) {
                prefs.edit()
                        .clear()
                        .apply();
            }
            printermodelList = JsonUtils.createPrinterSettingListFromJsonString(prefs.getString(PREF_KEY_PRINTER_SETTINGS_JSON, ""));
            if (printermodelList.size() > 0){

                printerModel = getSavedPrinterModel();
            }
            activeHubtelDevice = getActiveHubtelDevice();

        }catch (Exception e){


        }

       // initEpsonPrinter();




    }
*/
    public PrinterManager(Context _activity){



        portList = new ArrayList<PortInfo>();
        hubtelDeviceInfoList = new ArrayList<>();
        this.activity = _activity;
       // activity = _activity;



        try{

            prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            if (!prefs.contains(PREF_KEY_PRINTER_SETTINGS_JSON)) {
                prefs.edit()
                        .clear()
                        .apply();
            }
            printermodelList = JsonUtils.createPrinterSettingListFromJsonString(prefs.getString(PREF_KEY_PRINTER_SETTINGS_JSON, ""));
            if (printermodelList.size() > 0){

                printerModel = getSavedPrinterModel();
            }
            activeHubtelDevice = getActiveHubtelDevice();

        }catch (Exception e){


        }

        // initEpsonPrinter();




    }

    private Printer initEpsonPrinter() {

        try {
            mPrinter = new Printer(0, 0, activity);

            mPrinter.setReceiveEventListener(epSonreceiveListener);
            mPrinter.setConnectionEventListener(epSonconnectionListener);


        } catch (Exception e) {

        }



        return mPrinter;

    }




    private boolean connectPrinter(HubtelDeviceInfo deviceInfo) {
        boolean isBeginTransaction = false;

        if (mPrinter == null) {
            return false;
        }

        try {
            Log.d("Debug",deviceInfo.getTarget());
            mPrinter.connect(deviceInfo.getTarget(), Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {

            return false;
        }

        try {
            mPrinter.beginTransaction();
            isBeginTransaction = true;
        }
        catch (Exception e) {

        }

        if (isBeginTransaction == false) {
            try {
                mPrinter.disconnect();
            }
            catch (Epos2Exception e) {
                // Do nothing
                return false;
            }
        }

        return true;
    }




    public void unRegisterReceiver(){


        try {

            Discovery.stop();


            if(mPrinter != null) {
                mPrinter.disconnect();

                mPrinter.setReceiveEventListener(null);
                mPrinter.setConnectionEventListener(null);
                mPrinter.setStatusChangeEventListener(null);
            }


        }
        catch (Epos2Exception e) {

        }


    }




    public void disconnectPrinter(HubtelDeviceInfo deviceInfo){



        try {

            if (!prefs.contains(PREF_KEY_PRINTER_SETTINGS_JSON)) {
                prefs.edit()
                        .clear()
                        .apply();
            }
            printermodelList = JsonUtils.createPrinterSettingListFromJsonString(prefs.getString(PREF_KEY_PRINTER_SETTINGS_JSON, ""));


            printermodelList.clear();

        }catch (Exception e){


        }



    }


    private void disconnectPrinter() {

        /**
        if (mPrinter == null) {
            return;
        }

        try {
            mPrinter.endTransaction();
        }
        catch (final Exception e) {
          //delegate.printerConnectionFailed("Failed to disconnect Printer");
        }

        try {
            mPrinter.disconnect();
        }
        catch (final Exception e) {
          //  delegate.printerConnectionFailed("Failed to disconnect Printer");
        }

        finalizeObject();

         **/



    }

    public void finalizeObject() {
        if (mPrinter == null) {
            return;
        }

        mPrinter.clearCommandBuffer();

        mPrinter.setReceiveEventListener(null);
        mPrinter.setConnectionEventListener(null);
        mPrinter.setStatusChangeEventListener(null);

        mPrinter = null;
    }

    public StarIOPort getPort() {

         return  port;
     }

     public boolean isPortOpened(){

         if(port == null)
             return  false;
         else
             return true;

     }

    public  String getPrinterName(){


         return
                 this.mPortName.substring(PrinterConstants.IF_TYPE_BLUETOOTH.length());
    }

    public  void connectToPrinter(final  HubtelDeviceInfo portInfo){

                setActiveHubtelDevice(portInfo);

                switch (portInfo.getDeviceManufacturer()){


                    case "Epson":

                        Runnable task = new Runnable() {
                            @Override
                            public void run() {


                                PrinterConnectionTask task1 = new PrinterConnectionTask(activity);
                                task1.connectEpsonPrinter(portInfo,connectionDelegate,epSonconnectionListener);
                               // connectEpsonPrinterRaw(portInfo);
                            }
                        };
                        task.run();


                        break;
                    case "Star":

                        PrinterConnectionTask task1 = new PrinterConnectionTask(activity);
                        task1.connectToStarPrinter(printermodelList,portInfo,connectionDelegate,prefs);
                      //  (List<PrinterModel> printermodelList,HubtelDeviceInfo deviceInfo , PrinterConnectionDelegate delegate, SharedPreferences prefs)
                       // connectToStarPrinter(portInfo);
                        break;
                }








    }


   /** private boolean connectEpsonPrinterRaw(HubtelDeviceInfo deviceInfo) {
        boolean isBeginTransaction = false;
        delegate.printerConnectionBegan();
        if (mPrinter == null) {
        return false;
    }

        try {

        mPrinter.setConnectionEventListener(epSonconnectionListener);
        mPrinter.connect((deviceInfo.getTarget()), Printer.PARAM_DEFAULT);

        delegate.printerConnectionSuccess(deviceInfo);
    }
        catch (Exception e) {


        delegate.printerConnectionFailed(e.getLocalizedMessage() + "Epson connection error");

        return false;
    }

        try {
        mPrinter.beginTransaction();
        isBeginTransaction = true;
    }
        catch (Exception e) {

        delegate.printerConnectionFailed(e.getLocalizedMessage() + "Epson beginTransaction error");

    }

        if (isBeginTransaction == false) {
        try {
            mPrinter.disconnect();
        }
        catch (Epos2Exception e) {
            // Do nothing
            return false;
        }
    }

        return true;
}

    private void connectToStarPrinter(HubtelDeviceInfo portInfo){


        delegate.printerConnectionBegan();
        this.mManufacturer = portInfo.getDeviceManufacturer();
        this.mPortName = portInfo.getPortName();
        this.mModelName = portInfo.getPortName().substring(PrinterConstants.IF_TYPE_BLUETOOTH.length());
        this.mModelIndex = PrinterModelCapacity.getModel( this.mModelName);
        this.mPortSettings = PrinterModelCapacity.getPortSettings(mModelIndex);
        this.mDrawerOpenStatus = true ;
        if(portInfo.getMacAddress().startsWith("(") && portInfo.getMacAddress().endsWith(")"))
            this.mMacAddress = portInfo.getMacAddress().substring(1, portInfo.getMacAddress().length() - 1);
        else
            this.mMacAddress = portInfo.getMacAddress();




        printerModel = new PrinterModel(this.mModelIndex,
                this.mManufacturer,
                this.mPortName,
                this.mPortSettings,
                this.mMacAddress,
                this.mModelName,
                true,
                PrinterConstants.PAPER_SIZE_FOUR_INCH);


        saveActivePrinterModel(
                mPrinterSettingIndex,
                printerModel
        );



        printerModel = getSavedPrinterModel();


        if (port == null)
            rawConnectToStarPrinters(portInfo);
        else
            delegate.printerConnectionSuccess(portInfo);
    }
**/
    public void searchPrinter() {




        hubtelDeviceInfoList = new ArrayList<>();
        PrinterSeachTask printerSeachTask = new PrinterSeachTask(activity);
//        printerSeachTask.searchEpsonPrinters(mDiscoveryListener,seachDelegate);
        printerSeachTask.searchStarPrinters(seachDelegate);




     }
/**
    private boolean isPrintable(PrinterStatusInfo status) {
        if (status == null) {
            return false;
        }

        if (status.getConnection() == Printer.FALSE) {
            return false;
        }
        else if (status.getOnline() == Printer.FALSE) {
            return false;
        }
        else {

        }

        return true;
    }
    public void printSample(){

        ReceiptObject _object = new ReceiptObject();


        List<ReceiptOrderItem> items = new ArrayList<>();
        items.add(new ReceiptOrderItem("21","Yam Balls banana ","GHS 12.00"));
        items.add(new ReceiptOrderItem("3","Yam Balls pizza ","GHS 300.00"));
        items.add(new ReceiptOrderItem("331","Rabbit ","GHS 300.00"));
        items.add(new ReceiptOrderItem("21","The men of the league ","GHS 300.00"));
        items.add(new ReceiptOrderItem("10","Yam Balls ","GHS 300.00"));
        items.add(new ReceiptOrderItem("1","Yam Balls banana ","GHS 300.00"));
        items.add(new ReceiptOrderItem("1","Yam Balls pizza ","GHS 300.00"));
        items.add(new ReceiptOrderItem("1","Rabbit ","GHS 300.00"));
        items.add(new ReceiptOrderItem("1","The men of the league ","GHS 300.00"));
        items.add(new ReceiptOrderItem("1","Yam Balls ","GHS 300.00"));
        items.add(new ReceiptOrderItem("1","Yam Balls banana ","GHS 1,300.00"));
        items.add(new ReceiptOrderItem("1","Yam Balls pizza ","GHS 1,30,000.00"));
        items.add(new ReceiptOrderItem("1","Rabbit ","GHS 30,000.00"));
        items.add(new ReceiptOrderItem("1","The men of the league ","GHS 300.00"));
        items.add(new ReceiptOrderItem("1","Yam Balls ","GHS 300.00"));
        items.add(new ReceiptOrderItem("1","Yam Balls banana ","GHS 300.00"));
        items.add(new ReceiptOrderItem("1","Yam Balls pizza ","GHS 300.00"));
        items.add(new ReceiptOrderItem("1","Rabbit ","GHS 300.00"));
        items.add(new ReceiptOrderItem("1","The men of the league ","GHS 300.00"));
        items.add(new ReceiptOrderItem("1","Yam Balls ","GHS 300.00"));






        _object.setBusinessName("Hubtel Limited");
        _object.setBusinessBranch("Main");
        _object.setBusinessPhone( "0540256631");
        _object.setBusinessAddress("Kokomlemle/Accra");
        _object.setBusinessWebUrl( "www.hubtel.com");
        _object.setPaymentDate("December 5, 2018, 2:20 am");
        _object.setPaymentReceiptNumber( "099121-1212-9821");
        _object.setPaymentType("Cash");
        _object.setItems(items);
        _object.setSubtotal("GHS 1,090.00");
        _object.setDiscount("GHS 0.00");
        _object.setTax("GHS 0.00");
        _object.setGratisPoint("0.0 pts");
        _object.setAmountPaid("GHS 1,000.00");
        _object.setSubtotal("GHS 1,090.00");
        _object.setChange("GHS 90.00");
        _object.setTotal("GHS 1,090.00");
        _object.setEmployeeName("Apostle Boafo");
        _object.setCustomer("0540256631");
        _object.setDuplicate(false);

        CardDetails cardDetails = new CardDetails();
        cardDetails.setAuthorization("98989");
        cardDetails.setMid("191910022");
        cardDetails.setCard("98911****89");
        cardDetails.setSchema("Visa");
        cardDetails.setTransID("32HDD333D999D");
        cardDetails.setTid("ZHUB232");


        _object.setCardDetails(cardDetails);




printOrderPayment(_object);

    }

    private boolean createReceiptData(ReceiptObject object) {


        final int pageAreaHeight = 500;
        final int pageAreaWidth = 500;

        if (mPrinter == null) {
            return false;
        }

        try {


/*

            mPrinter.addPageBegin();
            mPrinter.addPageArea(0, 0, bitmap.getWidth(), bitmap.getHeight() + 50);
            mPrinter.addPagePosition(0, bitmap.getHeight());




            mPrinter.addImage(
                    bitmap, 0, 0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    Printer.PARAM_DEFAULT,
                    Printer.PARAM_DEFAULT,
                    Printer.PARAM_DEFAULT,
                    Printer.PARAM_DEFAULT,
                    Printer.PARAM_DEFAULT);



            mPrinter.addPageEnd();

            mPrinter.addCut(Printer.CUT_FEED);
        }
        catch (Exception e) {

            delegate.printingFailed(e.getLocalizedMessage());
            return false;
        }



        return true;
    }
    private boolean createReceiptData(Bitmap bitmap) {


        if (mPrinter == null) {
            return false;
        }

        try {



            mPrinter.addPageBegin();
            mPrinter.addPageArea(0, 0, bitmap.getWidth(), bitmap.getHeight() + 50);
            mPrinter.addPagePosition(0, bitmap.getHeight());




         mPrinter.addImage(
                    bitmap, 0, 0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    Printer.PARAM_DEFAULT,
                    Printer.PARAM_DEFAULT,
                    Printer.PARAM_DEFAULT,
                    Printer.PARAM_DEFAULT,
                    Printer.PARAM_DEFAULT);


            mPrinter.addPageEnd();

            mPrinter.addCut(Printer.CUT_FEED);
        }
        catch (Exception e) {

            delegate.printingFailed(e.getLocalizedMessage());
            return false;
        }



        return true;
    }

    private boolean printData(HubtelDeviceInfo deviceInfo) {



        delegate.printingBegan(deviceInfo);

        if (mPrinter == null) {

            delegate.printingFailed("Failed to print to  epson printer ");
            return false;
        }

      /*  if (!connectPrinter(deviceInfo)) {
            return false;
        }

        PrinterStatusInfo status = mPrinter.getStatus();

        dispPrinterWarnings(status);

        if (!isPrintable(status)) {
            delegate.printingCompletedResult(status + "sendData");
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
            return false;
        }

        try {

            delegate.printingBegan(deviceInfo);
            mPrinter.sendData(Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {

            delegate.printingCompletedResult(e + "sendData");

            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
            return false;
        }

        return true;
    }
    private void dispPrinterWarnings(PrinterStatusInfo status) {

        String warningsMsg = "";

        if (status == null) {
            return;
        }

        if (status.getPaper() == Printer.PAPER_NEAR_END) {
            warningsMsg += activity.getString(R.string.handlingmsg_warn_receipt_near_end);
        }

        if (status.getBatteryLevel() == Printer.BATTERY_LEVEL_1) {
            warningsMsg += activity.getString(R.string.handlingmsg_warn_battery_near_end);
        }



        delegate.printingCompletedResult(warningsMsg);
    }
**/
    public void openCashDrawer(){

            ICommandBuilder.PeripheralChannel channel = ICommandBuilder.PeripheralChannel.No1;
            StarIoExt.Emulation emulation = PrinterModelCapacity.getEmulation(getSavedPrinterModel().getModelIndex());


            byte[] data = openCashDrawerCommand(emulation, channel);



            Communication.sendCommands(this, data, getSavedPrinterModel().getPortName(), getSavedPrinterModel().getPortSettings(), 10000, activity, new Communication.SendCallback() {
                 @Override
                 public void onStatus(boolean result, Communication.Result communicateResult) {


                     Log.d("debug",communicateResult.toString());




                     if(printingTaskDelegate!=null)
                     printingTaskDelegate.cashDrawertatusReport(communicateResult);
                 }
             });
        }

    private void rawConnectToStarPrinters(final HubtelDeviceInfo hubtelDeviceInfo) {
        AsyncTask<Void, Void, StarIOPort> task = new AsyncTask<Void, Void, StarIOPort>() {





            @Override
            protected void onPreExecute() {
                mTryConnect = true;
               // delegate.printerConnectionBegan();
            }

            @Override
            protected StarIOPort doInBackground(Void... voids) {



                if(port != null){

                    return port;

                }else{
                    try {
                        synchronized (activity) {

                            port = StarIOPort.getPort(printerModel.getPortName(), printerModel.getPortSettings(), 10000,activity);
                        }
                    } catch (StarIOPortException e) {
                      //  delegate.printerConnectionFailed(e.getLocalizedMessage());
                    }
                    return port;

                }





            }

            @Override
            protected void onPostExecute(StarIOPort port) {

                mTryConnect = false;
                //delegate.printerConnectionSuccess(hubtelDeviceInfo);

            }
        };

        if (!mTryConnect) {
            task.execute();
        }
    }

    public void disconnect() {
         port = null;
    }

    public static byte[] SampleReceipt(StarIoExt.Emulation emulation) {

        ICommandBuilder builder = StarIoExt.createCommandBuilder(emulation);
        Log.d("got inside", "createTestReceiptData");
        builder.beginDocument();
        //  Typeface typeface = Typeface.defaultFromStyle();

        ReceiptCreator receiptCreator = new ReceiptCreator(570);
        receiptCreator.setMargin(2, 2).
                setAlign(Paint.Align.CENTER).
                setColor(Color.BLACK).
                setTextSize(25).

                 addTextCenter("HUBTEL LIMITED")
                .addText("TESTING SOMETHING \n Pringing ")
                .addBlankSpace(12)
                .setTextSize(20)
                .setAlign(Paint.Align.CENTER)
                .addText("A Hubtel Technology")
                .addLine();


        builder.appendBitmap(receiptCreator.build(), true);


        builder.appendCutPaper(ICommandBuilder.CutPaperAction.PartialCutWithFeed);

        builder.endDocument();

        return builder.getCommands();
    }

    public void printMainJob(Bitmap data){


        ICommandBuilder builder = StarIoExt.createCommandBuilder(PrinterModelCapacity.getEmulation(getSavedPrinterModel().getModelIndex()));
        builder.beginDocument();
        builder.appendBitmap(data, true);
        builder.appendCutPaper(ICommandBuilder.CutPaperAction.PartialCutWithFeed);
        builder.endDocument();
        Communication.sendCommands(this, builder.getCommands(), getSavedPrinterModel().getPortName(), getSavedPrinterModel().getPortSettings(), 10000, activity, mCallback);
    }

    public  void printOrderPayment(final ReceiptObject object){


        PrintingTask task = new PrintingTask(activity,getActiveHubtelDevice(),prefs,printingTaskDelegate);
        task.printOrderPayment(getActiveHubtelDevice(),object);




    }


    public  void printEndofDaySales(final ReceiptObject object){


        PrintingTask task = new PrintingTask(activity,getActiveHubtelDevice(),prefs,printingTaskDelegate);
        task.printEndOfDay(getActiveHubtelDevice(),object);




    }





    private static byte[] openCashDrawerCommand(StarIoExt.Emulation emulation, ICommandBuilder.PeripheralChannel channel) {
        ICommandBuilder builder = StarIoExt.createCommandBuilder(emulation);

        builder.beginDocument();

        builder.appendPeripheral(channel);

        builder.endDocument();

        return builder.getCommands();
    }

    private void saveActivePrinterModel(int index, PrinterModel settings) {
        if (printermodelList.size() > 1) {
            printermodelList.remove(index);
        }

        printermodelList.add(index, settings);


        prefs.edit()
                .putString(PREF_KEY_PRINTER_SETTINGS_JSON, JsonUtils.createJsonStringOfPrinterSettingList(printermodelList))
                .apply();
    }


    private void setActiveHubtelDevice(HubtelDeviceInfo device){


        Log.d("debug",JsonUtils.createJsonStringOfActiveHubtelDevices(device));

    prefs.edit()
            .putString(PREF_KEY_ACTIVE_HUBTEL_DEVICE, JsonUtils.createJsonStringOfActiveHubtelDevices(device))
            .apply();
      }

    public HubtelDeviceInfo getActiveHubtelDevice(){


Log.d("Debug",JsonUtils.createJsonStringOfActiveHubtelDevices(prefs.getString(PREF_KEY_ACTIVE_HUBTEL_DEVICE, "")).toString());

        return JsonUtils.createJsonStringOfActiveHubtelDevices(prefs.getString(PREF_KEY_ACTIVE_HUBTEL_DEVICE, ""));

    }

    public PrinterModel getSavedPrinterModel( ) {
        if (printermodelList.isEmpty()) {
            return null;
        }

        return printermodelList.get(0);
    }

    public PrinterModel getSavedPrinterModel(int index) {
        if (printermodelList.isEmpty() || (printermodelList.size() - 1) < index) {
            return null;
        }

        return printermodelList.get(index);
    }

    public List<PrinterModel> getPrinterSettingsList() {
        return printermodelList;
    }

    public void storeAllReceiptSettings(int allReceiptSettings) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

        prefs.edit()
                .putInt(PREF_KEY_ALLRECEIPTS_SETTINGS, allReceiptSettings)
                .apply();
    }

    public int getAllReceiptSetting() {
        return mAllReceiptSettings;
    }





    final Communication.SendCallback mCallback = new Communication.SendCallback() {
        @Override
        public void onStatus(boolean result, Communication.Result communicateResult) {


          //  if(delegate!=null)
          //  delegate.printingCompletedResult(communicateResult);



        }
    };


    final DiscoveryListener mDiscoveryListener = new DiscoveryListener() {
        @Override
        public void onDiscovery(final DeviceInfo deviceInfo) {


            HubtelDeviceInfo hubtelDeviceInfo = new HubtelDeviceInfo();



            hubtelDeviceInfo.setDeviceName(deviceInfo.getDeviceName());
            hubtelDeviceInfo.setBdAddress(deviceInfo.getBdAddress());
            hubtelDeviceInfo.setDeviceType(deviceInfo.getDeviceType());
            hubtelDeviceInfo.setIpAddress(deviceInfo.getIpAddress());
            hubtelDeviceInfo.setMacAddress(deviceInfo.getMacAddress());
            hubtelDeviceInfo.setTarget(deviceInfo.getTarget());
            hubtelDeviceInfo.setBdAddress(deviceInfo.getBdAddress());
            hubtelDeviceInfo.setDeviceManufacturer("Epson");
            hubtelDeviceInfo.setPortName(deviceInfo.getTarget());




            if( HubtelDeviceHelper.hubtelDeviceInfoList.contains(hubtelDeviceInfo)){

            }else{
                HubtelDeviceHelper.hubtelDeviceInfoList.add(hubtelDeviceInfo);

                if(seachDelegate!=null)
                seachDelegate.printerSearchCompleted( HubtelDeviceHelper.hubtelDeviceInfoList);
            }





        }
    };

    final ConnectionListener epSonconnectionListener = new ConnectionListener() {
        @Override
        public void onConnection(Object o, int i) {
        Log.d("Debug epson ","device was connected : "+o.toString());
        }
    };

    final ReceiveListener epSonreceiveListener = new ReceiveListener() {
        @Override
        public void onPtrReceive(Printer printer, int i, PrinterStatusInfo printerStatusInfo, String s) {

            printingTaskDelegate.printingTaskCompleted(getActiveHubtelDevice(),"Epson Print Success");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    disconnectPrinter();
                }
            }).start();

            Log.d("Debug epson",s +"was connected "+printer.getAdmin());
        }
    };

}


