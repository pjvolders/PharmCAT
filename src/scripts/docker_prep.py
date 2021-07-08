#!/usr/bin/env python3

"""
Used by Dockerfile to import fasta files.
Doing this in python so we don't have to maintain versions in two separate locations.
"""

import os

import vcf_preprocess_utilities as util


grch_file = os.path.join(os.getcwd(), 'grch38.fasta')
if not os.path.exists(grch_file):
    util.download_grch38_ref_fasta_and_index(os.getcwd(), grch_file)
