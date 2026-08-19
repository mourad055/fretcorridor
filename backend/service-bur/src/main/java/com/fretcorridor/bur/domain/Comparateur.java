package com.fretcorridor.bur.domain;

import java.math.BigDecimal;

public enum Comparateur {
    SUPERIEUR {
        @Override
        public boolean declenchee(BigDecimal valeur, BigDecimal seuil) {
            return valeur.compareTo(seuil) > 0;
        }
    },
    INFERIEUR {
        @Override
        public boolean declenchee(BigDecimal valeur, BigDecimal seuil) {
            return valeur.compareTo(seuil) < 0;
        }
    };

    public abstract boolean declenchee(BigDecimal valeur, BigDecimal seuil);
}
