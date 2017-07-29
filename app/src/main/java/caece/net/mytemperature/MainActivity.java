package caece.net.mytemperature;

import android.bluetooth.BluetoothGattService;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.*;

import android.content.Context;
import android.os.Handler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.util.Log;
import android.widget.*;
import android.os.*;

public class MainActivity extends AppCompatActivity {

    public UUID serviceUUID = UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB");
    public UUID notificationUUID = UUID.fromString("02005970-6D75-4753-5053-676E6F6C7553");
    public BluetoothAdapter BTAdapter;
    public BluetoothDevice BTDevice;
    public BluetoothGattService BTGattService;
    public BluetoothGatt BTGatt;
    public BluetoothGattCharacteristic BTGattChar;
    public Handler handler;
    String TAG = "MainActivity";
    String address = "C6:05:04:03:60:F4";
    TextView status, temperature, state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothManager BTManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BTAdapter = BTManager.getAdapter();

        handler = new Handler();

        BTDevice = BTAdapter.getRemoteDevice(address);
        BTGatt = BTDevice.connectGatt(getApplicationContext(), true, GattCallback);

        status = (TextView) findViewById(R.id.status);

        temperature = (TextView) findViewById(R.id.temperature);
        temperature.setTextColor(Color.BLUE);

        state = (TextView) findViewById(R.id.state);
        state.setTextColor(Color.GREEN);

        status.setText("額溫槍已斷線");
        status.setTextColor(Color.RED);

        Log.d(TAG, "V1");
    }


    public String State(int s) {
        String memo = "";
        switch (s) {
            case 0:
                memo = "正常";
                break;
            case 1:
                memo = "測量溫度Lo";
                break;
            case 2:
                memo = "測量溫度Hi";
                break;
            case 3:
                memo = "環溫Lo";
                break;
            case 4:
                memo = "環溫Hi";
                break;
            case 5:
                memo = "EEPROM出錯";
                break;
            case 6:
                memo = "感測器出錯";
                break;
        }
        return memo;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    status.setText("額溫槍已斷線");
                    state.setText("--");
                    status.setTextColor(Color.RED);
                    break;
                case 1:
                    status.setText("額溫槍已連線");
                    status.setTextColor(Color.BLUE);
                    break;

                case 2:
                    Temperature temp = (Temperature) msg.obj;
                    temperature.setText(String.valueOf(temp.getBt()));
                    state.setText(State(temp.getState()));
                    break;
            }
        }
    };

    public BluetoothGattCallback GattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.");

                Message message = new Message();
                message.what = 0;
                mHandler.sendMessage(message);

                gatt.disconnect();
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered ACTION_GATT_SERVICES_DISCOVERED: " + status);
                List<BluetoothGattService> ls = gatt.getServices();
                Log.d(TAG, "gatt.getServices()=" + ls.size());
                for (int i = 0; i < ls.size(); i++) {
                    BluetoothGattService bs = ls.get(i);
                    UUID uuid = bs.getUuid();
                    Log.d(TAG, "onServicesDiscovered uuid: " + uuid.toString());

                }

                UUID serviceUUID = UUID.fromString("00005970-6d75-4753-5053-676e6f6c7553");

                UUID notificationUUID = UUID.fromString("02005970-6d75-4753-5053-676e6f6c7553");


                List<BluetoothGattCharacteristic> al = gatt.getService(serviceUUID).getCharacteristics();
                for (BluetoothGattCharacteristic c : al) {
                    String cuuid = c.getUuid().toString();
                    Log.d(TAG, "BluetoothGattCharacteristic : " + cuuid);
                }

                BluetoothGattCharacteristic chara = gatt.getService(serviceUUID).getCharacteristic(notificationUUID);
                gatt.setCharacteristicNotification(chara, true);


                for (BluetoothGattDescriptor c : chara.getDescriptors()) {
                    String cuuid = c.getUuid().toString();
                    Log.d(TAG, "BluetoothGattDescriptor  uuid: " + cuuid);
                }

                UUID descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

                BluetoothGattDescriptor descriptor = chara.getDescriptor(descriptorUUID);

                if (descriptor != null) {
                    Log.d(TAG, "descriptor is not null");
                } else {
                    Log.d(TAG, "descriptor is  null");
                }

                byte[] ENABLE_INDICATION_VALUE = new byte[1];
                ENABLE_INDICATION_VALUE[0] = 0x1;
                boolean s1 = descriptor.setValue(ENABLE_INDICATION_VALUE);

                Log.d(TAG, "descriptor s1=" + s1);

                //descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                boolean s2 = gatt.writeDescriptor(descriptor);
                Log.d(TAG, "descriptor s2=" + s2);

                if (s2) {
                    Message message = new Message();
                    message.what = 1;
                    mHandler.sendMessage(message);
                }


            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicRead");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //Log.d(TAG, "onCharacteristicChanged");

            byte[] data = characteristic.getValue();
            if (data.length != 8) {
//            Log.e(TAG, "長度不對");
                return;
            }
            int head = data[0] & 0xFF;
            if (head != 0xFE) {
//            Log.e(TAG, "head不對");
                return;
            }
            int cn = data[1] & 0xFF;
            if (cn != 0x6A) {
                // 忽略藍牙以外的訊息
                return;
            }
            int di = data[2] & 0xFF;
            if (di != 0x72) {
                // 忽略體溫計以外的訊息
                return;
            }
            int cmd = data[3] & 0xFF;
            if (cmd != 0x5A) {
                // 忽略手機上傳到裝置的訊息
                return;
            }
            int d3 = data[4] & 0xFF;
            int d2 = data[5] & 0xFF;
            int d1 = data[6] & 0xFF;
            int cks = data[7] & 0xFF;
            // 忽略校驗和
            if (d3 == 0x55 && d2 == 0xAA && d1 == 0xBB) {
//            Log.e(TAG, "連機命令");
            } else if (d3 == 0x55 && d2 == 0xBB && d1 == 0xBB) {
//            Log.e(TAG, "請按鍵測量");
            } else if (d3 == 0x55 && d2 == 0xCC && d1 == 0xBB) {
//            Log.e(TAG, "測量中");
            } else {
                // 結果
//            String log = Utils.toHexString(data, " ");
//            Log.e(TAG, log);
                float degree = (d3 << 8 | d2) / 100.0f;
                boolean unit = (d1 & 0x80) == 0; // true(攝氏), false(華氏), 但是原始數值其實還是攝氏, 所以這個旗標就忽略不用
                int type = (d1 & 0x60) >> 5; // 0(人體), 1(物體), 2(環境)
                int state = (d1 & 0x1C) >> 2; // 0(正常), 1(測量溫度Lo), 2(測量溫度Hi), 3(環溫Lo), 4(環溫Hi), 5(EEPROM出錯), 6(感測器出錯)
                Log.e(TAG, "degree:" + degree + " type:" + type + " state:" + state);

                Temperature temp = new Temperature();
                temp.bt = degree;
                temp.state = state;

                Message message = new Message();
                message.what = 2;
                message.obj = temp;

                mHandler.sendMessage(message);
            }
        }

    };

    public void onDestroy() {
        if (BTGatt != null) {
            BTGatt.disconnect();
            BTGatt.close();
        }
        super.onDestroy();
    }

    ;


}
