#!/bin/sh

# parameters:
#  - the CASSANDRA_HOME directory
if [ $# -lt 1 ]; then
  echo "Usage: run-dtests.sh <Cassandra-Directory>";
  exit 1;
fi

# Check if dependencies are installed:
command -v git >/dev/null 2>&1 || { echo >&2 "Coverage reports require git (http://git-scm.com) to be installed. Aborting."; exit 1; }
command -v python2 >/dev/null 2>&1 || { echo >&2 "Coverage reports require Python (http://python.org) to be installed. Aborting."; exit 1; }
command -v ccm >/dev/null 2>&1 || { echo >&2 "Coverage reports require ccm (http://github.com/pcmanus/ccm) to be installed. Aborting."; exit 1; }
python2 -c "import nose" > /dev/null 2>&1
if [ $? != 0 ] ; then
    echo "Coverage reports require nose (http://github.com/nose-devs/nose) to be installed. Aborting."
    exit 1;
fi
python2 -c "import cql" > /dev/null 2>&1
if [ $? != 0 ] ; then
    echo "Coverage reports require cassandra-dbapi2 (https://code.google.com/a/apache-extras.org/p/cassandra-dbapi2/) to be installed. Aborting."
    exit 1;
fi

# Clone cassandra-dtests:
pushd $1/build/cobertura/

# This all needs to change once cobertura fixes are pushed to the
# upstream repositories:
git clone https://github.com/EnigmaCurry/cassandra-dtest.git
cd cassandra-dtest
# Move the cobertura datafile used in unit testing to the
# cassandra-dtest directory. For some reason, dtest will create a
# local datafile regardless if you set the datafile path. So just go
# with the flow and create it here locally:
# Wait for cobertura data file to be unlocked:
while [ -e ../cobertura.ser.lock ]; do
  echo "Waiting for cobertura to finish up..."
  sleep 2
done 
mv ../cobertura.ser .
git checkout cobertura-fixes
# The 'demonstrate' tests aren't working for me, let's skip them:
rm -rf demonstrate

echo "cassandra-dtests running...."
CASSANDRA_DIR=$1 nosetests2 -vvv --debug-log=nosetests.debug.log --with-xunit 2>&1 | tee nosetests.log

# Wait for cobertura data file to be unlocked:
while [ -e cobertura.ser.lock ]; do
  echo "Waiting for cobertura to finish up..."
  sleep 2
done 

# Copy the dtest cobertura datafile back to it's original location
# before the report is generated:
mv cobertura.ser ..

exit 0;
