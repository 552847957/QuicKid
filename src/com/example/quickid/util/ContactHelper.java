package com.example.quickid.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.example.quickid.AppApplication;
import com.example.quickid.adapter.ContactAdapter.ContactComparator;
import com.example.quickid.model.Contact;
import com.example.quickid.model.RecentContact;

import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;

public class ContactHelper {

	public static ArrayList<String> getPossibleKeys(String key) {
		ArrayList<String> list = new ArrayList<String>();
		if (key.length() > 0) {
			if (key.contains("1") || key.contains("0")) {
				list.add(key);
			} else {
				int keyLen = key.length();
				String[] words;
				if (keyLen == 1) {
					words = AppApplication.keyMaps.get(key.charAt(0));
					for (int i = 0; i < words.length; i++) {
						list.add(words[i]);
					}
				} else {
					ArrayList<String> sonList = getPossibleKeys(key.substring(
							0, key.length() - 1));
					words = AppApplication.keyMaps
							.get(key.charAt(key.length() - 1));
					for (int i = 0; i < words.length; i++) {
						for (Iterator<String> iterator = sonList.iterator(); iterator
								.hasNext();) {
							String sonStr = iterator.next();
							list.add(sonStr + words[i]);
						}
					}
				}
			}
		}
		return list;
	}

	/**
	 * 加载所有联系人
	 */
	synchronized public static void loadContacts() {
		ArrayList<Contact> AllContacts = new ArrayList<Contact>();
		ContentResolver resolver = AppApplication.globalApplication
				.getContentResolver();
		// 要使用RawContacts.CONTACT_ID而不是Contacts.CONTACT_ID
		String[] PROJECTION = { RawContacts.CONTACT_ID,
				Contacts.DISPLAY_NAME_PRIMARY, Contacts.LOOKUP_KEY,
				Contacts.PHOTO_THUMBNAIL_URI, Phone.NUMBER, Phone.TYPE,
				Contacts.STARRED };
		Cursor cursor = resolver.query(Phone.CONTENT_URI, PROJECTION, null,
				null, Contacts.SORT_KEY_PRIMARY);
		String preLookupKey = "";
		Contact preContact = null;
		if (cursor.moveToFirst()) {
			do {
				long contractID = cursor.getInt(0);
				String displayName = cursor.getString(1);
				String lookupKey = cursor.getString(2);
				String photoUri = cursor.getString(3);
				boolean starred = cursor.getInt(6) == 1;
				if (lookupKey.equals(preLookupKey) && preContact != null) {
					preContact.addPhone(cursor.getString(4), cursor.getInt(5));
				} else {
					Contact contact = new Contact();
					contact.setContactId(contractID);
					contact.setName(displayName);
					contact.setLookupKey(lookupKey);
					contact.setPhotoUri(photoUri);
					contact.addPhone(cursor.getString(4), cursor.getInt(5));
					contact.setStarred(starred);
					AllContacts.add(contact);
					preLookupKey = lookupKey;
					preContact = contact;
				}
			} while (cursor.moveToNext());
		} else {
			// No Phone Number Found
		}
		cursor.close();

		AppApplication.AllContacts = AllContacts;
		// TODO notify
		Intent intent = new Intent();
		intent.setAction(Consts.Action_All_Contacts_Changed);
		AppApplication.globalApplication.sendBroadcast(intent);
	}

	/**
	 * 加载通话记录
	 */
	synchronized public static void loadCallLogs() {
		ArrayList<RecentContact> AllRecentContacts = new ArrayList<RecentContact>();
		String[] projection = { Calls._ID, Calls.TYPE, Calls.CACHED_NAME,
				Calls.CACHED_NUMBER_TYPE, Calls.DATE, Calls.DURATION,
				Calls.NUMBER };
		ContentResolver resolver = AppApplication.globalApplication
				.getContentResolver();
		Cursor cursor = resolver.query(Calls.CONTENT_URI, projection, null,
				null, null);
		while (cursor.moveToNext()) {
			RecentContact contact = new RecentContact();

			long contractID = cursor.getInt(0);
			int callType = cursor.getInt(1);
			String name = cursor.getString(2);
			int numberType = cursor.getInt(3);
			long date = cursor.getLong(4);
			int duration = cursor.getInt(5);
			String number = cursor.getString(6);

			contact.setContractID(contractID);
			contact.setCallType(callType);
			contact.setDate(date);
			contact.setDuration(duration);
			contact.setName(name);
			contact.setNumber(number);
			contact.setNumberType(numberType);
			AllRecentContacts.add(contact);
		}
		cursor.close();
		AppApplication.AllRecentContacts = AllRecentContacts;
		// TODO notify
		Intent intent = new Intent();
		intent.setAction(Consts.Action_All_Contacts_Changed);
		AppApplication.globalApplication.sendBroadcast(intent);
	}

