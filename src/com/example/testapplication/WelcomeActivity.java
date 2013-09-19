package com.example.testapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class WelcomeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
		
		findViewById(R.id.registerButton).setOnClickListener(
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
						startActivityForResult(intent, 0);
					}
				});
		findViewById(R.id.loginButton).setOnClickListener(
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
						startActivityForResult(intent, 0);
					}
				});
		
	}

	@Override
	public void onBackPressed() {
	    Intent startMain = new Intent(Intent.ACTION_MAIN);
	    startMain.addCategory(Intent.CATEGORY_HOME);
	    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    startActivity(startMain);
	    finish();
	}

}
