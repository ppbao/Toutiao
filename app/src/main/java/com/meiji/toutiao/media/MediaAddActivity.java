package com.meiji.toutiao.media;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.meiji.toutiao.BaseActivity;
import com.meiji.toutiao.Constant;
import com.meiji.toutiao.InitApp;
import com.meiji.toutiao.R;
import com.meiji.toutiao.bean.media.MediaChannelBean;
import com.meiji.toutiao.database.dao.MediaChannelDao;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Meiji on 2017/4/10.
 */

public class MediaAddActivity extends BaseActivity {

    private static final String TAG = "MediaAddActivity";
    private static final String URLEXTRA = "url";
    private static final String TYPEEXTRA = "type";
    private ProgressDialog dialog;
    private MediaChannelDao dao = new MediaChannelDao();
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1) {
                onFinish(getString(R.string.add_success));
            }
            if (msg.what == 0) {
                onFinish(getString(R.string.add_fail));
            }
            return false;
        }
    });

    public static void startActivity(String url, String type) {
        InitApp.AppContext.startActivity(new Intent(InitApp.AppContext, MediaAddActivity.class).
                putExtra(URLEXTRA, url)
                .putExtra(TYPEEXTRA, type)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.loading));
        dialog.setCancelable(true);
        dialog.show();

        Intent intent = getIntent();
        String mediaUrl = intent.getStringExtra(URLEXTRA);
        String mediaType = intent.getStringExtra(TYPEEXTRA);
        Log.d(TAG, "onCreate: " + mediaUrl);
        if (!TextUtils.isEmpty(mediaUrl)) {
            handSendText(mediaUrl, mediaType);
        } else {
            onFinish(getString(R.string.formal_incorrect));
        }
    }

    private void onFinish(String message) {
        dialog.dismiss();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 800);
    }

    private void handSendText(final String mediaUrl, final String mediaType) {
        // http://toutiao.com/m1556753339387905/
        final String regex = "^.*http.*://.*toutiao.com/(.*)$";
        final Matcher matcher = Pattern.compile(regex).matcher(mediaUrl);
        if (matcher.find()) {
            final String id = matcher.group().replaceAll("[^0-9]", "");
            // 查询是否已添加
            List<MediaChannelBean> list = dao.queryId(id);
            for (MediaChannelBean bean : list) {
                if (TextUtils.equals(bean.getId(), id)) {
                    onFinish(getString(R.string.has_been_added));
                    return;
                }
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String avatar = null;
                        String name;
                        String descText;
                        Document doc = Jsoup
                                .connect(mediaUrl)
                                .userAgent(Constant.USER_AGENT_PHONE)
                                .get();

                        Elements imgs = doc.getElementsByClass("avatar round");
                        for (Element element : imgs) {
                            avatar = element.child(0).attr("src");
                        }
                        name = doc.getElementsByClass("mpl-head-name tt-font-1").text().trim();
                        descText = doc.getElementsByClass("mpl-head-desc").text().trim();

                        if (!TextUtils.isEmpty(avatar) && !TextUtils.isEmpty(name)) {
                            boolean result = dao.add(id, name, avatar, mediaType, "", descText, mediaUrl);
                            if (result) {
                                handler.obtainMessage(1).sendToTarget();
                            }
                        } else {
                            handler.obtainMessage(0).sendToTarget();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        handler.obtainMessage(0).sendToTarget();
                    }
                }
            }).start();
        } else {
            onFinish(getString(R.string.incorrect_link));
        }
    }

}
