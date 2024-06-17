package com.turtlehoarder.cobblemonchallenge.mixin;

import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.turtlehoarder.cobblemonchallenge.config.UniversalDifficultyConfig;

import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

@Mixin(BattleBuilder.class)
public class BattleBuilderMixin {

    @Unique private final static boolean useUniversalLevel = UniversalDifficultyConfig.USE_UNIVERSAL_LEVEL;
    @Unique private final static boolean useUniversalLevelRange = UniversalDifficultyConfig.USE_UNIVERSAL_LEVEL_RANGE;
    @Unique private final static boolean useUniversalHandicap = UniversalDifficultyConfig.USE_UNIVERSAL_HANDICAP;

    @Redirect(
            method = "pvp1v1*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/cobblemon/mod/common/api/storage/party/PartyStore;toBattleTeam(ZZLjava/util/UUID;)Ljava/util/List;",
                    remap = false
            ))
    private List<BattlePokemon> mixinPvpToBattleTeam(
            PartyStore partyStore,
            boolean cloneParties,
            boolean healFirst,
            @Nullable UUID leadingPokemon,
            CallbackInfoReturnable<List<BattlePokemon>> battleSideCI
    ) {
        Log.debug(LogCategory.LOG,"partyStore = '" + partyStore.toString() + "'");
        Log.debug(LogCategory.LOG,"mixin Redirect at BattleBuilder.pvp1v1 worked!");
        if (useUniversalLevel || useUniversalLevelRange) { // TODO expand on with future configs
            // TODO Implement custom battle team here w/ ChallengeBattlePokemon
            //return ;
        }
        // passing onto the default implementation
        return partyStore.toBattleTeam(cloneParties, healFirst, leadingPokemon);
    }


    @Redirect(
            method = "pve*",
            at = @At(
                value = "INVOKE",
                target = "Lcom/cobblemon/mod/common/api/storage/party/PartyStore;toBattleTeam(ZZLjava/util/UUID;)Ljava/util/List;",
                remap = false
            ))
    public List<BattlePokemon> mixinPveToBattleTeam(
            PartyStore partyStore,
            boolean cloneParties,
            boolean healFirst,
            @Nullable UUID leadingPokemon
    ) {
        // TODO expand on with future configs
        if ((useUniversalLevel || useUniversalLevelRange) || useUniversalHandicap) {
            // TODO Implement custom battle team here w/ ChallengeBattlePokemon
            //      !! possible alternative ->
            //          capture parameters, or @Inject + HEAD to capture/test parameters then ->
            //          @Redirect + INVOKE .toBattleTeam w/ callbackInfo
            //return ;
        }
        // passing onto the default implementation
        return partyStore.toBattleTeam(cloneParties, healFirst, leadingPokemon);
    }
}
