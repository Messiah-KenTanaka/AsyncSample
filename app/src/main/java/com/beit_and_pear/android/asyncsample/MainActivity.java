package com.beit_and_pear.android.asyncsample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

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
        @Override
        public void run() {
            // ここにWeb APIにアクセスするコードを記述
            WeatherInfoPostExecutor postExecutor = new WeatherInfoPostExecutor();
            _handler.post(postExecutor);
        }
    }

    // 非同期でお天気情報を取得した後、UIスレッドでその情報を表示するためのクラス
    private class WeatherInfoPostExecutor implements Runnable {

        @Override
        public void run() {
            // ここにUIスレッドで行う処理コードを記述
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