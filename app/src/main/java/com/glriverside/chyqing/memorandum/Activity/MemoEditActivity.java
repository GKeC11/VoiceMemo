package com.glriverside.chyqing.memorandum.Activity;

import android.app.AlarmManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glriverside.chyqing.memorandum.JsonParser;
import com.glriverside.chyqing.memorandum.Contract.MemoContract;
import com.glriverside.chyqing.memorandum.Manager.MemoOpenHelper;
import com.glriverside.chyqing.memorandum.R;
import com.glriverside.chyqing.memorandum.Values.MemoValues;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.ui.RecognizerDialog;

import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MemoEditActivity extends AppCompatActivity {

    private MemoOpenHelper memoOpenHelper;
    private ImageView ivSave;
    private ImageView ivAlarm;
    private ImageView ivToDo;
    private ImageView ivSpeak;
    private EditText etTitle;
    private EditText etContent;
    private TextView tvDate;
    private BottomNavigationView nvMemoContent;
    private Toolbar edit_toolbar;
    private TextView edit_toolbar_text;

    private Boolean isAlarm = false;
    private Boolean isToDo = false;
    private String alarmTime = null;
    private MemoValues memoValues;
    private int mPercentForBuffering;
    private int mPercentForPlaying;
    private ImageButton toolbar_btn;

    //新建或修改
    private Boolean model = false;

    String mTag = "voice_activity";
    Toast mToast;
    int ret=0;
    //EditText myEditText;
    private SpeechRecognizer mIat;
    private SpeechSynthesizer mTts;

    private AlarmManager amMemo;

    private View.OnClickListener save = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //获取可写数据库对象
            SQLiteDatabase db = memoOpenHelper.getWritableDatabase();

            //实例化一个ContentValues
            ContentValues contentValues = new ContentValues();

            String title = etTitle.getText().toString();
            String content = etContent.getText().toString();
            String time = tvDate.getText().toString();
            String alarmTime = "";

            if ("".equals(title)) {
                Toast.makeText(MemoEditActivity.this, "标题不能为空", Toast.LENGTH_LONG).show();
                return;
            }

            if ("".equals(content)) {
                Toast.makeText(MemoEditActivity.this, "内容不能为空", Toast.LENGTH_LONG).show();
                return;
            }

            //将数据存入ContentValues
            contentValues.put(MemoContract.MemoEntry.COLUMN_NAME_TITLE, title);
            contentValues.put(MemoContract.MemoEntry.COLUMN_NAME_CONTENT_PATH, content);
            contentValues.put(MemoContract.MemoEntry.COLUMN_NAME_DATE, time);
            contentValues.put(MemoContract.MemoEntry.COLUMN_NAME_ALARM, isAlarm);
            contentValues.put(MemoContract.MemoEntry.COLUMN_NAME_TODO, isToDo);

            if (isAlarm == true) {
                contentValues.put(MemoContract.MemoEntry.COLUMN_NAME_ALARM_TIME, alarmTime);
            }

            //插入数据库
            db.insert(MemoContract.MemoEntry.TABLE_NAME, null, contentValues);

            //显示Toast消息
            Toast.makeText(MemoEditActivity.this, "保存成功", Toast.LENGTH_LONG).show();

            //返回显示列表
            Intent intent = new Intent(MemoEditActivity.this, MemoActivity.class);
            startActivity(intent);
            db.close();
        }
    };

    private View.OnClickListener modification = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //获取可写数据库对象
            SQLiteDatabase db = memoOpenHelper.getWritableDatabase();

            //实例化一个ContentValues
            ContentValues contentValues = new ContentValues();

            String title = etTitle.getText().toString();
            String content = etContent.getText().toString();
            String time = tvDate.getText().toString();
            String alarmTime = "";

            if ("".equals(title)){
                Toast.makeText(MemoEditActivity.this, "标题不能为空", Toast.LENGTH_LONG).show();
                return;
            }

            if ("".equals(content)){
                Toast.makeText(MemoEditActivity.this, "内容不能为空", Toast.LENGTH_LONG).show();
                return;
            }

            //将数据存入ContentValues
            contentValues.put(MemoContract.MemoEntry.COLUMN_NAME_TITLE, title);
            contentValues.put(MemoContract.MemoEntry.COLUMN_NAME_CONTENT_PATH, content);
            contentValues.put(MemoContract.MemoEntry.COLUMN_NAME_DATE, time);
            contentValues.put(MemoContract.MemoEntry.COLUMN_NAME_ALARM,isAlarm);
            contentValues.put(MemoContract.MemoEntry.COLUMN_NAME_TODO, isToDo);

            /*if(isAlarm == true){
                contentValues.put(MemoContract.MemoEntry.COLUMN_NAME_ALARM_TIME, alarmTime);
            }*/

            //更新数据库
            db.update(MemoContract.MemoEntry.TABLE_NAME, contentValues,
                    MemoContract.MemoEntry._ID + "=?",
                    new String[]{memoValues.getId().toString()});

            //显示Toast消息
            Toast.makeText(MemoEditActivity.this, "更新成功", Toast.LENGTH_LONG).show();

            //返回显示列表
            Intent intent = new Intent(MemoEditActivity.this, MemoActivity.class);
            startActivity(intent);
            db.close();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memo_content);

        init();

        etTitle = findViewById(R.id.et_memo_title);
        etContent = findViewById(R.id.et_memo_content);
        tvDate = findViewById(R.id.tv_time);

        nvMemoContent = findViewById(R.id.memo_content_navigation);
        LayoutInflater.from(MemoEditActivity.this).inflate(R.layout.memo_content_navigation, nvMemoContent, true);
        ivSave = nvMemoContent.findViewById(R.id.iv_save);
        ivAlarm = nvMemoContent.findViewById(R.id.iv_alarm);
        ivToDo = nvMemoContent.findViewById(R.id.iv_todo);
        ivSpeak = nvMemoContent.findViewById(R.id.iv_speak);

        edit_toolbar_text = findViewById(R.id.text_toolbar);

        Log.d(mTag, "onCreate: logcat work");

        ImageView ivVoice = nvMemoContent.findViewById(R.id.iv_voice);

        mIat = SpeechRecognizer.createRecognizer(MemoEditActivity.this,mInitListener);
        mTts = SpeechSynthesizer.createSynthesizer(this,mInitListener);

        mToast = Toast.makeText(this,"",Toast.LENGTH_SHORT);

        Intent intent = this.getIntent();
        if (intent != null){
            model = Boolean.valueOf(intent.getStringExtra(MemoActivity.MODEL));
        }

        //获取当前时间
        tvDate.setText(getTime());
        if (model == false){
            edit();
        }else{
            alter();
        }
    }

    public void init(){
        toolbar_btn = findViewById(R.id.toobar_btn);

        toolbar_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    //添加功能栏
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu,menu);
        return true;
    }

    public void edit(){
        memoOpenHelper = new MemoOpenHelper(MemoEditActivity.this);
        SQLiteDatabase db = memoOpenHelper.getReadableDatabase();

        //保存
        ivSave.setOnClickListener(save);

        ivAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isAlarm = !isAlarm;
                if (isAlarm == true){
                    //设置时间
                }else{
                    //取消
                }

            }
        });

        ivToDo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isToDo = !isToDo;
                if (isToDo ==true){
                    //后台显示
                }else{
                    //不显示
                }
            }
        });
    }

    public void alter(){
        memoOpenHelper = new MemoOpenHelper(MemoEditActivity.this);

        Intent intent = this.getIntent();
        if (intent != null){
            memoValues = new MemoValues();
            memoValues.setTitle(intent.getStringExtra(MemoContract.MemoEntry.COLUMN_NAME_TITLE));
            //memoValues.setDate(intent.getStringExtra(MemoContract.MemoEntry.COLUMN_NAME_DATE));
            memoValues.setContent(intent.getStringExtra(MemoContract.MemoEntry.COLUMN_NAME_CONTENT_PATH));
            //memoValues.setAlarm(Boolean.valueOf(intent.getStringExtra(MemoContract.MemoEntry.COLUMN_NAME_ALARM)));
            memoValues.setAlarmTime(intent.getStringExtra(MemoContract.MemoEntry.COLUMN_NAME_ALARM_TIME));
            //memoValues.setToDo(Boolean.valueOf(intent.getStringExtra(MemoContract.MemoEntry.COLUMN_NAME_TODO)));
            memoValues.setId(Integer.valueOf(intent.getStringExtra(MemoContract.MemoEntry._ID)));

            etTitle.setText(memoValues.getTitle());
            tvDate.setText(getTime());
            etContent.setText(memoValues.getContent());
            isAlarm =  memoValues.getAlarm();
            alarmTime = memoValues.getAlarmTime();
            isToDo = memoValues.getToDo();

            edit_toolbar_text.setText(memoValues.getTitle());
        }

        ivSave.setOnClickListener(modification);

        //执行语音合成功能
        ivSpeak.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                read();
                Log.d(mTag, "onClick: speak start");
            }
        });
    }

    private String getTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String str = sdf.format(date);
        return str;
    }

    //朗读功能
    public void read(){
        String text = etContent.getText().toString();
        setSynthesisParam();
        int code = mTts.startSpeaking(text,mTtsListener);
    }

    //语音输入
    public void voice_btn_click(View view) {
        if(mIat == null){
            this.showTip("创建对象失败");
            Log.d(mTag, "voice_btn_click: ");
            return;
        }
        setParam();
        ret = mIat.startListening(recognizerListener);
        if(ret!= ErrorCode.SUCCESS){
            Log.d("voice_btn", "failure");
        }
        else{
            Log.d("voice_btn", "begin");
        }
        //Log.d("voice_btn", "voice_btn_click: ");
    }

    public void showTip(final String str){
        mToast.setText(str);
        mToast.show();
    }

    public void setSynthesisParam() {
        Log.d(mTag, "setSynthesisParam: ");
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        mTts.setParameter(SpeechConstant.SPEED, "50");
        mTts.setParameter(SpeechConstant.PITCH, "50");
        mTts.setParameter(SpeechConstant.VOLUME, "50");
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
    }

    public void setParam(){
        mIat.setParameter(SpeechConstant.CLOUD_GRAMMAR,null);
        mIat.setParameter(SpeechConstant.SUBJECT,null);
        mIat.setParameter(SpeechConstant.RESULT_TYPE,"json");
        mIat.setParameter(SpeechConstant.ENGINE_TYPE,"cloud");
        mIat.setParameter(SpeechConstant.LANGUAGE,"zh_cn");
        mIat.setParameter(SpeechConstant.ACCENT,"mandarin");
        mIat.setParameter(SpeechConstant.VAD_BOS,"4000");
        mIat.setParameter(SpeechConstant.VAD_EOS,"1000");
        mIat.setParameter(SpeechConstant.ASR_PTT,"1");
    }

    private RecognizerListener recognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
            showTip("当前正在说话，音量：" + i);
        }

        @Override
        public void onBeginOfSpeech() {
            showTip("开始说话");
        }

        @Override
        public void onEndOfSpeech() {
            showTip("结束说话");
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            String text = JsonParser.parseIatResult(recognizerResult.getResultString());
            etContent.append(text);
            etContent.setSelection(etContent.length());
        }

        @Override
        public void onError(SpeechError speechError) {
            Log.e(mTag, "onError: " );
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
            Log.e(mTag, "onEvent: " );
        }
    };

    private SynthesizerListener mTtsListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
            showTip("开始朗读");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
            mPercentForBuffering = percent;
            showTip(String.format("缓冲进度为%d%%", mPercentForBuffering));
        }

        @Override
        public void onSpeakPaused() {
            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            showTip("继续播放");
        }

        @Override
        public void onSpeakProgress(int i, int i1, int i2) {
            mPercentForPlaying = i;
            showTip(String.format("播放进度为%d%%", mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError speechError) {
            showTip("播放完成");
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
            Log.e(mTag, "onEvent: ");
        }
    };

    //初始化语音听写对象
    private InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int i) {
            Log.d(mTag, "onInit: "+i);
            if(i!=ErrorCode.SUCCESS){
                showTip("Failure");
            }
        }
    };

}
