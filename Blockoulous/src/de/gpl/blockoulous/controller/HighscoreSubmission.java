package de.gpl.blockoulous.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import de.gpl.blockoulous.activities.TetrisActivity;
import de.gpl.blockoulous.model.GameData;
import de.gpl.blockoulous.model.TetrominoType;

public class HighscoreSubmission {
	private Handler							mHandler;

	private static final String				HTTP_REQUEST_METHOD	= "POST";
	private static final String				HTTP_REQUEST_URI	= "NONE";
	private static final ProtocolVersion	HTTP_VERSION		= new ProtocolVersion("HTTP", 1, 1);

	private static final HttpHost			HTTP_HOST			= new HttpHost("NONE", 80, "http");

	private static final String				DIGEST_ALGORITHM	= "MD5";

	private static HttpRequest generateHttpRequest(Context ctx, GameData data, String name) throws MethodNotSupportedException, NoSuchAlgorithmException, UnsupportedEncodingException {

		// scores
		List<BasicNameValuePair> qparams = new ArrayList<BasicNameValuePair>();
		qparams.add(new BasicNameValuePair("stats_lines", Integer.toString(data.getmStats().getLines())));
		qparams.add(new BasicNameValuePair("stats_level", Integer.toString(data.getmStats().getLevel())));
		qparams.add(new BasicNameValuePair("stats_score", Long.toString(data.getmStats().getScore())));
		qparams.add(new BasicNameValuePair("stats_pieces", Integer.toString(data.getmStats().getTotalPieces())));

		// pieces
		TetrominoType[] types = TetrominoType.values();
		String pieces = "";
		for (int i = 0; i < types.length; i++) {
			int val = 0;
			if (data.getmStats().getPieces().containsKey(types[i])) {
				val = data.getmStats().getPieces().get(types[i]);
				pieces += Integer.toString(i) + "=" + Integer.toString(val) + ',';
			}
		}
		qparams.add(new BasicNameValuePair("stats_pieces_detail", pieces));

		// settings
		qparams.add(new BasicNameValuePair("board_width", Integer.toString(data.getBOARD_TILEMAP_WIDTH())));
		qparams.add(new BasicNameValuePair("board_height", Integer.toString(data.getBOARD_TILEMAP_HEIGHT())));

		// app version
		String app_ver = "meep";
		try {
			app_ver = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		Log.i(TetrisActivity.DEBUG_TAG, "generateHttpRequest() version=" + app_ver);
		qparams.add(new BasicNameValuePair("version", app_ver));

		// nonce
		qparams.add(new BasicNameValuePair("nonce", Long.toString(data.getNonce())));

		// name
		qparams.add(new BasicNameValuePair("name", name));

		// hashed val
		StringWriter str = new StringWriter();
		str.write(name);
		str.write(Integer.toString(data.getBOARD_TILEMAP_HEIGHT()));
		str.write(Integer.toString(data.getBOARD_TILEMAP_WIDTH()));
		str.write(Integer.toString(data.getmStats().getLevel()));
		str.write(Long.toString(data.getmStats().getScore()));
		str.write(Integer.toString(data.getmStats().getLines()));
		str.write(Integer.toString(data.getmStats().getTotalPieces()));
		str.write(Long.toString(data.getNonce()));
		str.write("SOME SALT");
		String hashSrc = str.toString();

		MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
		md.update(hashSrc.getBytes("UTF-8"));
		String hashval = byteArray2Hex(md.digest());
		qparams.add(new BasicNameValuePair("hash", hashval));

		String content = createContent(qparams);

		BasicHttpEntity entity = new BasicHttpEntity();
		entity.setContentEncoding(new BasicHeader("content-encoding", "UTF-8"));
		entity.setContentType(new BasicHeader("content-type", "application/x-www-form-urlencoded"));
		entity.setContentLength(content.length());
		InputStream in = new ByteArrayInputStream(content.getBytes("UTF-8"));
		entity.setContent(in);

		// create request
		HttpEntityEnclosingRequest req = new BasicHttpEntityEnclosingRequest(HTTP_REQUEST_METHOD, HTTP_REQUEST_URI, HTTP_VERSION);
		req.setEntity(entity);

		req.addHeader("connection", "close");
		req.addHeader("accept", "plain/text");
		req.addHeader("accept-encoding", "UTF-8");
		return req;
	}

	private static String createContent(List<BasicNameValuePair> qparams) {
		StringWriter str = new StringWriter();
		for (Iterator<BasicNameValuePair> it = qparams.iterator(); it.hasNext();) {
			BasicNameValuePair p = it.next();
			String urlencodedValue = URLEncoder.encode(p.getValue());
			str.write(p.getName());
			str.write("=");
			str.write(urlencodedValue);
			str.write("&");
		}
		return str.toString();
	}

	public void submitScore(Context ctx, GameData data, Handler mHandler, String storedName) {
		this.mHandler = mHandler;
		try {
			HttpRequest req = generateHttpRequest(ctx, data, storedName);

			HttpParams httpParameters = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is
			// established.
			// The default value is zero, that means the timeout is not used.
			int timeoutConnection = 3000;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			// Set the default socket timeout (SO_TIMEOUT)
			// in milliseconds which is the timeout for waiting for data.
			int timeoutSocket = 5000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

			DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
			HttpResponse response = httpClient.execute(HTTP_HOST, req);

			StatusLine l = response.getStatusLine();
			if (l.getStatusCode() == 200) {
				// String place =
				// convertStreamToString(response.getEntity().getContent(),
				// "utf-8");
				Message msg = HighscoreSubmission.this.mHandler.obtainMessage(TetrisActivity.HANDLER_STATUS_OK, Integer.parseInt(l.getReasonPhrase()), -1);
				msg.sendToTarget();
			} else {
				/*
				 * HttpEntity e = response.getEntity(); String encoding =
				 * "UTF-8"; if (e.getContentEncoding() != null) { encoding =
				 * e.getContentEncoding().getValue(); } String content =
				 * convertStreamToString(e.getContent(), encoding);
				 */
				Message msg = HighscoreSubmission.this.mHandler.obtainMessage(TetrisActivity.HANDLER_STATUS_FAIL);
				Bundle bundle = new Bundle();
				bundle.putString("msg", l.getStatusCode() + " " + l.getReasonPhrase());
				msg.setData(bundle);
				msg.sendToTarget();
			}
		} catch (Exception e) {
			Message msg = HighscoreSubmission.this.mHandler.obtainMessage(TetrisActivity.HANDLER_STATUS_FAIL);
			Bundle bundle = new Bundle();
			bundle.putString("msg", e.getClass().getCanonicalName() + " " + e.getMessage());
			msg.setData(bundle);
			msg.sendToTarget();
		}
	}

	/*
	 * private String convertStreamToString(InputStream is, String encoding) {
	 * try { return new java.util.Scanner(is, encoding).useDelimiter("\\A")
	 * .next(); } catch (java.util.NoSuchElementException e) { return ""; } }
	 */

	private static String byteArray2Hex(byte[] hash) {
		Formatter formatter = new Formatter();
		for (byte b : hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}
}
