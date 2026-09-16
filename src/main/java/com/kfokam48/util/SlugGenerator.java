package com.kfokam48.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Transforme un titre en slug utilisable dans une URL.
 *
 * Supprime les accents, passe en minuscules, ne garde que lettres, chiffres et
 * tirets, et borne la longueur. Classe utilitaire non instanciable.
 */
public class SlugGenerator {

    private SlugGenerator() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) return "";

        String slug = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s-]+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.length() > 250) {
            slug = slug.substring(0, 250).replaceAll("-+$", "");
        }

        return slug;
    }
}
