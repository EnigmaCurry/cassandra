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

# Clone cassandra-dtests:
pushd $1/build/cobertura/

# This all needs to change once cobertura fixes are pushed to the
# upstream repositories:
git clone https://github.com/EnigmaCurry/cassandra-dtest.git
cd cassandra-dtest
git checkout cobertura-fixes
rm -rf demonstrate

echo "cassandra-dtests running...."
CASSANDRA_DIR=$1 python2 -c "import nose; nose.run()" 2>&1

exit 0;
