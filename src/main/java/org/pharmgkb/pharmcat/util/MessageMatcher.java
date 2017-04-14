package org.pharmgkb.pharmcat.util;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.pharmgkb.pharmcat.reporter.model.MatchLogic;
import org.pharmgkb.pharmcat.reporter.model.MessageAnnotation;
import org.pharmgkb.pharmcat.reporter.model.result.GeneReport;
import org.pharmgkb.pharmcat.reporter.model.result.GuidelineReport;


/**
 * Class for handling the logic to match {@link MessageAnnotation} objects to their applicable report objects.
 *
 * @author Ryan Whaley
 */
public class MessageMatcher {

  private Collection<MessageAnnotation> m_messages;

  public MessageMatcher(@Nonnull Collection<MessageAnnotation> messages) {
    Preconditions.checkNotNull(messages);
    m_messages = ImmutableList.copyOf(messages);
  }

  @Nonnull
  public List<MessageAnnotation> match(GeneReport gene) {
    return m_messages.stream()
        .filter(m -> match(m.getMatches(), gene))
        .collect(Collectors.toList());
  }

  @Nonnull
  public List<MessageAnnotation> match(GuidelineReport guideline) {
    return m_messages.stream()
        .filter(m -> match(m.getMatches(), guideline))
        .collect(Collectors.toList());
  }



  public static boolean match(MatchLogic match, GeneReport gene) {

    boolean criteriaPass = !match.getGene().isEmpty() && match.getGene().equals(gene.getGene());

    if (criteriaPass && !match.getHapsCalled().isEmpty()) {
      criteriaPass = match.getHapsCalled().stream().anyMatch(h -> gene.getMatcherDiplotypes().stream().anyMatch(d -> d.hasAllele(h)));
    }

    if (criteriaPass && !match.getHapsMissing().isEmpty()) {
      criteriaPass = match.getHapsMissing().isEmpty()
          || match.getHapsMissing().stream().allMatch(h -> gene.getUncalledHaplotypes().contains(h));
    }

    if (criteriaPass && !match.getDips().isEmpty()) {
      criteriaPass = match.getDips().stream().allMatch(d -> gene.getMatcherDiplotypes().stream().anyMatch(e -> e.printBare().equals(d)));
    }

    if (criteriaPass && !match.getVariantsMissing().isEmpty()) {
      criteriaPass = match.getVariantsMissing().stream().allMatch(v -> gene.getVariantReports().isEmpty() || gene.getVariantReports().stream()
          .anyMatch(a -> a.getDbSnpId() != null && a.getDbSnpId().equals(v) && a.isMissing()));
    }

    return criteriaPass;
  }

  public static boolean match(MatchLogic matchLogic, GuidelineReport report) {
    return matchLogic.getDrugs().stream().anyMatch(d -> report.getRelatedDrugs().contains(d));
  }
}