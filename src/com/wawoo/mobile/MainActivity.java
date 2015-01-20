package com.wawoo.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.wawoo.adapter.MainMenuAdapter;

public class MainActivity extends Activity {

	// private static final String TAG = MainActivity.class.getName();
	ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// logout logic
				boolean isLogout = getIntent().getBooleanExtra("LOGOUT", false);
				if (isLogout) {
					finish();
				}
		setContentView(R.layout.activity_main);

		listView = (ListView) findViewById(R.id.a_main_lv_menu);
		MainMenuAdapter menuAdapter = new MainMenuAdapter(this);
		listView.setAdapter(menuAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				switch (arg2) {
				case 0:
					startActivity(new Intent(MainActivity.this,
							ChannelsActivity.class));
					break;
				case 1:
					Intent intent1 = new Intent(MainActivity.this,
							VodActivity.class);
					startActivity(intent1);
					break;
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.nav_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_account:
			startActivity(new Intent(this, MyAccountActivity.class));
			break;
		case R.id.action_logout:
			logout();
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			AlertDialog mConfirmDialog = ((MyApplication) getApplicationContext())
					.getConfirmDialog(this);
			mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			mConfirmDialog.show();
		} else if (keyCode == KeyEvent.KEYCODE_HOME) {
			Toast.makeText(this, "Home button pressed", Toast.LENGTH_LONG)
					.show();
			return super.onKeyDown(keyCode, event);
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public void logout() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this,
				AlertDialog.THEME_HOLO_LIGHT);
		builder.setIcon(R.drawable.ic_logo_confirm_dialog);
		builder.setTitle("Confirmation");
		builder.setMessage("Are you sure to Logout?");
		builder.setCancelable(false);
		AlertDialog dialog = builder.create();
		dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int buttonId) {
					}
				});
		dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						// Clear shared preferences..
						((MyApplication) getApplicationContext()).getEditor().clear().commit();
						// close all activities..
						Intent Closeintent = new Intent(MainActivity.this,
								MainActivity.class);
						// set the new task and clear flags
						Closeintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
								| Intent.FLAG_ACTIVITY_CLEAR_TOP);
						Closeintent.putExtra("LOGOUT", true);
						startActivity(Closeintent);
						finish();
					}
				});
		dialog.show();

	}

}