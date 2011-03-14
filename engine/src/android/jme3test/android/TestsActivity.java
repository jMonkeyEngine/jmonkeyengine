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


public class TestsActivity extends Activity {

	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TestsActivity.class.getName());


	public static class Test {

		private String name = null;
		private String className = null;

		public Test(String name, String className) {
			this.name = name;
			this.className = className;
		}

		public String getName() {
			return name;
		}

		public String getClassName() {
			return className;
		}
	}

	private final static Test[] tests = {
		new Test("SimpleTextured", "jme3test.android.SimpleTexturedTest"),
		new Test("light.TestLightRadius", "jme3test.light.TestLightRadius"),
		new Test("bullet.TestSimplePhysics", "jme3test.bullet.TestSimplePhysics"),
		new Test("helloworld.HelloJME3", "jme3test.helloworld.HelloJME3"),
		new Test("helloworld.HelloLoop", "jme3test.helloworld.HelloLoop"),
		new Test("helloworld.HelloNode", "jme3test.helloworld.HelloNode"),
		new Test("helloworld.HelloEffects", "jme3test.helloworld.HelloEffects"),
		new Test("helloworld.HelloTerrain", "jme3test.helloworld.HelloTerrain")
	};

	private CheckBox useVA = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		logger.info("onCreate(" + savedInstanceState + ")");

		super.onCreate(savedInstanceState);

		setContentView(R.layout.tests);

		try {

			useVA = (CheckBox) findViewById(R.id.useVA);

		LinearLayout buttonsContainer = (LinearLayout) findViewById(R.id.buttonsContainer);


		for (Test test: tests) {
			final Button button = new Button(this);
			final String finalName = test.getName();
			final String finalClassName = test.getClassName();

			button.setText(test.getName());
//			button.setTextSize(10.0f);
//			button.setTextColor(Color.rgb(100, 200, 200));
			buttonsContainer.addView(button);

			button.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent intent = new Intent(view.getContext(), AndroidActivity.class);
						intent.putExtra(AndroidActivity.class.getName() + ".TEST_CLASS_NAME", finalClassName); 
						intent.putExtra(AndroidActivity.class.getName() + ".USE_VA", useVA.isChecked()); 
						startActivityForResult(intent, 0);
					}
				}
			);
		}
		} catch (Exception exception) {
			logger.warning("exception: " + exception);
			exception.printStackTrace(System.err);
		}
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.about_button:
				about();
				return true;
			case R.id.quit_button:
				quit();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void quit() {
		finish();
	}

	private void about() {
	//	Intent intent = new Intent(getView().getContext(), AboutActivity.class);
		try {
			Intent intent = new Intent();
			intent.setClassName(
				"jme3test.android",
				"jme3test.android.AboutActivity"
			);
			startActivity(intent);
		} catch (Exception exception) {
			logger.warning("exception: " + exception);
			exception.printStackTrace(System.err);
		}
	}


}

