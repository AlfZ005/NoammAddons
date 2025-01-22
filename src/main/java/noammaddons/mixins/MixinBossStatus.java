package noammaddons.mixins;


import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.boss.IBossDisplayData;
import noammaddons.events.BossbarUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.RegisterEvents.postAndCatch;

@Mixin(BossStatus.class)
abstract class MixinBossStatus {
    @Inject(method = "setBossStatus", at = @At("HEAD"), cancellable = true)
    private static void onBossbarUpdate(IBossDisplayData displayData, boolean hasColorModifierIn, CallbackInfo ci) {
        String bossName = displayData.getDisplayName().getFormattedText();
        float maxHealth = displayData.getMaxHealth();
        float health = displayData.getHealth();
        float healthScale = health / maxHealth;
        float healthPresent = healthScale * 100;
        boolean cancel = postAndCatch(new BossbarUpdateEvent(bossName, maxHealth, health, healthScale, healthPresent));
        if (cancel) ci.cancel();
    }
}