package pl.electoroffline;

import android.annotation.SuppressLint;
import android.content.Context;

public class CopyUtility {

	@SuppressLint("NewApi")
	public static void copyText(Context context, String text) {
		
		int sdk = android.os.Build.VERSION.SDK_INT;
		if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		    clipboard.setText(text);
		} else {
		    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE); 
		    android.content.ClipData clip = android.content.ClipData.newPlainText(text,text);
		    clipboard.setPrimaryClip(clip);
		}
	}
}
