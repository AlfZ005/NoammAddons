package noammaddons.features.dungeons.terminals

import gg.essential.universal.UGraphics.getStringWidth
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiContainerEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.features.dungeons.terminals.ConstantsVariables.RubixTitle
import noammaddons.features.dungeons.terminals.ConstantsVariables.Slot
import noammaddons.features.dungeons.terminals.ConstantsVariables.getColorMode
import noammaddons.features.dungeons.terminals.ConstantsVariables.getSolutionColor
import noammaddons.features.dungeons.terminals.ConstantsVariables.getTermScale
import noammaddons.features.gui.Menus.renderBackground
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils.disableNEUInventoryButtons
import noammaddons.utils.GuiUtils.getMouseX
import noammaddons.utils.GuiUtils.getMouseY
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.LocationUtils
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.SoundUtils.ayaya
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.send
import kotlin.math.floor


object Rubix: Feature() {
    private var inTerminal = false
    private var cwid = - 1
    private var windowSize = 0
    private var slots = mutableListOf<Slot?>()

    private var clicked = false
    private var queue = mutableListOf<Pair<Int, Int>>()
    private var solution = mutableMapOf<Int, Int>()
    private val allowedSlots = listOf(12, 13, 14, 21, 22, 23, 30, 31, 32)
    private val order = listOf(14, 1, 4, 13, 11)


    @SubscribeEvent
    fun onClick(event: GuiContainerEvent.GuiMouseClickEvent) {
        if (! inTerminal) return
        event.isCanceled = true

        val termScale = getTermScale()
        val x = mc.getMouseX() / termScale
        val y = mc.getMouseY() / termScale

        val screenWidth = mc.getWidth() / termScale
        val screenHeight = mc.getHeight() / termScale

        val width = 9 * 18
        val height = windowSize / 9 * 18

        val offsetX = screenWidth / 2 - width / 2
        val offsetY = screenHeight / 2 - height / 2

        val slotX = floor((x - offsetX) / 18).toInt()
        val slotY = floor((y - offsetY) / 18).toInt()

        if (slotX < 0 || slotX > 8 || slotY < 0) return

        val slot = slotX + slotY * 9

        if (slot >= windowSize) return

        when {
            (solution[slot] ?: 0) > 0 -> {
                predict(slot, 0)
                if (clicked) queue.add(slot to 0) else click(slot, 0)
            }

            (solution[slot] ?: 0) < 0 -> {
                predict(slot, 1)
                if (clicked) queue.add(slot to 1) else click(slot, 1)
            }
        }
    }

    @SubscribeEvent
    fun cancelGui(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (! inTerminal) return
        if (! config.DevMode) event.isCanceled = true

        val termScale = getTermScale()
        val screenWidth = mc.getWidth() / termScale
        val screenHeight = mc.getHeight() / termScale

        val width = 9 * 18
        val height = windowSize / 9 * 18

        val offsetX = screenWidth / 2 - width / 2
        val offsetY = screenHeight / 2 - height / 2

        val colorMode = getColorMode()
        val solverColor = getSolutionColor()

        GlStateManager.pushMatrix()
        GlStateManager.scale(termScale, termScale, 0f)

        renderBackground(offsetX, offsetY, width, height, colorMode)
        drawText(RubixTitle, offsetX, offsetY)

        for (i in 0 until windowSize) {
            val solutionValue = solution[i] ?: continue

            val currentOffsetX = i % 9 * 18 + offsetX
            val currentOffsetY = floor(i / 9.0).toInt() * 18 + offsetY

            drawRoundedRect(solverColor, currentOffsetX, currentOffsetY, 16f, 16f, 1.5f)

            drawText(
                solutionValue.toString(),
                currentOffsetX + 8 - getStringWidth(solutionValue.toString()) / 2,
                currentOffsetY + 4,
            )
        }

        GlStateManager.popMatrix()
    }


