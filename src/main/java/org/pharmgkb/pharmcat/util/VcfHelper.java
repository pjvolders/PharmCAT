package org.pharmgkb.pharmcat.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pharmgkb.common.util.PathUtils;
import org.pharmgkb.pharmcat.definition.model.DefinitionExemption;
import org.pharmgkb.pharmcat.definition.model.DefinitionFile;
import org.pharmgkb.pharmcat.definition.model.VariantLocus;
import org.pharmgkb.pharmcat.haplotype.DefinitionReader;


/**
 * Helper utils for working VCF.
 *
 * @author Mark Woon
 */
public class VcfHelper implements AutoCloseable {
  private static final String sf_vcfUrl = "https://api.pharmgkb.org/v1/pharmcat/hgvs/%s/vcf";
  private static final String sf_extraPositionUrl = "https://api.pharmgkb.org/v1/pharmcat/extraPosition/%s";
  private static final String sf_vcfCacheFile   = "vcfQueryCache.json";
  private static final String sf_defaultAssembly = "GRCh38";
  private static final Gson sf_gson = new GsonBuilder()
      .setPrettyPrinting()
      .create();

  private final CloseableHttpClient m_httpclient;
  private Map<String, Map<String, Object>> m_queryCache;
  private boolean m_queryCacheUpdated;


  public VcfHelper() throws IOException {
    m_httpclient = HttpClients.createDefault();

    // load cached data
    try (BufferedReader reader = Files.newBufferedReader(PathUtils.getPathToResource(getClass(), sf_vcfCacheFile))) {
      //noinspection unchecked
      m_queryCache = sf_gson.fromJson(reader, Map.class);
      if (m_queryCache == null) {
        m_queryCache = new HashMap<>();
      }
    }
  }


