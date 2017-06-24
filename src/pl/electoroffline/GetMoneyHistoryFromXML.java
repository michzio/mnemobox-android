/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * @author Michał Ziobro
 */
public class GetMoneyHistoryFromXML {
    private static final String ns = null;
    //private static String historyUrl = "http://www.mnemobox.com/webservices/userMoneyHistory.xml.php?from=pl&to=en&";
    public ArrayList<MoneyHistoryObject> moneyHistoriesObjects;

    GetMoneyHistoryFromXML(InputStream xmlStream) { 
       
      try { 
       
       XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
       XmlPullParser parser = factory.newPullParser();
       parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
       
       InputStream is = xmlStream;
       parser.setInput(is, null);
       parser.nextTag();
       
       moneyHistoriesObjects = new ArrayList<MoneyHistoryObject>();

       
       parser.require(XmlPullParser.START_TAG, ns, "moneyhistory");
       while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the lektura tag
            if (name.equals("transaction")) {
              
                readHistoryElement(parser);
                
            } else {
                skip(parser);
            }
       }
       //txtview.setText(strLekturaTitle);
            
      } catch (Exception e) 
       {
           //insert something when error
       } finally {
       }
    
    }
    
  
    private void readHistoryElement(XmlPullParser parser) throws XmlPullParserException, IOException {
         
        MoneyHistoryObject moneyHistory = new MoneyHistoryObject(); 
        moneyHistory.mhid =  Integer.parseInt(parser.getAttributeValue(null, "mhid"));
        moneyHistory.typeid = Integer.parseInt(parser.getAttributeValue(null, "typeid"));
        
         while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            
           if (name.equals("mhDate")) {
               moneyHistory.moneyHistoryDate = readmhDate(parser);       
            } else if (name.equals("moneyTransfer")) { 
               moneyHistory.moneyTransfer = readMoneyTransfer(parser);  
            } else if (name.equals("operationDesc")) { 
               moneyHistory.description = readOperationDesc(parser); 
            } else if (name.equals("transactionType")) { 
               moneyHistory.transactionType = readTransactionType(parser); 
            } else if (name.equals("wordsetTitlePl")) { 
               moneyHistory.wordsetTitlePL = readWordsetTitlePl(parser); 
             } else if (name.equals("wordsetTitleEn")) { 
               moneyHistory.wordsetTitleEN = readWordsetTitleEn(parser); 
             }   else { 
                skip(parser);
            }
         }
         moneyHistoriesObjects.add(moneyHistory);
                 
     }
   
     private String readmhDate(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "mhDate");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "mhDate");
        return title;
    }
     private String readMoneyTransfer(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "moneyTransfer");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "moneyTransfer");
        return title;
    }
     private String readOperationDesc(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "operationDesc");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "operationDesc");
        return title;
    }
      private String readTransactionType(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "transactionType");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "transactionType");
        return title;
    }
     private String readWordsetTitlePl(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "wordsetTitlePl");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "wordsetTitlePl");
        return title;
    }
       private String readWordsetTitleEn(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "wordsetTitleEn");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "wordsetTitleEn");
        return title;
    }
    
      // For the tags title and summary, extracts their text values.
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }
// Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                    depth--;
                    break;
            case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
    
    public class MoneyHistoryObject { 
        public String moneyHistoryDate; 
        public int mhid;
        public int typeid; 
        public String moneyTransfer;
        public String description;
        public String transactionType; 
        public String wordsetTitleEN; 
        public String wordsetTitlePL; 
    }
    
      public static GetMoneyHistoryFromXML getHistoryObjectReader(Context ctx) {  
        
    	String nativeCode = Preferences.getAccountPreferences(ctx)
    			.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, ctx.getString(R.string.native_code_lower)); 
    	String foreignCode = Preferences.getAccountPreferences(ctx)
    			.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, ctx.getString(R.string.foreign_code_lower)); 
    	String email = Preferences.getString(ctx, Preferences.KEY_EMAIL, ""); 
    	String pass = Preferences.getString(ctx, Preferences.KEY_SHA1_PASSWORD, ""); 
        String url = ctx.getString(R.string.money_history_url, nativeCode, foreignCode, foreignCode, email, pass); 
        Log.d(GetMoneyHistoryFromXML.class.getName(), "Get money history URL: " + url); 
 
        try  { 
            InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
            GetMoneyHistoryFromXML moneyHistoryObjReader = new GetMoneyHistoryFromXML(is);
           
            try { 
            is.close();
            } catch(java.io.IOException e) { } 
             return moneyHistoryObjReader; 
        } catch (Exception e) { }
        
        return null; 
        
    }
    
}
