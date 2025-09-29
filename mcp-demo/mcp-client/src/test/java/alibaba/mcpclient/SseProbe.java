package alibaba.mcpclient;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import com.launchdarkly.eventsource.MessageEvent;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import java.net.URI;
import java.time.Duration;

public class SseProbe {

    public static void main(String[] args) throws InterruptedException {
        String url = "https://mcp.api-inference.modelscope.net/d5eddc445c1341/sse";

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .build();

        EventHandler handler = new EventHandler() {
            @Override
            public void onOpen() {
                System.out.println("✅ SSE 连接已打开");
            }

            @Override
            public void onClosed() {
                System.out.println("❌ SSE 连接关闭");
            }

            @Override
            public void onMessage(String event, MessageEvent messageEvent) {
                System.out.println("event = " + event + " | data = " + messageEvent.getData());
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("❌ SSE 错误: " + t.getMessage());
                t.printStackTrace();
            }

            @Override
            public void onComment(String comment) {
                System.out.println("comment = " + comment);
            }
        };

        EventSource es = new EventSource.Builder(handler, URI.create(url))
                .client(client)
                .build();

        es.start();

        // 跑 10 秒观察
        Thread.sleep(10_000);
        es.close();
    }
}