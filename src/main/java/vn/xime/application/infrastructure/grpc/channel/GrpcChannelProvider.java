package vn.xime.application.infrastructure.grpc.channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

import lombok.RequiredArgsConstructor;

import vn.xime.application.integration.trust.ssl.TrustSslContextProvider;

/**
 * Shared mTLS gRPC channel manager (create, cache, reuse by host:port).
 * Quản lý channel gRPC mTLS dùng chung (tạo, cache, tái dùng theo host:port).
 *
 * Chỉ quản lý vòng đời channel; không service discovery, không routing, không protobuf mapping.
 */
@Component
@RequiredArgsConstructor
public class GrpcChannelProvider {

    private final TrustSslContextProvider trustSslContextProvider;

    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    public ManagedChannel getChannel(String host, int port) {
        String key = host + ":" + port;
        return channels.computeIfAbsent(key, ignored ->
                NettyChannelBuilder
                        .forAddress(host, port)
                        .sslContext(trustSslContextProvider.getSslContext())
                        .build());
    }

    public synchronized void clearChannels() {
        channels.values().forEach(this::shutdownChannel);
        channels.clear();
    }

    @PreDestroy
    public void shutdown() {
        clearChannels();
    }

    private void shutdownChannel(ManagedChannel channel) {
        try {
            channel.shutdown();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            channel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
