git clone https://github.com/cfelde/nupow-miner.git
cd nupow-miner
./build.sh
cd bin
nano config.yml
java -cp nupow-miner.jar -Dlogback.configurationFile=logback.xml fi.nupow.Miner config.yml
