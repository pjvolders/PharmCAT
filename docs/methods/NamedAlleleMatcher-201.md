---
title: Named Allele Matcher 201
permalink: methods/named-allele-matcher-201/
parent: Methods
---
# NamedAlleleMatcher 201

This document attempts to cover more advanced details of the `NamedAlleleMatcher`.

This is really only applicable if you are defining your own named allele definitions.


### Scoring

The score for a given named allele is determined by the number of positions at which alleles have been provided.

Example:

|     | rs1 | rs2 | rs3 | rs4 | rs5 | score |
| --- | --- | --- | --- | --- | --- | ----- |
| *1  | C   | C   | T   | G   | A   | 5     |
| *2  | T   | T   |     | A   |     | 3     |

The default named allele definitions are designed to assume that missing alleles are the same as the reference named allele, which is defined as the first named allele in the definition file.

It is, however, possible to increse the score of a named allele by specifying the reference allele.  For example, this gene definition table is effectively identical to the one above, but _*2_ has a different score.

|     | rs1 | rs2 | rs3 | rs4 | rs5 | score |
| --- | --- | --- | --- | --- | --- | ----- |
| *1  | C   | C   | T   | G   | A   | 5     |
| *2  | T   | T   | T   | A   | A   | 5     |



### Exemptions

`src/main/resources/org/pharmgkb/pharmcat/definition/alleles/exemptions.json` gives you a way to modify the behavior of the `NamedAlleleMatcer`.


### Assuming the Reference

If you are designing your own named allele definitions, you might not want to assume the reference for missing alleles.  If you want to treat missing alleles as "unknown" and anything can be accepted in it's place, you can modify this behavior.

You would need to add something like this:

```
  {
    "gene": "XXX",
    "assumeReference": true
  }
```

### Ignoring Named Alleles

If you are designing your own named allele definitions, you might need to define a named allele but not want it to be considered by the `NamedAlleleMatcher`.

You would need to add something like this:

```
  {
    "gene": "XXX",
    "ignoredAlleles": [
      "*1S"
    ],
    "ignoredAllelesLc": [
      "*1s"
    ]
  }
```

### Return All Diplotypes

The `NamedAlleleMatcher` can return all matching diplotypes instead of just the top scoring one(s).

You would need to add something like this:

```
  {
    "gene": "XXX",
    "allHits": true
  }
```
