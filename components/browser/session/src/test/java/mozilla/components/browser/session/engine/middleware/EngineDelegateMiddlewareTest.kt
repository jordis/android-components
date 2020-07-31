/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.engine.middleware

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineDispatcher
import mozilla.components.browser.session.engine.EngineMiddleware
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify

class EngineDelegateMiddlewareTest {
    @Test
    fun `LoadUrlAction for tab without engine session`() {
        val engineSession: EngineSession = mock()
        val engine: Engine = mock()
        doReturn(engineSession).`when`(engine).createSession()

        val dispatcher = TestCoroutineDispatcher()
        val scope = CoroutineScope(dispatcher)

        val store = BrowserStore(
            middleware = EngineMiddleware.create(
                engine = engine,
                sessionLookup = { mock() },
                scope = scope
            ),
            initialState = BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "test-tab")
                )
            )
        )

        store.dispatch(EngineAction.LoadUrlAction(
            "test-tab",
            "https://www.firefox.com"
        )).joinBlocking()

        dispatcher.advanceUntilIdle()

        verify(engine).createSession(private = false, contextId = null)
        verify(engineSession).loadUrl("https://www.firefox.com")
        assertEquals(engineSession, store.state.tabs[0].engineState.engineSession)
    }

    @Test
    fun `LoadUrlAction for private tab without engine session`() {
        val engineSession: EngineSession = mock()
        val engine: Engine = mock()
        doReturn(engineSession).`when`(engine).createSession(private = true)

        val dispatcher = TestCoroutineDispatcher()
        val scope = CoroutineScope(dispatcher)

        val store = BrowserStore(
            middleware = EngineMiddleware.create(
                engine = engine,
                sessionLookup = { mock() },
                scope = scope
            ),
            initialState = BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "test-tab", private = true)
                )
            )
        )

        store.dispatch(
            EngineAction.LoadUrlAction(
                "test-tab",
                "https://www.firefox.com"
            )
        ).joinBlocking()

        dispatcher.advanceUntilIdle()

        verify(engine).createSession(private = true, contextId = null)
        verify(engineSession).loadUrl("https://www.firefox.com")
        assertEquals(engineSession, store.state.tabs[0].engineState.engineSession)
    }

    @Test
    fun `LoadUrlAction for container tab without engine session`() {
        val engineSession: EngineSession = mock()
        val engine: Engine = mock()
        doReturn(engineSession).`when`(engine).createSession(contextId = "test-container")

        val dispatcher = TestCoroutineDispatcher()
        val scope = CoroutineScope(dispatcher)

        val store = BrowserStore(
            middleware = EngineMiddleware.create(
                engine = engine,
                sessionLookup = { mock() },
                scope = scope
            ),
            initialState = BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "test-tab", contextId = "test-container")
                )
            )
        )

        store.dispatch(EngineAction.LoadUrlAction(
            "test-tab",
            "https://www.firefox.com"
        )).joinBlocking()

        dispatcher.advanceUntilIdle()

        verify(engine).createSession(private = false, contextId = "test-container")
        verify(engineSession).loadUrl("https://www.firefox.com")
        assertEquals(engineSession, store.state.tabs[0].engineState.engineSession)
    }
}
