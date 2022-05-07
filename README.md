NuPoW miner
===========

This is an implementation of a NuPoW miner, for use with the official NuPoW tokens.

More information about NuPoW can be found at [nupow.fi](https://nupow.fi)

How to use
----------

Using the miner is easy. Make sure you have Java 11 or later installed.

Steps for fetching, building and using miner:

```
git clone https://github.com/cfelde/nupow-miner.git
cd nupow-miner
./build.sh
cd bin
nano config.yml
java -cp nupow-miner.jar -Dlogback.configurationFile=logback.xml fi.nupow.Miner config.yml
```

You can also generate a configuration file by running `java -cp nupow-miner.jar fi.nupow.utils.ConfigBuilder` from
within the `bin` folder after running `./build.sh`.

More information on [mining NuPoW Crystal tokens](https://blog.cfelde.com/2022/05/mining-nupow-crystal-tokens/) in my blog post.
