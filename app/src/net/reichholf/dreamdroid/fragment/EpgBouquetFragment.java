package net.reichholf.dreamdroid.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

import net.reichholf.dreamdroid.R;
import net.reichholf.dreamdroid.activities.abs.MultiPaneHandler;
import net.reichholf.dreamdroid.adapter.EPGListAdapter;
import net.reichholf.dreamdroid.fragment.abs.AbstractHttpEventListFragment;
import net.reichholf.dreamdroid.helpers.ExtendedHashMap;
import net.reichholf.dreamdroid.helpers.Statics;
import net.reichholf.dreamdroid.helpers.enigma2.Event;
import net.reichholf.dreamdroid.helpers.enigma2.Service;
import net.reichholf.dreamdroid.helpers.enigma2.URIStore;
import net.reichholf.dreamdroid.helpers.enigma2.requesthandler.EventListRequestHandler;
import net.reichholf.dreamdroid.loader.AsyncListLoader;
import net.reichholf.dreamdroid.loader.LoaderResult;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Stephan on 01.11.2014.
 */
public class EpgBouquetFragment extends AbstractHttpEventListFragment implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
	private static final String LOG_TAG = EpgBouquetFragment.class.getSimpleName();
	private TextView mDateView;
	private TextView mTimeView;
	private int mTime;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mCardListStyle = true;
		mEnableReload = true;
		super.onCreate(savedInstanceState);
		initTitle(getString(R.string.epg));

		mReference = getArguments().getString(Event.KEY_SERVICE_REFERENCE);
		mName = getArguments().getString(Event.KEY_SERVICE_NAME);
		mTime = (int) (Calendar.getInstance().getTimeInMillis() / 1000);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.card_list_header_content, container, false);
		View header = inflater.inflate(R.layout.date_time_picker_header, null, false);

		Calendar cal = getCalendar();
		SimpleDateFormat today = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat now = new SimpleDateFormat("HH:mm");

		mDateView = (TextView) header.findViewById(R.id.textViewDate);
		mDateView.setText(today.format(cal.getTime()));
		mDateView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Calendar calendar = getCalendar();
				final DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(EpgBouquetFragment.this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), true);
				getMultiPaneHandler().showDialogFragment(datePickerDialog, "epg_bouquet_date_picker");
			}
		});

		mTimeView = (TextView) header.findViewById(R.id.textViewTime);
		mTimeView.setText(now.format(cal.getTime()));
		mTimeView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Calendar calendar = getCalendar();
				final TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(EpgBouquetFragment.this, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true, true);
				getMultiPaneHandler().showDialogFragment(timePickerDialog, "epg_bouquet_time_picker");
			}
		});

		FrameLayout frame = (FrameLayout) view.findViewById(R.id.content_header);
		frame.addView(header);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (mReference != null) {
			setAdapter();
			if (mMapList.size() <= 0)
				Log.w(LOG_TAG, String.format("%s", mTime));
			reload();
		} else {
			pickBouquet();
		}
	}

	@Override
	public void createOptionsMenu(Menu menu, MenuInflater inflater) {
		checkMenuReload(menu, inflater);
		inflater.inflate(R.menu.epgbouquet, menu);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK)
			return;
		switch (requestCode) {
			case Statics.REQUEST_PICK_BOUQUET:
				ExtendedHashMap service = data.getParcelableExtra(PickServiceFragment.KEY_BOUQUET);
				String reference = service.getString(Service.KEY_REFERENCE);
				if (!mReference.equals(reference)) {
					mReference = reference;
					mName = service.getString(Service.KEY_NAME);
					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1)
						getListView().smoothScrollToPosition(0);
				}
				reload();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void reload() {
		if(mReference != null)
			super.reload();
		else
			pickBouquet();
	}

	/**
	 * Initializes the <code>SimpleListAdapter</code>
	 */
	private void setAdapter() {
		mAdapter = new EPGListAdapter(getActionBarActivity(), mMapList, R.layout.epg_multi_service_list_item, new String[]{
				Event.KEY_EVENT_TITLE, Event.KEY_SERVICE_NAME, Event.KEY_EVENT_DESCRIPTION_EXTENDED, Event.KEY_EVENT_START_READABLE,
				Event.KEY_EVENT_DURATION_READABLE}, new int[]{R.id.event_title, R.id.service_name, R.id.event_short, R.id.event_start,
				R.id.event_duration});
		setListAdapter(mAdapter);
	}

	@Override
	public ArrayList<NameValuePair> getHttpParams(int loader) {
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("bRef", mReference));
		params.add(new BasicNameValuePair("time", Integer.toString(mTime)));
		return params;
	}

	@Override
	public String getLoadFinishedTitle() {
		return mName;
	}

	@Override
	public Loader<LoaderResult<ArrayList<ExtendedHashMap>>> onCreateLoader(int id, Bundle args) {
		AsyncListLoader loader = new AsyncListLoader(getActionBarActivity(), new EventListRequestHandler(URIStore.EPG_BOUQUET), false, args);
		return loader;
	}

	@Override
	protected boolean onItemSelected(int id) {
		switch (id) {
			case R.id.menu_pick_bouquet:
				pickBouquet();
				return true;
		}
		return super.onItemSelected(id);
	}

	private void pickBouquet() {
		PickServiceFragment f = new PickServiceFragment();
		Bundle args = new Bundle();

		ExtendedHashMap data = new ExtendedHashMap();
		data.put(Service.KEY_REFERENCE, "default");

		args.putSerializable(sData, data);
		args.putString("action", Statics.INTENT_ACTION_PICK_BOUQUET);

		f.setArguments(args);
		f.setTargetFragment(this, Statics.REQUEST_PICK_BOUQUET);
		((MultiPaneHandler) getActionBarActivity()).showDetails(f, true);
	}

	private Calendar getCalendar() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis((long) mTime * 1000);
		return cal;
	}

	@Override
	public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
		Calendar cal = getCalendar();
		if(cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DATE) == day)
			return;
		cal.set(year, month, day);

		SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
		mDateView.setText(dayFormat.format(cal.getTime()));
		mTime = (int) (cal.getTimeInMillis() / 1000);
		Log.i(LOG_TAG, String.format("%s", mTime));
		reload();
	}

	@Override
	public void onTimeSet(RadialPickerLayout radialPickerLayout, int hourOfDay, int minute) {
		Calendar cal = getCalendar();
		if(cal.get(Calendar.HOUR_OF_DAY) == hourOfDay && cal.get(Calendar.MINUTE) == minute)
			return;
		cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, 0);

		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
		mTimeView.setText(timeFormat.format(cal.getTime()));
		mTime = (int) (cal.getTimeInMillis() / 1000);
		Log.i(LOG_TAG, String.format("%s", mTime));
		reload();
	}


}
