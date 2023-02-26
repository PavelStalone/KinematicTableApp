package mega.bypasha.kinematictable;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final int REQ_ENABLE_BLUETOOTH = 1001;
    public final String TAG = getClass().getSimpleName();

    private Button mode1;
    private Button mode2;
    private Button mode3;
    private Button mode4;

    private Button clockWisebtn;
    private Button counterClockWisebtn;
    private Button btnSend;

    private LinearLayout layout;
    private EditText enterturns;
    private TextView deviceInform;

    private boolean isEnableMode1 = false;
    private boolean isEnableMode2 = false;
    private boolean isEnableMode3 = false;
    private boolean isEnableMode4 = false;

    private boolean isEnableClockWise = false;
    private boolean isEnableCounterClockWise = true;

    private BluetoothAdapter mBluetoothAdapter;
    private ProgressDialog mProgressDialog;
    private ArrayList<BluetoothDevice> mDevices = new ArrayList<>();
    private DeviceListAdapter mDeviceListAdapter;

    private BluetoothSocket mBluetoothSocket;
    private OutputStream mOutputStream;

    private ListView listDevices;

    private LinearLayout.LayoutParams releaseParams;
    private LinearLayout.LayoutParams pressParams;

    private LinearLayout.LayoutParams releaseParamsClock;
    private LinearLayout.LayoutParams pressParamsClock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mode1 = findViewById(R.id.Mode1);
        mode2 = findViewById(R.id.Mode2);
        mode3 = findViewById(R.id.Mode3);
        mode4 = findViewById(R.id.Mode4);
        clockWisebtn = findViewById(R.id.clockwiseBtn);
        counterClockWisebtn = findViewById(R.id.counterclockwiseBtn);
        btnSend = findViewById(R.id.buttonSend);
        enterturns = findViewById(R.id.EnterNumberTurns);
        deviceInform = findViewById(R.id.deviceInfo);

        mode1.setOnClickListener(clickListener);
        mode2.setOnClickListener(clickListener);
        mode3.setOnClickListener(clickListener);
        mode4.setOnClickListener(clickListener);
        clockWisebtn.setOnClickListener(clickListener);
        counterClockWisebtn.setOnClickListener(clickListener);
        btnSend.setOnClickListener(clickListener);

        enterturns = findViewById(R.id.EnterNumberTurns);
        layout = findViewById(R.id.dopLayout4);

        layout.setVisibility(View.INVISIBLE);


        releaseParams = new LinearLayout.LayoutParams(GetDp(100),GetDp(100));
        pressParams = new LinearLayout.LayoutParams(GetDp(140),GetDp(140));

        releaseParamsClock = new LinearLayout.LayoutParams(GetDp(50),GetDp(50));
        pressParamsClock = new LinearLayout.LayoutParams(GetDp(70),GetDp(70));

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "onCreate: Ваше устройство не поддерживает bluetooth");
            finish();
        }

        mDeviceListAdapter = new DeviceListAdapter(this, R.layout.device_item, mDevices);

        // включаем bluetooth
        enableBluetooth();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_search:
                searchDevices();
                break;

            case R.id.item_exit:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * запускает поиск bluetooth устройств
     */
    private void searchDevices() {
        Log.d(TAG, "searchDevices()");
        enableBluetooth();

        checkPermissionLocation();

        if (!mBluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "searchDevices: начинаем поиск устройств.");
            mBluetoothAdapter.startDiscovery();
        }

        if (mBluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "searchDevices: поиск уже был запущен... перезапускаем его еще раз.");
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter.startDiscovery();
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mRecevier, filter);
    }

    /**
     * Показывает диалоговое окно со списком найденых устройств
     */
    private void showListDevices() {
        Log.d(TAG, "showListDevices()");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Найденые устройства");

        View view = getLayoutInflater().inflate(R.layout.list_devices_view, null);
        listDevices = view.findViewById(R.id.listDevices);
        listDevices.setAdapter(mDeviceListAdapter);
        listDevices.setOnItemClickListener(itemOnClickListener);

        builder.setView(view);
        builder.setNegativeButton("Закрыть", null);
        builder.create();
        builder.show();
    }


    /**
     * Проверяет разрешения на доступ к данным местоположения
     */
    private void checkPermissionLocation() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int check = checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            check += checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");

            if (check != 0) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1002);
            }
        }
    }

    /**
     * Включаем bluetooth
     */
    private void enableBluetooth() {
        Log.d(TAG, "enableBluetooth()");
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableBluetooth: Bluetooth выключен, пытаемся включить");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQ_ENABLE_BLUETOOTH);
        }
    }

    private void setMessage(String command){
        byte[] buffer = command.getBytes();

        if (mOutputStream != null){
            try {
                mOutputStream.write(buffer);
                mOutputStream.flush();
                showToastMessage("Команда отправленна");
            } catch (Exception e){
                deviceInform.setText(R.string.deviceNotConnected);
                deviceInform.setTextColor(getResources().getColor(R.color.red));
                showToastMessage("Ошибка отправки");
                e.printStackTrace();
            }

        }
    }

    private void startConnection(BluetoothDevice device) {
        if (device != null){
            try {
                Method method = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
                mBluetoothSocket = (BluetoothSocket) method.invoke(device,1);
                mBluetoothSocket.connect();

                mOutputStream = mBluetoothSocket.getOutputStream();

                deviceInform.setText(R.string.deviceConnected);
                deviceInform.setTextColor(getResources().getColor(R.color.green));
                showToastMessage("Подключение успешно!");
            } catch (Exception e){
                deviceInform.setText(R.string.deviceNotConnected);
                deviceInform.setTextColor(getResources().getColor(R.color.red));
                showToastMessage("Ошибка подключения!");
                e.printStackTrace();
            }

        }
    }

    /**
     * Показывает всплывающее текстовое сообщение
     * @param message - текст сообщения
     */
    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // принудительно пытаемся включить bluetooth
        if (requestCode == REQ_ENABLE_BLUETOOTH) {
            if (!mBluetoothAdapter.isEnabled()) {
                Log.d(TAG, "onActivityResult: Повторно пытаемся отправить запрос на включение bluetooth");
                enableBluetooth();
            }
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mOutputStream != null) {
                String comm = "";
                int direction = 0;
                int Numturns = 0;
                int mode = 0;

                if (view.equals(mode1)) {
                    isEnableMode1 = !isEnableMode1;

                    isEnableMode2 = false;
                    isEnableMode3 = false;
                    isEnableMode4 = false;

                    Log.d(TAG, "onClick: isEnableMode1 = " + isEnableMode1);
                }

                if (view.equals(mode2)) {
                    isEnableMode2 = !isEnableMode2;

                    isEnableMode1 = false;
                    isEnableMode3 = false;
                    isEnableMode4 = false;

                    Log.d(TAG, "onClick: isEnableMode2 = " + isEnableMode2);
                }

                if (view.equals(mode3)) {
                    isEnableMode3 = !isEnableMode3;

                    isEnableMode1 = false;
                    isEnableMode2 = false;
                    isEnableMode4 = false;

                    Log.d(TAG, "onClick: isEnableMode3 = " + isEnableMode3);
                }

                if (view.equals(mode4)) {
                    isEnableMode4 = !isEnableMode4;

                    isEnableMode1 = false;
                    isEnableMode2 = false;
                    isEnableMode3 = false;

                    Log.d(TAG, "onClick: isEnableMode4 = " + isEnableMode4);
                }

                if (view.equals(clockWisebtn)) {
                    isEnableClockWise = true;

                    isEnableCounterClockWise = false;

                    Log.d(TAG, "onClick: isEnableClockWise = " + isEnableClockWise);
                }
                if (view.equals(counterClockWisebtn)) {
                    isEnableCounterClockWise = true;

                    isEnableClockWise = false;

                    Log.d(TAG, "onClick: isEnableCounterClockWise = " + isEnableCounterClockWise);
                }

                if (layout.getVisibility() != View.INVISIBLE) {
                    if (isEnableClockWise) {
                        direction = 1;
                        clockWisebtn.setLayoutParams(pressParamsClock);
                    } else {
                        clockWisebtn.setLayoutParams(releaseParamsClock);
                    }
                    if (isEnableCounterClockWise) {
                        direction = 2;
                        counterClockWisebtn.setLayoutParams(pressParamsClock);
                    } else {
                        counterClockWisebtn.setLayoutParams(releaseParamsClock);
                    }
                }

                if (isEnableMode1) {
                    mode = 1;
                    mode1.setLayoutParams(pressParams);
                    comm = mode + ";0;0/";
                    setMessage(comm);
                } else {
                    mode1.setLayoutParams(releaseParams);
                }
                if (isEnableMode2) {
                    mode = 2;
                    mode2.setLayoutParams(pressParams);
                    comm = mode + ";0;0/";
                    setMessage(comm);
                } else {
                    mode2.setLayoutParams(releaseParams);
                }
                if (isEnableMode3) {
                    mode = 3;
                    mode3.setLayoutParams(pressParams);
                    comm = mode + ";0;0/";
                    setMessage(comm);;
                } else {
                    mode3.setLayoutParams(releaseParams);
                }
                if (isEnableMode4) {
                    mode = 4;
                    mode4.setLayoutParams(pressParams);
                    layout.setVisibility(View.VISIBLE);
                } else {
                    mode4.setLayoutParams(releaseParams);
                    layout.setVisibility(View.INVISIBLE);
                }

                if (view.equals(btnSend)) {
                    if (enterturns.getText().toString().trim().length() > 0) {
                        Numturns = Integer.valueOf(enterturns.getText().toString());
                        comm = mode + ";" + Numturns + ";" + direction + "/";
                        setMessage(comm);
                    } else {
                        showToastMessage("Не введены значение в поле с количеством оборотов");
                    }
                }

            } else {
                deviceInform.setText(R.string.deviceNotConnected);
                deviceInform.setTextColor(getResources().getColor(R.color.red));
                showToastMessage("Устройство не подключено");
            }
        }
    };

    private int GetDp (int px){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    private AdapterView.OnItemClickListener itemOnClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            BluetoothDevice device = mDevices.get(position);

            startConnection(device);
        }
    };

    /**
     * Отслеживаем состояния bluetooth
     * Вкл/Выкл, поиск новых устройств
     */
    private BroadcastReceiver mRecevier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // начат поиск устройств
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                Log.d(TAG, "onReceive: ACTION_DISCOVERY_STARTED");

                showToastMessage("Начат поиск устройств.");

                mProgressDialog = ProgressDialog.show(MainActivity.this, "Поиск устройств", " Пожалуйста подождите...");
            }

            // поиск устройств завершен
            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Log.d(TAG, "onReceive: ACTION_DISCOVERY_FINISHED");
                showToastMessage("Поиск устройств завершен.");

                mProgressDialog.dismiss();

                showListDevices();
            }

            // если найдено новое устройство
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                Log.d(TAG, "onReceive: ACTION_FOUND");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (!mDevices.contains(device))
                        mDeviceListAdapter.add(device);
                }
            }
        }
    };
}