	public static void removeCallLog(long call_ID) {
		ContentResolver resolver = AppApplication.globalApplication
				.getContentResolver();
		if (resolver.delete(Contacts.CONTENT_STREQUENT_URI, Calls._ID,
				new String[] { String.valueOf(call_ID) }) > 0) {
			// delete ok
		}
	}

	public static void clearCallLogs() {
		ContentResolver resolver = AppApplication.globalApplication
				.getContentResolver();
		if (resolver.delete(Calls.CONTENT_URI, null, null) > 0) {
			// delete ok
		}
	}

	/**
	 * 加载最近联系人（和收藏联系人）
	 */
	public static void loadStrequent() {
		ArrayList<Contact> StrequentContacts = new ArrayList<Contact>();
		String[] projection = { Contacts._ID, Contacts.DISPLAY_NAME,
				Contacts.LOOKUP_KEY, Contacts.PHOTO_THUMBNAIL_URI,
				Contacts.TIMES_CONTACTED, Contacts.LAST_TIME_CONTACTED,
				Contacts.STARRED };
		ContentResolver resolver = AppApplication.globalApplication
				.getContentResolver();
		// 显示最近联系人和收藏的联系人
		Cursor cursor = resolver.query(Contacts.CONTENT_STREQUENT_URI,
				projection, null, null, null);
		while (cursor.moveToNext()) {
			Contact contact = new Contact();
			long contractID = cursor.getInt(0);
			String displayName = cursor.getString(1);
			String lookupKey = cursor.getString(2);
			String photoUri = cursor.getString(3);
			int TIMES_CONTACTED = cursor.getInt(4);
			long LAST_TIME_CONTACTED = cursor.getLong(5);
			boolean starred = cursor.getInt(6) == 1;
			contact.setContactId(contractID);
			contact.setName(displayName);
			contact.setLookupKey(lookupKey);
			contact.setPhotoUri(photoUri);
			contact.setStarred(starred);
			contact.TIMES_CONTACTED = TIMES_CONTACTED;
			contact.LAST_TIME_CONTACTED = LAST_TIME_CONTACTED;
			StrequentContacts.add(contact);
		}
		cursor.close();
		AppApplication.StrequentContacts = StrequentContacts;
		// notify
	}

	public static void removeStrequent(long contact_ID) {
		ContentResolver resolver = AppApplication.globalApplication
				.getContentResolver();
		if (resolver.delete(Contacts.CONTENT_STREQUENT_URI, Contacts._ID,
				new String[] { String.valueOf(contact_ID) }) > 0) {
			// delete ok
		}
	}

	public static void clearStrequent() {
		ContentResolver resolver = AppApplication.globalApplication
				.getContentResolver();
		if (resolver.delete(Contacts.CONTENT_STREQUENT_URI, null, null) > 0) {
			// delete ok
		}
	}

	/**
	 * 加载最近联系人（和收藏联系人）
	 */
	public static void loadFrequent() {
		ArrayList<Contact> FrequentContacts = new ArrayList<Contact>();
		String[] projection = { Contacts._ID, Contacts.DISPLAY_NAME,
				Contacts.LOOKUP_KEY, Contacts.PHOTO_THUMBNAIL_URI,
				Contacts.TIMES_CONTACTED, Contacts.LAST_TIME_CONTACTED,
				Contacts.STARRED };
		ContentResolver resolver = AppApplication.globalApplication
				.getContentResolver();
		// 显示最近联系人，不知为何不能排序，只能按通讯次数排序
		Cursor cursor = resolver.query(
				Uri.withAppendedPath(Contacts.CONTENT_URI, "frequent"),
				projection, null, null, null);
		while (cursor.moveToNext()) {
			Contact contact = new Contact();
			long contractID = cursor.getInt(0);
			String displayName = cursor.getString(1);
			String lookupKey = cursor.getString(2);
			String photoUri = cursor.getString(3);
			int TIMES_CONTACTED = cursor.getInt(4);
			long LAST_TIME_CONTACTED = cursor.getLong(5);
			boolean starred = cursor.getInt(6) == 1;
			contact.setContactId(contractID);
			contact.setName(displayName);
			contact.setLookupKey(lookupKey);
			contact.setPhotoUri(photoUri);
			contact.setStarred(starred);
			contact.TIMES_CONTACTED = TIMES_CONTACTED;
			contact.LAST_TIME_CONTACTED = LAST_TIME_CONTACTED;
			FrequentContacts.add(contact);
		}
		cursor.close();
		sortContactByLAST_TIME_CONTACTED(FrequentContacts);
		AppApplication.StrequentContacts = FrequentContacts;
		// notify
	}

