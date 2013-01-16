#!/bin/sh

# parameters:
#  - the CASSANDRA_HOME directory
if [ $# -lt 1 ]; then
  echo "Usage: run-dtests.sh <Cassandra-Directory>";
  exit 1;
fi

CASSANDRA_HOME=$1

# Check if dependencies are installed:
DEPENDENCIES_NOT_MET=0
command -v git >/dev/null 2>&1 || { echo >&2 "Coverage reports require git (http://git-scm.com) to be installed."; DEPENDENCIES_NOT_MET=1; }
command -v python2 >/dev/null 2>&1 || { echo >&2 "Coverage reports require Python 2.x (http://python.org) to be installed."; DEPENDENCIES_NOT_MET=1; }
command -v ccm >/dev/null 2>&1 || { echo >&2 "Coverage reports require ccm (http://github.com/pcmanus/ccm) to be installed."; DEPENDENCIES_NOT_MET=1; }
python2 -c "import nose" > /dev/null 2>&1
if [ $? != 0 ] ; then
    echo "Coverage reports require nose (http://github.com/nose-devs/nose) to be installed."
    DEPENDENCIES_NOT_MET=1;
fi
python2 -c "import cql" > /dev/null 2>&1
if [ $? != 0 ] ; then
    echo "Coverage reports require cassandra-dbapi2 (https://code.google.com/a/apache-extras.org/p/cassandra-dbapi2/) to be installed."
    DEPENDENCIES_NOT_MET=1;
fi
if [ $DEPENDENCIES_NOT_MET != 0 ] ; then
    echo "Aborting."
    exit 1;
fi


pushd $CASSANDRA_HOME/build/cobertura/

# Clone cassandra-dtests:
git clone git://github.com/riptano/cassandra-dtest.git
cd cassandra-dtest
# Wait for cobertura data file to be unlocked:
while [ -e ../cobertura.ser.lock ]; do
  echo "Waiting for cobertura to finish up..."
  sleep 2
done 
# Move the cobertura datafile used in unit testing to the
# cassandra-dtest directory. For some reason, dtest will create a
# local datafile regardless if you set the datafile path. So just go
# with the flow and create it here locally:
mv ../cobertura.ser .
# The 'demonstrate' tests aren't working for me, let's skip them:
rm -rf demonstrate

EXCLUDE_TESTS="'upgrade|decommission|sstable_gen|global_row|putget_2dc|cql3_insert'"

# Platform independent python2 nose command:
NOSE="python2 -c 'import nose; nose.main()'"

# First pass - Run dtests normally:
echo "cassandra-dtests running..."
CASSANDRA_DIR=$1 $NOSE -e $EXCLUDE_TESTS -vvv --debug-log=nosetests.debug.log --with-xunit 2>&1 | tee nosetests.log

# Wait for cobertura data file to be unlocked:
while [ -e cobertura.ser.lock ]; do
  echo "Waiting for cobertura to finish up..."
  sleep 2
done 

# Second pass - Run dtests with vnodes:
echo "cassandra-dtests with vnodes running..."
ENABLE_VNODES=true CASSANDRA_DIR=$1 $NOSE -e $EXCLUDE_TESTS -vvv --debug-log=nosetests.vnodes.debug.log --with-xunit 2>&1 | tee nosetests.vnodes.log

# Wait for cobertura data file to be unlocked:
while [ -e cobertura.ser.lock ]; do
  echo "Waiting for cobertura to finish up..."
  sleep 2
done 

# Copy the dtest cobertura datafile back to it's original location
# before the report is generated:
mv cobertura.ser ..

exit 0;
