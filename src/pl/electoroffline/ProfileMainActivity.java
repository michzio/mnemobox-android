/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;


/**
 *
 * @author Michał Ziobro
 */
public class ProfileMainActivity extends Activity {
       @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profilemain);
        
        GetUserinfoFromXML userinfo = GetUserinfoFromXML.getMyProfileInfo(this); 
        if(userinfo != null) { 
        TextView nametxt = (TextView) findViewById(R.id.nameText); 
        nametxt.setText(userinfo.firstName + " " + userinfo.lastName); 
        TextView emailtxt = (TextView) findViewById(R.id.emailText); 
        emailtxt.setText(userinfo.email); 
        ImageView image = (ImageView) findViewById(R.id.userPhoto); 
        UrlImageViewHelper.setUrlDrawable(image, 
                    getResources().getString(R.string.avatars_url) + userinfo.userImage);
        }
    }
    
    public void launchProfileInfo(View v) { 
        Intent mainIntent = new Intent().setClass(ProfileMainActivity.this, ProfileInfoActivity.class);
        startActivity(mainIntent); 
    }
    
    public void launchStats(View v) { 
        Intent mainIntent = new Intent().setClass(ProfileMainActivity.this, ProfileStatsActivity.class);
        startActivity(mainIntent); 
    }
    public void launchHistory(View v) { 
        Intent mainIntent = new Intent().setClass(ProfileMainActivity.this, HistoryActivity.class);
        startActivity(mainIntent); 
    }
    public void launchMoney(View v) { 
        Intent mainIntent = new Intent().setClass(ProfileMainActivity.this, MoneyHistoryActivity.class);
        startActivity(mainIntent); 
    }
          
}
