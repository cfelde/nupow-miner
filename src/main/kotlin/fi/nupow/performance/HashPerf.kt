/*
 * Copyright (C) 2022  Christian Felde
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
@file:JvmName("HashPerf")

package fi.nupow.performance

import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Uint
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

fun main() {
    while (true) {
        runHashPerf()
        println()
    }
}

private fun runHashPerf() {
    val address = "000000000000000000000000f8effd31978f2ceb483f6b1d0eb807461117ecc2" //Address("0xf8EffD31978f2ceb483F6b1d0Eb807461117ECc2")
    val maxRnd = BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16)
    val maxRadBitlength = maxRnd.bitLength()
    val random = ThreadLocalRandom.current()
    val output = CharArray(192)
    val outputBytes = ByteArray(96)

    for (i in address.indices) {
        output[64 + i] = address[i]
    }

    var difficulty = BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16)
    var encodedDifficulty: String
    var lastHash: BigInteger
    var chainLength = 0
    var seed = BigInteger.ZERO
    var hashCount: Long
    var timeLimit = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES)

    while (true) {
        lastHash = difficulty
        hashCount = 0L

        encodedDifficulty = TypeEncoder.encode(Uint(difficulty))
        for (i in encodedDifficulty.indices) {
            output[128 + i] = encodedDifficulty[i]
        }

        while (lastHash >= difficulty) {
            seed = BigInteger(maxRadBitlength, random).mod(maxRnd)
            val encodedSeed = TypeEncoder.encode(Uint(seed))

            for (i in encodedSeed.indices) {
                output[i] = encodedSeed[i]
            }

            for (i in 0 until 192 step 2) {
                outputBytes[(i + 1) / 2] = ((Character.digit(output[i], 16) shl 4) + Character.digit(output[i + 1], 16)).toByte()
            }

            val hashBytes = Hash.sha3(outputBytes)
            lastHash = BigInteger(Numeric.toHexString(hashBytes, 0, hashBytes.size, false), 16)

            hashCount++

            if (hashCount % 100000L == 0L && timeLimit < System.currentTimeMillis()) break
        }

        if (timeLimit < System.currentTimeMillis()) break

        difficulty = lastHash
        chainLength++
        timeLimit = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES)

        println("" + System.currentTimeMillis() + "\t" + hashCount + "\t" + chainLength + "\t" + seed.toString(16) + "\t" + lastHash.toString(16))
    }
}



