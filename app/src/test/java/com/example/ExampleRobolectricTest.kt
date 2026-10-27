package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GreenBasket", appName)
    }

    @Test
    fun `product discount and unit profit math is accurate`() {
        val product = Product(
            name = "Organic Avocados",
            costPrice = 3.00,
            sellingPrice = 5.00,
            discountPercent = 10.0,
            stockQuantity = 6L,
            reorderThreshold = 10L
        )
        assertEquals(4.50, product.effectiveSellingPrice(), 0.001)
        assertEquals(1.50, product.unitProfit(), 0.001)
        assertTrue(product.isLowStock())
    }
}
