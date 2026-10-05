#!/usr/bin/env bash
# Dumps public/protected signatures of selected Minecraft packages to ci-out/javap/.
set -u
mkdir -p ci-out/javap
CP=$(paste -sd: build/classpath.txt)
: > /tmp/classes.txt
grep -E 'minecraft' build/classpath.txt | while read -r jar; do
  case "$jar" in *.jar) unzip -Z1 "$jar" 2>/dev/null | grep '\.class$' >> /tmp/classes.txt ;; esac
done
sort -u /tmp/classes.txt -o /tmp/classes.txt
echo "$(wc -l < /tmp/classes.txt) classes in minecraft jars" > ci-out/javap/summary.txt
grep -E 'minecraft' build/classpath.txt >> ci-out/javap/summary.txt
sed 's#\.class$##; s#/#.#g' /tmp/classes.txt > ci-out/javap/all-classes.txt
while read -r pkg; do
  [ -z "$pkg" ] && continue
  case "$pkg" in \#*) continue ;; esac
  out="ci-out/javap/$(echo "$pkg" | tr -c 'A-Za-z0-9_.\n' '_').txt"
  grep -E "^${pkg//./\\.}(\\.|\$)" ci-out/javap/all-classes.txt | grep -v '\$[0-9]' > /tmp/sel.txt || true
  if [ -s /tmp/sel.txt ]; then
    xargs -a /tmp/sel.txt -n 150 javap -protected -cp "$CP" > "$out" 2>&1 || true
  fi
done < .github/javap-packages.txt
ls -la ci-out/javap >> ci-out/javap/summary.txt
