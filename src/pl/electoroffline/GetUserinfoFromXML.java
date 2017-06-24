/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;

/**
 *
 * @author Michał Ziobro
 */
public class GetUserinfoFromXML {
    private static final String ns = null;
    public String email; 
    public String userImage; 
    public String firstName; 
    public String lastName; 
    public String userAge; 
    public String gaduGadu; 
    public String skype; 
    public String phone; 
    public String city; 
    public boolean paidupAccount; 
    public String userLevel; 
    public String userMoney; 
    public String lastWordset;
    public String lastWordsetId; 
    
    GetUserinfoFromXML(InputStream xmlStream) { 
       
      try { 
       
       XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
       XmlPullParser parser = factory.newPullParser();
       parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
       
       InputStream is = xmlStream;
       parser.setInput(is, null);
       parser.nextTag();
       
       
       
       parser.require(XmlPullParser.START_TAG, ns, "profile");
       while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the lektura tag
            if (name.equals("email")) {
                
                email = readText(parser);
                
            } else if(name.equals("userImage")) { 
                userImage = readText(parser);
            } else if(name.equals("firstName")) {
                firstName = readText(parser);
            } else if(name.equals("lastName")) {
                lastName = readText(parser);
            } else if(name.equals("userAge")) {
                userAge = readText(parser);
            } else if(name.equals("gaduGadu")) {
                gaduGadu = readText(parser);
            } else if(name.equals("skype")) {
                skype = readText(parser);
            } else if(name.equals("phone")) {
                phone = readText(parser);
            } else if(name.equals("city")) {
                city = readText(parser);
            } else if(name.equals("paidupAccount")) {
                String paid = readText(parser);
                if(paid.equalsIgnoreCase("1")) { 
                    paidupAccount = true;
                } else { 
                    paidupAccount = false; 
                }
            } else if(name.equals("userLevel")) {
                userLevel = readText(parser);
            } else if(name.equals("userMoney")) {
                userMoney = readText(parser);
            } else if(name.equals("lastWordset")) {
                 lastWordsetId =  parser.getAttributeValue(null, "wid");
                 lastWordset = readText(parser);
            }  else { 
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
    
    public static GetUserinfoFromXML getMyProfileInfo(Context ctx) { 
         String url = ctx.getString(R.string.userinfo_url).replaceAll("&amp;", "&")
                 + Preferences.getString(ctx, Preferences.KEY_EMAIL, "") 
                 + "&pass=" + Preferences.getString(ctx, Preferences.KEY_SHA1_PASSWORD, "");
        try  { 
            InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
            GetUserinfoFromXML userinfo = new GetUserinfoFromXML(is);
           
            try { 
            is.close();
            } catch(java.io.IOException e) { } 
             return userinfo; 
        } catch (Exception e) { }
        
        return null; 
        
    }
    
    
}
