package com.turtlehoarder.cobblemonchallenge.api.storage.party

import  com.cobblemon.mod.common.api.storage.StorePosition
import com.cobblemon.mod.common.api.storage.party.PartyPosition
import com.cobblemon.mod.common.net.IntSize
import com.cobblemon.mod.common.util.readSizedInt
import com.cobblemon.mod.common.util.writeSizedInt
import net.minecraft.network.FriendlyByteBuf

class FakePartyPosition(val slot: Int = 0) : StorePosition {
    companion object {
        fun FriendlyByteBuf.writePartyPosition(position: FakePartyPosition) {
            writeSizedInt(IntSize.U_BYTE, position.slot)
        }
        fun FriendlyByteBuf.readPartyPosition() = FakePartyPosition(readSizedInt(IntSize.U_BYTE))
    }
}