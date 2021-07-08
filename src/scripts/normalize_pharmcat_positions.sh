#!/bin/sh


# cd to PharmCAT root
cd $(dirname $0)/../..


docker run --mount type=bind,source="$(pwd)"/build,target=/app/data pharmcat \
  bcftools norm -m+ -c ws -Oz -o data/pharmcat_positions.vcf -f grch38.fasta data/orig_pharmcat_positions.vcf
