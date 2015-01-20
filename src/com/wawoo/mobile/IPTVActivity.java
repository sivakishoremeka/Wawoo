package com.wawoo.mobile;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import com.wawoo.adapter.EPGFragmentPagerAdapter;
import com.wawoo.data.ChannelsDatum;
import com.wawoo.data.Reminder;
import com.wawoo.data.ResponseObj;
import com.wawoo.database.DBHelper;
import com.wawoo.mobile.EpgFragment.ProgDetails;
import com.wawoo.retrofit.OBSClient;
import com.wawoo.service.ScheduleClient;
import com.wawoo.utils.Utilities;

public class IPTVActivity extends FragmentActivity {

	/** This is live/Iptv activity */

	// public static String TAG = IPTVActivity.class.getName();
	public final static String CHANNEL_DESC = "Channel Desc";
	public final static String CHANNEL_URL = "URL";
	private SharedPreferences mPrefs;
	private Editor mPrefsEditor;
	EPGFragmentPagerAdapter mEpgPagerAdapter;

	// This is a handle so that we can call methods on our service
	private ScheduleClient scheduleClient;

	private ProgressDialog mProgressDialog;
	private String mChannelURL;
	private int mChannelId;
	ViewPager mViewPager;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;
	boolean requiredLiveData = false;

	AlertDialog mConfirmDialog;
	
