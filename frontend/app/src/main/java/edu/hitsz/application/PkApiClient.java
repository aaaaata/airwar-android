package edu.hitsz.application;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;

import edu.hitsz.config.ServerConfig;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * PkApiClient 封装联机房间相关的 HTTP 请求，包括创建房间、加入房间、查询房间状态、准备、提交最终成绩。
 * 它负责“房间流程”这类低频控制请求；实时分数同步则交给 WebSocket 客户端处理。
 */
public class PkApiClient {

    // OkHttp 负责普通 HTTP 请求；mainHandler 用于把回调切回主线程，方便 Activity 更新 UI。
    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 统一回调接口：成功返回 JSONObject，失败返回错误文本。
    public interface JsonCallback {
        void onSuccess(JSONObject json);

        void onFail(String message);
    }

    // 创建房间：由房主发起，服务器返回 roomId、roomCode 等房间信息。
    public void createRoom(long userId,
                           String username,
                           int avatarId,
                           JsonCallback callback) {

        HttpUrl base = HttpUrl.parse(ServerConfig.HTTP_BASE_URL + "/pk/createRoom");
        if (base == null) {
            callback.onFail("createRoom URL 错误");
            return;
        }

        HttpUrl url = base.newBuilder()
                .addQueryParameter("userId", String.valueOf(userId))
                .addQueryParameter("username", username)
                .addQueryParameter("avatarId", String.valueOf(avatarId))
                .build();

        getJson(url, callback);
    }

    // 加入房间：输入房间号后向服务器登记第二名玩家。
    public void joinRoom(String roomCode,
                         long userId,
                         String username,
                         int avatarId,
                         JsonCallback callback) {

        HttpUrl base = HttpUrl.parse(ServerConfig.HTTP_BASE_URL + "/pk/joinRoom");
        if (base == null) {
            callback.onFail("joinRoom URL 错误");
            return;
        }

        HttpUrl url = base.newBuilder()
                .addQueryParameter("roomCode", roomCode)
                .addQueryParameter("userId", String.valueOf(userId))
                .addQueryParameter("username", username)
                .addQueryParameter("avatarId", String.valueOf(avatarId))
                .build();

        getJson(url, callback);
    }

    // 查询房间状态：等待页定时轮询，用来判断对方是否加入/是否都已准备/是否结束。
    public void roomStatus(long roomId, JsonCallback callback) {
        HttpUrl base = HttpUrl.parse(ServerConfig.HTTP_BASE_URL + "/pk/roomStatus");
        if (base == null) {
            callback.onFail("roomStatus URL 错误");
            return;
        }

        HttpUrl url = base.newBuilder()
                .addQueryParameter("roomId", String.valueOf(roomId))
                .build();

        getJson(url, callback);
    }

    // 准备接口：玩家点击 READY 后调用，服务器判断双方都准备后进入 PLAYING。
    public void ready(long roomId, long userId, JsonCallback callback) {
        HttpUrl base = HttpUrl.parse(ServerConfig.HTTP_BASE_URL + "/pk/ready");
        if (base == null) {
            callback.onFail("ready URL 错误");
            return;
        }

        HttpUrl url = base.newBuilder()
                .addQueryParameter("roomId", String.valueOf(roomId))
                .addQueryParameter("userId", String.valueOf(userId))
                .build();

        getJson(url, callback);
    }

    // 提交最终成绩：一方游戏结束时上传分数，服务器在双方都结束后计算结果。
    public void finish(long roomId,
                       long userId,
                       int score,
                       JsonCallback callback) {

        HttpUrl base = HttpUrl.parse(ServerConfig.HTTP_BASE_URL + "/pk/finish");
        if (base == null) {
            callback.onFail("finish URL 错误");
            return;
        }

        HttpUrl url = base.newBuilder()
                .addQueryParameter("roomId", String.valueOf(roomId))
                .addQueryParameter("userId", String.valueOf(userId))
                .addQueryParameter("score", String.valueOf(score))
                .build();

        getJson(url, callback);
    }

    // 通用 GET + JSON 解析封装，减少每个接口重复写网络请求代码。
    private void getJson(HttpUrl url, JsonCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onFail(e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() == null ? "" : response.body().string();

                try {
                    JSONObject json = new JSONObject(body);
                    mainHandler.post(() -> callback.onSuccess(json));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFail("JSON 解析失败：" + body));
                }
            }
        });
    }
}
