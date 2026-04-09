/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.dispatch

import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import org.objectweb.asm.tree.analysis.Interpreter

class InstructionSet {
    val instructions: MutableList<InterpreterInstruction> = mutableListOf()

    fun getSubsequentInstructions(instruction: InterpreterInstruction): List<InterpreterInstruction> {
        val index = instructions.indexOf(instruction)
        return instructions.subList(index + 1, instructions.size).toList()
    }

    fun getPreviousInstructions(instruction: InterpreterInstruction): List<InterpreterInstruction> {
        val index = instructions.indexOf(instruction)
        return instructions.subList(0, index).toList()
    }

    inline fun <reified T> getMostRecentInstruction(comparedTo: InterpreterInstruction, predicate: (T) -> Boolean = { true }): T? {
        val index = instructions.indexOf(comparedTo)
        return instructions.subList(0, index).filterIsInstance<T>().lastOrNull(predicate)
    }

    fun findInstructionsCausedBy(causerInstruction: CauserInstruction): List<InterpreterInstruction> {
        causerInstruction as InterpreterInstruction
        val thisCauseIndex = instructions.indexOf(causerInstruction)
        if (thisCauseIndex == instructions.size - 1) {
            return emptyList()
        }
        val nextCauseIndex = getNextInstruction<CauserInstruction>(causerInstruction)?.let { instructions.indexOf(it as InterpreterInstruction) }
        return instructions.subList(thisCauseIndex + 1, nextCauseIndex ?: instructions.size)
    }

    inline fun <reified T> getNextInstruction(comparedTo: InterpreterInstruction, predicate: (T) -> Boolean = { true }): T? {
        val index = instructions.indexOf(comparedTo)
        if (instructions.last() == comparedTo) {
            return null
        }
        return instructions.subList(index + 1, instructions.size).filterIsInstance<T>().firstOrNull(predicate)
    }

    fun getMostRecentCauser(comparedTo: InterpreterInstruction) = getMostRecentInstruction<CauserInstruction>(comparedTo)

    fun execute(battle: PokemonBattle) {
        instructions.forEach { it(battle) }
    }
}