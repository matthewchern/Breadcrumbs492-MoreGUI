package com.tumblr.breadcrumbs492.testapplication;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Object;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Base64;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;


public class LoginActivity extends ActionBarActivity {
    private MainFragment mainFragment;

    private MyRequestReceiver receiver;
    private static boolean loggedIn = false;
    //sign in or register button click
    public void signInOrRegister(View view) {
        //get text from text fields
        EditText username = (EditText) findViewById(R.id.enter_username);
        EditText password = (EditText) findViewById(R.id.enter_password);

        String usernameText = username.getText().toString();
        String passwordText = password.getText().toString();

        if (usernameText.equalsIgnoreCase("admin") && passwordText.equals("admin")) {
            //this is the default user login that will launch the map activity
            Intent intent = new Intent(this, MapsActivity.class);
            intent.putExtra(MapsActivity.GUESTLOGIN, false);
            startActivity(intent);
        } else if (usernameText.equals("") && passwordText.equals("")) {
            //if fields are empty then launch register activity
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        } else {
            //this is for all user logins
            //credentials will be checked against a database for verification here
            //if successful, it will launch the map activity
            //toast is a UI notification the user will see
           login("login", usernameText, passwordText);

         }//end if else
    }//end signInOrRegister

    public void loginAsGuest(View view) {
        //automatically launch MapsActivity when signing in as guest
        Intent intent = new Intent(this, MapsActivity.class);

        //send boolean to signal it's a guest login
        intent.putExtra(MapsActivity.GUESTLOGIN, true);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //retrieve the hash key to log in with facebook
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.tumblr.breadcrumbs492.testapplication",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }

        if (savedInstanceState == null) {
            // Add the fragment on initial activity setup
            mainFragment = new MainFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, mainFragment)
                    .commit();
        } else {
            // Or set the fragment from restored state info
            mainFragment = (MainFragment) getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
        }

        //Register your receiver so that the Activity can be notified
        //when the JSON response came back
        IntentFilter filter = new IntentFilter(MyRequestReceiver.PROCESS_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new MyRequestReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void login(String queryID, String username, String passEntered) {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra(MapsActivity.GUESTLOGIN, false);

        //pass the request to your service so that it can
        //run outside the scope of the main UI thread
        Intent msgIntent = new Intent(this, JSONRequest.class);
        msgIntent.putExtra(JSONRequest.IN_MSG, queryID.trim());
        msgIntent.putExtra("queryID", queryID.trim());
        msgIntent.putExtra("jsonObject", "{\"username\":\"" + username.trim() + "\",\"passEntered\":\"" + passEntered + "\"}");
        msgIntent.putExtra("intent", intent.toUri(Intent.URI_INTENT_SCHEME));
        startService(msgIntent);
    }

    //broadcast receiver to receive messages sent from the JSON IntentService
    public class MyRequestReceiver extends BroadcastReceiver{

        public static final String PROCESS_RESPONSE = "com.tumblr.breadcrumbs492.testapplication.LoginActivity.MyRequestReceiver";
        public String response = null;
        @Override
        public void onReceive(Context context, Intent intent) {
           Intent mapsIntent = new Intent();
           String responseType = intent.getStringExtra(JSONRequest.IN_MSG);

           if(responseType.trim().equalsIgnoreCase("login")){

               this.response = intent.getStringExtra(JSONRequest.OUT_MSG);

               try {
                   mapsIntent = Intent.parseUri(intent.getStringExtra("intent"), Intent.URI_INTENT_SCHEME);
               }
               catch(URISyntaxException e)
               {
                   e.printStackTrace();
               }
               JSONObject tempJSON = new JSONObject();
               try {
                   tempJSON = new JSONObject(response);
                   if(tempJSON.get("loginResult").equals("true"))
                   {
                       loggedIn = true;
                       Toast.makeText(getApplicationContext(), "login success", Toast.LENGTH_SHORT).show();
                       mapsIntent.putExtra("username", tempJSON.getString("username"));
                       startActivity(mapsIntent);
                   }
                   else
                   {
                       Toast.makeText(getApplicationContext(), "login fail", Toast.LENGTH_SHORT).show();
                   }
               }
               catch(JSONException e)
               {
                   Toast.makeText(getApplicationContext(), "login fail", Toast.LENGTH_SHORT).show();
                   e.printStackTrace();
               }


            }
            else{
                //you can choose to implement another transaction here
            }

        }
        public String getResponse()
        {
            return response;
        }
    }
}
