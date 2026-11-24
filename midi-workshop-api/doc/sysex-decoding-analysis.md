# Analyse du Décodage SysEx Alesis (MSB vs LSB)

## 1. Le Problème Initial
Une confusion est apparue lors de l'implémentation du décodage des flux SysEx 7-bits vers 8-bits pour les appareils Alesis. 
La "clé officielle" semblait incorrecte, ce qui a mené à l'utilisation d'une clé inversée (`officialKeyReverted`) combinée à une méthode de lecture inversée (`readInvertedBits`).

### Comparaison des Clés
- **Clé Officielle (Alesis) :** `0 A6 A5 A4 A3 A2 A1 A0`
- **Clé Inversée (Reverse Engineering) :** `0 A1 A2 A3 A4 A5 A6 A7`

## 2. Analyse Technique de l'Erreur

### Index de Position vs Index de Poids (Bit Weight)
Le cœur du problème réside dans l'interprétation du chiffre après la lettre (ex: le `6` de `A6`) :

1. **Convention Alesis (Mathématique) :** Le chiffre représente le **poids binaire (LSB -> MSB)**. 
   - `A0` = Bit de poids $2^0$ (le plus à droite).
   - `A6` = Bit de poids $2^6$ (le deuxième en partant de la gauche dans un octet 7-bit).
2. **Interprétation Erronée (Positionnelle) :** Le chiffre a été interprété comme la **position dans le flux de lecture** (de gauche à droite).

### Mécanisme de Lecture (`BitStreamReader`)
La méthode `readBit()` lit les bits du **MSB vers le LSB** (de gauche à droite) :
```java
int bitValue = (currentByte >> (7 - bitIndex)) & 0x1;
```
Pour un octet `0 A6 A5 A4 A3 A2 A1 A0` :
- Le bit à la position 1 (juste après le 0) est physiquement le **bit 6**.
- La clé officielle dit que c'est `A6`.
- Le code fait `1 << 6`.
- **Résultat :** Le bit 6 reste à sa place. C'est correct.

### Pourquoi la clé inversée "fonctionnait" ?
En utilisant `officialKeyReverted` (`0 A1...`), vous déplaciez le bit 6 vers la position 1 en mémoire (effet miroir). Ce décalage était ensuite compensé par `readInvertedBits(7)` qui reconstruisait l'entier "à l'envers", remettant les bits à leur place par une double inversion.

## 3. Solution de Correction (Le "Fix")

Pour utiliser la clé officielle et supprimer la logique de compensation :

### Étape A : Dans `AlesisSysExParser.java`
1. Utiliser la constante `officialKey` dans la définition du périphérique.
2. Remplacer `readInvertedBits(7)` par `readBits(7)` dans toutes les méthodes de lecture de texte ou de données (`getString`, `getProgramName`, etc.).

### Étape B : Pourquoi `readBits(7)` ?
Une fois le tampon "déballé" (`unpack`) avec la clé officielle, les octets en mémoire sont des octets standards. Comme `readBit()` lit du MSB vers le LSB, la méthode `readBits(7)` (lecture normale) reconstruira la valeur ASCII correcte (ex: 65 pour 'A') sans avoir besoin d'inverser l'ordre des bits à nouveau.

---
*Note : Cette analyse confirme que la structure de `ManufacturerSysExParser` et `MidiDeviceDecodingKey` est robuste et suit les standards de programmation. Seule l'interprétation de la clé et la méthode de lecture finale devaient être alignées.*
