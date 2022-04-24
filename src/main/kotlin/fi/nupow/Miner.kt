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
@file:JvmName("Miner")

package fi.nupow

import fi.nupow.contract.NuPoW
import com.sksamuel.hoplite.ConfigLoader
import org.slf4j.LoggerFactory
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.Uint
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.StaticGasProvider
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.lang.Long.max
import java.lang.Long.min
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private val LOGGER = LoggerFactory.getLogger("NuPoW-Miner")

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Expecting first argument to point to configuration file")
        exitProcess(1)
    }

    val config = ConfigLoader().loadConfigOrThrow<MinerConfig>(args[0])

    val tag = config.tag
    val maxChainLengthAdjustment = config.maxChainLengthAdjustment
    val threadCount = if (config.threadCount <= 0) Runtime.getRuntime().availableProcessors() else config.threadCount
    val contractAddress = config.contractAddress
    val web3j = Web3j.build(HttpService(config.nodeEndpoint))
    val credentials = Credentials.create(config.privateKey)
    val gasPriceAdjustment = BigDecimal(config.gasPriceAdjustment)
    val minGasPrice = BigInteger(config.minGasPrice)
    val maxGasPrice = BigInteger(config.maxGasPrice)

    val transactionManager = object : RawTransactionManager(web3j, credentials, web3j.ethChainId().send().chainId.toLong()) {
        private fun nextNonce(web3j: Web3j, address: String): BigInteger {
            return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send().transactionCount
        }

        override fun getNonce(): BigInteger {
            val nonce = nextNonce(web3j, credentials.address)
            LOGGER.info("Nonce: $nonce")
            return nonce
        }
    }

    val gasLimit = BigInteger.valueOf(config.gasLimit.toLong())
    val gasProvider = object : StaticGasProvider(BigInteger(config.minGasPrice), gasLimit) {
        override fun getGasPrice(contractFunc: String?): BigInteger {
            return getGasPrice()
        }

        override fun getGasPrice(): BigInteger {
            val gasPrice = web3j.ethGasPrice().send().gasPrice.toBigDecimal().multiply(gasPriceAdjustment, MathContext.DECIMAL128).toBigInteger().let {
                if (it < minGasPrice) return@let minGasPrice
                else if (it > maxGasPrice && config.abortOnHighGasPrice) throw RuntimeException("Current gas price too high at $it > $maxGasPrice")
                else if (it > maxGasPrice) return@let maxGasPrice
                return@let it
            }
            LOGGER.info("Gas price: $gasPrice")
            return gasPrice
        }
    }

    val nupow = NuPoW.load(contractAddress, web3j, transactionManager, gasProvider)

    val running = AtomicBoolean(true)
    val results = LinkedBlockingQueue<MiningResult>(threadCount * 2)
    val maxRnd = BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16)
    val params = AtomicReference(MiningParams(credentials.address, BigInteger.ZERO, Long.MIN_VALUE, !config.preheatMiner))

    var iteration: Long = Long.MIN_VALUE

    LOGGER.info("Miner address: ${credentials.address}")
    web3j.ethGetBalance(credentials.address, DefaultBlockParameterName.LATEST).send().let {
        LOGGER.info("Miner Ether balance: ${Convert.fromWei(it.balance.toBigDecimal(), Convert.Unit.ETHER)}")
    }
    LOGGER.info("Using ${config.gasPriceAdjustment} as gas price adjustment factor, with ${config.minGasPrice} as min gas price and ${config.maxGasPrice} as max gas price")

    LOGGER.info("Crystal contract address: ${nupow.contractAddress}")
    LOGGER.info("Crystal contract name: ${nupow.name().send()}")
    LOGGER.info("Crystal contract symbol: ${nupow.symbol().send()}")
    val decimals = nupow.decimals().send()
    LOGGER.info("Crystal contract decimals: $decimals")
    val chainLengthTarget = nupow.CHAIN_LENGTH_TARGET().send().toInt()
    LOGGER.info("Crystal contract chain length target: $chainLengthTarget")
    val maxChainLength = chainLengthTarget + maxChainLengthAdjustment
    LOGGER.info("Configured max chain length target: $maxChainLength")
    val maxMint = nupow.MAX_MINT().send()
    LOGGER.info("Crystal contract max mint: 0x${maxMint.toString(16)} (${maxMint.toBigDecimal().divide(BigDecimal.TEN.pow(decimals.toInt())).toPlainString()})")
    nupow.balanceOf(credentials.address).send().let {
        LOGGER.info("Miner Crystal balance: ${it.toBigDecimal().divide(BigDecimal.TEN.pow(decimals.toInt())).toPlainString()}")
    }

    try {
        LOGGER.info("Starting $threadCount miner threads..")

        val threads = mutableListOf<Thread>()

        for (i in 1..threadCount) {
            threads.add(thread(isDaemon = true, name = "Miner-$i") {
                try {
                    LOGGER.info("Starting miner-$i thread..")
                    while (running.get()) {
                        if (params.get().paused) Thread.sleep(100)
                        else findSeed(params, results, running)
                    }
                    LOGGER.info("Ending miner-$i thread..")
                } catch (ex: Exception) {
                    LOGGER.error("Exception in miner thread: ${ex.message}", ex)
                } finally {
                    running.set(false)
                }
            })

            Thread.sleep(10)
        }

        if (config.preheatMiner) {
            LOGGER.info("Warming miner threads..")
            Thread.sleep(20000)
            params.set(MiningParams(credentials.address, BigInteger.ZERO, ++iteration, false))
            Thread.sleep(20000)
            params.set(MiningParams(credentials.address, BigInteger.ZERO, ++iteration, true))
            LOGGER.info("Miner threads warm..")
            Thread.sleep(20000)
        } else {
            LOGGER.debug("Skipping miner thread warming due to config")
        }

        while (running.get()) {
            val stalledTimestamp = (nupow.stalledTimestamp().send().toLong() * 1000L) + 1000L
            if (stalledTimestamp <= System.currentTimeMillis()) {
                // Stalled, try to mint
                LOGGER.info("Stalled, trying to mint..")
                nupow.mint(BigInteger(maxRnd.bitLength(), ThreadLocalRandom.current()).mod(maxRnd), tag).send()
                nupow.balanceOf(credentials.address).send().let {
                    LOGGER.info("Miner Crystal balance: ${it.toBigDecimal().divide(BigDecimal.TEN.pow(decimals.toInt())).toPlainString()}")
                }
            } else {
                val difficulty = Numeric.toBigInt(nupow.lastHash().send())
                val chainLength = nupow.chainLength().send().toLong()
                if (difficulty != params.get().difficulty) {
                    params.set(MiningParams(credentials.address, difficulty, ++iteration, chainLength >= maxChainLength))
                    LOGGER.info("Will stall at " + Instant.ofEpochMilli(stalledTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime())
                    val nextMint = nupow.nextMint().send()
                    LOGGER.info("Next mint: 0x${nextMint.toString(16)} (${nextMint.toBigDecimal().divide(BigDecimal.TEN.pow(decimals.toInt())).toPlainString()})")
                    LOGGER.info("Current difficulty: 0x${difficulty.toString(16)}")
                    LOGGER.info("Current chain length: ${nupow.chainLength().send()}")
                    if (params.get().paused) LOGGER.info("Miner is paused due to chain length..")
                }

                do {
                    val sleepTime = max(min(stalledTimestamp - System.currentTimeMillis(), 10 * 1000), 1000)
                    val result = results.poll(sleepTime, TimeUnit.MILLISECONDS)

                    if (result != null && result.iteration == iteration && chainLength < maxChainLength) {
                        LOGGER.info("Found new seed, minting with 0x${result.seed.toString(16)} !")
                        nupow.mint(result.seed, tag).send()
                        nupow.balanceOf(credentials.address).send().let {
                            LOGGER.info("Miner Crystal balance: ${it.toBigDecimal().divide(BigDecimal.TEN.pow(decimals.toInt())).toPlainString()}")
                        }
                        break
                    }
                } while (!results.isEmpty())
            }
        }

        threads.forEach { it.join() }
    } catch (ex: Exception) {
        LOGGER.error("Exception in main thread: ${ex.message}", ex)
    } finally {
        running.set(false)
    }
}

