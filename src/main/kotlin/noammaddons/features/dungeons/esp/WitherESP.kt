package noammaddons.features.dungeons.esp

import net.minecraft.entity.boss.EntityWither
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.EspUtils.EspMob
import noammaddons.utils.LocationUtils
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.LocationUtils.dungeonFloorNumber
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.world
import noammaddons.utils.RenderUtils.drawEntityBox
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color


object WitherESP: Feature() {
    private enum class Wither(val color: Color) {
        MAXOR(Color(88, 4, 164)),
        STORM(Color(0, 208, 255)),
        GOLDOR(Color(255, 255, 255)),
        NECRON(Color(255, 0, 0)),
        VANQUISHER(MAXOR.color);

        companion object {
            var currentWither: EntityWither? = null
            var currentColor: Color = MAXOR.color

            fun reset() {
                currentWither = null
                currentColor = MAXOR.color
            }

            fun updateColor(bossName: String) {
                currentColor = entries.find {
                    bossName.contains(it.name)
                }?.color ?: return
            }
        }
    }

    private fun isValidLoc(): Boolean {
        return (dungeonFloorNumber == 7 && inBoss && F7Phase != 5) || world == LocationUtils.WorldType.CrimonIsle
    }

    /*
     The wither from the armor set: isArmored: false, invulTime: 800
     Maxor: isArmored: true, invulTime: 200
     Storm: isArmored: true, invulTime: 1
     Goldor: isArmored: true, invulTime: 0
     Necron: isArmored: true, invulTime: 1
     Vanquisher: isArmored: false/true, invulTime: 250
     */
    @SubscribeEvent
    fun onArmorStandRender(event: PostEntityMetadataEvent) {
        if (! config.espWithers) return
        if (! isValidLoc()) return
        val entity = event.entity as? EntityWither ?: return
        if (entity.isInvisible) return
        if (entity.invulTime == 800) return
        if (entity.nbtTagCompound != null) return
        Wither.currentWither = entity
    }


    @SubscribeEvent
    fun onBossbarUpdate(event: BossbarUpdateEvent.Pre) {
        if (! config.espWithers) return
        if (! isValidLoc()) return
        Wither.updateColor(event.bossName.removeFormatting().uppercase())
    }

    @SubscribeEvent
    fun onEntityRender(event: PostRenderEntityModelEvent) {
        if (! config.espWithers) return
        if (! config.espType.equalsOneOf(0, 2)) return
        if (! isValidLoc()) return
        if (event.entity != Wither.currentWither) return
        EspMob(event, Wither.currentColor)
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorld) {
        if (! config.espWithers) return
        if (config.espType != 1) return
        if (! isValidLoc()) return
        if (Wither.currentWither == null) return
        drawEntityBox(Wither.currentWither !!, Wither.currentColor)
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) = Wither.reset()
}