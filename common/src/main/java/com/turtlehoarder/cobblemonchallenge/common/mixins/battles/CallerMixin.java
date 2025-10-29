package com.turtlehoarder.cobblemonchallenge.common.mixins.battles;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
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

@Mixin(com.cobblemon.mod.common.battles.interpreter.instructions.SwitchInstruction.class)
public class CallerMixin {

    /**
     * This is a complicated one... This solves a bug where if a challenge is occurring where the two players are far away from each other, the battle would hang due to animations not being finished.
     * We resolve this bug by sending pokemon to the player's position if they throw distance is too far
     */
    @ModifyArgs(method = "invoke$lambda$5$lambda$4(Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lnet/minecraft/world/entity/LivingEntity;Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;Lnet/minecraft/world/phys/Vec3;Lcom/cobblemon/mod/common/api/battles/model/actor/BattleActor;)Lkotlin/Unit;",
            at=@At(value="INVOKE",
        target = "Lcom/cobblemon/mod/common/pokemon/Pokemon;sendOutWithAnimation$default(Lcom/cobblemon/mod/common/pokemon/Pokemon;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Ljava/util/UUID;ZLcom/cobblemon/mod/common/entity/pokemon/effects/IllusionEffect;Lkotlin/jvm/functions/Function1;ILjava/lang/Object;)Ljava/util/concurrent/CompletableFuture;"),
            remap = false)
    private static void modifySendOutPosition(Args args) {
        LivingEntity source = args.get(1);
        Vec3 newPosition = source.position();
        Vec3 oldPosition = args.get(3);
        UUID battleId = args.get(4);

        if (battleId != null) { // Double check and make sure this code is only executed in the context of a battle
            double distanceToThrow = source.position().distanceTo(oldPosition);
            if (ChallengeUtil.isBattleChallenge(battleId) && distanceToThrow > 50) {
                args.set(3, newPosition); // Redirect the ball to be thrown at the player's position instead of between the two players. This will prevent animation hanging if the thrown pokeball is thrown so far it unloads
            }
        }
    }




}
