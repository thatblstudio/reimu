/**
 *  You can modify and use this source freely
 *  only for the development of application related Live2D.
 *
 *  (c) Live2D Inc. All rights reserved.
 */

package com.blStudio.Reimu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.speech.setting.IatSettings;
import com.iflytek.speech.util.JsonParser;

import jp.live2d.utils.android.FileManager;
import jp.live2d.utils.android.SoundManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;


public class MainActivity extends Activity implements LeftDrawer.OnItemClickListener{
	
	// Live2d相关
	private LAppLive2DManager live2DMgr ;
	static private Activity instance;
	public static Boolean lipSync = true;
	
	// navigationdrawer相关
	private DrawerLayout mDrawerLayout;
    private RecyclerView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;	
	private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mDrawerTitles;
    
    // opencloud语音云相关
    // TTS 文字转语音
    public static SpeechSynthesizer mTts;
    private String voicer="xiaoqi";
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    private SharedPreferences mTtsSharedPreferences;
    // IAT 语音转文字
    private SpeechRecognizer mIat;
	private SharedPreferences mIatSharedPreferences;
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    
    // 树莓派服务器相关
    public static final String TAG = "Network Connect";
    
    // 杂项
    // 界面相关
    private EditText mEditText;
    private Button oBallon;
    private Button iBallon;
    private Toast mToast;
    int ret = 0; // 函数调用返回值
    final float endAlpha = 0f;	// 动画结束时的透明度
    ObjectAnimator oBallonFadeOut;
    ObjectAnimator iBallonFadeOut;
    
	public MainActivity(){
		instance=this;
		if(LAppDefine.DEBUG_LOG)
		{
			Log.d( "", "==============================================\n" ) ;
			Log.d( "", "   Live2D Sample  \n" ) ;
			Log.d( "", "==============================================\n" ) ;
		}

		SoundManager.init(this);
		live2DMgr = new LAppLive2DManager() ;
	}


	 static public void exit(){
		SoundManager.release();
    	instance.finish();
    }
	
	@SuppressLint("ShowToast")
	@Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        
        // 初始化讯飞语音云平台
        SpeechUtility.createUtility(MainActivity.this, "appid="+getString(R.string.voicecloud_app_id));
        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
        mTtsSharedPreferences = getSharedPreferences("com.iflytek.setting", MODE_PRIVATE);
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
        mIatSharedPreferences = getSharedPreferences(IatSettings.PREFER_NAME,
				Activity.MODE_PRIVATE);
        
        setContentView(R.layout.activity_main);
        
        mTitle = mDrawerTitle = getTitle();
        mDrawerTitles = getResources().getStringArray(R.array.drawer_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (RecyclerView) findViewById(R.id.left_drawer);
        mEditText = (EditText)findViewById(R.id.mainEditText);
        oBallon = (Button)findViewById(R.id.outgoingBallon);
        iBallon = (Button)findViewById(R.id.incomingBallon);
        
        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // improve performance by indicating the list if fixed size.
        mDrawerList.setHasFixedSize(true);
        mDrawerList.setLayoutManager(new LinearLayoutManager(this));
        
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new LeftDrawer(mDrawerTitles, this));
       
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

      	setupGUI();
      	FileManager.init(this.getApplicationContext());
      	
      	// 淡出
      	oBallonFadeOut = ObjectAnimator.ofFloat(oBallon,"alpha",1f,endAlpha);
      	oBallonFadeOut.setDuration(5000);
      	oBallonFadeOut.setStartDelay(2000);
		
      	oBallonFadeOut.addListener(new AnimatorListenerAdapter(){
			@Override
		    public void onAnimationEnd(Animator animation) {
//		        super.onAnimationEnd(animation);
		        if(oBallon.getAlpha()==endAlpha){
		        	oBallon.setVisibility(View.INVISIBLE);
		        }
		    }
		});
      	
      	iBallonFadeOut = ObjectAnimator.ofFloat(iBallon,"alpha",1f,endAlpha);
      	iBallonFadeOut.setDuration(5000);
      	iBallonFadeOut.setStartDelay(2000);
		
