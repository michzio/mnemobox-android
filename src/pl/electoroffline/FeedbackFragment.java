/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
/**
 *
 * @author Michał Ziobro 
 */
public class FeedbackFragment extends Fragment {
	
	View view; 

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
    	view = inflater.inflate(R.layout.feedback, container, false);
        
    	Button sendButton = (Button) view.findViewById(R.id.sendFeedback);
    	sendButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View buttonView) {
				EditText editText = (EditText) view.findViewById(R.id.feedback_text); 
				sendFeedbackEmail(editText.getText().toString());
			}
		});
        
        return view; 
    }
    
    @Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
	}
    
    private void sendFeedbackEmail(String feedbackText) {
 
    	Intent i = new Intent(Intent.ACTION_SEND);
    	i.setType("message/rfc822");
    	i.putExtra(Intent.EXTRA_EMAIL  , new String[]{getActivity().getString(R.string.feedback_email)});
    	i.putExtra(Intent.EXTRA_SUBJECT, getActivity().getString(R.string.feedback_subject));
    	i.putExtra(Intent.EXTRA_TEXT   , feedbackText );
    	try {
    	    startActivity(Intent.createChooser(i, getString(R.string.send_feedback)));
    	} catch (android.content.ActivityNotFoundException ex) {
    	    Toast.makeText(getActivity(), R.string.no_email_clients, Toast.LENGTH_SHORT).show();
    	}
    }
    
}