private fun findSeed(
    params: AtomicReference<MiningParams>,
    results: BlockingQueue<MiningResult>,
    running: AtomicBoolean
) {
    val startTime = System.currentTimeMillis()

    val param = params.get()
    val iteration = param.iteration
    val difficulty = param.difficulty
    val addressString = "000000000000000000000000" + param.address.substring(2)

    val output = CharArray(192)
    val outputBytes = ByteArray(96)
    val encodedDifficulty = TypeEncoder.encode(Uint(difficulty))
    val maxRnd = BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16)

    for (i in addressString.indices) {
        output[64 + i] = addressString[i]
    }

    for (i in encodedDifficulty.indices) {
        output[128 + i] = encodedDifficulty[i]
    }

    var lastHash = difficulty
    var seed = BigInteger(maxRnd.bitLength(), ThreadLocalRandom.current()).mod(maxRnd)
    var hashCount: Long = 0

    while (lastHash >= difficulty) {
        seed = seed.subtract(BigInteger.ONE)
        if (seed.signum() == -1) seed = maxRnd

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

        if (hashCount % 100000L == 0L && (iteration != params.get().iteration || !running.get() || params.get().paused)) break
    }

    val endTime = System.currentTimeMillis()

    if (iteration == params.get().iteration && running.get() && !params.get().paused && lastHash < difficulty) {
        // If we block here, likely due to an ongoing mint transaction, so good to pause a bit
        results.offer(MiningResult(seed, iteration), 1, TimeUnit.SECONDS)
    }

    val duration = (endTime - startTime) / 1000
    val hashPerSecond = hashCount / if (duration > 0) duration else 1

    if (duration > 10) LOGGER.info("Hash rate: $hashPerSecond/s")
}

private class MiningParams(
    val address: String,
    val difficulty: BigInteger,
    val iteration: Long,
    val paused: Boolean
)

private class MiningResult(
    val seed: BigInteger,
    val iteration: Long
)