      	iBallonFadeOut.addListener(new AnimatorListenerAdapter(){
			@Override
		    public void onAnimationEnd(Animator animation) {
//		        super.onAnimationEnd(animation);
		        if(iBallon.getAlpha()==endAlpha){
		        	iBallon.setVisibility(View.INVISIBLE);
		        }
		    }
		});
    }
	
	void setupGUI(){        
        LAppView view = live2DMgr.createView(this) ;
        
        FrameLayout layout=(FrameLayout) findViewById(R.id.live2DLayout);
		layout.addView(view, 0, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		ImageButton imgBtnVoice = (ImageButton)findViewById(R.id.imgBtnVoice);
		GetVoice imgBtnVoiceListener = new GetVoice();
		imgBtnVoice.setOnClickListener(imgBtnVoiceListener);
		
		ImageButton imgBtnSend = (ImageButton)findViewById(R.id.imgBtnSend);
		SendMessage imgBtnSendListener = new SendMessage();
		imgBtnSend.setOnClickListener(imgBtnSendListener);
	}
	
	class GetVoice implements OnClickListener{
		@Override
		public void onClick(View v){
			mIatResults.clear();
			setIatParam();
			ret = mIat.startListening(recognizerListener);
			if (ret != ErrorCode.SUCCESS) {
				showTip("听写失败,错误码：" + ret);
			} else {
				showTip(getString(R.string.text_begin));
			}
		}
	}
	
	class SendMessage implements OnClickListener{
		@Override
		public void onClick(View v){
			String inputStr = mEditText.getText().toString();
			if (inputStr==null||inputStr.equals("")){
				//未输入字符
				showTip(getString(R.string.no_text_in_edit_text));
			}
			else{
				mEditText.setText("");
				showOnOutgoingBallon(inputStr);
				if(inputStr.equals("开灯")){
					new DownloadTask().execute(getString(R.string.home_url)+"gpio/gpio_set.php?id=38&mode=out&voltage=high");
				}
				else if(inputStr.equals("关灯")){
					new DownloadTask().execute(getString(R.string.home_url)+"gpio/gpio_set.php?id=38&mode=out&voltage=low");
				}
				else{
					getTuringRobotReply(inputStr);
//					getSimSimiReply(inputStr);
				}
			}
		}
	}
	
	@Override
	protected void onResume(){
		//live2DMgr.onResume() ;
		super.onResume();
	}
	
	@Override
	protected void onPause(){
		live2DMgr.onPause() ;
    	super.onPause();
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
 
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.author_detail).setVisible(!drawerOpen);
        menu.findItem(R.id.change_model).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	// call ActionBarDrawerToggle.onOptionsItemSelected(), if it returns true
        // then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch(item.getItemId()){
	    	case R.id.author_detail:
				onAuthorDetailClicked();
				return true;
	    	case R.id.change_model:
				showTip(getString(R.string.change_model));
				live2DMgr.changeModel();//Live2D Event
	    		return true;
	    	default:
	            return super.onOptionsItemSelected(item);	
		}
    }
    
    /* The click listener for RecyclerView in the navigation drawer */
    @Override
    public void onClick(View view, int position) {
        selectItem(position);
    }
    
    private long exitTime = System.currentTimeMillis();
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event){
		//双击后退键退出
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if((System.currentTimeMillis() - exitTime)>2000){
				showTip(getString(R.string.click_KEYCODE_BACK));
				exitTime = System.currentTimeMillis();
			}else{
				exit();
			}
		}
		return false;
	}

    private void selectItem(int position) {
    	switch (position){
    	case 0:
    		new DownloadTask().execute(getString(R.string.home_url)+"get_temperature.php");
    		return;
    	case 1:
    		new DownloadTask().execute(getString(R.string.home_url)+"gpio/gpio_set.php?id=7&mode=in");
    		return;
    	case 2:
    		new DownloadTask().execute(getString(R.string.home_url)+"gpio/gpio_set.php?id=38&mode=out&voltage=high");
    		return;
    	case 3:
    		new DownloadTask().execute(getString(R.string.home_url)+"gpio/gpio_set.php?id=38&mode=out&voltage=low");
    		return;
    	}
//        mDrawerLayout.closeDrawer(mDrawerList);
    }
    
    /**
     * Implementation of AsyncTask, to fetch the data in the background away from
     * the UI thread.
     */
    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                return loadFromNetwork(urls[0]);
            } catch (IOException e) {
              return getString(R.string.connection_error);
            }
        }

        /**
         * Uses the logging framework to display the output of the fetch
         * operation in the log fragment.
         */
        @Override
        protected void onPostExecute(String result) {
        	try {
				JSONObject mJson = new JSONObject(result);
				String name = mJson.optString("name");
				if (name.equals("temperature")){
					double var = mJson.optDouble("var");
					double var1 = (double)((int)(var/100))/10;
					showTip("服务器CPU温度为"+var1+"摄氏度");
				}
				else if(name.equals("gpio")){
					int id = mJson.optInt("id");
					String mode = mJson.optString("mode");
					if(id==7&&mode.equals("in")){
						int var = mJson.getInt("var");
						if(var==0){
							showTip("植物不需要浇水~");
						}
						else{
							showTip("要浇水啦要浇水啦！");
						}
					}
					else if(id==38&&mode.equals("out")){
						String var = mJson.getString("var");
						if(var.equals("high")){
							showTip("灯已打开~");
						}
						else{
							showTip("灯已关闭~");
						}
					}
				}
        	} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
    
    /** Initiates the fetch operation. */
    private String loadFromNetwork(String urlString) throws IOException {
        InputStream stream = null;
        String str ="";

        try {
            stream = downloadUrl(urlString);
            str = readIt(stream, 500);
       } finally {
           if (stream != null) {
               stream.close();
            }
        }
        return str;
    }
    
    /**
     * Given a string representation of a URL, sets up a connection and gets
     * an input stream.
     * @param urlString A string representation of a URL.
     * @return An InputStream retrieved from a successful HttpURLConnection.
     * @throws java.io.IOException
     */
    private InputStream downloadUrl(String urlString) throws IOException {

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Start the query
        conn.connect();
        InputStream stream = conn.getInputStream();
        return stream;

    }

    /** Reads an InputStream and converts it to a String.
     * @param stream InputStream containing HTML from targeted site.
     * @param len Length of string that this method returns.
     * @return String concatenated according to len parameter.
     * @throws java.io.IOException
     * @throws java.io.UnsupportedEncodingException
     */
        
    private String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
    	if (stream != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try
            {
                Reader reader = new BufferedReader(
                new InputStreamReader(stream, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) 
                {
                    writer.write(buffer, 0, n);
                }
            }
            finally 
            {
            	stream.close();
            }
            return writer.toString();
        } else {       
            return "";
        }
//        Reader reader = null;
//        reader = new InputStreamReader(stream, "UTF-8");
//        char[] buffer = new char[len];
//        reader.read(buffer);
//        return new String(buffer);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }
    
	void onAuthorDetailClicked(){
		Log.i("TAG", "=========选中作者详情键");
		AlertDialog.Builder builder = new Builder(MainActivity.this);
		builder.setTitle(R.string.author_detail_title);
		builder.setMessage(R.string.author_detail_text);
		builder.setIcon(R.drawable.logo);
		builder.create().show();
	}
	
	private void setTtsParam(){
		// 清空参数
		mTts.setParameter(SpeechConstant.PARAMS, null);
		//设置合成
		if(mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
			mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
			//设置发音人
			mTts.setParameter(SpeechConstant.VOICE_NAME,voicer);
			//设置语速
			mTts.setParameter(SpeechConstant.SPEED,mTtsSharedPreferences.getString("speed_preference", "50"));
			//设置音调
			mTts.setParameter(SpeechConstant.PITCH,mTtsSharedPreferences.getString("pitch_preference", "50"));
			//设置音量
			mTts.setParameter(SpeechConstant.VOLUME,mTtsSharedPreferences.getString("volume_preference", "50"));
			//设置播放器音频流类型
			mTts.setParameter(SpeechConstant.STREAM_TYPE,mTtsSharedPreferences.getString("stream_preference", "3"));
		}else {
			mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
			//设置发音人 voicer为空默认通过语音+界面指定发音人。
			mTts.setParameter(SpeechConstant.VOICE_NAME,"");
		}
	}
	
	private SynthesizerListener mTtsListener = new SynthesizerListener() {
		@Override
		public void onSpeakBegin() {
			//showTip("开始播放");
		}

		@Override
		public void onSpeakPaused() {
			//showTip("暂停播放");
		}

		@Override
		public void onSpeakResumed() {
			//showTip("继续播放");
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos,
				String info) {
			// 合成进度
			//mPercentForBuffering = percent;
			//showTip(String.format(getString(R.string.tts_toast_format),
					//mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			// 播放进度
			//mPercentForPlaying = percent;
			//showTip(String.format(getString(R.string.tts_toast_format),
			//		mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error == null) {
				//showTip("播放完成");
			} else if (error != null) {
				//showTip(error.getPlainDescription(true));
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			
		}
	};	
	
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			//Log.d(TAG, "InitListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
        		showTip("初始化失败,错误码："+code);
        	} else {
				// 初始化成功，之后可以调用startSpeaking方法
        		// 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
        		// 正确的做法是将onCreate中的startSpeaking调用移至这里
			}		
		}
	};
	
	/**
	 * 初始化监听器。
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败，错误码：" + code);
			}
		}
	};
	
	/**
	 * 听写监听器。
	 */
	private RecognizerListener recognizerListener = new RecognizerListener() {

		@Override
		public void onBeginOfSpeech() {
			showTip("开始说话");
		}

		@Override
		public void onError(SpeechError error) {
			// Tips：
			// 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
			// 如果使用本地功能（语音+）需要提示用户开启语音+的录音权限。
			showTip(error.getPlainDescription(true));
		}

		@Override
		public void onEndOfSpeech() {
			showTip("结束说话");
		}

		@Override
		public void onResult(RecognizerResult results, boolean isLast) {
			Log.d(TAG, results.getResultString());
			String text = JsonParser.parseIatResult(results.getResultString());

			String sn = null;
			// 读取json结果中的sn字段
			try {
				JSONObject resultJson = new JSONObject(results.getResultString());
				sn = resultJson.optString("sn");
			} catch (JSONException e) {
				e.printStackTrace();
			}

			mIatResults.put(sn, text);

			if (isLast) {
				printResult(results);
			}
		}

		@Override
		public void onVolumeChanged(int volume) {
			showTip("当前正在说话，音量大小：" + volume);
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
		}
	};
	
	private void printResult(RecognizerResult results) {
		
		StringBuffer resultBuffer = new StringBuffer();
		for (String key : mIatResults.keySet()) {
			resultBuffer.append(mIatResults.get(key));
		}
		
		String result = resultBuffer.toString();
		
		showOnOutgoingBallon(result);
		if(result.equals("开灯")||result.equals("开灯。")){
			new DownloadTask().execute(getString(R.string.home_url)+"gpio/gpio_set.php?id=38&mode=out&voltage=high");
			showOnInComingBallon("灯已打开~");
		}
		else if(result.equals("关灯")||result.equals("关灯。")){
			new DownloadTask().execute(getString(R.string.home_url)+"gpio/gpio_set.php?id=38&mode=out&voltage=low");
			showOnInComingBallon("灯已关闭~");
		}
		else{
			getTuringRobotReply(result);
//			getSimSimiReply(result);
		}	

	}
	
	private void showOnOutgoingBallon(String str){
		oBallon.setText(str);
		oBallon.setAlpha(1f);
		oBallon.setVisibility(View.VISIBLE);
		
		if(oBallonFadeOut.isStarted()){
			Log.d("FADEOUT","oBallon end");
			oBallonFadeOut.cancel();
		}
		Log.d("FADEOUT","oBallon start");
		oBallonFadeOut.start();
	}
	
	private void getTuringRobotReply(String outgoingString){
		Log.d("TuringRobot",outgoingString);
		String info;
		try {
			info = URLEncoder.encode(outgoingString, "UTF-8");
			String url = getString(R.string.turing_robot_api)
					+"key="+getString(R.string.turing_robot_key)
					+"&info="+info;
			Log.d("TuringRobot",url);
			new GetTuringRobot().execute(url);	
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	private class GetTuringRobot extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                return loadFromNetwork(urls[0]);
            } catch (IOException e) {
            	showTip("TuringRobot后台暂时无法访问");
            	return getString(R.string.connection_error);
            }
        }

        /**
         * Uses the logging framework to display the output of the fetch
         * operation in the log fragment.
         */
        @Override
        protected void onPostExecute(String replyJson) {
        	Log.d("TuringRobot", replyJson);
            try {
				JSONObject mJson = new JSONObject(replyJson);
				int code = mJson.optInt("code");
				String text = mJson.optString("text");
				String url,list;
				switch (code){
				case 100000:break;//文本类数据
				case 305000:
//					list = mJson.optString("list");
//					text = text+" "+list;
					text = text+" 列车详情功能尚未完成";
					break;//列车
				case 306000:
//					list = mJson.optString("list");
//					text = text+" "+list;
					text = text+" 航班详情功能尚未完成";
					break;//航班
				case 200000:
					url = mJson.optString("url");
					text = text+" "+url;
					break;//网址类数据
				case 302000:
//					list = mJson.optString("list");
//					text = text+" "+list;
					text = text+" 新闻详情功能尚未完成";
					break;//新闻
				case 308000:
//					list = mJson.optString("list");
//					text = text+" "+list;
					text = text+" 菜谱、视频、小说详情功能尚未完成";
					break;//菜谱、视频、小说
				case 40001:showTip("key的长度错误（32位）");break;//key的长度错误（32位）
				case 40002:showTip("请求内容为空");break;//请求内容为空
				case 40003:showTip("key错误或帐号未激活");break;//key错误或帐号未激活
				case 40004:showTip("当天请求次数已用完");break;//当天请求次数已用完
				case 40005:showTip("暂不支持该功能");break;//暂不支持该功能
				case 40006:showTip("服务器升级中");break;//服务器升级中
				case 40007:showTip("服务器数据格式异常");break;//服务器数据格式异常
				default:showTip("未知错误码"+code);break;
				}
//				if(result!=100){
//					showTip(String.valueOf(result)+msg);
//				}
//				else{
//					Log.d("TuringRobot",text);
//					if(text==null||text.equals("")){
//						text = getString(R.string.simsimi_no_reply);
//					}
//					Log.d("TuringRobot",text);
//					
//				}
				Log.d("TuringRobot",text);
				if(text==null||text.equals("")){
					text = getString(R.string.robot_no_reply);
				}
				Log.d("TuringRobot",text);
				showOnInComingBallon(text);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
        }
    }
	
