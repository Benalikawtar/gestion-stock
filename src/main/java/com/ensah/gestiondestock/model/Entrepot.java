package com.ensah.gestiondestock.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Entrepot implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;

    private String nom;
    private String adresse;

    // Transferts
    @OneToMany(mappedBy = "source")
    private List<Transfert> transfertsSortants;

    @OneToMany(mappedBy = "destination")
    private List<Transfert> transfertsEntrants;

    // Réceptions
    @OneToMany(mappedBy = "entrepot")
    private List<Reception> receptions;

    // Livraisons
    @OneToMany(mappedBy = "entrepot")
    private List<Livraison> livraisons;

    // Inventaires
    @OneToMany(mappedBy = "entrepot")
    private List<Inventaire> inventaires;
    @OneToMany(mappedBy = "entrepot")
    private List<Produit> produits;
    public Long getId() {
        return id;
    }

}
