package com.turtlehoarder.cobblemonchallenge.common.mixins.battles;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.ShowdownInterpreter;
import com.turtlehoarder.cobblemonchallenge.common.battle.BattlePeekTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShowdownInterpreter.class)
public class ShowdownMessageMixin {

    @Inject(
            method = "interpret(Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Ljava/lang/String;)V",
            at = @At("HEAD"),
            remap = false
    )
    private void onInterpret(PokemonBattle battle, String rawMessage, CallbackInfo ci) {
        BattlePeekTracker.processRawMessage(battle, rawMessage);
    }
}
