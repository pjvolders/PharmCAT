#!/bin/bash
# To run:
# chmod 775 update.sh
# ./update.sh merged_output.chrfixed.trimmed.vcf

echo "Replacing large indels"
vim \
-c '%s/chr2\t233760234\t.*\tA\t/chr2\t233760234\t.\tATATATATATATATAA\t/gc'  \
-c '%s/chr10\t94942212\t.*\tA\t/chr10\t94942212\t.\tAAGAAATGGAA\t/gc'  \
-c '%s/chr7\t117548628\t.*\tG\t/chr7\t117548628\t.\tGTTTTTTTA\t/gc'  \
-c 'wq' \
$1

echo "Replacing dels"
vim \
-c '%s/chr1\t97740414\t.*\tA\t/chr1\t97740414\trs72549309\tAATGA\t/gc'  \
-c '%s/chr7\t117559591\t.*\tA\t/chr7\t117559591\trs113993960\tTCTT\t/gc'  \
-c '%s/chr7\t117559589\t.*\tC\t/chr7\t117559589\trs121908745\tCATC\t/gc'  \
-c '%s/chr7\t117559591\t.*\tT\t/chr7\t117559591\trs113993960\tTCTT\t/gc'  \
-c '%s/chr7\t117592218\t.*\tA\t/chr7\t117592218\trs121908746\tAA\t/gc'  \
-c '%s/chr7\t117627580\t.*\tC\t/chr7\t117627580\trs121908747\tCC\t/gc'  \
-c '%s/chr10\t94949281\t.*\tG\t/chr10\t94949281\trs9332131\tGA\t/gc'  \
-c '%s/chr1\t97450065\t.*\tT\t/chr1\t97450065\trs72549303\tTG\t/gc'  \
-c 'wq' \
$1