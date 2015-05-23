package com.wawoo.mobile;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import com.wawoo.adapter.CustomExpandableListAdapter;
import com.wawoo.adapter.PaytermAdapter;
import com.wawoo.data.Paytermdatum;
import com.wawoo.data.PlanDatum;
import com.wawoo.data.ResponseObj;
import com.wawoo.mobile.MyApplication.DoBGTasks;
import com.wawoo.parser.JSONPaytermConverter;
import com.wawoo.retrofit.CustomUrlConnectionClient;
import com.wawoo.retrofit.OBSClient;
import com.wawoo.service.DoBGTasksService;
import com.wawoo.utils.Utilities;

public class PlanActivity extends Activity {

	
	public static String TAG = MyProfileFragment.class.getName();	
	private final static String NETWORK_ERROR = "Network error.";
	private ProgressDialog mProgressDialog;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;
	SharedPreferences mPrefs;
	List<PlanDatum> mPlans;
	List<Paytermdatum> mPayterms;
	CustomExpandableListAdapter mExListAdapter;
	PaytermAdapter  mListAdapter;
	ExpandableListView mExpListView;
	ListView mPaytermLv;
	public static int selGroupId = -1;
	public static int selPaytermId = -1;
	AlertDialog mConfirmDialog ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_plan);

		mApplication = ((MyApplication) getApplicationContext());
		mOBSClient = mApplication.getOBSClient();
		Button btnNext = (Button) findViewById(R.id.a_plan_btn_submit);
		btnNext.setText(R.string.next);
		Button btnCancel = (Button) findViewById(R.id.a_plan_btn_cancel);
		btnCancel.setText(R.string.cancel);
		
		fetchAndBuildPlanList();
	}

	public void fetchAndBuildPlanList() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(PlanActivity.this,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Connecting Server...");
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface arg0) {
				if (mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				mIsReqCanceled = true;
			}
		});
		mProgressDialog.show();
		mOBSClient.getPrepaidPlans(getPlansCallBack);
	}

	final Callback<List<PlanDatum>> getPlansCallBack = new Callback<List<PlanDatum>>() {
		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (retrofitError.isNetworkError()) {
					Toast.makeText(
							PlanActivity.this,
							getApplicationContext().getString(
									R.string.error_network), Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(
							PlanActivity.this,
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
			} else
				mIsReqCanceled = false;
		}

		@Override
		public void success(List<PlanDatum> planList, Response response) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (planList != null) {
					mPlans = planList;
					buildPlansList();
				}
			} else
				mIsReqCanceled = false;
		}
	};

	private void buildPlansList() {
		mExpListView = (ExpandableListView) findViewById(R.id.a_plan_exlv);
		mExListAdapter = new CustomExpandableListAdapter(this, mPlans);
		mExpListView.setAdapter(mExListAdapter);
		mExpListView.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {
				RadioButton rb1 = (RadioButton) v
						.findViewById(R.id.plan_list_plan_rb);
				if (null != rb1 && (!rb1.isChecked())) {
					PlanActivity.selGroupId = groupPosition;
				} else {
					PlanActivity.selGroupId = -1;
				}
				return false;
			}
		});

	}

	public void btnSubmit_onClick(View v) {
		if (((Button) v).getText().toString()
				.equalsIgnoreCase(getString(R.string.next))) {
			if(selGroupId!=-1){
				Button btnCancel = (Button) findViewById(R.id.a_plan_btn_cancel);
				btnCancel.setText(R.string.back);
			getPaytermsforSelPlan();
			}
			else{
				Toast.makeText(this, "Choose a Plan to Subscribe", Toast.LENGTH_LONG).show();
			}
		}else if (((Button) v).getText().toString()
				.equalsIgnoreCase(getString(R.string.subscribe))) {
			
			if(selGroupId !=-1 && selPaytermId != -1){
				//Redirect to Paypal
				if (mApplication.balanceCheck == true
						&& (mApplication.getBalance() > 0)) {
				//if(true){  //k
				//	mApplication.setPayPalCheck(true);//k
					
					final boolean isPayPalChk = mApplication
							.isPayPalCheck();
					AlertDialog.Builder builder = new AlertDialog.Builder(
							(PlanActivity.this),
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
												PlanActivity.this,
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
												PlanActivity.this,
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
					//Book paln in OBS
					orderPlans(mPlans.get(selGroupId).toString());
				}
				
			}
			else{
				Toast.makeText(this, "Choose a Payterm to Subscribe", Toast.LENGTH_LONG).show();
			}
		}
	}

	private void getPaytermsforSelPlan(){
	RestAdapter restAdapter = new RestAdapter.Builder()
				.setEndpoint(MyApplication.API_URL)
				.setLogLevel(RestAdapter.LogLevel.NONE)
				.setConverter(new JSONPaytermConverter())
				.setClient(
						new CustomUrlConnectionClient(MyApplication.tenentId,
								MyApplication.basicAuth,
								MyApplication.contentType)).build();
		 OBSClient client = restAdapter.create(OBSClient.class);
		 if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(this,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Connecting Server");
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface arg0) {
					if (mProgressDialog.isShowing())
						mProgressDialog.dismiss();
					mIsReqCanceled = true;
				}
			});
			mProgressDialog.show();
			client.getPlanPayterms((mPlans.get(selGroupId).getId())+"",getPlanPayterms);	
	}
	
	
	final Callback<List<Paytermdatum>> getPlanPayterms = new Callback<List<Paytermdatum>>() {
		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (retrofitError.isNetworkError()) {
					Toast.makeText(
							PlanActivity.this,
							getApplicationContext().getString(
									R.string.error_network), Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(
							PlanActivity.this,
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
			} else
				mIsReqCanceled = false;
		}

		@Override
		public void success(List<Paytermdatum> payterms, Response response) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (payterms != null) {
					mPayterms = payterms;
					buildPaytermList();
				}
			} else
				mIsReqCanceled = false;
		}
	};


	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
					"An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
			Toast.makeText(this,
					"An invalid Payment or PayPalConfiguration was submitted",
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
	mProgressDialog = new ProgressDialog(PlanActivity.this,
			ProgressDialog.THEME_HOLO_DARK);
	mProgressDialog.setMessage("Connecting Server...");
	mProgressDialog.setCanceledOnTouchOutside(false);
	mProgressDialog.setOnCancelListener(new OnCancelListener() {

		public void onCancel(DialogInterface arg0) {
			if (mProgressDialog.isShowing())
				mProgressDialog.dismiss();

			Toast.makeText(PlanActivity.this,
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
		Log.e("ChannelsActivity-ObsPaymentCheck",
				(e.getMessage() == null) ? "Json Exception" : e
						.getMessage());
		e.printStackTrace();
		Toast.makeText(PlanActivity.this,
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
					String mPaymentStatus = json
							.getString("paymentStatus");
					if (mPaymentStatus.equalsIgnoreCase("Success")) {
						mApplication.setBalance((float) json
								.getLong("totalBalance"));
						Toast.makeText(PlanActivity.this,
								"Payment Verification Success",
								Toast.LENGTH_LONG).show();
						//Book paln in OBS
						orderPlans(mPlans.get(selGroupId).toString());


					} else if (mPaymentStatus.equalsIgnoreCase("Fail")) {
						Toast.makeText(PlanActivity.this,
								"Payment Verification Failed",
								Toast.LENGTH_LONG).show();
					}
				}

			} catch (JSONException e) {
				Toast.makeText(PlanActivity.this,
						"Server Error", Toast.LENGTH_LONG).show();
				Log.i("VodMovieDetailsActivity",
						"JsonEXception at payment verification");
			} catch (NullPointerException e) {
				Toast.makeText(PlanActivity.this,
						"Server Error  ", Toast.LENGTH_LONG).show();
				Log.i("VodMovieDetailsActivity",
						"Null PointerEXception at payment verification");
			}
		}
	} else {
		Toast.makeText(PlanActivity.this, "Server Error",
				Toast.LENGTH_LONG).show();
	}
}
}	

	
	private void buildPaytermList(){
		
		(findViewById(R.id.a_plan_exlv)).setVisibility(View.GONE);
		(findViewById(R.id.a_plan_payterm_lv)).setVisibility(View.VISIBLE);
		
		TextView tv_title = (TextView) findViewById(R.id.a_plan_tv_selpkg);
		tv_title.setText(R.string.choose_payterm);
		
		Button btnNext = (Button) findViewById(R.id.a_plan_btn_submit);
		btnNext.setText(R.string.subscribe);
		
		
		mPaytermLv = (ListView) findViewById(R.id.a_plan_payterm_lv);
		mListAdapter = null;
		if (mPayterms != null && mPayterms.size() > 0) {
			boolean isNewPlan=true;
			mListAdapter = new PaytermAdapter(this, mPayterms,isNewPlan);
			mPaytermLv.setAdapter(mListAdapter);
			
			mPaytermLv.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
				
					// TODO Auto-generated method stub
					RadioButton rb1 = (RadioButton) view
							.findViewById(R.id.a_plan_payterm_row_rb);
					if (null != rb1 && (!rb1.isChecked())) {
						PlanActivity.selPaytermId = position;
					} else {
						PlanActivity.selPaytermId = -1;
					}
					
				}
			});
		} else {
			mPaytermLv.setAdapter(null);
			Toast.makeText(this, "No Payterms for this Plan",
					Toast.LENGTH_LONG).show();
		}		
	}
	
	public void btnCancel_onClick(View v) {
		
		if (((Button) v).getText().toString()
				.equalsIgnoreCase(getString(R.string.cancel))) {
		closeApp();
	}
		else if (((Button) v).getText().toString()
				.equalsIgnoreCase(getString(R.string.back))) {
			(findViewById(R.id.a_plan_payterm_lv)).setVisibility(View.GONE);
			(findViewById(R.id.a_plan_exlv)).setVisibility(View.VISIBLE);
			Button btnNext = (Button) findViewById(R.id.a_plan_btn_submit);
			btnNext.setText(R.string.next);
			Button btnCancel = (Button) findViewById(R.id.a_plan_btn_cancel);
			btnCancel.setText(R.string.cancel);
			PlanActivity.selPaytermId = -1;
			PlanActivity.selGroupId = -1;
			buildPlansList();
		}
	}
	private void closeApp() {
		AlertDialog mConfirmDialog = mApplication.getConfirmDialog(this);
		mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (mProgressDialog != null) {
							mProgressDialog.dismiss();
							mProgressDialog = null;
						}
						mIsReqCanceled = true;
						PlanActivity.this.finish();
					}
				});
		mConfirmDialog.show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			closeApp();
		}
		return super.onKeyDown(keyCode, event);
	}

	public void orderPlans(String planid) {
		new OrderPlansAsyncTask().execute();
	}

	private class OrderPlansAsyncTask extends
			AsyncTask<Void, Void, ResponseObj> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(PlanActivity.this,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Processing Order...");
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface arg0) {
					if (mProgressDialog.isShowing())
						mProgressDialog.dismiss();
					cancel(true);
				}
			});
			mProgressDialog.show();
		}

		@Override
		protected ResponseObj doInBackground(Void... params) {
			PlanDatum plan = mPlans.get(selGroupId);
			ResponseObj resObj = new ResponseObj();
			if (Utilities.isNetworkAvailable(getApplicationContext())) {
				HashMap<String, String> map = new HashMap<String, String>();
				Date date = new Date();
				SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy",
						new Locale("en"));
				String formattedDate = df.format(date);

				map.put("TagURL", "/orders/" + mApplication.getClientId());
				map.put("planCode", plan.getId().toString());
				map.put("dateFormat", "dd MMMM yyyy");
				map.put("locale", "en");
				map.put("contractPeriod", mPayterms.get(selPaytermId).getId()+"");
				map.put("isNewplan", "true");
				map.put("start_date", formattedDate);
				map.put("billAlign", "false");
				map.put("paytermCode", mPayterms.get(selPaytermId).getPaytermtype());

				resObj = Utilities.callExternalApiPostMethod(
						getApplicationContext(), map);
			} else {
				resObj.setFailResponse(100, NETWORK_ERROR);
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
				
				Editor prefsEditor = mApplication.getPrefs().edit();
				prefsEditor.putString(mApplication.getResources().getString(
						R.string.new_plans_data), "");
				prefsEditor.putString(mApplication.getResources().getString(
						R.string.my_plans_data), "");
				prefsEditor.commit();
				
				// update balance config n Values
				Intent intent = new Intent(PlanActivity.this,
						DoBGTasksService.class);
				intent.putExtra(DoBGTasksService.TASK_ID,
						DoBGTasks.UPDATESERVICES_CONFIGS.ordinal());
				startService(intent);

				Intent activityIntent = new Intent(PlanActivity.this,
						MainActivity.class);
				PlanActivity.this.finish();
				startActivity(activityIntent);
				//CheckBalancenGetData();
			} else {
				Toast.makeText(PlanActivity.this, resObj.getsErrorMessage(),
						Toast.LENGTH_LONG).show();
			}
		}
	}
}