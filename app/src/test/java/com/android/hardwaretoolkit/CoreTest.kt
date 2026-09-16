package com.android.hardwaretoolkit
import com.android.hardwaretoolkit.core.*
import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    @Test fun hexRoundTrip(){assertEquals("0001FEFF",Hex.encode(Hex.decode("00 01 FE FF")))}
    @Test fun registryRequiresReadyProvider(){
        val r=ProviderRegistry()
        r.upsert(HardwareProvider("x","X",Transport.USB,setOf(Capability.SUB_GHZ_RX),true,false,""))
        assertTrue(r.find(Capability.SUB_GHZ_RX).isEmpty())
        r.upsert(HardwareProvider("x","X",Transport.USB,setOf(Capability.SUB_GHZ_RX),true,true,""))
        assertEquals(1,r.find(Capability.SUB_GHZ_RX).size)
    }
}
