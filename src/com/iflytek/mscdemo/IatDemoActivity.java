package com.iflytek.mscdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.speech.DataDownloader;
import com.iflytek.cloud.speech.DataUploader;
import com.iflytek.cloud.speech.RecognizerListener;
import com.iflytek.cloud.speech.RecognizerResult;
import com.iflytek.cloud.speech.SpeechConstant;
import com.iflytek.cloud.speech.SpeechError;
import com.iflytek.cloud.speech.SpeechListener;
import com.iflytek.cloud.speech.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.ContactManager;
import com.iflytek.cloud.util.ContactManager.ContactListener;
import com.iflytek.cloud.util.UserWords;
import com.iflytek.mscdemo.util.JsonParser;

/**
 * 听写页面,通过调用SDK中提供的RecognizerDialog来实现听写功能.
 * 
 * @author iFlytek
 * @since 20120822
 */
public class IatDemoActivity extends Activity implements OnClickListener {
	// 日志TAG.
	private static final String TAG = "IatDemoActivity";
	// Tip
	private Toast mToast;
	// title的文本内容.
	private TextView mCategoryText;
	// 识别结果显示
	private EditText mResultText;
	// 缓存，保存当前的引擎参数到下一次启动应用程序使用.
	private SharedPreferences mSharedPreferences;
	// 识别窗口
	private RecognizerDialog iatDialog;
	// 识别对象
	private SpeechRecognizer iatRecognizer;
	// 上传
	private DataUploader dataUploader;
	// 下载用户词表
	private DataDownloader dataDownloader;
	// 用户词表下载结果
	private String mDownloadResult = "";

