package com.android.hardwaretoolkit.radio

import com.android.hardwaretoolkit.core.HardwareProvider

/**
 * Safe default: no unknown adapter is treated as functional.
 * Replace with a concrete, documented adapter implementation when hardware is selected.
 */
object NoopRadioDriverFactory:RadioDriverFactory {
    override fun subGhzFor(provider:HardwareProvider):SubGhzDriver? = null
    override fun lfRfidFor(provider:HardwareProvider):LfRfidDriver? = null
}