	public static int INSTALL_MXPLAYER_MARKET = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_iptv);
		findViewById(R.id.a_iptv_rl_root_layout).setVisibility(View.INVISIBLE);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		mApplication = ((MyApplication) getApplicationContext());
		mOBSClient = mApplication.getOBSClient();

		mPrefs = mApplication.getPrefs();
		// for not refresh data
		mPrefsEditor = mApplication.getEditor();
		mPrefsEditor.putBoolean(EpgFragment.IS_REFRESH, false);
		mPrefsEditor.commit();
		// for not refresh data
		Button btn = (Button) findViewById(R.id.a_iptv_btn_watch_remind);
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				String label = ((Button) v).getText().toString();
				if (label.trim().equalsIgnoreCase("Watch")) {

					if (mApplication.balanceCheck == true
							&& (mApplication.getBalance() > 0)) {
						final boolean isPayPalChk = mApplication
								.isPayPalCheck();
						AlertDialog.Builder builder = new AlertDialog.Builder(
								(IPTVActivity.this),
								AlertDialog.THEME_HOLO_LIGHT);
						builder.setIcon(R.drawable.ic_logo_confirm_dialog);
						builder.setTitle("Confirmation");
						String msg = "Insufficient Balance."
								+ (isPayPalChk == true ? "Go to PayPal ??"
										: "Please do Payment.");
						builder.setMessage(msg);
						builder.setCancelable(true);
						mConfirmDialog = builder.create();
						mConfirmDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
								(isPayPalChk == true ? "No" : ""),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int buttonId) {
									}
								});
						mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE,
								(isPayPalChk == true ? "Yes" : "Ok"),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										if (isPayPalChk == true) {
											Intent svcIntent = new Intent(
													IPTVActivity.this,
													PayPalService.class);

											svcIntent
													.putExtra(
															PayPalService.EXTRA_PAYPAL_CONFIGURATION,
															mApplication
																	.getPaypalConfig());

											startService(svcIntent);

											PayPalPayment paymentData = new PayPalPayment(
													new BigDecimal(mApplication
															.getBalance()),
													mApplication.getCurrency(),
													getResources().getString(
															R.string.app_name)
															+ " -Payment",
													PayPalPayment.PAYMENT_INTENT_SALE);

											Intent actviIntent = new Intent(
													IPTVActivity.this,
													PaymentActivity.class);

											actviIntent
													.putExtra(
															PaymentActivity.EXTRA_PAYMENT,
															paymentData);

											startActivityForResult(
													actviIntent,
													MyApplication.REQUEST_CODE_PAYMENT);
										}
									}
								});
						mConfirmDialog.show();
					} else {
						startMediaPlayer();
					}
				} else {
					/**
					 * This is called to set a new notification
					 */
					ProgDetails progDtls = (ProgDetails) ((Button) v).getTag();
					// Ask our service to set an alarm for that date, this
					// activity talks to the client that talks to the service

					if (progDtls != null) {
						scheduleClient.setAlarmForNotification(
								progDtls.calendar, progDtls.progTitle,
								mChannelId, progDtls.channelDesc, mChannelURL);
						SimpleDateFormat sdf = new SimpleDateFormat(
								"yyyy-MM-dd HH:mm", new Locale("en"));
						String date = sdf.format(progDtls.calendar.getTime());

						// add to db
						DBHelper dbHandler = new DBHelper(IPTVActivity.this);
						dbHandler.deleteOldReminders();
						dbHandler.addReminder(new Reminder(progDtls.progTitle,
								progDtls.calendar.getTimeInMillis(),
								mChannelId, progDtls.channelDesc, mChannelURL));
						Toast.makeText(IPTVActivity.this,
								"Notification set for: " + date,
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		});

		mViewPager = (ViewPager) findViewById(R.id.a_iptv_pager);

		CheckCacheForChannelList();

		// Create a new service client and bind our activity to this service
		scheduleClient = new ScheduleClient(this);
		scheduleClient.doBindService();
	}

	protected void startMediaPlayer() {

		Intent intent = new Intent();
		intent.putExtra("VIDEOTYPE", "LIVETV");
		intent.putExtra(CHANNEL_URL, mChannelURL);
		intent.putExtra("CHANNELID", mChannelId);
		switch (MyApplication.player) {
		case NATIVE_PLAYER:
			intent.setClass(getApplicationContext(), VideoPlayerActivity.class);
			startActivity(intent);
			break;
		case MXPLAYER:			
			initiallizeMXPlayer();
			break;
		default:
			intent.setClass(getApplicationContext(), VideoPlayerActivity.class);
			startActivity(intent);
			break;
		}
	}

	private void initiallizeMXPlayer() {
		//String TAG = "mxvp.intent.test";
		String MXVP = "com.mxtech.videoplayer.ad";
		String MXVP_PRO = "com.mxtech.videoplayer.pro";

		String MXVP_PLAYBACK_CLASS = "com.mxtech.videoplayer.ad.ActivityScreen";
		//String MXVP_PRO_PLAYBACK_CLASS = "com.mxtech.videoplayer.ActivityScreen";

		//String RESULT_VIEW = "com.mxtech.intent.result.VIEW";
		String EXTRA_DECODE_MODE = "decode_mode"; // (byte)
		String EXTRA_SECURE_URI = "secure_uri";
		//String EXTRA_VIDEO_LIST = "video_list";
		//String EXTRA_SUBTITLES = "subs";
		//String EXTRA_SUBTITLES_ENABLE = "subs.enable";
		//String EXTRA_TITLE = "title";
		//String EXTRA_POSITION = "position";
		String EXTRA_RETURN_RESULT = "return_result";
		String EXTRA_HEADERS = "headers";
		
		Uri mUri = Uri.parse(mChannelURL);
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(mUri, "application/*");
		i.putExtra(EXTRA_SECURE_URI, true);
		// s/w decoder
		i.putExtra(EXTRA_DECODE_MODE, (byte) 2);
		// request result
		i.putExtra(EXTRA_RETURN_RESULT, true);
		String[] headers = new String[] { "User-Agent",
				"MX Player Caller App/1.0", "Extra-Header", "911" };
		i.putExtra(EXTRA_HEADERS, headers);
		try {
			i.setPackage(MXVP_PRO);
			PackageManager pm = getPackageManager();
			ResolveInfo info = pm.resolveActivity(i,
					PackageManager.MATCH_DEFAULT_ONLY);
			if (info == null) {
			i.setPackage(MXVP);
			i.setClassName(MXVP, MXVP_PLAYBACK_CLASS);
			info = pm.resolveActivity(i,
					PackageManager.MATCH_DEFAULT_ONLY);
			if (info == null) {
				AlertDialog mConfirmDialog = ((MyApplication) getApplicationContext())
						.getConfirmDialog(this);
				mConfirmDialog.setMessage("MXPlayer is not found in Device. Are you sure to download from Play Store.??");
				mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Intent goToMarket = new Intent(Intent.ACTION_VIEW)
							    .setData(Uri.parse("market://details?id=com.mxtech.videoplayer.ad&hl=en"));
								startActivityForResult(goToMarket,INSTALL_MXPLAYER_MARKET);
							}
						});
				mConfirmDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						});
				mConfirmDialog.show();
			} else{
			startActivity(i);
			return;
			}
			}
			else
			{
				startActivity(i);
				return;
			}
		} catch (ActivityNotFoundException e2) {
			Log.e("MxException", e2.getMessage().toString());
		}
	}

	@Override
	protected void onStop() {
		// When our activity is stopped ensure we also stop the connection to
		// the service this stops us leaking our activity into the system *bad*
		if (scheduleClient != null)
			scheduleClient.doUnbindService();
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.nav_menu, menu);
		MenuItem refreshItem = menu.findItem(R.id.action_refresh);
		refreshItem.setVisible(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		case R.id.action_home:
			startActivity(new Intent(IPTVActivity.this, MainActivity.class)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			break;
		case R.id.action_account:
			startActivity(new Intent(this, MyAccountActivity.class));
			break;
		case R.id.action_refresh:
			// refresh epg data
			mApplication.getEditor().putBoolean(EpgFragment.IS_REFRESH, true);
			mApplication.getEditor().commit();
			mEpgPagerAdapter = new EPGFragmentPagerAdapter(
					IPTVActivity.this.getSupportFragmentManager());
			mViewPager.setAdapter(mEpgPagerAdapter);
			break;
		case R.id.action_logout:
			logout();
			break;
		default:
			break;
		}
		return true;
	}

	private void CheckCacheForChannelList() {
		String chListKey = getResources().getString(R.string.channels_list);
		String sChannelsList = mPrefs.getString(chListKey, "");
		if (sChannelsList.length() != 0) {
			updateChannels(getChannelsListFromJSON(sChannelsList));
		}
	}

	private void updateChannels(List<ChannelsDatum> result) {
		int imgno = 0;
		LinearLayout channels = (LinearLayout) findViewById(R.id.a_iptv_ll_channels);
		channels.removeAllViews();

		final Editor editor = mPrefs.edit();
		for (final ChannelsDatum data : result) {
			editor.putString(data.channelDescription, data.url);
			editor.commit();
			imgno += 1;
			ChannelInfo chInfo = new ChannelInfo(data.channelDescription,
					data.url, data.serviceId);
			final ImageButton button = new ImageButton(this);
			LayoutParams params = new LayoutParams(Gravity.CENTER);
			params.height = 96;
			params.width = 96;
			// params.setMargins(1, 1, 1, 1);
			button.setLayoutParams(params);
			button.setId(1000 + imgno);
			button.setTag(chInfo);
			button.setFocusable(false);
			button.setFocusableInTouchMode(false);
			// button.setBackgroundDrawable(getResources().getDrawable(R.drawable.border2));
			button.setImageDrawable(getResources().getDrawable(
					R.drawable.ic_default_ch));
			if (mPrefs.getString(CHANNEL_DESC, "") != null) {
				mChannelURL = mPrefs.getString(CHANNEL_URL, "");
			}
			ImageLoader.getInstance().displayImage(data.image, button);

			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ChannelInfo info = (ChannelInfo) v.getTag();
					editor.putString(CHANNEL_DESC, info.channelDesc);
					editor.putString(CHANNEL_URL, info.channelURL);
					// for not refresh data
					editor.putBoolean(EpgFragment.IS_REFRESH, false);
					// for not refresh data
					editor.commit();
					// centerLockHorizontalScrollview.setCenter(v.getId()-1001);
					mChannelURL = info.channelURL;
					mChannelId = info.channelId;
					mEpgPagerAdapter = new EPGFragmentPagerAdapter(
							IPTVActivity.this.getSupportFragmentManager());
					mViewPager.setAdapter(mEpgPagerAdapter);
				}
			});
			channels.addView(button);
		}
		mEpgPagerAdapter = new EPGFragmentPagerAdapter(
				this.getSupportFragmentManager());
		mViewPager.setAdapter(mEpgPagerAdapter);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		/** After Installation, start MXPLayer... */
		if(requestCode == INSTALL_MXPLAYER_MARKET){
			initiallizeMXPlayer();
			return;
		}
		/** Stop PayPalIntent Service... */
		stopService(new Intent(this, PayPalService.class));
		if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
			mConfirmDialog.dismiss();
		}
		if (resultCode == Activity.RESULT_OK) {
			PaymentConfirmation confirm = data
					.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
			if (confirm != null) {
				try {
					Log.i("OBSPayment", confirm.toJSONObject().toString(4));
					/** Call OBS API for verification and payment record. */
					OBSPaymentAsyncTask task = new OBSPaymentAsyncTask();
					task.execute(confirm.toJSONObject().toString(4));
				} catch (JSONException e) {
					Log.e("OBSPayment",
							"an extremely unlikely failure occurred: ", e);
				}
			}
		} else if (resultCode == Activity.RESULT_CANCELED) {
			Log.i("OBSPayment", "The user canceled.");
			Toast.makeText(this, "The user canceled.", Toast.LENGTH_LONG)
					.show();
		} else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
			Log.i("OBSPayment",
					"An Invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
			Toast.makeText(this,
					"An Invalid Payment or PayPalConfiguration was submitted",
					Toast.LENGTH_LONG).show();
		}
	}

	private class OBSPaymentAsyncTask extends
			AsyncTask<String, Void, ResponseObj> {
		JSONObject reqJson = null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(IPTVActivity.this,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Connecting to Server...");
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface arg0) {
					if (mProgressDialog.isShowing())
						mProgressDialog.dismiss();

					Toast.makeText(IPTVActivity.this,
							"Payment verification Failed.", Toast.LENGTH_LONG)
							.show();
					cancel(true);
				}
			});
			mProgressDialog.show();
		}

		@Override
		protected ResponseObj doInBackground(String... arg) {
			ResponseObj resObj = new ResponseObj();
			try {
				reqJson = new JSONObject(arg[0]);

				if (mApplication.isNetworkAvailable()) {
					resObj = Utilities.callExternalApiPostMethod(
							getApplicationContext(),
							"/payments/paypalEnquirey/"
									+ mApplication.getClientId(), reqJson);
				} else {
					resObj.setFailResponse(100, "Network error.");
				}
			} catch (JSONException e) {
				Log.e("IPTVActivity-ObsPaymentCheck",
						(e.getMessage() == null) ? "Json Exception" : e
								.getMessage());
				e.printStackTrace();
				Toast.makeText(IPTVActivity.this,
						"Invalid data: On PayPal Payment ", Toast.LENGTH_LONG)
						.show();
			}
			if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
				mConfirmDialog.dismiss();
			}
			return resObj;
		}

		@Override
		protected void onPostExecute(ResponseObj resObj) {

			super.onPostExecute(resObj);
			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}

			if (resObj.getStatusCode() == 200) {
				if (resObj.getsResponse().length() > 0) {
					JSONObject json;
					try {
						json = new JSONObject(resObj.getsResponse());
						json = json.getJSONObject("changes");
						if (json != null) {
							String paymentStatus = json
									.getString("paymentStatus");
							if (paymentStatus.equalsIgnoreCase("Success")) {
								mApplication.setBalance((float) json
										.getLong("totalBalance"));
								Toast.makeText(IPTVActivity.this,
										"Payment Verification Success",
										Toast.LENGTH_LONG).show();
								startMediaPlayer();

							} else if (paymentStatus.equalsIgnoreCase("Fail")) {
								Toast.makeText(IPTVActivity.this,
										"Payment Verification Failed",
										Toast.LENGTH_LONG).show();
							}
						}

					} catch (JSONException e) {
						Toast.makeText(IPTVActivity.this, "Server Error",
								Toast.LENGTH_LONG).show();
						Log.i("IPTVActivity",
								"JsonEXception at payment verification");
					} catch (NullPointerException e) {
						Toast.makeText(IPTVActivity.this, "Server Error  ",
								Toast.LENGTH_LONG).show();
						Log.i("IPTVActivity",
								"Null PointerEXception at payment verification");
					}

				}
			} else {
				Toast.makeText(IPTVActivity.this, "Server Error",
						Toast.LENGTH_LONG).show();
			}
		}
	}

	private List<ChannelsDatum> getChannelsListFromJSON(String json) {
		java.lang.reflect.Type t = new TypeToken<List<ChannelsDatum>>() {
		}.getType();
		List<ChannelsDatum> channelsList = new Gson().fromJson(json, t);
		return channelsList;
	}

	private class ChannelInfo {
		private String channelDesc;
		private String channelURL;
		private int channelId;

		public ChannelInfo(String channelDesc, String channelURL, int channelId) {
			this.channelDesc = channelDesc;
			this.channelURL = channelURL;
			this.channelId = channelId;
		}
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
						((MyApplication) getApplicationContext()).getEditor().clear().commit();;
						// close all activities..
						Intent Closeintent = new Intent(IPTVActivity.this,
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
