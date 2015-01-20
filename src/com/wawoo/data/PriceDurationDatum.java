package com.wawoo.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

public class PriceDurationDatum {

	@Expose
private List<Paytermdatum> paytermdata = new ArrayList<Paytermdatum>();
	@Expose
private List<Subscriptiondatum> subscriptiondata = new ArrayList<Subscriptiondatum>();

public List<Paytermdatum> getPaytermdata() {
return paytermdata;
}
public void setPaytermdata(List<Paytermdatum> paytermdata) {
this.paytermdata = paytermdata;
}

public List<Subscriptiondatum> getSubscriptiondata() {
return subscriptiondata;
}

public void setSubscriptiondata(List<Subscriptiondatum> subscriptiondata) {
this.subscriptiondata = subscriptiondata;
}

}
