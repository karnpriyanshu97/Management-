package com.example.ui.components

import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

data class TextToolbarMenuData(
    val rect: Rect,
    val onCopy: (() -> Unit)?,
    val onPaste: (() -> Unit)?,
    val onCut: (() -> Unit)?,
    val onSelectAll: (() -> Unit)?
)

/**
 * Safe TextToolbar implementation that coordinates with Android's DecorView and Compose lifecycles.
 * Renders text selection actions in Compose without invoking Android OS's native DecorView FloatingActionMode.
 * This permanently resolves:
 * "E/DecorView[]: Destroying unexpected ActionMode instance of TYPE_FLOATING... was not the current floating action mode! Expected null"
 */
class SafeTextToolbar(private val view: View? = null) : TextToolbar {
    var menuData by mutableStateOf<TextToolbarMenuData?>(null)
        private set

    private var _status: TextToolbarStatus = TextToolbarStatus.Hidden

    override val status: TextToolbarStatus
        get() = if (menuData != null) TextToolbarStatus.Shown else _status

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        if (view != null && (!view.hasWindowFocus() || !view.isAttachedToWindow)) {
            _status = TextToolbarStatus.Hidden
            menuData = null
            return
        }

        _status = TextToolbarStatus.Shown
        menuData = TextToolbarMenuData(
            rect = rect,
            onCopy = onCopyRequested,
            onPaste = onPasteRequested,
            onCut = onCutRequested,
            onSelectAll = onSelectAllRequested
        )
    }

    override fun hide() {
        _status = TextToolbarStatus.Hidden
        menuData = null
    }
}

class TextToolbarPopupPositionProvider(private val contentRect: Rect) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = (contentRect.left + (contentRect.width - popupContentSize.width) / 2f)
            .coerceIn(16f, maxOf(16f, (windowSize.width - popupContentSize.width - 16).toFloat()))
            .toInt()

        val spacing = 12
        var y = (contentRect.top - popupContentSize.height - spacing).toInt()
        if (y < 48) {
            y = (contentRect.bottom + spacing).toInt()
        }
        y = y.coerceIn(16, maxOf(16, windowSize.height - popupContentSize.height - 16))

        return IntOffset(x, y)
    }
}

@Composable
fun SafeTextToolbarHost(
    toolbar: SafeTextToolbar,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalTextToolbar provides toolbar
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            val data = toolbar.menuData
            if (data != null) {
                Popup(
                    popupPositionProvider = TextToolbarPopupPositionProvider(data.rect),
                    onDismissRequest = { toolbar.hide() },
                    properties = PopupProperties(
                        focusable = true,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF1E293B),
                        contentColor = Color.White,
                        shadowElevation = 8.dp,
                        tonalElevation = 6.dp,
                        border = BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (data.onCut != null) {
                                TextButton(
                                    onClick = {
                                        val action = data.onCut
                                        toolbar.hide()
                                        action.invoke()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Cut",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (data.onCopy != null) {
                                TextButton(
                                    onClick = {
                                        val action = data.onCopy
                                        toolbar.hide()
                                        action.invoke()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Copy",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (data.onPaste != null) {
                                TextButton(
                                    onClick = {
                                        val action = data.onPaste
                                        toolbar.hide()
                                        action.invoke()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Paste",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (data.onSelectAll != null) {
                                TextButton(
                                    onClick = {
                                        val action = data.onSelectAll
                                        toolbar.hide()
                                        action.invoke()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Select all",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Android window-level fallback to safely intercept any ActionMode.TYPE_FLOATING
 * requested by any Android View or system component, preventing DecorView from creating
 * or destroying mismatched FloatingActionMode instances.
 */
class SafeFloatingActionMode(
    private val context: Context,
    private val callback: ActionMode.Callback
) : ActionMode() {
    private var isFinished = false
    private val dummyMenu: Menu = PopupMenu(context, null).menu

    init {
        type = TYPE_FLOATING
    }

    override fun setTitle(title: CharSequence?) {}
    override fun setTitle(resId: Int) {}
    override fun setSubtitle(subtitle: CharSequence?) {}
    override fun setSubtitle(resId: Int) {}
    override fun setCustomView(view: View?) {}
    override fun invalidate() {
        if (!isFinished) {
            callback.onPrepareActionMode(this, dummyMenu)
        }
    }

    override fun finish() {
        if (!isFinished) {
            isFinished = true
            callback.onDestroyActionMode(this)
        }
    }

    override fun getMenu(): Menu = dummyMenu
    override fun getTitle(): CharSequence? = null
    override fun getSubtitle(): CharSequence? = null
    override fun getCustomView(): View? = null
    override fun getMenuInflater(): MenuInflater = MenuInflater(context)
}

@Composable
fun rememberSafeTextToolbar(): SafeTextToolbar {
    val view = LocalView.current
    return remember(view) { SafeTextToolbar(view) }
}
