package com.wawoo.service;

import java.util.Calendar;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.wawoo.data.Reminder;
import com.wawoo.database.DBHelper;
import com.wawoo.service.task.AlarmTask;

public class ScheduleAfterBootService extends Service {
	@Override
	public void onCreate() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		startBackgroundTask(intent, startId);
		return Service.START_STICKY;
	}

	private void startBackgroundTask(Intent intent, int startId) {
		backgroundExecution();
	}

	private void backgroundExecution() {
		Thread thread = new Thread(null, doBackgroundThreadProcessing,
				"Background");
		thread.start();
	}

	private Runnable doBackgroundThreadProcessing = new Runnable() {
		public void run() {
			backgroundThreadProcessing();
		}
	};

	private void backgroundThreadProcessing() {

		DBHelper dbHandler = new DBHelper(this);
		List<Reminder> remindersList = dbHandler.getAllReminder();
		Calendar c = Calendar.getInstance();
		for (Reminder r : remindersList) {
			c.setTimeInMillis(r.get_time());
			new AlarmTask(this, c, r.get_prog_name(), r.get_channel_id(),
					r.get_channel_desc(), r.get_url()).run();
		}
		dbHandler.deleteOldReminders();
	}

}