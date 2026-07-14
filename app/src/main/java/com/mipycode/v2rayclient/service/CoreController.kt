package com.mipycode.v2rayclient.service

import libv2ray.CoreCallbackHandler
import libv2ray.Libv2ray

/**
 * لایه‌ی واسط بین اپ و هسته‌ی نیتیو Xray (AndroidLibXrayLite).
 * این پیاده‌سازی دقیقاً منطبق با API واقعی و فعلی کتابخانه است:
 *
 *   type CoreCallbackHandler interface {
 *       Startup() int
 *       Shutdown() int
 *       OnEmitStatus(int, string) int
 *   }
 *   func NewCoreController(s CoreCallbackHandler) *CoreController
 *   func (x *CoreController) StartLoop(configContent string, tunFd int32) (err error)
 *   func (x *CoreController) StopLoop() error
 *   func (x *CoreController) QueryStats(tag string, direct string) int64
 *
 * نکته: این فایل به کتابخانه‌ی libv2ray import مستقیم داره، پس تا وقتی
 * app/libs/libv2ray.aar موجود نباشه، پروژه کامپایل نمی‌شه. این فایل رو
 * gomobile می‌سازه (طبق README یا خودکار در GitHub Actions).
 */
interface CoreController {
    fun startCore(configJson: String, tunFd: Int): Boolean
    fun stopCore()
    fun isRunning(): Boolean
    /** خروجی: Pair(uplinkBytes, downlinkBytes) برای تگ "proxy" (همونی که XrayConfigBuilder ست کرده) */
    fun queryStats(): Pair<Long, Long>
}

class RealCoreController(
    private val callback: V2RayCallback
) : CoreController {

    private var native: libv2ray.CoreController? = null

    private val handler = object : CoreCallbackHandler {
        override fun startup(): Long {
            callback.onCoreLog("هسته‌ی Xray استارت شد")
            return 0
        }

        override fun shutdown(): Long {
            callback.onCoreLog("هسته‌ی Xray متوقف شد")
            return 0
        }

        override fun onEmitStatus(code: Long, message: String?): Long {
            callback.onCoreLog("[core] ${message ?: ""}")
            return 0
        }
    }

    override fun startCore(configJson: String, tunFd: Int): Boolean {
        return try {
            val controller = Libv2ray.newCoreController(handler)
            native = controller
            controller.startLoop(configJson, tunFd)
            true
        } catch (e: Exception) {
            callback.onCoreLog("خطای استارت هسته: ${e.message}")
            false
        }
    }

    override fun stopCore() {
        try {
            native?.stopLoop()
        } catch (e: Exception) {
            callback.onCoreLog("خطای توقف هسته: ${e.message}")
        }
    }

    override fun isRunning(): Boolean = native?.isRunning ?: false

    override fun queryStats(): Pair<Long, Long> {
        val controller = native ?: return 0L to 0L
        val up = try { controller.queryStats("proxy", "uplink") } catch (e: Exception) { 0L }
        val down = try { controller.queryStats("proxy", "downlink") } catch (e: Exception) { 0L }
        return up to down
    }
}

interface V2RayCallback {
    fun onCoreLog(message: String)
}
