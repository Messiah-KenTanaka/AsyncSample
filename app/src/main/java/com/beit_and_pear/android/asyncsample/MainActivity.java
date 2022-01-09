package com.beit_and_pear.android.asyncsample;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // ログに記載するタグ用も文字列
    private static final String DEBUC_TAG = "AsyncSample";
    // お天気情報のURL
    private static final String WEATHERINFO_URL = "https://api.openweathermap.org/data/2.5/weather?long=ja";
    // お天気APIにアクセスするためのAPIキー
    private static final String APP_ID = "4a8c6842fe02425d375329e1dc0da4a0";
    // リストビューに表示されるリストデータ
    private List<Map<String, String>> _list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _list = createList();

        ListView lvCityList = findViewById(R.id.lvCityList);
        String[] from = {"name"};
        int[] to = {android.R.id.text1};
        SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), _list, android.R.layout.simple_list_item_1, from, to);
        lvCityList.setAdapter(adapter);
        lvCityList.setOnItemClickListener(new ListItemClickListener());

    }

    // リストビューに表示させる天気ポイントリストでーたを生成するメソッド
    private List<Map<String, String>> createList() {
        List<Map<String, String>> list = new ArrayList<>();

        Map<String, String> map = new HashMap<>();
        map.put("name", "大阪");
        map.put("q", "Osaka");
        list.add(map);

        map = new HashMap<>();
        map.put("name", "神戸");
        map.put("q", "Kobe");
        list.add(map);

        map = new HashMap<>();
        map.put("name", "鹿児島");
        map.put("q", "Kagoshima");
        list.add(map);

        map = new HashMap<>();
        map.put("name", "福岡");
        map.put("q", "Hukuoka");
        list.add(map);

        return list;
    }

    // お天気情報の取得処理を行うメソッド
    @UiThread
    private void receiveWeatherInfo(final String urlFull) {
        // ここに非同期で天気情報を取得する処理を記述
        Looper mainLooper = Looper.getMainLooper();

        Handler handler = HandlerCompat.createAsync(mainLooper);

        WeatherInfoBackgroundReceiver backgroundReceiver = new WeatherInfoBackgroundReceiver(handler, urlFull);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(backgroundReceiver);
    }

    // 非同期でお天気情報APIにアクセスするためのクラス
    private class WeatherInfoBackgroundReceiver implements Runnable {
        // ハンドラオブジェクト
        private final Handler _handler;
        // お天気情報を取得するURL
        private final String _urlFull;
        // コンストラクタ
        public WeatherInfoBackgroundReceiver(Handler handler, String urlFull) {
            _handler = handler;
            _urlFull = urlFull;
        }
        @WorkerThread
        @Override
        public void run() {
            // ここにWeb APIにアクセスするコードを記述
            // HTTP接続を行うHttpURLConnectionオブジェクトを宣言。finallyで解放するためにtryがいで宣言
            HttpURLConnection con = null;
            // HTTP接続のレスポンスデータとして取得するInputStreamオブジェクトを宣言。同じくtry外で宣言
            InputStream is = null;
            // 天気情報サービスから取得したJSON文字列。天気情報が格納されている
            String result = "";
            try {
                // URLオブジェクトを生成
                URL url = new URL(_urlFull);
                // URLオブジェクトからHttpURLConnectionオブジェクトを取得
                con = (HttpURLConnection) url.openConnection();
                // 接続に使っても良い時間を設定
                con.setConnectTimeout(1000);
                // データ取得に使っても良い時間
                con.setReadTimeout(1000);
                // HTTP接続メソッドをGETに設定
                con.setRequestMethod("GET");
                // 接続
                con.connect();
                // HttpURLConnectionオブジェクトからレスポンスデータを取得
                is = con.getInputStream();
                // レスポンスデータであるInputStreamオブジェクトを文字列に変換
                result = is2String(is);
            }catch (MalformedURLException ex) {
                Log.e(DEBUC_TAG, "URL変換失敗", ex);
            }catch (SocketTimeoutException ex) {
                Log.w(DEBUC_TAG, "通信タイムアウト", ex);
            }catch (IOException ex) {
                Log.e(DEBUC_TAG, "通信失敗", ex);
            }finally {
                // HttpURLConnectionオブジェクトがnull出ないなら解放
                if (con != null) {
                    con.disconnect();
                }
                // InputStreamオブジェクトがnullでないなら解放
                if (is != null) {
                    try {
                        is.close();
                    }catch (IOException ex) {
                        Log.e(DEBUC_TAG, "InputStream解放", ex);
                    }
                }
            }

            WeatherInfoPostExecutor postExecutor = new WeatherInfoPostExecutor(result);
            _handler.post(postExecutor);
        }

        private String is2String(InputStream is) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuffer sb = new StringBuffer();
            char[] b = new char[1024];
            int line;
            while (0 <= (line = reader.read(b))) {
                sb.append(b, 0, line);
            }
            return sb.toString();
        }
    }

    // 非同期でお天気情報を取得した後、UIスレッドでその情報を表示するためのクラス
    @UiThread
    private class WeatherInfoPostExecutor implements Runnable {

        // 取得したお天気情報JSON文字列
        private final String _result;
        // コンストラクタ
        public WeatherInfoPostExecutor(String result) {
            _result = result;
        }

        @Override
        public void run() {
            // ここにUIスレッドで行う処理コードを記述
            // 都市名
            String cityName = "";
            // 天気
            String weather = "";
            // 緯度
            String latitude = "";
            // 軽度
            String longitude = "";
            try {
                // ルートJSONオブジェクトを生成
                JSONObject rootJSON = new JSONObject(_result);
                // 都市名文字列を取得
                cityName = rootJSON.getString("name");
                // 緯度軽度情報JSONオブジェクトを取得
                JSONObject coordJSON = rootJSON.getJSONObject("coord");
                // 緯度情報文字列を取得
                latitude = coordJSON.getString("lat");
                // 軽度情報文字列を取得
                longitude = coordJSON.getString("log");
                // 天気情報JSON配列オブジェクトを取得
                JSONArray weatherJSONArray = rootJSON.getJSONArray("weather");
                // 現在の天気情報JSONオブジェクトを取得
                JSONObject weatherJSON = weatherJSONArray.getJSONObject(0);
                // 現在の天気情報文字列を取得
                weather = weatherJSON.getString("description");
            }catch (JSONException ex) {
                Log.e(DEBUC_TAG, "JSOn解析失敗", ex);
            }

            // 画面に表示する００天気文字列を生成
            String telop = cityName + "の天気";
            // 天気の詳細情報を表示する文字列を生成
            String desc = "現在は" + weather + "です。\n緯度は" + latitude + "度で軽度は" + longitude + "度です";
            // 天気情報を表示するTextViewを取得
            TextView tvWeatherTelop = findViewById(R.id.tvWeatherTelop);
            TextView tvWeatherDesc = findViewById(R.id.tvWeatherDesc);
            // 天気情報を表示
            tvWeatherTelop.setText(telop);
            tvWeatherDesc.setText(desc);
        }
    }

    // リストがタップされた時の処理が記載されたリスナクラス
    private class ListItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Map<String, String> item = _list.get(position);
            String q = item.get("q");
            String urlFull = WEATHERINFO_URL + "&q=" + q + "&appid=" + APP_ID;

            receiveWeatherInfo(urlFull);
        }
    }
}