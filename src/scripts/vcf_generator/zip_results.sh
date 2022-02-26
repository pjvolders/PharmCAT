#!/bin/bash
#
# Zip results of current test run.
#
set -e
set -u
set -o pipefail


EXACT=""
FUZZY=""
MISSING=""

while getopts 'efm' OPTION; do
  case "$OPTION" in
    e)
      EXACT="-exact"
      ;;

    f)
      FUZZY="-fuzzy"
      ;;

    m)
      MISSING="-missing"
      ;;

    ?)
      echo "script usage: $(basename $0) [-m]" >&2
      exit 1
      ;;
  esac
done
shift "$(($OPTIND -1))"


# cd to location of script
cd $(dirname $0)

if [ -z ${PHARMCAT_DATA_DIR+x} ]; then
  dataDir="../../../build"
else
  # expect PHARMCAT_DATA_DIR to be a relative directory
  dataDir="../../../${PHARMCAT_DATA_DIR}"
fi


zipFile="vcfTest-`date +'%Y-%m-%d'`${EXACT}${FUZZY}${MISSING}.zip"

cd ${dataDir}; zip -q -r ${zipFile} autogeneratedTestResults
echo "Zipped to: ${zipFile}"
