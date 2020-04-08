package com.example.voicemaster.voicechange;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.voicemaster.R;

import java.io.File;

public class ChangeVoive extends AppCompatActivity {

    private static final String TAG = "cypress";
    private static String fileName = null;

    //选择文件的路径
    public String path = null;

    private Button btn_choose;
    private Button btn_record;
    private Button btn_stop;

    private String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    private static final int REQUEST_PERMISSIONS = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_change);
        if (ContextCompat.checkSelfPermission(ChangeVoive.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(ChangeVoive.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(ChangeVoive.this, "用户曾拒绝权限", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(ChangeVoive.this, permissions, REQUEST_PERMISSIONS);
            }
        }

        btn_choose = (Button) findViewById(R.id.btn_CVchoose);
        btn_record = (Button) findViewById(R.id.btn_recordStart);
        btn_stop = (Button) findViewById(R.id.btn_stopRecord);

        btn_choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //通过文件管理器读取手机上的文件
                Log.d(TAG, "onClick: 通过文件管理器读取手机上的文件");
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*"); //选择音频
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, 1);
                Log.d(TAG, "onClick: path = " + path);
            }
        });

        btn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "voice_master");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File fileWav = new File(dir, System.currentTimeMillis() + "BaiVC.wav");
                File filePcm = new File(dir, System.currentTimeMillis() + "BaiVC.pcm");
                Log.d(TAG, "生成文件，名字是: " + fileWav.toString());
                String storePathWav = fileWav.toString();
                String storePathPcm = filePcm.toString();
                path = storePathWav;
                AudioRecordUtils.getInstance().startRecord(storePathPcm,storePathWav);
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioRecordUtils.getInstance().stopRecord();
                Toast.makeText(ChangeVoive.this, "录音完毕 ： " + path, Toast.LENGTH_SHORT).show();
            }
        });

        //开个线程读文件
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    InputStream inputStream =  getResources().openRawResource(R.raw.voice);
//                    File file = new File(Environment.getExternalStorageDirectory().getPath(),"voice.wav");
//                    if(!file.exists()){
//                        file.createNewFile();
//                    }
//                    FileOutputStream fileOutputStream = new FileOutputStream(file);
//                    byte[] buffer = new byte[10];
//                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                    int len = 0;
//                    while ((len=inputStream.read(buffer))!=-1){
//                        outputStream.write(buffer,0,len);
//                    }
//                    byte[] bs = outputStream.toByteArray();
//                    fileOutputStream.write(bs);
//                    outputStream.close();
//                    inputStream.close();
//                    fileOutputStream.flush();
//                    fileOutputStream.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Log.i("getPath",e.getMessage());
//                }
//            }
//        }).start();

    }

    public void startChange(View view) {
        if (path == null || path.equals("")) {
            Toast.makeText(this, "别急，还没选择文件呢", Toast.LENGTH_SHORT).show();
        } else {
            // tring path = Environment.getExternalStorageDirectory().getPath() + File.separator + "voice.wav";
            // Log.i("getPath", path);
            Toast.makeText(this, "文件是path " + path, Toast.LENGTH_SHORT).show();
            File file = new File(path);
            if (!file.exists()) {
                Log.e("Main", "没有文件");
                Toast.makeText(this, "文件是不存在", Toast.LENGTH_SHORT).show();
                return;
            }
            switch (view.getId()) {
                // 普通
                case R.id.btn_normal:
                    VoiceTools.changeVoice(path, 0);
                    break;
                // 萝莉
                case R.id.btn_luoli:
                    VoiceTools.changeVoice(path, 1);
                    break;// 萝莉
                // 大叔
                case R.id.btn_dashu:
                    VoiceTools.changeVoice(path, 2);
                    break;
                // 惊悚
                case R.id.btn_jingsong:
                    VoiceTools.changeVoice(path, 3);
                    break;
                // 搞怪
                case R.id.btn_gaoguai:
                    VoiceTools.changeVoice(path, 4);
                    break;
                // 空灵
                case R.id.btn_kongling:
                    VoiceTools.changeVoice(path, 5);
                    break;
            }
        }
    }

    //文件管理器获得选择文件的路径
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if ("file".equalsIgnoreCase(uri.getScheme())) {//使用第三方应用打开
                path = uri.getPath();
//				tv.setText(path);
                Toast.makeText(this, path + "11111", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                path = getPath(ChangeVoive.this, uri);
//				tv.setText(path);
                Toast.makeText(this, path, Toast.LENGTH_SHORT).show();
            } else {//4.4以下下系统调用方法
                path = getRealPathFromURI(uri);
//				tv.setText(path);
                Toast.makeText(this, path + "222222", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (null != cursor && cursor.moveToFirst()) {
            ;
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
    public String getPath(final ChangeVoive context, final Uri uri) {

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

}
