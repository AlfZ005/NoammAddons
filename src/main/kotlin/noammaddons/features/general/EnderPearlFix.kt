package noammaddons.features.general


import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting

object EnderPearlFix: Feature() {
    @SubscribeEvent
    fun FixEnderPearl(event: PlayerInteractEvent) {
        if (! config.enderPearlFix) return
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return
        if (mc.thePlayer?.heldItem?.displayName?.removeFormatting() != "Ender Pearl") return

        event.isCanceled = true
    }
}
