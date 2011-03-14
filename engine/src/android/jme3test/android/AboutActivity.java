package jme3test.android;


import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;

import android.content.Intent;

import android.view.View;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.LinearLayout;
import android.widget.TableRow;

import android.hardware.SensorManager;
//import android.hardware.SensorListener;

import jme3test.android.AndroidActivity;

import java.net.URI;


public class AboutActivity extends Activity {

	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AboutActivity.class.getName());


	@Override
	public void onCreate(Bundle savedInstanceState) {
		logger.info("onCreate(" + savedInstanceState + ")");

		super.onCreate(savedInstanceState);

		setContentView(R.layout.about);
	}

	@Override
	public void onDestroy() {
		logger.info("onDestroy()");
		super.onDestroy();
	}


	@Override
	protected void onResume() {
		super.onResume();
	}
 
	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}


}

