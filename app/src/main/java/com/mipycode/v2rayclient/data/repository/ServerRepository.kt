package com.mipycode.v2rayclient.data.repository

import android.content.Context
import com.mipycode.v2rayclient.data.db.AppDatabase
import com.mipycode.v2rayclient.data.model.ServerConfig
import com.mipycode.v2rayclient.data.parser.LinkParser
import kotlinx.coroutines.flow.Flow
import java.net.InetSocketAddress
import java.net.Socket

class ServerRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).serverDao()

    fun observeServers(): Flow<List<ServerConfig>> = dao.observeAll()

    suspend fun addFromLink(link: String): ServerConfig {
        val config = LinkParser.parse(link)
        val id = dao.insert(config)
        return config.copy(id = id)
    }

    suspend fun addManual(config: ServerConfig): Long = dao.insert(config)

    suspend fun update(config: ServerConfig) = dao.update(config)

    suspend fun delete(config: ServerConfig) = dao.delete(config)

    suspend fun toggleFavorite(id: Long, favorite: Boolean) = dao.updateFavorite(id, favorite)

    /** تست تأخیر (پینگ) با یک اتصال TCP ساده به آدرس:پورت سرور. */
    suspend fun pingServer(config: ServerConfig): Long {
        val start = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(config.address, config.port), 5000)
            }
            val elapsed = System.currentTimeMillis() - start
            dao.updatePing(config.id, elapsed)
            elapsed
        } catch (e: Exception) {
            dao.updatePing(config.id, -1)
            -1
        }
    }
}
