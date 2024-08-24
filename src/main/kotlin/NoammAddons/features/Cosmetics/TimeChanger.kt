package NoammAddons.features.Cosmetics

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.ReceivePacketEvent
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object TimeChanger {
    @SubscribeEvent
    fun onPacket(event: ReceivePacketEvent) {
        if (event.packet is S03PacketTimeUpdate && config.TimeChanger) {
            event.isCanceled = true
            mc.theWorld.worldTime = when (config.TimeChangerMode) {
                0 -> 1000 // Day
                1 -> 6000 // Noon
                2 -> 12000 // Sunset
                3 -> 13000 // Night
                4 -> 18000 // Midnight
                5 -> 23000 // Sunrise
                else -> 0
            }

        }
    }
}
