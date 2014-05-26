package com.cooby.voiceassistant.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.speech.RecognizerListener;
import com.iflytek.cloud.speech.RecognizerResult;
import com.iflytek.cloud.speech.SpeechConstant;
import com.iflytek.cloud.speech.SpeechError;
import com.iflytek.cloud.speech.SpeechRecognizer;
import com.iflytek.cloud.speech.SpeechSynthesizer;
import com.iflytek.mscdemo.CoobyUtils;
import com.iflytek.mscdemo.IatPreferenceActivity;
import com.iflytek.mscdemo.R;
import com.iflytek.mscdemo.util.JsonParser;

/**
 * 听写页面,通过调用SDK中提供的RecognizerDialog来实现听写功能.
 * 
 * @author iFlytek
 * @since 20120822
 */
public class TouchAndTalkActivity extends Activity implements OnClickListener {
	// 日志TAG.
	private static final String TAG = "CoobyVoiceActivity";
	// Tip
	private Toast mToast;

	// 识别结果显示
	private TextView request;
	private TextView response;
	// 缓存，保存当前的引擎参数到下一次启动应用程序使用.
	private SharedPreferences mSharedPreferences;
	// 识别对象
	private SpeechRecognizer iatRecognizer;
	
	private SpeechSynthesizer mSpeechSynthesizer;

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
		setContentView(R.layout.touch_and_talk);
		// 设置自定义标题类型
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.titlebar);

		mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

		// 初始化缓存对象.
		mSharedPreferences = getSharedPreferences(getPackageName(),
				MODE_PRIVATE);

		// “talk”按钮初始化
		Button talkButton = (Button) findViewById(R.id.buttonTalk);
		talkButton.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				if (action == MotionEvent.ACTION_DOWN) {
					if (mSpeechSynthesizer != null
							&& mSpeechSynthesizer.isSpeaking())
						mSpeechSynthesizer.stopSpeaking();

					showIatInvisble();


					System.out.println("Touch");
				} else if (action == MotionEvent.ACTION_UP) {
					System.out.println("Release");
					if (null == iatRecognizer) {
						iatRecognizer = SpeechRecognizer
								.createRecognizer(TouchAndTalkActivity.this);
					}
					if (iatRecognizer.isListening()) {
						iatRecognizer.stopListening();
						showTip("停止录音");
					}
				}
				return false;
			}
		});

		// 听写结果设置
		request = (TextView) findViewById(R.id.textViewRequest);
		response = (TextView) findViewById(R.id.textViewResponse);
		// findViewById(R.id.personal_layout).setVisibility(View.VISIBLE);

		// 创建听写对象,如果只使用无UI听写功能,不需要创建RecognizerDialog
		iatRecognizer = SpeechRecognizer.createRecognizer(this);

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
		// for (int i = 0; i < engineValues.length; i++) {
		// if (engineValues[i].equals(engine)) {
		// mCategoryText.setText(engineEntries[i]);
		// break;
		// }
		// }
	}

	@Override
	protected void onStop() {
		mToast.cancel();
		if (null != iatRecognizer) {
			iatRecognizer.cancel();
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

	}

	protected void cancelRecognize() {
		if (null != iatRecognizer) {
			iatRecognizer.cancel();
		}
		((Button) findViewById(android.R.id.button1))
				.setText(TouchAndTalkActivity.this.getString(R.string.text_iat));
		((Button) findViewById(android.R.id.button1)).setEnabled(true);
	}

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
		request.setText(null);
		// 显示听写对话框
		iatRecognizer.startListening(recognizerListener);
		// showTip(getString(R.string.text_iat_begin));
	}

	RecognizerListener recognizerListener = new RecognizerListener() {

		@Override
		public void onBeginOfSpeech() {
			showTip("开始说话");
		}

		@Override
		public void onError(SpeechError err) {
			showTip(err.getPlainDescription(true));

		}

		@Override
		public void onEndOfSpeech() {


		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, String msg) {

		}

		@Override
		public void onResult(RecognizerResult results, boolean isLast) {
			String text = JsonParser.parseIatResult(results.getResultString());
			request.append(text);
			// mResultText.setSelection(mResultText.length());
			// upload the result to cooby

			if (isLast) {
				String cmd = request.getText().toString();

				try{
				String resp = CoobyUtils.callCooby(cmd);

				// showTip(resp);
				response.setText(resp);
				synthetizeInSilence(resp);
				}catch(Exception e)
				{
					showTip(e.getMessage());
				}
			}
		}

		@Override
		public void onVolumeChanged(int volume) {
			showTip("当前正在说话，音量大小：" + volume);
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

	private void synthetizeInSilence(String text) {
		if (null == mSpeechSynthesizer) {
			// 创建合成对象.
			mSpeechSynthesizer = SpeechSynthesizer.createSynthesizer(this);
		}
		// 设置合成发音人.
		String role = mSharedPreferences.getString(
				getString(R.string.preference_key_tts_role),
				getString(R.string.preference_default_tts_role));

		// 设置发音人
		mSpeechSynthesizer.setParameter(SpeechConstant.VOICE_NAME, role);
		// 获取语速
		int speed = mSharedPreferences.getInt(
				getString(R.string.preference_key_tts_speed), 50);
		// 设置语速
		mSpeechSynthesizer.setParameter(SpeechConstant.SPEED, "" + speed);
		// 获取音量.
		int volume = mSharedPreferences.getInt(
				getString(R.string.preference_key_tts_volume), 50);
		// 设置音量
		mSpeechSynthesizer.setParameter(SpeechConstant.VOLUME, "" + volume);
		// 获取语调
		int pitch = mSharedPreferences.getInt(
				getString(R.string.preference_key_tts_pitch), 50);
		// 设置语调
		mSpeechSynthesizer.setParameter(SpeechConstant.PITCH, "" + pitch);
		// 获取合成文本.

		// 进行语音合成.
		mSpeechSynthesizer.startSpeaking(text, null);
		// showTip(String.format(getString(R.string.tts_toast_format), 0, 0));
	}

}
