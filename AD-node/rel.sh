#!/bin/sh
#
#PATH=$PATH:$HOME/bin:$HOME/jdk1.6.0_06/bin
#export PATH
echo "Clean..."
rm -f `find -name "*.class"`
echo "Compile..."
ant compile
echo "Making release..."
mv src $HOME/Etudes/INT/Backup/
zip -r "Expeshare(Linux).zip" *
echo "Completed!"
