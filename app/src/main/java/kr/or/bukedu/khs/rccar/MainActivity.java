package kr.or.bukedu.khs.rccar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button connectBtn, disconnectedBtn;
    ImageButton forwardBtn, backwardBtn, leftBtn, rightBtn, stopBtn;
    TextView logTextView;
    int REQUEST_CODE_PERMISSIONS = 1000;
    BluetoothService btService;

    Handler handler;

    String FORWARD = "0";//48
    String BACKWARD="1";//49
    String LEFT="2";//50
    String RIGHT="3";//51
    String STOP="4";//52

    //음성인식을위한
    TextView textViewRecog;
    SpeechRecognizer recognizer;
    Intent recogIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logTextView = findViewById(R.id.textView);
        logTextView.setMovementMethod(new ScrollingMovementMethod());//텍스트뷰가 drag되게.
        print("onCreate()");

        AutoPermissions.Companion.loadAllPermissions(this, REQUEST_CODE_PERMISSIONS);
        //create();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        AutoPermissions.Companion.parsePermissions(
                this,
                REQUEST_CODE_PERMISSIONS,
                permissions,
                new AutoPermissionsListener() {
                    @Override
                    public void onGranted(int i, String[] strings) {
                        //허락된 퍼미션
                        print("onGranted()");
                    }

                    @Override
                    public void onDenied(int i, String[] strings) {
                        //permission
                        //거부된 퍼미션
                        print("onDenied()");
                        print("ko");
                        if (strings.length == 0)
                        {
                            //거부된 권한이 없음.
                            create();//거부된 권한이 없으므로, 정상적 동작.
                        }
                        else
                        {
                            finish();
                            print("거부된 권한이 있음. 앱은 종료하겠음.");

                        }
                    }
                }
        );


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( requestCode == btService.REQUEST_CODE_ENABLE_BT)
        {
            print("BT가 활성화되고,");
            print("주변의 기기를 찾으러 감.");
            btService.scanDevice();
        }
        else if ( requestCode == btService.REQUEST_CODE_CONNECT_DEVICE)
        {
            print("연결할 기기의 address값을 결과로 돌려받음");
            //String address = data.putExtra("address");
            if ( resultCode == RESULT_OK)
            {
                String str = data.getStringExtra("data");//BTS100\n4E:BD:50:1F:3A:27
                print(str);
//                //BTS10000\n4E:BD:50:1F:3A:27
                String address = str.substring(str.length()-17);//4E:BD:50:1F:3A:27
                btService.getDeviceInfo(address);//필요한 address값 가지고 연결하러 감.
            }
        }
    }

    void create()
    {
        //블루투스 연결.
        getWidget();
        Toast.makeText(this,
                "연결 버튼을 눌러서 블루투스가 연결되고 난 후 컨트롤 하세요. " +
                        "PIN 비밀번호는 1234 입니다.", Toast.LENGTH_LONG).show();

        handler = new MyHandler();

        if ( btService == null)
        {
            btService = new BluetoothService(this, handler);
            print("btService객체 생성.");
        }
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //블루투스 연결하세요.
                if ( btService.getDeviceState() == true)
                {
                    //블루투스가 h/w지원
                    btService.enableBluetooth();
                }
                else
                {
                    print("블루투스가 h/w지원x");
                    Toast.makeText(getApplicationContext()
                            ,R.string.do_not_support_bt, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
        disconnectedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //블루투스 연결해제
                if ( btService.getState().equals("STATE_CONNECTED"))//연결된 상태
                {
                    sendCMD(STOP);//동작을 stop시켜주고
                    btService.stop();//블루투스를 연결해제.
                    Toast.makeText(getApplicationContext(),
                            "블루투스 연결이 해제되었습니다.",
                            Toast.LENGTH_LONG).show();
                }
                else
                    Toast.makeText(getApplicationContext(),
                            "블루투스 연결 버튼을 눌러 블루투스를 연결하세요",
                            Toast.LENGTH_LONG).show();


            }
        });
        control();
        controlRecognize();//음성인식으로 컨트롤.

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if ( recognizer!= null) {
            recognizer.stopListening();
            recognizer.destroy();
        }
    }
    void controlRecognize()
    {
        print("controlRecognize");
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                print("onReadyForSpeech()");
                //준비가 되면
            }

            @Override
            public void onBeginningOfSpeech() {
                print("onBeginningOfSpeech()");
                //말이 시작될 때
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                //The sound level in the audio stream has changed.
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                print("onBufferReceived()");
            }

            @Override
            public void onEndOfSpeech() {
                print("onEndOfSpeech()");
                //말이 끝날때
            }

            @Override
            public void onError(int error) {
                print("onError()"+error);
                switch (error) {
                    case 1:
                        //Network operation timed out
                        print(" ERROR_NETWORK_TIMEOUT : 네트워크 타임아웃");
                        break;
                    case 2:
                        //Other network related errors.
                        print("ERROR_NETWORK :  그 외 네트워크 에러");
                        break;
                    case 3:
                        print( "ERROR_AUDIO :  녹음 에러");
                        //Audio recording error
                        break;
                    case 4:
                        print("ERROR_SERVER :  서버에서 에러를 보냄");
                        //Server sends error status
                        break;
                    case 5:
                        print("ERROR_CLIENT :  클라이언트 에러");
                        //Other client side errors.
                        break;
                    case 6:
                        //No speech input
                        print( "ERROR_SPEECH_TIMEOUT :  아무 음성도 듣지 못했을 때");
                        break;
                    case 7:
                        //No recognition result matched
                        print( " ERROR_NO_MATCH :  적당한 결과를 찾지 못했을 때");
                        break;
                    case 8:
                        //RecognitionService busy.
                        print( "ERROR_RECOGNIZER_BUSY :  RecognitionService가 바쁠 때");
                        break;
                    case 9:
                        //Insufficient permissions
                        print("ERROR_INSUFFICIENT_PERMISSIONS: uses-permission(즉 RECORD_AUDIO) 이 없을 때");
                        break;
                    default:
                        print( "기타에러...");
                }
                setTextViewRecog("error : "+error);
                //다시        음성인식해야함.
                startRecognize(recogIntent);
            }

            @Override
            public void onResults(Bundle results) {

                //음성인식 결과를 알려줌.
                print("onResults()");
                ArrayList<String> array =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                textViewRecog.append("\n"+"인식된 단어는 "+array.size()+"개");
                for( String str:array)
                {
                    print(str);
                    setTextViewRecog(str);//음성인식결과창에 보여주기
                }



                checkCmd(array);
                startRecognize(recogIntent);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                print("onPartialResults()");
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                print("onEvent()");
            }
        });

        print("음성인식 intent구성");
        recogIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recogIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        recogIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE , "ko-KR");//인식할 언어.
        setTextViewRecog("음성인식을 시작합니다");

        startRecognize(recogIntent);

    }
    void startRecognize(Intent recogIntent)
    {
        recognizer.startListening(recogIntent);
        print("음성인식 리스너 시작.");
        setTextViewRecog("==============\n명령어를 말하시오.");
    }
    void setTextViewRecog(String str)
    {
        textViewRecog.append("\n"+str);
    }
    void checkCmd(ArrayList<String> array)//정지, 전지,
    {
        String[][] needString = {
                {"전진", "포워드", "폴워드", "고", "가", "앞으로"},//(0,0) (0,1)
                {"후진", "백워드", "빽워드", "빽", "뒤로" },//1
                {"좌회전", "왼쪽", "레프트", "좌로"},//2
                {"우회전", "롸이트", "라이트","오른쪽으로"},//3
                {"정지", "스탑", "멈춰", "그만", "서", "스톱"}//4
        };

        for ( int i = 0; i < needString.length ; i++)//5번.
        {
            for ( int j = 0; j<needString[i].length; j++)
            {
                for ( int k = 0; k<array.size(); k++) {
                    if (needString[i][j].equals(array.get(k)))
                    {
                        //명령어 찾음.
                        //i값이 명령어.
                        print("명령어는"+i);
                        sendCMD(String.valueOf(i));//명령어 전송.
                        return;//더이상 for돌지 않고 빠져나감.
                    }
                }
            }
        }
        Toast.makeText(this, "지원되지 않는 명령어입니다.", Toast.LENGTH_LONG).show();

    }    //음성인식
    void control()
    {
        //이미지 버튼 동작 정의
        print("control()");
        forwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //전진 명령어 보내기//write, output스트림 사용.
                sendCMD(FORWARD);
            }
        });
        backwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCMD(BACKWARD);
            }
        });
        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCMD(LEFT);//"2"
            }
        });
        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCMD(RIGHT);
            }
        });
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCMD(STOP);
            }
        });
    }
    void sendCMD(String cmd)
    {
        btService.write(cmd);
    }
    void getWidget()
    {
        print("getWidget()");
        connectBtn = findViewById(R.id.button);
        disconnectedBtn = findViewById(R.id.button2);
        forwardBtn = findViewById(R.id.imageButton2);
        backwardBtn = findViewById(R.id.imageButton);
        leftBtn = findViewById(R.id.imageButton3);
        rightBtn = findViewById(R.id.imageButton4);
        stopBtn = findViewById(R.id.imageButton5);
        textViewRecog = findViewById(R.id.textView3);
        //인식된 결과 보여주는 창
        textViewRecog.setMovementMethod(new ScrollingMovementMethod());
    }
    class MyHandler extends Handler
    {
        final int MESSAGE_LOG = 3;
        final int MESSAGE_CONNECT_FAIL = 2;
        final int MESSAGE_CONNECT_SUCCESS = 1;
        final int MESSAGE_RECEIVED_DATA = 0;
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case MESSAGE_CONNECT_FAIL:
                    //에러발생 토스트 보여줘.
                    Toast.makeText(getApplicationContext(),
                            "블루투스 연결이 실패했습니다. " +
                                    "잠시 뒤 다시 블루투스 연결 버튼을 눌러주세요.",Toast.LENGTH_LONG).show();
                    break;
                case MESSAGE_CONNECT_SUCCESS:
                    //연결성공시에 토스트 보여줘.
                    Toast.makeText(getApplicationContext(),
                            "블루투스 연결이 성공했습니다.",Toast.LENGTH_LONG).show();

                    break;
                case MESSAGE_RECEIVED_DATA:
                    //조도센서 값을 텍스트뷰에 보여주고,
                    //불을 키거나 끌수 있게 스위치 뷰를 배치해서 on/off동작 정의할수 도 있다.
                    //아두이노로부터 받은 데이터를 처리하자.
                    print("데이터 받음"+msg.arg1+"/"+msg.obj);

                    break;
                case MESSAGE_LOG:
                    //블루투스 서비스 클래스의 로그는 핸들러를 통해 전달 받음.
                    logTextView.append("\n"+msg.obj);
                    break;
            }

        }
    }
    void print(String str)
    {
        Log.d("MainActivity***", str);
        logTextView.append("\n"+str);
    }
}