# syntax=docker/dockerfile:1
FROM python:3

RUN apt-get -y update && \
    apt-get -y upgrade && \
    apt-get -y install bzip2 build-essential wget


ENV BCFTOOLS_VERSION 1.12
ENV HTSLIB_VERSION 1.12

# download the suite of tools
WORKDIR /usr/local/bin/
RUN wget https://github.com/samtools/bcftools/releases/download/${BCFTOOLS_VERSION}/bcftools-${BCFTOOLS_VERSION}.tar.bz2
RUN wget https://github.com/samtools/htslib/releases/download/${HTSLIB_VERSION}/htslib-${HTSLIB_VERSION}.tar.bz2

# extract files for the suite of tools
RUN tar -xjf /usr/local/bin/bcftools-${BCFTOOLS_VERSION}.tar.bz2 -C /usr/local/bin/
RUN tar -xjf /usr/local/bin/htslib-${HTSLIB_VERSION}.tar.bz2 -C /usr/local/bin/

# compile tools
RUN cd /usr/local/bin/htslib-${HTSLIB_VERSION}/ && ./configure
RUN cd /usr/local/bin/htslib-${HTSLIB_VERSION}/ && make && make install

RUN cd /usr/local/bin/bcftools-${BCFTOOLS_VERSION}/ && make && make install

# cleanup
RUN rm -f /usr/local/bin/bcftools-${BCFTOOLS_VERSION}.tar.bz2
RUN rm -rf /usr/local/bin/bcftools-${BCFTOOLS_VERSION}
RUN rm -f /usr/local/bin/htslib-${HTSLIB_VERSION}.tar.bz2
RUN rm -rf /usr/local/bin/htslib-${HTSLIB_VERSION}


WORKDIR /app
COPY src/scripts/PharmCAT_VCF_Preprocess.py .
COPY src/scripts/vcf_preprocess_*.py .
COPY src/scripts/PharmCAT_VCF_Preprocess_py3_requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# copy data
# use * after pharmcat_positions.vcf because file will not exist on initial build
COPY src/scripts/docker_prep.py build/pharmcat_positions.vcf* ./
RUN ./docker_prep.py


# clean up APT when done
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
