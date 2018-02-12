package com.prohua.socketdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;
import com.prohua.universal.DefaultAdapter;
import com.prohua.universal.DefaultViewHolder;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {

    private String no;
    private RecyclerView recyclerView;
    private EditText editTextId;
    private EditText editText;
    private Button button;

    private List<String> stringList;

    private DefaultAdapter defaultAdapter;

    private final int NORMAL_CLOSURE_STATUS = 1000;

    private OkHttpClient sClient;
    private WebSocket sWebSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextId = (EditText) findViewById(R.id.sendContentId);
        editText = (EditText) findViewById(R.id.sendContent);
        button = (Button) findViewById(R.id.sendButton);

        no = android.os.Build.MODEL;

        editTextId.setText(no);

        recyclerView = (RecyclerView) findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));

        stringList = new ArrayList<>();

        defaultAdapter = new DefaultAdapter(getBaseContext(), stringList, R.layout.string_recycler_item, 0, 0);

        defaultAdapter.setOnBindItemView(new DefaultAdapter.OnBindItemView() {
            @Override
            public void onBindItemViewHolder(DefaultViewHolder defaultViewHolder, int i) {
                defaultViewHolder.setText(R.id.listContentText, stringList.get(i));
            }
        });
        recyclerView.setAdapter(defaultAdapter);

        // 初始化连接
        startRequest();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MessageBean messageBean = new MessageBean(no,
                        no, editText.getText().toString().trim());
                Gson gson = new Gson();
                String msg = gson.toJson(messageBean);
                sendMessage(msg);
                editText.setText("");
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        closeWebSocket();

        destroy();
    }

    public synchronized void startRequest() {
        if (sClient == null) {
            sClient = new OkHttpClient();
        }
        if (sWebSocket == null) {
            Request request = new Request.Builder().url("ws://192.168.31.129:2000").build();

            EchoWebSocketListener listener = new EchoWebSocketListener();
            sWebSocket = sClient.newWebSocket(request, listener);
        }
    }

    private void sendMessage(WebSocket webSocket, final String msg) {
        webSocket.send(msg);
    }

    public void sendMessage(String msg) {
        WebSocket webSocket;
        synchronized (MainActivity.class) {
            webSocket = sWebSocket;
        }
        if (webSocket != null) {
            sendMessage(webSocket, msg);
        }
    }


    public synchronized void closeWebSocket() {
        if (sWebSocket != null) {
            sWebSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye!");
            sWebSocket = null;
        }
    }

    public synchronized void destroy() {
        if (sClient != null) {
            sClient.dispatcher().executorService().shutdown();
            sClient = null;
        }
    }

    private void resetWebSocket() {
        synchronized (MainActivity.class) {
            sWebSocket = null;
        }
    }

    private void addMessage(String msg) {
        Gson gson = new Gson();
        final MessageBean messageBean = gson.fromJson(msg, MessageBean.class);

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stringList.add(messageBean.getId() + ":" + messageBean.getMsg());
                defaultAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(stringList.size() - 1);
            }
        });
    }

    private void addMessageOrdinary(final String msg) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stringList.add(msg);
                defaultAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(stringList.size() - 1);
            }
        });
    }

    public class EchoWebSocketListener extends WebSocketListener {
        private final String TAG = "EchoWebSocketListener";

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            MessageBean messageBean = new MessageBean(no,
                    no, no + "上线了");
            Gson gson = new Gson();
            String msg = gson.toJson(messageBean);
            sendMessage(webSocket, msg);
        }

        @Override
        public void onMessage(WebSocket webSocket, final String text) {
            Log.i(TAG, "Receiving: " + text);
            addMessage(text);
        }

        @Override
        public void onMessage(WebSocket webSocket, final ByteString bytes) {
            Log.i(TAG, "Receiving: " + bytes.hex());
            addMessage(bytes.toString());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            Log.i(TAG, "Closing: " + code + " " + reason);
            addMessageOrdinary("正在关闭连接");
            resetWebSocket();
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            Log.i(TAG, "Closed: " + code + " " + reason);
            addMessageOrdinary("连接关闭了");
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            t.printStackTrace();

            addMessageOrdinary("连接失败");

            resetWebSocket();
        }
    }
}
