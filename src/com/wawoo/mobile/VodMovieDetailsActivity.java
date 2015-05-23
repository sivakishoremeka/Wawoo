package com.wawoo.mobile;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import com.wawoo.data.MediaDetailsResDatum;
import com.wawoo.data.PriceDetail;
import com.wawoo.data.ResponseObj;
import com.wawoo.mobile.MyApplication.DoBGTasks;
import com.wawoo.paypal.PaypalHelper;
import com.wawoo.paypal.PaypalHelper.UpdatePaymentTaskListener;
import com.wawoo.retrofit.OBSClient;
import com.wawoo.service.DoBGTasksService;
import com.wawoo.utils.Utilities;

public class VodMovieDetailsActivity extends Activity implements
		UpdatePaymentTaskListener {

	// public static String TAG = VodMovieDetailsActivity.class.getName();
	private final static String NETWORK_ERROR = "NETWORK_ERROR";
	private final static String BOOK_ORDER = "BOOK_ORDER";
	private ProgressDialog mProgressDialog;
	String mediaId;
	String eventId;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;
	String mDeviceId;

	private String mChannelURL;
	AlertDialog mConfirmDialog;
	double mVodPrice;
	PaypalHelper mPaypalHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vod_mov_details);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		Bundle b = getIntent().getExtras();
		mediaId = b.getString("MediaId");
		eventId = b.getString("EventId");

		mApplication = ((MyApplication) getApplicationContext());
		mOBSClient = mApplication.getOBSClient();
		mPaypalHelper = new PaypalHelper(this, mApplication);
		mDeviceId = Settings.Secure.getString(
				mApplication.getContentResolver(), Settings.Secure.ANDROID_ID);

		if ((!(mediaId.equalsIgnoreCase("")) || mediaId != null)
				&& (!(eventId.equalsIgnoreCase("")) || eventId != null)) {
			RelativeLayout rl = (RelativeLayout) findViewById(R.id.a_vod_mov_dtls_root_layout);
			rl.setVisibility(View.INVISIBLE);

			Intent updateDataIntent = new Intent(VodMovieDetailsActivity.this,
					DoBGTasksService.class);
			updateDataIntent.putExtra(DoBGTasksService.TASK_ID,
					DoBGTasks.UPDATECLIENT_CONFIGS.ordinal());
			VodMovieDetailsActivity.this.startService(updateDataIntent);
			UpdateDetails();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.nav_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		case R.id.action_home:
			startActivity(new Intent(VodMovieDetailsActivity.this,
					MainActivity.class)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			break;
		case R.id.action_account:
			startActivity(new Intent(this, MyAccountActivity.class));
			break;
		case R.id.action_refresh:
			// refresh data
			UpdateDetails();
			break;
		case R.id.action_logout:
			logout();
			break;
		default:
			break;
		}
		return true;
	}

	public void btnOnClick(View v) {

		// Log.d("Btn Click", ((Button) v).getText().toString());
		final boolean isPayPalReq = mApplication.isPayPalCheck();
		final float balance = mApplication.getBalance();

		if ((mVodPrice != 0 && (-balance < mVodPrice)) || balance > 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder((this),
					AlertDialog.THEME_HOLO_LIGHT);
			builder.setIcon(R.drawable.ic_logo_confirm_dialog);
			builder.setTitle("Confirmation");
			String msg = "Insufficient Balance."
					+ (isPayPalReq == true ? "Go to PayPal ??"
							: "Please do Payment.");
			builder.setMessage(msg);
			builder.setCancelable(true);
			mConfirmDialog = builder.create();
			mConfirmDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
					(isPayPalReq == true ? "No" : ""),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int buttonId) {
						}
					});
			mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE,
					(isPayPalReq == true ? "Yes" : "Ok"),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (isPayPalReq == true) {
								mPaypalHelper.startPaypalActivity();
							}
						}
					});
			mConfirmDialog.show();
		} else {
			AlertDialog dialog = new AlertDialog.Builder(
					VodMovieDetailsActivity.this, AlertDialog.THEME_HOLO_LIGHT)
					.create();
			dialog.setIcon(R.drawable.ic_logo_confirm_dialog);
			dialog.setTitle("Confirmation");
			dialog.setMessage("Do you want to continue?");
			dialog.setCancelable(false);

			dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int buttonId) {
							BookOrder();
						}
					});
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int buttonId) {

						}
					});
			dialog.show();
		}
	}

	private void BookOrder() {
		new doBackGround().execute(BOOK_ORDER, "HD", "RENT");
	}

	public void UpdateDetails() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(VodMovieDetailsActivity.this,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Retrieving Details...");
		mProgressDialog.setCancelable(true);
		mProgressDialog.show();
		mOBSClient.getMediaDetails(mediaId, eventId, mDeviceId,
				getMovDetailsCallBack);
	}

	final Callback<MediaDetailsResDatum> getMovDetailsCallBack = new Callback<MediaDetailsResDatum>() {
		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (retrofitError.isNetworkError()) {
					Toast.makeText(
							VodMovieDetailsActivity.this,
							getApplicationContext().getString(
									R.string.error_network), Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(
							VodMovieDetailsActivity.this,
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
			}
		}

		@Override
		public void success(MediaDetailsResDatum data, Response response) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (data != null) {
					updateUI(data);
				} else {
					Toast.makeText(VodMovieDetailsActivity.this,
							"Server Error  ", Toast.LENGTH_LONG).show();
				}
			}
		}
	};

	private void initiallizeMXPlayer() {
		// String TAG = "mxvp.intent.test";
		String MXVP = "com.mxtech.videoplayer.ad";
		String MXVP_PRO = "com.mxtech.videoplayer.pro";

		String MXVP_PLAYBACK_CLASS = "com.mxtech.videoplayer.ad.ActivityScreen";
		// String MXVP_PRO_PLAYBACK_CLASS =
		// "com.mxtech.videoplayer.ActivityScreen";

		// String RESULT_VIEW = "com.mxtech.intent.result.VIEW";
		String EXTRA_DECODE_MODE = "decode_mode"; // (byte)
		String EXTRA_SECURE_URI = "secure_uri";
		// String EXTRA_VIDEO_LIST = "video_list";
		// String EXTRA_SUBTITLES = "subs";
		// String EXTRA_SUBTITLES_ENABLE = "subs.enable";
		// String EXTRA_TITLE = "title";
		// String EXTRA_POSITION = "position";
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
				info = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
				if (info == null) {
					AlertDialog mConfirmDialog = ((MyApplication) getApplicationContext())
							.getConfirmDialog(VodMovieDetailsActivity.this);
					mConfirmDialog
							.setMessage("MXPlayer is not found in Device. Are you sure to download from Play Store.??");
					mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE,
							"Yes", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Intent goToMarket = new Intent(
											Intent.ACTION_VIEW).setData(Uri
											.parse("market://details?id=com.mxtech.videoplayer.ad&hl=en"));
									startActivityForResult(
											goToMarket,
											MXPlayerActivity.INSTALL_MXPLAYER_MARKET);
								}
							});
					mConfirmDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
								}
							});
					mConfirmDialog.show();
				} else {
					startActivity(i);
					return;
				}
			} else {
				startActivity(i);
				return;
			}
		} catch (ActivityNotFoundException e2) {
			Log.e("MxException", e2.getMessage().toString());
		}
	}

	private class doBackGround extends AsyncTask<String, Void, ResponseObj> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(VodMovieDetailsActivity.this,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Retrieving Details...");
			mProgressDialog.setCancelable(true);
			mProgressDialog.show();
		}

		@Override
		protected ResponseObj doInBackground(String... params) {
			ResponseObj resObj = new ResponseObj();
			if (Utilities.isNetworkAvailable(VodMovieDetailsActivity.this
					.getApplicationContext())) {

				HashMap<String, String> map = new HashMap<String, String>();
				String sDateFormat = "yyyy-mm-dd";
				DateFormat df = new SimpleDateFormat(sDateFormat);
				String formattedDate = df.format(new Date());

				map.put("TagURL", "/eventorder");
				map.put("locale", "en");
				map.put("dateFormat", sDateFormat);
				map.put("eventBookedDate", formattedDate);
				map.put("formatType", params[1]);
				map.put("optType", params[2]);
				map.put("eventId", eventId);
				map.put("deviceId", mDeviceId);

				resObj = Utilities.callExternalApiPostMethod(
						VodMovieDetailsActivity.this.getApplicationContext(),
						map);
				return resObj;
			} else {
				resObj.setFailResponse(100, NETWORK_ERROR);
				return resObj;
			}
		}

		@Override
		protected void onPostExecute(ResponseObj resObj) {
			super.onPostExecute(resObj);
			if (resObj.getStatusCode() == 200) {
				if (mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}

				Intent intent = new Intent();
				try {
					mChannelURL = ((String) (new JSONObject(
							resObj.getsResponse())).get("resourceIdentifier"));
					intent.putExtra("URL",
							((String) (new JSONObject(resObj.getsResponse()))
									.get("resourceIdentifier")));
					intent.putExtra("VIDEOTYPE", "VOD");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				switch (MyApplication.player) {
				case NATIVE_PLAYER:
					intent.setClass(getApplicationContext(),
							VideoPlayerActivity.class);
					startActivity(intent);
					break;
				case MXPLAYER:
					initiallizeMXPlayer();
					break;
				default:
					intent.setClass(getApplicationContext(),
							VideoPlayerActivity.class);
					startActivity(intent);
					break;
				}

				finish();

			} else {
				if (mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(
						VodMovieDetailsActivity.this,
						AlertDialog.THEME_HOLO_LIGHT);
				// Add the buttons
				builder.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// MovieDetailsActivity.this.finish();
							}
						});
				AlertDialog dialog = builder.create();
				dialog.setMessage(resObj.getsErrorMessage());
				dialog.show();
			}
		}

	}

	public void updateUI(MediaDetailsResDatum data) {
		if (data != null) {
			List<PriceDetail> priceList = data.getPriceDetails();
			if (priceList != null && priceList.size() > 0) {
				for (PriceDetail detail : priceList) {
					if (detail.getOptType().equalsIgnoreCase("RENT")) {
						mVodPrice = detail.getPrice();
						break;
					}
				}
			}
			ImageLoader.getInstance().displayImage(data.getImage(),
					((ImageView) findViewById(R.id.a_vod_mov_dtls_iv_mov_img)));
			((RatingBar) findViewById(R.id.a_vod_mov_dtls_rating_bar))
					.setRating(data.getRating().floatValue());
			((TextView) findViewById(R.id.a_vod_mov_dtls_tv_mov_title))
					.setText(data.getTitle());
			((TextView) findViewById(R.id.a_vod_mov_dtls_tv_descr_value))
					.setText(data.getOverview());
			((TextView) findViewById(R.id.a_vod_mov_dtls_tv_durn_value))
					.setText(data.getDuration());
			((TextView) findViewById(R.id.a_vod_mov_dtls_tv_lang_value))
					.setText(getResources()
							.getStringArray(R.array.arrLangauges)[1]);
			((TextView) findViewById(R.id.a_vod_mov_dtls_tv_release_value))
					.setText(data.getReleaseDate());
			if (data.getActor().size() > 0) {
				String[] arrActors = new String[data.getActor().size()];
				data.getActor().toArray(arrActors);
				String actors = "";
				for (String actor : arrActors) {
					actors += actor;
				}
				if (actors.length() > 0) {
					((TextView) findViewById(R.id.a_vod_mov_dtls_tv_cast_value))
							.setText(actors);
				}
			}
			RelativeLayout rl = (RelativeLayout) findViewById(R.id.a_vod_mov_dtls_root_layout);
			if (rl.getVisibility() == View.INVISIBLE)
				rl.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == MXPlayerActivity.INSTALL_MXPLAYER_MARKET) {
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
					mPaypalHelper.updatePaymentInOBS(confirm.toJSONObject()
							.toString(4));
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
					"An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
			Toast.makeText(this,
					"An invalid Payment or PayPalConfiguration was submitted",
					Toast.LENGTH_LONG).show();
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
						((MyApplication) getApplicationContext()).getEditor()
								.clear().commit();
						;
						// close all activities..
						Intent Closeintent = new Intent(
								VodMovieDetailsActivity.this,
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

	// UpdatePaymentTaskListener callbacks
	@Override
	public void onSuccess() {
		BookOrder();
	}

	@Override
	public void onFailure() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getTag() {

		return VodMovieDetailsActivity.this.getClass().getName();
	}
}
