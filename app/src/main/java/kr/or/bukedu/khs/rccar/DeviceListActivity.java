package kr.or.bukedu.khs.rccar;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    Button scanBtn;
    TextView titlePairedDevices, titleNewDevices;
    ListView pairedDevicesListView, newDevicesListView;
    BluetoothAdapter mBtAdapter;

    BroadcastReceiver receiver;

    ArrayAdapter<String> mNewDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //윈도우 셋팅 관련 작업은 setContentView()보다 먼저 호출되어야.
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_device_list);

        print("onCreate()");
        setResult(RESULT_CANCELED);//액티비티 응답에 대한 default값을 지정.
                                    //사용자가 기기를 선택한 경우에 RESULT_OK로 응답을 줄 예정.

        titlePairedDevices = findViewById(R.id.textView5);
        titleNewDevices=findViewById(R.id.textView6);
        pairedDevicesListView = findViewById(R.id.pairedList);
        newDevicesListView=findViewById(R.id.newList);
        scanBtn = findViewById(R.id.button3);

        //리스트뷰 구성하기
        //이미 페어링된 bt 기기들의 리스트를 구성하자.
        //보여지는 것은 리스트뷰가 담당.
        //데이터의 어댑터가 리스트에서 다루는 데이터를 관리.
        //둘이 연동할 수 있게 하자.

        ArrayAdapter<String> pairedDevicesArrayAdapter = new ArrayAdapter<String>(
                this, R.layout.device_name
        );//45:46:12:45:

        //이미 페어링된 기기들의 정보를 받아서 보여주자.

        pairedDevicesListView.setAdapter(pairedDevicesArrayAdapter);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        @SuppressLint("MissingPermission") Set<BluetoothDevice> set = mBtAdapter.getBondedDevices();//이미 페어링되었던 기기들의 set
        if ( set.size() == 0)
        {
            print("페어링된 기기가 없다.");
            pairedDevicesArrayAdapter.add("페어링된 장치가 없음");
        }
        else {
            print("페어링된 기기들이 있다.");
            for (BluetoothDevice device : set) {
                String str = device.getName() + "\n" + device.getAddress();
                pairedDevicesArrayAdapter.add(str);
            }
        }

        mNewDevicesArrayAdapter = new ArrayAdapter<>(
                this, R.layout.device_name
        );

        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);

        setTitle("기기를 선택하거나, 찾아서 선택하세요.");
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTitle("기기를 찾는 중...");
                v.setVisibility(View.GONE);//기기 찾기 버튼 안 보이게.
                titlePairedDevices.setVisibility(View.VISIBLE);
                titleNewDevices.setVisibility(View.VISIBLE);
                setProgressBarIndeterminateVisibility(true);
                doDiscovery();//주변의 BT장치를 찾자.
            }
        });

        //시스템이 이벤트가 발생하면, 브로드캐스트 형태로 보내주는데,
        //내가 원하는 브로드캐스트를 intentFilter로 등록해두고,
        //브로드캐스트 리시버를 만들어서 브로드캐스트를 받는다.
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ( intent.getAction().equals(BluetoothDevice.ACTION_FOUND))
                {
                    print("주변의 BT기기 하나 찾음");
                    //찾은 기기 정보를 두번째 새 기기들의 리스트뷰에 아이템 추가.
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String str = device.getName() + "\n"+device.getAddress();
                    if ( device.getBondState()!=BluetoothDevice.BOND_BONDED) {
                        print("이미 페어링된 기기가 아니라면, 새 기기리스트에 넣어줘.");
                        mNewDevicesArrayAdapter.add(str);//BTS100\n11:55:44:12:46
                    }
                }
                else if ( intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
                {
                    print("주변의 기기들을 다 찾았다고 브로드캐스트 받음.");
                    if(mNewDevicesArrayAdapter.getCount() == 0)
                    {
                        mNewDevicesArrayAdapter.add("기기가 없음");
                        print("주변에 연결가능한 새 기기가 없음.");
                    }

                    setTitle("연결할 기기를 선택해주세요.");
                    setProgressBarIndeterminateVisibility(false);
                    //유저가 기기 선택하기를 유도.
                }


            }
        };
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);//주변의 장치 찾음.
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(receiver, intentFilter);//내가 받고자 하는 브로드캐스트 등록.

        //아이템을 선택하면 어드레스 값 가져다가 응답으로 돌려줌.
        ItemClickListener itemClickListener = new ItemClickListener();
        pairedDevicesListView.setOnItemClickListener(itemClickListener);
        newDevicesListView.setOnItemClickListener(itemClickListener);

    }//onCreate()

    class ItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String str = (String)parent.getItemAtPosition(position);
            Intent intent = new Intent();
            intent.putExtra("data", str);//BTS100\n45:54:12:45;
            setResult(RESULT_OK, intent);
            finish();///DeviceListActivity종료해주어야 응답결과가 main으로 전달됨.
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if ( receiver != null)//등록된 경우만.
            unregisterReceiver(receiver);//등록한 브로드캐스트를 해제
    }

    void doDiscovery()
    {
        print("doDiscovery()");
        //
        if (mBtAdapter.isDiscovering())//이제 찾으러 왔는데, 찾고 있다면,
        {
            print("이전에 기기를 찾던 동작이 이어지고 있어, 취소함.");
            mBtAdapter.cancelDiscovery();
        }
        mBtAdapter.startDiscovery();
        print("이제서야, 주변기기 찾기 시작됨.");

    }
    void print(String str)
    {
        Log.d("DeviceListActivity***", str);
    }
}