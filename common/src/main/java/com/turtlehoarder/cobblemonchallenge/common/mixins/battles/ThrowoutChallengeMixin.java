package com.turtlehoarder.cobblemonchallenge.common.mixins.battles;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokemon.effects.IllusionEffect;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.turtlehoarder.cobblemonchallenge.common.CobblemonChallenge;
import com.turtlehoarder.cobblemonchallenge.common.util.ChallengeUtil;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mixin(Pokemon.class)
public abstract class ThrowoutChallengeMixin {

    @Shadow
    public abstract CompletableFuture<PokemonEntity> sendOutWithAnimation(LivingEntity owner, ServerLevel level, Vec3 position, UUID battleId, boolean playingOpeningAnim, IllusionEffect illusion, Function1<? super PokemonEntity, Unit> callback);

    @Inject(
            method = "sendOutWithAnimation(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Ljava/util/UUID;ZLcom/cobblemon/mod/common/entity/pokemon/effects/IllusionEffect;Lkotlin/jvm/functions/Function1;)Ljava/util/concurrent/CompletableFuture;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSendOut(LivingEntity source, ServerLevel level, Vec3 position, UUID battleId, boolean doCry, IllusionEffect illusion, Function1<? super PokemonEntity, Unit> mutation, CallbackInfoReturnable<CompletableFuture<PokemonEntity>> cir) {
        if (battleId == null || position == null || source == null) {
            return;
        }
        double distanceToThrow = source.position().distanceTo(position);
        // If this is a Challenge Battle AND the throw distance is huge (>50 blocks),we intervene to prevent the "chunk unload/hanging animation" bug.
        if (ChallengeUtil.isBattleChallenge(battleId) && distanceToThrow > 50) {
            CobblemonChallenge.LOGGER.info("Intercept: Redirecting challenge far throw to player position to prevent animation hang.");

            // Instead of modifying args, we cancel the current execution and immediately call the method again with the corrected position (owner.position()).
            Vec3 newPosition = source.position();

            // This recursive call is safe because on the second pass, distanceToThrow will be 0, so this 'if' block will be skipped.
            CompletableFuture<PokemonEntity> newResult = this.sendOutWithAnimation(source, level, newPosition, battleId, doCry, illusion, mutation);

            cir.setReturnValue(newResult);
        }
    }
}
