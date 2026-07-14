package com.mipycode.v2rayclient.data.parser

import android.util.Base64
import com.mipycode.v2rayclient.data.model.ProtocolType
import com.mipycode.v2rayclient.data.model.ServerConfig
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder

/**
 * تبدیل لینک‌های اشتراک‌گذاری استاندارد (vless://, vmess://, trojan://, ss://)
 * به مدل [ServerConfig]. این لینک‌ها همان چیزی هستند که با اسکن QR یا Copy Link
 * از پنل سرویس‌دهنده به دست می‌آید.
 */
object LinkParser {

    class ParseException(message: String) : Exception(message)

    fun parse(rawLink: String): ServerConfig {
        val link = rawLink.trim()
        return when {
            link.startsWith("vless://") -> parseVless(link)
            link.startsWith("trojan://") -> parseTrojan(link)
            link.startsWith("vmess://") -> parseVmess(link)
            link.startsWith("ss://") -> parseShadowsocks(link)
            else -> throw ParseException("پروتکل پشتیبانی نمی‌شود. لینک باید با vless:// ، vmess:// ، trojan:// یا ss:// شروع شود.")
        }
    }

    // vless://uuid@host:port?encryption=none&security=tls&sni=...&fp=...&type=ws&path=...&host=...#remark
    private fun parseVless(link: String): ServerConfig {
        val uri = URI(link)
        val userId = uri.userInfo ?: throw ParseException("UUID در لینک VLESS یافت نشد")
        val params = parseQuery(uri.rawQuery)
        return ServerConfig(
            remark = decodeFragment(uri.rawFragment) ?: "VLESS Server",
            protocol = ProtocolType.VLESS,
            address = uri.host ?: throw ParseException("آدرس سرور نامعتبر است"),
            port = if (uri.port != -1) uri.port else 443,
            userId = userId,
            encryption = params["encryption"] ?: "none",
            network = params["type"] ?: "tcp",
            path = params["path"] ?: "",
            host = params["host"] ?: "",
            tls = params["security"] ?: "none",
            sni = params["sni"] ?: "",
            alpn = params["alpn"] ?: "",
            fingerprint = params["fp"] ?: "chrome",
            publicKey = params["pbk"] ?: "",
            shortId = params["sid"] ?: "",
            spiderX = params["spx"] ?: "",
            flow = params["flow"] ?: "",
            rawLink = link
        )
    }

    // trojan://password@host:port?security=tls&sni=...&type=ws&path=...#remark
    private fun parseTrojan(link: String): ServerConfig {
        val uri = URI(link)
        val password = uri.userInfo ?: throw ParseException("پسورد در لینک Trojan یافت نشد")
        val params = parseQuery(uri.rawQuery)
        return ServerConfig(
            remark = decodeFragment(uri.rawFragment) ?: "Trojan Server",
            protocol = ProtocolType.TROJAN,
            address = uri.host ?: throw ParseException("آدرس سرور نامعتبر است"),
            port = if (uri.port != -1) uri.port else 443,
            userId = password,
            network = params["type"] ?: "tcp",
            path = params["path"] ?: "",
            host = params["host"] ?: "",
            tls = params["security"] ?: "tls",
            sni = params["sni"] ?: "",
            fingerprint = params["fp"] ?: "chrome",
            flow = params["flow"] ?: "",
            rawLink = link
        )
    }

    // vmess://<base64 JSON>
    private fun parseVmess(link: String): ServerConfig {
        val b64 = link.removePrefix("vmess://")
        val json = try {
            JSONObject(String(Base64.decode(b64, Base64.DEFAULT)))
        } catch (e: Exception) {
            throw ParseException("لینک VMess نامعتبر است یا فرمت آن پشتیبانی نمی‌شود")
        }
        return ServerConfig(
            remark = json.optString("ps", "VMess Server"),
            protocol = ProtocolType.VMESS,
            address = json.optString("add"),
            port = json.optString("port", "443").toIntOrNull() ?: 443,
            userId = json.optString("id"),
            encryption = json.optString("scy", "auto"),
            network = json.optString("net", "tcp"),
            path = json.optString("path", ""),
            host = json.optString("host", ""),
            tls = json.optString("tls", "none"),
            sni = json.optString("sni", ""),
            alpn = json.optString("alpn", ""),
            rawLink = link
        )
    }

    // ss://base64(method:password)@host:port#remark  یا  ss://base64(method:password@host:port)#remark
    private fun parseShadowsocks(link: String): ServerConfig {
        val withoutScheme = link.removePrefix("ss://")
        val fragmentSplit = withoutScheme.split("#", limit = 2)
        val remark = fragmentSplit.getOrNull(1)?.let { decodeFragment(it) } ?: "Shadowsocks Server"
        val body = fragmentSplit[0]

        val (methodPass, hostPort) = if (body.contains("@")) {
            val atSplit = body.split("@", limit = 2)
            val decodedMethodPass = tryDecodeBase64(atSplit[0]) ?: atSplit[0]
            decodedMethodPass to atSplit[1]
        } else {
            val decoded = tryDecodeBase64(body) ?: throw ParseException("لینک Shadowsocks نامعتبر است")
            val atIdx = decoded.lastIndexOf('@')
            if (atIdx == -1) throw ParseException("لینک Shadowsocks نامعتبر است")
            decoded.substring(0, atIdx) to decoded.substring(atIdx + 1)
        }

        val methodPassParts = methodPass.split(":", limit = 2)
        if (methodPassParts.size != 2) throw ParseException("متد یا پسورد Shadowsocks نامعتبر است")
        val hostPortParts = hostPort.split(":", limit = 2)
        if (hostPortParts.size != 2) throw ParseException("آدرس یا پورت Shadowsocks نامعتبر است")

        return ServerConfig(
            remark = remark,
            protocol = ProtocolType.SHADOWSOCKS,
            address = hostPortParts[0],
            port = hostPortParts[1].toIntOrNull() ?: 8388,
            userId = methodPassParts[1],
            ssMethod = methodPassParts[0],
            rawLink = link
        )
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split("&").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8") else null
        }.toMap()
    }

    private fun decodeFragment(fragment: String?): String? =
        fragment?.let { URLDecoder.decode(it, "UTF-8") }

    private fun tryDecodeBase64(s: String): String? = try {
        String(Base64.decode(s, Base64.URL_SAFE.or(Base64.NO_WRAP)))
    } catch (e: Exception) {
        try { String(Base64.decode(s, Base64.DEFAULT)) } catch (e2: Exception) { null }
    }
}