  @Override
  public void close() throws IOException {
    m_httpclient.close();

    if (m_queryCacheUpdated) {
      Path cacheFile = PathUtils.getPathToResource(getClass(), sf_vcfCacheFile);
      System.out.println("Query cache updated!  Saving cache file to " + cacheFile);
      System.out.println("Don't forget to save this file!");
      try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(cacheFile))) {
        sf_gson.toJson(m_queryCache, writer);
      } catch (Exception ex) {
        throw new IOException("Error writing new " + sf_vcfCacheFile, ex);
      }
    }
  }


  /**
   * Translate HGVS to VCF.
   */
  public VcfData hgvsToVcf(String hgvs) throws IOException {
    String url = String.format(sf_vcfUrl, URLEncoder.encode(hgvs, StandardCharsets.UTF_8.name()));
    return new VcfData(runQuery(url));
  }

  /**
   * Translate RSID to {@link VariantLocus}.
   */
  public VariantLocus rsidToVariantLocus(String rsid) throws IOException {
    String url = String.format(sf_extraPositionUrl, URLEncoder.encode(rsid, StandardCharsets.UTF_8.name()));
    VcfData vcfData = new VcfData(runQuery(url));

    VariantLocus vl = new VariantLocus(vcfData.chrom, vcfData.pos,
        vcfData.hgvs.substring(0, vcfData.hgvs.indexOf(":") + 1));
    vl.setRsid(rsid);
    vl.setRef(vcfData.ref);
    vl.addAlt(vcfData.alt);

    SortedSet<String> cpicAlleles = new TreeSet<>();
    cpicAlleles.add(vcfData.ref);
    cpicAlleles.add(vcfData.alt);
    vl.setCpicAlleles(cpicAlleles);

    Map<String, String> vcfMap = new HashMap<>();
    vcfMap.put(vcfData.ref, vcfData.ref);
    vcfMap.put(vcfData.alt, vcfData.alt);
    vl.setCpicToVcfAlleleMap(vcfMap);

    return vl;
  }

  private Map<String, Object> runQuery(String url) throws IOException {

    if (m_queryCache.containsKey(url)) {
      return m_queryCache.get(url);
    }

    HttpGet httpGet = new HttpGet(url);
    httpGet.setHeader(HttpHeaders.ACCEPT, "application/json");
    try (CloseableHttpResponse response = m_httpclient.execute(httpGet)) {
      HttpEntity entity = response.getEntity();
      if (response.getStatusLine().getStatusCode() != 200) {
        String error = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        throw new IOException(response.getStatusLine().getReasonPhrase() + " querying " + url + ": " + error);
      }
      try (InputStreamReader reader = new InputStreamReader(entity.getContent())) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>)sf_gson.fromJson(reader, Map.class).get("data");
        m_queryCache.put(url, data);
        m_queryCacheUpdated = true;
        return data;
      }
    }
  }



  /**
   * Runs bcftools to normalize VCF.
   */
  public static VcfData normalizeRepeats(String chr, Collection<VcfData> data) throws IOException {

    Path inFile = Files.createTempFile(null, ".vcf");
    Path outFile = Files.createTempFile(null, ".vcf");
    try {
      try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(inFile))) {
        HashMap<String, String> contigs = new HashMap<>();
        contigs.put(chr, sf_defaultAssembly);
        printVcfHeaders(writer, "PharmCAT", contigs);

        // don't bother sorting, bcftools will deal with it
        for (VcfData vcf : data) {
          printVcfLine(writer, chr, vcf.pos, null, vcf.ref, vcf.alt, null, "0/0");
        }
      }

      DockerRunner.normalizeVcf(inFile, outFile);
      try (BufferedReader bufferedReader = Files.newBufferedReader(outFile)) {
        String line;
        do {
          line = bufferedReader.readLine();
        } while (line.startsWith("#"));
        List<VcfData> output = new ArrayList<>();
        while (line != null) {
          output.add(new VcfData(line));
          line = bufferedReader.readLine();
        }
        if (output.size() > 1) {
          throw new IllegalStateException("Could not merge repeats: " + output);
        }
        return output.get(0);
      }

    } finally {
      Files.deleteIfExists(inFile);
      Files.deleteIfExists(outFile);
    }
  }


  public static void extractPositions(SortedSet<String> genes, DefinitionReader definitionReader, Path file)
      throws IOException {

    Date date = definitionReader.getDefinitionFile(genes.first()).getModificationDate();
    ZonedDateTime timestamp = date.toInstant().atZone(ZoneId.systemDefault());

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
      printVcfHeaders(writer, "PharmCAT allele definitions", getContigs(genes, definitionReader), timestamp);

      SortedSet<VcfLine> lines = new TreeSet<>();
      for (String gene : genes) {
        DefinitionFile definitionFile = definitionReader.getDefinitionFile(gene);
        for (VariantLocus vl : definitionFile.getVariants()) {
          lines.add(new VcfLine(definitionFile.getChromosome(), vl.getPosition(), vl.getRsid(), vl.getRef(),
              String.join(",", vl.getAlts()), "PX=" + gene, "0/0"));
        }

        DefinitionExemption exemption = definitionReader.getExemption(gene);
        if (exemption != null) {
          for (VariantLocus vl : exemption.getExtraPositions()) {
            lines.add(new VcfLine(definitionFile.getChromosome(), vl.getPosition(), vl.getRsid(), vl.getRef(),
                String.join(",", vl.getAlts()), "POI", "0/0"));
          }
        }
      }
      for (VcfLine line : lines) {
        writer.println(line.toString());
      }
    }
  }


  /**
   * Gets a contig map containing chromosome name to assembly.
   */
  public static HashMap<String, String> getContigs(Collection<String> genes, DefinitionReader definitionReader) {
    HashMap<String, String> contigs = new HashMap<>();
    for (String gene : genes) {
      DefinitionFile definitionFile = definitionReader.getDefinitionFile(gene);
      contigs.put(definitionFile.getChromosome(), definitionFile.getGenomeBuild());
    }
    return contigs;
  }

  public static void printVcfHeaders(PrintWriter writer, String source, @Nullable HashMap<String, String> contigs) {
    printVcfHeaders(writer, source, contigs, ZonedDateTime.now());
  }

  public static void printVcfHeaders(PrintWriter writer, String source, @Nullable HashMap<String, String> contigs,
      ZonedDateTime timestamp) {
    writer.println("##fileformat=VCFv4.2");
    writer.println("##source=" + source);
    writer.println("##fileDate=" + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp));

    if (contigs != null) {
      Set<String> assemblies = new HashSet<>(contigs.values());
      if (assemblies.size() > 1) {
        throw new IllegalStateException("Multiple assemblies found: " + assemblies);
      }
      String assembly = assemblies.iterator().next();
      SortedSet<String> sortedChr = new TreeSet<>(new ChrNameComparator());
      sortedChr.addAll(contigs.keySet());
      for (String chr : sortedChr) {
        writer.println("##contig=<ID=" + chr + ",assembly=" + assembly + ",species=\"Homo sapiens\">");
      }
    }

    writer.println("##FILTER=<ID=PASS,Description=\"All filters passed\">");
    writer.println("##INFO=<ID=PX,Number=.,Type=String,Description=\"Gene\">");
    writer.println("##INFO=<ID=POI,Number=0,Type=Flag,Description=\"Position of Interest but not part of an allele definition\">");
    writer.println("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">");
    writer.println("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tPharmCAT");
  }


  public static void printVcfLine(PrintWriter writer, String chr, long pos, @Nullable String rsid, String ref,
      String alt, @Nullable String info, String sample) {

    writer.print(chr);
    writer.print("\t");
    writer.print(pos);
    writer.print("\t");
    writer.print(Objects.requireNonNullElse(rsid, "."));
    writer.print("\t");
    writer.print(ref);
    writer.print("\t");
    writer.print(alt);
    writer.print("\t." +    // qual
        "\tPASS\t");        // filter
    writer.print(Objects.requireNonNullElse(info, "."));
    writer.print("\tGT\t"); // format
    writer.println(sample);
  }



  private static class VcfLine implements Comparable<VcfLine> {
    String chrom;
    long pos;
    String id;
    String ref;
    String alt;
    String info;
    String sample;

    VcfLine(String chr, long pos, @Nullable String rsid, String ref,
        String alt, @Nullable String info, String sample) {
      chrom = chr;
      this.pos = pos;
      id = rsid;
      this.ref = ref;
      this.alt = alt;
      this.info = info;
      this.sample = sample;
    }


    @Override
    public int compareTo(@NonNull VcfLine o) {
      if (this == o) {
        return 0;
      }
      int rez = ChrNameComparator.INSTANCE.compare(chrom, o.chrom);
      if (rez != 0) {
        return rez;
      }
      if (pos == o.pos) {
        throw new IllegalArgumentException("2 entries at same position:\n" + this + "\n" + o);
      }
      return Long.compare(pos, o.pos);
    }


    @Override
    public String toString() {
      return chrom + "\t" + pos + "\t" + Objects.requireNonNullElse(id, ".") + "\t" +
          ref + "\t" + alt + "\t" +
          ".\tPASS\t" + // qual, filter
          Objects.requireNonNullElse(info, ".") +
          "\tGT\t" + // format
          sample;
    }
  }

  public static class VcfData {
    public String hgvs;
    public String chrom;
    public final long pos;
    public final String ref;
    public final String alt;

    /**
     * Parses response from API.
     */
    VcfData(Map<String, Object> data) {
      hgvs = (String)data.get("hgvs");
      chrom = (String)data.get("chrom");
      pos = ((Number)data.get("position")).longValue();
      ref = (String)data.get("ref");
      alt = (String)data.get("alt");
    }

    /**
     * Parses data line from VCF.
     */
    VcfData(String vcfLine) {
      String[] data = vcfLine.split("\t");
      chrom = data[0];
      pos = Long.parseLong(data[1]);
      ref = data[3];
      alt = data[4];
    }


    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof VcfData)) {
        return false;
      }
      VcfData o = (VcfData)obj;
      return Objects.equals(chrom, o.chrom) &&
          Objects.equals(pos, o.pos) &&
          Objects.equals(ref, o.ref) &&
          Objects.equals(alt, o.alt);
    }

    @Override
    public String toString() {
      return "pos=" + pos + "; ref=" + ref + "; alt=" + alt;
    }
  }
}
