package com.iflytek.mscdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.cooby.voiceassistant.activity.TouchAndTalkActivity;
import com.iflytek.cloud.speech.SpeechError;
import com.iflytek.cloud.speech.SpeechListener;
import com.iflytek.cloud.speech.SpeechUser;

/**
 * 应用程序主界面,可以向语音听写,识别,合成跳转.
 * @author iFlytek
 * @since  20120821
 */
public class MainActivity extends Activity implements OnClickListener {
	
	public static final String KEY = "KEY_ISR";
	public static final String KEYWORD = "keyword";
	public static final String ABNF = "abnf";
	/**
	 * 界面初始化入口函数.
	 * @param savedInstanceState
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//标题初始化
		setTitle(R.string.title);
		((TextView) findViewById(android.R.id.title))
				.setGravity(Gravity.CENTER);
		((TextView) findViewById(R.id.text_isr_introduction))
				.setMovementMethod(ScrollingMovementMethod
				.getInstance());
		//用户登录
		SpeechUser.getUser().login(MainActivity.this, null, null
				, "appid=" + getString(R.string.app_id), listener);
		//按钮设置监听
		findViewById(R.id.btn_iat_demo).setOnClickListener(this);
		findViewById(R.id.btn_understander_demo).setOnClickListener(this);
		findViewById(R.id.btn_isr_keyword_demo).setOnClickListener(this);
		findViewById(R.id.btn_isr_abnf_demo).setOnClickListener(this);
		findViewById(R.id.btn_tts_demo).setOnClickListener(this);
		//关闭log的打印
//		Setting.showLogcat(false);

		// start IAT
		Intent intent = null;
		intent = new Intent(this, TouchAndTalkActivity.class);
		startActivity(intent);
	}

	/**
	 * 按钮点击事件.
	 */
	@Override
	public void onClick(View v) {

	}
	
	/**
	 * 用户登录回调监听器.
	 */
	private SpeechListener listener = new SpeechListener()
	{

		@Override
		public void onData(byte[] arg0) {
		}

		@Override
		public void onCompleted(SpeechError error) {
			if(error != null) {
				Toast.makeText(MainActivity.this, getString(R.string.text_login_fail)
						, Toast.LENGTH_SHORT).show();
				
			}			
		}

		@Override
		public void onEvent(int arg0, Bundle arg1) {
		}		
	};
}