//	private void getSimSimiReply(String outgoingString){
////		showTip(outgoingString);
//		Log.d("SimSimi",outgoingString);
//		// 简体用ch 繁体用zh
//		String url = getString(R.string.simsimi_trial_api)
//				+"?key="+getString(R.string.simsimi_trial_key)
//				+"&lc=ch&ft=1.0&text="+outgoingString;
//
//		Log.d("SimSimi",url);
//		new GetSimSimi().execute(url);		
//	}
//		
//	private class GetSimSimi extends AsyncTask<String, Void, String> {
//
//        @Override
//        protected String doInBackground(String... urls) {
//            try {
//                return loadFromNetwork(urls[0]);
//            } catch (IOException e) {
//            	showTip("Simsimi后台暂时无法访问");
////            	showOnInComingBallon("Simsimi后台无法访问");
//            	return getString(R.string.connection_error);
//            }
//        }
//
//        /**
//         * Uses the logging framework to display the output of the fetch
//         * operation in the log fragment.
//         */
//        @Override
//        protected void onPostExecute(String replyJson) {
//        	Log.d(TAG, replyJson);
//            try {
//				JSONObject mJson = new JSONObject(replyJson);
//				String response = mJson.optString("response");
//				int result = mJson.optInt("result");
//				String msg = mJson.optString("msg");
//				if(result!=100){
//					showTip(String.valueOf(result)+msg);
//				}
//				else{
//					Log.d("SimSimi",response);
//					if(response==null||response.equals("")){
//						response = getString(R.string.simsimi_no_reply);
//					}
//					Log.d("SimSimi",response);
//					showOnInComingBallon(response);
//				}
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//            
//        }
//    }
	
	private void showOnInComingBallon(String str){
		iBallon.setText(str);
		iBallon.setAlpha(1f);
		iBallon.setVisibility(View.VISIBLE);
		
		if(iBallonFadeOut.isStarted()){
			Log.d("FADEOUT","iBallon end");
			iBallonFadeOut.cancel();
		}
		Log.d("FADEOUT","iBallon start");
		iBallonFadeOut.start();
		startSpeaking(str);
	}
	
	private void startSpeaking(String str){
		setTtsParam();
		int code = mTts.startSpeaking(str, mTtsListener);
		if (code != ErrorCode.SUCCESS) {
			showTip("初始化失败，错误码：" + code);
		}
	}
	
	/**
	 * 参数设置
	 * 
	 * @param param
	 * @return
	 */
	public void setIatParam() {
		// 清空参数
		mIat.setParameter(SpeechConstant.PARAMS, null);

		// 设置听写引擎
		mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
		// 设置返回结果格式
		mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

		String lag = mIatSharedPreferences.getString("iat_language_preference",
				"mandarin");
		if (lag.equals("en_us")) {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
		} else {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
			// 设置语言区域
			mIat.setParameter(SpeechConstant.ACCENT, lag);
		}
		// 设置语音前端点
		mIat.setParameter(SpeechConstant.VAD_BOS, mIatSharedPreferences.getString("iat_vadbos_preference", "4000"));
		// 设置语音后端点
		mIat.setParameter(SpeechConstant.VAD_EOS, mIatSharedPreferences.getString("iat_vadeos_preference", "1000"));
		// 设置标点符号
		mIat.setParameter(SpeechConstant.ASR_PTT, mIatSharedPreferences.getString("iat_punc_preference", "1"));
		// 设置音频保存路径
		mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()
				+ "/iflytek/wavaudio.pcm");
		// 设置听写结果是否结果动态修正，为“1”则在听写过程中动态递增地返回结果，否则只在听写结束之后返回最终结果
		// 注：该参数暂时只对在线听写有效
		mIat.setParameter(SpeechConstant.ASR_DWA, mIatSharedPreferences.getString("iat_dwa_preference", "0"));
	}
	
	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}
}
