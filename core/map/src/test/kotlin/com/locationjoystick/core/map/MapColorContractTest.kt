package com.locationjoystick.core.map

import androidx.compose.ui.graphics.Color
import com.locationjoystick.core.common.constants.AppConstants
import com.locationjoystick.core.designsystem.LjMapColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapColorContractTest {
    @Test
    fun `current position dot is larger than route points`() {
        assertTrue(
            AppConstants.MapConstants.POSITION_DOT_RADIUS >
                AppConstants.MapConstants.ROUTE_POINT_RADIUS,
        )
    }

    @Test
    fun `current position is blue not orange`() {
        assertNotEquals(LjMapColors.PositionBlue, LjMapColors.RouteLine)
    }

    @Test
    fun `route line is 65A5FF`() {
        assertEquals(Color(0xFF65A5FF), LjMapColors.RouteLine)
    }

    @Test
    fun `position blue is 3284FF`() {
        assertEquals(Color(0xFF3284FF), LjMapColors.PositionBlue)
    }

    @Test
    fun `point stroke is white`() {
        assertEquals(Color(0xFFFFFFFF), LjMapColors.PointStroke)
    }
}
