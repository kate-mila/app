package com.mipycode.v2rayclient.util

import com.mipycode.v2rayclient.data.model.ProtocolType
import com.mipycode.v2rayclient.data.model.ServerConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * پیکربندی JSON مورد نیاز هسته‌ی Xray/V2Ray را از روی [ServerConfig] می‌سازد.
 * خروجی این تابع همان چیزی است که به متد استارت هسته (libv2ray) پاس داده می‌شود.
 * ساختار مطابق مستندات رسمی Xray-core (https://xtls.github.io/config/) است.
 */
object XrayConfigBuilder {

    fun build(server: ServerConfig, socksPort: Int = 10808, httpPort: Int = 10809): String {
        val root = JSONObject()
        root.put("log", JSONObject().put("loglevel", "warning"))

        // ---- inbounds: پراکسی محلی که ترافیک را از VpnService دریافت می‌کند ----
        val inbounds = JSONArray()
        inbounds.put(
            JSONObject()
                .put("tag", "socks-in")
                .put("port", socksPort)
                .put("listen", "127.0.0.1")
                .put("protocol", "socks")
                .put("settings", JSONObject().put("udp", true))
        )
        inbounds.put(
            JSONObject()
                .put("tag", "http-in")
                .put("port", httpPort)
                .put("listen", "127.0.0.1")
                .put("protocol", "http")
        )
        root.put("inbounds", inbounds)

        // ---- outbounds: سرور واقعی + بلاک/فریدام ----
        val outbounds = JSONArray()
        outbounds.put(buildProxyOutbound(server))
        outbounds.put(JSONObject().put("protocol", "freedom").put("tag", "direct"))
        outbounds.put(JSONObject().put("protocol", "blackhole").put("tag", "block"))
        root.put("outbounds", outbounds)

        // ---- routing ساده: همه از پراکسی، به‌جز شبکه‌ی محلی ----
        // نکته: قبلاً اینجا از "geoip:private" استفاده می‌شد که نیازمند فایل
        // geoip.dat است (باید کنار هسته/asset ها بمونه). چون این اپ چنین فایلی
        // را همراه نمی‌کند (و کاربر نهایی معمولاً آن را دانلود نمی‌کند)، به‌جایش
        // مستقیماً بازه‌های IP خصوصی/رزرو شده را لیست می‌کنیم. Xray این مقادیر را
        // بدون نیاز به هیچ دیتابیس خارجی می‌پذیرد، پس دیگر خطای
        // "failed to open geoip.dat" رخ نمی‌دهد.
        val privateCidrs = JSONArray(
            listOf(
                "10.0.0.0/8",
                "172.16.0.0/12",
                "192.168.0.0/16",
                "127.0.0.0/8",
                "169.254.0.0/16",
                "100.64.0.0/10",
                "fc00::/7",
                "fe80::/10",
                "::1/128"
            )
        )
        val routing = JSONObject()
        val rules = JSONArray()
        rules.put(
            JSONObject()
                .put("type", "field")
                .put("ip", privateCidrs)
                .put("outboundTag", "direct")
        )
        routing.put("rules", rules)
        root.put("routing", routing)

        return root.toString()
    }

    private fun buildProxyOutbound(s: ServerConfig): JSONObject {
        val outbound = JSONObject().put("tag", "proxy")
        val streamSettings = buildStreamSettings(s)

        when (s.protocol) {
            ProtocolType.VLESS -> {
                outbound.put("protocol", "vless")
                val user = JSONObject()
                    .put("id", s.userId)
                    .put("encryption", s.encryption.ifBlank { "none" })
                if (s.flow.isNotBlank()) user.put("flow", s.flow)
                val vnext = JSONObject()
                    .put("address", s.address)
                    .put("port", s.port)
                    .put("users", JSONArray().put(user))
                outbound.put("settings", JSONObject().put("vnext", JSONArray().put(vnext)))
            }
            ProtocolType.VMESS -> {
                outbound.put("protocol", "vmess")
                val user = JSONObject().put("id", s.userId).put("security", s.encryption.ifBlank { "auto" })
                val vnext = JSONObject()
                    .put("address", s.address)
                    .put("port", s.port)
                    .put("users", JSONArray().put(user))
                outbound.put("settings", JSONObject().put("vnext", JSONArray().put(vnext)))
            }
            ProtocolType.TROJAN -> {
                outbound.put("protocol", "trojan")
                val server = JSONObject()
                    .put("address", s.address)
                    .put("port", s.port)
                    .put("password", s.userId)
                outbound.put("settings", JSONObject().put("servers", JSONArray().put(server)))
            }
            ProtocolType.SHADOWSOCKS -> {
                outbound.put("protocol", "shadowsocks")
                val server = JSONObject()
                    .put("address", s.address)
                    .put("port", s.port)
                    .put("method", s.ssMethod)
                    .put("password", s.userId)
                outbound.put("settings", JSONObject().put("servers", JSONArray().put(server)))
            }
        }

        outbound.put("streamSettings", streamSettings)
        return outbound
    }

    private fun buildStreamSettings(s: ServerConfig): JSONObject {
        val stream = JSONObject().put("network", s.network.ifBlank { "tcp" })

        when (s.network) {
            "ws" -> {
                val wsSettings = JSONObject().put("path", s.path.ifBlank { "/" })
                if (s.host.isNotBlank()) {
                    wsSettings.put("headers", JSONObject().put("Host", s.host))
                }
                stream.put("wsSettings", wsSettings)
            }
            "grpc" -> {
                stream.put("grpcSettings", JSONObject().put("serviceName", s.path))
            }
            "http", "h2" -> {
                stream.put(
                    "httpSettings",
                    JSONObject()
                        .put("path", s.path.ifBlank { "/" })
                        .put("host", JSONArray().put(s.host.ifBlank { s.address }))
                )
            }
        }

        when (s.tls) {
            "tls" -> {
                stream.put("security", "tls")
                val tlsSettings = JSONObject()
                    .put("serverName", s.sni.ifBlank { s.address })
                    .put("fingerprint", s.fingerprint.ifBlank { "chrome" })
                if (s.alpn.isNotBlank()) {
                    tlsSettings.put("alpn", JSONArray(s.alpn.split(",")))
                }
                stream.put("tlsSettings", tlsSettings)
            }
            "reality" -> {
                stream.put("security", "reality")
                val realitySettings = JSONObject()
                    .put("serverName", s.sni.ifBlank { s.address })
                    .put("fingerprint", s.fingerprint.ifBlank { "chrome" })
                    .put("publicKey", s.publicKey)
                    .put("shortId", s.shortId)
                if (s.spiderX.isNotBlank()) realitySettings.put("spiderX", s.spiderX)
                stream.put("realitySettings", realitySettings)
            }
            else -> stream.put("security", "none")
        }

        return stream
    }
}
