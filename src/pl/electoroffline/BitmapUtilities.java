/**
 * @date 16.10.2014
 */
package pl.electoroffline;

import pl.elector.service.WordsLoaderService;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;


/**
 * @author Michał Ziobro
 *
 */
public class BitmapUtilities {

	public static Bitmap fitImageView(Bitmap b, ImageView imageView) 
    {
    	if(b == null) return b; 
    	int bwidth= b.getWidth();
    	int bheight= b.getHeight();
    	int swidth= imageView.getWidth();
    	int sheight= imageView.getHeight();
    	
    	Log.w(BitmapUtilities.class.getName(), "Bitmap size: " + bwidth + "x" + bheight + ", ImageView size: " + swidth + "x" + sheight);
    	int newWidth = 0; 
    	int newHeight = 0; 
    	if(swidth > 0) { 
	    	newWidth=swidth;
	    	newHeight = (int) Math.floor((double) bheight *( (double) newWidth / (double) bwidth));
    	} else if(sheight > 0) { 
    		newHeight = sheight;
    		newWidth = (int) Math.floor((double) bwidth *( (double) newHeight / (double) bheight));
    	} else { 
    		return b; 
    	}
    	Bitmap newbitMap = Bitmap.createScaledBitmap(b,newWidth,newHeight, true);
    	return newbitMap; 
    }
	
	public static Bitmap scale(double scale, Bitmap b)
	{
		int bwidth = b.getWidth();
		int bheight = b.getHeight(); 
		return Bitmap.createScaledBitmap(b, (int) scale*bwidth, (int) scale*bheight, true);
	}
	
	public static Bitmap fitSize(Bitmap b, int w, int h) 
	{
		if(b == null) return b;
		
		int newWidth = 0; 
		int newHeight = 0;
		
		if(w > 0) { 
			newWidth=w;
	    	newHeight = (int) Math.floor((double) b.getHeight() *( (double) newWidth / (double) b.getWidth()));
		} else { 
			newHeight=h;
	    	newWidth = (int) Math.floor((double) b.getWidth() *( (double) newHeight / (double) b.getHeight()));
		}
		return Bitmap.createScaledBitmap(b, newWidth, newHeight, true);
	}

	public static byte[] getImageBlob(Context context, String urlPath, String imageName) {
		
		if(imageName.equals("")) {
			Log.w(WordsLoaderService.class.getName(), "Skipping image, url empty.");
			return null; 
		}
		
		// concatenating URL path to image file on web server
		String imageURL = urlPath + imageName;
		
		Log.w(Bitmap.class.getName(), "Downloading image: " + imageURL); 
		
		// downloading image as byte array
		byte[] imageBuffer = NetworkUtilities.downloadFromURL(imageURL); 
	     
		return imageBuffer; 
	}
}
