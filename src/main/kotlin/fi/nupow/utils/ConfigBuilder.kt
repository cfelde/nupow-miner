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
@file:JvmName("ConfigBuilder")

package fi.nupow.utils

import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric
import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
    val reader = BufferedReader(InputStreamReader(System.`in`))

    println("Welcome to the NuPoW configuration builder, select options below..")
    println()

    println("Select a token contract:")
    println("""

        Mainnets
        
        1  -> NuPoW Quartz on Ethereum mainnet at 0x5078dd37ee699eB59c096b41C6415417fA02Ec8c
        2  -> NuPoW Ruby on Optimism mainnet at 0xD024b2D92872394F43EE99aa0177d5a24E3A6fC2
        3  -> NuPoW Sapphire on Arbitrum mainnet at 0xDdA51a4585545FBF1169393C8F39686B9838C693
        
        # Testnets
        
        10 -> NuPoW Kovan 1 on Ethereum Kovan at 0x5754046EaD16f75CfB2Cc6FCc91C6Ace118d6f71
        11 -> NuPoW Kovan 2 on Ethereum Kovan at 0xBC0D8283aF1CadEDbbe6445E292811E7cEb58703
        12 -> NuPoW Rinkeby 1 on Ethereum Rinkeby at 0xDD917AfF3cB7Df442e45A93E6B33B02caDe150Eb
        13 -> NuPoW Rinkeby 2 on Ethereum Rinkeby at 0x5754046EaD16f75CfB2Cc6FCc91C6Ace118d6f71
        14 -> NuPoW Görli 1 on Ethereum Görli at 0x1B471d9440FE8e256B8b3568d55e84AA2811b9fc
        15 -> NuPoW Görli 2 on Ethereum Görli at 0x714B3d65824CdD9E7Fe9E9cffE427E4Bb821cC7d
        16 -> NuPoW Ropsten 1 on Ethereum Ropsten at 0xDD917AfF3cB7Df442e45A93E6B33B02caDe150Eb
        17 -> NuPoW Ropsten 2 on Ethereum Ropsten at 0x5754046EaD16f75CfB2Cc6FCc91C6Ace118d6f71

        30 -> NuPoW Arbitrum Rinkeby 1 on Arbitrum Rinkeby at 0x450bBb17f7FF7ffDea973830d93a5707b6763b51
        31 -> NuPoW Arbitrum Rinkeby 2 on Arbitrum Rinkeby at 0x1dF67daF0c7DEE6dA4BBd90AB8Fa934a291886Eb
        32 -> NuPoW Optimism Kovan 1 on Optimism Kovan at 0xeF7697768fdc972e2B2c1bC01656bacE5d158062
        33 -> NuPoW Optimism Kovan 2 on Optimism Kovan at 0xF9030587Dba580022c72b2F6868ac8a4E0404471

    """.trimIndent())

    val (contractAddress, gasLimit, minGasPrice, maxGasPrice) = fun(): List<String> {
        while (true) {
            print(": ")
            try {
                return when (reader.readLine().toInt()) {
                    1 -> listOf("0x5078dd37ee699eB59c096b41C6415417fA02Ec8c", "150000", "1", "45000000000")
                    2 -> listOf("0xD024b2D92872394F43EE99aa0177d5a24E3A6fC2", "150000", "1", "10000000")
                    3 -> listOf("0xDdA51a4585545FBF1169393C8F39686B9838C693", "1000000", "1", "1000000000")
                    10 -> listOf("0x5754046EaD16f75CfB2Cc6FCc91C6Ace118d6f71", "150000", "1", "45000000000")
                    11 -> listOf("0xBC0D8283aF1CadEDbbe6445E292811E7cEb58703", "150000", "1", "45000000000")
                    12 -> listOf("0xDD917AfF3cB7Df442e45A93E6B33B02caDe150Eb", "150000", "1", "45000000000")
                    13 -> listOf("0x5754046EaD16f75CfB2Cc6FCc91C6Ace118d6f71", "150000", "1", "45000000000")
                    14 -> listOf("0x1B471d9440FE8e256B8b3568d55e84AA2811b9fc", "150000", "1", "45000000000")
                    15 -> listOf("0x714B3d65824CdD9E7Fe9E9cffE427E4Bb821cC7d", "150000", "1", "45000000000")
                    16 -> listOf("0xDD917AfF3cB7Df442e45A93E6B33B02caDe150Eb", "150000", "1", "100000000000")
                    17 -> listOf("0x5754046EaD16f75CfB2Cc6FCc91C6Ace118d6f71", "150000", "1", "100000000000")
                    30 -> listOf("0x450bBb17f7FF7ffDea973830d93a5707b6763b51", "1000000", "1", "1000000000")
                    31 -> listOf("0x1dF67daF0c7DEE6dA4BBd90AB8Fa934a291886Eb", "1000000", "1", "1000000000")
                    32 -> listOf("0xeF7697768fdc972e2B2c1bC01656bacE5d158062", "150000", "1", "1000000000")
                    33 -> listOf("0xF9030587Dba580022c72b2F6868ac8a4E0404471", "150000", "1", "1000000000")
                    else -> continue
                }
            } catch (_: Exception) {}
        }
    }()

    println()
    println("Enter private key of miner (or leave blank to generate a new)")
    print(": ")
    val (privateKey, address) = fun(): Pair<String, String> {
        val key = reader.readLine().trim()

        val credentials = if (key.isEmpty())
            Credentials.create(Keys.createEcKeyPair())
        else
            Credentials.create(key)

        return Numeric.toHexStringWithPrefix(credentials.ecKeyPair.privateKey) to credentials.address
    }()

    println()
    println("Enter node endpoint URL (or leave blank to use http://127.0.0.1:8545/)")
    print(": ")
    val nodeEndpoint = reader.readLine().trim().ifEmpty { "http://127.0.0.1:8545/" }

    println()
    println()

    println("""
        # Start of config file (copy this to your config.yml file)
        # -----------------------------------------------------------------------------------------------
        
        # Crystal contract address for mining
        contractAddress: $contractAddress
        
        # Wallet private key (keep private) for $address
        privateKey: $privateKey
        
        # Node URL endpoint
        nodeEndpoint: $nodeEndpoint
        
        # Chain length mining target, relative to contract chain length target.
        # If the contract target is 37, and maxChainLengthAdjustment is set to -1,
        # then the miner will not try to mine past a chain length of 36.
        maxChainLengthAdjustment: -1
        
        # How many CPU threads to use for mining. If set to less than 1,
        # it will use all available CPU threads automatically.
        threadCount: 0
        
        # How much gas to allocate per transaction. Generally should be below 100k,
        # but setting to 150k to have some margin.
        gasLimit: $gasLimit
        
        # Multiplication factor for gas price. Will take current gas price as
        # reported by eth_gasPrice, and multiply by this factor.
        gasPriceAdjustment: 1.1
        
        # Absolute lower bound for gas price
        minGasPrice: $minGasPrice
        
        # Absolute upper bound for gas price
        maxGasPrice: $maxGasPrice
        
        # Abort if current gas price is above max gas price
        abortOnHighGasPrice: true
        
        # Your preferred miner tag
        tag: NuPoW miner
        
        # If true, the miner will run a performance test at startup, to report hash rates
        preheatMiner: true
        
        # -----------------------------------------------------------------------------------------------
        # End of config file
    """.trimIndent())
}