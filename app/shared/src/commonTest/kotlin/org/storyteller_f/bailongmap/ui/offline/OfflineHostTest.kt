package org.storyteller_f.bailongmap.ui.offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.maplibre.compose.offline.OfflinePack
import org.maplibre.compose.offline.OfflinePackDefinition
import org.maplibre.spatialk.geojson.BoundingBox

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineHostTest {
    @Test
    fun creationUsesInjectedDispatcherAndPreservesDefinitionAndTitle() = runTest {
        val operations = FakeOfflineOperations()
        val host = OfflineHost(operations, StandardTestDispatcher(testScheduler))
        try {
            host.create(definition, "测试区域")
            assertEquals(0, operations.creations.size)
            runCurrent()
            assertEquals(definition, operations.creations.single().first)
            assertEquals("测试区域", operations.creations.single().second.decodeToString())
            assertFalse(host.isCreatingPack.value)
        } finally {
            host.close()
        }
    }

    @Test
    fun repeatedDownloadWhileCreationIsSuspendedDoesNotCreateAnotherPack() = runTest {
        val pending = CompletableDeferred<Unit>()
        val operations = FakeOfflineOperations(createAction = { pending.await() })
        val host = OfflineHost(operations, StandardTestDispatcher(testScheduler))
        try {
            host.create(definition, "区域一")
            runCurrent()
            assertTrue(host.isCreatingPack.value)
            host.create(definition, "区域二")
            runCurrent()
            assertEquals(1, operations.creations.size)
            pending.complete(Unit)
            runCurrent()
            assertFalse(host.isCreatingPack.value)
        } finally {
            host.close()
        }
    }

    @Test
    fun failedCreationResetsBusyStateAndReportsAnError() = runTest {
        val operations = FakeOfflineOperations(createAction = { error("synthetic failure") })
        val host = OfflineHost(operations, StandardTestDispatcher(testScheduler))
        val errors = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { host.effects.collect { errors += it } }
        try {
            host.create(definition, "区域")
            runCurrent()
            assertFalse(host.isCreatingPack.value)
            assertEquals(listOf("创建离线区域失败"), errors)
        } finally {
            host.close()
        }
    }

    @Test
    fun closingHostCancelsCreationWithoutReportingFailure() = runTest {
        val pending = CompletableDeferred<Unit>()
        var cancelled = false
        val operations = FakeOfflineOperations(createAction = {
            try {
                pending.await()
            } catch (exception: CancellationException) {
                cancelled = true
                throw exception
            }
        })
        val host = OfflineHost(operations, StandardTestDispatcher(testScheduler))
        val errors = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { host.effects.collect { errors += it } }
        host.create(definition, "区域")
        runCurrent()
        host.close()
        runCurrent()
        assertTrue(cancelled)
        assertFalse(host.isCreatingPack.value)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun cacheFailureIsReportedThroughEffects() = runTest {
        val host = OfflineHost(
            FakeOfflineOperations(clearAction = { error("synthetic failure") }),
            StandardTestDispatcher(testScheduler),
        )
        val errors = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { host.effects.collect { errors += it } }
        try {
            host.clearCache()
            runCurrent()
            assertEquals(listOf("清理缓存失败"), errors)
        } finally {
            host.close()
        }
    }

    private class FakeOfflineOperations(
        val createAction: suspend () -> Unit = {},
        val clearAction: suspend () -> Unit = {},
    ) : OfflineOperations {
        val creations = mutableListOf<Pair<OfflinePackDefinition, ByteArray>>()
        override suspend fun createAndResume(definition: OfflinePackDefinition, metadata: ByteArray) {
            creations += definition to metadata
            createAction()
        }
        override suspend fun clearCache() = clearAction()
        override suspend fun delete(pack: OfflinePack) = error("Unexpected delete")
        override fun pause(pack: OfflinePack) = error("Unexpected pause")
        override fun resume(pack: OfflinePack) = error("Unexpected resume")
    }

    private val definition = OfflinePackDefinition.TilePyramid(
        styleUrl = "https://example.com/style.json",
        bounds = BoundingBox(116.3, 39.8, 116.5, 40.0),
        pixelRatio = 2f,
        minZoom = 8.0,
        maxZoom = 12.0,
    )
}
