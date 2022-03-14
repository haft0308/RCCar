package kr.or.bukedu.khs.rccar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService {
    BluetoothAdapter btAdapter;
    int REQUEST_CODE_ENABLE_BT = 1001;
    int REQUEST_CODE_CONNECT_DEVICE = 1002;

    Activity mActivity;

    ConnectThread mConnectThread;
    ConnectedThread mConnectedThread;

    //spp프로파일사용
    UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Handler handler;
    int MESSAGE_LOG=3;
    int MESSAGE_CONNECT_FAIL =2;
    int MESSAGE_CONNECT_SUCCESS = 1;
    int MESSAGE_RECEIVED_DATA = 0;

    int mState;
    int STATE_NONE=0;
    int STATE_LISTEN = 1;
    int STATE_CONNECTING=2;
    int STATE_CONNECTED=3;

    BluetoothService(Activity ac, Handler handler)
    {
        //print("BluetoothService() 생성자, btAdapter의 값을 얻음");
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        mActivity = ac;
        this.handler = handler;
    }
    boolean getDeviceState()
    {
        if (btAdapter == null)
        {
            // 하드웨어적으로 BT가 없음.
            return false;
        }
        else
            return true;
    }
    void enableBluetooth()
    {
        print("enableBluetooth()");
        if ( btAdapter.isEnabled())
        {
            print("BT가 활성화 되어 있는 상태");
            print("주변의 BT기기 찾으러 갑시다");
            scanDevice();
        }
        else
        {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(intent, REQUEST_CODE_ENABLE_BT);
        }
    }
    void scanDevice()
    {
        print("scanDevice() 주변의 BT기기들의 리스트를 구성후 선택을 통해 연결할 장치를 선택해옴.");
        Intent intent = new Intent(mActivity, DeviceListActivity.class);
        mActivity.startActivityForResult(intent, REQUEST_CODE_CONNECT_DEVICE);
    }
    void getDeviceInfo(String address)
    {
        print("getDeviceInfo()"+address);
        BluetoothDevice device = btAdapter.getRemoteDevice(address);//어드레스값을 통해 device값얻음
        connect(device);
    }
    //동기화 키워드
    //자원의 값을 신뢰하기 위해, 자원을 잘 분배해서 사용하기 위해.
    //동기화 방법
    //1. 함수명 앞에 synchronized키워드 붙이기
    //2. synchronized ( device) 블럭
//    {
//
//    }
    //3. notify

    String getState()
    {
        String stateStr="STATE_NONE";
        switch (mState)
        {
            case 0:
                stateStr="STATE_NONE";
                break;
            case 1:
                stateStr = "STATE_LISTEN";
                break;

            case 2:
                stateStr="STATE_CONNECTING";
                break;
            case 3:
                stateStr = "STATE_CONNECTED";
                break;
        }
        return stateStr;
    }
    void setState(int state)
    {
        print("setState() "+ mState+"->"+state);
        mState = state;
    }
    //connectThread생성.
    synchronized void connect(BluetoothDevice device)
    {
        //쓰레드의 상태를 체크해서, 연결시도중이면 초기화.
        if (getState().equals("STATE_CONNECTING") ) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if ( mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        //연결시작
        //connectThread 생성, 실행.
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);//쓰레드의 상태 지정.

    }

    synchronized void connected(BluetoothSocket socket)
    {
        //쓰레드변수 초기화.
        start();
        //스트림을 통해 데이터 송수신.
        //connectedThread 생성, 실행.
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        setState(STATE_CONNECTED);
    }
    class ConnectThread extends Thread{
        BluetoothDevice mDevice;
        @SuppressLint("MissingPermission") BluetoothSocket mmSocket;

        ConnectThread(BluetoothDevice device)
        {
            mDevice = device;
            //RFComm : 안드로이드 API가 지원하는 블루투스 소켓의 가장 일반적인 유형
            //          블루투스를 통한 연결 지향 스트리밍 전송
            //          = SPP시리얼 데이터 전송

            try {

                   mmSocket     = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
                print("소켓 잘 만들었음");
            } catch (IOException e) {
                print("소켓 만들다가 에러발생.");
                //사용자에게 연결에러 발생을 알려주고, 다시 연결작업을 시도하도록 유도.
                //유저에게 화면으로 알려주자.
                //서브쓰레드에서 UI작업은 안됨. UI쓰레드(Main쓰레드)에게 작업 요청.
                //메세지로 메인쓰레드에게 요청하겠음.
                //쓰레드 간의 통신은 Handler이용.
                Message msg = handler.obtainMessage();
                msg.what = MESSAGE_CONNECT_FAIL;
                handler.sendMessage(msg);

                //handler.obtainMessage(MESSAGE_CONNECT_FAIL).sendToTarget();

                e.printStackTrace();

            }
        }
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            super.run();
            print("이 시점에도 장치를 찾고 있다면, 멈추시오, 연결이 잘 안됩니다.");
            btAdapter.cancelDiscovery();//주변의 장치를 찾는 것을 중단시킴.
            try {
                mmSocket.connect();
                print("소켓으로 연결 성공.");
            } catch (IOException e) {
                print("소켓연결실패");
                handler.obtainMessage(MESSAGE_CONNECT_FAIL).sendToTarget();
                e.printStackTrace();

                //에러가 나서 소켓 정리함.
                connectionFailed();

                try {
                    mmSocket.close();
                    print("소켓닫기 성공.");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    print("소켓닫기 실패");
                }

                print("쓰레드 초기화");
                BluetoothService.this.start();
                return;
            }

            //연결 성공. 동작 정의
            synchronized (BluetoothService.this)
            {
                mConnectThread = null;
            }
            //연결이 되었으니, 소켓으로부터 (1:1회선연결)스트림정보 얻어 데이터 송수신하러가자.
            connected(mmSocket);

        }
        public void cancel()
        {
            print("ConnectThread cancel()");
            try {
                mmSocket.close();
                print("소켓 잘 닫음.");
            } catch (IOException e) {
                e.printStackTrace();
                print("소켓 닫기 에러.");
            }
        }
    }//ConnectThread

    synchronized void start()
    {
        print("start. 쓰레드변수 초기화");
        if ( mConnectThread != null)
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if( mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }
    void connectionFailed()
    {
        print("connectionFailed()");
        setState(STATE_LISTEN);
    }
    class ConnectedThread extends Thread
    {
        BluetoothSocket mmSocket;
        InputStream mmInput;
        OutputStream mmOutput;

        ConnectedThread(BluetoothSocket socket)
        {
            mmSocket = socket;
            try {
                mmInput = mmSocket.getInputStream();
                print("소켓으로부터 input스트림을 얻음.");
                mmOutput = mmSocket.getOutputStream();//출력스트림.
                print("소켓으로부터 output스트림을 얻음.");
                //유저에게 연결되었음을 알려줌. 서브쓰레드라, UI쓰레드에게 요청.
                handler.obtainMessage(MESSAGE_CONNECT_SUCCESS).sendToTarget();


            } catch (IOException e) {
                print("스트림 정보를 얻지 못함.");
                //유저에게 연결되었음을 알려줌. 서브쓰레드라, UI쓰레드에게 요청.
                handler.obtainMessage(MESSAGE_CONNECT_FAIL).sendToTarget();

                e.printStackTrace();
            }
        }


        @Override
        public void run() {
            super.run();
            //스트림 정보가 있으니, 아두이노가 보내주는 데이터
            // (ex. 아두이노의 조도센서 값을 읽어서 안드로이드에게 보내줌)
            byte[] buffer = new byte[256];
            int byte_size;
            while (true)
            {
                try {
                    byte_size = mmInput.read(buffer);
                    print("잘 읽었음.buffer를. 아두이노가 보내주는 데이터를 읽어 담았음.");
                    handler.obtainMessage(MESSAGE_RECEIVED_DATA, byte_size,0, buffer).sendToTarget();
                } catch (IOException e) {

                    print("읽다가 에러발생");
                    connectionFailed();
                    e.printStackTrace();
                    break;//에러발생해서 무한루프 종료.=>쓰레드를 종료시킴.
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
        public void cancel()
        {
            print("connectedThread cancel()");
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }//cancel()
        public void write(byte[] out)
        {
            try {
                mmOutput.write(out);
                print("write()데이터 송신 성공"+out[0]);
            } catch (IOException e) {
                print("write()데이터 송신 실패");
                e.printStackTrace();
            }
        }

    }//ConnectedThread
    void write(String cmd)//"2"
    {
        byte[] out = cmd.getBytes();//byte[]로 만들어줌.
        print("out[0]="+out[0]);//49, 52

        ConnectedThread r;//mConnectedThread변수의 값을 잠시 담을 임시변수
        synchronized (this) {// 동기화처리 하고 copy함.
            if ( getState().equals("STATE_CONNECTED"))
                r = mConnectedThread;
            else
                return;

            r.write(out);
        }
    }
    void stop()
    {
        print("stop()");
        start();//쓰레드변수 초기화
        setState(STATE_NONE);
    }
    void print(String str)
    {
        Log.d("BluetoothService***", str);
        handler.obtainMessage(MESSAGE_LOG, "BluetoothService***"+str).sendToTarget();
    }
}
