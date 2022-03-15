---
title: VCF Preprocessor
permalink: technical-docs/vcf-preprocessor/
parent: Using PharmCAT
---
# PharmCAT VCF Preprocessor

The PharmCAT VCF preprocessor is a script that can preprocess VCF files for PharmCAT.


This tool will:

1. Retain only PharmCAT PGx positions and eliminate positions that PharmCAT does not care about.
2. Break down a multi-sample VCF to multiple single-sample VCF that PharmCAT requires.
3. Automatically download the necessary Human Reference Genome Sequence fasta and index files from the NIH FTP site, if files are not provided.
4. Perform VCF normalization - a standardization process that turns VCF into a parsimonious, left-aligned variant representation format (as discussed in [Unified Representation of Genetic Variants](https://doi.org/10.1093/bioinformatics/btv112) by Tan, Abecasis, and Kang)
5. Normalize the multiallelic variant representation to PharmCAT's expectation.
6. Process a subset of samples if a sample file is provided.

Two types of **output** are availble from the PharmCAT VCF preprocessing tool:

1. One or more PharmCAT-ready, single-sample VCF file(s)
2. A report of missing pharmacogenomics core allele defining positions in user's input


## How to run the PharmCAT VCF preprocessing tool

### Prerequisite

We assume that the input VCF files are prepared following the [Variant Call Format (VCF) Version >= 4.1](https://samtools.github.io/hts-specs/VCFv4.2.pdf).

To run the tool, you need to download the following bioinformatic tools:
* [bcftools >= v1.11](http://www.htslib.org/download/)
* [htslib >= v1.11](http://www.htslib.org/download/)

We assume a working python3 installation with necessary dependencies:
* python >= 3.5
* pandas>=1.2.3
* scikit-allel>=1.3.3

To install necessary python packages, run the following code
```
pip3 install -r PharmCAT_VCF_Preprocess_py3_requirements.txt
```

### Command line

To normalize and prepare a VCF file (single or multiple samples) for PharmCAT, run the following code substituted with proper arguments/inputs:

```
# kickstart
python3 PharmCAT_VCF_Preprocess.py \
--input_vcf path/to/compressed_vcf.vcf.gz

# more arguments
python3 PharmCAT_VCF_Preprocess.py \
--input_list path/to/vcf_list.txt \
--ref_seq /path/to/reference/human/genome/sequence/grch38 \
--ref_pgx_vcf /path/to/pharmcat_positions.vcf.bgz \
--sample_file /path/to/sample.file \
--path_to_bcftools /path/to/executable/bcftools \
--path_to_tabix /path/to/executable/tabix \
--path_to_bgzip /path/to/executable/bgzip \
--output_folder /folder/to/output/vcf \
--output_prefix pharmcat_ready_vcf_prefix \
--keep_intermediate_files \
--missing_to_ref
```

**Mandatory** input argument:
* `--input_vcf` or `--input_list`
    * `--input_vcf` - path to a single VCF file
    * `--input_list` - path to file containing list of paths to VCF files (one per line), sorted by chromosome position. All VCF files must have the same set of samples.  Use this when data for a sample has been split among multiple files (e.g. VCF files from large cohorts, such as UK Biobank).
      Example valid `input_list` file:
      ```
      chr1_set1.vcf
      chr1_set2.vcf
      chr2_set1.vcf
      chr2_set2.vcf
      ...
      ```
      Example invalid `input_list` file:
      ```
      chr3_set2.vcf
      chr2_set2.vcf
      chr1_set1.vcf
      chr1_set2.vcf
      ...
      ```

VCF files can have more than 1 sample.

VCF files should be bgzip compressed, and if not, they will be automatically bgzipped.


**Optional** input arguments or files
* `--ref_pgx_vcf`  A sorted, compressed VCF of PGx core allele defining positions used by PharmCAT, by default, _"pharmcat_positions.vcf.bgz"_ under the current working directory. You can find this VCF in the *"pharmcat_preprocessor-<release_version>.tar.gz"* avaiable from the PharmCAT GitHub releases page.
* `--ref_seq`  The fasta file of [Genome Reference Consortium Human Build 38 patch release 13 (GRCh38.p13)](https://www.ncbi.nlm.nih.gov/assembly/GCF_000001405.39/). The fasta file has to be decompressed and indexed (.fai). These mandatory files will be automatically downloaded (~1GB) from the NIH FTP site to the current working directory if not provided by user (see **Notes** for details).
* `--sample_file` The list of samples to be processed and prepared for PharmCAT. The file should contain one sample at a line.
* `--path_to_bcftools`  Bcftools must be installed. This argument is optional if users can run bcftools directly using the command line `bcftools <commands>`. Alternatively, users can download and compile [bcftools](http://www.htslib.org/download/) and provide the path to the executable bcftools program as `/path/to/executable/bcftools`.
* `--path_to_tabix` Similar to bcftools, tabix must be installed. Tabix is a part of the [htslib](http://www.htslib.org/download/). If users cannot directly run tabix using the command line `tabix <commands>`, the alternative is to download and compile [htslib](http://www.htslib.org/download/) and provide the path to the executable tabix program as `/path/to/executable/tabix` which should be under the htslib program folder.
* `--path_to_bgzip` Similar to tabix, bgzip must be installed. Bgzip is a part of the [htslib](http://www.htslib.org/download/). If users cannot directly run bgzip using the command line `bgzip <commands>`, the alternative is to download and compile [htslib](http://www.htslib.org/download/) and provide the path to the executable bgzip program as `/path/to/executable/bgzip` which should be under the htslib program folder.
* `--output_folder`  Output a compressed PharmCAT VCF file to /path/to/output/pharmcat/vcf. The default is the parent directory of the input.
* `--output_prefix`  Prefix of the output VCF files. Default is `pharmcat_ready_vcf`.
* `--keep_intermediate_files` This option will help you save useful intermediate files, for example, a normalized, multiallelic VCF named <input_prefix.pgx_regions.normalized.multiallelic.vcf.gz>, which will include all PGx regions from the first position to the last one in each chromosome as listed in the reference PGx VCF.
* `--missing_to_ref` or `-0` for short. This option will add missing PGx positions to the output. Missing PGx positions are those whose genotypes are all missing "./." in every single sample.
  * This option will not convert "./." to "0/0" if any other sample has non-missing genotype at this position as these missing calls are likely missing for good reasons.
  * This **SHOULD ONLY BE USED** if you are sure your data is reference at the missing positions
    instead of unreadable/uncallable at those positions. Running PharmCAT with positions as missing vs reference can lead to different results.


**Output**
1. `≥` 1 PharmCAT-ready VCF file(s), which will be named as "<output_prefix>_<sample_ID>.vcf", for example, `pharmcat_ready_vcf.sample_1.vcf`, `pharmcat_ready_vcf.sample_2.vcf`, etc.
2. The report of missing PGx positions, which will be named as "<output_prefix>.missing_pgx_var.vcf.gz", for example `pharmcat_ready_vcf.missing_pgx_var.vcf.gz`. This file only reports positions that are missing in all samples.
   1. If `--missing_to_ref` is turned on, you can use this report to trace positions whose genotypes are missing in all samples ("./.") in the original input but have now been added into the output VCF(s) as reference ("0/0").

## Tutorial

### Case 1 - single-sample VCF
Imagine we have a VCF named *"test_1.vcf.gz"* to be used in PharmCAT.
```
$ gunzip -c test_1.vcf.gz

##fileformat=VCFv4.1
##source=PharmCAT allele definitions
##reference=hg38
##FORMAT=<ID=GT,Number=1,Type=String,Description="Genotype">
##FILTER=<ID=PASS,Description="All filters passed">
#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	Sample_1
2	233760233	rs3064744	C	CAT	.	PASS	.	GT	1/0
2	233760233	rs3064744	CAT	C	.	PASS	.	GT	0/0
2	233760233	rs3064744	C	CATAT	.	PASS	.	GT	0/1
7	117548628	.	GTTTTTTTA	GTTTTTA	.	PASS	.	GT	0/1
```

Command to run the PharmCAT VCF preprocessor:
```
$ python3 PharmCAT_VCF_Preprocess.py --input_vcf test_1.vcf.gz
```

VCF preprocessor will return two files in this test case.
1. one named *"pharmcat_ready_vcf.Sample_1.vcf"*, which is a PharmCAT-ready VCF
2. the other named *"pharmcat_ready_vcf.missing_pgx_var.vcf.gz"* as a report of missing PGx positions.

To be noted, the chr7 variant is not used in PharmCAT and as such, was accordingly removed by the PharmCAT VCF preprocessor.

```
$ cat reference.Sample_1.vcf

##fileformat=VCFv4.1
##FILTER=<ID=PASS,Description="All filters passed">
##source=PharmCAT allele definitions
##reference=hg38
##FORMAT=<ID=GT,Number=1,Type=String,Description="Genotype">
##INFO=<ID=PX,Number=.,Type=String,Description="Gene">
#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	Sample_1
chr2	233760233	rs3064744	CAT	C,CATATAT,CATAT	.	PASS	PX=UGT1A1	3/2


$ gunzip -c reference.missing_pgx_var.vcf.gz

##fileformat=VCFv4.1
##FILTER=<ID=PASS,Description="All filters passed">
##source=PharmCAT allele definitions
##FORMAT=<ID=GT,Number=1,Type=String,Description="Genotype">
#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	PharmCAT
chr1	97078987	rs114096998	G	T	.	PASS	PX=DPYD	GT	0/0
chr1	97078993	rs148799944	C	G	.	PASS	PX=DPYD	GT	0/0
chr1	97079005	rs140114515	C	T	.	PASS	PX=DPYD	GT	0/0
<...truncated...>
```

### Case 2 - multi-sample VCF
Imagine we have a VCF named *"test_2.vcf.gz"* that has two samples with different sample names from the case 1.
```
$ gunzip -c test_2.vcf.gz

##fileformat=VCFv4.1
##source=PharmCAT allele definitions
##reference=hg38
##FORMAT=<ID=GT,Number=1,Type=String,Description="Genotype">
##FILTER=<ID=PASS,Description="All filters passed">
#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	s1	s2
1	97740414	rs72549309	AATGA	A	.	PASS	.	GT	1/0	0/1
2	233760233	rs3064744	C	CAT	.	PASS	.	GT	1/0	0/0
2	233760233	rs3064744	CAT	C	.	PASS	.	GT	0/0	0/1
2	233760233	rs3064744	C	CATAT	.	PASS	.	GT	0/1	1/0
7	117548628	.	GTTTTTTTA	GTTTTTA	.	PASS	.	GT	0/1	1/0
10	94942212	rs1304490498	AAGAAATGGAA	A	.	PASS	.	GT	1/0	0/1
13	48037826	rs777311140	G	GCGGG	.	PASS	.	GT	1/0	0/1
19	38499645	rs121918596	GGAG	G	.	PASS	.	GT	1/0	0/1
22	42130727	.	AG	A	.	PASS	.	GT	1/0	0/1
M	1555	.	G	A	PASS	.	GT	1/0	0/1
```

Command to run the PharmCAT VCF preprocessor:
```
$ python3 PharmCAT_VCF_Preprocess.py --input_vcf test_2.vcf.gz
```

VCF preprocessor will return three (3) files in this test case.
1. one named *"pharmcat_ready_vcf.s1.vcf"*. Note that the output PharmCAT-ready VCFs will use the exact sample names from the input VCF.
2. one named *"pharmcat_ready_vcf.s2.vcf"*
3. the third named *"pharmcat_ready_vcf.missing_pgx_var.vcf.gz"* as a report of missing PGx positions.

```
$ cat reference.s1.vcf

##fileformat=VCFv4.1
##FILTER=<ID=PASS,Description="All filters passed">
##source=PharmCAT allele definitions
##reference=hg38
##FORMAT=<ID=GT,Number=1,Type=String,Description="Genotype">
##INFO=<ID=PX,Number=.,Type=String,Description="Gene">
#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	Sample_1
chr1    97740410        rs72549309      GATGA   G       .       PASS    PX=DPYD       GT      1/0
chr2    233760233       rs3064744       CAT     C,CATATAT,CATAT .       PASS    PX=UGT1A1 GT      3/2
chr10   94942205        rs1304490498    CAATGGAAAGA     C       .       PASS    PX=CYP2C9     GT      1/0
chr13   48037825        rs777311140     C       CGCGG   .       PASS    PX=NUDT15     GT      1/0
chr19   38499644        rs121918596     TGGA    T       .       PASS    PX=RYR1       GT      1/0

$ cat reference.s2.vcf

##fileformat=VCFv4.1
##FILTER=<ID=PASS,Description="All filters passed">
##source=PharmCAT allele definitions
##reference=hg38
##FORMAT=<ID=GT,Number=1,Type=String,Description="Genotype">
##INFO=<ID=PX,Number=.,Type=String,Description="Gene">
#CHROM  POS     ID      REF     ALT     QUAL    FILTER  INFO    FORMAT  Sample_2
chr1    97740410        rs72549309      GATGA   G       .       PASS    PX=DPYD       GT      0/1
chr2    233760233       rs3064744       CAT     C,CATATAT,CATAT .       PASS    PX=UGT1A1 GT      2/1
chr10   94942205        rs1304490498    CAATGGAAAGA     C       .       PASS    PX=CYP2C9     GT      0/1
chr13   48037825        rs777311140     C       CGCGG   .       PASS    PX=NUDT15     GT      0/1
chr19   38499644        rs121918596     TGGA    T       .       PASS    PX=RYR1       GT      0/1


$ gunzip -c reference.missing_pgx_var.vcf.gz

##fileformat=VCFv4.1
##FILTER=<ID=PASS,Description="All filters passed">
##source=PharmCAT allele definitions
##FORMAT=<ID=GT,Number=1,Type=String,Description="Genotype">
#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	PharmCAT
chr1	97078987	rs114096998	G	T	.	PASS	PX=DPYD	GT	0/0
chr1	97078993	rs148799944	C	G	.	PASS	PX=DPYD	GT	0/0
chr1	97079005	rs140114515	C	T	.	PASS	PX=DPYD	GT	0/0
<...truncated...>
```

### Notes

#### The Human Reference Genome assembly **GRCh38.p13**
Till Jan 2021, The latest Human Reference Genome assembly is **GRCh38.p13** which is accessible through the [NCBI RefSeq FTP site](https://ftp.ncbi.nlm.nih.gov/genomes/all/GCA/000/001/405/GCA_000001405.15_GRCh38/seqs_for_alignment_pipelines.ucsc_ids/). Please download the sequence file, and the index file:
    1. [The GCA 000001405.15 GRCh38](https://ftp.ncbi.nlm.nih.gov/genomes/all/GCA/000/001/405/GCA_000001405.15_GRCh38/seqs_for_alignment_pipelines.ucsc_ids/GCA_000001405.15_GRCh38_no_alt_analysis_set.fna.gz) indexed by the default samtools command.
    2. [The GCA 000001405.15 GRCh38 index file](https://ftp.ncbi.nlm.nih.gov/genomes/all/GCA/000/001/405/GCA_000001405.15_GRCh38/seqs_for_alignment_pipelines.ucsc_ids/GCA_000001405.15_GRCh38_no_alt_analysis_set.fna.fai) generated by the default samtools command.

For more information of the GRCh38.p13, please refer to the [pertinent NCBI Assembly Website](https://www.ncbi.nlm.nih.gov/assembly/GCF_000001405.39). Download and preparation of the human reference genome followed [this blog](http://lh3.github.io/2017/11/13/which-human-reference-genome-to-use).




