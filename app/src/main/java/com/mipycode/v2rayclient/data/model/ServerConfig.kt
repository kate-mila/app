package com.mipycode.v2rayclient.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProtocolType { VLESS, VMESS, TROJAN, SHADOWSOCKS }

@Entity(tableName = "servers")
data class ServerConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remark: String,
    val protocol: ProtocolType,
    val address: String,
    val port: Int,
    val userId: String,        // uuid (vless/vmess) یا password (trojan/ss)
    val encryption: String = "none",
    val network: String = "tcp",   // tcp, ws, grpc, http
    val path: String = "",
    val host: String = "",
    val tls: String = "none",      // none, tls, reality
    val sni: String = "",
    val alpn: String = "",
    val fingerprint: String = "chrome",
    val publicKey: String = "",    // برای Reality
    val shortId: String = "",      // برای Reality
    val spiderX: String = "",
    val flow: String = "",         // xtls-rprx-vision و غیره
    val ssMethod: String = "",     // فقط shadowsocks
    val rawLink: String = "",      // لینک اشتراک‌گذاری اصلی، برای پشتیبان
    val addedAt: Long = System.currentTimeMillis(),
    val lastPingMs: Long = -1,     // -1 یعنی هنوز تست نشده
    val isFavorite: Boolean = false
)
