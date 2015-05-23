package com.wawoo.paypal;

import java.math.BigDecimal;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.wawoo.data.ResponseObj;
import com.wawoo.mobile.MyApplication;
import com.wawoo.mobile.R;
import com.wawoo.utils.Utilities;

public class PaypalHelper {

	public interface UpdatePaymentTaskListener {
		public String getTag();

		public void onSuccess();

		public void onFailure();
	}

	private Context mContext;
	private MyApplication mApplication;

	public PaypalHelper(Context context, MyApplication application) {
		mContext = context;
		mApplication = application;
	}

	public void startPaypalActivity() {
		Intent svcIntent = new Intent(mContext, PayPalService.class);
		svcIntent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION,
				mApplication.getPaypalConfig());
		mContext.startService(svcIntent);
		PayPalPayment paymentData = new PayPalPayment(new BigDecimal(
				mApplication.getBalance()), "USD",//mApplication.getCurrency(),
				mContext.getResources().getString(R.string.app_name)
						+ " -Payment", PayPalPayment.PAYMENT_INTENT_SALE);
		Intent actviIntent = new Intent(mContext, PaymentActivity.class);
		actviIntent.putExtra(PaymentActivity.EXTRA_PAYMENT, paymentData);
		((Activity) mContext).startActivityForResult(actviIntent,
				MyApplication.REQUEST_CODE_PAYMENT);
	}

	public void updatePaymentInOBS(String confirmation) {
		OBSPaymentAsyncTask task = new OBSPaymentAsyncTask();
		task.execute(confirmation);
	}

	private class OBSPaymentAsyncTask extends
			AsyncTask<String, Void, ResponseObj> {
		JSONObject reqJson = null;
		ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = new ProgressDialog(mContext,
					ProgressDialog.THEME_HOLO_DARK);
			progressDialog.setMessage("Connecting Server...");
			progressDialog.setCanceledOnTouchOutside(false);
			progressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface arg0) {
					if (progressDialog.isShowing())
						progressDialog.dismiss();
					Toast.makeText(mContext, "Payment verification Failed.",
							Toast.LENGTH_LONG).show();
					cancel(true);
				}
			});
			progressDialog.show();
		}

		@Override
		protected ResponseObj doInBackground(String... arg) {
			ResponseObj resObj = new ResponseObj();
			try {
				reqJson = new JSONObject(arg[0]);
				if (mApplication.isNetworkAvailable()) {
					resObj = Utilities.callExternalApiPostMethod(
							mApplication,
							"/payments/paypalEnquirey/"
									+ mApplication.getClientId(), reqJson);
				} else {
					resObj.setFailResponse(100, "Network error.");
				}
			} catch (JSONException e) {
				Log.e(((UpdatePaymentTaskListener) mContext).getTag()
						+ "- ObsPaymentCheck",
						(e.getMessage() == null) ? "Json Exception" : e
								.getMessage());
				e.printStackTrace();
				Toast.makeText(mContext, "Invalid data: On PayPal Payment ",
						Toast.LENGTH_LONG).show();
			}
			return resObj;
		}

		@Override
		protected void onPostExecute(ResponseObj resObj) {

			super.onPostExecute(resObj);
			if (progressDialog.isShowing()) {
				progressDialog.dismiss();
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
								Toast.makeText(mContext,
										"Payment Verification Success",
										Toast.LENGTH_LONG).show();
								((UpdatePaymentTaskListener) mContext)
										.onSuccess();

							} else if (mPaymentStatus.equalsIgnoreCase("Fail")) {
								Toast.makeText(mContext,
										"Payment Verification Failed",
										Toast.LENGTH_LONG).show();
							}
						}

					} catch (JSONException e) {
						Toast.makeText(mContext, "Server Error",
								Toast.LENGTH_LONG).show();
						Log.i(((UpdatePaymentTaskListener) mContext).getTag(),
								"JsonEXception at payment verification");
					} catch (NullPointerException e) {
						Toast.makeText(mContext, "Server Error  ",
								Toast.LENGTH_LONG).show();
						Log.i(((UpdatePaymentTaskListener) mContext).getTag(),
								"Null PointerEXception at payment verification");
					}
				}
			} else {
				Toast.makeText(mContext, "Server Error", Toast.LENGTH_LONG)
						.show();
			}
		}
	}

}
