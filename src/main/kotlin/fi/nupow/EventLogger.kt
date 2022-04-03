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
package fi.nupow

import fi.nupow.contract.NuPoW
import com.sksamuel.hoplite.ConfigLoader
import org.slf4j.LoggerFactory
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger
import kotlin.system.exitProcess

private val LOGGER = LoggerFactory.getLogger("NuPoW-Events")

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Expecting first argument to point to configuration file")
        exitProcess(1)
    }

    val config = ConfigLoader().loadConfigOrThrow<MinerConfig>(args[0])

    val contractAddress = config.contractAddress
    val web3j = Web3j.build(HttpService(config.nodeEndpoint))
    val credentials = Credentials.create(config.privateKey)

    val transactionManager = RawTransactionManager(web3j, credentials, web3j.ethChainId().send().chainId.toLong())
    val nupow = NuPoW.load(contractAddress, web3j, transactionManager, DefaultGasProvider())

    nupow.chainProgressEventFlowable(DefaultBlockParameter.valueOf(BigInteger.valueOf(0)), DefaultBlockParameterName.LATEST).subscribe {
        LOGGER.info(it.toString())
    }
}