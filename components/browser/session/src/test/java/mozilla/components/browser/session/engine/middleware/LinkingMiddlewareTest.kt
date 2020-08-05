/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.engine.middleware

import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.test.any
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.mock
import org.junit.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class LinkingMiddlewareTest {

    @Test
    fun `loads URL after linking`() {
        val middleware = LinkingMiddleware()

        val tab = createTab("https://www.mozilla.org", id = "1")
        val store = BrowserStore(
            initialState = BrowserState(tabs = listOf(tab)),
            middleware = listOf(middleware)
        )

        val engineSession: EngineSession = mock()
        store.dispatch(EngineAction.LinkEngineSessionAction(tab.id, engineSession)).joinBlocking()
        verify(engineSession).loadUrl(tab.content.url)
    }

    @Test
    fun `loads URL with parent after linking`() {
        val middleware = LinkingMiddleware()

        val parent = createTab("https://www.mozilla.org", id = "1")
        val child = createTab("https://www.firefox.com", id = "2", parent = parent)
        val store = BrowserStore(
            initialState = BrowserState(
                tabs = listOf(parent, child)
            ),
            middleware = listOf(middleware)
        )

        val parentEngineSession: EngineSession = mock()
        store.dispatch(EngineAction.LinkEngineSessionAction(parent.id, parentEngineSession)).joinBlocking()

        val childEngineSession: EngineSession = mock()
        store.dispatch(EngineAction.LinkEngineSessionAction(child.id, childEngineSession)).joinBlocking()
        verify(childEngineSession).loadUrl(child.content.url, parentEngineSession)
    }

    @Test
    fun `loads URL without parent for extension URLs`() {
        val middleware = LinkingMiddleware()

        val parent = createTab("https://www.mozilla.org", id = "1")
        val child = createTab("moz-extension://1234", id = "2", parent = parent)
        val store = BrowserStore(
            initialState = BrowserState(
                tabs = listOf(parent, child)
            ),
            middleware = listOf(middleware)
        )

        val parentEngineSession: EngineSession = mock()
        store.dispatch(EngineAction.LinkEngineSessionAction(parent.id, parentEngineSession)).joinBlocking()

        val childEngineSession: EngineSession = mock()
        store.dispatch(EngineAction.LinkEngineSessionAction(child.id, childEngineSession)).joinBlocking()
        verify(childEngineSession).loadUrl(child.content.url)
    }

    @Test
    fun `skips loading URL if specified in action`() {
        val middleware = LinkingMiddleware()

        val tab = createTab("https://www.mozilla.org", id = "1")
        val store = BrowserStore(
            initialState = BrowserState(tabs = listOf(tab)),
            middleware = listOf(middleware)
        )

        val engineSession: EngineSession = mock()
        store.dispatch(EngineAction.LinkEngineSessionAction(tab.id, engineSession, skipLoading = true)).joinBlocking()
        verify(engineSession, never()).loadUrl(tab.content.url)
    }

    @Test
    fun `does nothing if tab does not exist`() {
        val middleware = LinkingMiddleware()

        val store = BrowserStore(
            initialState = BrowserState(tabs = listOf()),
            middleware = listOf(middleware)
        )

        val engineSession: EngineSession = mock()
        store.dispatch(EngineAction.LinkEngineSessionAction("invalid", engineSession)).joinBlocking()
        verify(engineSession, never()).loadUrl(anyString(), any(), any(), any())
    }
}