	/**
	 * 页面初始化入口函数.
	 * 
	 * @param savedInstanceState
	 */
	@SuppressLint("ShowToast")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "[onCreate]" + savedInstanceState);
		// 用户自定义窗体显示状态
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.demo);
		// 设置自定义标题类型
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.titlebar);
		((TextView) findViewById(R.id.titlebar_text))
				.setText(R.string.text_iat_demo);

		mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

		// 听写的说明描述
		mCategoryText = (TextView) findViewById(R.id.categoty);
		mCategoryText.setText(R.string.text_iat_demo);
		mCategoryText.setVisibility(View.VISIBLE);

		// 创建数据上传实例
		dataUploader = new DataUploader(this);
		// 创建数据下载实例
		dataDownloader = new DataDownloader(this);
		// 初始化缓存对象.
		mSharedPreferences = getSharedPreferences(getPackageName(),
				MODE_PRIVATE);
		// 将“讯飞语音 沟通无限”提示隐藏
		findViewById(R.id.txt_hint).setVisibility(View.GONE);

		// “设置”按钮初始化
		ImageButton settingButton = (ImageButton) findViewById(R.id.tts_setting_btn);
		settingButton.setOnClickListener(this);

		// “识别”按钮初始化
		Button iatButton = (Button) findViewById(android.R.id.button1);
		iatButton.setOnClickListener(this);
		iatButton.setText(R.string.text_iat);

		// “取消”按钮初始化
		Button cancelButton = (Button) findViewById(android.R.id.button2);
		cancelButton.setOnClickListener(this);
		cancelButton.setText(R.string.text_iat_cancel);

		setCacelButtonVisible();

		// "上传联系人"、“上传词表”、“下载词表”按钮初始化
		Button contactsUploadBtn = (Button) findViewById(R.id.upload_contacts);
		Button UserwordsUploadBtn = (Button) findViewById(R.id.upload_userwords);
		Button UserwordsDownloadBtn = (Button) findViewById(R.id.download_userwords);
		contactsUploadBtn.setOnClickListener(this);
		UserwordsUploadBtn.setOnClickListener(this);
		UserwordsDownloadBtn.setOnClickListener(this);

		// 听写结果设置
		mResultText = (EditText) findViewById(R.id.txt_result);
		findViewById(R.id.personal_layout).setVisibility(View.VISIBLE);
		findViewById(R.id.linear_userwords).setVisibility(View.VISIBLE);

		// 创建听写对象,如果只使用无UI听写功能,不需要创建RecognizerDialog
		iatRecognizer = SpeechRecognizer.createRecognizer(this);
		// 初始化听写Dialog,如果只使用有UI听写功能,无需创建SpeechRecognizer
		iatDialog = new RecognizerDialog(this);
	}

	/**
	 * 重写Activity的onStart方法
	 * 
	 * @param
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// 获取之前保存的引擎参数，若没有使用默认的参数iat.
		String engine = mSharedPreferences.getString(
				getString(R.string.preference_key_iat_engine),
				getString(R.string.preference_default_iat_engine));
		String[] engineEntries = getResources().getStringArray(
				R.array.preference_entries_iat_engine);
		String[] engineValues = getResources().getStringArray(
				R.array.preference_values_iat_engine);
		for (int i = 0; i < engineValues.length; i++) {
			if (engineValues[i].equals(engine)) {
				mCategoryText.setText(engineEntries[i]);
				break;
			}
		}
	}

	@Override
	protected void onStop() {
		mToast.cancel();
		if (null != iatRecognizer) {
			iatRecognizer.cancel();
		}
		if (null != iatDialog) {
			iatDialog.cancel();
		}
		super.onStop();
	}

	/**
	 * 按钮点击事件.
	 * 
	 * @param v
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		// 听写按钮
		case android.R.id.button1:
			boolean isShowDialog = mSharedPreferences.getBoolean(
					getString(R.string.preference_key_iat_show), true);
			if (isShowDialog) {
				// 显示语音听写Dialog.
				showIatDialog();
			} else {
				if (null == iatRecognizer) {
					iatRecognizer = SpeechRecognizer.createRecognizer(this);
				}
				if (iatRecognizer.isListening()) {
					iatRecognizer.stopListening();
					showTip("停止录音");
					((Button) findViewById(android.R.id.button1))
							.setEnabled(false);
				} else {
					// 不显示Dialog.
					showIatInvisble();
					((Button) findViewById(android.R.id.button1))
							.setText(IatDemoActivity.this
									.getString(R.string.text_iat_stop));
				}
			}
			break;
		// 取消按钮
		case android.R.id.button2:
			cancelRecognize();
			showTip("取消识别");
			break;
		// 设置按钮
		case R.id.tts_setting_btn:
			cancelRecognize();
			Intent intent = new Intent(this, IatPreferenceActivity.class);
			startActivityForResult(intent, 0);
			break;
		// 上传联系人
		case R.id.upload_contacts:
			Toast.makeText(IatDemoActivity.this,
					getString(R.string.text_upload_contacts),
					Toast.LENGTH_SHORT).show();
			ContactManager mgr = ContactManager.createManager(
					IatDemoActivity.this, mContactListener);
			mgr.asyncQueryAllContactsName();
			break;
		// 上传用户词表
		case R.id.upload_userwords:
			try {
				/*
				 * 获取用户词表信息 数据格式为：JSON格式
				 */
				Toast.makeText(IatDemoActivity.this,
						getString(R.string.text_upload_userwords),
						Toast.LENGTH_SHORT).show();
				String contents = readStringFromInputStream(getAssets().open(
						"userwords"));
				UserWords userwords = new UserWords(contents);
				// 用户词表信息需转为utf-8格式的二进制数组
				byte[] datas = userwords.toString().getBytes("utf-8");
				dataUploader.setParameter(SpeechConstant.SUBJECT, "uup");
				dataUploader.setParameter(SpeechConstant.DATA_TYPE, "userword");
				dataUploader.uploadData(uploadListener, "userwords", datas);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		// 下载用户词表
		case R.id.download_userwords:
			mResultText.setText(null);
			// 清空下载内容.
			dataDownloader.setParameter(SpeechConstant.SUBJECT, "spp");
			dataDownloader.setParameter(SpeechConstant.DATA_TYPE, "userword");
			dataDownloader.downloadData(downloadlistener);
			break;
		default:
			break;
		}
	}

	protected void cancelRecognize() {
		if (null != iatRecognizer) {
			iatRecognizer.cancel();
		}
		((Button) findViewById(android.R.id.button1))
				.setText(IatDemoActivity.this.getString(R.string.text_iat));
		((Button) findViewById(android.R.id.button1)).setEnabled(true);
	}

	protected ContactListener mContactListener = new ContactListener() {

		@Override
		public void onContactQueryFinish(String contactInfos, boolean changeFlag) {
			// 注：实际应用中除第一次上传之外，之后应该通过changeFlag判断是否需要上传，否则会造成不必要的流量.
			// if(changeFlag) {
			try {
				// 联系人信息需转成utf-8格式的二进制数组
				byte[] datas = ContactManager.getManager()
						.queryAllContactsName().getBytes("utf-8");
				dataUploader.setParameter(SpeechConstant.SUBJECT, "uup");
				dataUploader.setParameter(SpeechConstant.DATA_TYPE, "contact");
				dataUploader.uploadData(uploadListener, "contacts", datas);
			} catch (Exception e) {
				e.printStackTrace();
			}
			// }

		}

	};

	/**
	 * 不显示听写对话框
	 */
	public void showIatInvisble() {
		if (null == iatRecognizer) {
			iatRecognizer = SpeechRecognizer.createRecognizer(this);
		}
		// 获取引擎参数
		String engine = mSharedPreferences.getString(
				getString(R.string.preference_key_iat_engine),
				getString(R.string.preference_default_iat_engine));

		// 清空Grammar_ID，防止识别后进行听写时Grammar_ID的干扰
		iatRecognizer.setParameter(SpeechConstant.CLOUD_GRAMMAR, null);
		// 设置听写引擎
		iatRecognizer.setParameter(SpeechConstant.DOMAIN, engine);
		// 设置采样率参数，支持8K和16K
		String rate = mSharedPreferences.getString(
				getString(R.string.preference_key_iat_rate),
				getString(R.string.preference_default_iat_rate));
		if (rate.equals("rate8k")) {
			iatRecognizer.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
		} else {
			iatRecognizer.setParameter(SpeechConstant.SAMPLE_RATE, "16000");
		}

		// 当DOMAIN设置为POI搜索时,获取area参数
		if (IatPreferenceActivity.ENGINE_POI.equals(engine)) {
			String province = mSharedPreferences.getString(
					getString(R.string.preference_key_poi_province),
					getString(R.string.preference_default_poi_province));
			String city = mSharedPreferences.getString(
					getString(R.string.preference_key_poi_city),
					getString(R.string.preference_default_poi_city));

			iatRecognizer.setParameter(SpeechConstant.SEARCH_AREA, province
					+ city);
		}

		// 清空结果显示框.
		mResultText.setText(null);
		// 显示听写对话框
		iatRecognizer.startListening(recognizerListener);
		showTip(getString(R.string.text_iat_begin));
	}

	/**
	 * 显示听写对话框.
	 * 
	 * @param
	 */
	public void showIatDialog() {
		if (null == iatDialog) {
			// 初始化听写Dialog
			iatDialog = new RecognizerDialog(this);
		}

		// 获取引擎参数
		String engine = mSharedPreferences.getString(
				getString(R.string.preference_key_iat_engine),
				getString(R.string.preference_default_iat_engine));

		// 清空Grammar_ID，防止识别后进行听写时Grammar_ID的干扰
		iatDialog.setParameter(SpeechConstant.CLOUD_GRAMMAR, null);
		// 设置听写Dialog的引擎
		iatDialog.setParameter(SpeechConstant.DOMAIN, engine);
		// 设置采样率参数，支持8K和16K
		String rate = mSharedPreferences.getString(
				getString(R.string.preference_key_iat_rate),
				getString(R.string.preference_default_iat_rate));
		if (rate.equals("rate8k")) {
			iatDialog.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
		} else {
			iatDialog.setParameter(SpeechConstant.SAMPLE_RATE, "16000");
		}

		// 当DOMAIN设置为POI搜索时,获取area参数
		if (IatPreferenceActivity.ENGINE_POI.equals(engine)) {
			String province = mSharedPreferences.getString(
					getString(R.string.preference_key_poi_province),
					getString(R.string.preference_default_poi_province));
			String city = mSharedPreferences.getString(
					getString(R.string.preference_key_poi_city),
					getString(R.string.preference_default_poi_city));

			iatDialog.setParameter(SpeechConstant.SEARCH_AREA, province + city);
		}

		// 清空结果显示框.
		mResultText.setText(null);
		// 显示听写对话框
		iatDialog.setListener(recognizerDialogListener);
		iatDialog.show();
		showTip(getString(R.string.text_iat_begin));
	}

	RecognizerListener recognizerListener = new RecognizerListener() {

		@Override
		public void onBeginOfSpeech() {
			showTip("开始说话");
		}

		@Override
		public void onError(SpeechError err) {
			showTip(err.getPlainDescription(true));
			((Button) findViewById(android.R.id.button1))
					.setText(IatDemoActivity.this.getString(R.string.text_iat));
			((Button) findViewById(android.R.id.button1)).setEnabled(true);
		}

		@Override
		public void onEndOfSpeech() {
			showTip("结束说话");
			
			
			showTip("Please wait, connecting to server.");

			// upload the result to cooby
			String cmd = mResultText.getText().toString();



			String resp = CoobyUtils.callCooby(cmd);

			showTip(resp);
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, String msg) {

		}

		@Override
		public void onResult(RecognizerResult results, boolean isLast) {
			String text = JsonParser.parseIatResult(results.getResultString());
			mResultText.append(text);
			mResultText.setSelection(mResultText.length());
			if (isLast) {
				((Button) findViewById(android.R.id.button1))
						.setText(IatDemoActivity.this
								.getString(R.string.text_iat));
				((Button) findViewById(android.R.id.button1)).setEnabled(true);
			}
		}

		@Override
		public void onVolumeChanged(int volume) {
			showTip("当前正在说话，音量大小：" + volume);
		}

	};
	/**
	 * 识别回调监听器
	 */
	RecognizerDialogListener recognizerDialogListener = new RecognizerDialogListener() {
		@Override
		public void onResult(RecognizerResult results, boolean isLast) {
			String text = JsonParser.parseIatResult(results.getResultString());
			mResultText.append(text);
			mResultText.setSelection(mResultText.length());
		}

		/**
		 * 识别回调错误.
		 */
		public void onError(SpeechError error) {

		}

	};
	/**
	 * 上传联系人 回调监听器.
	 */
	SpeechListener uploadListener = new SpeechListener() {
		/**
		 * 上传结束.
		 */
		public void onCompleted(SpeechError error) {
			if (error != null) {
				showTip(error.toString());
			} else {
				showTip(getString(R.string.text_upload_success));
			}
		}

		@Override
		public void onData(byte[] data) {
		}

		@Override
		public void onEvent(int paramInt, Bundle paramBundle) {
		}
	};

	/**
	 * 获取字节流对应的字符串,文件默认编码为UTF-8
	 * 
	 * @param inputStream
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private String readStringFromInputStream(InputStream inputStream)
			throws UnsupportedEncodingException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				inputStream, "UTF-8"));
		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}
		return builder.toString();
	}

	/**
	 * 用户词表下载监听器
	 */
	SpeechListener downloadlistener = new SpeechListener() {

		@Override
		public void onData(byte[] data) {
			try {
				if (data != null && data.length > 1)
					mDownloadResult = new String(data, "utf-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error != null) {
				showTip(error.toString());
			} else if (TextUtils.isEmpty(mDownloadResult)) {
				showTip(getString(R.string.text_userword_empty));
			} else {
				mResultText.setText("");
				UserWords userwords = new UserWords(mDownloadResult.toString());
				if (userwords == null || userwords.getKeys() == null) {
					showTip(getString(R.string.text_userword_empty));
					return;
				}
				for (String key : userwords.getKeys()) {
					mResultText.append(key + ":");
					for (String word : userwords.getWords(key)) {
						mResultText.append(word + ",");
					}
					mResultText.append("\n");
				}
				showTip(getString(R.string.text_download_success));
			}
		}

		@Override
		public void onEvent(int paramInt, Bundle paramBundle) {

		}

	};

	private void showTip(String str) {
		if (!TextUtils.isEmpty(str)) {
			mToast.setText(str);
			mToast.show();
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
			setCacelButtonVisible();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void setCacelButtonVisible() {
		boolean isShowDialog = mSharedPreferences.getBoolean(
				getString(R.string.preference_key_iat_show), true);
		if (!isShowDialog) {
			((Button) findViewById(android.R.id.button2))
					.setVisibility(View.VISIBLE);
		} else {
			((Button) findViewById(android.R.id.button2))
					.setVisibility(View.GONE);
		}
	}



}
