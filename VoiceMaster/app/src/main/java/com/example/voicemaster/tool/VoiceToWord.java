package com.example.voicemaster.tool;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.voicemaster.MainActivity;
import com.example.voicemaster.R;
import com.example.voicemaster.keyword.KeyWordFind;
import com.example.voicemaster.keyword.bean.KeyWordBean;
import com.example.voicemaster.translate.Translate;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.ContactManager;
import com.iflytek.cloud.util.ContactManager.ContactListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.example.voicemaster.speech.setting.VoiceToWordSetting;
import com.example.voicemaster.speech.util.FucUtil;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;

//import com.iflytek.cloud.param.MscKeys;

public class VoiceToWord extends Activity implements OnClickListener{
	private static String TAG = "cypress";
	public static File RecordSoundFile = null;
	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE };

	// 语音听写对象
	private SpeechRecognizer mIat;
	// 语音听写UI
	private RecognizerDialog mIatDialog;
	// 听写结果内容
	private EditText mResultText;
	// 用HashMap存储听写结果
	private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

	private Toast mToast;

	private SharedPreferences mSharedPreferences;
	private String mEngineType = "cloud";

	//选择文件的路径
	public String path = null;

	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.voice_to_word);
		initLayout();
		// 初始化识别无UI识别对象
		// 使用SpeechRecognizer对象，可根据回调消息自定义界面；
		mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
		
		// 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
		// 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
		mIatDialog = new RecognizerDialog(this,mInitListener);
		
		mSharedPreferences = getSharedPreferences(VoiceToWordSetting.PREFER_NAME, Activity.MODE_PRIVATE);
		mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		mResultText = ((EditText)findViewById(R.id.iat_text));
	}

	/**
	 * 初始化Layout。
	 */
	private void initLayout(){
		findViewById(R.id.iat_recognize).setOnClickListener(this);
		findViewById(R.id.iat_recognize_stream).setOnClickListener(this);
		findViewById(R.id.iat_upload_contacts).setOnClickListener(this);
		findViewById(R.id.iat_upload_userwords).setOnClickListener(this);
		findViewById(R.id.iat_stop).setOnClickListener(this);
		findViewById(R.id.iat_cancel).setOnClickListener(this);
		findViewById(R.id.image_iat_set).setOnClickListener(this);
		findViewById(R.id.btn_chooseFile).setOnClickListener(this);
		findViewById(R.id.btn_jumpKey).setOnClickListener(this);
		findViewById(R.id.btn_jumpTranslation).setOnClickListener(this);


		//选择云端or本地
		RadioGroup group = (RadioGroup)this.findViewById(R.id.iat_radioGroup);
		group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(checkedId == R.id.iat_radioCloud) {
					findViewById(R.id.iat_upload_contacts).setEnabled(true);
					findViewById(R.id.iat_upload_userwords).setEnabled(true);
					mEngineType = SpeechConstant.TYPE_CLOUD;
				}else if(checkedId == R.id.iat_radioLocal) {
					//离线听写不支持联系人/热词上传
					findViewById(R.id.iat_upload_contacts).setEnabled(false);
					findViewById(R.id.iat_upload_userwords).setEnabled(false);
					mEngineType =  SpeechConstant.TYPE_LOCAL;
				}
			}
		});
	}

	int ret = 0;// 函数调用返回值
	@Override
	public void onClick(View view) {		
		if( null == mIat ){
			// 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
			this.showTip( "创建对象失败，请确认 libmsc.so 放置正确，\n 且有调用 createUtility 进行初始化" );
			return;
		}
		
		switch (view.getId()) {
		// 进入参数设置页面
		case R.id.image_iat_set:
			Intent intents = new Intent(VoiceToWord.this, VoiceToWordSetting.class);
			startActivity(intents);
			break;
		// 开始听写
		// 如何判断一次听写结束：OnResult isLast=true 或者 onError
		case R.id.iat_recognize:
			mResultText.setText(null);// 清空显示内容
			mIatResults.clear();
			// 设置参数
			setParam();
			boolean isShowDialog = mSharedPreferences.getBoolean(getString(R.string.pref_key_iat_show), true);
			if (isShowDialog) {
				// 显示听写对话框
				mIatDialog.setListener(mRecognizerDialogListener);
				mIatDialog.show();
				showTip(getString(R.string.text_begin));
			} else {
				// 不显示听写对话框
				ret = mIat.startListening(mRecognizerListener);
				if (ret != ErrorCode.SUCCESS) {
					showTip("听写失败,错误码：" + ret+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
				} else {
					showTip(getString(R.string.text_begin));
				}
			}
			break;
		//选择文件
		case R.id.btn_chooseFile:
			//通过文件管理器读取手机上的文件
			Log.d(TAG, "onClick: 通过文件管理器读取手机上的文件");
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			//intent.setType(“image/*”);//选择图片
			//intent.setType("audio/*;*/*"); //选择音频
			//intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
			//intent.setType(“video/*;image/*”);//同时选择视频和图片
			intent.setType("*/*");//无类型限制
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			startActivityForResult(intent, 1);
			Log.d(TAG, "onClick: path = " + path);
			break;
		// 音频流识别
		case R.id.iat_recognize_stream:
			mResultText.setText(null);// 清空显示内容
			mIatResults.clear();
			// 设置参数
			setParam();

			// 设置音频来源为外部文件
			if(path == null){
			    if(RecordSoundFile != null){
			        path = RecordSoundFile.toString();
                }
			    else {
                    Toast.makeText(this, "请先选择文件", Toast.LENGTH_SHORT).show();
                    break;
                }
			}
			verifyStoragePermissions(this);

			mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
			// 也可以像以下这样直接设置音频文件路径识别（要求设置文件在sdcard上的全路径）：
//			 mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-2");
//			 mIat.setParameter(SpeechConstant.ASR_SOURCE_PATH, "sdcard/XXX/XXX.pcm");
//			mIat.setParameter(SpeechConstant.ASR_SOURCE_PATH, path);
			ret = mIat.startListening(mRecognizerListener);
			if (ret != ErrorCode.SUCCESS) {
				showTip("识别失败,错误码：" + ret+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
			} else {
//				byte[] audioData = FucUtil.readAudioFile(VoiceToWord.this, "isetest.wav");
				byte[] audioData = FucUtil.readDesAudioFile(VoiceToWord.this, path);

				if (null != audioData) {
					showTip(getString(R.string.text_begin_recognizer));
					// 一次（也可以分多次）写入音频文件数据，数据格式必须是采样率为8KHz或16KHz（本地识别只支持16K采样率，云端都支持），位长16bit，单声道的wav或者pcm
					// 写入8KHz采样的音频时，必须先调用setParameter(SpeechConstant.SAMPLE_RATE, "8000")设置正确的采样率
					// 注：当音频过长，静音部分时长超过VAD_EOS将导致静音后面部分不能识别
					Log.d(TAG, "onClick: 准备分析音频");
					mIat.writeAudio(audioData, 0, audioData.length);
					mIat.stopListening();
				} else {
					mIat.cancel();
					Log.d(TAG, "onClick: 读取音频流失败");
					showTip("读取音频流失败");
				}
			}
			break;
		// 停止听写
		case R.id.iat_stop:
			mIat.stopListening();
			showTip("停止听写");
			break;
		// 取消听写
		case R.id.iat_cancel:
			mIat.cancel();
			showTip("取消听写");
			break;
		// 上传联系人
		case R.id.iat_upload_contacts:
			showTip(getString(R.string.text_upload_contacts));
			ContactManager mgr = ContactManager.createManager(VoiceToWord.this, mContactListener);
			mgr.asyncQueryAllContactsName();
			break;
			// 上传用户词表
		case R.id.iat_upload_userwords:
			showTip(getString(R.string.text_upload_userwords));
			String contents = FucUtil.readFile(VoiceToWord.this, "userwords","utf-8");
			mResultText.setText(contents);
			// 指定引擎类型
			mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
			// 置编码类型
			mIat.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
			ret = mIat.updateLexicon("userword", contents, mLexiconListener);
			if (ret != ErrorCode.SUCCESS)
				showTip("上传热词失败,错误码：" + ret+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
			break;
		//跳转到关键词分析
		case R.id.btn_jumpKey:
			if (mResultText.getText().toString() == null || mResultText.getText().toString().equals("")){
				Toast.makeText(this, "啥都没有，不能跳转", Toast.LENGTH_SHORT).show();
				break;
			}
		    KeyWordFind.TEXT = mResultText.getText().toString();
			Log.d(TAG, "onNavigationItemSelected: 打开关键词提取");
			startActivity(new Intent(this, KeyWordFind.class));
			break;
		//跳转到翻译
		case R.id.btn_jumpTranslation:
			if (mResultText.getText().toString() == null || mResultText.getText().toString().equals("")){
				Toast.makeText(this, "啥都没有，不能跳转", Toast.LENGTH_SHORT).show();
				break;
			}
			Translate.TEXT = mResultText.getText().toString();
			Log.d(TAG, "onNavigationItemSelected: 打开翻译");
			startActivity(new Intent(this, Translate.class));
			break;
		default:
			break;
		}
	}

	/**
	 * 初始化监听器。
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败，错误码：" + code+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
			}
		}
	};

	/**
	 * 上传联系人/词表监听器。
	 */
	private LexiconListener mLexiconListener = new LexiconListener() {

		@Override
		public void onLexiconUpdated(String lexiconId, SpeechError error) {
			if (error != null) {
				showTip(error.toString());
			} else {
				showTip(getString(R.string.text_upload_success));
			}
		}
	};

	/**
	 * 听写监听器。
	 */
	private RecognizerListener mRecognizerListener = new RecognizerListener() {

		@Override
		public void onBeginOfSpeech() {
			// 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
			showTip("开始说话");
		}

		@Override
		public void onError(SpeechError error) {
			// Tips：
			// 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。

			showTip(error.getPlainDescription(true));

		}

		@Override
		public void onEndOfSpeech() {
			// 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
			showTip("结束说话");
		}

		@Override
		public void onResult(RecognizerResult results, boolean isLast) {

			String text = JsonParser.parseIatResult(results.getResultString());
			mResultText.append(text);
			mResultText.setSelection(mResultText.length());

			
			if(isLast) {
				//TODO 最后的结果

			}
		}

		@Override
		public void onVolumeChanged(int volume, byte[] data) {
			showTip("当前正在说话，音量大小：" + volume);
			Log.d(TAG, "返回音频数据："+data.length);
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
				if (SpeechEvent.EVENT_SESSION_ID == eventType) {
					String sid = obj.getString(SpeechEvent.KEY_EVENT_AUDIO_URL);
					Log.d(TAG, "session id =" + sid);
				}
		}
	};
	
	/**
	 * 听写UI监听器
	 */
	private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
		public void onResult(RecognizerResult results, boolean isLast) {
			Log.d(TAG, "recognizer result：" + results.getResultString());

			String text = JsonParser.parseIatResult(results.getResultString());
			mResultText.append(text);
			mResultText.setSelection(mResultText.length());

		}

		/**
		 * 识别回调错误.
		 */
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));

		}

	};

	/**
	 * 获取联系人监听器。
	 */
	private ContactListener mContactListener = new ContactListener() {

		@Override
		public void onContactQueryFinish(final String contactInfos, boolean changeFlag) {
			// 注：实际应用中除第一次上传之外，之后应该通过changeFlag判断是否需要上传，否则会造成不必要的流量.
			// 每当联系人发生变化，该接口都将会被回调，可通过ContactManager.destroy()销毁对象，解除回调。
			// if(changeFlag) {
			// 指定引擎类型
			runOnUiThread(new Runnable() {
				public void run() {
					mResultText.setText(contactInfos);
				}
			});
			
			mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
			mIat.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
			ret = mIat.updateLexicon("contact", contactInfos, mLexiconListener);
			if (ret != ErrorCode.SUCCESS) {
				showTip("上传联系人失败：" + ret);
			}
		}
	};

	private void showTip(final String str)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mToast.setText(str);
				mToast.show();
			}
		});
	}

	/**
	 * 参数设置
	 * @return
	 */
	public void setParam(){
		// 清空参数
		mIat.setParameter(SpeechConstant.PARAMS, null);
		String lag = mSharedPreferences.getString("iat_language_preference", "mandarin");
		// 设置引擎
		mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
		// 设置返回结果格式
		mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

		//mIat.setParameter(MscKeys.REQUEST_AUDIO_URL,"true");

	//	this.mTranslateEnable = mSharedPreferences.getBoolean( this.getString(R.string.pref_key_translate), false );
		if (mEngineType.equals(SpeechConstant.TYPE_LOCAL)) {
			// 设置本地识别资源
			mIat.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
		}
        // 在线听写支持多种小语种，若想了解请下载在线听写能力，参看其speechDemo
		if (lag.equals("en_us")) {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
			mIat.setParameter(SpeechConstant.ACCENT, null);

			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
			// 设置语言区域
			mIat.setParameter(SpeechConstant.ACCENT,lag);

		}


		// 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
		mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));
		
		// 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
		mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));
		
		// 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
		mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));
		
		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
		mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
	}

	private String getResourcePath(){
		StringBuffer tempBuffer = new StringBuffer();
		//识别通用资源
		tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "iat/common.jet"));
		tempBuffer.append(";");
		tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "iat/sms_16k.jet"));
		//识别8k资源-使用8k的时候请解开注释
		return tempBuffer.toString();
	}

	//文件管理器获得选择文件的路径
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			if ("file".equalsIgnoreCase(uri.getScheme())){//使用第三方应用打开
				path = uri.getPath();
//				tv.setText(path);
				Toast.makeText(this,path+"11111",Toast.LENGTH_SHORT).show();
				return;
			}
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
				path = getPath(VoiceToWord.this, uri);
