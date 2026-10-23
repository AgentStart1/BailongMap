package org.storyteller_f.bailongmap.ui.offline

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.maplibre.compose.offline.OfflinePack
import org.maplibre.compose.offline.OfflinePackDefinition
import org.maplibre.compose.offline.OfflineStorage

internal object OfflineDispatchers {
    val coordination = Dispatchers.Default.limitedParallelism(1)
}

internal interface OfflineOperations {
    suspend fun createAndResume(definition: OfflinePackDefinition, metadata: ByteArray)
    suspend fun clearCache()
    suspend fun delete(pack: OfflinePack)
    fun pause(pack: OfflinePack)
    fun resume(pack: OfflinePack)
}

internal class MaplibreOfflineOperations(private val storage: OfflineStorage) : OfflineOperations {
    override suspend fun createAndResume(definition: OfflinePackDefinition, metadata: ByteArray) {
        val pack = storage.create(definition, metadata)
        storage.resume(pack)
    }

    override suspend fun clearCache() = storage.clearAmbientCache()
    override suspend fun delete(pack: OfflinePack) = storage.delete(pack)
    override fun pause(pack: OfflinePack) = storage.pause(pack)
    override fun resume(pack: OfflinePack) = storage.resume(pack)
}

/** Owns offline actions for one screen; [dispatcher] must serialize coordination off the UI thread. */
internal class OfflineHost(
    private val operations: OfflineOperations,
    dispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val creating = MutableStateFlow(false)
    val isCreatingPack = creating.asStateFlow()
    private val errors = Channel<String>(Channel.BUFFERED)
    val effects = errors.receiveAsFlow()

    fun create(definition: OfflinePackDefinition, title: String) {
        scope.launch {
            if (creating.value) return@launch
            creating.value = true
            try {
                perform("创建离线区域失败") {
                    operations.createAndResume(definition, title.encodeToByteArray())
                }
            } finally {
                creating.value = false
            }
        }
    }

    fun clearCache() = scope.launch { perform("清理缓存失败") { operations.clearCache() } }
    fun delete(pack: OfflinePack) = scope.launch { perform("删除离线区域失败") { operations.delete(pack) } }
    fun pause(pack: OfflinePack) = scope.launch { perform("暂停下载失败") { operations.pause(pack) } }
    fun resume(pack: OfflinePack) = scope.launch { perform("继续下载失败") { operations.resume(pack) } }

    private suspend fun perform(message: String, action: suspend () -> Unit) {
        try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            errors.send(message)
        }
    }

    fun close() {
        scope.cancel()
    }
}
