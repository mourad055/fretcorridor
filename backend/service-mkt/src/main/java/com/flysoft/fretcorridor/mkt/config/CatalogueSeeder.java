package com.flysoft.fretcorridor.mkt.config;

import com.flysoft.fretcorridor.mkt.entity.CatalogueEmballage;
import com.flysoft.fretcorridor.mkt.repository.CatalogueEmballageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// RG-035 : catalogue d'emballages types du marché camerounais.
// Valeurs de poids/volume par défaut = estimations MVP à affiner par une
// vraie observation de terrain (mentionné explicitement dans le CDC).
@Component
@RequiredArgsConstructor
public class CatalogueSeeder implements CommandLineRunner {

    private final CatalogueEmballageRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;

        repository.saveAll(java.util.List.of(
            CatalogueEmballage.builder().nom("Sac de ciment").icone("inventory_2")
                .poidsUnitaireKg(50.0).volumeUnitaireM3(0.03).gerbable(true).ordreAffichage(1).build(),
            CatalogueEmballage.builder().nom("Sac 50 kg (général)").icone("shopping_bag")
                .poidsUnitaireKg(50.0).volumeUnitaireM3(0.05).gerbable(true).ordreAffichage(2).build(),
            CatalogueEmballage.builder().nom("Carton").icone("inventory")
                .poidsUnitaireKg(15.0).volumeUnitaireM3(0.06).gerbable(true).ordreAffichage(3).build(),
            CatalogueEmballage.builder().nom("Fût").icone("propane_tank")
                .poidsUnitaireKg(180.0).volumeUnitaireM3(0.2).gerbable(false).ordreAffichage(4).build(),
            CatalogueEmballage.builder().nom("Bidon").icone("water_drop")
                .poidsUnitaireKg(25.0).volumeUnitaireM3(0.03).gerbable(true).ordreAffichage(5).build(),
            CatalogueEmballage.builder().nom("Palette").icone("pallet")
                .poidsUnitaireKg(500.0).volumeUnitaireM3(1.2).gerbable(false).ordreAffichage(6).build(),
            CatalogueEmballage.builder().nom("Régime de bananes").icone("eco")
                .poidsUnitaireKg(30.0).volumeUnitaireM3(0.08).fragileParDefaut(true).gerbable(false).ordreAffichage(7).build(),
            CatalogueEmballage.builder().nom("Sac de charbon").icone("inventory_2")
                .poidsUnitaireKg(40.0).volumeUnitaireM3(0.07).gerbable(true).ordreAffichage(8).build(),
            CatalogueEmballage.builder().nom("Mobilier").icone("chair")
                .poidsUnitaireKg(80.0).volumeUnitaireM3(1.0).fragileParDefaut(true).gerbable(false).ordreAffichage(9).build(),
            CatalogueEmballage.builder().nom("Matériel électronique").icone("devices")
                .poidsUnitaireKg(20.0).volumeUnitaireM3(0.15).fragileParDefaut(true).gerbable(false).ordreAffichage(10).build(),
            CatalogueEmballage.builder().nom("Véhicule").icone("directions_car")
                .poidsUnitaireKg(1200.0).volumeUnitaireM3(8.0).gerbable(false).ordreAffichage(11).build(),
            CatalogueEmballage.builder().nom("Vrac").icone("grain")
                .poidsUnitaireKg(1.0).volumeUnitaireM3(0.001).gerbable(true).ordreAffichage(12).build()
        ));
    }
}
