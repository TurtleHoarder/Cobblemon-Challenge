package com.turtlehoarder.cobblemonchallenge.common.mixins.battles;

import com.cobblemon.mod.common.battles.interpreter.instructions.SwitchInstruction;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.common.util.ChallengeUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.UUID;

@Mixin(targets = "com/cobblemon/mod/common/battles/interpreter/instructions/SwitchInstruction$Companion$createEntitySwitch$1")
public abstract class SecondCallerMixin {

    /**
     * This is a complicated one... This solves a bug where if a challenge is occurring where the two players are far away from each other, the battle would hang due to animations not being finished.
     * We resolve this bug by sending pokemon to the player's position if they throw distance is too far
     */
    @ModifyArgs(
            method = "invoke(Ljava/lang/Object;)V",
            at = @At(
                    value = "INVOKE",
                    target = "com/cobblemon/mod/common/pokemon/Pokemon.sendOutWithAnimation$default (Lcom/cobblemon/mod/common/pokemon/Pokemon;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Ljava/util/UUID;ZLcom/cobblemon/mod/common/entity/pokemon/effects/IllusionEffect;Lkotlin/jvm/functions/Function1;ILjava/lang/Object;)Ljava/util/concurrent/CompletableFuture;"
            )
    )
    private void modifySecondArgs(Args args) {
        LivingEntity source = args.get(1);
        UUID battleId = args.get(4);
        Vec3 oldPosition = args.get(3);
        Vec3 newPosition = source.position();

        if (battleId != null) { // Double check and make sure this code is only executed in the context of a battle
            double distanceToThrow = source.position().distanceTo(oldPosition);
            if (ChallengeUtil.isBattleChallenge(battleId) && distanceToThrow > 50) {
                args.set(3, newPosition); // Redirect the ball to be thrown at the player's position instead of between the two players. This will prevent animation hanging if the thrown pokeball is thrown so far it unloads
            }
        }
    }
}
