package pl.electoroffline;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import pl.elector.service.LearningStatisticsItem;

/**
 * @author Michal Ziobro, 07.04.2015
 */

public class GetLearningStatisticsFromXML implements Iterable<LearningStatisticsItem> {
	
	private static final String ns = null; 
	
	// learning statistics items read from XML web service
	public List<LearningStatisticsItem> learningStatisticsItems; 
	
	public int size() { 
		return learningStatisticsItems.size(); 
	}
	
	public LearningStatisticsItem item(int pos) { 
		return learningStatisticsItems.get(pos); 
	}
	
	@Override
	public Iterator<LearningStatisticsItem> iterator() {
		return learningStatisticsItems.iterator();
	}
	
	/**
	 * Factory method used to create learning statistics items reader based on 
	 * data received from XML web service.
	 */
	public static GetLearningStatisticsFromXML getLearningStatisticsReader(String url) { 
		
		try { 
			InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url); 
			GetLearningStatisticsFromXML learningStatisticsReader = new GetLearningStatisticsFromXML(is);
			try { 
				is.close();
			} catch(java.io.IOException e) { }
			return learningStatisticsReader; 
		} catch(Exception e) { }
		
		return null; 
	}
	
	/**
	 * Constructor of learning statistics items reader from XML input stream.
	 */
	public GetLearningStatisticsFromXML(InputStream xmlStream) { 
		
		try { 
			
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance(); 
			XmlPullParser parser = factory.newPullParser(); 
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false); 
			
			InputStream is = xmlStream; 
			parser.setInput(is, null); 
			parser.nextTag(); 
			
			learningStatisticsItems = new ArrayList<LearningStatisticsItem>();
			
			parser.require(XmlPullParser.START_TAG, ns, "learningStats"); 
			while(parser.next() != XmlPullParser.END_TAG) { 
				if(parser.getEventType() != XmlPullParser.START_TAG) { 
					continue; 
				}
				String name = parser.getName(); 
				
				if(name.equals("learningStatsItem")) { 
					readLearningStatsItemElement(parser); 
				} else { 
					skip(parser); 
				}
			}
			
		} catch(Exception e) { 
			e.printStackTrace(); 
		} finally { } 
	}

	/**
	 * Helper method used to parse each XML learningStatsItem element
	 */
	private void readLearningStatsItemElement(XmlPullParser parser) throws XmlPullParserException, IOException {
		
		LearningStatisticsItem learningStatsItem = new LearningStatisticsItem();
		
		learningStatsItem.setProfileId(Integer.valueOf(parser.getAttributeValue(null, "userId")));
		
		while(parser.next() != XmlPullParser.END_TAG) { 
			if(parser.getEventType() != XmlPullParser.START_TAG) { 
				continue; 
			}
			
			String name = parser.getName(); 
			
			// reading each child element with info about learningStatsItem
			if(name.equals("accessDate")) { 
				learningStatsItem.setAccessDate(readAccessDate(parser)); 
			} else if(name.equals("badAnswers")) { 
				learningStatsItem.setBadAnswers(readBadAnswers(parser)); 
			} else if(name.equals("goodAnswers")) { 
				learningStatsItem.setGoodAnswers(readGoodAnswers(parser)); 
			} else { 
				skip(parser); 
			}
		}
		
		learningStatisticsItems.add(learningStatsItem);
	}

	private String readAccessDate(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, "accessDate"); 
		String accessDate = readText(parser); 
		parser.require(XmlPullParser.END_TAG, ns, "accessDate");
		return accessDate;
	}

	private int readBadAnswers(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, "badAnswers"); 
		String badAnswers = readText(parser); 
		parser.require(XmlPullParser.END_TAG, ns, "badAnswers");
		return Integer.valueOf(badAnswers);
	}

	private int readGoodAnswers(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, "goodAnswers");
		String goodAnswers = readText(parser); 
		parser.require(XmlPullParser.END_TAG, ns, "goodAnswers");
		return Integer.valueOf(goodAnswers);
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
}
