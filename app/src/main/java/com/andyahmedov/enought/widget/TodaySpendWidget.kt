package com.andyahmedov.enought.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.andyahmedov.enought.app.MainActivity
import com.andyahmedov.enought.app.appContainer
import com.andyahmedov.enought.common.toRubDisplayString
import com.andyahmedov.enought.domain.model.DailyLimitWarningLevel
import kotlin.math.absoluteValue

object TodaySpendWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val widgetState = context.appContainer.getTodayWidgetStateUseCase()
        val openAppAction = actionStartActivity(
            Intent(context, MainActivity::class.java),
        )

        provideContent {
            TodaySpendWidgetContent(
                widgetState = widgetState,
                openAppAction = openAppAction,
            )
        }
    }
}

class TodaySpendWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodaySpendWidget
}

@Composable
private fun TodaySpendWidgetContent(
    widgetState: TodayWidgetState,
    openAppAction: Action,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WIDGET_BACKGROUND_COLOR)
            .cornerRadius(24.dp)
            .clickable(openAppAction)
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = "Today",
            style = TextStyle(
                color = WIDGET_LABEL_COLOR,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(modifier = GlanceModifier.height(10.dp))

        when (widgetState) {
            TodayWidgetState.NoPermission -> NoPermissionWidgetState()
            is TodayWidgetState.NoData -> NoDataWidgetState(widgetState)
            is TodayWidgetState.ReadyPrivate -> ReadyPrivateWidgetState(widgetState)
            is TodayWidgetState.ReadyRegular -> ReadyRegularWidgetState(widgetState)
        }
    }
}

@Composable
private fun NoPermissionWidgetState() {
    Text(
        text = "Notification access needed",
        style = TextStyle(
            color = WIDGET_PRIMARY_TEXT_COLOR,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
    Spacer(modifier = GlanceModifier.height(8.dp))
    Text(
        text = "Open the app to enable access and start tracking phone payments.",
        style = TextStyle(
            color = WIDGET_SECONDARY_TEXT_COLOR,
            fontSize = 14.sp,
        ),
    )
}

@Composable
private fun NoDataWidgetState(
    widgetState: TodayWidgetState.NoData,
) {
    Text(
        text = "No confirmed payments",
        style = TextStyle(
            color = WIDGET_PRIMARY_TEXT_COLOR,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
    Spacer(modifier = GlanceModifier.height(8.dp))
    Text(
        text = "Today's confirmed phone spend will appear here.",
        style = TextStyle(
            color = WIDGET_SECONDARY_TEXT_COLOR,
            fontSize = 14.sp,
        ),
    )
    if (widgetState.hasLowConfidenceItems) {
        Spacer(modifier = GlanceModifier.height(10.dp))
        WidgetHint(text = "There are suspected items today.")
    }
}

@Composable
private fun ReadyRegularWidgetState(
    widgetState: TodayWidgetState.ReadyRegular,
) {
    Text(
        text = widgetState.totalAmountMinor.toRubDisplayString(),
        style = TextStyle(
            color = WIDGET_PRIMARY_TEXT_COLOR,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        ),
        maxLines = 1,
    )
    Spacer(modifier = GlanceModifier.height(10.dp))
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        SummaryMetric(
            label = "Payments",
            value = widgetState.paymentsCount.toString(),
        )
        Spacer(modifier = GlanceModifier.width(16.dp))
        if (widgetState.remainingAmountMinor == null) {
            SummaryMetric(
                label = "Last",
                value = widgetState.lastPaymentAmountMinor?.toRubDisplayString() ?: "—",
            )
        } else {
            SummaryMetric(
                label = if (widgetState.remainingAmountMinor >= 0L) "Left" else "Over",
                value = widgetState.remainingAmountMinor.toWidgetRemainingDisplayString(),
            )
        }
    }
    widgetState.limitWarningLevel?.let { warningLevel ->
        Spacer(modifier = GlanceModifier.height(10.dp))
        WidgetHint(text = warningLevel.toWidgetWarningText())
    }
    if (widgetState.hasLowConfidenceItems) {
        Spacer(modifier = GlanceModifier.height(10.dp))
        WidgetHint(text = "There are suspected items today.")
    }
}

@Composable
private fun ReadyPrivateWidgetState(
    widgetState: TodayWidgetState.ReadyPrivate,
) {
    if (widgetState.remainingAmountMinor == null) {
        Text(
            text = widgetState.paymentsCount.toString(),
            style = TextStyle(
                color = WIDGET_PRIMARY_TEXT_COLOR,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "Confirmed phone payments today",
            style = TextStyle(
                color = WIDGET_SECONDARY_TEXT_COLOR,
                fontSize = 14.sp,
            ),
        )
    } else {
        Text(
            text = widgetState.remainingAmountMinor.toWidgetRemainingDisplayString(),
            style = TextStyle(
                color = WIDGET_PRIMARY_TEXT_COLOR,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = if (widgetState.remainingAmountMinor >= 0L) {
                "Left before today's limit"
            } else {
                "Over today's limit"
            },
            style = TextStyle(
                color = WIDGET_SECONDARY_TEXT_COLOR,
                fontSize = 14.sp,
            ),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "Payments: ${widgetState.paymentsCount}",
            style = TextStyle(
                color = WIDGET_LABEL_COLOR,
                fontSize = 12.sp,
            ),
        )
    }
    widgetState.limitWarningLevel?.let { warningLevel ->
        Spacer(modifier = GlanceModifier.height(10.dp))
        WidgetHint(text = warningLevel.toWidgetWarningText())
    }
    if (widgetState.hasLowConfidenceItems) {
        Spacer(modifier = GlanceModifier.height(10.dp))
        WidgetHint(text = "There are suspected items today.")
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = TextStyle(
                color = WIDGET_LABEL_COLOR,
                fontSize = 12.sp,
            ),
        )
        Text(
            text = value,
            style = TextStyle(
                color = WIDGET_PRIMARY_TEXT_COLOR,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun WidgetHint(
    text: String,
) {
    Text(
        text = text,
        style = TextStyle(
            color = WIDGET_SECONDARY_TEXT_COLOR,
            fontSize = 12.sp,
            textAlign = TextAlign.Start,
        ),
    )
}

private fun Long.toWidgetRemainingDisplayString(): String {
    return absoluteValue.toRubDisplayString()
}

private fun DailyLimitWarningLevel.toWidgetWarningText(): String {
    return when (this) {
        DailyLimitWarningLevel.NEAR_LIMIT -> "Close to today's limit."
        DailyLimitWarningLevel.LIMIT_REACHED -> "Today's limit is already exceeded."
    }
}

private val WIDGET_BACKGROUND_COLOR = ColorProvider(Color(0xFFF4EFE7))
private val WIDGET_PRIMARY_TEXT_COLOR = ColorProvider(Color(0xFF1F1A17))
private val WIDGET_SECONDARY_TEXT_COLOR = ColorProvider(Color(0xFF5F5650))
private val WIDGET_LABEL_COLOR = ColorProvider(Color(0xFF7A6D63))
