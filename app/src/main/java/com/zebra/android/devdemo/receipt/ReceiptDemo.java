/***********************************************
 * CONFIDENTIAL AND PROPRIETARY 
 *
 * The source code and other information contained herein is the confidential and the exclusive property of
 * ZIH Corp. and is subject to the terms and conditions in your end user license agreement.
 * This source code, and any other information contained herein, shall not be copied, reproduced, published, 
 * displayed or distributed, in whole or in part, in any medium, by any means, for any purpose except as
 * expressly permitted under such license agreement.
 *
 * Copyright ZIH Corp. 2012
 *
 * ALL RIGHTS RESERVED
 ***********************************************/

package com.zebra.android.devdemo.receipt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

import com.zebra.android.devdemo.ConnectionScreen;
import com.zebra.android.devdemo.R;
import com.zebra.android.devdemo.util.DemoSleeper;
import com.zebra.android.devdemo.util.SettingsHelper;
import com.zebra.android.devdemo.util.UIHelper;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

public class ReceiptDemo extends ConnectionScreen {

    private UIHelper helper = new UIHelper(this);
    private boolean sendData = true;
    Connection printerConnection = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        testButton.setText("Print Using One Job");

        Button printAsManyJobsButton = new Button(this);
        printAsManyJobsButton.setText("Print Using Multiple Jobs");

        LinearLayout connection_screen_layout = (LinearLayout) findViewById(R.id.connection_screen_layout);
        int index = connection_screen_layout.indexOfChild(testButton);

        connection_screen_layout.addView(printAsManyJobsButton, index + 1);

        printAsManyJobsButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                sendData = false;
                performTestWithManyJobs();
            }
        });

    }

    @Override
    public void performTest() {
        executeTest(false);
    }

    public void performTestWithManyJobs() {
        executeTest(true);
    }

    public void executeTest(final boolean withManyJobs) {
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                connectAndSendLabel(withManyJobs);
                Looper.loop();
                Looper.myLooper().quit();
                sendData = true;
            }
        }).start();

    }

    private void connectAndSendLabel(final boolean withManyJobs) {
        if (isBluetoothSelected() == false) {
            try {
                printerConnection = new TcpConnection(getTcpAddress(), Integer.valueOf(getTcpPortNumber()));
            } catch (NumberFormatException e) {
                helper.showErrorDialogOnGuiThread("Port number is invalid");
                return;
            }
        } else {
            printerConnection = new BluetoothConnection(getMacAddressFieldText());
        }
        try {
//            sendTestLabel();
            helper.showLoadingDialog("Connecting...");
            printerConnection.open();

            ZebraPrinter printer = null;

            if (printerConnection.isConnected()) {
                printer = ZebraPrinterFactory.getInstance(printerConnection);

                if (printer != null) {
                    PrinterLanguage pl = printer.getPrinterControlLanguage();
                    if (pl == PrinterLanguage.CPCL) {
                        helper.showErrorDialogOnGuiThread("This demo will not work for CPCL printers!");
                    } else {
                        // [self.connectivityViewController setStatus:@"Building receipt in ZPL..." withColor:[UIColor
                        // cyanColor]];
                        if (withManyJobs) {
                            sendTestLabelWithManyJobs(printerConnection);
                        } else {
                            sendTestLabel();
                        }
                    }
                    printerConnection.close();
                    saveSettings();
                }
            }
        } catch (ConnectionException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } catch (ZebraPrinterLanguageUnknownException e) {
            helper.showErrorDialogOnGuiThread("Could not detect printer language");
        } finally {
            helper.dismissLoadingDialog();
        }
    }

    private void sendTestLabel() {
        try {
            byte[] configLabel = createZplReceipt1().getBytes();
            printerConnection.write(configLabel);
            DemoSleeper.sleep(1500);
            if (printerConnection instanceof BluetoothConnection) {
                DemoSleeper.sleep(500);
            }
        } catch (ConnectionException e) {
        }
    }

    private void sendTestLabelWithManyJobs(Connection printerConnection) {
        try {
            sendZplReceipt(printerConnection);
        } catch (ConnectionException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        }

    }


    private void saveSettings() {
        SettingsHelper.saveBluetoothAddress(ReceiptDemo.this, getMacAddressFieldText());
        SettingsHelper.saveIp(ReceiptDemo.this, getTcpAddress());
        SettingsHelper.savePort(ReceiptDemo.this, getTcpPortNumber());
    }

    private Map<String, String> createListOfItems() {
        String[] names = {"Microwave Oven", "Sneakers (Size 7)", "XL T-Shirt", "Socks (3-pack)", "Blender", "DVD Movie"};
        String[] prices = {"79.99", "69.99", "39.99", "12.99", "34.99", "16.99"};
        Map<String, String> retVal = new HashMap<String, String>();

        for (int ix = 0; ix < names.length; ix++) {
            retVal.put(names[ix], prices[ix]);
        }
        return retVal;
    }


    private String createZplReceipt1() {
//        String str = "^XA" + "\r\n" +
////                "^FO150,30^GFA,8405,8405,41,,:::::::::::X07CgL0EgL0E,U07IFCgI03FFEgI01FFE,T0KFCgI03FFEgI03FFE,T0KFCgI03FFCgI03FFE,T0KF8gI07FFCgI03FFE,S01KF8gI07FFCgI03FFE,S03KF8gI07FFCgI03FFE,S03KF8gI07FFCgI03FFC,S07KF8gI07FFCgI07FFC,S0LF8gI07FFCgI07FFC,S0LF8gI07FF8gI07FFC,R01LFgJ0IF8gI07FFC,:R03LFgJ0IF8gI07FFC,R03LFgJ0IF8gI0IF8,R07LFgJ0IFgJ0IF8,R0MFgJ0IFgJ0IF8,R0MFgI01IFgJ0IF8,Q01MFgI01IFgJ0IF8,Q01IFBIFgI01IFgJ0IF8,Q03IFBIFgI01IFgJ0IF,Q03IF3FFEgI01IFgI01IF,Q07IF3FFEgI01IFgI01IF,Q0IFE3FFEgI01FFEL01F8T01IF,Q0IFE3FFEN0FFEO07FC3FFEL0IF8L01800FC01IF,P01IFC3FFEI0FFE07IFCM07IFBFFEK0JFEJ03FF80FFC01IF,P01IFC3FFE001FFE1JFEL01MFEJ01KF8I03FF83FFC01IF,P03IF83FFE001FFE7KFL0NFEJ07KFCI03FF87FFC01FFE,P07IF07FFE001OF8J01NFCI01LFEI03FF8IFC03FFE,P07IF07FFC001OFCJ07NFCI03MFI07FFBIF803FFE,P0IFE07FFC001OFCJ0OFCI07MF8007LF803FFE,P0IFE07FFC001OFEI01OFCI0NF8007LF803FFE,O01IFC07FFC003OFEI03OFC001NFC007LF803FFE,O01IFC07FFC003OFEI07OFC003NFC007LF803FFE,O03IF807FFC003PFI07OFC007IFC0IFC007LF803FFC,O07IF007FFC003JFE0JFI0JFE03IFC007IF007FFC007LF807FFC,O07IF007FFC003JF007IF001JFI0IF800IFC001FFE00KFCI07FFC,O0IFE00IFC003IFC003IF001IFEI0IF801IF8001FFE00KFJ07FFC,O0IFC00IFC007IF8001IF003IF8I0IF801IFJ0FFE00JFCJ07FFC,N01IFC00IF8007IFI01IF003IFJ0IF801FFEJ0FFE00JF8J07FFC,N01IF800IF8007FFCI01IF007IFJ0IF803FFCJ0FFE00JFK07FFC,N03IF800IF8007FFCI01FFE007FFEJ0IF003FFCJ0FFE00IFEK07FF8,N07IFI0IF8007FFCI01FFE007FFCI01IF007FF8J0FFE00IFCK0IF8,N07IFI0IF8007FFCI03FFE00IFCI01IF007FF8J0FFE01IF8K0IF8,N0IFEI0IF8007FF8I03FFE00IFCI01IF007FF8I01FFC01IFL0IF8,N0IFC001IF800IF8I03FFE00IF8I01IF00PFC01IFL0IF8,M01IFC001IF800IF8I03FFE00IF8I01IF00PFC01IFL0IF8,M03IF8001IF800IF8I03FFC00IF8I01IF00PFC01IFL0IF,M03IF8001IFI0IF8I07FFC01IF8I01FFE00PFC01FFEK01IF,M07IFI01IFI0IF8I07FFC01IFJ03FFE00PFC03FFEK01IF,M07IFI01IFI0IF8I07FFC01IFJ03FFE00PF803FFEK01IF,M0IFEI01IF001IFJ07FFC01IFJ03FFE01PF803FFEK01IF,:L01IFCI01IF001IFJ07FFC01IFJ03FFE01PF003FFEK01IF,L03IF8I01IF001IFJ07FF801IFJ07FFC01FFEO03FFEK01FFE,L03IF8I03IF001IFJ0IF801IFJ07FFC01FFEO03FFEK01FFE,L07IFJ03FFE001IFJ0IF801IFJ07FFC00FFEO03FFCK03FFE01JF8,L07FFEJ03FFE001FFEJ0IF801IF8I0IFC00IFO07FFCK03FFE01JF8,L0IFEJ03FFE003FFEJ0IF801IF8001IFC00IFO07FFCK03FFE01JF8,L0IFCJ03FFE003FFEJ0IF801IF8003IFC00IF8N07FFCK03FFC01JF,K01IFCJ03FFE003FFEJ0IF001IFC00JFC007FF8N07FFCK03FFC01JF,K03IF800KFE003FFEI01IFI0JF07JF8007FFEI07J07FFCK07FFC01JF,K03IF800KFE003FFEI01IFI0PF8007IFC07F8I07FF8K07FFC03JF,K07IF001KFE003FFEI01IFI0PF8003MF8I0IF8K07FFC03JF,K07FFE001KFE003FFEI01IFI07OF8003MFCI0IF8K07FFC03JF,K0IFE003KFE003FFCI01IFI07OF8001MFEI0IF8K07FFC03JF,K0IFC003KFC007FFCI01IFI03OF8001MFEI0IF8K07FFC03IFE,J01IFC003KFC007FFCI01FFEI03KFEIF8I0NFI0IF8K07FF803IFE,J03IF8007KFC007FFCI03FFEI01KFCIFJ07MFI0IF8K07FF807IFE,J03IF8007KFC007FFCI03FFEJ0KF8IFJ01LFCI0IF8K0IF807IFE,J07IFI0LFC007FFCI03FFEJ07JF0IFK0LFJ0IFL0IF807IFE,J07FFEI0LFC007FF8I03FFEJ01IFC0FFEK01JF8I01IFL0IF807IFE,J0IFEgN07FEQ07FFC,I01IFC,:I03IF8,I03IF,I07IF,I07FFE,I0IFE,001IFC,:003IF8,003IF,007IF,007FFE,00IFE,01IFChS03C,01IFCgY02S03C,hH03C0FS03C,T03FFgK07C0FS078,S01IFCgJ07C0FS078,S03IFER07Q07C0FS078,S0FF03FR0FQ01006S078,R01FC01FR0FgN0F8,R03FI07R0EgN0F,R07EI04Q01EgN0F,R0FCU01EgN0F,R0F8U01EgN0F,Q01FP0FEJ03EL03EJ0F8M03FJ01FN07E,Q01FO03FFI03IF80078FEI07FE3CI0F0FFCI01E003EI03FF8,Q03EO0IFC003IF80071FCI0IF3CI0F1FFEI01E007CI07FFC,Q03CN01FCFC003IF80073FC001IFBCI0F7FFEI01E00F8001FC7E,Q07CN03E01EI07CJ0F78I07E03F8I0FF03EI03E01FI03E01E,Q07CN07C01EI07CJ0FEJ07C01F8I0FE01FI03E03EI07C00F,Q078N0F800FI078J0FCJ0F800F8001FC01FI03C07CI07800F,Q078N0FI0FI078J0F8I01FI0F8001F001FI03C0F8I0FI0F,Q0F8M01EI0FI078I01F8I01EI0FI01E001FI03C1FJ0FI07,Q0F001FFE001EI0FI078I01FJ03EI0FI01E001EI03C3EI01EI07,Q0F001FFE003CI0FI0F8I01EJ03CI0FI01E001EI07C7CI01EI07,Q0F001FFC003CI0FI0FJ01EJ07CI0FI03E001EI078F8I03CI07,Q0FJ03C003KFI0FJ01EJ078I0FI03C001EI079F8I03KF,Q0FJ03C003KFI0FJ01EJ078001FI03C001EI07BF8I03KF,Q0FJ03C007JFEI0FJ03EJ0F8001EI03C003CI07BFCI03JFE,Q0FJ03C0078L01EJ03CJ0F8001EI03C003CI0FFBCI07C,Q0FJ0380078L01EJ03CJ0F8001EI07C003CI0FF1EI078,Q0F8I0780078L01EJ03CJ0FI03EI078003CI0FE1EI078,Q0F8I0780078L01EJ03CJ0FI03CI078003CI0FC1EI078,Q078I0780078L01EJ078J0F8003CI0780078I0F80FI03C,Q07CI078003CL03EJ078J0F8007CI0780078001F00FI03C,Q07CI078003CL03EJ078J0F8007CI0780078001F0078003C,Q03EI0FI03EL03EJ078J0F800FCI0FI078001E0078003E,Q03F001FI03E002I03EJ078J07C03FCI0FI078001E0078001F002,Q01FE07FI01F80FI01FJ0F8J07E0FB8I0FI0F8003E003CI0F80F,R0JFEJ0JFI01FF800FK03IF38001FI0FI03E003CI0JF8,R03IF8J07FFEI01FFC00FK01FFC78001FI0FI03C001EI07FFE,S0FFEK01FF8J07F800FL0FF078001EI0FI03C001EI01FF8,T04M02hK04,,::::::::::::::Y01hFE,Y03hGF,:,:::::::::::hK0E,hJ01F,gP03EQ0F3F,:Q0FX03EQ0F1FT07,Q0F8W03EQ0F0CT0F8,Q0F8W03EQ0FV0F8,:007E3C03F00F81E183F8007E03E3E03E03E03F001F8F1F01F8078FC00FE0F8I03F801FC078FC0FC,01FFBC0FFC7FF9F79FFE01FF83EFF83E07E0FFC07IF1F07FF0FBFF07KF800FFE07FF07IF3FF,03IFC1FFE7FF9FF9IF03FFC3IFC3E0FC1FFE0JF1F0IF8JF87KF801IF1IF87IF7FF,07IFC3F3F7FF9FF9IF87E7E3IFE3E1F87F3F0JF1F1F9FCJF8FCJF803IF9IFC7LF8,07E0FC7C0F8F81FE180F8F81F3F87E3E3F07C0F9F83F1F3E07CFF1F9FI0F8007E0FBF0FC7F1FF1F8,0F807C78078F81F8I0FCF00F3F03E3E7E07807BF01F1F3E03EFC07DFI0F8007C067E03E7E0FC0FC,0F803CF807CF81FJ07DF00FBF01E3EFC0F807FE01F1F3C01EFC07DF800F800F8007C03E7C07C07C,0F803CJFCF81F003FFDJFBE01E3FF80KFE00F1F3IFEFC07CFFC0F800F8007C03E7C07C07C,0F803CJFCF81F00IFDJFBE01E3FFC0KFE00F1F7IFEF807C7FF0F800F8007C01E7C07C07C,0F803CJFCF81F01IFDJFBE01E3FFE0KFE00F1F7IFEF807C3FFCF800F8007C01E7C07C07C,0F807CF8I0F81F03E07DFI03E01E3IF0F8003E01F1F3CI0F807C01FCF800F8007C03E7C07C07C,0FC0FCF8I0F81F03C07DFI03E01E3F3F0F8003E01F1F3EI0F807C007CF800FC007C03E7C07C07C,07IFC7C020F81F07C07CF8003E01E3E1F87C001F03F1F3E010F807C003CF81E7C0F3E07E7C07C07C,03IFC7F1E0FC9F03E0FCFE3C3E01E3E0FC7F1E1FEFF1F1FC78F807CF07CFCBF3FBFBF9FC7C07C07C,01IFC3IF07F9F03IFC7FFE3E01E3E07E3IF0JF1F0IFCF807DIFC7IF3IF1IF87C07C07C,00FF3C1IF07FDF01IFC3FFE3E01E3E03E1IF07IF1F07FF8F807DIF87IF1FFE0IF07C07C07C,J03C07FC01FDF00FF7C0FF83E01E3E03F07FC01FCF1F01FE07807C7FE01FDE07FC03FC07C07C07C,J07C00EI06J018I01CS0EI06L03N07I060800EI06,02007C,03C1F8,07IF8,07IF,01FFC,I0C,,::^FS" + "\r\n" +
////                "^FO50,250^GB520,3,3^FS" + "\r\n" +
//                "^CF0,65" + "\r\n" +
//                "^FO120,280^FDLIEFERSCHEIN^FS" + "\r\n" +
//                "^CF0,35" + "\r\n" +
//                "^FO165,350^FDBestellnr.: 16162020^FS" + "\r\n" +
//                "^CFA,23" + "\r\n" +
//                "^FO110,410^FB400,4,2,C^FH\\^FDJohn Doe^FS" + "\r\n" +
//                "^CFA,21" + "\r\n" +
//                "^FO110,445^FB400,4,2,C^FH\\^FD100 Main Street, Springfield TN 39021^FS" + "\r\n" +
//                "^FX Second section with recipient address and permit information." + "\r\n" +
//                "^CFA,20" + "\r\n" +
//                "^FO50,530^FDKundennr.: 121290^FS" + "\r\n" +
//                "^FO50,560^FDLieferdatum: 12-10-2021^FS" + "\r\n" +
//                "^FO50,590^FDZahlungsart: Cash^FS" + "\r\n" +
//                "^FO50,620^FDBestell-Hotline: 089-350810 ^FS" + "\r\n" +
//                "^CF0,22" + "\r\n" +
//                "^FO50,675^FDAnz.^FS" + "\r\n" +
//                "^FO110,675^FDBezeichnung^FS" + "\r\n" +
//                "^FO430,675^FDPreis^FS" + "\r\n" +
//                "^FO500,675^FDGesamt^FS" + "\r\n" +
//                "^FO50,700^GB520,2,1^FS" + "\r\n" +
//                "^CF0,22" + "\r\n" +
//                "^FO50,715^FD4^FS" + "\r\n" +
//                "^FO110,715^FDAugustiners Hell 20 x 0,5l^FS" + "\r\n" +
//                "^FH\\^FO430,715^FD10,99\\15^FS" + "\r\n" +
//                "^FH\\^FO500,715^FD43,96\\15^FS" + "\r\n" +
//                "^FO50,900^GB520,2,1^FS" + "\r\n" +
//                "^CF0,23" + "\r\n" +
//                "^FO340,930^FB225,4,2,R^FH\\^FDGesamtbetrag: 86,55\\15^FS" + "\r\n" +
//                "^CFB,15" + "\r\n" +
//                "^FO340,960^FB225,4,2,R^FH\\^FDRechnung erfolgt separat.^FS" + "\r\n" +
//                "^XZ";
//
//
//        String wholeZplLabel = String.format("%s", str);

        // get the assets manager from your activity or `Context`
//        try {
//            final InputStream in = getAssets().open("anderl_logo_1_.pcx");
//            final ByteArrayOutputStream imageString = readFileToString(in);
//
//            String wholeZplLabel = String.format("%s", imageString.toByteArray());
//            return wholeZplLabel;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        String image = "COMPRESSED-GRAPHIC 8 4 30 30 FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFF0FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0FFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFF000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFF000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0000FFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFF0000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFF00000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFF00000F00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000FFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFF000000F00000FFFFFFFFFFFF0000FFFFFFFFFFFFFF0000F00000FFFFFFFFF00000FFFFFFFFFF0FFFF00FFFF00000FFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFF00000FF00000FFFF00000F00000000FFFFFFFFF0000000000000FFFFFFF00000000FFFFFF0000FF00000FFF00000FFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFF000000FF0000FFFFF00000000000000FFFFFFF00000000000000FFFFFF00000000000FFFFF00000000000FFF0000FFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFF00000FFF0000FFFFF000000000000000FFFFF000000000000000FFFFF0000000000000FFFF0000000000FFFF0000FFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFF00000FFFF0000FFFFF00000000F000000FFFFF0000000FF000000FFFF000000FFF00000FFFF0000000000FFF00000FFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFF000000FFF00000FFFFF000000FFFF00000FFFF000000FFFFF00000FFFF00000FFFFF0000FFFF0000000FFFFFF00000FFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFF00000FFFF00000FFFF00000FFFFFF00000FFFF00000FFFFFF00000FFF00000FFFFFF0000FFFF000000FFFFFFF00000FFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFF000000FFFF00000FFFF00000FFFFFF00000FFF00000FFFFFFF0000FFFF0000FFFFFFF0000FFF00000FFFFFFFFF00000FFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFF000000FFFFF00000FFFF00000FFFFFF00000FFF00000FFFFFFF0000FFF0000000000000000FFF00000FFFFFFFFF00000FFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFF00000FFFFFF00000FFFF00000FFFFFF00000FFF00000FFFFFFF0000FFF0000000000000000FFF00000FFFFFFFFF0000FFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFF000000FFFFFF00000FFFF00000FFFFFF00000FFF00000FFFFFF00000FFF0000000000000000FFF00000FFFFFFFFF0000FFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFF00000FFFFFFF00000FFFF0000FFFFFFF0000FFFF00000FFFFFF00000FFF00000FFFFFFFFFFFFFF00000FFFFFFFF00000FFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFF000000FFFFFF00000FFFFF0000FFFFFF00000FFFF00000FFFFFF00000FFF00000FFFFFFFFFFFFFF0000FFFFFFFFF00000FFF000000FFFFF" + "\r\n" +
                "FFFFFFFFF00000FFFFFFF00000FFFF00000FFFFFF00000FFFF00000FFFFF000000FFF00000FFFFFFFFFFFFFF0000FFFFFFFFF00000FFF000000FFFFF" + "\r\n" +
                "FFFFFFFF000000FFFF00000000FFFF00000FFFFFF00000FFFF000000000000000FFFFF000000FFF00FFFFFF00000FFFFFFFFF00000FFF000000FFFFF" + "\r\n" +
                "FFFFFFFF00000FFFF000000000FFFF00000FFFFFF00000FFFFF00000000000000FFFFF000000000000FFFFF00000FFFFFFFFF00000FFF000000FFFFF" + "\r\n" +
                "FFFFFFF00000FFFFF000000000FFFF00000FFFFFF00000FFFFF00000000000000FFFFFF000000000000FFFF00000FFFFFFFFF0000FFF0000000FFFFF" + "\r\n" +
                "FFFFFF000000FFFF0000000000FFFF00000FFFFFF0000FFFFFFF0000000FF0000FFFFFFF000000000FFFFFF00000FFFFFFFF00000FFF0000000FFFFF" + "\r\n" +
                "FFFFFF00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000FFFFFFFFFFFFFFFFFF0000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFF000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFF00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFF000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFF00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFF00000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0FFF0FFFFFFFFFFFFFFFFFFFF0FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFF000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00FF00FFFFFFFFFFFFFFFFFFFF0FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFF00FFFF00FFFFFFFFFFFFFFFFF00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFF00FFFFFFFFFFFFFFFFFFFFFFFF0FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFF00FFFFFFFFFFFFFFF000FFFFFF000FFFFFFFFF00FFFFFF000FFFFFFFFFFF000FFFFFFF0FFFFFFFFFFFF000FFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFF00FFFFFFFFFFFFF000F00FFFF00000FFFFF0F000FFFFF0000000FFFFF0000000FFFFFF0FFFF0FFFFFF00F00FFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFF0FFFFFFFFFFFFF00FFFF0FFFFF00FFFFFFF00FFFFFFF0FFFF00FFFFFF00FFF00FFFFF00FFF0FFFFFF0FFFF00FFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFF00FFFF0000FFFFF0FFFFF0FFFFF00FFFFFF00FFFFFFF00FFFF00FFFFF00FFFF00FFFFF00FF0FFFFFF00FFFFF0FFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFF00FFFF0000FFFF00FFFFF0FFFFF0FFFFFFF00FFFFFFF0FFFFF00FFFFF00FFFF00FFFFF00F00FFFFFF00FFFF00FFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFF00FFFFFF00FFFF00000000FFFFF0FFFFFFF00FFFFFF00FFFFF00FFFFF00FFFF00FFFFF00000FFFFFF0000000FFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFF00FFFFFF00FFFF0FFFFFFFFFFF00FFFFFFF0FFFFFFF00FFFFF00FFFFF0FFFFF0FFFFFF00FF0FFFFF00FFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFF0FFFFFF0FFFFF00FFFFFFFFFF00FFFFFFF0FFFFFFF00FFFFF0FFFFFF0FFFFF0FFFFF00FFF00FFFFF0FFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFF00FFFFF0FFFFF00FFFFFFFFFF00FFFFFFF0FFFFFFF00FFFF00FFFFF00FFFFF0FFFFF00FFF00FFFFF00FFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFF0000000FFFFFF000000FFFFF0000FFFF00FFFFFFFF0000000FFFFF00FFFF00FFFFF00FFFF0FFFFF000000FFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFF000FFFFFFFFFF00FFFFFFFFF00FFFFFFFFFFFFFFF00FFFFFFFFF0FFFFFFFFFFFF0FFFFFFFFFFFFF000FFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00FFFFFFFFFFFFFFFFF0F00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFF00FFFFFFFFFFFFFFFFFFFFFFFFFFF00FFFFFFFFFFFFFFFFF0FFFFFFFFFFFFFFFFFFFFFFFF0FFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFF00000FF0000F0000F0000F0000FFF0000FF00000FF00FF00FF0000FFF00000F00FF0000F000000FF000000000FFFF0000FF0000FF00000FF000FF" + "\r\n" +
                "FF000000F00FF00F000F0000F0FF00F00FF00F000000F00F00FF00FF00F00F000F00F00F0000000000F00FFF000FFFF000F00000000F00000000000F" + "\r\n" +
                "F00FFFF0F00FF00F00FF00FFFFFF00000FF00000FF00F0000FFF00FF0000FFF00F00F00FF00000FF00000FFFF0FFFFF0FFFFF00FFF0F00FFF0FFF00F" + "\r\n" +
                "F00FFFF0F000000F00FF00FFF000000000000000FF00F00000FF00000000FFF00F00000000000FFF00F00000F0FFFFF0FFFFF00FFF0F00FFF0FFF00F" + "\r\n" +
                "FF000000F00FFFFF00FF00FF00FF00F00FFFFF00FF00F00F00FF00FFFFF0FFF00F00F00FFFF00FFF00FFFF00F00FF0F00FF0F00FF00F00FFF0FFF00F" + "\r\n" +
                "FFF00000FF00000FF00000FFF00000F00000FF00FF00F00FF00FF0000FF000000F00FF0000F00FFF00000000F00000000000FF00000F00FFF0FFF00F" + "\r\n" +
                "FFFFFF00FFFF0FFFFF0FFFFFFF0FFFFFFF0FFFFFFFFFFFFFFFFFFFF0FFFFF0FFFFFFFFFFFFFFFFFFFFFFF0FFFFFFFFFFFF0FFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FF000000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFF000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" + "\r\n" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";

        String wholeZplLabel = "! 0 200 200 700 1\r\n" + "ON-FEED IGNORE\r\n"
                + "LINE 25 50 600 50 2\r\n"
                + "CENTER\r\n"
                + "T 0 3 0 70 LIEFERSCHEIN\r\n"
                + "CENTER\r\n"
                + "T 0 0 50 120 Bestellnr.: ordernumber\r\n"
                + "CENTER\r\n"
                + "T 0 0 50 160 Customer’s name\r\n"
                + "CENTER\r\n"
                + "T 0 0 50 190 Customer’s address\r\n"
                + "LEFT\r\n"
                + "T 0 0 50 220 Kundennr.: Customernumber\r\n"
                + "LEFT\r\n"
                + "T 0 0 50 250 Lieferdatum: Today's date\r\n"
                + "LEFT\r\n"
                + "T 0 0 50 280 Zahlungsart: payment method\r\n"
                + "LEFT\r\n"
                + "T 0 0 50 310 Bestell-Hotline: 089 - 350 81 01\r\n"
                + "TEXT 0 0 50 400 Anz.\r\n"
                + "TEXT 0 0 70 400 Bezeichnung\r\n"
                + "TEXT 0 0 110 400 Preis\r\n"
                + "TEXT 0 0 120 400 Gesamt\r\n"
                + "LINE 25 450 600 190 1\r\n"
                + "TEXT 0 0 50 500 4\r\n"
                + "TEXT 0 0 70 500 Hell 20 x 0,5l\r\n"
                + "TEXT 0 0 110 500 10,99€\r\n"
                + "TEXT 0 0 120 500 43,96€\r\n"
                + "PRINT\r\n";

        return String.format("%s", wholeZplLabel);

    }


    public ByteArrayOutputStream readFileToString(InputStream is) {
        InputStreamReader isr = null;
        ByteArrayOutputStream bos = null;
        try {
            isr = new InputStreamReader(is);
            bos = new ByteArrayOutputStream();

            byte[] buffer = new byte[2048];
            int n = 0;
            while (-1 != (n = is.read(buffer))) {
                bos.write(buffer, 0, n);
            }

            return bos;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private String createZplReceipt() {
        /*
         This routine is provided to you as an example of how to create a variable length label with user specified data.
         The basic flow of the example is as follows

            Header of the label with some variable data
            Body of the label
                Loops thru user content and creates small line items of printed material
            Footer of the label

         As you can see, there are some variables that the user provides in the header, body and footer, and this routine uses that to build up a proper ZPL string for printing.
         Using this same concept, you can create one label for your receipt header, one for the body and one for the footer. The body receipt will be duplicated as many items as there are in your variable data

         */

        String tmpHeader =
        /*
         Some basics of ZPL. Find more information here : http://www.zebra.com/content/dam/zebra/manuals/en-us/printer/zplii-pm-vol2-en.pdf

         ^XA indicates the beginning of a label
         ^PW sets the width of the label (in dots)
         ^MNN sets the printer in continuous mode (variable length receipts only make sense with variably sized labels)
         ^LL sets the length of the label (we calculate this value at the end of the routine)
         ^LH sets the reference axis for printing.
            You will notice we change this positioning of the 'Y' axis (length) as we build up the label. Once the positioning is changed, all new fields drawn on the label are rendered as if '0' is the new home position
         ^FO sets the origin of the field relative to Label Home ^LH
         ^A sets font information
         ^FD is a field description
         ^GB is graphic boxes (or lines)
         ^B sets barcode information
         ^XZ indicates the end of a label
         */

                "^XA" +

                        "^PON^PW400^MNN^LL%d^LH0,0" + "\r\n" +

                        "^FO50,50" + "\r\n" + "^A0,N,70,70" + "\r\n" + "^FD Shipping^FS" + "\r\n" +

                        "^FO50,130" + "\r\n" + "^A0,N,35,35" + "\r\n" + "^FDPurchase Confirmation^FS" + "\r\n" +

                        "^FO50,180" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDCustomer:^FS" + "\r\n" +

                        "^FO225,180" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDAcme Industries^FS" + "\r\n" +

                        "^FO50,220" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDDelivery Date:^FS" + "\r\n" +

                        "^FO225,220" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FD%s^FS" + "\r\n" +

                        "^FO50,273" + "\r\n" + "^A0,N,30,30" + "\r\n" + "^FDItem^FS" + "\r\n" +

                        "^FO280,273" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDPrice^FS" + "\r\n" +

                        "^FO50,300" + "\r\n" + "^GB350,5,5,B,0^FS";

        int headerHeight = 325;
        String body = String.format("^LH0,%d", headerHeight);

        int heightOfOneLine = 40;

        float totalPrice = 0;

        Map<String, String> itemsToPrint = createListOfItems();

        int i = 0;
        for (String productName : itemsToPrint.keySet()) {
            String price = itemsToPrint.get(productName);

            String lineItem = "^FO50,%d" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD%s^FS" + "\r\n" + "^FO280,%d" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD$%s^FS";
            totalPrice += Float.parseFloat(price);
            int totalHeight = i++ * heightOfOneLine;
            body += String.format(lineItem, totalHeight, productName, totalHeight, price);

        }

        long totalBodyHeight = (itemsToPrint.size() + 1) * heightOfOneLine;

        long footerStartPosition = headerHeight + totalBodyHeight;

        String footer = String.format("^LH0,%d" + "\r\n" +

                "^FO50,1" + "\r\n" + "^GB350,5,5,B,0^FS" + "\r\n" +

                "^FO50,15" + "\r\n" + "^A0,N,40,40" + "\r\n" + "^FDTotal^FS" + "\r\n" +

                "^FO175,15" + "\r\n" + "^A0,N,40,40" + "\r\n" + "^FD$%.2f^FS" + "\r\n" +

                "^FO50,130" + "\r\n" + "^A0,N,45,45" + "\r\n" + "^FDPlease Sign Below^FS" + "\r\n" +

                "^FO50,190" + "\r\n" + "^GB350,200,2,B^FS" + "\r\n" +

                "^FO50,400" + "\r\n" + "^GB350,5,5,B,0^FS" + "\r\n" +

                "^FO50,420" + "\r\n" + "^A0,N,30,30" + "\r\n" + "^FDThanks for choosing us!^FS" + "\r\n" +

                "^FO50,470" + "\r\n" + "^B3N,N,45,Y,N" + "\r\n" + "^FD0123456^FS" + "\r\n" + "^XZ", footerStartPosition, totalPrice);

        long footerHeight = 600;
        long labelLength = headerHeight + totalBodyHeight + footerHeight;

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String dateString = sdf.format(date);

        String header = String.format(tmpHeader, labelLength, dateString);

        String wholeZplLabel = String.format("%s%s%s", header, body, footer);

        return wholeZplLabel;
    }

    private void sendZplReceipt(Connection printerConnection) throws ConnectionException {
        /*
         This routine is provided to you as an example of how to create a variable length label with user specified data.
         The basic flow of the example is as follows

            Header of the label with some variable data
            Body of the label
                Loops thru user content and creates small line items of printed material
            Footer of the label
         
         As you can see, there are some variables that the user provides in the header, body and footer, and this routine uses that to build up a proper ZPL string for printing.
         Using this same concept, you can create one label for your receipt header, one for the body and one for the footer. The body receipt will be duplicated as many items as there are in your variable data
         
         */

        String tmpHeader =
        /*
         Some basics of ZPL. Find more information here : http://www.zebra.com
                  
         ^XA indicates the beginning of a label
         ^PW sets the width of the label (in dots)
         ^MNN sets the printer in continuous mode (variable length receipts only make sense with variably sized labels)
         ^LL sets the length of the label (we calculate this value at the end of the routine)
         ^LH sets the reference axis for printing. 
            You will notice we change this positioning of the 'Y' axis (length) as we build up the label. Once the positioning is changed, all new fields drawn on the label are rendered as if '0' is the new home position
         ^FO sets the origin of the field relative to Label Home ^LH
         ^A sets font information 
         ^FD is a field description
         ^GB is graphic boxes (or lines)
         ^B sets barcode information
         ^XZ indicates the end of a label
         */

                "^XA" +

                        "^POI^PW400^MNN^LL325^LH0,0" + "\r\n" +

                        "^FO50,50" + "\r\n" + "^A0,N,70,70" + "\r\n" + "^FD Shipping^FS" + "\r\n" +

                        "^FO50,130" + "\r\n" + "^A0,N,35,35" + "\r\n" + "^FDPurchase Confirmation^FS" + "\r\n" +

                        "^FO50,180" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDCustomer:^FS" + "\r\n" +

                        "^FO225,180" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDAcme Industries^FS" + "\r\n" +

                        "^FO50,220" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDDelivery Date:^FS" + "\r\n" +

                        "^FO225,220" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FD%s^FS" + "\r\n" +

                        "^FO50,273" + "\r\n" + "^A0,N,30,30" + "\r\n" + "^FDItem^FS" + "\r\n" +

                        "^FO280,273" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDPrice^FS" + "\r\n" +

                        "^FO50,300" + "\r\n" + "^GB350,5,5,B,0^FS" + "^XZ";

        int headerHeight = 325;

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String dateString = sdf.format(date);

        String header = String.format(tmpHeader, dateString);

        printerConnection.write(header.getBytes());

        int heightOfOneLine = 40;

        float totalPrice = 0;

        Map<String, String> itemsToPrint = createListOfItems();

        int i = 0;
        for (String productName : itemsToPrint.keySet()) {
            String price = itemsToPrint.get(productName);

            String lineItem = "^XA^POI^LL40" + "^FO50,10" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD%s^FS" + "\r\n" + "^FO280,10" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD$%s^FS" + "^XZ";
            totalPrice += Float.parseFloat(price);
            String oneLineLabel = String.format(lineItem, productName, price);

            printerConnection.write(oneLineLabel.getBytes());

        }

        long totalBodyHeight = (itemsToPrint.size() + 1) * heightOfOneLine;

        long footerStartPosition = headerHeight + totalBodyHeight;

        String footer = String.format("^XA^POI^LL600" + "\r\n" +

                "^FO50,1" + "\r\n" + "^GB350,5,5,B,0^FS" + "\r\n" +

                "^FO50,15" + "\r\n" + "^A0,N,40,40" + "\r\n" + "^FDTotal^FS" + "\r\n" +

                "^FO175,15" + "\r\n" + "^A0,N,40,40" + "\r\n" + "^FD$%.2f^FS" + "\r\n" +

                "^FO50,130" + "\r\n" + "^A0,N,45,45" + "\r\n" + "^FDPlease Sign Below^FS" + "\r\n" +

                "^FO50,190" + "\r\n" + "^GB350,200,2,B^FS" + "\r\n" +

                "^FO50,400" + "\r\n" + "^GB350,5,5,B,0^FS" + "\r\n" +

                "^FO50,420" + "\r\n" + "^A0,N,30,30" + "\r\n" + "^FDThanks for choosing us!^FS" + "\r\n" +

                "^FO50,470" + "\r\n" + "^B3N,N,45,Y,N" + "\r\n" + "^FD0123456^FS" + "\r\n" + "^XZ", totalPrice);

        printerConnection.write(footer.getBytes());

    }
}