    private fun solve() {
        solution.clear()
        val calcIndex = { index: Int -> (index + order.size) % order.size }
        val clicks = MutableList(5) { 0 }

        for (i in 0 until 5) {
            slots.filter { it != null && allowedSlots.contains(it.num) && it.meta != order[calcIndex(i)] }
                .forEach {
                    when (it !!.meta) {
                        order[calcIndex(i - 2)] -> clicks[i] += 2
                        order[calcIndex(i - 1)] -> clicks[i] += 1
                        order[calcIndex(i + 1)] -> clicks[i] += 1
                        order[calcIndex(i + 2)] -> clicks[i] += 2
                    }
                }
        }

        val origin = clicks.indexOf(clicks.minOrNull() ?: 0)
        slots.filter { it != null && allowedSlots.contains(it.num) && it.meta != order[calcIndex(origin)] }.forEach {
            solution[it !!.num] = when (it.meta) {
                order[calcIndex(origin - 2)] -> 2
                order[calcIndex(origin - 1)] -> 1
                order[calcIndex(origin + 1)] -> - 1
                order[calcIndex(origin + 2)] -> - 2
                else -> 0
            }
        }
    }

    private fun predict(slot: Int, button: Int) {
        val currentSolution = solution[slot] ?: return
        solution[slot] = if (button == 0) currentSolution - 1 else currentSolution + 1
        if (solution[slot] == 0) solution.remove(slot)
    }

    private fun click(slot: Int, button: Int) {
        clicked = true
        C0EPacketClickWindow(cwid, slot, button, if (button == 2) 3 else 0, null, 0).send()
        val initialWindowId = cwid
        setTimeout(600) {
            if (! inTerminal || initialWindowId != cwid) return@setTimeout
            queue.clear()
            solve()
            clicked = false
        }
    }


    @SubscribeEvent
    fun onWindowOpen(event: PacketEvent.Received) {
        if (! config.CustomTerminalsGui || ! config.CustomRubixTerminal || LocationUtils.dungeonFloor != 7 || F7Phase != 3) return
        if (event.packet !is S2DPacketOpenWindow) return

        val windowTitle = event.packet.windowTitle.unformattedText.removeFormatting()
        val slotCount = event.packet.slotCount
        cwid = event.packet.windowId

        if (Regex("^Change all to same color!$").matches(windowTitle)) {
            inTerminal = true
            clicked = false
            slots.clear()
            windowSize = slotCount
            disableNEUInventoryButtons()
        }
        else inTerminal = false
    }

    @SubscribeEvent
    fun onS2FPacketSetSlot(event: PacketEvent.Received) {
        if (! inTerminal) return
        if (event.packet !is S2FPacketSetSlot) return

        val itemStack = event.packet.func_149174_e()
        val slot = event.packet.func_149173_d()

        if (slot < 0) return
        if (slot >= windowSize) return

        if (itemStack !== null) {
            slots.add(
                Slot(
                    slot,
                    itemStack.getItemId(),
                    itemStack.metadata,
                    itemStack.stackSize,
                    itemStack.displayName.removeFormatting(),
                    itemStack.isItemEnchanted,
                )
            )
        }
        else slots.add(null)

        if (slots.size == windowSize && slot == windowSize - 1) {
            solve()
            if (queue.isNotEmpty() && queue.all { (slot, button) ->
                    ((solution[slot] ?: 0) > 0 && button == 0) || ((solution[slot] ?: 0) < 0 && button == 1)
                }) {
                queue.forEach { (slot, button) -> predict(slot, button) }
                click(queue[0].first, queue[0].second)
                queue.removeAt(0)
            }
            else queue.clear()
        }
    }

    @SubscribeEvent
    fun onWindowClose(event: PacketEvent.Received) {
        if (event.packet !is S2EPacketCloseWindow) return
        if (! inTerminal) return
        reset()
        ayaya.start()
    }

    @SubscribeEvent
    fun onSentPacket(event: PacketEvent.Sent) {
        if (event.packet !is C0DPacketCloseWindow) return
        if (! inTerminal) return
        reset()
    }

    private fun reset() {
        inTerminal = false
        queue.clear()
    }
}