//				tv.setText(path);
				Toast.makeText(this,path,Toast.LENGTH_SHORT).show();
			} else {//4.4以下下系统调用方法
				path = getRealPathFromURI(uri);
//				tv.setText(path);
				Toast.makeText(this, path+"222222", Toast.LENGTH_SHORT).show();
			}
		}
	}
	public String getRealPathFromURI(Uri contentUri) {
		String res = null;
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
		if(null!=cursor&&cursor.moveToFirst()){;
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			res = cursor.getString(column_index);
			cursor.close();
		}
		return res;
	}
	/**
	 * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
	 */
	@SuppressLint("NewApi")
	public String getPath(final VoiceToWord context, final Uri uri) {

		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

		// DocumentProvider
		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
			// ExternalStorageProvider
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/" + split[1];
				}
			}
			// DownloadsProvider
			else if (isDownloadsDocument(uri)) {

				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(
						Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

				return getDataColumn(context, contentUri, null, null);
			}
			// MediaProvider
			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[]{split[1]};

				return getDataColumn(context, contentUri, selection, selectionArgs);
			}
		}
		// MediaStore (and general)
		else if ("content".equalsIgnoreCase(uri.getScheme())) {
			return getDataColumn(context, uri, null, null);
		}
		// File
		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}
		return null;
	}
	/**
	 * Get the value of the data column for this Uri. This is useful for
	 * MediaStore Uris, and other file-based ContentProviders.
	 *
	 * @param context       The context.
	 * @param uri           The Uri to query.
	 * @param selection     (Optional) Filter used in the query.
	 * @param selectionArgs (Optional) Selection arguments used in the query.
	 * @return The value of the _data column, which is typically a file path.
	 */
	public String getDataColumn(Context context, Uri uri, String selection,
								String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = {column};

		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
					null);
			if (cursor != null && cursor.moveToFirst()) {
				final int column_index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(column_index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}
	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 */
	public boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}
	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 */
	public boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}
	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 */
	public boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	//获取文件名
	public String getFileName(String pathandname){
		if(pathandname == null) return null;
		int start=pathandname.lastIndexOf("/");
		int end=pathandname.lastIndexOf(".");
		if (start!=-1 && end!=-1) {
//			return pathandname.substring(start+1, end);
			return pathandname.substring(start+1);
		}
		else {
			return null;
		}
	}

	public static void verifyStoragePermissions(Activity activity) {
		// Check if we have write permission
		int permission = ActivityCompat.checkSelfPermission(activity,
				Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (permission != PackageManager.PERMISSION_GRANTED) {
			// We don't have permission so prompt the user
			ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
					REQUEST_EXTERNAL_STORAGE);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if( null != mIat ){
			// 退出时释放连接
			mIat.cancel();
			mIat.destroy();
		}
	}

	
}
