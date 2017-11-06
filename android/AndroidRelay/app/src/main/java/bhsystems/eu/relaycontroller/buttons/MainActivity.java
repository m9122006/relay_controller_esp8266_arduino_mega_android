package bhsystems.eu.relaycontroller.buttons;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import bhsystems.eu.relaycontroller.R;
import bhsystems.eu.relaycontroller.application.RelayControllerApplication;
import bhsystems.eu.relaycontroller.entity.RelayControllerButton;

public class MainActivity extends AppCompatActivity implements ButtonsAdapter.ButtonSelectedListener {
    private static final int BUTTON_REQUEST_CODE = 1;
    public static final String CONTROLLER_NAME = "relaycontroller";
    public static final int RESQUEST_GPIO_STATES = -1;
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private NsdServiceInfo mServiceInfo;
    public String controllerIp;
    private static final String SERVICE_TYPE = "_http._tcp.";


    private RecyclerView rvButtons;
    private ButtonsAdapter buttonsAdapter;

    private ArrayList<RelayControllerButton> buttons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.app_name);
        rvButtons = findViewById(R.id.rv_buttons);
        new ButtonsRepository(this).execute();
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(MainActivity.this, NewButtonActivity.class), BUTTON_REQUEST_CODE);
            }
        });

        mNsdManager = (NsdManager) (getApplicationContext().getSystemService(Context.NSD_SERVICE));
        initializeResolveListener();
        initializeDiscoveryListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

    }


    private void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                String name = service.getServiceName();
                String type = service.getServiceType();
                Log.d("NSD", "Service Name=" + name);
                Log.d("NSD", "Service Type=" + type);
                if (type.equals(SERVICE_TYPE) && name.contains(CONTROLLER_NAME)) {
                    Log.d("NSD", "Service Found @ '" + name + "'");
                    mNsdManager.resolveService(service, mResolveListener);

                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    private void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e("NSD", "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                mServiceInfo = serviceInfo;
                InetAddress host = mServiceInfo.getHost();
                String address = host.getHostAddress();
                Log.d("NSD", "Resolved address = " + address);
                controllerIp = address;
                sendRequestToController(RESQUEST_GPIO_STATES);
            }
        };
    }


    private void prepareRecycleView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvButtons.addItemDecoration(new DividerItemDecoration(this, linearLayoutManager.getOrientation()));
        rvButtons.setLayoutManager(linearLayoutManager);
        buttonsAdapter = new ButtonsAdapter(this);
        buttonsAdapter.addAll(buttons);
        rvButtons.setAdapter(buttonsAdapter);
    }

    @Override
    public void onButtonClicked(RelayControllerButton relayControllerButton) {
        sendRequestToController(relayControllerButton.getPin());
    }

    private void sendRequestToController(int gpIO) {
        if (controllerIp == null) {
            Toast.makeText(getApplicationContext(), "Controlador não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://" + controllerIp + "/" + gpIO;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i("RESP", response);
                        if (response.length() == 30) {
                            for (int i = 0; i < response.length() - 1; i++) {
                                char s = response.charAt(i);
                                if (s != '1' && s != '0') {
                                    Toast.makeText(getApplicationContext(), "Erro - Estado inválido", Toast.LENGTH_SHORT).show();
                                    break;
                                }
                                int itemIndex = 0;
                                for (RelayControllerButton button : buttons) {
                                    if (button.getPin() == i + 23) {
                                        if (button.getRelayControllerButtonType() == RelayControllerButton.RelayControllerButtonType.TOGGLE) {
                                            button.setActive(s == '1');
                                            buttonsAdapter.notifyItemChanged(itemIndex);
                                        }
                                    }
                                    itemIndex++;
                                }

                            }

                        } else {
                            Toast.makeText(getApplicationContext(), "Erro - Tamanho da resposta inválido", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("NSD", error.getMessage());
            }
        });
        queue.add(stringRequest);
    }

    private static class ButtonsRepository extends AsyncTask<Void, Void, List<RelayControllerButton>> {

        private WeakReference<MainActivity> activityReference;

        // only retain a weak reference to the activity
        ButtonsRepository(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected List<RelayControllerButton> doInBackground(Void... params) {
            return RelayControllerApplication.getInstance().getDb().relayControllerButtonDao().getAll();
        }

        @Override
        protected void onPostExecute(List<RelayControllerButton> result) {
            MainActivity activity = activityReference.get();
            if (activity == null) return;
            activity.buttons.addAll(result);
            activity.prepareRecycleView();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BUTTON_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    new ButtonsRepository(this).execute();
                    controllerIp = "";
                    mNsdManager = (NsdManager) (getApplicationContext().getSystemService(Context.NSD_SERVICE));
                    initializeResolveListener();
                    initializeDiscoveryListener();
                    mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}
