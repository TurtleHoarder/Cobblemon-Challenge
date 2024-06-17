package com.turtlehoarder.cobblemonchallenge.battle.pokemon

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.evolution.requirements.LevelRequirement

import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt


class ChallengeBattlePokemon(
    originalPokemon: Pokemon,
    effectedPokemon: Pokemon,
    postBattleEntityOperation: (PokemonEntity) -> Unit
) : BattlePokemon(originalPokemon) {

    companion object {
        fun safeCopyOfChallenge(pokemon: Pokemon): ChallengeBattlePokemon = ChallengeBattlePokemon(
            originalPokemon = pokemon,
            effectedPokemon = pokemon.clone(),
            postBattleEntityOperation = { entity -> entity.discard() }
        )
    }

    /**
     * Method for clamping Battle Pokemon to level range & applying a handicap
     *
     * - [handicap] is applied AFTER level clamp to range
     * - The [effectedPokemon] level may be outside the levelRange after the handicap is applied, but will be a hard clamped to 1-100 inclusive
     */
    fun applyChallengePropertiesToEffectedPokemon(minLevel: Int, maxLevel: Int, handicap: Int,heal: Boolean) : BattlePokemon {
        val adjustedLevel = if ((originalPokemon.level < minLevel)) minLevel + handicap else (min(originalPokemon.level, maxLevel) + handicap)
        effectedPokemon.level = if ((adjustedLevel < 1)) 1 else min(adjustedLevel,100)
        if (heal) effectedPokemon.heal()
        return this
    }

    /*
     * TODO: call from mixin -> if BattlePokemon is ChallengeBattlePokemon in PokemonBattle.end() ->
     *      redirect here else ->
     *      call ExperienceCalculator.Calculate as normally done
     *      original call ->
     *      com.cobblemon.mod.common.api.pokemon.experience.ExperienceCalculator.Calculate
     */
    fun calculateExperience(battlePokemon: BattlePokemon, opponentPokemon: BattlePokemon, participationMultiplier: Double): Int {
        // This is meant to be a division but this is due to the intended behavior of handling the 2.0 sent over from Exp. All in modern Pokémon

        //  Tweaked method to get exp gain that reflects that original pokemon's unaltered levels
        val term2 = 1 * participationMultiplier
        val victorPokemon = opponentPokemon.effectedPokemon
        val victorLevel = victorPokemon.level
        val baseExp = opponentPokemon.originalPokemon.form.baseExperienceYield
        val opponentLevel = victorLevel + (opponentPokemon.effectedPokemon.level - battlePokemon.effectedPokemon.level)
        val term1 = (baseExp * opponentLevel) / 5.0
        val term3 = (((2.0 * opponentLevel) + 10) / (opponentLevel + victorLevel + 10)).pow(2.5)

        // eight addition not yet in base mod -> can use ternary, but it is super wide, so I tried to keep it readable
        val validOriginalTrainer = victorPokemon.originalTrainer != null && battlePokemon.actor.type == ActorType.PLAYER
        val differentUuid = victorPokemon.originalTrainer != battlePokemon.actor.uuid.toString()
        val validName = victorPokemon.originalTrainerName != null
        val differentName = victorPokemon.originalTrainerName != battlePokemon.actor.getName().toString()
        val nonOtBonus = when {
            validOriginalTrainer && differentUuid && (!validName || differentName) -> 1.7
            validName && differentName && !validOriginalTrainer -> 1.7
            else -> 1.0
        }

        val luckyEggMultiplier = if (battlePokemon.effectedPokemon.heldItem().tags.anyMatch { tag ->
                tag == CobblemonItemTags.LUCKY_EGG
            }) Cobblemon.config.luckyEggMultiplier else 1.0
        val evolutionMultiplier = if (battlePokemon.effectedPokemon.evolutionProxy.server().any { evolution ->
                val requirements = evolution.requirements.asSequence()
                requirements.any { it is LevelRequirement } && requirements.all { it.check(battlePokemon.effectedPokemon) }
            }) 1.2 else 1.0
        val affectionMultiplier = if (battlePokemon.effectedPokemon.friendship >= 220) 1.2 else 1.0
        val gimmickBoost = Cobblemon.config.experienceMultiplier

        val term4 = term1 * term2 * term3 + 1
        return (term4 * nonOtBonus * luckyEggMultiplier * evolutionMultiplier * affectionMultiplier * gimmickBoost).roundToInt()
    }
}