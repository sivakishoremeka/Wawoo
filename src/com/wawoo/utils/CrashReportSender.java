package com.wawoo.utils;

import java.util.HashMap;

import org.acra.collector.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import com.wawoo.data.ResponseObj;

public class CrashReportSender implements ReportSender {

	public static String ClientId;
	public static String tenentId;
	public static String basicAuth;
	public static String contentType;
	public static String API_URL;

	public CrashReportSender(String url, String tId, String bAuth, String cType) {
		tenentId = tId;
		basicAuth = bAuth;
		contentType = cType;
		API_URL = url;
	}

	@Override
	public void send(CrashReportData report) throws ReportSenderException {

		if (ClientId != null && ClientId.length() > 0) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("TagURL", "/selfcare/status/" + ClientId);
			map.put("crashReportString", report.toString());
			ResponseObj resObj = Utilities.callExternalApiPutMethod(API_URL,
					tenentId, basicAuth, contentType, map);
		}
	}
}