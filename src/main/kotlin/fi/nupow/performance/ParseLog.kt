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
@file:JvmName("ParseLog")

package fi.nupow.performance

import java.io.File
import java.math.BigInteger

fun main(args: Array<String>) {
    val file = File(args.first())

    var startTime = 0L
    var totalHashCount = 0L

    val totalHashCountHash = mutableListOf<String>()
    val chainLengthHash = mutableListOf<String>()
    val durationHash = mutableListOf<String>()

    for (line in file.readLines()) {
        if (line.trim().isBlank()) {
            startTime = 0L
            totalHashCount = 0L
            continue
        }

        val parts = line.trim().splitToSequence("\t").toList()

        val timestamp = parts[0].toLong()
        val hashCount = parts[1].toLong()
        val chainLength = parts[2].toLong()
        //val seed = BigInteger(parts[3], 16)
        val hash = BigInteger(parts[4], 16)

        totalHashCount += hashCount

        if (startTime == 0L) {
            startTime = timestamp
            continue
        }

        if (totalHashCount > 1000) {
            totalHashCountHash.add("" + totalHashCount + "\t" + hash)
        }

        chainLengthHash.add("" + chainLength + "\t" + hash)

        if (timestamp - startTime > 1000) {
            durationHash.add("" + (timestamp - startTime) + "\t" + hash)
        }
    }

    println("totalHashCountHash ----")
    for (line in totalHashCountHash) {
        println(line)
    }
    println()

    println("chainLengthHash ----")
    for (line in chainLengthHash) {
        println(line)
    }
    println()

    println("durationHash ----")
    for (line in durationHash) {
        println(line)
    }
    println()
}