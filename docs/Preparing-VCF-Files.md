---
title: Preparing VCF
permalink: preparing-vcf/
---

The requirements for the VCF input file for PharmCAT are described in the VCF [requirement doc](VCF-Requirements). This document explores some reasoning behind those requirements and some specific examples of ways to fulfill them.

* [Must be aligned to GRCh38 Assembly](#must-be-aligned-to-grch38-assembly)
* [Must normalize variant representation](#must-normalize-variant-representation)
* [Must have all allele-defining positions](#must-have-all-allele-defining-positions)
* [The `CHROM` field must be in the format __chr##__](#the-chrom-field-must-be-in-the-format-__chr__)
* [The `QUAL` and `FILTER` columns are __not interpreted__](#the-qual-and-filter-columns-are-__not-interpreted__)  


---


## Must be aligned to GRCh38 Assembly

PharmCAT requires VCF files aligned to GRCh38.

We recommend aligning to GRCh38 from the very beginning - for example, see our [pharmacogenomics NGS pipeline](https://github.com/PharmGKB/pgkb-ngs-pipeline).

While you can translate positions between assemblies, this can easily introduce errors into your data.  For example, LiftOver can be problematic due to ambiguity in positions. In addition, many tools won't update the genotype.

### Known issue with remapping

If you use [CrossMap](http://crossmap.sourceforge.net/) it will update the position and reference nucleotides. However, if the reference is now the same as the alternate, it will remove the variant from the file.  

Likewise, [NCBI Remap](http://crossmap.sourceforge.net/) will not update genotypes, so even if the updated position means the variant should now be called as 0/0 it will not be updated from 1/1.  Presently there are only a few positions where this matters, but this will need to be very carefully checked.  

Another issue with NCBI remap is that some of the alternates can be lost following remap.  For instance:

```
 Pre LiftOver: 2	234668879	rs57191451	CAT	C,CATAT,CATATAT	0/3
Post LiftOver: 2	233760233	rs57191451	CAT	CATAT,CATATAT	0/3
```

We currently do not have a fix for this (NCBI has been contacted), so again you will have to check these sites carefully by hand.


### Example: LiftOver using the GATK

This is a GRCh37-toGRCh38 LiftOver example which uses the GATK LiftoverVcf tool on the UK Biobank genotype data in the GRCh37 coordinates. The GATK LiftoverVcf tool can update genomic coordinates. If the reference allele and alternate allele are swapped in the new genome build for a single nucleotide polymorphism (SNP) locus, the GATK LiftoverVcf can reverse-complement the SNP, update the relevant genotypes (GT), and correct AF-like INFO fields. Due to the length of the example, we host the [example with detailed explanations on Google Drive](https://docs.google.com/document/d/15rxe0iG2kruEWsvBCLyNGof-YRo5T10zuQiBJkUbyJ0/edit?usp=sharing). 

Note: this example is not meant to be a comprehensive documentation of solutions to all LiftOver issues. LiftOver may require additional data cleaning or preparation steps that are specific to your genomic data.


## Must have all allele-defining positions

All positions that are used to define alleles in PharmCAT must be present in the VCF file you want to run through PharmCAT, _even if they are 0/0 (reference) or ./. (missing)_. This is different from a typical VCF file which usually only contains variant sites.

PharmCAT needs this level specificity because reference and missing alleles are interpreted differently by the allele matcher. When positions are missing from the input PharmCAT does make assumptions about whether this is because they are reference or missing.  

Missing positions can be added in the following way using GATK to 'EMIT_ALL_ACTIVE_SITES':

```commandline
gatk --java-options "-Xmx4g" HaplotypeCaller \
     -R grc38.reference.fasta -I input.bam -O output.vcf \
     -L pharmcat_positions.vcf -ip 20 --output-mode EMIT_ALL_ACTIVE_SITES
```

The `pharmcat_positions.vcf` file is [included in the PharmCAT release package on GitHub](https://github.com/PharmGKB/PharmCAT/releases).

Please refer to the [HaplotypeCaller](https://gatk.broadinstitute.org/hc/en-us/articles/360037225632-HaplotypeCaller) documentation for details and tuning options.


## Must normalize variant representation

Variant representation is an on-going problem in NGS ([related article](https://macarthurlab.org/2014/04/28/converting-genetic-variants-to-their-minimal-representation/)).  For example, the following vcf lines all specify the same variant with different formats:

```
chr7    117548628    .    GTTTTTTTA    GTTTTTA    .    PASS    CFTR:5T    GT    0/1
chr7    117548628    .    GTT     G    .    PASS    CFTR:5T    GT    0/1
chr7    117548628    .    G    .    .    PASS    CFTR:5T    GT    0/1
chr7    117548628    .    G(T)7A    G(T)5A    .    PASS    CFTR:5T    GT    0/1
```

Different NGS pipelines, the way files are created (for instance if a multisample file is split), and post-processing software tools all lead to these differences.  As PharmCAT is directly matching these strings to what is in the definition files this can cause problems. For example, PharmCAT expects to find deletions as the ref="ATCT"  alt="A", rather than ref="TCT" alt=".".  Therefore you will need to replace all the deletions within in the file. If the ref is a single letter it means no variant was found, so it's safe to replace it with the appropriate nucleotide string.

To avoid ambiguity in variant representation, PharmCAT is using a parsimonious, left-aligned variant representation format (as discussed in [Unified Representation of Genetic Variants](https://doi.org/10.1093/bioinformatics/btv112) by Tan, Abecasis, and Kang).

We recommend performing this normalization with [bcftools](http://samtools.github.io/bcftools/bcftools.html):

```commandline
bcftools norm -m+ -c ws -Oz -o output.vcf -f grc38.reference.fasta input.vcf
```

* `-m+` joins biallelic sites into multiallelic records (+)
* `-f <ref_seq_fasta>` is the reference sequence. This flag is required for normalization
* `-c ws` when incorrect or missing REF allele is encountered, warn (w) and set/fix(s) bad sites

Please consult the [bcftools documentation](http://samtools.github.io/bcftools/bcftools.html) for details.

Alternatively, you can PharmCAT's [VCF preprocessing tool](Preprocessing-VCF-Files-for-PharmCAT), which also relies on bcftools for this.

It is highly recommended that you always check the output files from these tools manually to make sure the correct format normalizations have been made.


## The `CHROM` field must be in the format __chr##__

PharmCAT expects the `CHROM` field to have entries that begin with "chr" (e.g. `chr1` instead of just `1`).

The [PharmCAT VCF preprocessing tool](Preprocessing-VCF-Files-for-PharmCAT) takes care of this issue by automatically detecting and updating the `CHROM` field format.

Alternatively,`CHROM` field format can be updated using the following command:

```commandline
perl -pe '/^((?!^chr).)*$/ && s/^([^#])/chr$1/gsi' merged_output.vcf > merged_output.chrfixed.vcf
```


## The `QUAL` and `FILTER` columns are __not interpreted__

PharmCAT considers all variants in your file, even if they fail filters.

If you use a tool like VSQR you can use the following Perl one-liner to change the genotype to 0/0 if necessary:

```commandline
perl -pe '/^((?!PASS).)*$/ && /^((?!0\/0).)*$/ && /^((?!0\|0).)*$/ && s/[0-9][\/|][0-9]/0|0/' merged_output.vcf > merged_output_pass.vcf
```