	public static void removeFrequent(long contact_ID) {
		ContentResolver resolver = AppApplication.globalApplication
				.getContentResolver();
		if (resolver.delete(
				Uri.withAppendedPath(Contacts.CONTENT_URI, "frequent"),
				Contacts._ID, new String[] { String.valueOf(contact_ID) }) > 0) {
			// delete ok
		}
	}

	public static void clearFrequent() {
		ContentResolver resolver = AppApplication.globalApplication
				.getContentResolver();
		if (resolver.delete(
				Uri.withAppendedPath(Contacts.CONTENT_URI, "frequent"), null,
				null) > 0) {
			// delete ok
		}
	}

	public static void sortContactByLAST_TIME_CONTACTED(ArrayList<Contact> lis) {
		ContactComparator comparator = new ContactComparator();
		Collections.sort(lis, comparator);
	}

	public static class ContactComparator implements Comparator<Contact> {

		@Override
		public int compare(Contact lhs, Contact rhs) {

			// 如果同是文件夹或者文件，则按名称排序
			if (lhs.LAST_TIME_CONTACTED > rhs.LAST_TIME_CONTACTED) {
				return -1;
			} else if (lhs.LAST_TIME_CONTACTED == rhs.LAST_TIME_CONTACTED) {
				return 0;
			} else {
				return 1;
			}
		}
	}

	/**
	 * 根据电话号码寻出联系人
	 * 
	 * @param contactNumber
	 * @return
	 */
	public static Contact getContactByPhoneNumber(String contactNumber) {
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(contactNumber));
		return lookupContactFromUri(uri);
	}

	public static Contact lookupContactFromUri(Uri uri) {
		final Contact info;
		Cursor phonesCursor = AppApplication.globalApplication
				.getContentResolver().query(uri, PhoneQuery._PROJECTION, null,
						null, null);

		if (phonesCursor != null) {
			try {
				if (phonesCursor.moveToFirst()) {
					info = new Contact();
					long contactId = phonesCursor.getLong(PhoneQuery.PERSON_ID);
					String lookupKey = phonesCursor
							.getString(PhoneQuery.LOOKUP_KEY);
					info.setLookupKey(lookupKey);
					info.setLookupUri(Contacts.getLookupUri(contactId,
							lookupKey));
					info.setName(phonesCursor.getString(PhoneQuery.NAME));

					info.type = phonesCursor.getInt(PhoneQuery.PHONE_TYPE);
					info.label = phonesCursor.getString(PhoneQuery.LABEL);
					info.number = phonesCursor
							.getString(PhoneQuery.MATCHED_NUMBER);
					info.normalizedNumber = phonesCursor
							.getString(PhoneQuery.NORMALIZED_NUMBER);
					info.photoId = phonesCursor.getLong(PhoneQuery.PHOTO_ID);
					info.setPhotoUri(phonesCursor
							.getString(PhoneQuery.PHOTO_URI));
					info.formattedNumber = null;
				} else {
					info = new Contact();
				}
			} finally {
				phonesCursor.close();
			}
		} else {
			info = null;
		}
		return info;
	}

	final static class PhoneQuery {
		public static final String[] _PROJECTION = new String[] {
				PhoneLookup._ID, PhoneLookup.DISPLAY_NAME, PhoneLookup.TYPE,
				PhoneLookup.LABEL, PhoneLookup.NUMBER,
				PhoneLookup.NORMALIZED_NUMBER, PhoneLookup.PHOTO_ID,
				PhoneLookup.LOOKUP_KEY, PhoneLookup.PHOTO_URI };

		public static final int PERSON_ID = 0;
		public static final int NAME = 1;
		public static final int PHONE_TYPE = 2;
		public static final int LABEL = 3;
		public static final int MATCHED_NUMBER = 4;
		public static final int NORMALIZED_NUMBER = 5;
		public static final int PHOTO_ID = 6;
		public static final int LOOKUP_KEY = 7;
		public static final int PHOTO_URI = 8;
	}

	private static Vibrator vibrator;

	public static void vibrate(long duaration) {
		if (vibrator == null) {
			vibrator = (Vibrator) AppApplication.globalApplication
					.getSystemService(Context.VIBRATOR_SERVICE);
		}
		vibrator.vibrate(duaration);
	}

}
